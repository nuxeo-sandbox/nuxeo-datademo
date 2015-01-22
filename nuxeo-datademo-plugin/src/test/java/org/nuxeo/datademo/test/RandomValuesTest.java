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
import java.util.Calendar;
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
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-datademo" })
public class RandomValuesTest {

    public static long MS_IN_DAY = 24 * 3600000;

    protected DocumentModel parentOfTestDocs;

    @Inject
    CoreSession coreSession;

    protected void doLog(String what) {
        System.out.println(what);
    }

    // Not sure it's the best way to get the current method name, but at least
    // it works
    protected String getCurrentMethodName(RuntimeException e) {
        StackTraceElement currentElement = e.getStackTrace()[0];
        return currentElement.getMethodName();
    }

    protected DocumentModel createDocument(String inType, String inTitle,
            boolean inSave) {

        DocumentModel doc = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), inTitle, inType);
        doc.setPropertyValue("dc:title", inTitle);
        doc = coreSession.createDocument(doc);
        if (inSave) {
            doc = coreSession.saveDocument(doc);
        }

        return doc;
    }

    protected DocumentModel createDocument(String inType, String inTitle) {
        return createDocument(inType, inTitle, false);
    }

    @Before
    public void setUp() {

        parentOfTestDocs = coreSession.createDocumentModel("/",
                "test-random-data", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-random-data");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);
        parentOfTestDocs = coreSession.saveDocument(parentOfTestDocs);

    }

    @After
    public void cleanup() {
        coreSession.removeDocument(parentOfTestDocs.getRef());
        
        coreSession.removeChildren(new PathRef("/"));

        coreSession.save();
    }

    @Test
    public void testFirstLastName() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        RandomFirstLastNames rfln;

        rfln = RandomFirstLastNames.getInstance();

        String value = rfln.getAFirstName(GENDER.MALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        value = rfln.getAFirstName(GENDER.FEMALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        value = rfln.getAFirstName(GENDER.ANY);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        value = rfln.getALastName();
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        RandomFirstLastNames.release();

    }

    @Test
    public void testFirstLastNameSingleton() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        RandomFirstLastNames r1, r2, r3;

        // should be in different threads, but whatever.
        // Get 3 instances
        r1 = RandomFirstLastNames.getInstance();
        r2 = RandomFirstLastNames.getInstance();
        r3 = RandomFirstLastNames.getInstance();
        assertEquals(3, RandomFirstLastNames.getUsageCount());

        // Get a value
        String value = r1.getAFirstName(GENDER.MALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        // Say thread using r1 calls release() => the singleton is not really
        // released
        RandomFirstLastNames.release();
        assertEquals(2, RandomFirstLastNames.getUsageCount());
        // "Thread" 2 can get a value
        value = r2.getALastName();
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        // "Thread" 2 calls release()
        RandomFirstLastNames.release();
        assertEquals(1, RandomFirstLastNames.getUsageCount());
        // "Thread" 3 can get a value
        value = r3.getAFirstName(GENDER.FEMALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        // "Thread" 3 calls release()
        RandomFirstLastNames.release();
        assertEquals(0, RandomFirstLastNames.getUsageCount());

        // Now, we must have an error
        try {
            value = r3.getALastName();
        } catch (Exception e) {
            assertEquals("NullPointerException", e.getClass().getSimpleName());
        }
    }

    @Test
    public void testCompanyName() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        RandomCompanyName rcn = RandomCompanyName.getInstance();

        String value = rcn.getAName(0);
        assertEquals(3, value.split(" ").length);

        value = rcn.getAName(1);
        assertEquals(1, value.split(" ").length);

        value = rcn.getAName(2);
        assertEquals(value, 2, value.split(" ").length);

        RandomCompanyName.release();
    }

    protected boolean sameYMD(GregorianCalendar inD1, GregorianCalendar inD2) {

        return inD1.get(Calendar.YEAR) == inD2.get(Calendar.YEAR)
                && inD1.get(Calendar.MONTH) == inD2.get(Calendar.MONTH)
                && inD1.get(Calendar.DATE) == inD2.get(Calendar.DATE);
    }

    protected boolean d2IsInNDays(GregorianCalendar inD1,
            GregorianCalendar inD2, int inDays) {

        return ((inD2.getTimeInMillis() - inD1.getTimeInMillis()) / MS_IN_DAY) == inDays;

    }

    /*
     * WARNING: Even if test runs fast, maybe that exactly around midnight,
     * Comparison with "today and now" may fail.
     * 
     * You have been warned.
     */
    @Test
    public void testRandomDates() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        Calendar d;
        long diff;
        Calendar now = Calendar.getInstance();

        d = RandomDates.addDays(now, 3);
        assertTrue(d2IsInNDays((GregorianCalendar) now, (GregorianCalendar) d,
                3));

        // maxIsToday true and + 3 days => should stays to today
        d = RandomDates.addDays(now, 3, true);
        assertTrue(sameYMD((GregorianCalendar) now, (GregorianCalendar) d));

        d = RandomDates.buildDate(null, 10, 90, true);
        diff = now.getTimeInMillis() - d.getTimeInMillis();
        assertTrue(diff >= (10 * MS_IN_DAY));
        assertTrue(diff <= (90 * MS_IN_DAY));

        d = RandomDates.buildDate(null, 10, 90, false);
        diff = d.getTimeInMillis() - now.getTimeInMillis();
        assertTrue(diff >= (10 * MS_IN_DAY));
        assertTrue(diff <= (90 * MS_IN_DAY));
    }

    @Test
    public void testRandomDublincore() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        String[] users = { "Administrator", "jim", "john", "kate", "alan",
                "rob", "julie" };

        DocumentModel doc;
        String[] contributors;

        doc = createDocument("File", "testRandomDublincore", true);

        // All the users in users
        doc = RandomDublincoreContributors.setContributors(doc, users);
        contributors = (String[]) doc.getPropertyValue("dc:contributors");
        boolean missing = false;
        for (String s : users) {
            boolean found = false;
            for (String c : contributors) {
                if (c.equals(s)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing = true;
                break;
            }
        }
        assertFalse(missing);

        // Exactly 3 users
        doc = RandomDublincoreContributors.setContributors(doc, users, 3);
        contributors = (String[]) doc.getPropertyValue("dc:contributors");
        assertEquals(3, contributors.length);

        // Between 3 and 5 users
        doc = RandomDublincoreContributors.setContributors(doc, users, 3, 5);
        contributors = (String[]) doc.getPropertyValue("dc:contributors");
        assertTrue(contributors.length >= 3);
        assertTrue(contributors.length <= 5);
    }

    @Test
    public void testLifecycle() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        String[] lcs = { "project", "approved" };
        String[] lct = { "approve" };

        LifecycleHandler lch = new LifecycleHandler(lcs, lct);

        DocumentModel doc;

        doc = createDocument("File", "test-moveToRandomState", true);
        doc = lch.moveToRandomState(doc);
        assertNotEquals("project", doc.getCurrentLifeCycleState());

        doc = createDocument("File", "test-moveToNextRandomState", true);
        doc = LifecycleHandler.moveToNextRandomState(doc, true);
        assertNotEquals("project", doc.getCurrentLifeCycleState());

    }

    @Ignore
    @Test
    public void testRandomVocabulary() throws Exception {
        RandomVocabulary voc = new RandomVocabulary("country");
        System.out.println(voc.size());
    }
    

    @Test
    public void hop() throws Exception {

        String nxql = "SELECT * FROM File";

        for (int i = 1; i <= 22; i++) {
            createDocument("File", "test-hop-" + i, true);
        }
        coreSession.save();

        CoreQueryDocumentPageProvider cqpp = new CoreQueryDocumentPageProvider();
        CoreQueryPageProviderDescriptor ppDesc = new CoreQueryPageProviderDescriptor();
        ppDesc.setPattern(nxql);
        cqpp.setDefinition(ppDesc);

        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) coreSession);
        cqpp.setProperties(props);
        //cqpp.setMaxPageSize(5);
        cqpp.setPageSize(5);

        List<DocumentModel> docs = cqpp.getCurrentPage();
        while(docs != null && docs.size() > 0) {
            
            System.out.println("Docs: " + docs.size());
            String ids = "";
            for(DocumentModel d : docs) {
                ids += "    " + d.getId() + "\n";
            }
            System.out.println(ids);
            System.out.println("Page: " + cqpp.getCurrentPageIndex());
            
            if(cqpp.isNextPageAvailable()) {
                cqpp.nextPage();
                docs = cqpp.getCurrentPage();
            } else {
                docs = null;
            }
        }


    }
}
