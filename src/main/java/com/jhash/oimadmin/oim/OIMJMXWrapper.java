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
import com.jhash.oimadmin.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.io.Serializable;
import java.util.*;

public class OIMJMXWrapper extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(OIMJMXWrapper.class);
    protected String STRING_REPRESENTATION = "OIMJMXWrapper";
    private JMXConnection jmxConnection = null;
    private Map<OIM_JMX_BEANS, ObjectInstance> beanCache = new HashMap<OIM_JMX_BEANS, ObjectInstance>();
    private Map<OIM_JMX_BEANS, List<ObjectInstance>> beanTypeCache = new HashMap<>();

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

    private void initializeBeanCache() {
        logger.trace("Initializing Bean cache for connection {}", jmxConnection);
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
                        String beanType = bean.getObjectName().getKeyPropertyList().get("type");
                        if (beanName != null && OIM_JMX_BEANS.beanNames.contains(beanName)) {
                            OIM_JMX_BEANS mappedBean = OIM_JMX_BEANS.beanMapping.get(beanName);
                            if (mappedBean.type == null) {
                                logger.trace("Located Bean {} for requested bean {} with name {}", new Object[]{bean, mappedBean, beanName});
                                beanCache.put(mappedBean, bean);
                            } else if (mappedBean.type.equals(beanType)) {
                                logger.trace("Located Bean {} for requested bean {} with name {} & type {}", new Object[]{bean, mappedBean, beanName, beanType});
                                beanCache.put(mappedBean, bean);
                            } else {
                                logger.trace("Ignoring bean {} since type {} != {} of bean {}", new Object[]{bean, beanType, mappedBean.type, mappedBean});
                            }
                        }
                        if (beanType != null && OIM_JMX_BEANS.beanTypeNames.contains(beanType)) {
                            OIM_JMX_BEANS mappedBean = OIM_JMX_BEANS.beanTypeMapping.get(beanType);
                            logger.trace("Located Bean {} for requested bean {} of type {}", new Object[]{bean, mappedBean, beanType});
                            List<ObjectInstance> beanList = null;
                            if (beanTypeCache.containsKey(mappedBean)) {
                                beanList = beanTypeCache.get(mappedBean);
                            } else {
                                beanList = new ArrayList<>();
                                beanTypeCache.put(mappedBean, beanList);
                            }
                            beanList.add(bean);
                        }
                        // TODO: Use for quick tests.
                        /*try {
                        if (beanName != null && beanName.contains("Cache")) {logger.debug("FOUND BEAN WITH CACHE {}", bean);}
                        for ( MBeanAttributeInfo attrInfo :jmxConnection.getConnection().getMBeanInfo(bean.getObjectName()).getAttributes()){
                            if (attrInfo.getName().contains("Cache") && attrInfo.isWritable()) {
                                logger.debug("FOUND BEAN {} WITH CACHE ATTR {}", bean, attrInfo);
                            }
                        }}catch(Exception exception) {logger.debug("FAILED TO PROCESS BEAN {}", bean);}*/
                    } catch (Exception exception) {
                        throw new OIMAdminException("Failed to process bean " + bean, exception);
                    }
                }
            }
        }
        logger.trace("Initialized Bean cache for connection {}", jmxConnection);
    }

    private Set<ObjectInstance> getJMXBean(OIM_JMX_BEANS jmxBeans) {
        logger.trace("Trying to locate beans for {}", jmxBeans);
        try {
            String expression = "*:";
            if (jmxBeans.type != null) {
                expression += "type=" + jmxBeans.type;
            } else {
                expression += "type=*";
            }
            if (jmxBeans.name != null) {
                expression += ",name=" + jmxBeans.name + ",";
            } else {
                expression += ",";
            }
            expression += "*";
            logger.trace("Query expression {}", expression);
            Set<ObjectInstance> jmxBeanObjectInstances = jmxConnection.getConnection().queryMBeans(new ObjectName(expression), null);
            logger.trace("Returning search result {}", jmxBeanObjectInstances);
            return jmxBeanObjectInstances;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to get JMX Bean for " + jmxBeans, exception);
        }
    }

    private List<ObjectInstance> getBeansOfType(OIM_JMX_BEANS jmxBeans) {
        if (beanTypeCache.isEmpty()) {
            initializeBeanCache();
        }
        if (!beanTypeCache.containsKey(jmxBeans)) {
            Set<ObjectInstance> result = getJMXBean(jmxBeans);
            if (result.isEmpty()) {
                return null;
            } else {
                List<ObjectInstance> resultAsList = new ArrayList<>(result);
                beanTypeCache.put(jmxBeans, resultAsList);
                if (result.size() == 1 && !Utils.isEmpty(jmxBeans.name)) {
                    beanCache.put(jmxBeans, resultAsList.get(0));
                }
            }
        }
        return beanTypeCache.get(jmxBeans);
    }

    private ObjectInstance getBean(OIM_JMX_BEANS jmxBean) {
        if (beanCache.isEmpty()) {
            initializeBeanCache();
        }
        if (!beanCache.containsKey(jmxBean)) {
            Set<ObjectInstance> result = getJMXBean(jmxBean);
            if (result.isEmpty()) {
                return null;
            } else {
                List<ObjectInstance> resultAsList = new ArrayList<>(result);
                if (!Utils.isEmpty(jmxBean.type)) {
                    beanTypeCache.put(jmxBean, resultAsList);
                }
                ObjectInstance resultValue = resultAsList.get(0);
                beanCache.put(jmxBean, resultValue);
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
        Map<String, String> nameToColumnNameMapping = EVENT_HANDLER_DETAILS.getNameToColumnNameMapping();
        if (methodInvocationResult != null && methodInvocationResult instanceof CompositeData[]) {
            try {
                for (CompositeData data : ((CompositeData[]) methodInvocationResult)) {
                    Map<String, Object> dataMap = new HashMap<String, Object>();
                    Set<String> columnNames = data.getCompositeType().keySet();
                    columns.addAll(columnNames);
                    for (String columnName : columnNames) {
                        dataMap.put(nameToColumnNameMapping.getOrDefault(columnName, columnName), data.get(columnName));
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
        return new Details(result, EVENT_HANDLER_DETAILS.getColumnNames());
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

    public void setCacheDetails(Map<String, Object> cacheItem, OIM_CACHE_ATTRS cacheAttr, String value) {
        try {
            if (cacheItem != null) {
                if (cacheItem.containsKey("BEAN")) {
                    ObjectInstance bean = (ObjectInstance) cacheItem.get("BEAN");
                    switch (cacheAttr) {
                        case ENABLED:
                            jmxConnection.getConnection().setAttribute(bean.getObjectName(), new Attribute("Enabled", Boolean.valueOf(value)));
                            break;
                        case ExpirationTime:
                            jmxConnection.getConnection().setAttribute(bean.getObjectName(), new Attribute("ExpirationTime", Integer.valueOf(value)));
                            break;
                        default:
                            throw new UnsupportedOperationException("Setting cache attribute " + cacheAttr + " is not supported for cache " + cacheItem);
                    }
                } else {
                    throw new NullPointerException("Failed to locate the corresponding bean for cache category " + cacheItem);
                }
            } else {
                switch (cacheAttr) {
                    case ENABLED:
                    case CLUSTERED:
                    case ThreadLocalCacheEnabled:
                        jmxConnection.getConnection().setAttribute(getBean(OIM_JMX_BEANS.CACHE_MBEAN_NAME).getObjectName(), new Attribute(cacheAttr.nameValue, Boolean.valueOf(value)));
                        break;
                    case ExpirationTime:
                        jmxConnection.getConnection().setAttribute(getBean(OIM_JMX_BEANS.CACHE_MBEAN_NAME).getObjectName(), new Attribute(cacheAttr.nameValue, Integer.parseInt(value)));
                        break;
                    case Provider:
                        jmxConnection.getConnection().setAttribute(getBean(OIM_JMX_BEANS.CACHE_MBEAN_NAME).getObjectName(), new Attribute(cacheAttr.nameValue, value));
                        break;
                    case MulticastAddress:
                    case MulticastConfig:
                        jmxConnection.getConnection().setAttribute(getBean(OIM_JMX_BEANS.CACHE_PROVIDER_MBEAN_NAME).getObjectName(), new Attribute(cacheAttr.nameValue, value));
                        break;
                    case Size:
                        jmxConnection.getConnection().setAttribute(getBean(OIM_JMX_BEANS.CACHE_PROVIDER_MBEAN_NAME).getObjectName(), new Attribute(cacheAttr.nameValue, Integer.parseInt(value)));
                        break;
                }
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to set attribute " + cacheAttr + "=" + value + " on " + (cacheItem == null ? "Cache" : cacheItem), exception);
        }
    }

    public <T> T getCacheDetails(OIM_CACHE_ATTRS attributeRequested) {
        logger.debug("Trying to get OIM Cache Detail {}", attributeRequested);
        Object returnValue = null;
        try {
            switch (attributeRequested) {
                case CLUSTERED: {
                    returnValue = jmxConnection.getConnection().invoke(getBean(OIM_JMX_BEANS.CACHE_MBEAN_NAME).getObjectName(), "isClustered", new Object[]{}, new String[]{});
                    break;
                }
                case ENABLED: {
                    returnValue = jmxConnection.getConnection().invoke(getBean(OIM_JMX_BEANS.CACHE_MBEAN_NAME).getObjectName(), "isEnabled", new Object[]{}, new String[]{});
                    break;
                }
                case ThreadLocalCacheEnabled: {
                    returnValue = jmxConnection.getConnection().invoke(getBean(OIM_JMX_BEANS.CACHE_MBEAN_NAME).getObjectName(), "isThreadLocalCacheEnabled", new Object[]{}, new String[]{});
                    break;
                }
                case ExpirationTime:
                case Provider: {
                    returnValue = jmxConnection.getConnection().getAttribute(getBean(OIM_JMX_BEANS.CACHE_MBEAN_NAME).getObjectName(), attributeRequested.nameValue);
                    break;
                }
                case MulticastAddress:
                case MulticastConfig:
                case Size: {
                    returnValue = jmxConnection.getConnection().getAttribute(getBean(OIM_JMX_BEANS.CACHE_PROVIDER_MBEAN_NAME).getObjectName(), attributeRequested.nameValue);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Invalid attribute " + attributeRequested + " requested");
            }
            logger.debug("Returning value {} of type {}", returnValue, returnValue != null ? returnValue : "null");
            return (T) returnValue;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to get attribute " + attributeRequested + " from bean " + OIM_JMX_BEANS.CACHE_MBEAN_NAME, exception);
        }
    }

    public <T> T getValue(OIM_JMX_BEANS bean, String attributeName) {
        try {
            ObjectInstance objectInstance = getBean(bean);
            if (objectInstance == null) {
                logger.warn("Could not locate the bean corresponding to {}", bean);
                return null;
            }
            try {
                Object returnValue = jmxConnection.getConnection().getAttribute(objectInstance.getObjectName(), attributeName);
                logger.trace("Returning value {} corresponding to attribute {} of bean {}", new Object[]{returnValue, attributeName, bean});
                return (T) returnValue;
            } catch (AttributeNotFoundException exception) {
                logger.warn("Could not locate the attribute {} in bean {} associated with {}", new Object[]{attributeName, objectInstance, bean});
                return null;
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to get attribute " + attributeName + " from bean " + bean, exception);
        }
    }

    public Details getCacheCategories() {
        try {
            logger.debug("Trying to locate cache categories...");
            List<Map<String, Object>> categoryDetails = new ArrayList<>();
            for (ObjectInstance bean : getBeansOfType(OIM_JMX_BEANS.CACHE_CATEGORIES)) {
                Map<String, Object> categoryDetail = new HashMap<>();
                ObjectName objectName = bean.getObjectName();
                String name = (String) jmxConnection.getConnection().getAttribute(objectName, "Name");
                categoryDetail.put("Name", name);
                boolean isEnabled = (Boolean) jmxConnection.getConnection().invoke(objectName, "isEnabled", new Object[]{}, new String[]{});
                categoryDetail.put("Enabled?", isEnabled);
                int expirationTime = (Integer) jmxConnection.getConnection().getAttribute(objectName, "ExpirationTime");
                categoryDetail.put("Expires in", expirationTime);
                categoryDetail.put("BEAN", bean);
                categoryDetails.add(categoryDetail);
                logger.trace("Added cache detail {}", categoryDetail);
            }
            logger.debug("Located cache details {}", categoryDetails);
            return new Details(categoryDetails, new String[]{"Name", "Enabled?", "Expires in"});
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to retrieve cache details", exception);
        }
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
                "offBand"), CLASS("class", "class"), LOCATION("location", "location");

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

        public static Map<String, String> getNameToColumnNameMapping() {
            Map<String, String> nameToColumnNameMapping = new HashMap<>();
            for (EVENT_HANDLER_DETAILS detail : allValues) {
                nameToColumnNameMapping.put(detail.name, detail.columnName);
            }
            return nameToColumnNameMapping;
        }

    }

    public static enum OIM_CACHE_ATTRS {
        CLUSTERED("Clustered"), ENABLED("Enabled"), ExpirationTime("ExpirationTime"), Provider("Provider"), ThreadLocalCacheEnabled("ThreadLocalCacheEnabled"),
        MulticastAddress("MulticastAddress"), MulticastConfig("MulticastConfig"), Size("Size");

        public final String nameValue;

        private OIM_CACHE_ATTRS(String nameValue) {
            this.nameValue = nameValue;
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

        public static Map<String, Set<String>> getOperationDetails(OIMJMXWrapper connection) {
            return allowedOperations.get(connection);
        }

        public Details getEventHandlers(OperationDetail operation) {
            return connection.getEventHandlers(operation);
        }
    }

    public static class Details {
        private List<Map<String, Object>> values;
        private String[] columnNames;

        public Details(List<Map<String, Object>> values, String[] columnNames) {
            this.values = values;
            this.columnNames = columnNames;
        }

        public Map<String, Object> getItemAt(int index) {
            return values.get(index);
        }

        public Object[][] getData() {
            Object[][] data = new Object[values.size()][];
            int rowCounter = 0;
            for (Map<String, Object> value : values) {
                Object[] valueArray = new Object[columnNames.length];
                int columnCounter = 0;
                for (String columnName : columnNames) {
                    valueArray[columnCounter++] = value.get(columnName);
                }
                data[rowCounter++] = valueArray;
            }
            return data;
        }

        public String[] getColumns() {
            return columnNames;
        }

        public int size() {
            return values.size();
        }
    }

    public static class OIM_JMX_BEANS {

        private static final Set<String> beanNames = new HashSet<String>();
        private static final Set<String> beanTypeNames = new HashSet<>();
        private static final Map<String, OIM_JMX_BEANS> beanMapping = new HashMap<String, OIM_JMX_BEANS>();
        private static final Map<String, OIM_JMX_BEANS> beanTypeMapping = new HashMap<>();
        public static final OIM_JMX_BEANS CONFIG_QUERY_MBEAN_NAME = new OIM_JMX_BEANS("ConfigQueryMBeanName");
        public static final OIM_JMX_BEANS OPERATION_CONFIG_MBEAN_NAME = new OIM_JMX_BEANS("OperationConfigMXBean");
        public static final OIM_JMX_BEANS CACHE_MBEAN_NAME = new OIM_JMX_BEANS("Cache");
        public static final OIM_JMX_BEANS CACHE_PROVIDER_MBEAN_NAME = new OIM_JMX_BEANS("XLCacheProvider");
        public static final OIM_JMX_BEANS CACHE_CATEGORIES = new OIM_JMX_BEANS(null, "XMLConfig.CacheConfig.CacheCategoryConfig");

        //TODO: There is a incorrect dependency between the static variables which is dependent on location of variable in file
        // beanNames should come before CONFIG_QUERY_MBEAN_NAME;
        public final String name;
        public final String type;
        private final String stringRepresentation;

        public OIM_JMX_BEANS(String name) {
            this(name, null);
        }

        public OIM_JMX_BEANS(String name, String type) {
            this.name = name;
            this.type = type;
            if (name != null) {
                OIM_JMX_BEANS.beanNames.add(name);
                OIM_JMX_BEANS.beanMapping.put(name, this);
            }
            if (type != null) {
                OIM_JMX_BEANS.beanTypeNames.add(type);
                beanTypeMapping.put(type, this);
            }
            stringRepresentation = "JMX Bean [" + (name == null ? "" : name) + ":" + (type == null ? "" : type) + "]";
        }

        public String toString() {
            return stringRepresentation;
        }
    }
}
