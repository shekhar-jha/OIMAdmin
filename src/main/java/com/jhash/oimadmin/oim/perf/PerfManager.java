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

package com.jhash.oimadmin.oim.perf;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.Details;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.JMXUtils;
import com.jhash.oimadmin.oim.OIMUtils;
import com.jhash.oimadmin.oim.eventHandlers.Manager;
import com.jhash.oimadmin.oim.eventHandlers.OperationDetail;
import com.jhash.oimadmin.oim.orch.OrchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import java.util.*;

public class PerfManager {

    public static final String ATTR_PERFORMANCE_CONFIG_PREFIX = "sysadmin.performance.";
    public static final String ATTR_PERFORMANCE_CONFIG_OPTIONS = "options";
    public static final String ATTR_PERFORMANCE_CONFIG_NAME = ".name";
    public static final String ATTR_PERFORMANCE_CONFIG_TYPE = ".type";
    public static final String ATTR_PERFORMANCE_CONFIG_DISPLAY_NAME = ".display";
    public static final String ATTR_PERFORMANCE_CONFIG_CALL = ".call";
    public static final JMXConnection.OIM_JMX_BEANS DMS_CONFIG_MBEAN = new JMXConnection.OIM_JMX_BEANS(null,
            "JMXEventConfig");
    public static final JMXConnection.OIM_JMX_BEANS ALL_DMS_PERF_MBEANS = new JMXConnection.OIM_JMX_BEANS("oracle.dms", null, null);
    public static final JMXConnection.JMX_BEAN_METHOD ADD_EVENT_ROUTE = new JMXConnection.JMX_BEAN_METHOD(DMS_CONFIG_MBEAN,
            "addEventRoute", new String[]{String.class.getName(), String.class.getName(), boolean.class.getName()});
    public static final JMXConnection.JMX_BEAN_METHOD ACTIVATE_CONFIGURATION = new JMXConnection.JMX_BEAN_METHOD(DMS_CONFIG_MBEAN, "activateConfiguration");

    private static final Logger logger = LoggerFactory.getLogger(OrchManager.class);
    private final JMXConnection jmxConnection;
    private final Manager eventHandlerManager;

    public PerfManager(Manager eventHandlerManager, JMXConnection jmxConnection) {
        this.jmxConnection = jmxConnection;
        this.eventHandlerManager = eventHandlerManager;
    }

    public Map<String, Boolean> performanceConfigurationForServer() {
        final Map<String, Boolean> applicableValues = new HashMap<>();
        jmxConnection.invoke(DMS_CONFIG_MBEAN, new JMXConnection.ProcessBeanType() {
            @Override
            public void execute(JMXConnection.ProcessingBean bean) {
                Map<String, String> beanProperties = bean.getProperties();
                logger.trace("Processing bean {}", beanProperties);
                if (beanProperties.containsKey("ServerName")) { // this give server specific beans.
                    Object allEventRouteStatusObject = bean.getValue("AllEventRouteStatus");
                    if (!(allEventRouteStatusObject instanceof TabularData))
                        throw new OIMAdminException("Located value of JMX Attribute value 'AllEventRouteStatus'  of type "
                                + (allEventRouteStatusObject == null ? "null" : allEventRouteStatusObject.getClass()) + " expected "
                                + TabularData.class + "Value : " + allEventRouteStatusObject + " Bean: " + beanProperties);
                    TabularData routeStatus = (TabularDataSupport) allEventRouteStatusObject;
                    Collection routeStatusValue = routeStatus.values();
                    if (routeStatusValue.size() != 1)
                        throw new OIMAdminException("Value of JMX Attribute 'AllEventRouteStatus' was expected to contain 1 element, found " + routeStatusValue.size() + ". Bean: " + beanProperties);
                    Object routeStatusElementValues = routeStatusValue.toArray()[0];
                    if (!(routeStatusElementValues instanceof CompositeData)) {
                        throw new OIMAdminException("Value if JMX Attribute 'AllEventRouteStatus' must be of type "
                                + CompositeData.class + ". Found " + (routeStatusElementValues == null ? "null" : routeStatusElementValues.getClass())
                                + " Bean: " + beanProperties);
                    }
                    applicableValues.put(beanProperties.get("ServerName"), JMXUtils.<Boolean>extractData(
                            (TabularData) JMXUtils.extractData(
                                    (CompositeData) routeStatusElementValues).get("value")).get("mbeanCreationDestination"));
                }
            }
        });
        String[] servers = OIMUtils.getOIMServerDetails(jmxConnection).Servers;
        if (servers != null && servers.length > 0) {
            Map<String, Boolean> serverConfiguration = new HashMap<>();
            for (String server : servers) {
                serverConfiguration.put(server, applicableValues.get(server));
            }
            logger.debug("Performance bean configuration {}", serverConfiguration);
            return serverConfiguration;
        }
        logger.debug("Performance bean configuration {}", applicableValues);
        return applicableValues;
    }

    public void enablePerformance(String serverName) {
        setPerformance(serverName, true);
    }

    public void disablePerformance(String serverName) {
        setPerformance(serverName, false);
    }

    public void setPerformance(final String serverName, final boolean enable) {
        logger.debug("Setting performance on {} to {}", serverName, enable);
        if (Utils.isEmpty(serverName))
            return;
        final Map<String, Boolean> applicableValues = new HashMap<>();
        jmxConnection.invoke(DMS_CONFIG_MBEAN, new JMXConnection.ProcessBeanType() {
            @Override
            public void execute(JMXConnection.ProcessingBean bean) {
                Map<String, String> beanProperties = bean.getProperties();
                logger.trace("Processing bean {}", beanProperties);
                if (beanProperties.containsKey("ServerName") && beanProperties.get("ServerName").equalsIgnoreCase(serverName)) { // this give server specific beans.
                    logger.debug("Identified performance bean as {}, Invoking {} with parameters {}, {}, {}", new Object[]{beanProperties, ADD_EVENT_ROUTE, null, "mbeanCreationDestination", enable});
                    bean.invoke(ADD_EVENT_ROUTE, null, "mbeanCreationDestination", enable);
                    logger.debug("Invoked the set operation. Invoking the {} operation", ACTIVATE_CONFIGURATION);
                    bean.invoke(ACTIVATE_CONFIGURATION);
                    logger.debug("Invoked the activate operation.");
                }
            }
        });
        logger.debug("Performance set.");
    }

    public Map<String, List<PerfConfiguration>> getPerformanceConfiguration(Config.Configuration configuration) {
        Map<String, List<PerfConfiguration>> performanceConfiguration = new HashMap<>();
        logger.debug("Trying to retrieve OIM Operations available");
        Map<String, OperationDetail> eventHandlerOperations = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (eventHandlerManager != null) {
            Set<OperationDetail> eventHandlerOperationDetails = eventHandlerManager.getOperations();
            for (OperationDetail operationDetail : eventHandlerOperationDetails) {
                eventHandlerOperations.put(operationDetail.name, operationDetail);
            }
        }
        String options = configuration.getProperty(ATTR_PERFORMANCE_CONFIG_PREFIX + ATTR_PERFORMANCE_CONFIG_OPTIONS);
        if (!Utils.isEmpty(options)) {
            logger.debug("Trying to extract performance items from given options {}", options);
            String[] performanceItems = options.split(",");
            for (String performanceItem : performanceItems) {
                logger.trace("Processing item {}", performanceItem);
                String name = configuration.getProperty(ATTR_PERFORMANCE_CONFIG_PREFIX + performanceItem + ATTR_PERFORMANCE_CONFIG_NAME);
                logger.trace("Trying to validate performance item's name {}", name);
                if (!Utils.isEmpty(name)) {
                    List<PerfConfiguration> performanceMetrics = new ArrayList<>();
                    int missingCounter = 0;
                    int counter = 1;
                    while (missingCounter < 10) {
                        String attPrefix = ATTR_PERFORMANCE_CONFIG_PREFIX + performanceItem + "." + counter;
                        String typeAttr = attPrefix + ATTR_PERFORMANCE_CONFIG_TYPE;
                        logger.trace("Trying to locate type {}", typeAttr);
                        String type = configuration.getProperty(typeAttr);
                        logger.trace("Trying to validate type {}", type);
                        if (!Utils.isEmpty(type)) {
                            String displayName = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_DISPLAY_NAME);
                            logger.trace("Extracted display name {}", displayName);
                            switch (type) {
                                case "api":
                                    logger.trace("Processing {} as api", displayName);
                                    String beanName = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_NAME);
                                    logger.trace("Bean : {}", beanName);
                                    if (Utils.isEmpty(beanName))
                                        throw new NullPointerException("Failed to locate name of the bean using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_NAME + " property for " + displayName + " api.");
                                    String beanAttrPrefix = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_CALL);
                                    logger.trace("Attribute : {}", beanAttrPrefix);
                                    if (Utils.isEmpty(beanAttrPrefix))
                                        throw new NullPointerException("Failed to locate method being measured using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_CALL + " property for " + displayName + " api.");
                                    performanceMetrics.add(new PerfConfiguration(displayName, beanName, beanAttrPrefix));
                                    logger.trace("Processed API");
                                    break;
                                case "event-handler":
                                    logger.trace("Processing {} as event handler", displayName);
                                    String operationName = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_NAME);
                                    logger.trace("Validating operation name {}", operationName);
                                    if (Utils.isEmpty(operationName))
                                        throw new NullPointerException("Failed to locate " + attPrefix + ATTR_PERFORMANCE_CONFIG_NAME + " property for " + displayName + " event handler.");
                                    logger.trace("Retrieving operation for name {}", operationName);
                                    OperationDetail operation;
                                    if ((operation = eventHandlerOperations.get(operationName)) != null) {
                                        logger.trace("Trying to retrieve event handler for operation {}", operation);
                                        Details eventHandlers = eventHandlerManager.getEventHandlers(operation);
                                        if (eventHandlers != null) {
                                            for (int eventHandlerCounter = 0; eventHandlerCounter < eventHandlers.size(); eventHandlerCounter++) {
                                                logger.trace("Processing item {}", eventHandlerCounter);
                                                Map<String, Object> eventHandlerDetail = eventHandlers.getItemAt(eventHandlerCounter);
                                                logger.trace("Processing Event handler details {}", eventHandlerDetail);
                                                String eventHandlerName = (String) eventHandlerDetail.get(Manager.EVENT_HANDLER_DETAILS.NAME.columnName);
                                                performanceMetrics.add(new PerfConfiguration((displayName == null ? operationName : displayName) + " [" + eventHandlerName + "]",
                                                        "/" + eventHandlerName, "execute"));
                                            }
                                        }
                                    }
                                    logger.trace("Processed Event Handler");
                                    break;
                                case "adapter":
                                    logger.trace("Processing adapter");
                                    String adapterBeanName = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_NAME);
                                    logger.trace("Validating bean name {}", adapterBeanName);
                                    if (Utils.isEmpty(adapterBeanName))
                                        throw new NullPointerException("Failed to locate name of the bean using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_NAME + " property for " + displayName + " adapter.");
                                    String adapterBeanAttrPrefix = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_CALL);
                                    logger.trace("Validating bean attribute {}", adapterBeanAttrPrefix);
                                    if (Utils.isEmpty(adapterBeanAttrPrefix))
                                        throw new NullPointerException("Failed to locate method being measured using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_CALL + " property for " + displayName + " api.");
                                    performanceMetrics.add(new PerfConfiguration(displayName, adapterBeanName, adapterBeanAttrPrefix));
                                    logger.trace("Processed Adapter");
                                    break;
                            }
                        } else {
                            logger.debug("Skipped {} items since last attribute as no type({}) was found", missingCounter, typeAttr);
                            missingCounter++;
                        }
                        counter++;
                    }
                    performanceConfiguration.put(name, performanceMetrics);
                } else {
                    logger.debug("Skipping item {} since it does not have any associated name", performanceItem);
                }
            }
        }
        return performanceConfiguration;
    }


    public Set<PerfConfiguration> getPerformanceConfiguration(final String serverName) {
        final Set<PerfConfiguration> allPerformanceConfiguration = new TreeSet<>(new CASE_INSENSITIVE_COMPARATOR());
        jmxConnection.invoke(ALL_DMS_PERF_MBEANS, new JMXConnection.ProcessBeanType() {
            @Override
            public void execute(JMXConnection.ProcessingBean bean) {
                logger.trace("Processing bean {}", bean);
                if (bean.getProperties().containsKey("Location") && bean.getProperties().get("Location").equalsIgnoreCase(serverName)) {
                    List<String> beanAttributeNames = bean.getAttributeNames();
                    if (beanAttributeNames != null) {
                        for (String beanAttributeName : beanAttributeNames) {
                            if (beanAttributeName.endsWith(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS.beanNameSuffix)) {
                                String attributeName = beanAttributeName.substring(0, beanAttributeName.indexOf(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS.beanNameSuffix));
                                logger.trace("Adding performance attribute {} for bean {}", attributeName, bean.getBean());
                                PerfConfiguration perfConfiguration = new PerfConfiguration(null, bean.getBean(), attributeName);
                                allPerformanceConfiguration.add(perfConfiguration);
                            }
                        }
                    }
                }
            }
        });
        return allPerformanceConfiguration;
    }

    public PerformanceData.Snapshot capturePerformanceData(final String serverName, final PerfConfiguration perfConfiguration) {
        Map<PerfConfiguration.DATA_POINT, Object> performanceData = new HashMap<>();
        for (PerfConfiguration.DATA_POINT data_point : PerfConfiguration.DATA_POINT.values()) {
            final PerfConfiguration.DATA_POINT processingDataPoint = data_point;
            final List<Object> performanceDataValue = new ArrayList<>();
            jmxConnection.invoke(perfConfiguration.mBean, new JMXConnection.ProcessBeanType() {
                @Override
                public void execute(JMXConnection.ProcessingBean bean) {
                    logger.trace("Processing bean {}", bean);
                    if (bean.getProperties().containsKey("Location") && bean.getProperties().get("Location").equalsIgnoreCase(serverName)) {
                        performanceDataValue.add(bean.getValue(perfConfiguration.attributeName + processingDataPoint.beanNameSuffix));
                    }
                }
            });
            if (performanceDataValue.size() == 1)
                performanceData.put(data_point, performanceDataValue.get(0));
            else if (performanceDataValue.size() > 1) {
                logger.warn("Performance capture: Found {} beans corresponding to {} while capturing data for {}. Using first value identified.", new Object[]{performanceDataValue.size(), perfConfiguration.mBean, perfConfiguration.attributeName});
                performanceData.put(data_point, performanceDataValue.get(0));
            }
        }
        return new PerformanceData.Snapshot(performanceData);
    }

    private class CASE_INSENSITIVE_COMPARATOR implements Comparator<PerfConfiguration> {

        @Override
        public int compare(PerfConfiguration o1, PerfConfiguration o2) {
            if (o1 == null || o2 == null)
                throw new NullPointerException("Can not compare null PerfConfiguration object.");
            return o1.displayName.compareToIgnoreCase(o2.displayName);
        }
    }

}
