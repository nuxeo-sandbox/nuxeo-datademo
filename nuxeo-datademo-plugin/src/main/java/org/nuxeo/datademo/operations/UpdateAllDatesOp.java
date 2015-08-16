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

package org.nuxeo.datademo.operations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.datademo.UpdateAllDates;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * 
 */
@Operation(id=UpdateAllDatesOp.ID, category=Constants.CAT_SERVICES, label="Update All Dates", description="Update all date fields. Default values: <code>listenersToDisable>/code> (comma separated list) is empty, and <code>newThread=true</code> ")
public class UpdateAllDatesOp {

    public static final String ID = "UpdateAllDatesOp";

    @Context
    protected CoreSession session;

    @Param(name = "numberOfDays", required = true)
    protected long numberOfDays;

    // List of comma-separated values
    @Param(name = "listenersToDisable", required = false)
    protected String listenersToDisable = "";

    @Param(name = "inWorker", required = false, values = { "true" })
    protected boolean inWorker = true;

    @OperationMethod
    public void run() {
        
        ArrayList<String> listenersNames = null;
        
        if(StringUtils.isNotBlank(listenersToDisable)) {
            String[] names = listenersToDisable.trim().split(",");
            listenersNames = new ArrayList<String>();
            for(String oneName : names) {
                listenersNames.add(oneName);
            }
        }
        
        if(inWorker) {
            
            UpdateAllDates.runInWorker((int) numberOfDays, listenersNames);
            
        } else {
            UpdateAllDates uad = new UpdateAllDates(session, (int) numberOfDays);
            if(listenersNames != null) {
                for(String oneName : listenersNames) {
                    uad.addListenerToDisable(oneName);
                }
            }
            uad.run();
        }
        
    }

}
