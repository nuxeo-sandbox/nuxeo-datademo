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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.tools.ToolsMisc;

/**
 * Thread-safe class to get random first/last names with gender.
 * <p>
 * <b>WARNING</b>
 * <p>
 * The class is thread safe only when at creation/release time. To avoid too
 * many locks when <i>getting</i> a value (<code>getAFirstName()</code>,
 * <code>getALastName()</code>), there is no thread safety, because we assume
 * you, the caller ;->, will make sure you don't try to get a value
 * <i>after</if> having released the instance.
 *
 * @since 7.1
 */
public class RandomFirstLastNames {

    private static Log log = LogFactory.getLog(RandomFirstLastNames.class);

    public enum GENDER {
        MALE, FEMALE, ANY
    };

    protected static ArrayList<String> firstNamesMale = null;

    protected static int fnMaleMaxForRandom = -1;

    protected static ArrayList<String> firstNamesFemale = null;

    protected static int fnFemaleMaxForRandom = -1;

    protected static ArrayList<String> lastNames = null;

    protected static int lnMaxForRandom = -1;

    private static RandomFirstLastNames instance = null;

    private static final String LOCK = "RandomFirstLastNames";

    private static int usageCount = 0;

    protected ArrayList<String> loadFile(String inLocalPath) throws IOException {

        ArrayList<String> as = new ArrayList<String>();
                
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream(inLocalPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    as.add(line);
                }
            }
        } finally {
            if(in != null) {
                in.close();
            }
        }

        return as;
    }

    private RandomFirstLastNames() throws IOException {
        firstNamesMale = loadFile("/files/FirstNames-Male.txt");
        fnMaleMaxForRandom = firstNamesMale.size() - 1;

        firstNamesFemale = loadFile("/files/FirstNames-Female.txt");
        fnFemaleMaxForRandom = firstNamesFemale.size() - 1;

        lastNames = loadFile("/files/LastNames.txt");
        lnMaxForRandom = lastNames.size() - 1;
    }

    protected void cleanup() {
        firstNamesMale = null;
        firstNamesFemale = null;
        lastNames = null;

        fnMaleMaxForRandom = -1;
        fnFemaleMaxForRandom = -1;
        lnMaxForRandom = -1;
    }

    /**
     * Load in memory the files used to get random values.
     *
     * It is recommenced to explicitly call <code>release()</code> once you are
     * done with getting values
     *
     * @return
     * @throws IOException
     *
     * @since 7.1
     */
    public static RandomFirstLastNames getInstance() throws IOException {

        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RandomFirstLastNames();
                }
            }
        }

        usageCount += 1;
        return instance;
    }

    /**
     * It is recommended to explicitly release the object once you don't need it
     * anymore, to avoid taking room in memory while not used at all
     *
     *
     * @since 7.1
     */
    public synchronized static void release() {

        usageCount -= 1;
        if (usageCount == 0) {
            instance.cleanup();
            instance = null;
        }

        if (usageCount < 0) {
            usageCount = 0;
            log.error("Releasing the instance too many time");
        }
    }

    public synchronized static int getUsageCount() {
        return usageCount;
    }

    public String getAFirstName() {
        return getAFirstName(GENDER.ANY);
    }

    public String getAFirstName(GENDER inKind) {

        switch (inKind) {
        case MALE:
            return firstNamesMale.get(ToolsMisc.randomInt(0, fnMaleMaxForRandom));

        case FEMALE:
            return firstNamesFemale.get(ToolsMisc.randomInt(0,
                    fnFemaleMaxForRandom));

        default:
            if (ToolsMisc.randomInt(0, 1) == 0) {
                return getAFirstName(GENDER.MALE);
            } else {
                return getAFirstName(GENDER.FEMALE);
            }
        }
    }

    public String getALastName() {
        return lastNames.get(ToolsMisc.randomInt(0, lnMaxForRandom));
    }
}
