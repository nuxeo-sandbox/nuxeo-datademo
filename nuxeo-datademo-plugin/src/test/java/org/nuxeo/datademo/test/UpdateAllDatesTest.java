/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.datademo.LifecycleHandler;
import org.nuxeo.datademo.RandomCompanyName;
import org.nuxeo.datademo.RandomDates;
import org.nuxeo.datademo.RandomDublincoreContributors;
import org.nuxeo.datademo.RandomFirstLastNames;
import org.nuxeo.datademo.RandomFirstLastNames.GENDER;
import org.nuxeo.datademo.RandomVocabulary;
import org.nuxeo.datademo.UpdateAllDates;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.datademo.tools.SimpleNXQLDocumentsPageProvider;
import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
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

    protected DateFormat _yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    // DocWithListDates document type, ListOfDates schema and its date_list
    // are declared in doc-type-contrib.xml and test_dates_list.xsd
    protected static final String DOCTYPE_TEST_DOC = "TestDoc";

    protected static final String XPATH_DATES_LIST = "TestSchema:list_of_dates_main";

    protected static final String XPATH_COMPLEX_DATEFIELD = "TestSchema:the_complex/one_date";

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

    @Test
    public void testUpdateAlDates_SimpleField() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 1010;
        int NUMBER_OF_DOCS_TO_CHECK = 10;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        assertTrue(NUMBER_OF_DOCS_TO_CHECK < NUMBER_OF_DOCS);

        // ==========> Create the documents <==========
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        testUtils.doLog("Creating " + NUMBER_OF_DOCS + " 'File'");
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            testUtils.createDocument("File", "doc-" + i, true);
            if ((i % 50) == 0) {
                coreSession.save();
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ==========> Save values for checking <==========
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
        ual.run(true);

        // ==========> Check new dates <==========
        for (String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Calendar c = (Calendar) doc.getPropertyValue("dc:created");
            long ms = c.getTimeInMillis();
            long originalMS = originalIDsAndMS.get(id);

            assertEquals(NUMBER_OF_MILLISECONDS, ms - originalMS);
        }

        testUtils.endMethod();
    }

    @Test
    public void testUpdateAlDates_ListOfDates() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 1010;
        int NUMBER_OF_DOCS_TO_CHECK = 100;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        assertTrue(NUMBER_OF_DOCS_TO_CHECK < NUMBER_OF_DOCS);

        // ==========> Create the documents <==========
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        testUtils.doLog("Creating " + NUMBER_OF_DOCS + " '" + DOCTYPE_TEST_DOC
                + "'");
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-with-datesList-" + i, false);

            int count = ToolsMisc.randomInt(1, 5);
            Calendar[] dates = RandomDates.buildDates(count, null,
                    ToolsMisc.randomInt(4, 10), 5, false);
            doc.setPropertyValue(XPATH_DATES_LIST, dates);

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
        // We also want some null fields
        NUMBER_OF_DOCS += 6;
        for (int i = 1; i < 6; i++) {
            testUtils.createDocument(DOCTYPE_TEST_DOC, "doc-with-datesList-"
                    + i, true);
        }
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ==========> Save values for checking <==========
        // Let's save the first NUMBER_OF_DOCS_TO_CHECK dates
        String nxql = "SELECT * FROM " + DOCTYPE_TEST_DOC;
        DocumentModelList docs = coreSession.query(nxql);
        assertNotNull(docs);
        assertTrue(docs.size() > 0);
        HashMap<String, Long[]> originalIDsAndMS = new HashMap<String, Long[]>();
        for (int i = 0; i < NUMBER_OF_DOCS_TO_CHECK; i++) {
            DocumentModel doc = docs.get(i);
            Calendar[] c = (Calendar[]) doc.getPropertyValue(XPATH_DATES_LIST);
            if (c != null && c.length > 0) {
                Long[] ms = new Long[c.length];
                for (int j = 0; j < c.length; j++) {
                    ms[j] = c[j].getTimeInMillis();
                }
                originalIDsAndMS.put(doc.getId(), ms);
            }
        }

        // ==========> Update all docs <==========
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.run(true);

        // ==========> Check new dates <==========
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

    @Test
    public void testUpdateAlDates_Complex() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 1010;
        int NUMBER_OF_DOCS_TO_CHECK = 100;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        assertTrue(NUMBER_OF_DOCS_TO_CHECK < NUMBER_OF_DOCS);

        // ==========> Create the documents <==========
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        testUtils.doLog("Creating " + NUMBER_OF_DOCS + " '" + DOCTYPE_TEST_DOC
                + "'");
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-complex-simple-date-" + i, false);

            Calendar c = RandomDates.buildDate(null, 4, 10, false);
            doc.setPropertyValue(XPATH_COMPLEX_DATEFIELD, c);

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ==========> Save values for checking <==========
        // Let's save the first NUMBER_OF_DOCS_TO_CHECK dates
        String nxql = "SELECT * FROM " + DOCTYPE_TEST_DOC;
        DocumentModelList docs = coreSession.query(nxql);
        assertNotNull(docs);
        assertTrue(docs.size() > 0);
        HashMap<String, Long> originalIDsAndMS = new HashMap<String, Long>();
        for (int i = 0; i < NUMBER_OF_DOCS_TO_CHECK; i++) {
            DocumentModel doc = docs.get(i);
            Calendar c = (Calendar) doc.getPropertyValue(XPATH_COMPLEX_DATEFIELD);
            if (c != null) {
                originalIDsAndMS.put(doc.getId(), c.getTimeInMillis());
            }
        }

        // ==========> Update all docs <==========
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.run(true);

        // ==========> Check new dates <==========
        for (String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Calendar c = (Calendar) doc.getPropertyValue(XPATH_COMPLEX_DATEFIELD);
            long ms = c.getTimeInMillis();
            long originalMS = originalIDsAndMS.get(id);

            assertEquals(NUMBER_OF_MILLISECONDS, ms - originalMS);
        }

        testUtils.endMethod();

    }
    
    @Test
    public void testUpdateAlDates_ListOfComplexWithDates() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 50;
        int NUMBER_OF_DOCS_TO_CHECK = 10;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;

        assertTrue(NUMBER_OF_DOCS_TO_CHECK < NUMBER_OF_DOCS);

        // ==========> Create the documents <==========
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        testUtils.doLog("Creating " + NUMBER_OF_DOCS + " '" + DOCTYPE_TEST_DOC
                + "'");
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                    "doc-complex-list-" + i, false);
            
            // No random here, we always create 3 complex
            for(int j = 0; j < 3; j++) {
                Property complexMeta = doc.getProperty("TestSchema:the_complex_multivalued");
                ListType ltype = (ListType) complexMeta.getField().getType();
                //assertTrue(ltype.getFieldType().isComplexType());
                HashMap<String, Serializable> oneEntry = new HashMap<String, Serializable>();
                Calendar c = RandomDates.buildDate(null, 4, 10, false);
                oneEntry.put("one_date_2", c);
                complexMeta.addValue(oneEntry);
            }

            doc = coreSession.saveDocument(doc);
            if ((i % 50) == 0) {
                coreSession.save();
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
            
        }
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        
        DocumentModelList docs = coreSession.query("SELECT * FROM Document WHERE TestSchema:the_complex_multivalued/*/one_date_2 IS NOT NULL");
        System.out.println(docs.size());

        testUtils.endMethod();
        
    }

    /*
     * COMPLEX AND MULTIVALUED: TestSchema:the_complex_multivalued
     * TestSchema:the_complex_multivalued/list_of_dates_2: date
     * TestSchema:the_complex_multivalued/one_date_2: date
     * 
     * COMPLEX: TestSchema:the_complex
     * 
     * TestSchema:the_complex/one_date: date
     * TestSchema:the_complex/list_of_dates: date
     * 
     * TestSchema:list_of_dates_main
     */

    @Ignore
    @Test
    public void quickTests() throws Exception {

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        DocumentModel doc = testUtils.createDocument(DOCTYPE_TEST_DOC,
                "doc-complex-simple-date-" + 8989, true);
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // String nxql =
        // "SELECT * FROM Document WHERE TestSchema:the_complex/list_of_dates/0 IS NULL";
        String nxql = "SELECT * FROM " + DOCTYPE_TEST_DOC;
        DocumentModelList docs = coreSession.query(nxql);
        DocumentModel zedoc = docs.get(0);

        // Marche pas: "TestSchema:the_complex_multivalued/0/list_of_dates_2";
        // Marche pas: "TestSchema:the_complex_multivalued/0/list_of_dates_2/0";
        String zez = "TestSchema:the_complex_multivalued";
        Object o = zedoc.getPropertyValue(zez);

        if (docs == null || (docs != null && docs.size() != -2343)) {
            return;
        }
        // ==============================================================

        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        Schema schema = sm.getSchema("TestSchema");
        for (Field field : schema.getFields()) {
            Type t = field.getType();

            testUtils.doLog("Field: " + t.getName());
            testUtils.doLog("Type: " + ToolsMisc.getCoreFieldType(t));
            if (t.getName() != "dldkljdl") {
                continue;
            }
            testUtils.doLog("DOIT PAS ETRE LA");

            if (t.isSimpleType()) {
                // testUtils.doLog("Simple type: " + t.getName());
            } else if (t.isListType()) {
                testUtils.doLog("List type name: " + t.getName());
                ListType lt = (ListType) t;
                Type tt = lt.getFieldType();
                testUtils.doLog(tt.getName());
                if (tt.isComplexType()) {
                    testUtils.doLog("Complex");
                }

            } else if (t.isComplexType()) {
                testUtils.doLog("Complex type: " + t.getName());
                ComplexType ct = (ComplexType) t;
                Collection<Field> subfields = ct.getFields();
                String xpath = ct.getName();
                for (Field subF : subfields) {
                    testUtils.doLog("" + subF.getName());
                    Type subType = subF.getType();
                    testUtils.doLog(subType.getName());
                    testUtils.doLog("getCoreFieldType: "
                            + ToolsMisc.getCoreFieldType(subType));
                }
            } else {
                testUtils.doLog("?????");
            }
        }
    }
}
