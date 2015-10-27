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

package org.nuxeo.datademo.test;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.datademo.RandomDates;
import org.nuxeo.datademo.UpdateAllDates;
import org.nuxeo.datademo.UpdateAllDatesWorker;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
// Using transactions in this test.
@Features({ PlatformFeature.class, TransactionalFeature.class,
        CoreFeature.class, EmbeddedAutomationServerFeature.class })
// We deploy org.nuxeo.datademo.test which contains the DocTypes, list dates,
// etc. we need
@Deploy({ "nuxeo-datademo", "org.nuxeo.datademo.test" })
public class UpdateAllDatesTest {

    TestUtils testUtils;

    protected DocumentModel parentOfTestDocs;

    protected DateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    // DocWithListDates document type, ListOfDates schema and its date_list
    // are declared in doc-type-contrib.xml and test_dates_list.xsd
    protected static final String DOCTYPE_TEST_DOC = "TestDoc";

    // Fields are declared in /schemas/TestSchema.xsd
    protected static final String XPATH_DATES_LIST = "TestSchema:list_of_dates_main";

    protected static final String XPATH_COMPLEX_DATEFIELD_SIMPLE = "TestSchema:the_complex/one_date";

    protected static final String XPATH_COMPLEX_DATEFIELD_LIST = "TestSchema:the_complex/list_of_dates";

    protected static final String XPATH_COMPLEX_MULTIVALUED = "TestSchema:the_complex_multivalued";

    protected static final String COMPLEX_MULTIVALUED_SIMPLE_DATE_FIELD = "one_date_2";

    protected static final String COMPLEX_MULTIVALUED_DATELIST_FIELD = "list_of_dates_2";

    @Inject
    CoreSession coreSession;

    @Before
    public void setUp() {

        if (testUtils == null) {
            testUtils = new TestUtils(coreSession);
        }

        parentOfTestDocs = coreSession.createDocumentModel("/",
                "test-random-data", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-random-data");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);
        parentOfTestDocs = coreSession.saveDocument(parentOfTestDocs);

        coreSession.save();

        testUtils.setParentFolder(parentOfTestDocs);
    }

    @After
    public void cleanup() {

        coreSession.removeDocument(parentOfTestDocs.getRef());
        coreSession.save();
    }

    /*
     * In this test, we create a lot of documents, so we can also test how
     * UpdateAllDates behaves with pagination and so on.
     * 
     * In other tests, we will just create a few documents. So, make sure this
     * specific tests is not removed or @Ignore, or whatever makes it not run.
     */
    @Test
    public void testUpdateAllDates_SimpleField() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        // + 3 to make sure we'll have ate least one not-full page in the query
        // made by the UpdateAllDates object
        int NUMBER_OF_DOCS = 2003;
        int NUMBER_OF_DOCS_TO_CHECK = 100;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        assertTrue(NUMBER_OF_DOCS_TO_CHECK < NUMBER_OF_DOCS);
        
        coreSession.removeChildren(parentOfTestDocs.getRef());
        coreSession.save();

        // ==========> Create the documents
        testUtils.doLog("Creating " + NUMBER_OF_DOCS + " 'File'");
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            testUtils.createDocument("File", "doc-date-simplefield-" + i, true);
            if ((i % 50) == 0) {
                coreSession.save();
            }
        }
        coreSession.save();

        // ==========> Store values for checking after update
        // Let's save the first NUMBER_OF_DOCS_TO_CHECK dates
        String nxql = "SELECT * FROM File";
        DocumentModelList docs = coreSession.query(nxql);
        assertNotNull(docs);
        assertTrue(docs.size() > 0);
        HashMap<String, Long> originalIDsAndMS = new HashMap<String, Long>();
        for (int i = 0; i < NUMBER_OF_DOCS_TO_CHECK; i++) {
            DocumentModel doc = docs.get(i);
            Calendar c = (Calendar) doc.getPropertyValue("dc:created");
            originalIDsAndMS.put(doc.getId(), c.getTimeInMillis());
        }

        // ==========> Update all docs <==========
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        //ual.setDoLog(false);
        ual.run();

        // ==========> Check dates have changed
        for (String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Calendar c = (Calendar) doc.getPropertyValue("dc:created");
            long ms = c.getTimeInMillis();
            long originalMS = originalIDsAndMS.get(id);

            assertEquals(NUMBER_OF_MILLISECONDS, ms - originalMS);
        }

        testUtils.endMethod();
    }

    /*
     * See comments in testUpdateAlDates_SimpleField. here, we create/check only
     * few documents.
     */
    @Test
    public void testUpdateAllDates_ListOfDates() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 20;
        int NUMBER_OF_DAYS = 4;
        int NUMBER_OF_DATES_PER_FIELD = 3;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;
        
        coreSession.removeChildren(parentOfTestDocs.getRef());
        coreSession.save();

        // ==========> Create documents. Store values for checking after update.
        HashMap<String, Long[]> originalIDsAndMS = new HashMap<String, Long[]>();
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-with-datesList-" + i, false);

            Calendar[] dates = RandomDates.buildDates(
                    NUMBER_OF_DATES_PER_FIELD, null, 4, 10, false);
            doc.setPropertyValue(XPATH_DATES_LIST, dates);

            Long[] ms = new Long[NUMBER_OF_DATES_PER_FIELD];
            for (int j = 0; j < NUMBER_OF_DATES_PER_FIELD; j++) {
                ms[j] = dates[j].getTimeInMillis();
            }
            originalIDsAndMS.put(doc.getId(), ms);

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
            }
        }
        // We also want some null fields to make sure UpdateAllDates does not
        // fail on null values and just ignore them
        NUMBER_OF_DOCS += 6;
        for (int i = 1; i < 6; i++) {
            testUtils.createDocument(DOCTYPE_TEST_DOC, "doc-with-datesList-"
                    + i, true);
        }
        coreSession.save();

        // ==========> Update all docs
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.setDoLog(false);
        ual.run();

        // ==========> Check dates have changed
        for (String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Calendar[] c = (Calendar[]) doc.getPropertyValue(XPATH_DATES_LIST);
            Long[] originalMS = originalIDsAndMS.get(id);
            // We did not save null values in originalIDsAndMS
            assertNotNull(c);
            assertNotNull(originalMS);

            int length = c.length;
            assertEquals(length, originalMS.length);

            for (int i = 0; i < length; i++) {                
                long diff = c[i].getTimeInMillis() - originalMS[i].longValue();
                assertEquals(NUMBER_OF_MILLISECONDS, diff);
            }
        }

        testUtils.endMethod();

    }

    /*
     * See comments in testUpdateAlDates_SimpleField. here, we create/check only
     * few documents.
     */
    @Test
    public void testUpdateAllDates_SimpleFieldInComplex() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 20;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        // ==========> Create documents. Store values for checking after update.
        HashMap<String, Long> originalIDsAndMS = new HashMap<String, Long>();
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-complex-simple-date-" + i, false);

            Calendar c = RandomDates.buildDate(null, 4, 10, false);
            doc.setPropertyValue(XPATH_COMPLEX_DATEFIELD_SIMPLE, c);

            originalIDsAndMS.put(doc.getId(), c.getTimeInMillis());

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
            }
        }
        coreSession.save();

        // ==========> Update all docs
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.setDoLog(false);
        ual.run();

        // ==========> Check dates have changed
        for (String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Calendar c = (Calendar) doc.getPropertyValue(XPATH_COMPLEX_DATEFIELD_SIMPLE);
            long ms = c.getTimeInMillis();
            long originalMS = originalIDsAndMS.get(id);

            assertEquals(NUMBER_OF_MILLISECONDS, ms - originalMS);
        }

        testUtils.endMethod();

    }

    /*
     * See comments in testUpdateAlDates_SimpleField. here, we create/check only
     * few documents.
     * 
     * The @SuppressWarnings annotation is for the call to:
     * 
     * ArrayList<Map<String, Serializable>> values = (ArrayList<Map<String,
     * Serializable>>) doc.getPropertyValue(
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateAllDates_SimpleFieldInListOfComplex()
            throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 20;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        // No random here, we always create 3 complex
        int NUMBER_OF_COMPLEX_ENTRIES = 3;

        HashMap<String, Long[]> originalIDsAndMS = new HashMap<String, Long[]>();

        // ==========> Create documents. Store values for checking after update.
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-complex-list-and-simple-date-" + i, false);

            // Setup the complex-multivalued property. This is done passing an
            // Array of Map<String, Serializable>. Each entry of the array is a
            // hashmap where where the key if the name of
            // the field (short name, no prefix)
            //
            // Notice there is no need to setPropertyValue() with the list of
            // complex, the values are saved because we are using a Property
            // (the complexMeta variable) to which we add a value:
            // complexMeta.addValue(oneEntry).
            Long[] ms = new Long[NUMBER_OF_COMPLEX_ENTRIES];
            for (int j = 0; j < NUMBER_OF_COMPLEX_ENTRIES; j++) {
                Property complexMeta = doc.getProperty(XPATH_COMPLEX_MULTIVALUED);
                HashMap<String, Serializable> oneEntry = new HashMap<String, Serializable>();
                Calendar c = RandomDates.buildDate(null, 4, 10, false);
                ms[j] = c.getTimeInMillis();
                oneEntry.put(COMPLEX_MULTIVALUED_SIMPLE_DATE_FIELD, c);
                complexMeta.addValue(oneEntry);
            }
            originalIDsAndMS.put(doc.getId(), ms);

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
            }

        }
        coreSession.save();

        // DocumentModelList docs =
        // coreSession.query("SELECT * FROM Document WHERE TestSchema:the_complex_multivalued/*/one_date_2 IS NOT NULL");
        // System.out.println(docs.size());

        // ==========> Update all docs
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.setDoLog(false);
        ual.run();

        // ==========> Check dates have changed
        for (String id : originalIDsAndMS.keySet()) {
            Long[] originalMS = originalIDsAndMS.get(id);
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            // We get an array of Complex values, which means an array of
            // Map<String, Serializable> where the String is the name of the
            // field
            ArrayList<Map<String, Serializable>> values = (ArrayList<Map<String, Serializable>>) doc.getPropertyValue(XPATH_COMPLEX_MULTIVALUED);
            assertNotNull(values);
            assertEquals(NUMBER_OF_COMPLEX_ENTRIES, values.size());
            for (int i = 0; i < NUMBER_OF_COMPLEX_ENTRIES; i++) {
                Map<String, Serializable> oneEntry = values.get(i);
                Calendar c = (Calendar) oneEntry.get(COMPLEX_MULTIVALUED_SIMPLE_DATE_FIELD);
                assertNotNull(c);
                assertEquals(NUMBER_OF_MILLISECONDS, c.getTimeInMillis()
                        - originalMS[i]);
            }
        }

        testUtils.endMethod();

    }

    @Test
    public void testUpdateAllDates_ListOfDatesInComplex() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 20;
        int NUMBER_OF_DAYS = 4;
        int NUMBER_OF_DATES_PER_FIELD = 3;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        // ==========> Create documents. Store values for checking after update.
        HashMap<String, Long[]> originalIDsAndMS = new HashMap<String, Long[]>();
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-complex-list-and-list-of-date-" + i, false);

            Calendar[] dates = RandomDates.buildDates(
                    NUMBER_OF_DATES_PER_FIELD, null, 4, 10, false);
            doc.setPropertyValue(XPATH_COMPLEX_DATEFIELD_LIST, dates);

            Long[] ms = new Long[NUMBER_OF_DATES_PER_FIELD];
            for (int j = 0; j < NUMBER_OF_DATES_PER_FIELD; j++) {
                ms[j] = dates[j].getTimeInMillis();
            }
            originalIDsAndMS.put(doc.getId(), ms);

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
            }
        }
        coreSession.save();

        // ==========> Update all docs
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.setDoLog(false);
        ual.run();

        // ==========> Check dates have changed
        for (String id : originalIDsAndMS.keySet()) {
            Long[] originalMS = originalIDsAndMS.get(id);
            DocumentModel doc = coreSession.getDocument(new IdRef(id));

            Calendar[] ms = (Calendar[]) doc.getPropertyValue(XPATH_COMPLEX_DATEFIELD_LIST);
            assertEquals(NUMBER_OF_DATES_PER_FIELD, ms.length);
            for (int i = 0; i < ms.length; i++) {
                assertEquals(NUMBER_OF_MILLISECONDS, ms[i].getTimeInMillis()
                        - originalMS[i]);
            }
        }

        testUtils.endMethod();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateAllDates_ListOfDatesInListOfComplex()
            throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 20;
        int NUMBER_OF_DAYS = 4;
        int NUMBER_OF_DATES_PER_FIELD = 3;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        // No random here, we always create 3 complex
        int NUMBER_OF_COMPLEX_ENTRIES = 3;

        HashMap<String, Long[]> originalIDsAndMS = new HashMap<String, Long[]>();

        // ==========> Create documents. Store values for checking after update.
        // Because here, we have 3 dates x 3 complex fields => 9 dates/document
        // we actually save the sum of the milliseconds for each list of dates.
        // Jut to make it a bit simpler.
        long EXPECTED_DIFF_IN_MS = NUMBER_OF_MILLISECONDS * NUMBER_OF_DATES_PER_FIELD;
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-complex-list-and-simple-date-" + i, false);

            // Setup the complex-multivalued property. This is done passing an
            // Array of Map<String, Serializable>. Each entry of the array is a
            // hashmap where where the key if the name of
            // the field (short name, no prefix)
            //
            // Notice there is no need to setPropertyValue() with the list of
            // complex, the values are saved because we are using a Property
            // (the complexMeta variable) to which we add a value:
            // complexMeta.addValue(oneEntry).
            Long[] ms = new Long[NUMBER_OF_COMPLEX_ENTRIES];
            for (int j = 0; j < NUMBER_OF_COMPLEX_ENTRIES; j++) {
                // Setup the field
                Property complexMeta = doc.getProperty(XPATH_COMPLEX_MULTIVALUED);
                HashMap<String, Serializable> oneEntry = new HashMap<String, Serializable>();
                Calendar[] dates = RandomDates.buildDates(
                        NUMBER_OF_DATES_PER_FIELD, null, 4, 10, false);
                oneEntry.put(COMPLEX_MULTIVALUED_DATELIST_FIELD, dates);
                complexMeta.addValue(oneEntry);

                // Setup the values
                long totalMS = 0;
                for (Calendar oneC : dates) {
                    totalMS += oneC.getTimeInMillis();
                }
                ms[j] = totalMS;
            }
            originalIDsAndMS.put(doc.getId(), ms);

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
            }

        }
        coreSession.save();

        // DocumentModelList docs =
        // coreSession.query("SELECT * FROM Document WHERE TestSchema:the_complex_multivalued/*/one_date_2 IS NOT NULL");
        // System.out.println(docs.size());

        // ==========> Update all docs
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.setDoLog(false);
        ual.run();

        // ==========> Check dates have changed
        for (String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Long[] originalMS = originalIDsAndMS.get(id);
            // Get the multivalued-complex field
            ArrayList<Map<String, Serializable>> values = (ArrayList<Map<String, Serializable>>) doc.getPropertyValue(XPATH_COMPLEX_MULTIVALUED);
            assertNotNull(values);
            assertEquals(NUMBER_OF_COMPLEX_ENTRIES, values.size());

            for (int i = 0; i < NUMBER_OF_COMPLEX_ENTRIES; i++) {
                Map<String, Serializable> oneEntry = values.get(i);
                Calendar[] dates = (Calendar[]) oneEntry.get(COMPLEX_MULTIVALUED_DATELIST_FIELD);
                assertNotNull(dates);
                assertEquals(NUMBER_OF_DATES_PER_FIELD, dates.length);
                
                // Get the sum (we stored the sum)
                long totalMS = 0;
                for (Calendar oneC : dates) {
                    totalMS += oneC.getTimeInMillis();
                }
                // Now, check. At least :->
                assertEquals(EXPECTED_DIFF_IN_MS, totalMS - originalMS[i]);
            }
            
        }

        testUtils.endMethod();
    }
    
    @Ignore
    public void testUpdateAllDates_SimpleField_worker() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        // + 3 to make sure we'll have ate least one not-full page in the query
        // made by the UpdateAllDates object
        int NUMBER_OF_DOCS = 2003;
        int NUMBER_OF_DOCS_TO_CHECK = 100;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        assertTrue(NUMBER_OF_DOCS_TO_CHECK < NUMBER_OF_DOCS);
        
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ==========> Create the documents
        testUtils.doLog("Creating " + NUMBER_OF_DOCS + " 'File'");
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            testUtils.createDocument("File", "doc-date-simplefield-" + i, true);
            if ((i % 50) == 0) {
                coreSession.save();
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ==========> Store values for checking after update
        // Let's save the first NUMBER_OF_DOCS_TO_CHECK dates
        String nxql = "SELECT * FROM File";
        DocumentModelList docs = coreSession.query(nxql);
        assertNotNull(docs);
        assertTrue(docs.size() > 0);
        HashMap<String, Long> originalIDsAndMS = new HashMap<String, Long>();
        for (int i = 0; i < NUMBER_OF_DOCS_TO_CHECK; i++) {
            DocumentModel doc = docs.get(i);
            Calendar c = (Calendar) doc.getPropertyValue("dc:created");
            originalIDsAndMS.put(doc.getId(), c.getTimeInMillis());
        }

        // ==========> Update all docs <==========
        testUtils.doLog("Launching the worker");
        UpdateAllDatesWorker worker = new UpdateAllDatesWorker((int) NUMBER_OF_DAYS);
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(worker, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);

        // Wait...
        testUtils.doLog("Wait until worker done");
        /*
        boolean workDone = workManager.awaitCompletion(queueId, 10, TimeUnit.SECONDS);
        if(!workDone) {
            testUtils.doLog("Should not last that long...");
        }
        */
        
        int count = 0;
        boolean doContinue = true;
        do {
            Thread.sleep(100);
            count += 1;
            // Wait max 10s
            if(count > 100) {
                testUtils.doLog("Should not last that long...");
                doContinue = false;
            } else {
                doContinue = !worker.getStatus().equals(UpdateAllDatesWorker.UPDATE_ALL_DATES_DONE_STATUS);
            }
        } while (doContinue);
        testUtils.doLog("Worker is done => Checking results");

        // ==========> Check dates have changed
        for (String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Calendar c = (Calendar) doc.getPropertyValue("dc:created");
            long ms = c.getTimeInMillis();
            long originalMS = originalIDsAndMS.get(id);

            assertEquals(NUMBER_OF_MILLISECONDS, ms - originalMS);
        }

        testUtils.endMethod();
    }
}
