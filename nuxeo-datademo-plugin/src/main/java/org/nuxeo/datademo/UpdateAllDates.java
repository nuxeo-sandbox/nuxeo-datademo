/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.datademo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 *
 * @since 7.1
 */
public class UpdateAllDates {

    private static final Log log = LogFactory.getLog(UpdateAllDates.class);
    
    public static final int DEFAULT_DOCS_PER_TRANSACTION = 50;
    
    public static final int DEFAULT_DOCS_PER_PAGE = 500;
    
    public static final int DEFAULT_LOG_EVENY_N_DOCS = 500;

    CoreSession session;

    int diffInDays;
    
    long updateDocCount = 0;
    
    long totalUpdatedDocs = 0;

    ArrayList<String> enabledListeners = new ArrayList<String>();

    boolean wasBlockAsyncHandlers;

    boolean wasBlockSyncPostCommitHandlers;
    
    int docsPerTransaction = DEFAULT_DOCS_PER_TRANSACTION;
    
    int docsPerPage = DEFAULT_DOCS_PER_PAGE;
    
    int logEveryNDocs = DEFAULT_LOG_EVENY_N_DOCS;

    public UpdateAllDates(CoreSession inSession, int inDays) {

        session = inSession;
        diffInDays = inDays;
    }

    public UpdateAllDates(CoreSession inSession, Date inLastUpdate) {

        session = inSession;

        long diffInMs = Calendar.getInstance().getTimeInMillis()
                - inLastUpdate.getTime();
        diffInDays = (int) TimeUnit.DAYS.convert(diffInMs,
                TimeUnit.MILLISECONDS);
        if (diffInMs < 86400000 || diffInDays < 1) {
            diffInDays = 0;
        }
    }

    public void run(boolean inDisableListeners) {

        if (diffInDays < 1) {
            log.error("Date received is in the future or less than one day: No update done");
            return;
        }

        ToolsMisc.forceLogInfo(log,
                "\n--------------------\nIncrease all dates by " + diffInDays
                        + " days\n--------------------");

        if(inDisableListeners) {
            disableListeners();
        }

        totalUpdatedDocs = 0;
        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        DocumentType[] allTypes = sm.getDocumentTypes();
        for (DocumentType dt : allTypes) {
            Collection<Schema> schemas = dt.getSchemas();
            ArrayList<String> xpaths = new ArrayList<String>();

            for (Schema schema : schemas) {
                for (Field field : schema.getFields()) {
                    Type t = field.getType();
                    
                    // PAsser par: field.getType().getTypeHierarchy()
                    // et récupérer en fait le dernier type => ce sera le type de base
                    if (t.isSimpleType() && t.getName().equals("date")) {
                        xpaths.add("" + field.getName());
                    }
                }
            }

            if (xpaths.size() > 0) {
                
                String nxql;
                long updatedDocsCount = 0;
                
                ToolsMisc.forceLogInfo(log,
                        "Update dates for documents of type: " + dt.getName());

                // Create CoreQueryDocumentPageProvider and updates its description 
                CoreQueryDocumentPageProvider cqpp = new CoreQueryDocumentPageProvider();
                CoreQueryPageProviderDescriptor ppDesc = new CoreQueryPageProviderDescriptor();
                ppDesc.setPattern("SELECT * FROM " + dt.getName());
                cqpp.setDefinition(ppDesc);

                HashMap<String, Serializable> props = new HashMap<String, Serializable>();
                props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                        (Serializable) session);
                cqpp.setProperties(props);
                cqpp.setMaxPageSize(docsPerPage);
                cqpp.setPageSize(docsPerPage);

                List<DocumentModel> docs = cqpp.getCurrentPage();
                while(docs != null && docs.size() > 0) {
                    
                    updatedDocsCount += docs.size();
                    updateDocs(docs, xpaths);
                    
                    if(cqpp.isNextPageAvailable()) {
                        cqpp.nextPage();
                        docs = cqpp.getCurrentPage();
                    } else {
                        docs = null;
                    }
                }

                ToolsMisc.forceLogInfo(log,
                        "" + updatedDocsCount + "'" + dt.getName() + "' documents updated");
            }
        }

        if(inDisableListeners) {
            restoreListeners();
        }
    }

    protected void disableListeners() {

        ToolsMisc.forceLogInfo(log, "Disabling all listeners...");

        EventServiceImpl esi = (EventServiceImpl) Framework.getService(EventService.class);

        wasBlockAsyncHandlers = esi.isBlockAsyncHandlers();
        wasBlockSyncPostCommitHandlers = esi.isBlockSyncPostCommitHandlers();
        esi.setBlockAsyncHandlers(true);
        esi.setBlockSyncPostCommitHandlers(true);

        EventListenerList ell = esi.getListenerList();

        ArrayList<EventListenerDescriptor> descs = new ArrayList<EventListenerDescriptor>();
        descs.addAll(ell.getEnabledInlineListenersDescriptors());
        //descs.addAll(ell.getEnabledAsyncPostCommitListenersDescriptors());
        //descs.addAll(ell.getEnabledSyncPostCommitListenersDescriptors());

        for (EventListenerDescriptor d : descs) {
            enabledListeners.add(d.getName());
            d.setEnabled(false);
        }

        ell.recomputeEnabledListeners();

        ToolsMisc.forceLogInfo(log,
                "Disabled listeners: " + enabledListeners.toString());
    }

    protected void restoreListeners() {

        ToolsMisc.forceLogInfo(log, "Restoring the listeners...");

        EventServiceImpl esi = (EventServiceImpl) Framework.getService(EventService.class);
        esi.setBlockAsyncHandlers(wasBlockAsyncHandlers);
        esi.setBlockSyncPostCommitHandlers(wasBlockSyncPostCommitHandlers);

        EventListenerList ell = esi.getListenerList();

        ArrayList<EventListenerDescriptor> descs = new ArrayList<EventListenerDescriptor>();
        descs.addAll(ell.getInlineListenersDescriptors());
        //descs.addAll(ell.getAsyncPostCommitListenersDescriptors());
        //descs.addAll(ell.getSyncPostCommitListenersDescriptors());

        for (EventListenerDescriptor d : descs) {
            if (enabledListeners.contains(d.getName())) {
                d.setEnabled(true);
            }
        }

        ell.recomputeEnabledListeners();
    }
    
    /**
     * Update all date fields whose xpaths are passed in <code>inXPaths</code>.
     * <p>
     * No control/check if the document has the correct schema.
     * 
     * @param inDocs
     * @param inXPaths
     *
     * @since 7.2
     */
    protected void updateDocs(List<DocumentModel> inDocs, ArrayList<String> inXPaths) {
        
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        
        /* Once saveDocument(s) is optimized, we will be able to use it.
         * Not yet done as of "today" (JAN 2015)
         */
        /*
        int total = 0;
        Iterator<DocumentModel> it = inDocs.iterator();
        while(it.hasNext()) {
            
            ArrayList<DocumentModel> subList = new ArrayList<DocumentModel>();
            int count = 0;
            do {
                DocumentModel doc = it.next();
                for(String xpath : inXPaths) {
                    updateDate(doc, xpath);
                }
                subList.add(doc);
                count += 1;
                total += 1;
            } while(it.hasNext() || count < docsPerTransaction);
            
            session.saveDocuments( (DocumentModel[]) subList.toArray() );
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            if((total % logEveryNDocs) == 0) {
                ToolsMisc.forceLogInfo(log, "" + count);
            }
        };
        */
        
        int count = 0;
        for(DocumentModel oneDoc : inDocs) {
            
            for(String xpath : inXPaths) {
                updateDate(oneDoc, xpath);
            }
            
            // Save without dublincore and custom events (in the Studio project)
            //oneDoc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
            //oneDoc.putContextData("UpdatingData_NoEventPlease", true);
            oneDoc = session.saveDocument(oneDoc);
            
            count += 1;
            if((count % docsPerTransaction) == 0) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
            
            updateDocCount += 1;
            totalUpdatedDocs += 1;
            if((updateDocCount % logEveryNDocs) == 0) {
                ToolsMisc.forceLogInfo(log, "" + updateDocCount + " (total docs: " + updateDocCount + ")");
            }
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        
    }

    protected void updateDate(DocumentModel inDoc, String inXPath) {

        Calendar d = (Calendar) inDoc.getPropertyValue(inXPath);
        if (d != null) {
            d.add(Calendar.DATE, diffInDays);
            inDoc.setPropertyValue(inXPath, d);
        }
    }
    
    public void setDocsPerTransaction(int inNewValue) {
        docsPerTransaction = inNewValue > 0 ? inNewValue : DEFAULT_DOCS_PER_TRANSACTION;
    }
    
    public int getDocsPerTransaction() {
        return docsPerTransaction;
    }

    public int getLogeveryNDocs() {
        return logEveryNDocs;
    }

    public void setLogeveryNDocs(int inNewValue) {
        logEveryNDocs = inNewValue > 0 ? inNewValue : DEFAULT_LOG_EVENY_N_DOCS;;
    }

    public int getDocsPerPage() {
        return docsPerPage;
    }

    public void setDocsPerPage(int inNewValue) {
        docsPerPage = inNewValue > 0 ? inNewValue : DEFAULT_DOCS_PER_PAGE;
    }

}
