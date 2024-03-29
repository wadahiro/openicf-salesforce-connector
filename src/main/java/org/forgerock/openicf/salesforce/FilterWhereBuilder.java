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
 * The Filter Where builder is component intended to be used within subclass of
 * <code>AbstractFilterTranslator</code> to help create the Salesforce Object
 * Query Language (SOQL) WHERE query clause.
 * <p>
 * The main functionality of this helper class is create SOQL WHERE query clause
 * </p>
 * <p>
 * The builder can return a List of params to be used within preparedStatement
 * creation
 * <p>
 * <p>
 * About SOQL, see http://www.salesforce.com/us/developer/docs/soql_sosl/
 * </p>
 * 
 * @author Hiroyuki Wada
 */
public class FilterWhereBuilder {

    private boolean in;
    private StringBuilder where = new StringBuilder();

    /**
     * Compound join operator
     * 
     * @param operator
     *            AND/OR
     * @param l
     *            left <CODE>FilterQueryBuiler</CODE>
     * @param r
     *            right <CODE>FilterQueryBuiler</CODE>
     */
    public void join(final String operator, final FilterWhereBuilder l,
            final FilterWhereBuilder r) {
        this.in = true;
        if (l.isIn())
            where.append("( ");
        where.append(l.getWhere());
        if (l.isIn())
            where.append(" )");
        where.append(" ");
        where.append(operator);
        where.append(" ");
        if (r.isIn())
            where.append("( ");
        where.append(r.getWhere());
        if (r.isIn())
            where.append(" )");
    }

    /**
     * @return the where
     */
    public StringBuilder getWhere() {
        return where;
    }

    /**
     * Add name value pair bindings with operator, this is lazy bindings
     * resolved at {@link #getWhereClause()} The names are quoted using the
     * {@link #columnQuote} value
     * 
     * @see FilterWhereBuilder#getWhereClause()
     * 
     * @param param
     *            value to builder
     * @param operator
     *            an operator to compare
     * @param index
     */
    public void addBind(final SOQLParam param, final String operator) {
        if (param == null)
            throw new IllegalArgumentException("null.param.not.suported");
        where.append(param.getName());
        where.append(" ").append(operator).append(" ");

        switch (param.getsoqlType()) {
        case Types.VARCHAR:
            where.append("'");
            where.append(param.getValue());
            where.append("'");
            break;

        default:
            where.append(param.getValue());
            break;
        }
    }

    /**
     * Add null value The names are quoted using the {@link #columnQuote} value
     * 
     * @see FilterWhereBuilder#getWhereClause()
     * 
     * @param name
     *            of the column
     * @param operator
     *            an operator to compare
     * @param param
     *            value to builder
     * @param index
     */
    public void addNull(final String name) {
        where.append(name);
        where.append(" IS NULL");
    }

    /**
     * There is a need to put the content into brackets
     * 
     * @return boolean a in
     */
    public boolean isIn() {
        return in;
    }

    /**
     * @param columnQuote
     *            The required quote type
     * @return the where clause as a String
     */
    public String getWhereClause() {
        return this.getWhere().toString();
    }
}
