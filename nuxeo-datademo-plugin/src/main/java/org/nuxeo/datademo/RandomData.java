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
package org.nuxeo.datademo;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.datademo.tools.TransactionInLoop;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 *
 *
 * @since 7.1
 */
public class RandomData {

    private static Log log = LogFactory.getLog(RandomData.class);

    protected int logStatusModulo = 0;

    protected int commitModulo = 0;

    protected int counter = 0;

    public RandomData() {
        this(0, 0);
    }

    /**
     * Main constructor
     *
     * @param inCommitModulo
     * @param inLogStatusModulo
     */
    public RandomData(int inCommitModulo, int inLogStatusModulo) {

        commitModulo = inCommitModulo < 0 ? 0 : inCommitModulo;
        logStatusModulo = inLogStatusModulo < 0 ? 0 : inCommitModulo;
    }

    /**
     * The keys in <code>inValues</code> are the data to be saved in the field.
     * The values are the % to dispatch. For example: <code>
     * HashMap<String, Integer> data = new HashMap<String, Integer>();
     * data.put("Red", 60);
     * data.put("Green", 20);
     * data.put("Blue", 10);
     * data.put("White", 10);
     * </code>
     * <p>
     * The total of the values must == 100. If it is not, the repartition will
     * not be as expected
     * <p>
     * IMPORTANT: We are just using Math.random(), so don't expect an exact
     * repartition.
     * <p>
     * An info if logged every <code>inLogStatusModulo</code> if it > 0.
     *
     * @param inDocs
     * @param inXPath
     * @param inValues
     * @param inLogStatusModulo
     *
     * @since 7.1
     */
    public void updateField(DocumentModelList inDocs, String inXPath,
            HashMap<String, Integer> inValues) {

        /*
         * We build an array of integers where we store the values of an index
         * to a String field holding the data to use for the field. In this
         * array of integer, the same index is repeated to match the %, so when
         * looping thru each document, we will just get a random value in this
         * array, which will give us the string value to use.
         *
         * For example, with 2 values, "a"-95 and "b"-5, we build (1) a String
         * array of 2 elements, "a" and "b", and (2) an integer array of 100
         * elements where elements 0-94 are filled with 0 and 95-99 with 1.
         *
         * This way when asking for a random number between 0-99, we will have
         * more 0s than 1s, which is expected
         *
         * (Avoiding talking about "true" random, or "false" random here)
         */
        String[] fieldData = new String[inValues.size()];
        int idx = 0;
        ArrayList<Integer> indices = new ArrayList<Integer>();
        for (String key : inValues.keySet()) {
            fieldData[idx] = key;
            for (int i = 0; i < inValues.get(key); i++) {
                indices.add(idx);
            }
            idx += 1;
        }
        int maxForRandom = indices.size() - 1;

        boolean hasLogModulo = logStatusModulo > 0;
        String logPrefix = "Updated count: ";
        String logSuffix = "/" + inDocs.size();
        CoreSession session = ToolsMisc.getCoreSession(inDocs);
        TransactionInLoop transactionLoop = new TransactionInLoop(session,
                commitModulo);
        counter = 0;
        for (DocumentModel oneDoc : inDocs) {

            idx = indices.get(ToolsMisc.randomInt(0, maxForRandom));
            String value = fieldData[idx];
            oneDoc.setPropertyValue(inXPath, value);

            transactionLoop.saveDocumentAndCommitIfNeeded(oneDoc);

            counter += 1;
            if (hasLogModulo && (counter % logStatusModulo) == 0) {
                ToolsMisc.forceLogInfo(log, logPrefix + counter + logSuffix);
            }
        }

    }

    public int getCounter() {
        return counter;
    }

    public RandomData resetCounter() {
        counter = 0;
        return this;
    }

    public int getLogStatusModulo() {
        return logStatusModulo;
    }

    public RandomData setLogStatusModulo(int inLogStatusModulo) {
        logStatusModulo = inLogStatusModulo;
        return this;
    }

    public int getCommitModulo() {
        return commitModulo;
    }

    public RandomData setCommitModulo(int inCommitModulo) {
        commitModulo = inCommitModulo;
        return this;
    }
}
