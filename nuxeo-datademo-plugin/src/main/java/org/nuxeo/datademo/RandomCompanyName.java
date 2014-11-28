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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;

/**
 *
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

    protected RandomCompanyName() throws IOException {

        comps1 = new ArrayList<String>();
        comps2 = new ArrayList<String>();
        comps3 = new ArrayList<String>();

        int count = 0;
        File f = FileUtils.getResourceFileFromContext("files/Companies.txt");
        try (BufferedReader reader = Files.newBufferedReader(f.toPath())) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                count += 1;
                if(!line.isEmpty()) {
                    String [] elements = line.split("\t");
                    if(elements.length > 2) {
                        comps1.add(elements[0]);
                        comps2.add(elements[1]);
                        comps3.add(elements[2]);
                    } else {
                        log.error("Line #" + count + " does not contain at least 3 elements");
                    }
                } else {
                    log.error("Line #" + count + " is empty");
                }
            }
            maxForRandom = comps1.size() -1;
        }

    }

    protected void cleanup() {

        comps1 = null;
        comps2 = null;
        comps3 = null;

        maxForRandom = -1;
    }

    public synchronized static RandomCompanyName getInstance() throws IOException {

        if(instance == null) {
            instance = new RandomCompanyName();
        }
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
        if(usageCount == 0) {
            instance.cleanup();
            instance = null;
        }

        if(usageCount < 0) {
            usageCount = 0;
            log.error("Releasing the instance too many time");
        }
    }

    public synchronized static int getUsageCount() {
        return usageCount;
    }

    public String getAName(int inElementsCount) {

        String name = "";

        inElementsCount = inElementsCount < 1 || inElementsCount > 3 ? 3 : inElementsCount;

        name = comps1.get(ToolsMisc.randomInt(0, maxForRandom));
        if(inElementsCount > 1) {
            name += " " + comps2.get(ToolsMisc.randomInt(0, maxForRandom));
        }
        if(inElementsCount > 2) {
            name += " " + comps3.get(ToolsMisc.randomInt(0, maxForRandom));
        }

        return name;
    }
}
