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

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.restlet.data.Form;
import org.restlet.data.Reference;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Salesforce Connector.
 * 
 * @author Hiroyuki Wada
 * @see <a herf=
 *      "http://wiki.developerforce.com/page/Digging_Deeper_into_OAuth_2.0_on_Force.com"
 *      > Digging Deeper into OAuth 2.0 on Force.com</a>
 */
public class SalesforceConfiguration extends AbstractConfiguration {

    // Exposed configuration properties.

    /**
     * The Consumer Key
     */
    private String clientId = null;

    /**
     * The Consumer Secret
     */
    private GuardedString clientSecret = null;

    /**
     * The Username to authenticate with..
     */
    private String username;

    /**
     * The Password to authenticate with.
     */
    private GuardedString password = null;

    /**
     * The API type of Salesforce (scim, rest, soap...) Currently rest is
     * supported.
     */
    private String apiType = "rest";

    private String uniqueAttribute = "Id";
    private String nameAttribute = "Username";
    private String passwordAttribute = "Password";
    private String activeAttribute = "IsActive";
    private String trustStore = null;
    private String loginUrl = "https://login.salesforce.com/services/oauth2/token";
    private String servicePath = "services/data/v27.0";
    private boolean ignorePasswordError = true;

    /**
     * The Password to authenticate with.
     * <p/>
     * When accessing salesforce.com from outside of your company’s trusted
     * networks, you must add a security token to your password.
     */
    private GuardedString security_token = null;

    @ConfigurationProperty(order = 1, displayMessageKey = "CLIENTID_PROPERTY_DISPLAY", helpMessageKey = "CLIENTID_PROPERTY_HELP", required = true, confidential = false)
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String remoteUser) {
        this.clientId = remoteUser;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "CLIENTSECRET_PROPERTY_DISPLAY", helpMessageKey = "CLIENTSECRET_PROPERTY_HELP", required = true, confidential = true)
    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString password) {
        this.clientSecret = password;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "USERNAME_PROPERTY_DISPLAY", helpMessageKey = "USERNAME_PROPERTY_HELP", required = true)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "PASSWORD_PROPERTY_DISPLAY", helpMessageKey = "PASSWORD_PROPERTY_HELP", required = true, confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "SECURITY_TOKEN_PROPERTY_DISPLAY", helpMessageKey = "SECURITY_TOKEN_PROPERTY_HELP", confidential = true)
    public GuardedString getSecurityToken() {
        return security_token;
    }

    public void setSecurityToken(GuardedString security_token) {
        this.security_token = security_token;
    }

    @ConfigurationProperty(order = 7, displayMessageKey = "API_TYPE_HELP", helpMessageKey = "API_TYPE_HELP")
    public String getApiType() {
        return apiType;
    }

    @ConfigurationProperty(displayMessageKey = "UI_FLAT_UNIQUE_ATTRIBUTE", helpMessageKey = "UI_FLAT_UNIQUE_ATTRIBUTE_HELP", required = true)
    public String getUniqueAttribute() {
        return uniqueAttribute;
    }

    public void setUniqueAttribute(String uniqueAttribute) {
        this.uniqueAttribute = uniqueAttribute;
    }

    @ConfigurationProperty(displayMessageKey = "UI_FLAT_NAME_ATTRIBUTE", helpMessageKey = "UI_FLAT_NAME_ATTRIBUTE_HELP", required = true)
    public String getNameAttribute() {
        return nameAttribute;
    }

    public void setNameAttribute(String nameAttribute) {
        this.nameAttribute = nameAttribute;
    }

    @ConfigurationProperty(displayMessageKey = "PASSWORD_ATTRIBUTE_PROPERTY_DISPLAY", helpMessageKey = "PASSWORD_ATTRIBUTE_PROPERTY_HELP", required = true)
    public void setPasswordAttribute(String passwordAttribute) {
        this.passwordAttribute = passwordAttribute;
    }

    public String getPasswordAttribute() {
        return passwordAttribute;
    }

    @ConfigurationProperty(displayMessageKey = "ACTIVE_ATTRIBUTE_PROPERTY_DISPLAY", helpMessageKey = "ACTIVE_ATTRIBUTE_PROPERTY_HELP", required = true)
    public void setActiveAttribute(String activeAttribute) {
        this.activeAttribute = activeAttribute;
    }

    public String getActiveAttribute() {
        return activeAttribute;
    }

    @ConfigurationProperty(displayMessageKey = "TRUST_STORE_PROPERTY_DISPLAY", helpMessageKey = "TRUST_STORE_PROPERTY_HELP", required = true)
    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    @ConfigurationProperty(displayMessageKey = "LOGIN_URL_PROPERTY_DISPLAY", helpMessageKey = "LOGIN_URL_PROPERTY_HELP", required = true)
    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    @ConfigurationProperty(displayMessageKey = "IGNORE_PASSWORD_ERROR_PROPERTY_DISPLAY", helpMessageKey = "IGNORE_PASSWORD_ERROR_PROPERTY_HELP", required = true)
    public boolean isIgnorePasswordError() {
        return ignorePasswordError;
    }

    public void setIgnorePasswordError(boolean ignorePasswordError) {
        this.ignorePasswordError = ignorePasswordError;
    }

    @ConfigurationProperty(displayMessageKey = "SERVICE_PATH_PROPERTY_DISPLAY", helpMessageKey = "SERVICE_PATH_PROPERTY_HELP", required = true)
    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getServicePath() {
        return servicePath;
    }

    public String getUserServicePath() {
        return getServicePath() + "/sobjects/User/";
    }

    public String getQueryServicePath() {
        return getServicePath() + "/query/?q=";
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        Assertions.blankCheck(clientId, "clientId");
        Assertions.nullCheck(clientSecret, "clientSecret");
        Assertions.blankCheck(username, "username");
        Assertions.nullCheck(password, "password");
        Assertions.blankCheck(apiType, "apiType");
    }

    public Form getAuthenticationForm() {
        final StringBuilder clear = new StringBuilder();
        GuardedString.Accessor accessor = new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                clear.append(clearChars);
            }
        };

        Form form = new Form();
        form.add(SalesforceConnection.GRANT_TYPE, SalesforceConnection.PASSWORD);
        form.add(SalesforceConnection.USERNAME, getUsername());

        getPassword().access(accessor);
        if (null != getSecurityToken()) {
            getSecurityToken().access(accessor);
        }
        form.add(SalesforceConnection.PASSWORD, clear.toString());

        clear.setLength(0);

        getClientSecret().access(accessor);
        form.add(SalesforceConnection.CLIENT_ID, getClientId());
        form.add(SalesforceConnection.CLIENT_SECRET, clear.toString());

        return form;
    }

}
