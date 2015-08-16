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
package org.nuxeo.datademo.tools;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Disable 1-n listeners. Usable is simple: Add listeners by their names, then call <code>disableListeners()</code>.
 * Once the work that requested to disable the listeners is done, call <code>restoreListeners()</code>.
 * <P>
 * IMPORTANT: You must call <code>disableListeners()</code> <i>before</i> <code>restoreListeners()</code>, because the
 * state (enabled/disabled) is saved when you disable them.
 * 
 * @since 7.3
 */
public class ListenersDisabler {

    private static final Log log = LogFactory.getLog(ListenersDisabler.class);
    
    // See nxdublincore-service.xml (in Nuxeo sources, nuxeo-platform-dublincore)
    public static final String DUBLINCORELISTENER_NAME = "dclistener";
    
    // See OperationEventListener.java (in Nuxeo sources, nuxeo-automation-core)
    public static final String OP_CHAIN_LISTENER_NAME = "opchainlistener";
    
    // See OperationEventListener.java (in Nuxeo sources, nuxeo-automation-core)
    public static final String OP_CHAIN_POSTCOMMIT_LISTENER_NAME = "opchainpclistener";

    HashMap<String, EventListenerDescriptor> listeners = new HashMap<String, EventListenerDescriptor>();

    HashMap<String, Boolean> originalStatus = null;

    EventServiceImpl eventService;

    public ListenersDisabler() {

        eventService = (EventServiceImpl) Framework.getService(EventService.class);

    }

    public void addListener(String inName) {

        if (StringUtils.isBlank(inName)) {
            throw new IllegalArgumentException();
        }

        EventListenerDescriptor desc = eventService.getEventListener(inName);
        if (desc == null) {
            log.warn("Listener <" + inName + "> not found. Cannot enable/disable it.");
        }

        if (listeners.get(inName) == null) {
            listeners.put(inName, desc);
        }

    }

    public void addListeners(String... inNames) {

        for (String oneName : inNames) {
            addListener(oneName);
        }
    }

    public void disableListeners() {

        originalStatus = new HashMap<String, Boolean>();
        for (Entry<String, EventListenerDescriptor> entry : listeners.entrySet()) {
            EventListenerDescriptor desc = entry.getValue();
            originalStatus.put(entry.getKey(), desc.isEnabled());
            desc.setEnabled(false);
        }

        eventService.getListenerList().recomputeEnabledListeners();

    }

    public void restoreListeners() {

        if (originalStatus == null) {
            throw new RuntimeException("You must call disableListeners() before restoring them.");
        }
        
        for (Entry<String, Boolean> entry : originalStatus.entrySet()) {
            String name = entry.getKey();
            boolean status = entry.getValue();
            listeners.get(name).setEnabled(status);
            
        }

        eventService.getListenerList().recomputeEnabledListeners();

    }
    
    public void reset() {
        
        listeners = new HashMap<String, EventListenerDescriptor>();
        originalStatus = null;
        
    }

}
