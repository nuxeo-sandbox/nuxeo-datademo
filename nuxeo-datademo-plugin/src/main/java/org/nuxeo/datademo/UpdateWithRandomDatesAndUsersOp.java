/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.datademo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Creation date will be from today minus <code>createDateTodayFrom</code> to
 * <code>createDateTodayTo</code> days. Modification date will be creation date
 * + 0-<code>modifDateUpTo</code> days.
 *
 */
@Operation(id = UpdateWithRandomDatesAndUsersOp.ID, category = Constants.CAT_SERVICES, label = "Update Demo Data: With Random Dates and Users", description = "<code>docTypes</code> is a comma-separated list of doc types to handle. <code>users</code> is a comma-separated list of users. Creation date will be from today minus <code>createDateTodayFrom</code> to <code>createDateTodayTo</code> days. Modification date will be creation date + 0-<code>modifDateUpTo</code> days.")
public class UpdateWithRandomDatesAndUsersOp {

    public static final String ID = "UpdateDemoData.WithRandomDatesAndUsers";

    private static final Log log = LogFactory.getLog(UpdateWithRandomDatesAndUsersOp.class);

    @Context
    protected CoreSession session;

    @Param(name = "docTypes", required = true)
    protected String docTypes = "";

    @Param(name = "users", required = true)
    protected String users = "";

    @Param(name = "createDateTodayFrom", required = false, values = { "0" })
    protected long createDateTodayFrom = 0;

    @Param(name = "createDateTodayTo", required = false, values = { "90" })
    protected long createDateTodayTo = 0;

    @Param(name = "modifDateUpTo", required = false, values = { "20" })
    protected long modifDateUpTo = 20;

    protected int saveCounter = 0;

    protected DateFormat _yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected Calendar _today = Calendar.getInstance();

    @OperationMethod
    public void run() throws InterruptedException {

        String[] docTypesArr = docTypes.trim().split(",");
        for (int i = 0; i < docTypesArr.length; i++) {
            docTypesArr[i] = "'" + docTypesArr[i].trim() + "'";
        }

        String[] usersArr = users.split(",");
        for (int i = 0; i < usersArr.length; i++) {
            usersArr[i] = usersArr[i].trim();
        }
        int usersMacForRandom = usersArr.length - 1;

        boolean hasDocTypes = docTypesArr.length > 0;
        boolean hasUsers = usersArr.length > 0;

        String nxql = "SELECT * FROM Document";
        if (hasDocTypes) {
            nxql += " WHERE ecm:primaryType IN ("
                    + String.join(",", docTypesArr) + ")";
        }
        DocumentModelList allDocs = session.query(nxql);

        saveCounter = 0;
        String logSuffix = " / " + allDocs.size();
        for (DocumentModel oneDoc : allDocs) {

            Calendar creationDate, modifDate;

            creationDate = (Calendar) _today.clone();
            creationDate.add(
                    Calendar.DATE,
                    randomInt((int) createDateTodayFrom,
                            (int) createDateTodayTo) * -1);

            modifDate = buildDate(creationDate, (int) modifDateUpTo);
            if (hasUsers) {
                _updateModificationInfo(oneDoc,
                        usersArr[randomInt(0, usersMacForRandom)], modifDate);
            } else {
                _updateModificationInfo(oneDoc, null, modifDate);
            }

            oneDoc.putContextData(
                    DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
            doSaveDoc(oneDoc);

            log.warn("Modifed doc #" + saveCounter + logSuffix);

        }

        session.save();
    }

    protected int randomInt(int inMin, int inMax) {
        // No error check here
        return inMin + (int) (Math.random() * ((inMax - inMin) + 1));
    }

    protected Calendar buildDate(Calendar inDate, int inDays) {
        Calendar d = (Calendar) inDate.clone();

        d.add(Calendar.DATE, inDays);
        if (d.after(_today)) {
            d = (Calendar) _today.clone();
        }

        return d;
    }

    protected void doSaveDoc(DocumentModel inDoc) throws InterruptedException {
        session.saveDocument(inDoc);

        saveCounter += 1;
        if ((saveCounter % 10) == 0) {
            session.save();
            TransactionHelper.commitOrRollbackTransaction();
            Thread.sleep(500);
            TransactionHelper.startTransaction(5000);
        }
    }

    // Does not save the document
    protected void _updateModificationInfo(DocumentModel inDoc, String inUser,
            Calendar inDate) throws PropertyException, ClientException {

        if (inUser != null) {
            inDoc.setPropertyValue("dc:lastContributor", inUser);

            // Handling the list of contributors: The following is a
            // copy/paste from...
            // nuxeo-platform-dublincore/src/main/java/org/nuxeo/ecm/
            // platform/dublincore/service/DublinCoreStorageService.java
            // ... with very little change (no try-catch for example)
            String[] contributorsArray;
            contributorsArray = (String[]) inDoc.getProperty("dublincore",
                    "contributors");
            List<String> contributorsList = new ArrayList<String>();
            if (contributorsArray != null && contributorsArray.length > 0) {
                contributorsList = Arrays.asList(contributorsArray);
                // make it resizable
                contributorsList = new ArrayList<String>(contributorsList);
            }
            if (!contributorsList.contains(inUser)) {
                contributorsList.add(inUser);
                String[] contributorListIn = new String[contributorsList.size()];
                contributorsList.toArray(contributorListIn);
                inDoc.setProperty("dublincore", "contributors",
                        contributorListIn);
                inDoc.setPropertyValue("dc:contributors", contributorListIn);
            }
        }
        inDoc.setPropertyValue("dc:modified", inDate);
    }

}
