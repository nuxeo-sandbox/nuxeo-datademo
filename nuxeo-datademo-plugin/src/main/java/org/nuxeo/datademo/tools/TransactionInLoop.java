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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Wrapper around the
 * <code>org.nuxeo.runtime.transaction.TransactionHelper</code> object.
 * <p>
 * Commit the transaction every n calls (default is 50). Typical usage is:
 * <code>
 * //session is a CoreSession we got previously
 * TransactionInLoop til = new TransactionInLoop(session);
 * til.commitAndStartNewTransaction();
 * for(DocumentModel doc : foundDocs) {
 *     //... do something, update fields, ...
 *     til.saveDocumentAndCommitIfNeeded(doc);
 * }
 * til.commitAndStartNewTransaction();
 * </code>
 *
 * @since 7.1
 */
public class TransactionInLoop {

    public static final int COMMIT_MODUL0 = 50;

    protected int counter = 0;

    protected int commitModulo = COMMIT_MODUL0;
    
    protected int sleepDurationAfterCommit = 0;

    protected CoreSession session = null;

    /**
     * Contructor
     * 
     * @param inSession
     */
    public TransactionInLoop(CoreSession inSession) {
        session = inSession;
    }

    /**
     * Constructor
     * 
     * Passing a <code>inCommitModulo</code> value <= 0 restes the value to the
     * default one
     * 
     * @param inSession
     * @param inCommitModulo
     */
    public TransactionInLoop(CoreSession inSession, int inCommitModulo) {
        session = inSession;
        setCommitModulo(inCommitModulo);
    }

    /**
     * usually called (not mandatory) before and after looping, so:
     * <p>
     * <ul>
     * <li>Before: The transaction is committed so any pending value is saved</li>
     * <li>After: So remaining documents if any are saved to the db</li>
     * </ul>
     * <p>
     * <b>Notice
     * </p>
     * : The code also save the session (calling CoreSession#save)
     * <p>
     * 
     * <pre>
     * TransactionInLoop til = new TransactionInLoop(session);
     * til.commitAndStartNewTransaction();
     * // docs is a DocumentModelList
     * for (DocumentModel oneDoc : docs) {
     *     // . . .
     *     til.saveDocumentAndCommitIfNeeded();
     * }
     * til.commitAndStartNewTransaction();
     * 
     * <pre>
     *
     * @since 7.2
     */
    public void commitAndStartNewTransaction() {
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    /**
     * Save the document, and if the number of saved documents has reached the
     * commitModule, then the transaction is committed.
     * <p>
     * WARNING: "the number of saved documents" actually means "the number of
     * calls to <code>saveDocumentAndCommitIfNeeded()</code>"
     * 
     * @param inDoc
     *
     * @since 7.2
     */
    public DocumentModel saveDocumentAndCommitIfNeeded(DocumentModel inDoc) {

        inDoc = session.saveDocument(inDoc);
        counter += 1;
        commitOrRollbackIfNeeded();

        return inDoc;
    }

    /**
     * If you handle the saving of the document and are using
     * <code>setCounter()</code> you can then also call this method to commit
     * the transaction.
     *
     * @since 7.2
     */
    public void commitOrRollbackIfNeeded() {
        if ((counter % commitModulo) == 0) {
            commitAndStartNewTransaction();
            if(sleepDurationAfterCommit > 0) {
                try {
                    Thread.sleep(sleepDurationAfterCommit);
                } catch (InterruptedException e) {
                    // Just ignore
                }
            }
        }
    }

    public int getCommitModulo() {
        return commitModulo;
    }

    /**
     * Passing a <code>inCommitModulo</code> value <= 0 restes the value to the
     * default one.
     * 
     * @param inCommitModulo
     *
     * @since 7.2
     */
    public void setCommitModulo(int inCommitModulo) {
        commitModulo = inCommitModulo <= 0 ? COMMIT_MODUL0 : inCommitModulo;
    }

    public int getCounter() {
        return counter;
    }

    /**
     * Useful API if don't use <code>saveDocumentAndCommitIfNeeded</code> and
     * handle saves. You can then later call
     * <code>commitOrRollbackIfNeeded()</code>
     * 
     * @since 7.2
     */
    public void setCounter(int inValue) {
        counter = inValue;
    }

    /**
     * Useful API if don't use <code>saveDocumentAndCommitIfNeeded</code> and
     * handle saves. You can then later call
     * <code>commitOrRollbackIfNeeded()</code>
     * 
     * @since 7.2
     */
    public void incrementCounter() {
        counter += 1;
    }
    
    public int getSleepDurationAfterCommit() {
        return sleepDurationAfterCommit;
    }
    
    public void setSleepDurationAfterCommit(int inValue) {
        sleepDurationAfterCommit = inValue;
    }

}
