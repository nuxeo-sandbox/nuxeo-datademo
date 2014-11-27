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

package org.nuxeo.datademo.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.datademo.RandomFirstLastNames;
import org.nuxeo.datademo.RandomFirstLastNames.GENDER;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;



@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-datademo" })
public class DataDemoTest {

    @Inject
    CoreSession coreSession;


    @Test
    public void testFirstLastName() throws Exception {

        RandomFirstLastNames r;

        r = RandomFirstLastNames.getInstance();

        String value = r.getFirstName(GENDER.MALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        value = r.getFirstName(GENDER.FEMALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        value = r.getFirstName(GENDER.ANY);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        value = r.getLastName();
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        RandomFirstLastNames.release();

    }

    @Test
    public void testFirstLastNameSingleton() throws Exception {

        RandomFirstLastNames r1, r2, r3;

        // should be in different threads, but whatever.
        // Get 3 instances
        r1 = RandomFirstLastNames.getInstance();
        r2 = RandomFirstLastNames.getInstance();
        r3 = RandomFirstLastNames.getInstance();
        assertEquals(3, RandomFirstLastNames.getUsageCount());

        // Get a value
        String value = r1.getFirstName(GENDER.MALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        // Say thread using r1 calls release() => the singleton is not really released
        RandomFirstLastNames.release();
        assertEquals(2, RandomFirstLastNames.getUsageCount());
        // "Thread" 2 can get a value
        value = r2.getLastName();
        assertNotNull(value);
        assertTrue(!value.isEmpty());

        // "Thread" 2 calls release()
        RandomFirstLastNames.release();
        assertEquals(1, RandomFirstLastNames.getUsageCount());
        // "Thread" 3 can get a value
        value = r3.getFirstName(GENDER.FEMALE);
        assertNotNull(value);
        assertTrue(!value.isEmpty());RandomFirstLastNames.release();

        // "Thread" 3 calls release()
        RandomFirstLastNames.release();
        assertEquals(0, RandomFirstLastNames.getUsageCount());

        // Now, we must have an error
        try {
            value = r3.getLastName();
        } catch (Exception e) {
            assertEquals("NullPointerException", e.getClass().getSimpleName());
        }

    }
}
