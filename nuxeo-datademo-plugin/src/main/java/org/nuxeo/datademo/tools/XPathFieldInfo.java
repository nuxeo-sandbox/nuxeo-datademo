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

/**
 * utility class used to store information about a field via its xpath, so it
 * can be directly used with APIs such as get/setPropertyValue().
 * <p>
 * To handle complex types and multivalued-complex types, check
 * getComplexListParentXPath() is not null or empty, then build the final xpath,
 * which is:
 * <p>
 * <pre>
 * complexParentXPath /indice/subFieldName
 * </pre>
 * So for example: <code>myschema:my_complex_list/0/list_of_dates</code>.
 * <p>
 * You should first check the field is not empty using
 * <code>getPropertyValue(complexParentXPath);</code>
 *
 * @since 7.2
 */
public class XPathFieldInfo {

    protected String xpath;

    protected String coreTypeName;

    protected boolean isList;

    protected String complexListParentXPath;

    protected String subFieldName;

    public XPathFieldInfo(String inXPath) {
        this(inXPath, false, null);
    }

    public XPathFieldInfo(String inXPath, boolean inIsList) {
        this(inXPath, inIsList, null);
    }

    public XPathFieldInfo(String inXPath, boolean inIsList,
            String inCoreTypeName) {
        xpath = inXPath;
        isList = inIsList;
        coreTypeName = inCoreTypeName;
    }

    protected void updateSubFieldName() {
        if (complexListParentXPath != null && !complexListParentXPath.isEmpty()) {
            subFieldName = xpath.replace(complexListParentXPath + "/", "");
        } else {
            subFieldName = null;
        }
    }

    public String toString() {
        String str = xpath;

        if (coreTypeName != null && !coreTypeName.isEmpty()) {
            str += "/" + coreTypeName;
        }

        if (isList) {
            str += "/List";
        } else {
            str += "/Simple";
        }

        if (!complexListParentXPath.isEmpty()) {
            str += "/" + complexListParentXPath;
        }

        return str;
    }

    public String getXPath() {
        return xpath;
    }

    public boolean isSimple() {
        return !isList;
    }

    public boolean isList() {
        return isList;
    }

    public String getComplexListParentXPath() {
        return complexListParentXPath;
    }

    public void setComplexListParentXPath(String inValue) {
        complexListParentXPath = inValue;
    }

    public String getSubFieldName() {
        return subFieldName;
    }

    public String getCoreTypeName() {
        return coreTypeName;
    }

    public void setCoreTypeName(String inValue) {
        this.coreTypeName = inValue;
    }

}
