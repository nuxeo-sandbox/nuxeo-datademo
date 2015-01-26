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

import org.nuxeo.datademo.tools.DocumentsCallback.ReturnStatus;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * Encapsulation of a {@link CoreQueryDocumentPageProvider} which allows to use
 * a callback on each page or each document. This makes it easier to use when
 * querying a lots of documents server-side and pagination will occur by
 * default.
 * <p>
 * The usage is quite simple:
 * <ul>
 * <li>Create class that implements {@link DocumentsCallback}</li>
 * <li>Create a <code>DocumentsWalker</code> with a query (and some other
 * parameters)</li>
 * <li>Possibly, setup more parameters</li>
 * <li>Then just call the <code>runForEachPage()</code> or
 * <code>runForEachDocument()</code> API</li>
 * </ul>
 * <i>See the unit tests for an example of use)</i>
 *
 * @since 7.2
 */
public class DocumentsWalker {

    public static final int DEFAULT_DOCS_PER_PAGE = 500;

    CoreQueryDocumentPageProvider coreQueryPP;

    /**
     * Initialize the underlying <code>CoreQueryDocumentPageProvider</code>
     * 
     * @param inSession
     * @param inQuery
     * @param inPageSize
     */
    public DocumentsWalker(CoreSession inSession, String inQuery, int inPageSize) {

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

    /**
     * Reseting the query makes the next call to <code>runForEachPage()</code>/
     * <code>runForEachDocument()</code> to start over at first page.
     * 
     * @since 7.2
     */
    protected void resetQuery() {

        coreQueryPP.setCurrentPageIndex(0);
        coreQueryPP.setCurrentPageOffset(0);
    }

    /**
     * Run the query, then call <code>inCallback</code> with a
     * <code>List<DocumentModel></code> for each page of the query result. If
     * the callback returns <code>false</code>, the method stops walking the
     * pages and returns.
     * 
     * @param inCallback
     *
     * @since 7.2
     */
    public void runForEachPage(DocumentsCallback inCallback) {

        ReturnStatus status = ReturnStatus.CONTINUE;

        resetQuery();

        inCallback.init();
        List<DocumentModel> docs = coreQueryPP.getCurrentPage();
        while (status == ReturnStatus.CONTINUE && docs != null && docs.size() > 0) {

            status = inCallback.callback(docs);

            if (status == ReturnStatus.STOP) {
                break;
            }

            if (coreQueryPP.isNextPageAvailable()) {
                coreQueryPP.nextPage();
                docs = coreQueryPP.getCurrentPage();
            } else {
                docs = null;
            }
        }
        inCallback.end(status);
    }

    /**
     * Run the query, then call <code>inCallback</code> with a
     * <code>DocumentModel</code> for each document of the query result. If the
     * callback returns <code>false</code>, the method stops walking the pages
     * and returns.
     * 
     * @param inCallback
     *
     * @since 7.2
     */
    public void runForEachDocument(DocumentsCallback inCallback) {

        ReturnStatus status = ReturnStatus.CONTINUE;

        resetQuery();

        inCallback.init();
        List<DocumentModel> docs = coreQueryPP.getCurrentPage();
        while (status == ReturnStatus.CONTINUE && docs != null && docs.size() > 0) {

            for (DocumentModel doc : docs) {
                status = inCallback.callback(doc);
                if (status == ReturnStatus.STOP) {
                    break;
                }
            }

            if (status == ReturnStatus.STOP) {
                break;
            }

            if (coreQueryPP.isNextPageAvailable()) {
                coreQueryPP.nextPage();
                docs = coreQueryPP.getCurrentPage();
            } else {
                docs = null;
            }
        }
        inCallback.end(status);
    }

}
