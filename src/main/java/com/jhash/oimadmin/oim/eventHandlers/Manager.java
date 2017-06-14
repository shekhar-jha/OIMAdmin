/*
 * Copyright 2015 Shekhar Jha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jhash.oimadmin.oim.eventHandlers;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.oim.Details;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.JMXUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Manager {

    public static final JMXConnection.OIM_JMX_BEANS OPERATION_CONFIG_MBEAN_NAME = new JMXConnection.OIM_JMX_BEANS("OperationConfigMXBean");
    public static final JMXConnection.OIM_JMX_BEANS ORCH_ENGINE_MBEAN_NAME = new JMXConnection.OIM_JMX_BEANS("OrchestrationEngine", "Kernel");
    public static final JMXConnection.JMX_BEAN_METHOD FIND_EVENT_HANDLERS = new JMXConnection.JMX_BEAN_METHOD(OPERATION_CONFIG_MBEAN_NAME,
            "findEventHandlers", new String[]{"java.lang.String"});
    public static final JMXConnection.JMX_BEAN_METHOD FIND_CONFIGURED_OPERATIONS = new JMXConnection.JMX_BEAN_METHOD(OPERATION_CONFIG_MBEAN_NAME, "findConfiguredOperations");
    public static final JMXConnection.JMX_BEAN_METHOD FIND_EVENT_HANDLERS_ORC = new JMXConnection.JMX_BEAN_METHOD(ORCH_ENGINE_MBEAN_NAME,
            "findEventHandlers", new String[]{"java.lang.String", "java.lang.String"});
    public static final JMXConnection.JMX_BEAN_METHOD LIST_ENTITY_TYPES = new JMXConnection.JMX_BEAN_METHOD(ORCH_ENGINE_MBEAN_NAME, "listEntityTypes");
    public static final JMXConnection.JMX_BEAN_METHOD FIND_OPERATIONS = new JMXConnection.JMX_BEAN_METHOD(ORCH_ENGINE_MBEAN_NAME, "findOperations", new String[]{"java.lang.String"});

    private static final Logger logger = LoggerFactory.getLogger(Manager.class);
    private final JMXConnection connection;

    public Manager(JMXConnection connection) {
        this.connection = connection;
    }

    public Config.OIM_VERSION getVersion() {
        return connection.getVersion();
    }

    public Details getEventHandlers(OperationDetail operation) {
        Object methodInvocationResult;
        switch (connection.getVersion()) {
            case OIM11GR2PS2: {
                try {
                    methodInvocationResult = connection.invoke(FIND_EVENT_HANDLERS, operation.name);
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to invoke " + FIND_EVENT_HANDLERS + " with parameters "
                            + operation.name, exception);
                }
                if (methodInvocationResult == null || !(methodInvocationResult instanceof CompositeData[])) {
                    throw new OIMAdminException("Returned " + methodInvocationResult + "  of type "
                            + (methodInvocationResult == null ? "null" : methodInvocationResult.getClass())
                            + " on invoking " + FIND_EVENT_HANDLERS + " using parameters "
                            + operation.name);
                }
                return JMXUtils.extractCompositeData((CompositeData[]) methodInvocationResult, EVENT_HANDLER_DETAILS.getNameToColumnNameMapping());
            }
            default: {
                try {
                    methodInvocationResult = connection.invoke(FIND_EVENT_HANDLERS_ORC, operation.entity, operation.operation);
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to invoke findEventHandlers on " + FIND_EVENT_HANDLERS_ORC + " with parameters "
                            + operation, exception);
                }
                if (methodInvocationResult == null || !(methodInvocationResult instanceof CompositeData[])) {
                    throw new NullPointerException("Returned " + methodInvocationResult + "  of type "
                            + (methodInvocationResult == null ? "null" : methodInvocationResult.getClass())
                            + " on invoking " + operation + " on " + FIND_EVENT_HANDLERS_ORC + " using parameters "
                            + operation);
                }
                return JMXUtils.extractCompositeData((CompositeData[]) methodInvocationResult, EVENT_HANDLER_DETAILS.getNameToColumnNameMapping());
            }
        }
    }


    private Set<OperationDetail> getOperationsOIM11gR2PS2() {
        Set<OperationDetail> operations = new HashSet<>();
        try {
            Object result = connection.invoke(FIND_CONFIGURED_OPERATIONS);
            if (result != null && result instanceof CompositeData[]) {
                Details operationDetails = JMXUtils.extractCompositeData((CompositeData[]) result);
                for (Map<String, Object> operationDetail : operationDetails) {
                    operations.add(new OperationDetail((String) operationDetail.get("name"), (String) operationDetail.get("description"),
                            this));
                }
            } else {
                throw new ClassCastException("Expected call to findConfiguredOperations on "
                        + OPERATION_CONFIG_MBEAN_NAME + " to return CompositeData[], it returned "
                        + (result == null ? null : result.getClass()));
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to invoke operation "
                    + FIND_CONFIGURED_OPERATIONS, exception);
        }
        return operations;
    }

    public Set<OperationDetail> getOperations() {
        switch (connection.getVersion()) {
            case OIM11GR2PS2:
                return getOperationsOIM11gR2PS2();
            default: {
                Set<OperationDetail> operationDetails = new HashSet<>();
                String[] entityTypes;
                try {
                    Object result = connection.invoke(LIST_ENTITY_TYPES);
                    if (result == null)
                        throw new NullPointerException("Invocation of " + LIST_ENTITY_TYPES + " returned null");
                    if (!(result instanceof String[]))
                        throw new ClassCastException("Invocation of " + LIST_ENTITY_TYPES + " returned object " + result + " of type " + result.getClass() + ", Expected: String[]");
                    entityTypes = (String[]) result;
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to invoke operation "
                            + LIST_ENTITY_TYPES, exception);
                }
                for (String entityType : entityTypes) {
                    try {
                        Object result = connection.invoke(FIND_OPERATIONS, entityType);
                        if (result == null)
                            throw new NullPointerException("Invocation of " + FIND_OPERATIONS + " with parameter " + entityType + " returned null");
                        if (!(result instanceof String[]))
                            throw new ClassCastException("Invocation of " + FIND_OPERATIONS + " with parameter " + entityType + " returned object " + result + " of type " + result.getClass() + ", Expected: String[]");
                        for (String operation : (String[]) result) {
                            operationDetails.add(new OperationDetail(entityType, operation, operation + " on " + entityType,
                                    this));
                        }
                    } catch (Exception exception) {
                        throw new OIMAdminException("Failed to invoke "
                                + FIND_OPERATIONS, exception);
                    }

                }
                return operationDetails;
            }
        }
    }

    public Map<String, Set<String>> getAvailableEventHandlers() {
        return OperationDetail.getOperationDetails(this);
    }

    public enum EVENT_HANDLER_DETAILS {
        STAGE("Stage of Execution", "stage"), ORDER("Order of Execution", "order"), NAME("Name", "name"), CUSTOM(
                "Is custom?", "custom"), CONDITIONAL("Conditional", "conditional"), OFFBAND("Executed Offband",
                "offBand"), CLASS("class", "class"), LOCATION("location", "location"), EXCEPTION("Exception", "exception"),
        SYNC("Sync", "sync"), TRANSACTIONAL("Transactional", "transactional");

        private static final EVENT_HANDLER_DETAILS[] allValues = new EVENT_HANDLER_DETAILS[]{STAGE, ORDER, NAME,
                CONDITIONAL};
        public final String columnName;
        public final String name;

        EVENT_HANDLER_DETAILS(String columnName, String name) {
            this.name = name;
            this.columnName = columnName;
        }

        public static String[] getColumnNames() {
            String[] columnNames = new String[allValues.length];
            int counter = 0;
            for (EVENT_HANDLER_DETAILS detail : allValues) {
                columnNames[counter++] = detail.columnName;
            }
            return columnNames;
        }

        public static Map<String, String> getNameToColumnNameMapping() {
            Map<String, String> nameToColumnNameMapping = new HashMap<>();
            for (EVENT_HANDLER_DETAILS detail : allValues) {
                nameToColumnNameMapping.put(detail.name, detail.columnName);
            }
            return nameToColumnNameMapping;
        }

    }

}
