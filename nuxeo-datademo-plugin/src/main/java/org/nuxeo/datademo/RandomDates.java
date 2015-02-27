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

import java.util.Calendar;

import org.nuxeo.datademo.tools.ToolsMisc;

/**
 * Class handling date utilities.
 *
 * The class stores "now" when it is first instantiated, and method referencing
 * "today" will use this static value. If you intend to use the class for a long
 * time, where "today" could become inaccurate, then call
 * <code>setUseStaticToday(false)</code>. By default, we use the once shot date
 * because it is then faster in huge loop (we save the picoseconds of
 * instantiating <code>Calendar.getInstance()</code>)
 *
 *
 * @since 7.1
 */
public class RandomDates {

    protected static Calendar today = Calendar.getInstance();

    protected static boolean useStaticToday = false;

    /**
     * Some methods can build dates relative to "today". By default, this
     * utility class stores "today" once for all, which can be a problem if you
     * intend to use it for a long time. In this case, call this method passing
     * it <cde>false</false>.
     *
     * @param inValue
     *
     * @since TODO
     */
    public static void setUseStaticToday(boolean inValue) {
        useStaticToday = inValue;

        if (useStaticToday) {
            today = Calendar.getInstance();
        }
    }

    /**
     * Wrapper for the main method
     * <code>addDays(Calendar inDate, int inDays, boolean inMaxIsToday)</code>,
     * setting <code>inMaxIsToday</code> to <code>false</false>
     *
     * @param inDate
     * @param inDays
     * @return
     *
     * @since TODO
     */
    public static Calendar addDays(Calendar inDate, int inDays) {

        return addDays(alignDateIfNeeded(inDate), inDays, false);

    }

    /**
     * Return a Calendar date equals to <code>inDate</code> +/-
     * <code>inDays</code>. If <code>inMaxIsToday</inMaxIsToday>
     * is true and <code>inDays</code> is positive, then "today" will be applied
     * if a random value is past "today"
     * <p>
     * Notice that despite the name "addDate", you can pass negative
     * <code>inDays</code>, so the method returns a date that is lower than
     * <code>inDate</code
     * <p>
     * <code>inDate</code> cannot be null.
     *
     * @param inDate
     * @param inDays
     * @return
     *
     * @since 7.1
     */
    public static Calendar addDays(Calendar inDate, int inDays,
            boolean inMaxIsToday) {
        
        inDate = alignDateIfNeeded(inDate);

        if (inDays == 0) {
            return inDate;
        }

        Calendar d = (Calendar) inDate.clone();

        d.add(Calendar.DATE, inDays);
        if (inMaxIsToday) {
            if (useStaticToday) {
                if (d.after(today)) {
                    d = (Calendar) today.clone();
                }
            } else {
                Calendar newToday = Calendar.getInstance();
                if (d.after(newToday)) {
                    d = (Calendar) newToday.clone();
                }
            }
        }

        return d;
    }

    /**
     * If <code>inFrom</code> is null, the returned date is relative to "now".
     * <p>
     * To return a date which is older than <code>inFrom</code>, set
     * <code>inRewind</code> to true.
     *
     * @param inFrom, if null, the class uses "now" (depending on the
     *            <code>setUseStaticToday()</code> value)
     *
     * @since 7.1
     */
    public static Calendar buildDate(Calendar inFrom, int inDaysFrom,
            int inDaysTo, boolean inRewind) {

        Calendar result;
        
        inFrom = alignDateIfNeeded(inFrom);
        result = (Calendar) inFrom.clone();
        result.add(Calendar.DATE, ToolsMisc.randomInt(inDaysFrom, inDaysTo)
                * (inRewind ? -1 : 1));

        return result;
    }

    /**
     * Return a Calendar array of <code>inCount</code> length. For each element,
     * call {@link buildDate} is called.
     * <p>
     * If <code>inFrom</code> is null, the returned date is relative to "now".
     * <p>
     * To return a date which is older than <code>inFrom</code>, set
     * <code>inRewind</code> to true.
     * 
     * @param inCount
     * @param inFrom
     * @param inDaysFrom
     * @param inDaysTo
     * @param inRewind
     * @return
     *
     * @since 7.2
     */
    public static Calendar[] buildDates(int inCount, Calendar inFrom,
            int inDaysFrom, int inDaysTo, boolean inRewind) {

        Calendar[] dates = new Calendar[inCount];

        Calendar from = alignDateIfNeeded(inFrom);
        for (int i = 0; i < inCount; i++) {
            dates[i] = buildDate(from, inDaysFrom, inDaysTo, inRewind);
        }

        return dates;
    }

    public static Calendar[] buildDates(Calendar[] inFrom, int inDaysFrom,
            int inDaysTo, boolean inRewind) {

        Calendar[] dates = new Calendar[inFrom.length];

        int max = inFrom.length;
        for (int i = 0; i < max; i++) {
            dates[i] = buildDate(inFrom[i], inDaysFrom, inDaysTo, inRewind);
        }

        return dates;
    }

    /**
     * Add <code>inDays</code> to <code>inDate</code> (<code>inDays</code> can
     * be a negative value).
     * <p>
     * If the result is > <code>inMax</code> it is set to <code>inMax</code>.
     * 
     * @param inDate
     * @param inDays
     * @param inMax
     * @return
     *
     * @since 7.2
     */
    public static Calendar addDays(Calendar inDate, int inDays, Calendar inMax) {

        Calendar result = alignDateIfNeeded(inDate);
        result.add(Calendar.DATE, inDays);

        if (result.after(inMax)) {
            result = (Calendar) inMax.clone();
        }
        
        return null;
    }

    /**
     * Add inDaysFrom to inDaysTo to inDate, with a limit: If the result is >
     * <code>inMax</code>, it is set to <code>inMax</code>.
     * 
     * @param inDate
     * @param inDaysFrom
     * @param inDaysTo
     * @param inMax
     * @return
     *
     * @since 7.2
     */
    public static Calendar addDays(Calendar inDate, int inDaysFrom,
            int inDaysTo, Calendar inMax) {

        Calendar result;
        
        inDate = alignDateIfNeeded(inDate);

        if (inMax.before(inDate)) {
            throw new IllegalArgumentException(
                    "Thee max. date should be greater than the start date");
        }

        result = (Calendar) inDate.clone();
        result.add(Calendar.DATE, ToolsMisc.randomInt(inDaysFrom, inDaysTo));
        if (result.after(inMax)) {
            result = (Calendar) inMax.clone();
        }

        return result;
    }
    
    private static Calendar alignDateIfNeeded(Calendar inDate) {
        
        if(inDate == null) {
            if (useStaticToday) {
                return (Calendar) today.clone();
            } else {
                return Calendar.getInstance();
            }
        } else {
            return inDate;
        }
    }

}
