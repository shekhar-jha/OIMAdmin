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

package com.jhash.oimadmin.ui;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.OIMJMXWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OIMPerformanceTreeNode extends AbstractUIComponentTreeNode<OIMJMXWrapper> implements DisplayableNode<OIMCacheDetails> {

    public static final String ATTR_PERFORMANCE_CONFIG_PREFIX = "sysadmin.performance.";
    public static final String ATTR_PERFORMANCE_CONFIG_OPTIONS = "options";
    public static final String ATTR_PERFORMANCE_CONFIG_NAME = ".name";
    public static final String ATTR_PERFORMANCE_CONFIG_TYPE = ".type";
    public static final String ATTR_PERFORMANCE_CONFIG_DISPLAY_NAME =".display";
    public static final String ATTR_PERFORMANCE_CONFIG_CALL = ".call";

    private static final Logger logger = LoggerFactory.getLogger(OIMPerformanceTreeNode.class);

    private OIMJMXWrapper connection;
    Map<String, List<Map<String, Object>>> performanceItemDetails = new HashMap<>();

    public OIMPerformanceTreeNode(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        logger.debug("OIMPerformanceTreeNode({}, {}, {}, {})", new Object[]{name, configuration, selectionTree, displayArea});
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        connection = new OIMJMXWrapper();
        logger.debug("Trying to setup JMX Connection");
        connection.initialize(configuration);
        logger.debug("Trying to retrieve OIM Operations available");
        Set<OIMJMXWrapper.OperationDetail> eventHandlerOperations = connection.getOperations();
        String options = configuration.getProperty(ATTR_PERFORMANCE_CONFIG_PREFIX + ATTR_PERFORMANCE_CONFIG_OPTIONS);
        if (!Utils.isEmpty(options)) {
            logger.debug("Trying to extract performance items from given options {}", options);
            String[] performanceItems = options.split(",");
            for (String performanceItem : performanceItems) {
                logger.trace("Processing item {}", performanceItem);
                String name = configuration.getProperty(ATTR_PERFORMANCE_CONFIG_PREFIX + performanceItem + ATTR_PERFORMANCE_CONFIG_NAME);
                logger.trace("Trying to validate performance item's name {}", name);
                if (!Utils.isEmpty(name)) {
                    List<Map<String, Object>> performanceMetrics = new ArrayList<>();
                    int missingCounter = 0;
                    int counter=1;
                    while (missingCounter < 10) {
                        String attPrefix = ATTR_PERFORMANCE_CONFIG_PREFIX + performanceItem + "." + counter;
                        String typeAttr = attPrefix + ATTR_PERFORMANCE_CONFIG_TYPE;
                        logger.trace("Trying to locate type {}", typeAttr);
                        String type = configuration.getProperty(typeAttr);
                        logger.trace("Trying to validate type {}", type);
                        if (!Utils.isEmpty(type)) {
                            String displayName = configuration.getProperty(attPrefix +ATTR_PERFORMANCE_CONFIG_DISPLAY_NAME);
                            logger.trace("Extracted display name {}", displayName);
                            switch (type) {
                                case "api":
                                    logger.trace("Processing {} as api", displayName);
                                    Map<String, Object> performanceMetricForAPI = new HashMap<>();
                                    String beanName = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_NAME);
                                    logger.trace("Bean : {}", beanName);
                                    if (Utils.isEmpty(beanName))
                                        throw new NullPointerException("Failed to locate name of the bean using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_NAME + " property for " + displayName + " api.");
                                    String beanAttrPrefix = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_CALL);
                                    logger.trace("Attribute : {}", beanAttrPrefix);
                                    if (Utils.isEmpty(beanAttrPrefix))
                                        throw new NullPointerException("Failed to locate method being measured using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_CALL + " property for " + displayName + " api.");
                                    performanceMetricForAPI.put(OIMPerformanceDetails.ATTR_BEAN, new OIMJMXWrapper.OIM_JMX_BEANS(beanName));
                                    performanceMetricForAPI.put(OIMPerformanceDetails.ATTR_BEAN_ATTRIBUTE, beanAttrPrefix);
                                    performanceMetricForAPI.put(OIMPerformanceDetails.ATTR_NAME, (displayName==null?beanName:displayName));
                                    logger.trace("Adding {} to {}", performanceMetricForAPI, performanceMetrics);
                                    performanceMetrics.add(performanceMetricForAPI);
                                    logger.trace("Processed API");
                                    break;
                                case "event-handler":
                                    logger.trace("Processing {} as event handler", displayName);
                                    String operationName = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_NAME);
                                    logger.trace("Validating operation name {}", operationName);
                                    if (Utils.isEmpty(operationName))
                                        throw new NullPointerException("Failed to locate " + attPrefix + ATTR_PERFORMANCE_CONFIG_NAME + " property for " + displayName + " event handler.");
                                    for (OIMJMXWrapper.OperationDetail operation : eventHandlerOperations) {
                                        logger.trace("Trying to validate operation {} has name {}", operation, operationName);
                                        if (operation.name.equals(operationName)) {
                                            logger.trace("Trying to retrieve event handler for operation {}", operation);
                                            OIMJMXWrapper.Details eventHandlers = connection.getEventHandlers(operation);
                                            for (int eventHandlerCounter=0; eventHandlerCounter < eventHandlers.size(); eventHandlerCounter++) {
                                                logger.trace("Processing item {}", eventHandlerCounter);
                                                Map<String, Object> eventHandlerDetail = eventHandlers.getItemAt(eventHandlerCounter);
                                                logger.trace("Processing Event handler details {}", eventHandlerDetail);
                                                String eventHandlerName = (String) eventHandlerDetail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.NAME.columnName);
                                                Map<String, Object> performanceMetricForEventHandler = new HashMap<>();
                                                performanceMetricForEventHandler.put(OIMPerformanceDetails.ATTR_BEAN, new OIMJMXWrapper.OIM_JMX_BEANS("/" + eventHandlerName));
                                                performanceMetricForEventHandler.put(OIMPerformanceDetails.ATTR_BEAN_ATTRIBUTE, "execute");
                                                performanceMetricForEventHandler.put(OIMPerformanceDetails.ATTR_NAME, (displayName==null?operationName:displayName) + " [" + eventHandlerName + "]");
                                                logger.trace("Adding {} to {}", performanceMetricForEventHandler, performanceMetrics);
                                                performanceMetrics.add(performanceMetricForEventHandler);
                                            }
                                        }
                                    }
                                    logger.trace("Processed Event Handler");
                                    break;
                                case "adapter":
                                    logger.trace("Processing adapter");
                                    Map<String, Object> performanceMetricForAdapter = new HashMap<>();
                                    String adapterBeanName = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_NAME);
                                    logger.trace("Validating bean name {}", adapterBeanName);
                                    if (Utils.isEmpty(adapterBeanName))
                                        throw new NullPointerException("Failed to locate name of the bean using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_NAME + " property for " + displayName + " adapter.");
                                    String adapterBeanAttrPrefix = configuration.getProperty(attPrefix + ATTR_PERFORMANCE_CONFIG_CALL);
                                    logger.trace("Validating bean attribute {}", adapterBeanAttrPrefix);
                                    if (Utils.isEmpty(adapterBeanAttrPrefix))
                                        throw new NullPointerException("Failed to locate method being measured using property " + attPrefix + ATTR_PERFORMANCE_CONFIG_CALL + " property for " + displayName + " api.");
                                    performanceMetricForAdapter.put(OIMPerformanceDetails.ATTR_BEAN, new OIMJMXWrapper.OIM_JMX_BEANS(adapterBeanName));
                                    performanceMetricForAdapter.put(OIMPerformanceDetails.ATTR_BEAN_ATTRIBUTE, adapterBeanAttrPrefix);
                                    performanceMetricForAdapter.put(OIMPerformanceDetails.ATTR_NAME, (displayName==null?adapterBeanName:displayName));
                                    logger.trace("Adding {} to {}", performanceMetricForAdapter, performanceMetrics);
                                    performanceMetrics.add(performanceMetricForAdapter);
                                    logger.trace("Processed Adapter");
                                    break;
                            }
                        } else {
                            logger.debug("Skipped {} items since last attribute as no type({}) was found", missingCounter, typeAttr);
                            missingCounter++;
                        }
                        counter++;
                    }
                    performanceItemDetails.put(name, performanceMetrics);
                } else {
                    logger.debug("Skipping item {} since it does not have any associated name", performanceItem);
                }
            }
            for (String performanceItem: performanceItemDetails.keySet()) {
                List<Map<String, Object>> performanceMetrics = performanceItemDetails.get(performanceItem);
                AbstractUIComponentTreeNode node = new AbstractUIComponentTreeNode.DisplayComponentNode(performanceItem,
                        new OIMPerformanceDetails(performanceItem, connection, performanceMetrics, configuration, selectionTree, displayArea),
                        connection, configuration, selectionTree, displayArea);
                logger.trace("Adding node {} for item {}", node, performanceItem );
                selectionTree.addChildNode(this, node);
            }
        }
        logger.debug("Initialized {}", this);
    }

    @Override
    public OIMJMXWrapper getComponent() {
        return connection;
    }

    @Override
    public OIMCacheDetails getDisplayComponent() {
        return null;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying {}", this);
        if (performanceItemDetails != null) {
            performanceItemDetails.clear();
        }
        if (connection != null) {
            try {
                connection.destroy();
            }catch (Exception exception) {
                logger.warn("Failed to destroy JMX Connection " + connection, exception);
            }
            connection = null;
        }
        logger.debug("Destroyed {}", this);
    }
}
