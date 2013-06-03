/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.forgerock.openicf.salesforce;

import java.sql.Types;

/**
 * The SOQL parameter / util class
 * 
 * see http://www.salesforce.com/us/developer/docs/soql_sosl/index_Left.htm
 * 
 * @author Hiroyuki Wada
 * 
 */
public final class SOQLParam {

    private String _name;
    private Object _value;
    private int _soqlType;

    /**
     * The SOQL param is a pair of value and its soqlType
     * 
     * @param name
     *            name of the attribute
     * @param value
     *            value
     * @param soqlType
     *            sql type
     */
    public SOQLParam(String name, Object value, int soqlType) {
        if (name == null || name.length() == 0) {
            // TODO localize this
            throw new IllegalArgumentException(
                    "SOQL param name should be not null");
        }
        _name = name;
        _value = value;
        _soqlType = soqlType;
    }

    /**
     * The SOQL param is a pair of value and its soqlType
     * 
     * @param name
     *            name of the attribute
     * @param value
     *            value
     */
    public SOQLParam(String name, Object value) {
        if (name == null || name.length() == 0) {
            // TODO localize this
            throw new IllegalArgumentException(
                    "SQL param name should be not null");
        }
        _name = name;
        _value = value;
        _soqlType = Types.NULL;
    }

    /**
     * Accessor for the name property
     * 
     * @return the _name
     */
    public String getName() {
        return _name;
    }

    /**
     * The param value
     * 
     * @return a value
     */
    public Object getValue() {
        return _value;
    }

    /**
     * Sql Type
     * 
     * @return a type
     */
    public int getsoqlType() {
        return _soqlType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        SOQLParam other = (SOQLParam) obj;
        return (_name == other._name || (_name != null && _name
                .equals(other._name)))
                && (_value == other._value || (_value != null && _value
                        .equals(other._value))) && _soqlType == other._soqlType;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == _name ? 0 : _name.hashCode());
        hash = 31 * hash + (null == _value ? 0 : _value.hashCode());
        hash = 31 * hash + _soqlType;
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        if (getName() != null) {
            ret.append(getName());
            ret.append("=");
        }
        ret.append("\"" + getValue() + "\"");
        switch (getsoqlType()) {
        case Types.ARRAY:
            ret.append(":[ARRAY]]");
            break;
        case Types.BIGINT:
            ret.append(":[BIGINT]");
            break;
        case Types.BINARY:
            ret.append(":[BINARY]");
            break;
        case Types.BIT:
            ret.append(":[BIT]");
            break;
        case Types.BLOB:
            ret.append(":[BLOB]");
            break;
        case Types.BOOLEAN:
            ret.append(":[BOOLEAN]");
            break;
        case Types.CHAR:
            ret.append(":[CHAR]");
            break;
        case Types.CLOB:
            ret.append(":[CLOB]");
            break;
        case Types.DATALINK:
            ret.append(":[DATALINK]");
            break;
        case Types.DATE:
            ret.append(":[DATE]");
            break;
        case Types.DECIMAL:
            ret.append(":[DECIMAL]");
            break;
        case Types.DISTINCT:
            ret.append(":[DISTINCT]");
            break;
        case Types.DOUBLE:
            ret.append(":[DOUBLE]");
            break;
        case Types.FLOAT:
            ret.append(":[FLOAT]");
            break;
        case Types.INTEGER:
            ret.append(":[INTEGER]");
            break;
        case Types.JAVA_OBJECT:
            ret.append(":[JAVA_OBJECT]");
            break;
        case Types.LONGVARBINARY:
            ret.append(":[LONGVARBINARY]");
            break;
        case Types.LONGVARCHAR:
            ret.append(":[LONGVARCHAR]");
            break;
        case Types.NULL:
            break;
        case Types.NUMERIC:
            ret.append(":[NUMERIC]");
            break;
        case Types.OTHER:
            ret.append(":[OTHER]");
            break;
        case Types.REAL:
            ret.append(":[REAL]");
            break;
        case Types.REF:
            ret.append(":[REF]");
            break;
        case Types.SMALLINT:
            ret.append(":[SMALLINT]");
            break;
        case Types.STRUCT:
            ret.append(":[STRUCT]");
            break;
        case Types.TIME:
            ret.append(":[TIME]");
            break;
        case Types.TIMESTAMP:
            ret.append(":[TIMESTAMP]");
            break;
        case Types.TINYINT:
            ret.append(":[TINYINT]");
            break;
        case Types.VARBINARY:
            ret.append(":[VARBINARY]");
            break;
        case Types.VARCHAR:
            ret.append(":[VARCHAR]");
            break;
        default:
            ret.append(":[SOQL Type:" + getsoqlType() + "]");
        }
        return ret.toString();

    }

}
