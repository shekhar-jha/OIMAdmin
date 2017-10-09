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

package com.jhash.oimadmin.oim;

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import java.util.*;

public class JMXUtils {

    private static final Logger logger = LoggerFactory.getLogger(JMXUtils.class);

    public static Set<ObjectInstance> getJMXBean(MBeanServerConnection connection, JMXConnection.OIM_JMX_BEANS jmxBeans) {
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
            Set<ObjectInstance> jmxBeanObjectInstances = connection.queryMBeans(new ObjectName(expression), null);
            logger.trace("Returning search result {}", jmxBeanObjectInstances);
            return jmxBeanObjectInstances;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to get JMX Bean for " + jmxBeans, exception);
        }
    }

    public static String getName(ObjectInstance bean) {
        String beanName = bean.getObjectName().getKeyPropertyList().get("name");
        if (Utils.isEmpty(beanName)) {
            beanName = bean.getObjectName().getKeyPropertyList().get("Name");
        }
        return beanName;
    }

    public static String getType(ObjectInstance bean) {
        String type = bean.getObjectName().getKeyPropertyList().get("type");
        if (Utils.isEmpty(type)) {
            type = bean.getObjectName().getKeyPropertyList().get("Type");
        }
        return type;
    }

    public static Object getValue(MBeanServerConnection connection, ObjectInstance objectInstance, String attributeName) {
        try {
            Object returnValue = connection.getAttribute(objectInstance.getObjectName(), attributeName);
            logger.trace("Returning value {} corresponding to attribute {} of bean {}", new Object[]{returnValue, attributeName, objectInstance});
            return returnValue;
        } catch (AttributeNotFoundException exception) {
            logger.warn("Could not locate the attribute {} in bean {}", new Object[]{attributeName, objectInstance});
            return null;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to get attribute " + attributeName + " from bean " + objectInstance, exception);
        }
    }

    public static void setValue(MBeanServerConnection connection, ObjectInstance objectInstance, String attributeName, Object value) {
        try {
            connection.setAttribute(objectInstance.getObjectName(), new Attribute(attributeName, value));
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to set attribute " + attributeName + " to " + value + " on bean " + objectInstance, exception);
        }
    }

    public static Object invoke(MBeanServerConnection connection, ObjectInstance objectInstance, String methodName, String[] parametersType, Object... parameterValues) {
        if (parameterValues == null)
            parameterValues = new Object[]{};
        try {
            return connection.invoke(objectInstance.getObjectName(), methodName, parameterValues, parametersType);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to invoke method " + methodName + " on bean " + objectInstance, exception);
        }
    }

    public static Details extractCompositeData(CompositeData[] methodInvocationResult) {
        return extractCompositeData(methodInvocationResult, null);
    }

    public static Details extractCompositeData(CompositeData[] methodInvocationResult, Map<String, String> nameToColumnNameMapping) {
        Set<String> applicableColumns = new HashSet<String>();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            for (CompositeData data : methodInvocationResult) {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                Set<String> columnNames = data.getCompositeType().keySet();
                for (String columnName : columnNames) {
                    String applicableColumnName = Utils.getOrDefault(nameToColumnNameMapping, columnName, columnName);
                    applicableColumns.add(applicableColumnName);
                    dataMap.put(applicableColumnName, data.get(columnName));
                }
                result.add(dataMap);
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to read the event handler details from result "
                    + methodInvocationResult, exception);
        }
        return new Details(result, applicableColumns.toArray(new String[0]));
    }

    public static Map<String, Object> extractData(TabularData tabularData) {
        Map<String, Object> extractedData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Collection<?> elements = tabularData.values();
        if (elements != null) {
            for (Object element : elements) {
                if (element instanceof CompositeDataSupport) {
                    Object key = (((CompositeDataSupport) element).get("key"));
                    Object value = ((CompositeDataSupport) element).get("value");
                    if (key instanceof String) {
                        logger.trace("Adding key {} and value {}", key, value);
                        extractedData.put((String) key, value);
                    } else {
                        logger.debug("Skipping element {} with key {} since it does not contain String key. Found {}", new Object[]{element, key, key == null ? "null" : key.getClass()});
                    }
                } else {
                    logger.debug("Skipping element {} of type {} since it is not of type {}", new Object[]{element, element == null ? "null" : element.getClass(), CompositeDataSupport.class});
                }
            }
        } else {
            logger.debug("Tabular data does not contain any elements. Returning empty map.");
        }
        return extractedData;
    }

    static class ProcessingBeanImpl implements JMXConnection.ProcessingBean {
        private final MBeanServerConnection serverConnection;
        private final ObjectInstance objectInstance;
        private final JMXConnection.OIM_JMX_BEANS bean;

        public ProcessingBeanImpl(MBeanServerConnection connection, ObjectInstance objectInstance) {
            this.serverConnection = connection;
            this.objectInstance = objectInstance;
            this.bean = new JMXConnection.OIM_JMX_BEANS(objectInstance);
        }

        @Override
        public JMXConnection.OIM_JMX_BEANS getBean() {
            return bean;
        }

        @Override
        public Map<String, String> getProperties() {
            return objectInstance.getObjectName().getKeyPropertyList();
        }

        @Override
        public Object getValue(String attributeName) {
            if (objectInstance == null)
                return null;
            return JMXUtils.getValue(serverConnection, objectInstance, attributeName);
        }

        @Override
        public void setValue(String attributeName, Object value) {
            if (objectInstance == null)
                return;
            JMXUtils.setValue(serverConnection, objectInstance, attributeName, value);
        }

        @Override
        public Object invoke(JMXConnection.JMX_BEAN_METHOD method, Object... parameterValues) {
            if (objectInstance == null || method == null)
                return null;
            return JMXUtils.invoke(serverConnection, objectInstance, method.methodName, method.methodParameterClass, parameterValues);
        }
    }

}
