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

import java.util.HashMap;

/**
 *
 *
 * @since 7.1
 */
public class RandomVocabularies {

    protected HashMap<String, RandomVocabulary> vocabularies = null;

    public RandomVocabularies() {
        vocabularies = new HashMap<String, RandomVocabulary>();
    }

    public RandomVocabulary addVocabulary(String inVocName) {

        RandomVocabulary theVoc = vocabularies.get(inVocName);
        if (theVoc == null) {
            theVoc = new RandomVocabulary(inVocName);
            vocabularies.put(inVocName, theVoc);
        }
        return theVoc;
    }

    public String getRandomValue(String inVocName) {

        RandomVocabulary theVoc = addVocabulary(inVocName);
        return theVoc.getRandomValue();
    }
}
