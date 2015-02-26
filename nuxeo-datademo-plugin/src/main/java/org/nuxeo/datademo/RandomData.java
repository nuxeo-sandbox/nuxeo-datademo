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

    protected RandomVocabularies randomVocs = null;

    public RandomData() {
        this(0, 0);
    }

    /**
     * Main constructor
     * 
     * Default value for <codecommitModulo</code> (if value <= 0) is
     * <code>TransactionInLoop.COMMIT_MODUL0</code>
     * <p>
     * If <code>inLogStatusModulo</code> <= 0, nothing is logged.
     *
     * @param inCommitModulo
     * @param inLogStatusModulo
     */
    public RandomData(int inCommitModulo, int inLogStatusModulo) {

        commitModulo = inCommitModulo < 0 ? 0 : inCommitModulo;
        logStatusModulo = inLogStatusModulo < 0 ? 0 : inCommitModulo;

        randomVocs = new RandomVocabularies();
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
     *
     * @param inDocs
     * @param inXPath
     * @param inValues
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
        transactionLoop.commitAndStartNewTransaction();
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
        transactionLoop.commitAndStartNewTransaction();

    }

    /**
     * The method fills the fields in <code>inXPathsAndVocs</code> with a random
     * value taken from the vocabulary.
     * <p>
     * For example with dublincore: <code>
     * HashMap<String, String> f = new HashMap<String, String>();
     * f.put("dc:nature", "nature");
     * f.put("dc:language", "language");
     * f.put("dc:coverage", "country");
     * f.put("dc:subjects", "subtopic");
     * </code>
     * <p>
     * <b>Important<i>:
     * <ul>
     * <li>The fields cannot be of type complex.</li>
     * <li>The fields can be multivalued. 1-3 values will be set, but notice
     * that it can happen the same value is set 2-3 times (we don't check this)</li>
     * </ul>
     * <p>
     * <i>NOTICE</i>: When you have a list of DocumentModel, it will be faster
     * to call
     * <code>updateFieldsWithVocabularies(DocumentModelList inDocs, HashMap<String, String> inXPathsAndVocs)</code>
     * instead.
     *
     * @param inDoc
     * @param inXPathsAndVocs
     *
     * @since 7.1
     */
    public void updateFieldsWithVocabularies(DocumentModel inDoc,
            HashMap<String, String> inXPathsAndVocs) {

        for (String xpath : inXPathsAndVocs.keySet()) {
            String vocName = inXPathsAndVocs.get(xpath);
            if (inDoc.getProperty(xpath).isList()) {

                int valuesCount = ToolsMisc.randomInt(1, 3);
                String[] values = new String[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    // We just hope we will not have 2 or 3 times the same value
                    values[i] = randomVocs.getRandomValue(vocName);
                }
                inDoc.setPropertyValue(xpath, values);

            } else {
                inDoc.setPropertyValue(xpath,
                        randomVocs.getRandomValue(vocName));
            }
        }
    }

    /**
     * For each document in
     * <code>inDocs</doc>, the method fills the fields in <code>inXPathsAndVocs</code>
     * with a random value taken from the vocabulary.
     * <p>
     * For example with dublincore: <code>
     * HashMap<String, String> f = new HashMap<String, String>();
     * f.put("dc:nature", "nature");
     * f.put("dc:language", "language");
     * f.put("dc:coverage", "country");
     * f.put("dc:subjects", "subtopic");
     * </code>
     * <p>
     * <b>Important<i>:
     * <ul>
     * <li>The fields cannot be of type complex.</li>
     * <li>The fields can be multivalued. 1-3 values will be set, but notice
     * that it can happen the same value is set 2-3 times (we don't check this)</li>
     * </ul>
     * <p>
     *
     * @param inFields
     * @param inXPathsAndVocs
     *
     * @since 7.1
     */
    public void updateFieldsWithVocabularies(DocumentModelList inDocs,
            HashMap<String, String> inXPathsAndVocs) {

        if (inDocs == null || inDocs.size() == 0) {
            return;
        }
        // We need a DocumentModel to check if a field is multivalued
        DocumentModel doc = inDocs.get(0);

        if (inXPathsAndVocs == null || inXPathsAndVocs.size() == 0) {
            return;
        }
        int fieldsCount = inXPathsAndVocs.size();
        // We have a bunch of "synchronized" arrays ("synchronized": the same
        // index applies to the same set of info in all arrays). This is a bit
        // faster than building an object of complex HashMap, or checking the
        // field property for each document
        String[] fields = new String[fieldsCount];
        String[] vocs = new String[fieldsCount];
        boolean[] fieldIsList = new boolean[fieldsCount];

        int idx = 0;
        for (String xpath : inXPathsAndVocs.keySet()) {
            fields[idx] = xpath;
            fieldIsList[idx] = doc.getProperty(xpath).isList();
            vocs[idx] = inXPathsAndVocs.get(xpath);

            idx += 1;
        }

        boolean hasLogModulo = logStatusModulo > 0;
        String logPrefix = "Updated count: ";
        String logSuffix = "/" + inDocs.size();
        CoreSession session = ToolsMisc.getCoreSession(inDocs);
        TransactionInLoop transactionLoop = new TransactionInLoop(session,
                commitModulo);

        counter = 0;
        int i;
        String vocValue;
        transactionLoop.commitAndStartNewTransaction();
        for (DocumentModel oneDoc : inDocs) {

            for (i = 0; i < fieldsCount; i++) {
                if (fieldIsList[i]) {
                    int valuesCount = ToolsMisc.randomInt(1, 3);
                    String[] values = new String[valuesCount];
                    for (int iValue = 0; iValue < valuesCount; iValue++) {
                        // We just hope we will not have 2 or 3 times the same.
                        values[iValue] = randomVocs.getRandomValue(vocs[i]);
                    }
                    oneDoc.setPropertyValue(fields[i], values);

                } else {
                    vocValue = randomVocs.getRandomValue(vocs[i]);
                    oneDoc.setPropertyValue(fields[i], vocValue);
                }
            }

            transactionLoop.saveDocumentAndCommitIfNeeded(oneDoc);
            counter += 1;
            if (hasLogModulo && (counter % logStatusModulo) == 0) {
                ToolsMisc.forceLogInfo(log, logPrefix + counter + logSuffix);
            }
        }
        transactionLoop.commitAndStartNewTransaction();

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

    public void resetVocabularies() {
        randomVocs = null;
    }
}
