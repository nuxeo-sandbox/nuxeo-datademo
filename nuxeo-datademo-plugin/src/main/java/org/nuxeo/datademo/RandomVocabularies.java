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

import java.util.HashMap;

/**
 * Utility class: Loads in memory all the <code>id</code> of all the entries in
 * the vocabularies, and can return (<code>getRandomValue()</code>) a random
 * value from one vocabulary.
 * <p>
 * <b>Notice</b>: The returned value is the
 * <code>id</id> of the item, not its <code>label</code>.
 * <p>
 * <i>Warning</i>: As always with this kind of utility working in memory, make
 * sure you don't shock the JVM with a lot of vocabularies and/or with huge
 * vocabularies.
 *
 * @since 7.1
 */
public class RandomVocabularies {

    protected HashMap<String, RandomVocabulary> vocabularies = null;

    /**
     * Constructor
     */
    public RandomVocabularies() {
        vocabularies = new HashMap<String, RandomVocabulary>();
    }

    /**
     * Constructor
     * 
     * @param inVocNames
     */
    public RandomVocabularies(String...inVocNames) {
        vocabularies = new HashMap<String, RandomVocabulary>();
        for(String vocName : inVocNames) {
            addVocabulary(vocName);
        }
    }

    /**
     * Loads all the <code>id</code> of the vocabulary. Does nothing if the
     * vocabulary was already loaded.
     * <p>
     * This means that if the vocabulary was modified in between, it is not
     * reloaded. Call <code>reload()</code> if needed
     *
     * @param inVocName
     * @return
     *
     * @since 7.1
     */
    public RandomVocabulary addVocabulary(String inVocName) {

        RandomVocabulary theVoc = vocabularies.get(inVocName);
        if (theVoc == null) {
            theVoc = new RandomVocabulary(inVocName);
            vocabularies.put(inVocName, theVoc);
        }
        return theVoc;
    }

    /**
     * Return a random value (the <code>id</code> of the item) for this
     * vocabulary.
     *
     * @param inVocName
     * @return the RandomVocabulary
     *
     * @since 7.1
     */
    public String getRandomValue(String inVocName) {

        RandomVocabulary theVoc = addVocabulary(inVocName);
        return theVoc.getRandomValue();
    }

    /**
     * Force the reload of all the values of the vocabulary.
     *
     * @param inVocName
     * @return the RandomVocabulary
     *
     * @since 7.1
     */
    public RandomVocabulary reload(String inVocName) {

        RandomVocabulary theVoc = vocabularies.get(inVocName);
        if(theVoc != null) {
            theVoc.reload();
        } else {
            theVoc = addVocabulary(inVocName);
        }
        return theVoc;
    }
}
