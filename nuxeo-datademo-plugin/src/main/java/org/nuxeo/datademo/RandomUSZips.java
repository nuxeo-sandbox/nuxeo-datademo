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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.datademo.tools.ToolsMisc;

/**
 * Thread-safe class to get ZIPs code (from the US only)
 * <p>
 * <b>WARNING</b>
 * <p>
 * The class is thread safe only when at creation/release time. To avoid too
 * many locks when <i>getting</i> a value (<code>getAZip()</code>), there is no
 * thread safety, because we assume you, the caller ;->, will make sure you
 * don't try to get a value <i>after</if> having released the instance.
 *
 * @since 7.1
 */
public class RandomUSZips {

    private static Log log = LogFactory.getLog(RandomUSZips.class);

    protected static ArrayList<String> zips = null;

    protected static ArrayList<String> states = null;

    protected static ArrayList<String> cities = null;

    protected static ArrayList<Double> latitudes = null;

    protected static ArrayList<Double> longitudes = null;

    protected static int maxForRandom = -1;

    private static int usageCount = 0;

    private static RandomUSZips instance;

    private static final String LOCK = "RandomUSZips";

    private static HashMap<String, ArrayList<Integer>> statesAndIndices = null;

    /**
     * Private constructor to handle the singleton.
     * 
     * @throws IOException
     */
    private RandomUSZips() throws IOException {

        zips = new ArrayList<String>();
        states = new ArrayList<String>();
        cities = new ArrayList<String>();
        latitudes = new ArrayList<Double>();
        longitudes = new ArrayList<Double>();

        int count = 0;
        File f = FileUtils.getResourceFileFromContext("files/US-zips.txt");
        try (BufferedReader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                count += 1;
                if (!line.isEmpty()) {
                    String[] elements = line.split("\t");
                    if (elements.length > 4) {
                        zips.add(elements[0]);
                        states.add(elements[1]);
                        cities.add(elements[2]);
                        latitudes.add(Double.valueOf(elements[3]));
                        longitudes.add(Double.valueOf(elements[4]));
                    } else {
                        log.error("Line #" + count
                                + " does not contain at least 5 elements");
                    }
                } else {
                    log.error("Line #" + count + " is empty");
                }
            }
            maxForRandom = zips.size() - 1;
        }

        statesAndIndices = new HashMap<String, ArrayList<Integer>>();
        for (int i = 0; i <= maxForRandom; i++) {

            String theState = states.get(i);
            ArrayList<Integer> indices = statesAndIndices.get(theState);
            if (indices == null) {
                indices = new ArrayList<Integer>();
            }
            indices.add(i);
            statesAndIndices.put(theState, indices);
        }

    }

    protected void cleanup() {

        zips = null;
        states = null;
        cities = null;
        latitudes = null;
        longitudes = null;

        maxForRandom = -1;
    }

    public static RandomUSZips getInstance() throws IOException {

        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RandomUSZips();
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

    public class USZip {
        public String zip;

        public String state;

        public String city;

        public Double latitude;

        public Double longitude;

        USZip(String inZip, String inState, String inCity, double inLatitude,
                double inLongitude) {

            zip = inZip;
            state = inState;
            city = inCity;
            latitude = inLatitude;
            longitude = inLongitude;
        }

        public String toString() {
            return zip + "," + state + "," + city + "," + latitude + ","
                    + longitude;
        }
    }
    
    /*
     * Utility used by other accessors
     * We don't check the indice is valid
     */
    protected USZip getAZip(int idx) {
        
        USZip zip = new USZip(zips.get(idx), states.get(idx), cities.get(idx),
                latitudes.get(idx), longitudes.get(idx));
        
        return zip;
    }

    /**
     * Return a USZip
     * 
     * @return
     *
     * @since 7.2
     */
    public USZip getAZip() {

        int idx = ToolsMisc.randomInt(0, maxForRandom);
        return getAZip(idx);
    }

    /**
     * Return a USZip found in the requested state. Return null if the state is
     * not in the initial list.
     * 
     * @param inElementsCount
     * @return
     *
     * @since 7.2
     */
    public USZip getAZip(String inState) {

        ArrayList<Integer> indices = statesAndIndices.get(inState);
        if (indices != null) {
            int idx = indices.get(ToolsMisc.randomInt(0, indices.size() - 1));
            return getAZip(idx);
        }

        return null;
    }
}
