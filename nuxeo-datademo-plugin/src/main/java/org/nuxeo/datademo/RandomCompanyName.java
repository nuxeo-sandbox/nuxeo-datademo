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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.datademo.tools.ToolsMisc;

/**
 * Thread-safe class to get random company name built with 1-3 words ("Bravo",
 * "Bravo East" or "Bravo East Yellow" for example).
 * <p>
 * <b>WARNING</b>
 * <p>
 * The class is thread safe only at creation/release time. To avoid too many
 * locks when <i>getting</i> a value (<code>getAName()</code>), there is no
 * thread safety, because we assume you, the caller ;->, will make sure you
 * don't try to get a value <i>after</if> having released the instance.
 *
 * @since 7.1
 */
public class RandomCompanyName {

    private static Log log = LogFactory.getLog(RandomCompanyName.class);

    protected static ArrayList<String> comps1 = null;

    protected static ArrayList<String> comps2 = null;

    protected static ArrayList<String> comps3 = null;

    protected static int maxForRandom = -1;

    private static int usageCount = 0;

    private static RandomCompanyName instance;

    private static final String LOCK = "RandomCompanyName";

    /**
     * Private constructor to handle the singleton.
     * 
     * @throws IOException
     */
    private RandomCompanyName() throws IOException {

        comps1 = new ArrayList<String>();
        comps2 = new ArrayList<String>();
        comps3 = new ArrayList<String>();

        int count = 0;

        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("/files/Companies.txt");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                count += 1;
                if (!line.isEmpty()) {
                    String[] elements = line.split("\t");
                    if (elements.length > 2) {
                        comps1.add(elements[0]);
                        comps2.add(elements[1]);
                        comps3.add(elements[2]);
                    } else {
                        log.error("Line #" + count
                                + " does not contain at least 3 elements");
                    }
                } else {
                    log.error("Line #" + count + " is empty");
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        maxForRandom = comps1.size() - 1;
    }

    protected void cleanup() {

        comps1 = null;
        comps2 = null;
        comps3 = null;

        maxForRandom = -1;
    }

    public static RandomCompanyName getInstance() throws IOException {

        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RandomCompanyName();
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

    /**
     * Return a company built with 1-3 words. If <code>inElementsCount</code> is
     * < 1 or > 3, it is reset to 3.
     * 
     * @param inElementsCount
     * @return
     *
     * @since 7.2
     */
    public String getAName(int inElementsCount) {

        String name = "";

        inElementsCount = inElementsCount < 1 || inElementsCount > 3 ? 3
                : inElementsCount;

        name = comps1.get(ToolsMisc.randomInt(0, maxForRandom));
        if (inElementsCount > 1) {
            name += " " + comps2.get(ToolsMisc.randomInt(0, maxForRandom));
        }
        if (inElementsCount > 2) {
            name += " " + comps3.get(ToolsMisc.randomInt(0, maxForRandom));
        }

        return name;
    }
}
