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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.datademo.tools.XPathFieldInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
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

    /*
     * The callback for the DocumentsWalker. We perform the update here without
     * having to care about the query, the pagination, ...
     */
    protected class DocumentsCallbackImpl implements DocumentsCallback {

        long pageCount = 0;

        long documentCount = 0;

        ArrayList<XPathFieldInfo> fieldInfos;

        protected DocumentsCallbackImpl(ArrayList<XPathFieldInfo> inFieldsInfo) {
            fieldInfos = inFieldsInfo;
        }

        @Override
        public boolean callback(List<DocumentModel> inDocs) {

            updateDocs(inDocs, fieldInfos);

            pageCount += 1;
            documentCount += inDocs.size();

            return true;
        }

        @Override
        public boolean callback(DocumentModel inDoc) {

            // We don't use this one, so make sure we don't try to use it in he
            // future
            throw new UnsupportedOperationException();
        }

        public long getPageCount() {
            return pageCount;
        }

        public long getDocumentCount() {
            return documentCount;
        }

    }

    /*
     * Main entry point
     */
    public void run(boolean inDisableListeners) {

        if (diffInDays < 1) {
            log.error("Date received is in the future or less than one day: No update done");
            return;
        }

        ToolsMisc.forceLogInfo(log,
                "\n--------------------\nIncrease all dates by " + diffInDays
                        + " days\n--------------------");

        if (inDisableListeners) {
            disableListeners();
        }

        totalUpdatedDocs = 0;
        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        DocumentType[] allTypes = sm.getDocumentTypes();
        for (DocumentType dt : allTypes) {
            Collection<Schema> schemas = dt.getSchemas();
            ArrayList<XPathFieldInfo> fieldsInfo = new ArrayList<XPathFieldInfo>();

            for (Schema schema : schemas) {
                for (Field field : schema.getFields()) {
                    Type t = field.getType();
                    if (t.isSimpleType() || t.isListType()) {
                        String typeName = ToolsMisc.getCoreFieldType(t);
                        // If ToolsMisc.getCoreFieldType() could find a "date"
                        // core type, we know the field is not complex or
                        // complex-multivalued => We can handle it.
                        //
                        // If it does not return "date", we will check complex
                        // types in the else clauses
                        if (typeName.equals("date")) {
                            fieldsInfo.add(new XPathFieldInfo("" + field.getName(),
                                    t.isListType(), "date"));
                        } else if (t.isListType()) {

                            // Check if complex-multivalued
                            Type subType = ((ListType) t).getFieldType();
                            if (subType.isComplexType()) {
                                ComplexType ct = (ComplexType) subType;
                                String parentXPath = field.getName().getPrefixedName();
                                Map<String, String> subFieldsXPathsAndTypes = ToolsMisc.getComplexFieldSubFieldsInfo(
                                        ct, parentXPath);
                                for (String oneXPath : subFieldsXPathsAndTypes.keySet()) {
                                    if (subFieldsXPathsAndTypes.get(oneXPath).equals(
                                            "date")) {
                                        XPathFieldInfo xpfi = new XPathFieldInfo(oneXPath, true, "date");
                                        xpfi.setComplexListParentXPath(parentXPath);
                                        fieldsInfo.add(xpfi);
                                    }
                                }
                            }
                        }
                    } else if (t.isComplexType()) {
                        ComplexType ct = (ComplexType) t;
                        Map<String, String[]> subFieldsXPathsAndTypes = ToolsMisc.getComplexFieldSubFieldsInfoPro(
                                ct, field.getName().getPrefixedName());
                        for (String oneXPath : subFieldsXPathsAndTypes.keySet()) {
                            String[] subInfos = subFieldsXPathsAndTypes.get(oneXPath);
                            if (subInfos[0].equals("date")) {
                                fieldsInfo.add(new XPathFieldInfo(oneXPath,
                                        subInfos[1].equals("1"), "date"));
                            }
                        }
                    }
                }
            }

            if (fieldsInfo.size() > 0) {

                String nxql = "SELECT * FROM " + dt.getName();

                DocumentModelList docs = session.query(nxql, 1);
                if (docs.size() == 0) {
                    continue;
                }

                ToolsMisc.forceLogInfo(log,
                        "Update dates for documents of type: " + dt.getName());

                DocumentsCallbackImpl cb = new DocumentsCallbackImpl(fieldsInfo);
                DocumentsWalker dw = new DocumentsWalker(session, nxql,
                        docsPerPage);
                dw.runForEachPage(cb);

                ToolsMisc.forceLogInfo(log, "" + cb.getDocumentCount() + " '"
                        + dt.getName() + "' documents updated");
            }
        }

        if (inDisableListeners) {
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

        for (EventListenerDescriptor d : descs) {
            if (enabledListeners.contains(d.getName())) {
                d.setEnabled(true);
            }
        }

        ell.recomputeEnabledListeners();

        ToolsMisc.forceLogInfo(log, "Listeners restored.");
    }

    /**
     * Update all date fields whose xpaths are passed in <code>inXPaths</code>.
     * <p>
     * No control/check if the document has the correct schema.
     * 
     * @param inDocs
     * @param inFieldsInfo
     *
     * @since 7.2
     */
    protected void updateDocs(List<DocumentModel> inDocs,
            ArrayList<XPathFieldInfo> inFieldsInfo) {

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        int count = 0;
        for (DocumentModel oneDoc : inDocs) {

            for (XPathFieldInfo oneInfo : inFieldsInfo) {
                updateDate(oneDoc, oneInfo);
            }
            oneDoc = session.saveDocument(oneDoc);

            count += 1;
            if ((count % docsPerTransaction) == 0) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

            updateDocCount += 1;
            totalUpdatedDocs += 1;
            if ((updateDocCount % logEveryNDocs) == 0) {
                ToolsMisc.forceLogInfo(log, "" + updateDocCount
                        + " (total docs: " + updateDocCount + ")");
            }
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

    }

    protected void updateListOfDates(DocumentModel inDoc, String inXPath) {
        Calendar[] dates = (Calendar[]) inDoc.getPropertyValue(inXPath);
        if (dates != null && dates.length > 0) {
            for (Calendar d : dates) {
                d.add(Calendar.DATE, diffInDays);
            }
            inDoc.setPropertyValue(inXPath, dates);
        }
    }

    /*
     * We know we handle only dates. No need to check inInfo.getCoreTypeName()
     */
    protected void updateDate(DocumentModel inDoc, XPathFieldInfo inInfo) {

        if (inInfo.isSimple()) {
            if (inInfo.getXPath().equals("TestSchema:the_complex/list_of_dates")) {
                System.out.println(inInfo.getXPath());
            }
            Calendar d = (Calendar) inDoc.getPropertyValue(inInfo.getXPath());
            if (d != null) {
                d.add(Calendar.DATE, diffInDays);
                inDoc.setPropertyValue(inInfo.getXPath(), d);
            }
        } else if (inInfo.isList()) {
            String complexParentXPath = inInfo.getComplexListParentXPath();
            String subFieldName = inInfo.getSubFieldName();
            if (complexParentXPath != null && !complexParentXPath.isEmpty()) {
                Serializable values = inDoc.getPropertyValue(complexParentXPath);
                if (values != null) {
                    int length = 0;
                    if (values instanceof ArrayList) {
                        length = ((ArrayList<?>) values).size();
                    } else if (values instanceof HashMap) {
                        length = ((HashMap<?, ?>) values).size();
                    } else if (values instanceof Object[]) {
                        length = ((Object[]) values).length;
                    }
                    for (int i = 0; i < length; i++) {
                        String finalXPath = complexParentXPath + "/" + i + "/"
                                + subFieldName;
                        updateListOfDates(inDoc, finalXPath);
                    }
                }

            } else {
                updateListOfDates(inDoc, inInfo.getXPath());
                /*
                 * Calendar[] dates = (Calendar[])
                 * inDoc.getPropertyValue(inInfo.getXPath()); if (dates != null
                 * && dates.length > 0) { for (Calendar d : dates) {
                 * d.add(Calendar.DATE, diffInDays); }
                 * inDoc.setPropertyValue(inInfo.getXPath(), dates); }
                 */
            }
        } else {
            if (inInfo.getXPath().equals("TestSchema:the_complex/list_of_dates")) {
                System.out.println(inInfo.getXPath());
            }
        }/*
          * else if (inInfo.isList() && inInfo.isComplex()) { // To be done
          * if(inInfo.getXPath().equals("TestSchema:the_complex/list_of_dates"))
          * { System.out.println(inInfo.getXPath()); }
          * 
          * } else if (inInfo.isComplex()) {
          * if(inInfo.getXPath().equals("TestSchema:the_complex/list_of_dates"))
          * { System.out.println(inInfo.getXPath()); } Calendar d = (Calendar)
          * inDoc.getPropertyValue(inInfo.getXPath()); if (d != null) {
          * d.add(Calendar.DATE, diffInDays);
          * inDoc.setPropertyValue(inInfo.getXPath(), d); } }
          */
    }

    public void setDocsPerTransaction(int inNewValue) {
        docsPerTransaction = inNewValue > 0 ? inNewValue
                : DEFAULT_DOCS_PER_TRANSACTION;
    }

    public int getDocsPerTransaction() {
        return docsPerTransaction;
    }

    public int getLogeveryNDocs() {
        return logEveryNDocs;
    }

    public void setLogeveryNDocs(int inNewValue) {
        logEveryNDocs = inNewValue > 0 ? inNewValue : DEFAULT_LOG_EVENY_N_DOCS;
        ;
    }

    public int getDocsPerPage() {
        return docsPerPage;
    }

    public void setDocsPerPage(int inNewValue) {
        docsPerPage = inNewValue > 0 ? inNewValue : DEFAULT_DOCS_PER_PAGE;
    }

}
