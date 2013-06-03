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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.engine.http.header.ChallengeWriter;
import org.restlet.engine.security.AuthenticatorHelper;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

/**
 * Class to represent a Salesforce Connection
 * 
 * @author Hiroyuki Wada
 */
public class SalesforceConnection extends ClientResource {

    /**
     * Setup logging for the {@link SalesforceConnector}.
     */
    private static final Log log = Log.getLog(SalesforceConnection.class);

    public static final String INSTANCE_URL = "instance_url";

    public static final String ACCESS_TOKEN = "access_token";

    public static final String CLIENT_ID = "client_id";

    public static final String CLIENT_SECRET = "client_secret";

    public static final String ERROR = "error";

    public static final String ERROR_DESC = "error_description";

    public static final String ERROR_URI = "error_uri";

    public static final String EXPIRES_IN = "expires_in";

    public static final String GRANT_TYPE = "grant_type";

    public static final String PASSWORD = "password";

    public static final String SCOPE = "scope";

    public static final String STATE = "state";

    public static final String USERNAME = "username";

    public static final String SIGNATURE = "signature";

    private SalesforceConfiguration configuration;

    private OAuthUser authentication = null;

    public SalesforceConnection(SalesforceConnection resource) {
        super(resource);
        this.configuration = resource.configuration;
    }

    public SalesforceConnection(SalesforceConfiguration configuration) {
        super(new Context(), configuration.getLoginUrl());
        this.configuration = configuration;

        List<Protocol> p = new ArrayList<Protocol>();
        p.add(Protocol.HTTPS);
        // Client client = new Client(getContext(), p ,
        // "org.restlet.ext.httpclient.HttpClientHelper");
        Client client = new Client(Protocol.HTTPS);

        String trustStore = this.configuration.getTrustStore();
        if (StringUtil.isNotEmpty(trustStore)) {
            Series<Parameter> parameters = getContext().getParameters();
            parameters.add("truststorePath", trustStore);
        }

        client.setContext(getContext());
        setNext(client);

        Engine.getInstance().getRegisteredAuthenticators()
                .add(new OAuthHelper());

        refreshOAuthToken();
    }

    public void test() {
        Representation body = null;
        try {
            ClientResource rc = getChild(this.configuration.getServicePath());
            body = rc.get();
        } catch (Exception e) {
            throw new ConnectionFailedException(e);
        } finally {
            if (body != null)
                body.release();
        }
    }

    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    public ClientResource getChild(Reference relativeRef)
            throws ResourceException {
        ClientResource result = null;

        if ((relativeRef != null) && relativeRef.isRelative()) {
            result = new SalesforceConnection(this);
            result.setReference(new Reference(
                    authentication.getBaseReference(), relativeRef)
                    .getTargetRef());
            // -------------------------------------
            // Add user-defined extension headers
            // -------------------------------------

            // Can't append Authorization header with restlet 2.0.15
            //
            // Series additionalHeaders = (Series) result.getRequest()
            // .getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
            // if (additionalHeaders == null) {
            // additionalHeaders = new Form();
            // result.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
            // additionalHeaders);
            // }
            //
            // additionalHeaders.add(HeaderConstants.HEADER_AUTHORIZATION,
            // authentication.getAccessToken());

            ChallengeResponse challengeResponse = new ChallengeResponse(
                    ChallengeScheme.HTTP_OAUTH);
            challengeResponse.setRawValue(authentication.getAccessToken());
            result.getRequest().setChallengeResponse(challengeResponse);

        } else {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "The child URI is not relative.");
        }
        return result;
    }

    public void refreshOAuthToken() {
        // Accept: application/json
        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(
                1);
        acceptedMediaTypes.add(new Preference(MediaType.APPLICATION_JSON));
        getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);

        Form form = configuration.getAuthenticationForm();
        Representation body = null;

        try {
            body = post(form.getWebRepresentation());

            if (getStatus().isSuccess()) {
                if (body instanceof EmptyRepresentation == false) {
                    authentication = createJson(new JacksonRepresentation<Map>(
                            body, Map.class));
                }
            }
        } catch (Exception e) {
            throw new ConnectionFailedException(e);
        } finally {
            if (body != null)
                body.release();
            release();
        }
    }

    /**
     * Converts successful JSON token body responses to OAuthUser.
     * 
     * @param body
     *            Representation containing a successful JSON body element.
     * @return OAuthUser object containing accessToken, refreshToken and
     *         expiration time.
     */
    public OAuthUser createJson(JacksonRepresentation<Map> body) {
        /*
         * { "id" :
         * "https://login.salesforce.com/id/00Dd0000000bkONEAY/005d0000000uqvbAAA"
         * , "issued_at" : "1325869693249", "instance_url" :
         * "https://na14.salesforce.com", "signature" :
         * "dUaMkN5HSskfclyE8uol9Wn3vg6rdJLdZXK5hFkM9TE=", "access_token" :
         * "00Dd0000000bkON!AQ4AQGtHVon9uQuQw28DVX6V6OP.6LRhnItGj0PpRcMO_w4giGTKbvSXBYHcKtx8sKm4lNiDJoRyA4EdwrPXArRPIIMp_IGh"
         * }
         */

        Logger log = Context.getCurrentLogger();

        Map answer = body != null ? body.getObject() : null;

        if (null != answer) {
            String accessToken = null;
            if (answer.get(ACCESS_TOKEN) instanceof String) {
                accessToken = (String) answer.get(ACCESS_TOKEN);
                log.fine("AccessToken = " + accessToken);
            }

            String signature = null;
            if (answer.get(SIGNATURE) instanceof String) {
                signature = (String) answer.get(SIGNATURE);
                log.fine("Signature = " + signature);
            }

            String instanceUrl = null;
            if (answer.get(INSTANCE_URL) instanceof String) {
                instanceUrl = (String) answer.get(INSTANCE_URL);
                log.fine("InstanceUrl = " + instanceUrl);
            }

            String id = null;
            if (answer.get("id") instanceof String) {
                id = (String) answer.get("id");
                log.fine("Id = " + signature);
            }

            Date issued = null;
            if (answer.get("issued_at") instanceof String) {
                issued = new Date(Long.parseLong((String) answer
                        .get("issued_at")));
                log.fine("Issued at = " + issued);
            }

            return new OAuthUser(id, issued, instanceUrl, signature,
                    accessToken);
        }
        return null;
    }

    class OAuthUser {

        private final String id;
        private final Date issued;
        private final Reference instanceUrl;
        private final String signature;
        private final String accessToken;

        public OAuthUser(String id, Date issued, String instanceUrl,
                String signature, String accessToken) {
            this.id = id;
            this.issued = issued;
            this.instanceUrl = new Reference(instanceUrl);
            this.signature = signature;
            ChallengeWriter cw = new ChallengeWriter();
            // cw.append(ChallengeScheme.HTTP_OAUTH.getTechnicalName()).appendSpace().append(accessToken);
            // this.accessToken = cw.toString();
            this.accessToken = accessToken;
        }

        public Reference getBaseReference() {
            return instanceUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }
    }

    class OAuthHelper extends AuthenticatorHelper {

        public OAuthHelper() {
            super(ChallengeScheme.HTTP_OAUTH, true, false);
        }

        @Override
        public void formatRawResponse(ChallengeWriter cw,
                ChallengeResponse challenge, Request request,
                Series<Parameter> httpHeaders) {
            try {
                if (challenge == null) {
                    throw new RuntimeException(
                            "No challenge provided, unable to encode credentials");
                } else {
                    cw.append(challenge.getRawValue());
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unexpected exception, unable to encode credentials", e);
            }
        }
    }
}
