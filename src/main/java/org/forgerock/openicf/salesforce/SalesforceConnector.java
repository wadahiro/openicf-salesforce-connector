/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.openicf.salesforce;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * Main implementation of the Salesforce Connector
 * 
 * @author Hiroyuki Wada
 * 
 */
@ConnectorClass(displayNameKey = "SALESFORCE", configurationClass = SalesforceConfiguration.class)
public class SalesforceConnector implements PoolableConnector, AuthenticateOp,
        ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp, ScriptOnConnectorOp,
        ScriptOnResourceOp, SearchOp<FilterWhereBuilder>, SyncOp, TestOp,
        UpdateAttributeValuesOp {
    /**
     * Setup logging for the {@link SalesforceConnector}.
     */
    private static final Log log = Log.getLog(SalesforceConnector.class);

    /**
     * Place holder for the Connection created in the init method
     */
    private SalesforceConnection connection;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link SalesforceConnector#init(org.identityconnectors.framework.spi.Configuration)}
     * .
     */
    private SalesforceConfiguration configuration;

    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration configuration) {
        this.configuration = (SalesforceConfiguration) configuration;
        String apiType = this.configuration.getApiType();
        this.connection = new SalesforceConnection(this.configuration);
    }

    protected void refreshAccessToken() {
        log.info("OAuth2 access token may be expired. Do refesh access token.");
        this.connection.refreshOAuthToken();
    }

    /**
     * Disposes of the {@link SalesforceConnector}'s resources.
     * 
     * @see Connector#dispose()
     */
    public void dispose() {
        configuration = null;
        if (connection != null) {
            connection.release();
            connection = null;
        }
    }

    public void checkAlive() {
        connection.test();
    }

    /******************
     * SPI Operations
     * 
     * Implement the following operations using the contract and description
     * found in the Javadoc for these methods.
     ******************/

    /**
     * {@inheritDoc}
     */
    public Uid authenticate(final ObjectClass objectClass,
            final String userName, final GuardedString password,
            final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass,
            final String userName, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass,
            final Set<Attribute> createAttributes,
            final OperationOptions options) {

        Name name = AttributeUtil.getNameFromAttributes(createAttributes);
        log.info("Create {0}", name);

        if (name == null) {
            return null;
        }

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            return new SalesforceUserOp(this).createUser(createAttributes,
                    options);

        } else if (ObjectClass.GROUP.equals(objectClass)) {
            throw new UnsupportedOperationException();
        } else {
            throw new IllegalArgumentException("Unsupported Object Class="
                    + objectClass.getObjectClassValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, final Uid uid,
            Set<Attribute> replaceAttributes, OperationOptions options) {
        log.info("Update {0}", uid.getUidValue());

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            return new SalesforceUserOp(this).update(uid, replaceAttributes,
                    options);

        } else if (ObjectClass.GROUP.equals(objectClass)) {
            throw new UnsupportedOperationException();
        } else {
            throw new IllegalArgumentException("Unsupported Object Class="
                    + objectClass.getObjectClassValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {
        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            new SalesforceUserOp(this).deleteUser(uid, options);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        JacksonRepresentation<Map> body = null;
        SchemaBuilder schemaBuilder = new SchemaBuilder(
                SalesforceConnector.class);
        try {
            body = new JacksonRepresentation<Map>(connection.getChild(
                    "/services/data/v20.0/sobjects/").get(), Map.class);
            Object o = body.getObject();
            System.out.println(o);
            body.release();
            if (o instanceof Map) {
                List<Map<String, Object>> sobjects = (List<Map<String, Object>>) ((Map) o)
                        .get("sobjects");
                if (null != sobjects) {
                    for (Map<String, Object> sobject : sobjects) {
                        Object urls = sobject.get("urls");
                        if (urls instanceof Map) {
                            Object describe = ((Map) urls).get("describe");
                            System.out.println(describe);
                            if (describe instanceof String
                                    && ((String) describe).contains("/User/")) {
                                body = new JacksonRepresentation<Map>(
                                        connection.getChild((String) describe)
                                                .get(), Map.class);
                                /*
                                 * File root = new
                                 * File(SalesforceConnector.class
                                 * .getResource("/").toURI().resolve("cache/"));
                                 * root = new File(root, ((String) describe));
                                 * root.mkdirs(); FileOutputStream out = new
                                 * FileOutputStream(new File(root,"GET.json"));
                                 * body.write(out);
                                 */
                                Object so = body.getObject();
                                body.release();
                                if (so instanceof Map) {
                                    SalesforceAttributeUtils.parseDescribe(
                                            (Map) so, schemaBuilder);
                                }

                            }
                        }
                    }
                } else {
                    log.error("/services/data/v24.0/sobjects/sobjects is null");
                }
            }
        } finally {
            if (null != body) {
                body.release();
            }
        }
        return schemaBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    public Object runScriptOnConnector(ScriptContext request,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object runScriptOnResource(ScriptContext request,
            OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(
            ObjectClass objectClass, OperationOptions options) {
        return new SalesforceFilterTranslator(objectClass, options, this);
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, FilterWhereBuilder query,
            final ResultsHandler handler, OperationOptions options) {

        if (log.isInfo()) {
            log.info("executeQuery {0}, {1}, {2}", objectClass, query, options);
        }

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            new SalesforceUserOp(this).executeQuery(query, handler, options);

        } else if (ObjectClass.GROUP.equals(objectClass)) {
            throw new UnsupportedOperationException();
        } else {
            throw new IllegalArgumentException("Unsupported Object Class="
                    + objectClass.getObjectClassValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objectClass, SyncToken token,
            SyncResultsHandler handler, final OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        this.connection.test();
    }

    /**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid,
            Set<Attribute> valuesToAdd, OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid,
            Set<Attribute> valuesToRemove, OperationOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * Convert the attribute name to resource specific columnName
     * 
     * @param attributeName
     * @return the Column Name value
     */
    public String getColumnName(String attributeName) {
        if (Name.NAME.equalsIgnoreCase(attributeName)) {
            log.ok("attribute name {0} map to key column", attributeName);
            return configuration.getNameAttribute();
        }
        if (Uid.NAME.equalsIgnoreCase(attributeName)) {
            log.ok("attribute name {0} map to key column", attributeName);
            return configuration.getUniqueAttribute();
        }
        if (!StringUtil.isBlank(configuration.getPasswordAttribute())
                && OperationalAttributes.PASSWORD_NAME
                        .equalsIgnoreCase(attributeName)) {
            log.ok("attribute name {0} map to password column", attributeName);
            return configuration.getPasswordAttribute();
        }
        return attributeName;
    }

    public Integer getColumnType(String columnName) {
        // TODO implements SOQL type
        return Types.VARCHAR;
    }

    SalesforceConnection getConnection() {
        return connection;
    }

}
