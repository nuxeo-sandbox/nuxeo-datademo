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
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * 
 */
@Operation(id=UpdateAllDatesOp.ID, category=Constants.CAT_SERVICES, label="Update Dll Aates", description="UPdate all date fields. Default values: <code>disableListeners=true</code> and <code>newThread=true</code> ")
public class UpdateAllDatesOp {

    public static final String ID = "UpdateAllDatesOp";

    @Context
    protected CoreSession session;

    @Param(name = "numberOfDays", required = true)
    protected long numberOfDays;

    @Param(name = "disableListeners", required = false, values = { "true" })
    protected boolean disableListeners = true;

    @Param(name = "newThread", required = false, values = { "true" })
    protected boolean newThread = true;

    @OperationMethod
    public void run() {

        if(newThread) {
            
            Runnable ualTask = new UpdateAlDatesTask(disableListeners);
            Thread t = new Thread(ualTask, "Nuxeo_UpdateAlDatesTask");
            t.setDaemon(false);
            t.start();
            
        } else {
            UpdateAllDates ual = new UpdateAllDates(session, (int) numberOfDays);
            ual.run(disableListeners);
        }
        
    }

    public class UpdateAlDatesTask implements Runnable {
        
        protected boolean disableListeners;
        
        public UpdateAlDatesTask(boolean inDisableListeners) {
            disableListeners = inDisableListeners;
        }
        
        @Override
        public void run() {
            UpdateAllDates ual = new UpdateAllDates(session, (int) numberOfDays);
            ual.run(disableListeners);
        }
        
    }

}
