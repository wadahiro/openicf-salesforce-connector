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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * 
 * @author Hiroyuki Wada
 * 
 */
public class SalesforceAttributeUtils {

    private SalesforceAttributeUtils() {
        // util class
    }

    public static Map toForceJsonMap(SalesforceConfiguration conf,
            Set<Attribute> attrs) {
        Map map = new HashMap();

        for (Attribute attr : attrs) {
            if (attr.is(Name.NAME)) {
                map.put(conf.getNameAttribute(), attr.getValue().get(0));
                continue;
            }
            List<Object> value = attr.getValue();
            if (value.size() == 1) {
                map.put(attr.getName(), value.get(0));
            } else if (value.size() == 0) {
                // nothing
            } else {
                map.put(attr.getName(), value);
            }
        }
        return map;
    }

    public static String[] toAttrList(SalesforceConfiguration conf,
            OperationOptions options) {
        Set<String> alist = new LinkedHashSet<String>();
        // append Id and Username always.
        alist.add(conf.getUniqueAttribute());
        alist.add(conf.getNameAttribute());

        if (options != null) {
            String attrs[] = options.getAttributesToGet();

            for (String attr : attrs) {
                if (attr.equals(Uid.NAME) || attr.equals(Name.NAME)) {
                    continue;

                } else if (attr.equals(OperationalAttributes.PASSWORD_NAME)) {
                    // don't include password column into query
                    continue;

                } else {
                    alist.add(attr);
                }
            }
        }

        return alist.toArray(new String[0]);
    }

    public static void parseDescribe(Map describe, SchemaBuilder schemaBuilder) {
        Object name = describe.get("name");
        ObjectClassInfoBuilder ocBuilder = new ObjectClassInfoBuilder();
        if (name instanceof String) {
            if ("User".equalsIgnoreCase((String) name)) {
                ocBuilder.setType(ObjectClass.ACCOUNT_NAME);
            } else if ("Group".equalsIgnoreCase((String) name)) {
                ocBuilder.setType(ObjectClass.GROUP_NAME);
            } else {
                ocBuilder.setType((String) name);
            }
            ocBuilder.addAttributeInfo(Name.INFO);
        } else {
            return;
        }
        Object fields = describe.get("fields");

        if (fields instanceof List) {
            for (Map<String, Object> field : (List<Map<String, Object>>) fields) {
                Object fieldName = field.get("name");
                if (fieldName instanceof String) {
                    if ("Id".equalsIgnoreCase((String) fieldName)) {
                        // __UID__ Attribute
                        continue;
                    }
                    Object idLookup = field.get("idLookup");
                    if (Boolean.valueOf((Boolean) idLookup)) {
                        // __NAME__ Attribute
                        continue;
                    }
                    AttributeInfoBuilder attributeInfoBuilder = new AttributeInfoBuilder(
                            (String) fieldName);
                    // NOT_UPDATEABLE
                    Object updateable = field.get("updateable");
                    if ((updateable != null)
                            && !Boolean.valueOf((Boolean) updateable)) {
                        attributeInfoBuilder.setUpdateable(false);
                    }
                    // NOT_CREATABLE
                    Object createable = field.get("createable");
                    if ((createable != null)
                            && !Boolean.valueOf((Boolean) createable)) {
                        attributeInfoBuilder.setCreateable(false);
                    }
                    // REQUIRED
                    Object nillable = field.get("nillable");
                    if ((nillable != null)
                            && !Boolean.valueOf((Boolean) nillable)) {
                        attributeInfoBuilder.setRequired(true);
                    }
                    /*
                     * MULTIVALUED, NOT_READABLE, NOT_RETURNED_BY_DEFAULT
                     */
                    ocBuilder.addAttributeInfo(attributeInfoBuilder.build());
                }

            }
        }
        ObjectClassInfo objectClassInfo = ocBuilder.build();
        schemaBuilder.defineObjectClass(objectClassInfo);
        Object updateable = describe.get("updateable");
        if ((updateable != null) && !Boolean.valueOf((Boolean) updateable)) {
            schemaBuilder.removeSupportedObjectClass(UpdateOp.class,
                    objectClassInfo);
        }
        // Object queryable = describe.get("queryable");
        // Object retrieveable = describe.get("retrieveable");
        Object searchable = describe.get("searchable");
        if ((searchable != null) && !Boolean.valueOf((Boolean) searchable)) {
            schemaBuilder.removeSupportedObjectClass(SearchOp.class,
                    objectClassInfo);
        }
        Object createable = describe.get("createable");
        if ((createable != null) && !Boolean.valueOf((Boolean) createable)) {
            schemaBuilder.removeSupportedObjectClass(CreateOp.class,
                    objectClassInfo);
        }
        Object deletable = describe.get("deletable");
        if ((deletable != null) && !Boolean.valueOf((Boolean) deletable)) {
            schemaBuilder.removeSupportedObjectClass(DeleteOp.class,
                    objectClassInfo);
        }
    }
}
