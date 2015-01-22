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
package org.nuxeo.datademo.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * 
 *
 * @since 7.2
 */
public class DocumentsWalker {

    public static final int DEFAULT_DOCS_PER_PAGE = 500;

    CoreQueryDocumentPageProvider coreQueryPP;

    public DocumentsWalker(CoreSession inSession, String inQuery,
            int inPageSize) {

        coreQueryPP = new CoreQueryDocumentPageProvider();
        CoreQueryPageProviderDescriptor ppDesc = new CoreQueryPageProviderDescriptor();
        ppDesc.setPattern(inQuery);
        coreQueryPP.setDefinition(ppDesc);

        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) inSession);
        coreQueryPP.setProperties(props);

        inPageSize = inPageSize > 0 ? inPageSize : DEFAULT_DOCS_PER_PAGE;
        coreQueryPP.setMaxPageSize(inPageSize);
        coreQueryPP.setPageSize(inPageSize);
    }
    
    protected void resetQuery() {

        coreQueryPP.setCurrentPageIndex(0);
        coreQueryPP.setCurrentPageOffset(0);
    }

    public void runForEachPage(DocumentsCallback inCallback) {

        boolean goOn = true;
        
        resetQuery();
        
        List<DocumentModel> docs = coreQueryPP.getCurrentPage();
        while (goOn && docs != null && docs.size() > 0) {

            goOn = inCallback.callback(docs);

            if(!goOn) {
                break;
            }
            
            if (coreQueryPP.isNextPageAvailable()) {
                coreQueryPP.nextPage();
                docs = coreQueryPP.getCurrentPage();
            } else {
                docs = null;
            }
        }
    }

    public void runForEachDocument(DocumentsCallback inCallback) {

        boolean goOn = true;
        
        resetQuery();

        List<DocumentModel> docs = coreQueryPP.getCurrentPage();
        while (goOn && docs != null && docs.size() > 0) {

            for(DocumentModel doc : docs) {
                goOn = inCallback.callback(doc);
                if(!goOn) {
                    break;
                }
            }

            if(!goOn) {
                break;
            }
            
            if (coreQueryPP.isNextPageAvailable()) {
                coreQueryPP.nextPage();
                docs = coreQueryPP.getCurrentPage();
            } else {
                docs = null;
            }
        }
    }

}
