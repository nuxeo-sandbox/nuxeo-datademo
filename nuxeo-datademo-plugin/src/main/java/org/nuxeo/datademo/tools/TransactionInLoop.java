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
package org.nuxeo.datademo.tools;

import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Wrapper around the
 * <code>org.nuxeo.runtime.transaction.TransactionHelper</code> object.
 * <p>
 * Commit the transaction every n calls (default is 10). Typical usage is:
 * <code>
 * TransactionInLoop t = new TransactionInLoop();
 * for(DocumentModel doc : foundDocs) { // foundDocs is a DocumentModelList
 *     ... do something, update field
 *     session.saveDocument(doc);
 *     t.commitOrRollbackIfNeeded();
 * }
 * </code>
 *
 * @since 7.1
 */
public class TransactionInLoop {
    public static final int COMMIT_MODUL0 = 10;

    protected int counter = 0;

    protected int commitModulo = COMMIT_MODUL0;

    public TransactionInLoop() {

    }

    public TransactionInLoop(int inCommitModulo) {
        commitModulo = inCommitModulo < 0 ? COMMIT_MODUL0 : inCommitModulo;
    }

    public void commitOrRollbackIfNeeded() {

        if ((counter % 10) == commitModulo) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    public int getCommitModulo() {
        return commitModulo;
    }

    public void setCommitModulo(int inCommitModulo) {
        commitModulo = inCommitModulo < 0 ? COMMIT_MODUL0 : inCommitModulo;
    }

    public int getCounter() {
        return counter;
    }

}
