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
package org.nuxeo.datademo.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * Simple encapsulation of {@link #CoreQueryDocumentPageProvider} with basic
 * navigation feature.
 * <p>
 * The <code>CoreQueryDocumentPageProvider</code> is already optimized when
 * requesting several time the same page for example (list of docs is cached if
 * we are on the same page). Also, when asking for <code>nextPage()</code> it
 * goes to the last one if there is no next page, etc.
 * <p>
 * We must handle it to make navigation easier, so the basic way of using the
 * class is something like: <code>
 * SimpleNXQLDocumentsPageProvider myPP = new SimpleNXQLDocumentsPageProvider(coreSession, "SELECT * FROM Document", 50);
 * List<DocumentModel> docs;
 * while(myPP.hasDocuments()) {
 *     docs = myPP.getDocuments();
 *     // . . .
 *     // Do something with docs
 *     // . . .
 *     
 *     myPP.nextPage(); // If there is no next page, next call to hasDocuments() will return false
 * }
 * </code>
 * <p>
 * To navigate with the documents themselves, one by one:
 * <p>
 * <code>
 * SimpleNXQLDocumentsPageProvider myPP = new SimpleNXQLDocumentsPageProvider(coreSession, "SELECT * FROM Document", 50);
 * myPP.firstQuery();
 * if (myPP.hasDocuments()) {
 *     while (myPP.hasDocument()) {
 *         DocumentModel doc = myPP.getDocument();
 *         // . . .
 *         // Do something with docs
 *         // . . .
 *         myPP.nextDocument();
 *     }
 * }
 * </code>
 *
 * @since 7.1
 */

public class SimpleNXQLDocumentsPageProvider {

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final int DEFAULT_DOCS_PER_PAGE = 500;

    CoreQueryDocumentPageProvider coreQueryPP;

    boolean noMoreDocs = false;

    public SimpleNXQLDocumentsPageProvider(CoreSession inSession,
            String inQuery, int inDocsPerPage) {

        int pageSize = inDocsPerPage <= 0 ? DEFAULT_DOCS_PER_PAGE
                : inDocsPerPage;

        coreQueryPP = new CoreQueryDocumentPageProvider();
        CoreQueryPageProviderDescriptor ppDesc = new CoreQueryPageProviderDescriptor();
        ppDesc.setPattern(inQuery);
        coreQueryPP.setDefinition(ppDesc);

        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) inSession);
        coreQueryPP.setProperties(props);
        // coreQueryPP.setMaxPageSize(pageSize);
        coreQueryPP.setPageSize(pageSize);
        
    }

    public SimpleNXQLDocumentsPageProvider(CoreSession inSession, String inQuery) {
        this(inSession, inQuery, 0);
    }
    
    public void firstQuery() {
        coreQueryPP.setCurrentPageIndex(0);
        coreQueryPP.setCurrentPageOffset(0);
        coreQueryPP.getCurrentPage();
    }

    public boolean hasDocuments() {
        if (noMoreDocs) {
            return false;
        }
        return coreQueryPP.getCurrentPageSize() > 0;
    }

    public List<DocumentModel> getDocuments() {
        return coreQueryPP.getCurrentPage();
    }

    public boolean hasPreviousPage() {
        return coreQueryPP.isPreviousPageAvailable();
    }

    public boolean hasNextPage() {
        return coreQueryPP.isNextPageAvailable();
    }

    public long getCurrentPageIndex() {
        return coreQueryPP.getCurrentPageIndex();
    }

    public void firstPage() {
        coreQueryPP.firstPage();
        coreQueryPP.getCurrentPage();
    }

    public void previousPage() {
        noMoreDocs = true;
        if (hasPreviousPage()) {
            coreQueryPP.previousPage();
            coreQueryPP.getCurrentPage();
        } else {
            noMoreDocs = true;
        }
    }

    public void nextPage() {
        if (hasNextPage()) {
            noMoreDocs = false;
            coreQueryPP.nextPage();
            coreQueryPP.getCurrentPage();
        } else {
            noMoreDocs = true;
        }
    }

    public void lastPage() {
        coreQueryPP.lastPage();
        coreQueryPP.getCurrentPage();
    }
    
    public boolean hasDocument() {
        if(noMoreDocs) {
            return false;
        }
        return true;
    }
    
    public DocumentModel getDocument() {
        if(noMoreDocs) {
            return null;
        }
        return coreQueryPP.getCurrentEntry();
    }
    
    public boolean hasPreviousDocument() {
        return coreQueryPP.isPreviousEntryAvailable();
    }
    
    public void previousDocument() {
        if(hasPreviousDocument()) {
            noMoreDocs = false;
            coreQueryPP.previousEntry();
        } else {
            noMoreDocs = true;
        }
    }
    
    public boolean hasNextDocument() {
        return coreQueryPP.isNextEntryAvailable();
    }
    
    public void nextDocument() {
        if(hasNextDocument()) {
            noMoreDocs = false;
            coreQueryPP.nextEntry();
        } else {
            noMoreDocs = true;
        }
    }

}
