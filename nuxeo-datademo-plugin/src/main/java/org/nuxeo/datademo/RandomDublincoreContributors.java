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

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 *
 * @since 7.1
 */
public class RandomDublincoreContributors {

    /**
     * Set the <code>contributors</code> and the <code>lastContributor</code>
     * fields, randomly picking a number between <code>inMin</code> and the
     * number of <code>inUsers</code>. <code>inMin</code> is realigned if needed
     * (< 1 => 1, > count of users => count of users), as is <code>inMax</code>
     * <p>
     * Return the document with the modified fields (document is not saved)
     *
     * @param inDoc
     * @param inUsers
     * @param inMin
     * @param inMax
     * @return
     *
     * @since 7.1
     */
    public static DocumentModel setContributors(DocumentModel inDoc,
            String[] inUsers, int inMin, int inMax) {

        int countUsers = inUsers.length;
        inMin = inMin < 1 ? 1 : inMin;
        inMin = inMin > countUsers ? countUsers : inMin;

        inMax = inMax > countUsers || inMax < 1 ? countUsers : inMax;
        inMax = inMax < inMin ? inMin : inMax;

        int[] indexes = ToolsMisc.buildShuffledIndexArray(countUsers);
        int modifUsersCount = inMin == countUsers ? countUsers
                : ToolsMisc.randomInt(inMin, inMax);
        String[] modifUsers = new String[modifUsersCount];
        for (int i = 0; i < modifUsersCount; i++) {
            modifUsers[i] = inUsers[indexes[i]];
        }

        inDoc.setPropertyValue("dc:contributors", modifUsers);
        inDoc.setPropertyValue("dc:lastContributor",
                modifUsers[modifUsersCount - 1]);

        return inDoc;
    }

    /**
     * Wrapper for the main <code>setContributors</code> API. All the users
     * passed will be used
     *
     * @param inDoc
     * @param inUsers
     * @return
     *
     * @since 7.1
     */
    public static DocumentModel setContributors(DocumentModel inDoc,
            String[] inUsers) {
        return setContributors(inDoc, inUsers, inUsers.length, 0);
    }

    /**
     * Wrapper for the main <code>setContributors</code> API, with an exact
     * count of users
     *
     * @param inDoc
     * @param inUsers
     * @return
     *
     * @since 7.1
     */
    public static DocumentModel setContributors(DocumentModel inDoc,
            String[] inUsers, int inHowMany) {
        return setContributors(inDoc, inUsers, inHowMany, inHowMany);
    }

    /**
     * Setup a random user among <code>inUsers</code> in the
     * <code>dc:lastContributor</code> field, and update the
     * <code>dc:contributors</code> if needed (the user was not already a
     * contributor)
     *
     * @param inDoc
     * @param inUsers
     * @return
     *
     * @since 7.1
     */
    public static DocumentModel addContributor(DocumentModel inDoc,
            String[] inUsers) {

        return ToolsMisc.addContributor(inDoc,
                inUsers[ToolsMisc.randomInt(0, inUsers.length)]);
    }

}
