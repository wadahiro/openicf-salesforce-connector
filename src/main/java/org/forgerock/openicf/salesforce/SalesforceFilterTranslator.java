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

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

/**
 * DatabaseFilterTranslator abstract class translate filters to database WHERE
 * clause The resource specific getAttributeName must be provided in real
 * translator
 * 
 * @author Hiroyuki Wada
 * 
 */
public class SalesforceFilterTranslator extends
        AbstractFilterTranslator<FilterWhereBuilder> {

    ObjectClass oclass;
    OperationOptions options;
    SalesforceConnector connector;

    /**
     * DatabaseFilterTranslator translate filters to database WHERE clause
     * 
     * @param oclass
     *            the object class
     * @param options
     *            the filter options
     */
    public SalesforceFilterTranslator(ObjectClass oclass,
            OperationOptions options, SalesforceConnector connector) {
        this.oclass = oclass;
        this.options = options;
        this.connector = connector;
    }

    /**
     * @return
     */
    protected FilterWhereBuilder createBuilder() {
        return new FilterWhereBuilder();
    }

    @Override
    protected FilterWhereBuilder createAndExpression(
            FilterWhereBuilder leftExpression,
            FilterWhereBuilder rightExpression) {
        FilterWhereBuilder build = createBuilder();
        build.join("AND", leftExpression, rightExpression);
        return build;
    }

    @Override
    protected FilterWhereBuilder createOrExpression(
            FilterWhereBuilder leftExpression,
            FilterWhereBuilder rightExpression) {
        FilterWhereBuilder build = createBuilder();
        build.join("OR", leftExpression, rightExpression);
        return build;
    }

    @Override
    protected FilterWhereBuilder createEqualsExpression(EqualsFilter filter,
            boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null) {
            return null;
        }
        final FilterWhereBuilder ret = createBuilder();
        if (not)
            ret.getWhere().append("NOT ");
        // Normalize NULLs
        if (param.getValue() == null) {
            ret.addNull(param.getName());
            return ret;
        }
        ret.addBind(param, "=");
        return ret;
    }

    @Override
    protected FilterWhereBuilder createContainsExpression(
            ContainsFilter filter, boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null || param.getValue() == null
                || !(param.getValue() instanceof String)) {
            // Null value filter is not supported
            return null;
        }

        String value = (String) param.getValue();

        final FilterWhereBuilder ret = createBuilder();
        if (not)
            ret.getWhere().append("NOT ");
        // To be sure, this is not already quoted
        if (!value.startsWith("%")) {
            value = "%" + value;
        }
        if (!value.endsWith("%")) {
            value = value + "%";
        }
        ret.addBind(new SOQLParam(param.getName(), value, param.getsoqlType()),
                "LIKE");
        return ret;
    }

    @Override
    protected FilterWhereBuilder createEndsWithExpression(
            EndsWithFilter filter, boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null || param.getValue() == null
                || !(param.getValue() instanceof String)) {
            // Null value filter is not supported
            return null;
        }

        String value = (String) param.getValue();

        final FilterWhereBuilder ret = createBuilder();
        if (not)
            ret.getWhere().append("NOT ");
        // To be sure, this is not already quoted
        if (!value.startsWith("%")) {
            value = "%" + value;
        }
        ret.addBind(new SOQLParam(param.getName(), value, param.getsoqlType()),
                "LIKE");
        return ret;
    }

    @Override
    protected FilterWhereBuilder createStartsWithExpression(
            StartsWithFilter filter, boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null || param.getValue() == null
                || !(param.getValue() instanceof String)) {
            // Null value filter is not supported
            return null;
        }

        String value = (String) param.getValue();

        final FilterWhereBuilder ret = createBuilder();
        if (not)
            ret.getWhere().append("NOT ");
        // To be sure, this is not already quoted
        if (!value.endsWith("%")) {
            value = value + "%";
        }
        ret.addBind(new SOQLParam(param.getName(), value, param.getsoqlType()),
                "LIKE");
        return ret;
    }

    @Override
    protected FilterWhereBuilder createGreaterThanExpression(
            GreaterThanFilter filter, boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null || param.getValue() == null) {
            return null;
        }
        final FilterWhereBuilder ret = createBuilder();
        final String op = not ? "<=" : ">";
        ret.addBind(param, op);
        return ret;
    }

    @Override
    protected FilterWhereBuilder createGreaterThanOrEqualExpression(
            GreaterThanOrEqualFilter filter, boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null || param.getValue() == null) {
            return null;
        }

        final FilterWhereBuilder ret = createBuilder();
        final String op = not ? "<" : ">=";
        ret.addBind(param, op);
        return ret;
    }

    @Override
    protected FilterWhereBuilder createLessThanExpression(
            LessThanFilter filter, boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null || param.getValue() == null) {
            return null;
        }

        final FilterWhereBuilder ret = createBuilder();
        final String op = not ? ">=" : "<";
        ret.addBind(param, op);
        return ret;
    }

    @Override
    protected FilterWhereBuilder createLessThanOrEqualExpression(
            LessThanOrEqualFilter filter, boolean not) {
        final Attribute attribute = filter.getAttribute();
        if (!validateSearchAttribute(attribute)) {
            return null;
        }

        SOQLParam param = getSQLParam(attribute, oclass, options);
        if (param == null || param.getValue() == null) {
            return null;
        }

        final FilterWhereBuilder ret = createBuilder();
        final String op = not ? ">" : "<=";
        ret.addBind(param, op);
        return ret;
    }

    /**
     * Get the SQLParam for given attribute
     * 
     * @param attribute
     *            to translate
     * @return the expected SQLParam, or null if filter not supported
     *         {@link java.sql.Types}
     */
    protected SOQLParam getSQLParam(Attribute attribute, ObjectClass oclass,
            OperationOptions options) {
        final Object value = AttributeUtil.getSingleValue(attribute);
        final String columnName = connector.getColumnName(attribute.getName());
        final Integer columnType = connector.getColumnType(columnName);
        return new SOQLParam(columnName, value, columnType);
    }

    /**
     * Validate the attribute to supported search types
     * 
     * @param singleValue
     */
    protected boolean validateSearchAttribute(final Attribute attribute) {
        // Ignore streamed ( byte[] objects ) from query
        if (byte[].class.equals(AttributeUtil.getSingleValue(attribute)
                .getClass())) {
            return false;
        }
        // Otherwise let the database process
        return true;
    }
}
