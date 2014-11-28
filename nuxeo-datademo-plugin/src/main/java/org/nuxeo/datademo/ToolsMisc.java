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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 *
 * @since 7.1
 */
public class ToolsMisc {

    public static int randomInt(int inMin, int inMax) {
        if (inMin == inMax) {
            return inMin;
        }
        return inMin + (int) (Math.random() * ((inMax - inMin) + 1));
    }

    public static int[] buildShuffledIndexArray(int inCount) {

        int[] array = new int[inCount];
        for (int i = 0; i < inCount; i++) {
            array[i] = i;
        }

        return shuffleArray(array);
    }

    public static int[] shuffleArray(int[] inArray) {
        int index, temp;
        Random random = new Random();
        for (int i = inArray.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = inArray[index];
            inArray[index] = inArray[i];
            inArray[i] = temp;
        }

        return inArray;
    }

    /**
     * Add <code>inUser</code> to the <code>dc:contributors</code> field (if
     * this user was not already a contributor) and update the
     * <code>dc:lastContributor</code> field.
     *
     * @param inDoc
     * @param inUser
     * @return
     *
     * @since 7.1
     */
    public static DocumentModel addContributor(DocumentModel inDoc,
            String inUser) {

        if (inUser != null) {
            inDoc.setPropertyValue("dc:lastContributor", inUser);

            // Handling the list of contributors: The following is a
            // copy/paste from...
            // nuxeo-platform-dublincore/src/main/java/org/nuxeo/ecm/
            // platform/dublincore/service/DublinCoreStorageService.java
            // ... with very little change (no try-catch for example)
            String[] contributorsArray;
            contributorsArray = (String[]) inDoc.getPropertyValue("dc:contributors");
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
                inDoc.setPropertyValue("dc:contributors", contributorListIn);
            }
        }

        return inDoc;
    }
}
