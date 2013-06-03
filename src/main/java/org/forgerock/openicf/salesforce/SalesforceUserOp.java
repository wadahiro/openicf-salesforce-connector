package org.forgerock.openicf.salesforce;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

public class SalesforceUserOp {

    private static final Log log = Log.getLog(SalesforceUserOp.class);
    private final SalesforceConnector connector;
    private final SalesforceConfiguration configuration;

    SalesforceUserOp(SalesforceConnector connector) {
        this.connector = connector;
        this.configuration = (SalesforceConfiguration) connector
                .getConfiguration();
    }

    public Uid createUser(final Set<Attribute> createAttributes,
            final OperationOptions options) {
        Map map = SalesforceAttributeUtils.toForceJsonMap(this.configuration,
                createAttributes);

        final GuardedString newPassword = (GuardedString) map
                .remove(OperationalAttributes.PASSWORD_NAME);

        JacksonRepresentation jsonRepresentation = new JacksonRepresentation(
                map);

        final Uid[] uid = new Uid[1];

        doPost(this.configuration.getUserServicePath(), jsonRepresentation,
                new Callback() {
                    public void call(Representation body) {
                        JacksonRepresentation<Map> bodyRepresentation = new JacksonRepresentation<Map>(
                                body, Map.class);
                        Map answer = bodyRepresentation != null ? bodyRepresentation
                                .getObject() : null;
                        boolean succcess = (Boolean) answer.get("success");
                        List<String> errors = (List<String>) answer
                                .get("errors");
                        String id = (String) answer.get("id");

                        log.info(
                                "Create Result. success: {0}, errors: {1}, id: {2}",
                                succcess, errors, id);

                        // accountId is Salesforce's id
                        // String accountId =
                        // attrsAccessor.getName().getNameValue();

                        uid[0] = new Uid(id);

                        newPassword.access(new Accessor() {
                            public void access(char[] clearChars) {
                                updatePassword(uid[0],
                                        String.valueOf(clearChars));
                            }
                        });
                    }
                }, new ErrorCallback() {
                    public void call(ResourceException e, Representation body) {
                        JacksonRepresentation<List> bodyRepresentation = new JacksonRepresentation<List>(
                                body, List.class);
                        List<Map> answers = bodyRepresentation != null ? bodyRepresentation
                                .getObject() : null;

                        for (Map answerMap : answers) {
                            Object message = answerMap.get("message");
                            Object errorCode = answerMap.get("errorCode");
                            log.info(
                                    "Create user error. message={0}, errorCode={1}",
                                    message, errorCode);
                            if ("DUPLICATE_USERNAME".equals(errorCode)) {
                                throw new AlreadyExistsException(errorCode
                                        .toString(), e);
                            }
                        }
                        throw new ConnectorException("Create user error.", e);
                    }
                });

        if (uid[0] == null) {
            throw new ConnectorException("Create user error");
        }
        return uid[0];
    }

    public Uid update(final Uid uid, Set<Attribute> replaceAttributes,
            OperationOptions options) {

        Map map = SalesforceAttributeUtils.toForceJsonMap(this.configuration,
                replaceAttributes);

        final GuardedString newPassword = (GuardedString) map
                .remove(OperationalAttributes.PASSWORD_NAME);

        JacksonRepresentation jsonRepresentation = new JacksonRepresentation(
                map);

        final Uid[] uidHolder = new Uid[1];

        String relativeUrl = this.configuration.getUserServicePath()
                + uid.getUidValue() + "?_HttpMethod=PATCH";

        doPost(relativeUrl, jsonRepresentation, new Callback() {
            public void call(Representation body) {

                newPassword.access(new Accessor() {
                    public void access(char[] clearChars) {
                        updatePassword(uid, String.valueOf(clearChars));
                        uidHolder[0] = uid;
                    }
                });
            }
        });

        if (uidHolder[0] == null) {
            throw new ConnectorException("Update user error");
        }

        return uidHolder[0];
    }

    public void deleteUser(final Uid uid, final OperationOptions options) {

        // Salesforce don't allow delete user
        //
        // String relativeUrl = this.configuration.getUserServicePath()
        // + uid.getUidValue();
        //
        // doDelete(relativeUrl, new NothingCallback(), new ErrorCallback() {
        // public void call(ResourceException e, Representation body) {
        // new ConnectorException("delete error", e);
        // }
        // });

        // In case delete use, we update 'IsActive' attribute to false.
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        map.put(this.configuration.getActiveAttribute(), false);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        JacksonRepresentation jsonRepresentation = new JacksonRepresentation(
                map);

        String relativeUrl = this.configuration.getUserServicePath()
                + uid.getUidValue() + "?_HttpMethod=PATCH";

        doPost(relativeUrl, jsonRepresentation, new NothingCallback());
    }

    public void executeQuery(FilterWhereBuilder query,
            final ResultsHandler handler, OperationOptions options) {

        String searchQuery = convertSearchQuery(query, options);
        String relativeUrl = this.configuration.getQueryServicePath()
                + searchQuery;

        doGet(relativeUrl, new Callback() {
            public void call(Representation body) {
                JacksonRepresentation<Map> bodyRepresentation = new JacksonRepresentation<Map>(
                        body, Map.class);
                Map answer = bodyRepresentation != null ? bodyRepresentation
                        .getObject() : null;
                boolean done = (Boolean) answer.get("done");
                List<Map> records = (List<Map>) answer.get("records");
                int totalSize = (Integer) answer.get("totalSize");

                log.info(
                        "Execute Query Result. totalSize: {0}, done: {1}, records: {2}",
                        totalSize, done, records);

                for (Map record : records) {
                    ConnectorObject co = createConnectorObject(record);
                    handler.handle(co);
                }
            }
        }, null);
    }

    private String convertSearchQuery(FilterWhereBuilder query,
            OperationOptions options) {
        String[] alist = SalesforceAttributeUtils.toAttrList(
                this.configuration, options);

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT");
        sb.append("+");

        for (String attr : alist) {
            sb.append(attr);
            sb.append(",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));

        sb.append("+from+User");

        if (query != null) {
            // TODO make salesforce search query
            sb.append("+WHERE");
            sb.append("+");
            sb.append(query.getWhereClause());
        }

        String q = sb.toString();

        log.info("SOQL Where clause: {0}", q);

        return q;
    }

    private ConnectorObject createConnectorObject(Map record) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

        Set<Entry<String, Object>> entrySet = record.entrySet();

        for (Entry<String, Object> entry : entrySet) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // UID (Id)
            if (key.equals(configuration.getUniqueAttribute())) {
                builder.setUid(value.toString());
                continue;
            }

            // NAME (Username)
            if (key.equals(configuration.getNameAttribute())) {
                builder.setName(value.toString());
                continue;
            }

            // Other attributes
            if (value instanceof String) {
                String stringValue = (String) value;
                if (StringUtil.isEmpty(stringValue)) {
                    continue;
                }
                builder.addAttribute(key, stringValue);

            } else if (value instanceof List) {
                List multiValue = (List) value;
                builder.addAttribute(key, multiValue);
            }
        }
        return builder.build();
    }

    protected void doPost(String relativeUrl, Object representation,
            Callback callback) {
        doPost(relativeUrl, representation, callback, null);
    }

    private void updatePassword(Uid uid, String newPassword) {
        if (newPassword == null) {
            return;
        }

        // update password.
        Map<String, String> jsonMap = new HashMap<String, String>();
        jsonMap.put("NewPassword", newPassword);
        JacksonRepresentation<Map<String, String>> jsonRepresentation = new JacksonRepresentation<Map<String, String>>(
                jsonMap);

        String relativeUrl = this.configuration.getUserServicePath()
                + uid.getUidValue() + "/password";

        final Boolean[] ignorePasswordError = new Boolean[1];
        ignorePasswordError[0] = this.configuration.isIgnorePasswordError();

        doPost(relativeUrl, jsonRepresentation, new NothingCallback(),
                new ErrorCallback() {
                    public void call(ResourceException e, Representation body) {
                        JacksonRepresentation<List> bodyRepresentation = new JacksonRepresentation<List>(
                                body, List.class);
                        List answer = bodyRepresentation != null ? bodyRepresentation
                                .getObject() : null;
                        Map answerMap = (Map) answer.get(0);
                        String message = (String) answerMap.get("message");
                        String errorCode = (String) answerMap.get("errorCode");

                        log.info(
                                "Update password error. message={0}, errorCode={1}",
                                message, errorCode);

                        if (!ignorePasswordError[0]) {
                            throw new InvalidNewPasswordException(message,
                                    errorCode);
                        }
                    }
                });
    }

    protected void doPost(String relativeUrl, Object representation,
            Callback callback, ErrorCallback errorCallback) {

        ClientResource child = getClient().getChild(relativeUrl);

        try {
            Representation body = child.post(representation);

            if (child.getStatus().isSuccess()) {
                callback.call(body);
            }

        } catch (ResourceException e) {
            if (isUnAuthorized(e)) {
                this.connector.refreshAccessToken();
                doPost(relativeUrl, representation, callback, errorCallback);
            }
            if (errorCallback == null) {
                throw e;
            } else {
                errorCallback.call(e, child.getResponseEntity());
            }
        }
    }

    protected void doGet(String relativeUrl, Callback callback,
            ErrorCallback errorCallback) {

        ClientResource child = getClient().getChild(relativeUrl);

        try {
            Representation body = child.get();

            if (child.getStatus().isSuccess()) {
                callback.call(body);
            }

        } catch (ResourceException e) {
            if (isUnAuthorized(e)) {
                this.connector.refreshAccessToken();
                doGet(relativeUrl, callback, errorCallback);
            }
            if (errorCallback == null) {
                throw e;
            } else {
                errorCallback.call(e, child.getResponseEntity());
            }
        }
    }

    protected void doDelete(String relativeUrl, Callback callback,
            ErrorCallback errorCallback) {

        ClientResource child = getClient().getChild(relativeUrl);

        try {
            Representation body = child.delete();

            if (child.getStatus().isSuccess()) {
                callback.call(body);
            }

        } catch (ResourceException e) {
            if (isUnAuthorized(e)) {
                this.connector.refreshAccessToken();
                doDelete(relativeUrl, callback, errorCallback);
            }
            if (errorCallback == null) {
                throw e;
            } else {
                errorCallback.call(e, child.getResponseEntity());
            }
        }
    }

    private boolean isUnAuthorized(ResourceException e) {
        return e.getStatus().getCode() == 401;
    }

    interface Callback {
        public void call(Representation r);
    }

    interface ErrorCallback {
        public void call(ResourceException e, Representation r);
    }

    class NothingCallback implements Callback {
        public void call(Representation r) {
            // nothing
        }
    }

    protected SalesforceConnection getClient() {
        return this.connector.getConnection();
    }
}
