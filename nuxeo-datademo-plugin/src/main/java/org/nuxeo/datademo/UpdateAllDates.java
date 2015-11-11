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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.datademo.tools.ListenersDisabler;
import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.datademo.tools.XPathFieldInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * The main <code>run()</code> method walks all Document types and each schema to find date fields. It updates all and
 * every date fields, adding some days, allowing some options (disable some listeners, log progress, ...).
 * <p>
 * WARNING: In this first version, caller must make sure the update is done with enough rights, the code is not run in
 * an unrestricted session.
 * <p>
 * ABOUT LISTENERS:
 * <ul>
 * <li>The code always disable the dublincore listener while updating the dates.</li>
 * <li>If you have EventHandlers configured in Studio, check if they must be disabled, and call
 * <code>addListenerToDisable()</code>. IMPORTANT: To disable Handlers built in Studio, use the "opchainlistener" and
 * the "opchainpclistener" listeners (they handle all the operationChain-based listeners)</li>
 * </ul>
 * <p>
 * The following cases are handled:
 * <ul>
 * <li>Simple date field (<code>myschema:my_date</code>)</li>
 * <li>Multivalued (list) date field (<code>myschema:list_of_dates</code>)</li>
 * <li>In a <i>complex</li> field:
 * <ul>
 * <li>Simple date field (<code>myschema:the_complex/sub_date</code>)</li>
 * <li>Multivalued (list) date field ( <code>myschema:the_complex/sub_list_of_dates</code>)</li>
 * <li>In a <i>multivalued (list) complex</li> field:
 * <ul>
 * <li>Simple date field (<code>myschema:the_complex_list/n/one_date</code>)</li>
 * <li>Multivalued (list) date field ( <code>myschema:the_complex_list/n/a_list_of_dates</code>)</li>
 * <li>Where <code>n</code> is the index of an entry in the list of complex fields.</li>
 * </ul>
 * <p>
 * This means that multiple levels of Complex fields are not handled, only the first level. Also, if this class is
 * called from a worker (a <code>UpdateAllDatesWorker</code>), it will update the status of the worker
 *
 * @since 7.2
 */
public class UpdateAllDates {

    private static final Log log = LogFactory.getLog(UpdateAllDates.class);

    public static final int DEFAULT_DOCS_PER_TRANSACTION = 50;

    public static final int DEFAULT_DOCS_PER_PAGE = 500;

    public static final int DEFAULT_LOG_EVENY_N_DOCS = 500;

    protected CoreSession session;

    protected int diffInDays = 0;

    protected long diffInDaysInMs = 0;

    protected long updateDocCount = 0;

    protected long totalUpdatedDocs = 0;

    protected ListenersDisabler listenersDisabler = null;

    protected ArrayList<String> listenersToDisable = null;

    protected boolean wasBlockAsyncHandlers;

    protected boolean wasBlockSyncPostCommitHandlers;

    protected int docsPerTransaction = DEFAULT_DOCS_PER_TRANSACTION;

    protected int docsPerPage = DEFAULT_DOCS_PER_PAGE;

    protected int logEveryNDocs = DEFAULT_LOG_EVENY_N_DOCS;

    protected boolean doLog = true;

    protected AbstractWork worker = null;

    public static void runInWorker(int inDays, ArrayList<String> inListenersToDisable) {

        UpdateAllDatesWorker worker = new UpdateAllDatesWorker(inDays);
        worker.setListenersToDisable(inListenersToDisable);
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(worker, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }

    /**
     * Constructor. Adds <code>inDays</code> to all and every <code>date</code> field.
     * <p>
     * WARNING: In this first version, caller must make sure the update is done with enough rights, the code is not run
     * in an unrestricted session.
     * 
     * @param inSession
     * @param inDays
     */
    public UpdateAllDates(CoreSession inSession, int inDays) {

        session = inSession;
        diffInDays = inDays;
        diffInDaysInMs = diffInDays * 24 * 3600000;
    }

    /**
     * Constructor. Calculates the difference between <code>inLastUpdate</code> and now, and adds the corresponding days
     * to all and every <code>date</code> field.
     * <p>
     * WARNING: In this first version, caller must make sure the update is done with enough rights, the code is not run
     * in an unrestricted session.
     * 
     * @param inSession
     * @param inLastUpdate
     */
    public UpdateAllDates(CoreSession inSession, Date inLastUpdate) {

        log.error("<UpdateAllDates> is Worj In Progress. Should not be used yet.");
        return;

        // session = inSession;
        //
        // long diffInMs = Calendar.getInstance().getTimeInMillis()
        // - inLastUpdate.getTime();
        // diffInDays = (int) TimeUnit.DAYS.convert(diffInMs,
        // TimeUnit.MILLISECONDS);
        // diffInDaysInMs = diffInDays * 24 * 3600000;
        // if (diffInMs < 86400000 || diffInDays < 1) {
        // diffInDays = 0;
        // }
    }

    /**
     * This is the callback for the DocumentsWalker. We perform the update in this callback without having to care about
     * the query, the pagination, ...
     * 
     * @since 7.2
     */
    protected class DocumentsCallbackImpl implements DocumentsCallback {

        long pageCount = 0;

        long documentCount = 0;

        ArrayList<XPathFieldInfo> fieldInfos;

        protected DocumentsCallbackImpl(ArrayList<XPathFieldInfo> inFieldsInfo) {
            fieldInfos = inFieldsInfo;
        }

        @Override
        public ReturnStatus callback(List<DocumentModel> inDocs) {

            updateDocCount = 0;
            updateDocs(inDocs, fieldInfos);

            pageCount += 1;
            documentCount += inDocs.size();

            return ReturnStatus.CONTINUE;
        }

        @Override
        public ReturnStatus callback(DocumentModel inDoc) {

            // We don't use this one, so make sure we don't try to use it in he
            // future
            throw new UnsupportedOperationException();
        }

        @Override
        public void init() {
            // Nothing special here
        }

        @Override
        public void end(ReturnStatus inLastReturnStatusc) {
            // Nothing special here
        }

        public long getPageCount() {
            return pageCount;
        }

        public long getDocumentCount() {
            return documentCount;
        }

    }

    /**
     * Main entry point. Check all document types and their schemas, then update all dates.
     * <p>
     * If <code>inDisableListeners</code> is <code>true</code>, all and every listeners are disabled during the update,
     * and re-enabled after the update (actually, only the listeners that were enabled are re-enabled. If a lister was
     * disabled, it stays disabled)
     * <p>
     * WARNING: In this first version, caller must make sure the update is done with enough rights, the code is not run
     * in an unrestricted session.
     * 
     * @param inDisableListeners
     * @since 7.2
     */
    public void run() {

        if (diffInDays < 1) {
            log.error("Date received is in the future or less than one day: No update done");
            return;
        }

        logIfCanLog("\n--------------------\nIncrease all dates by " + diffInDays + " days\n--------------------");

        disableListeners();

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
                            fieldsInfo.add(new XPathFieldInfo("" + field.getName(), t.isListType(), "date"));
                        } else if (t.isListType()) {

                            // Check if complex-multivalued
                            Type subType = ((ListType) t).getFieldType();
                            if (subType.isComplexType()) {
                                ComplexType ct = (ComplexType) subType;
                                String parentXPath = field.getName().getPrefixedName();
                                Map<String, String[]> subFieldsXPathsAndTypes = ToolsMisc.getComplexFieldSubFieldsInfoPro(
                                        ct, parentXPath);
                                for (String oneXPath : subFieldsXPathsAndTypes.keySet()) {
                                    String[] subInfos = subFieldsXPathsAndTypes.get(oneXPath);
                                    if (subInfos[0].equals("date")) {
                                        XPathFieldInfo xpfi = new XPathFieldInfo(oneXPath, subInfos[1].equals("1"),
                                                "date");
                                        xpfi.setComplexListParentXPath(parentXPath);
                                        fieldsInfo.add(xpfi);
                                    }
                                }
                            }
                        }
                    } else if (t.isComplexType()) {
                        ComplexType ct = (ComplexType) t;
                        Map<String, String[]> subFieldsXPathsAndTypes = ToolsMisc.getComplexFieldSubFieldsInfoPro(ct,
                                field.getName().getPrefixedName());
                        for (String oneXPath : subFieldsXPathsAndTypes.keySet()) {
                            String[] subInfos = subFieldsXPathsAndTypes.get(oneXPath);
                            if (subInfos[0].equals("date")) {
                                fieldsInfo.add(new XPathFieldInfo(oneXPath, subInfos[1].equals("1"), "date"));
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

                logIfCanLog("Update dates for documents of type: " + dt.getName());
                setWorkerStatus("Updating dates for '" + dt.getName() + "'...");

                DocumentsCallbackImpl cb = new DocumentsCallbackImpl(fieldsInfo);
                DocumentsWalker dw = new DocumentsWalker(session, nxql, docsPerPage);
                dw.runForEachPage(cb);

                logIfCanLog("" + cb.getDocumentCount() + " '" + dt.getName() + "' documents updated");
            }
        }

        logIfCanLog("\n--------------------\nAll documents updated\n--------------------");

        restoreListeners();
    }

    protected void disableListeners() {

        if (listenersDisabler == null) {
            listenersDisabler = new ListenersDisabler();
            listenersDisabler.addListener(ListenersDisabler.DUBLINCORELISTENER_NAME);
        }
        if (listenersToDisable != null) {
            for (String name : listenersToDisable) {
                listenersDisabler.addListener(name);
            }
        }

        logIfCanLog("Disabling listeners...");
        listenersDisabler.disableListeners();
        logIfCanLog("Disabled listeners: " + listenersDisabler.getHandledListeners().toString());
    }

    protected void restoreListeners() {

        logIfCanLog("Restoring the listeners...");

        if (listenersDisabler != null) {
            listenersDisabler.restoreListeners();
            listenersDisabler.reset();
            listenersDisabler = null;
        }

        logIfCanLog("Listeners restored.");
    }

    /**
     * Update all date fields whose xpaths are encapsulated in <code>inFieldsInfo</code>, handling all cases: simple
     * field, list fields, simple field in a complex, list field in a complex, simple field in a list of complex, list
     * field in a list of complex.
     * <p>
     * No control/check if the document has the correct schema.
     * 
     * @param inDocs
     * @param inFieldsInfo
     * @since 7.2
     */
    protected void updateDocs(List<DocumentModel> inDocs, ArrayList<XPathFieldInfo> inFieldsInfo) {

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
                String theType = "'" + oneDoc.getType() + "'";
                logIfCanLog("" + updateDocCount + " " + theType + ", (total docs: " + totalUpdatedDocs + ")");

                setWorkerStatus("Updating dates for " + theType + ": " + totalUpdatedDocs + " updated");

            }
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

    }

    /**
     * Simple utility, when same code is used more than once
     * 
     * @param inDoc
     * @param inXPath
     * @since 7.2
     */
    protected void updateListOfDates(DocumentModel inDoc, String inXPath) {
        Calendar[] dates = (Calendar[]) inDoc.getPropertyValue(inXPath);
        if (dates != null && dates.length > 0) {
            for (Calendar c : dates) {
                updateDate(c);
            }
            inDoc.setPropertyValue(inXPath, dates);
        }
    }

    /*
     * We know we handle only dates. No need to check inInfo.getCoreTypeName() Some explanation (auto-remember things
     * too actually ;-)) about multivalued-complex fields: - To get all values, call getPropertyValue(xpath of the
     * complex field) - You then receive an ArrayList of Map<String, Serializable> where the key is the name of the
     * field (not an xpath) - Hence our calls to <code>ArrayList<Map<String, Serializable>> complexValues =
     * (ArrayList<Map<String, Serializable>>) inDoc.getPropertyValue(complexParentXPath);</code>
     */
    @SuppressWarnings("unchecked")
    protected void updateDate(DocumentModel inDoc, XPathFieldInfo inInfo) {

        boolean isInComplexField = inInfo.isInComplexField();
        String complexParentXPath = inInfo.getComplexListParentXPath();
        String subFieldName = inInfo.getSubFieldName();

        if (inInfo.isSimple()) {
            if (isInComplexField) {
                ArrayList<Map<String, Serializable>> complexValues = (ArrayList<Map<String, Serializable>>) inDoc.getPropertyValue(complexParentXPath);
                if (complexValues != null && complexValues.size() > 0) {
                    for (Map<String, Serializable> oneEntry : complexValues) {
                        Calendar c = (Calendar) oneEntry.get(subFieldName);
                        if (c != null) {
                            updateDate(c);
                            oneEntry.put(subFieldName, c);
                        }
                    }
                    inDoc.setPropertyValue(complexParentXPath, complexValues);
                }
            } else {
                Calendar c = (Calendar) inDoc.getPropertyValue(inInfo.getXPath());
                if (c != null) {
                    updateDate(c);
                    inDoc.setPropertyValue(inInfo.getXPath(), c);
                }
            }
        } else if (inInfo.isList()) {
            if (isInComplexField) {
                // We get an array of Complex values, which means an array of
                // Map<String, Serializable> where the String is the name of the
                // field
                ArrayList<Map<String, Serializable>> complexValues = (ArrayList<Map<String, Serializable>>) inDoc.getPropertyValue(complexParentXPath);
                if (complexValues != null) {
                    int length = complexValues.size();
                    for (int i = 0; i < length; i++) {
                        String finalXPath = complexParentXPath + "/" + i + "/" + subFieldName;
                        updateListOfDates(inDoc, finalXPath);
                    }
                }
            } else {
                updateListOfDates(inDoc, inInfo.getXPath());
            }
        }
    }

    protected void updateDate(Calendar c) {

        // This one will have potential problems with daylight saving and can
        // ruin the unit tests. WHile we don't care about daylight saving
        // when updating the dates.
        // c.add(Calendar.DATE, diffInDays);
        c.setTimeInMillis(c.getTimeInMillis() + diffInDaysInMs);
    }

    protected void logIfCanLog(String inWhat) {
        if (doLog) {
            ToolsMisc.forceLogInfo(log, inWhat);
        }
    }

    protected void setWorkerStatus(String inStatus) {

        if (worker != null) {
            worker.setStatus(inStatus);
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
        logEveryNDocs = inNewValue > 0 ? inNewValue : DEFAULT_LOG_EVENY_N_DOCS;
        ;
    }

    public int getDocsPerPage() {
        return docsPerPage;
    }

    public void setDocsPerPage(int inNewValue) {
        docsPerPage = inNewValue > 0 ? inNewValue : DEFAULT_DOCS_PER_PAGE;
    }

    public boolean getDoLog() {
        return doLog;
    }

    public void setDoLog(boolean inValue) {
        doLog = inValue;
    }

    public void setWorker(AbstractWork inWorker) {

        worker = inWorker;
    }

    public void addListenerToDisable(String inName) {

        if (StringUtils.isBlank(inName)) {
            return;
        }

        if (listenersToDisable == null) {
            listenersToDisable = new ArrayList<String>();
        }
        if (!listenersToDisable.contains(inName)) {
            listenersToDisable.add(inName);
        }

    }

}
