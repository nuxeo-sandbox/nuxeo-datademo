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
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
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
@Features({ PlatformFeature.class, TransactionalFeature.class,
        CoreFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-datademo" })
public class UpdateAllDatesTest {

    TestUtils testUtils;

    protected DocumentModel parentOfTestDocs;

    protected DateFormat _yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

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
    public void testUpdateAlDates() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 5000;
        int NUMBER_OF_DOCS_TO_CHECK = 10;
        int NUMBER_OF_DAYS = 4;
        long NUMBER_OF_MILLISECONDS = NUMBER_OF_DAYS * 24 * 3600000;
        
        assertTrue(NUMBER_OF_DOCS_TO_CHECK < NUMBER_OF_DOCS);
        
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        testUtils.doLog("Creating " + NUMBER_OF_DOCS + " 'File'");
        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            testUtils.createDocument("File", "doc-" + i, true);
            if ((i % 50) == 0) {
                coreSession.save();
            }
        }
        coreSession.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Let's save the first 10 dates
        String nxql = "SELECT * FROM File";
        DocumentModelList docs = coreSession.query(nxql);
        System.out.println("docs => " + docs.size());
        HashMap<String, Long> originalIDsAndMS = new HashMap<String, Long>();
        for (int i = 0; i < NUMBER_OF_DOCS_TO_CHECK; i++) {
            DocumentModel doc = docs.get(i);
            Calendar c = (Calendar) doc.getPropertyValue("dc:created");
            originalIDsAndMS.put(doc.getId(), c.getTimeInMillis());
        }

        // Update all docs
        UpdateAllDates ual = new UpdateAllDates(coreSession, NUMBER_OF_DAYS);
        ual.run(true);
        // Check
        for(String id : originalIDsAndMS.keySet()) {
            DocumentModel doc = coreSession.getDocument(new IdRef(id));
            Calendar c = (Calendar) doc.getPropertyValue("dc:created");
            long ms = c.getTimeInMillis();
            long originalMS = originalIDsAndMS.get(id);
            
            assertEquals(NUMBER_OF_MILLISECONDS, ms - originalMS);
        }

        testUtils.endMethod();
    }
}
