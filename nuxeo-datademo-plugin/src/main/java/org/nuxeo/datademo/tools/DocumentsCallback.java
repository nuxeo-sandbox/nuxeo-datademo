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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Defines the interface typically used by walkers running loops on documents
 * after a query
 *
 * @since 7.2
 */
public interface DocumentsCallback {

    public enum ReturnStatus {
        STOP, CONTINUE;
    }

    /**
     * Possibly called before performing action on a list of documents
     * (typically, before doing a query), so the callback can initialize some
     * values.
     *
     * @since 7.2
     */
    void init();

    /**
     * Called after walking a list of documents, so the callback can perform
     * cleanup if needed.
     * <p>
     * <code>inLastReturnStatus</code> is stored, so later, a code can check the
     * status.
     *
     * @since 7.2
     */
    void end(ReturnStatus inLastReturnStatus);

    /**
     * Receives a list of documents. Return true if the caller can continue,
     * false if not
     * 
     * @param inDocs
     * @return
     *
     * @since 7.2
     */
    ReturnStatus callback(List<DocumentModel> inDocs);

    /**
     * Receives one document. Return true if the caller can continue, false if
     * not
     * 
     * @param inDoc
     * @return
     *
     * @since 7.2
     */
    ReturnStatus callback(DocumentModel inDoc);
}
