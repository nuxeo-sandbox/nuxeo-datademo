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

    /**
     * Receives a list of documents. Return true if the caller can continue,
     * false if not
     * 
     * @param inDocs
     * @return
     *
     * @since 7.2
     */
    boolean callback(List<DocumentModel> inDocs);


    /**
     * Receives one document. Return true if the caller can continue,
     * false if not
     * 
     * @param inDoc
     * @return
     *
     * @since 7.2
     */
    boolean callback(DocumentModel inDoc);
}
