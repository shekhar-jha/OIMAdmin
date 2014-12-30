/*
 * Copyright 2014 Shekhar Jha
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
package com.jhash.oimadmin.oim;

import com.jhash.oimadmin.Config.Configuration;
import com.jhash.oimadmin.OIMAdminException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectInstance;
import javax.management.openmbean.CompositeData;
import java.io.Serializable;
import java.util.*;

public class OIMJMXWrapper extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(OIMJMXWrapper.class);
    protected String STRING_REPRESENTATION = "OIMJMXWrapper";
    private JMXConnection jmxConnection = null;
    private Map<OIM_JMX_BEANS, ObjectInstance> beanCache = new HashMap<OIM_JMX_BEANS, ObjectInstance>();

    @Override
    protected void initializeConnection(Configuration config) {
        logger.debug("Trying to initialize OIM JMX Wrapper Connection using configuration {}", config);
        JMXConnection tmpConnection = new JMXConnection();
        logger.debug("Trying to initialize JMX Connection.");
        tmpConnection.initialize(config);
        logger.debug("Trying to get JMX Server Connection.");
        jmxConnection = tmpConnection;
        STRING_REPRESENTATION += "(" + jmxConnection + ")";
    }

    private ObjectInstance getBean(OIM_JMX_BEANS jmxBean) {
        if (beanCache.isEmpty()) {
            synchronized (beanCache) {
                if (beanCache.isEmpty()) {
                    Set<ObjectInstance> allBeans = null;
                    try {
                        allBeans = jmxConnection.getConnection().queryMBeans(null, null);
                    } catch (Exception exception) {
                        throw new OIMAdminException("Failed to get a list of all the beans ", exception);
                    }
                    for (ObjectInstance bean : allBeans) {
                        try {
                            // TODO: May need to support additional validations
                            // later in case of conflict
                            String beanName = bean.getObjectName().getKeyPropertyList().get("name");
                            if (beanName != null && OIM_JMX_BEANS.beanNames.contains(beanName)) {
                                OIM_JMX_BEANS mappedBeanType = OIM_JMX_BEANS.beanMapping.get(beanName);
                                beanCache.put(mappedBeanType, bean);
                            }
                        } catch (Exception exception) {
                            throw new OIMAdminException("Failed to process bean " + bean, exception);
                        }
                    }
                }
            }
        }
        return beanCache.get(jmxBean);
    }

    public Details getEventHandlers(OperationDetail operation) {
        ObjectInstance configMBean = getBean(OIM_JMX_BEANS.OPERATION_CONFIG_MBEAN_NAME);
        Object methodInvocationResult;
        Object[] parameters = new Object[]{operation.name};
        try {
            methodInvocationResult = jmxConnection.getConnection().invoke(configMBean.getObjectName(),
                    "findEventHandlers", parameters, new String[]{"java.lang.String"});
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to invoke findEventHandlers on " + configMBean + " with parameters "
                    + operation.name, exception);
        }
        Set<String> columns = new HashSet<String>();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (methodInvocationResult != null && methodInvocationResult instanceof CompositeData[]) {
            try {
                for (CompositeData data : ((CompositeData[]) methodInvocationResult)) {
                    Map<String, Object> dataMap = new HashMap<String, Object>();
                    Set<String> columnNames = data.getCompositeType().keySet();
                    columns.addAll(columnNames);
                    for (String columnName : columnNames) {
                        dataMap.put(columnName, data.get(columnName));
                    }
                    result.add(dataMap);
                }
            } catch (Exception exception) {
                throw new OIMAdminException("Failed to read the event handler details from result "
                        + methodInvocationResult, exception);
            }
        } else {
            throw new NullPointerException("Returned " + methodInvocationResult + "  of type "
                    + (methodInvocationResult == null ? "null" : methodInvocationResult.getClass())
                    + " on invoking getEventHandlers on " + configMBean + " using parameters "
                    + Arrays.toString(parameters));
        }
        return new Details(result);
    }

    public Set<OperationDetail> getOperations() {
        Set<OperationDetail> operations = new HashSet<OperationDetail>();
        ObjectInstance operationConfigObjectInstance = getBean(OIM_JMX_BEANS.OPERATION_CONFIG_MBEAN_NAME);
        try {
            Object result = jmxConnection.getConnection().invoke(operationConfigObjectInstance.getObjectName(),
                    "findConfiguredOperations", new Object[]{}, new String[]{});
            if (result != null && result instanceof CompositeData[]) {
                for (CompositeData data : (CompositeData[]) result) {
                    operations.add(new OperationDetail((String) data.get("name"), (String) data.get("description"),
                            this));
                }
            } else {
                throw new ClassCastException("Expected call to findConfiguredOperations on "
                        + OIM_JMX_BEANS.OPERATION_CONFIG_MBEAN_NAME + " to return CompositeData[], it returned "
                        + (result == null ? result : result.getClass()));
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to invoke operation findConfiguredOperations on "
                    + operationConfigObjectInstance, exception);
        }
        return operations;
    }

    protected void destroyConnection() {
        logger.debug("Trying to destroy OIM JMX Connection");
        if (this.jmxConnection != null) {
            logger.debug("Trying to destroy JMX connection {}", jmxConnection);
            jmxConnection.destroy();
            logger.debug("Destroyed JMX connection.");
            jmxConnection = null;
        }
        logger.debug("Destroyed OIM JMX Connection");
    }

    public static enum EVENT_HANDLER_DETAILS {
        STAGE("Stage of Execution", "stage"), ORDER("Order of Execution", "order"), NAME("Name", "name"), CUSTOM(
                "Is custom?", "custom"), CONDITIONAL("Conditional", "conditional"), OFFBAND("Executed Offband",
                "offBand"), CLASS(null, "class"), LOCATION(null, "location");

        private static final EVENT_HANDLER_DETAILS[] allValues = new EVENT_HANDLER_DETAILS[]{STAGE, ORDER, NAME,
                CUSTOM, CONDITIONAL, OFFBAND};
        public final String columnName;
        public final String name;

        private EVENT_HANDLER_DETAILS(String columnName, String name) {
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

    }

    public static class OperationDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final Map<OIMJMXWrapper, Map<String, Set<String>>> allowedOperations = new HashMap<OIMJMXWrapper, Map<String, Set<String>>>();
        public final String name;
        public final String description;
        private OIMJMXWrapper connection;

        private OperationDetail(String name, String description, OIMJMXWrapper connection) {
            this.name = name;
            this.description = description;
            this.connection = connection;
            String[] nameSplits = name.split("-");
            if (nameSplits == null || nameSplits.length != 2) {
                logger.debug("ignoring the name {} since it does not have two components with - inbetween", name);
            } else {
                Map<String, Set<String>> allowedEntityOperationsForConnection = null;
                if (allowedOperations.containsKey(connection)) {
                    allowedEntityOperationsForConnection = allowedOperations.get(connection);
                } else {
                    allowedEntityOperationsForConnection = new HashMap<String, Set<String>>();
                    allowedOperations.put(connection, allowedEntityOperationsForConnection);
                }
                Set<String> allowedOperationsForEntityConnection = null;
                if (allowedEntityOperationsForConnection.containsKey(nameSplits[0])) {
                    allowedOperationsForEntityConnection = allowedEntityOperationsForConnection.get(nameSplits[0]);
                } else {
                    allowedOperationsForEntityConnection = new HashSet<String>();
                    allowedEntityOperationsForConnection.put(nameSplits[0], allowedOperationsForEntityConnection);
                }
                allowedOperationsForEntityConnection.add(nameSplits[1]);
            }
        }

        public Details getEventHandlers(OperationDetail operation) {
            return connection.getEventHandlers(operation);
        }

        public Map<String, Set<String>> getOperationDetails() {
            return allowedOperations.get(connection);
        }
    }

    public static class Details {
        private List<Map<String, Object>> values;

        public Details(List<Map<String, Object>> values) {
            this.values = values;
        }

        public Map<String, Object> getItemAt(int index) {
            return values.get(index);
        }

        public Object[][] getData() {
            Object[][] data = new Object[values.size()][];
            int rowCounter = 0;
            for (Map<String, Object> value : values) {
                Object[] valueArray = new Object[EVENT_HANDLER_DETAILS.allValues.length];
                int columnCounter = 0;
                for (EVENT_HANDLER_DETAILS column : EVENT_HANDLER_DETAILS.allValues) {
                    valueArray[columnCounter++] = value.get(column.name);
                }
                data[rowCounter++] = valueArray;
            }
            return data;
        }

        public String[] getColumns() {
            return EVENT_HANDLER_DETAILS.getColumnNames();
        }
    }

    public static class OIM_JMX_BEANS {

        //TODO: There is a incorrect dependency between the static variables which is dependent on location of variable in file
        // beanNames should come before CONFIG_QUERY_MBEAN_NAME;
        private static final Set<String> beanNames = new HashSet<String>();
        private static final Map<String, OIM_JMX_BEANS> beanMapping = new HashMap<String, OIM_JMX_BEANS>();
        public final String name;

        public static final OIM_JMX_BEANS CONFIG_QUERY_MBEAN_NAME = new OIM_JMX_BEANS("ConfigQueryMBeanName");
        public static final OIM_JMX_BEANS OPERATION_CONFIG_MBEAN_NAME = new OIM_JMX_BEANS("OperationConfigMXBean");

        private OIM_JMX_BEANS(String name) {
            this.name = name;
            OIM_JMX_BEANS.beanNames.add(name);
            OIM_JMX_BEANS.beanMapping.put(name, this);
        }
    }

}
