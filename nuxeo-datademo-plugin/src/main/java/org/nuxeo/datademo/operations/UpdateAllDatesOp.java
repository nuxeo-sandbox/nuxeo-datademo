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
@Operation(id=UpdateAllDatesOp.ID, category=Constants.CAT_SERVICES, label="Update All Dates", description="Update all date fields. Default values: <code>disableListeners=true</code> and <code>newThread=true</code> ")
public class UpdateAllDatesOp {

    public static final String ID = "UpdateAllDatesOp";

    @Context
    protected CoreSession session;

    @Param(name = "numberOfDays", required = true)
    protected long numberOfDays;

    @Param(name = "disableListeners", required = false, values = { "true" })
    protected boolean disableListeners = true;

    @Param(name = "inWorker", required = false, values = { "true" })
    protected boolean inWorker = true;

    @OperationMethod
    public void run() {
        
        if(inWorker) {
            
            UpdateAllDates.runInWorker((int) numberOfDays, disableListeners);
            
        } else {
            UpdateAllDates uad = new UpdateAllDates(session, (int) numberOfDays);
            uad.run(disableListeners);
        }
        
    }

}
