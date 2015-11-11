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
package org.nuxeo.datademo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.datademo.tools.TransactionInLoop;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;

/**
 * ALlow to change lifecycle states by following transitions.
 * <p>
 * It is a very straightforward process state1->state2->...->stateN.
 *
 * Notice: To bulk-change the state, it would be probably faster to directly
 * change the <code>misc</code> table in the database.
 *
 * @since 7.1
 */
public class LifecycleHandler {

    protected HashMap<String, Integer> stateLabelsAndIndices = new HashMap<String, Integer>();

    protected int statesCount;

    int lastStateIndice;

    protected String[] transitions;

    protected int transitionsCount;

    public LifecycleHandler(String[] inStates, String[] inTransitions)
            throws NuxeoException {

        statesCount = inStates.length;
        transitionsCount = inTransitions.length;

        if (transitionsCount < (statesCount - 1)) {
            throw new NuxeoException("Not enough transitions.");
        }

        int idx = 0;
        for (String state : inStates) {
            stateLabelsAndIndices.put(state, idx);
            idx += 1;
        }
        lastStateIndice = statesCount - 1;

        transitions = Arrays.copyOf(inTransitions, transitionsCount);

    }

    protected DocumentModel moveToState(DocumentModel inDoc,
            int inCurrentState, int inNewState) {

        if (inNewState > inCurrentState) {
            for (int i = inCurrentState + 1; i <= inNewState; i++) {
                inDoc.followTransition(transitions[i - 1]);
            }
        }

        return inDoc;
    }

    public DocumentModel moveToState(DocumentModel inDoc, String inState) {

        int newStateIndice = stateLabelsAndIndices.get(inState);
        int currentStateIndice = stateLabelsAndIndices.get(inDoc.getCurrentLifeCycleState());

        return moveToState(inDoc, currentStateIndice, newStateIndice);
    }

    /**
     * Change the state only if it is ok to follow the transitions. If the
     * document is already in a state that does not allow a transition to be
     * used, the method does nothing.
     * <p>
     *
     * @param inDoc
     * @return
     *
     * @since 7.1
     */
    public DocumentModel moveToRandomState(DocumentModel inDoc) {

        int currentStateIndice = stateLabelsAndIndices.get(inDoc.getCurrentLifeCycleState());
        if (currentStateIndice == lastStateIndice) {
            return inDoc;
        }

        int newStateIndice = ToolsMisc.randomInt(currentStateIndice + 1,
                lastStateIndice);

        return moveToState(inDoc, currentStateIndice, newStateIndice);
    }

    /**
     * Utility wrapper: Applies the <code>moveToRandomState()</code> API on all
     * documents in <code>inDocs</code>
     *
     * @param inDocs
     *
     * @since 7.2
     */
    public void moveToRandomState(CoreSession inSession,
            DocumentModelList inDocs) {

        TransactionInLoop til = new TransactionInLoop(inSession);
        til.commitAndStartNewTransaction();
        for (DocumentModel oneDoc : inDocs) {
            moveToRandomState(oneDoc);
            til.incrementCounter();
            til.commitOrRollbackIfNeeded();
        }
        til.commitAndStartNewTransaction();
    }

    /**
     * Return -1 if state1 is < state2, 1 if state1 if after state2 and 0 if
     * they are equal.
     * 
     * @param inState1
     * @param inState2
     * @return
     *
     * @since 7.2
     */
    public int compareStates(String inState1, String inState2) {

        int result = 0;

        Integer idx1 = stateLabelsAndIndices.get(inState1);
        Integer idx2 = stateLabelsAndIndices.get(inState2);

        if (idx1 == null || idx2 == null) {
            throw new IllegalArgumentException("Invalid state");
        }

        if (idx1 < idx2) {
            result = -1;
        } else if (idx1 == idx2) {
            result = 0;
        } else if (idx1 > idx2) {
            result = 1;
        }

        return result;
    }

    /**
     * This method gets the allowed transitions for the document and will follow
     * one randomly.
     * <p>
     * Does nothing if there is no transition allowed
     *
     * @param inDoc
     * @return
     *
     * @since 7.1
     */
    public static DocumentModel moveToNextRandomState(DocumentModel inDoc,
            boolean inIgnoreDelete) {

        Collection<String> allowedTransitions = inDoc.getAllowedStateTransitions();

        // Collection<String> hop = Collections.
        if (inIgnoreDelete && allowedTransitions.contains("delete")) {
            ArrayList<String> tmp = new ArrayList<String>();
            for (String c : allowedTransitions) {
                if (!c.equals("delete")) {
                    tmp.add(c);
                }
            }
            allowedTransitions = java.util.Collections.unmodifiableCollection(tmp);
        }

        if (allowedTransitions.size() < 1) {
            return inDoc;
        }

        int count = 0;
        // We have a Collection, not an Array: We just loop until our random
        // index (instead of creating a String[] and jumping to its index)
        int idx = ToolsMisc.randomInt(0, allowedTransitions.size() - 1);
        for (String oneTransition : allowedTransitions) {
            if (idx == count) {
                inDoc.followTransition(oneTransition);
                break;
            }
            count += 1;
        }

        return inDoc;
    }

    /**
     * Utility wrapper: Applies the <code>moveToNextRandomState()</code> API on
     * all documents in <code>inDocs</code>
     *
     * @param inDocs
     *
     * @since 7.1
     */
    public static void moveToNextRandomState(CoreSession inSession,
            DocumentModelList inDocs, boolean inIgnoreDelete) {

        TransactionInLoop til = new TransactionInLoop(inSession);
        til.commitAndStartNewTransaction();
        for (DocumentModel oneDoc : inDocs) {
            moveToNextRandomState(oneDoc, inIgnoreDelete);
            til.commitOrRollbackIfNeeded();
        }
        til.commitAndStartNewTransaction();
    }

    /**
     * <b>WARNING</b>
     * <p>
     * This code bypasses the sanity check done by the low-level services in
     * nuxeo, so you could find yourself setting a state that does not exist.
     * <p>
     * This method makes <i>a lot</i> of assumptions: The session is a
     * <code>LocalSession</code>, data is stored in a SQL database, etc.
     * <p>
     * Basically: <i>"Use it at your own risk"</i>
     * 
     * @param inSession
     * @param inDoc
     * @param inState
     * @throws DocumentException
     * @throws LifeCycleException
     *
     * @since 7.2
     */
    public static void directSetCurrentLifecycleState(CoreSession inSession,
            DocumentModel inDoc, String inState) throws DocumentNotFoundException {

        LocalSession localSession = (LocalSession) inSession;
        Session baseSession = localSession.getSession();

        Document baseDoc = baseSession.getDocumentByUUID(inDoc.getId());
        // SQLDocument sqlDoc = (SQLDocument) baseDoc;
        // sqlDoc.setCurrentLifeCycleState(inState);
        baseDoc.setCurrentLifeCycleState(inState);

    }
}
