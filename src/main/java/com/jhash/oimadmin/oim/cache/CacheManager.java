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

package com.jhash.oimadmin.oim.cache;

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.oim.Details;
import com.jhash.oimadmin.oim.JMXConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheManager {

    private static final JMXConnection.OIM_JMX_BEANS CACHE_MBEAN_NAME = new JMXConnection.OIM_JMX_BEANS("Cache");
    private static final JMXConnection.OIM_JMX_BEANS CACHE_PROVIDER_MBEAN_NAME = new JMXConnection.OIM_JMX_BEANS("XLCacheProvider");
    private static final JMXConnection.OIM_JMX_BEANS CACHE_CATEGORIES = new JMXConnection.OIM_JMX_BEANS(null, "XMLConfig.CacheConfig.CacheCategoryConfig");
    private static final JMXConnection.JMX_BEAN_METHOD CACHE_IS_CLUSTERED = new JMXConnection.JMX_BEAN_METHOD(CACHE_MBEAN_NAME, "isClustered", new String[]{});
    private static final JMXConnection.JMX_BEAN_METHOD CACHE_IS_ENABLED = new JMXConnection.JMX_BEAN_METHOD(CACHE_MBEAN_NAME, "isEnabled", new String[]{});
    private static final JMXConnection.JMX_BEAN_METHOD CACHE_IS_THREAD_LOCAL_CACHE_ENABLED = new JMXConnection.JMX_BEAN_METHOD(CACHE_MBEAN_NAME, "isThreadLocalCacheEnabled", new String[]{});
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

    private final JMXConnection connection;

    public CacheManager(JMXConnection connection) {
        this.connection = connection;
    }

    public void setCacheDetails(Map<String, Object> cacheItem, OIM_CACHE_ATTRS cacheAttr, String value) {
        try {
            if (cacheItem != null) {
                if (cacheItem.containsKey("BEAN")) {
                    JMXConnection.OIM_JMX_BEANS bean = (JMXConnection.OIM_JMX_BEANS) cacheItem.get("BEAN");
                    switch (cacheAttr) {
                        case ENABLED:
                            connection.setValue(bean, "Enabled", Boolean.valueOf(value));
                            break;
                        case ExpirationTime:
                            connection.setValue(bean, "ExpirationTime", Integer.valueOf(value));
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
                        connection.setValue(CACHE_MBEAN_NAME, cacheAttr.nameValue, Boolean.valueOf(value));
                        break;
                    case ExpirationTime:
                        connection.setValue(CACHE_MBEAN_NAME, cacheAttr.nameValue, Integer.parseInt(value));
                        break;
                    case Provider:
                        connection.setValue(CACHE_MBEAN_NAME, cacheAttr.nameValue, value);
                        break;
                    case MulticastAddress:
                    case MulticastConfig:
                        connection.setValue(CACHE_PROVIDER_MBEAN_NAME, cacheAttr.nameValue, value);
                        break;
                    case Size:
                        connection.setValue(CACHE_PROVIDER_MBEAN_NAME, cacheAttr.nameValue, Integer.parseInt(value));
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
                    switch (connection.getVersion()) {
                        case OIM11GR2PS2:
                            try {
                                returnValue = connection.invoke(CACHE_IS_CLUSTERED);
                                break;
                            } catch (Exception exception) {
                                logger.debug("Catching an exception while trying to read isClustered attribute.", exception);
                            }
                        default:
                            returnValue = connection.getValue(CACHE_MBEAN_NAME, "Clustered");
                            break;
                    }
                    break;
                }
                case ENABLED: {
                    switch (connection.getVersion()) {
                        case OIM11GR2PS2:
                            try {
                                returnValue = connection.invoke(CACHE_IS_ENABLED);
                                break;
                            } catch (Exception exception) {
                                logger.debug("Catching an exception while trying to read isClustered attribute.", exception);
                            }
                        default:
                            returnValue = connection.getValue(CACHE_MBEAN_NAME, "Enabled");
                            break;
                    }
                    break;
                }
                case ThreadLocalCacheEnabled: {
                    switch (connection.getVersion()) {
                        case OIM11GR2PS2:
                            try {
                                returnValue = connection.invoke(CACHE_IS_THREAD_LOCAL_CACHE_ENABLED);
                                break;
                            } catch (Exception exception) {
                                logger.debug("Catching an exception while trying to read isClustered attribute.", exception);
                            }
                        default:
                            returnValue = connection.getValue(CACHE_MBEAN_NAME, "ThreadLocalCacheEnabled");
                            break;
                    }
                    break;
                }
                case ExpirationTime:
                case Provider: {
                    returnValue = connection.getValue(CACHE_MBEAN_NAME, attributeRequested.nameValue);
                    break;
                }
                case MulticastAddress:
                case MulticastConfig:
                case Size: {
                    returnValue = connection.getValue(CACHE_PROVIDER_MBEAN_NAME, attributeRequested.nameValue);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Invalid attribute " + attributeRequested + " requested");
            }
            logger.debug("Returning value {} of type {}", returnValue, returnValue != null ? returnValue : "null");
            return (T) returnValue;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to get attribute " + attributeRequested + " from bean " + CACHE_MBEAN_NAME, exception);
        }
    }

    public Details getCacheCategories() {
        try {
            logger.debug("Trying to locate cache categories...");
            final List<Map<String, Object>> categoryDetails = new ArrayList<>();
            connection.invoke(CACHE_CATEGORIES, new JMXConnection.ProcessBeanType() {
                @Override
                public void execute(JMXConnection.ProcessingBean bean) {
                    Map<String, Object> categoryDetail = new HashMap<>();
                    String name = (String) bean.getValue("Name");
                    categoryDetail.put("Name", name);
                    boolean isEnabled = (Boolean) bean.invoke(CACHE_IS_ENABLED);
                    categoryDetail.put("Enabled?", isEnabled);
                    int expirationTime = (Integer) bean.getValue("ExpirationTime");
                    categoryDetail.put("Expires in", expirationTime);
                    categoryDetail.put("BEAN", bean.getBean());
                    categoryDetails.add(categoryDetail);
                    logger.trace("Added cache detail {}", categoryDetail);
                }
            });
            logger.debug("Located cache details {}", categoryDetails);
            return new Details(categoryDetails, new String[]{"Name", "Enabled?", "Expires in"});
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to retrieve cache details", exception);
        }
    }

    public enum OIM_CACHE_ATTRS {
        CLUSTERED("Clustered"), ENABLED("Enabled"), ExpirationTime("ExpirationTime"), Provider("Provider"), ThreadLocalCacheEnabled("ThreadLocalCacheEnabled"),
        MulticastAddress("MulticastAddress"), MulticastConfig("MulticastConfig"), Size("Size");

        public final String nameValue;

        OIM_CACHE_ATTRS(String nameValue) {
            this.nameValue = nameValue;
        }
    }

}
