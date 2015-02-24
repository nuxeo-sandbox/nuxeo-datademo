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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-datademo" })
public class DocumentsWalkerTest {

    TestUtils testUtils;

    protected DocumentModel parentOfTestDocs;

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
     * Caller must have call testUtils.checkUniqueStrings_Start() before using
     * this class
     */
    protected class DocumentsCallbackImpl implements DocumentsCallback {

        long pageCount = -1; // irrelevant when walking documents one by one

        long documentCount = 0;
        
        long interruptAfter = -1;
        
        ReturnStatus lastReturnStatus;
        
        @Override
        public ReturnStatus callback(List<DocumentModel> inDocs) {

            if (pageCount < 0) {
                pageCount = 0;
            }

            testUtils.checkUniqueStrings_Add(inDocs);

            pageCount += 1;
            documentCount += inDocs.size();

            if(shouldInterrupt()) {
                return ReturnStatus.STOP;
            } else {
                return ReturnStatus.CONTINUE;
            }
        }

        @Override
        public ReturnStatus callback(DocumentModel inDoc) {

            documentCount += 1;

            testUtils.checkUniqueStrings_Add(inDoc.getId());

            if(shouldInterrupt()) {
                return ReturnStatus.STOP;
            } else {
                return ReturnStatus.CONTINUE;
            }
        }
        
        private boolean shouldInterrupt() {
            return interruptAfter > 0 && documentCount >= interruptAfter;
        }

        @Override
        public void init() {
            //Unused here
        }

        @Override
        public void end(ReturnStatus inLastReturnStatus) {
            lastReturnStatus = inLastReturnStatus;
        }
        
        public void setInterruptAfter(long inValue) {
            interruptAfter = inValue;
        }

        public long getPageCount() {
            return pageCount;
        }

        public long getDocumentCount() {
            return documentCount;
        }
        
        public boolean wasInterrupted() {
            return lastReturnStatus == ReturnStatus.STOP;
        }
        
        public boolean wasNotInterrupted() {
            return lastReturnStatus == ReturnStatus.CONTINUE;
        }

    }

    @Test
    public void testDocumentsWalker_byPage() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 22;
        int PAGE_SIZE = 5;
        int EXPECTED_NUMBER_OF_PAGES = 5;

        String nxql = "SELECT * FROM File";

        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            testUtils.createDocument("File", "test-doc-" + i, true);
        }
        coreSession.save();

        DocumentsCallbackImpl cb = new DocumentsCallbackImpl();
        DocumentsWalker dw = new DocumentsWalker(coreSession, nxql, PAGE_SIZE);

        testUtils.checkUniqueStrings_Start();
        dw.runForEachPage(cb);
        testUtils.checkUniqueStrings_Cleanup();
        assertTrue(cb.wasNotInterrupted());
        assertEquals(EXPECTED_NUMBER_OF_PAGES, cb.getPageCount());
        assertEquals(NUMBER_OF_DOCS, cb.getDocumentCount());

        testUtils.endMethod();
    }

    @Test
    public void testDocumentsWalker_byDocument() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 22;
        int PAGE_SIZE = 5;

        String nxql = "SELECT * FROM File";

        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            testUtils.createDocument("File", "test-doc-" + i, true);
        }
        coreSession.save();

        DocumentsCallbackImpl cb = new DocumentsCallbackImpl();
        DocumentsWalker dw = new DocumentsWalker(coreSession, nxql, PAGE_SIZE);

        testUtils.checkUniqueStrings_Start();
        dw.runForEachDocument(cb);
        testUtils.checkUniqueStrings_Cleanup();
        assertTrue(cb.wasNotInterrupted());
        assertEquals(NUMBER_OF_DOCS, cb.getDocumentCount());

        testUtils.endMethod();
    }

    @Test
    public void testDocumentsWalker_byPageWithInterruption() throws Exception {

        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));

        int NUMBER_OF_DOCS = 22;
        int INTERRUPT_AFTER_N_DOCS = 8;
        int PAGE_SIZE = 5;
        int EXPECTED_NUMBER_OF_PAGES = 2;

        String nxql = "SELECT * FROM File";

        for (int i = 1; i <= NUMBER_OF_DOCS; i++) {
            testUtils.createDocument("File", "test-doc-" + i, true);
        }
        coreSession.save();

        DocumentsCallbackImpl cb = new DocumentsCallbackImpl();
        cb.setInterruptAfter(INTERRUPT_AFTER_N_DOCS);
        DocumentsWalker dw = new DocumentsWalker(coreSession, nxql, PAGE_SIZE);

        testUtils.checkUniqueStrings_Start();
        dw.runForEachPage(cb);
        testUtils.checkUniqueStrings_Cleanup();
        assertTrue(cb.wasInterrupted());
        assertEquals(EXPECTED_NUMBER_OF_PAGES, cb.getPageCount());

        testUtils.endMethod();
    }
}
