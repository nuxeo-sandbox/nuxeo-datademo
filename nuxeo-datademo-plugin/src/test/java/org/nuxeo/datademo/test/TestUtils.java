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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * 
 *
 * @since 7.2
 */
public class TestUtils {
    
    CoreSession session;
    
    DocumentModel parentFolder;
    
    String currentMethodName;
    
    public TestUtils(CoreSession inSession) {
        session = inSession;
    }
    
    public void setParentFolder(DocumentModel inParent) {
        parentFolder = inParent;
    }
    
    public void doLog(String what) {
        System.out.println(what);
    }

    // Not sure it's the best way to get the current method name, but at least
    // it works
    public String getCurrentMethodName(RuntimeException e) {
        StackTraceElement currentElement = e.getStackTrace()[0];
        return currentElement.getMethodName();
    }


    public DocumentModel createDocument(String inType, String inTitle,
            boolean inSave) {
        
        if(parentFolder == null) {
            throw new RuntimeException("parentFolder is not defined");
        }

        DocumentModel doc = session.createDocumentModel(
                parentFolder.getPathAsString(), inTitle, inType);
        doc.setPropertyValue("dc:title", inTitle);
        doc = session.createDocument(doc);
        if (inSave) {
            doc = session.saveDocument(doc);
        }

        return doc;
    }

    public DocumentModel createDocument(String inType, String inTitle) {
        return createDocument(inType, inTitle, false);
    }
    
    public void startMethod(String inName) {
        currentMethodName = inName;
        
        doLog(currentMethodName + "...");
    }
    
    public void endMethod() {
        doLog(currentMethodName + ": done");
    }

}
