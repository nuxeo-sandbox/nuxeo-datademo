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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility class: Loads in memory all the <code>id</code> of all the entries in
 * the vocabulary, and can return ( <code>getRandomValue()</code>) a random
 * value.
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
public class RandomVocabulary {

    List<String> values = null;

    String vocName;

    int maxForRandom;

    public RandomVocabulary(String inVocName) {

        vocName = inVocName;
        loadValues();
    }

    protected void loadValues() {
        org.nuxeo.ecm.directory.Session session = Framework.getService(
                DirectoryService.class).open(vocName);
        values = session.getProjection(new HashMap<String, Serializable>(),
                "id");
        session.close();
        maxForRandom = values.size() - 1;
        
    }
    
    public int size() {
        return values.size();
    }

    public String getRandomValue() {
        return values.get(ToolsMisc.randomInt(0, maxForRandom));
    }

    public void reload() {
        values = null;
        loadValues();
    }

    @Override
    public String toString() {
        return "Vocabulary " + vocName + ": " + values.toString();
    }
}
