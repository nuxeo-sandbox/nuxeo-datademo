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

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.AbstractWork;

/**
 *
 *
 * @since 7.3
 */
public class UpdateAllDatesWorker extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UpdateAllDatesWorker.class);

    // The same as the one used in Workers-Queues.xml
    public static final String CATEGORY_UPDATE_ALL_DATES = "updateAllDates";

    public static final String UPDATE_ALL_DATES_DONE_EVENT = "updateAllDatesDone";

    public static final String UPDATE_ALL_DATES_DONE_STATUS = "Updating all dates: Done";

    protected UpdateAllDates updateDates;

    protected int days = -1;

    protected Date lastUpdate = null;

    protected ArrayList<String> disabledListeners;

    public UpdateAllDatesWorker(int inDays) {

        days = inDays;
    }

    public UpdateAllDatesWorker(Date inLastUpdate) {

        lastUpdate = inLastUpdate;
    }

    @Override
    public String getTitle() {

        return "Data Demo: Update All Dates";
    }

    @Override
    public void work() {

        log.info("Starting the <Updating all dates> work.");

        setStatus("Updating all dates");
        setProgress(Progress.PROGRESS_INDETERMINATE);

        try {
            initSession();
            if(lastUpdate == null) {
                updateDates = new UpdateAllDates(session, days);
            } else {
                updateDates = new UpdateAllDates(session, lastUpdate);
            }
            updateDates.setWorker(this);
            if(disabledListeners != null) {
                for(String name : disabledListeners) {
                    updateDates.addListenerToDisable(name);
                }
            }
            updateDates.run();

        } finally {
            cleanUp(true, null);
        }

        setProgress(Progress.PROGRESS_100_PC);
        setStatus(UPDATE_ALL_DATES_DONE_STATUS);
    }

    public void setListenersToDisable(ArrayList<String> inListeners) {
        disabledListeners = inListeners;
    }

}
