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
 *     thibaud
 */
package org.nuxeo.datademo.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.datademo.tools.ListenersDisabler;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * 
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class })
@Deploy({ "nuxeo-datademo" })
public class TestListeneresDisabler {

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
    
    @Test
    public void testDisableDubincore() throws Exception {
        
        testUtils.startMethod(testUtils.getCurrentMethodName(new RuntimeException()));
        
        DocumentModel doc = testUtils.createDocument("File", "listeners-enabled", true);
        assertEquals("listeners-enabled", (String) doc.getPropertyValue("dc:title"));
        assertNotNull(doc.getPropertyValue("dc:creator"));
        assertNotNull(doc.getPropertyValue("dc:created"));
        assertNotNull(doc.getPropertyValue("dc:modified"));
        
        ListenersDisabler ld = new ListenersDisabler();
        ld.addListener(ListenersDisabler.DUBLINCORELISTENER_NAME);
        
        ld.disableListeners();
        doc = testUtils.createDocument("File", "dublincore-disabled", true);
        ld.restoreListeners();
        
        assertEquals("dublincore-disabled", (String) doc.getPropertyValue("dc:title"));
        assertNull(doc.getPropertyValue("dc:creator"));
        assertNull(doc.getPropertyValue("dc:created"));
        assertNull(doc.getPropertyValue("dc:modified"));
        
        testUtils.endMethod();
    }

}
