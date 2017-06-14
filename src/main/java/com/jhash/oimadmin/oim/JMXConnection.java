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

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.Config.Configuration;
import com.jhash.oimadmin.Config.PLATFORM;
import com.jhash.oimadmin.Connection;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.*;

public class JMXConnection extends AbstractConnection {

    public static final String ATTR_JMX_PROTOCOL = "jmx_protocol";
    public static final String ATTR_JMX_HOSTNAME = "jmx_host";
    public static final String ATTR_JMX_PORT = "jmx_port";
    public static final String ATTR_JMX_USER = "jmx_user";
    public static final String ATTR_JMX_PWD = "jmx_pwd";


    private static final Logger logger = LoggerFactory.getLogger(JMXConnection.class);
    private final Map<OIM_JMX_BEANS, ObjectInstance> beanCache = new HashMap<>();
    private final Map<OIM_JMX_BEANS, List<ObjectInstance>> beanTypeCache = new HashMap<>();
    private JMXConnector jmxConnector = null;
    private MBeanServerConnection serverConnection = null;
    private Config.OIM_VERSION oimVersion = null;

    public JMXConnection() {
        STRING_REPRESENTATION = "JMXConnection:";
    }

    @Override
    protected void initializeConnection(Configuration config) {
        // TODO: Encrypt password
        logger.debug("Trying to initialize JMX Connection with configuration {}", config);
        try {
            logger.debug("Trying to create JMX Connector");
            jmxConnector = createJMXConnector(config);
            logger.debug("Created JMX Connector");
            String connectionId = jmxConnector.getConnectionId();
            STRING_REPRESENTATION = STRING_REPRESENTATION + "(Connection ID: " + connectionId + ")";
            serverConnection = jmxConnector.getMBeanServerConnection();
            isConnected = true;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to create JMX Connector while initializing JMX Connection " + this,
                    exception);
        }
        oimVersion = OIMUtils.getVersion(this);
        logger.debug("Initialized JMX Connection");
    }

    private JMXConnector createJMXConnector(Configuration config) {
        logger.debug("Trying to create JMX Connector using configuration {}", config);
        Map<String, Object> env = new HashMap<>();
        JMXServiceURL serviceUrl;
        String hostname = config.getProperty(ATTR_JMX_HOSTNAME);
        logger.debug("Host name ({})={}", ATTR_JMX_HOSTNAME, hostname);
        if (hostname == null || hostname.isEmpty())
            throw new NullPointerException("Failed to locate hostname attribute " + ATTR_JMX_HOSTNAME
                    + " in JMX configuration for " + name);
        String portString = config.getProperty(ATTR_JMX_PORT);
        logger.debug("Port ({})={}", ATTR_JMX_PORT, portString);
        if (portString == null || portString.isEmpty())
            throw new NullPointerException("Failed to locate port attribute " + ATTR_JMX_PORT
                    + " in JMX configuration for " + name);
        String username = config.getProperty(ATTR_JMX_USER);
        logger.debug("User ({})={}", ATTR_JMX_USER, hostname);
        String password = config.getProperty(ATTR_JMX_PWD);
        logger.debug("Password ({})=********", ATTR_JMX_PWD);
        logger.debug("Trying to identify platform by looking for {} in config", Connection.ATTR_CONN_PLATFORM);
        PLATFORM platform = PLATFORM.fromString(config.getProperty(Connection.ATTR_CONN_PLATFORM));
        logger.debug("Trying to invoke JMX Connector process for platform  {}", platform);
        switch (platform) {
            case WEBLOGIC:
                String protocol = config.getProperty(ATTR_JMX_PROTOCOL, "t3");
                int port = Integer.valueOf(portString);
                String jndiroot = "/jndi/";
                String mserver = "weblogic.management.mbeanservers.domainruntime";
                try {
                    logger.debug(
                            "Trying to create JMX Service URL with protocol {}, hostname {}, port {}, jndiroot+mserver {} for weblogic",
                            new Object[]{protocol, hostname, port, jndiroot + mserver});
                    serviceUrl = new JMXServiceURL(protocol, hostname, port, jndiroot + mserver);
                    logger.debug("Successfully created JMX Service URL");
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to create weblogic JMX connection for connection " + name + ".",
                            exception);
                }
                env.put("java.naming.security.principal", username);
                env.put("java.naming.security.credentials", password);
                env.put("jmx.remote.protocol.provider.pkgs", "weblogic.management.remote");
                break;
            case WEBSPHERE:
                try {
                    logger.debug("Trying to create JMX Service URL with hostname {}, port {} for websphere", new Object[]{
                            hostname, portString});
                    serviceUrl = new JMXServiceURL("service:jmx:iiop://" + hostname + ":" + portString
                            + "/jndi/JMXConnector");
                    logger.debug("Successfully created JMX Service URL");
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to create websphere JMX connection for connection " + name + ".",
                            exception);
                }
                env.put("java.naming.provider.url", "iiop://" + hostname + ":" + portString);
                env.put("java.naming.factory.initial", "com.ibm.websphere.naming.WsnInitialContextFactory");
                String[] credentials = new String[2];
                credentials[0] = username;
                credentials[1] = password;
                env.put("jmx.remote.credentials", credentials);
                break;
            case JBOSS:
                try {
                    logger.debug("Trying to create JMX Service URL with hostname {}, port {} for JBoss", new Object[]{
                            hostname, portString});
                    serviceUrl = new JMXServiceURL("service:jmx:rmi://" + hostname + "/jndi/rmi://" + hostname + ":"
                            + portString + "/jmxconnector");
                    logger.debug("Successfully created JMX Service URL");
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to create JBoss JMX connection for connection " + name + ".",
                            exception);
                }
                break;
            default:
                throw new UnsupportedOperationException("The platform " + platform + " is not supported.");
        }
        STRING_REPRESENTATION = STRING_REPRESENTATION + "[" + serviceUrl + "]";
        try {
            logger.debug("Trying to connect to JMX server");
            return JMXConnectorFactory.connect(serviceUrl, env);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to connect to JMX server of connection " + name + ".", exception);
        }
    }

    @Override
    protected void destroyConnection() {
        logger.debug("Trying to destroy JMXConnection {}", this);
        if (jmxConnector != null) {
            try {
                logger.debug("Trying to close the  JMXConnector {}", jmxConnector);
                jmxConnector.close();
                logger.debug("Successfully closed the  JMXConnector {}", jmxConnector);
            } catch (Exception exception) {
                logger.warn("Failed to close the JMX Connector " + this + ". Ignoring the error", exception);
            }
            jmxConnector = null;
        }
        logger.debug("Destroyed JMXConnection {}", this);
    }

    public MBeanServerConnection getConnection() {
        if (!isConnected || serverConnection == null)
            throw new IllegalStateException("The JMX Connection is not initialized");
        return serverConnection;
    }

    public Config.OIM_VERSION getVersion() {
        return oimVersion;
    }

    private void initializeBeanCache() {
        logger.trace("Initializing Bean cache for connection {}", this);
        synchronized (beanCache) {
            if (beanCache.isEmpty()) {
                Set<ObjectInstance> allBeans;
                try {
                    allBeans = getConnection().queryMBeans(null, null);
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to get a list of all the beans ", exception);
                }
                for (ObjectInstance bean : allBeans) {
                    try {
                        String beanName = JMXUtils.getName(bean);
                        String beanType = JMXUtils.getType(bean);
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
                            List<ObjectInstance> beanList;
                            if (beanTypeCache.containsKey(mappedBean)) {
                                beanList = beanTypeCache.get(mappedBean);
                            } else {
                                beanList = new ArrayList<>();
                                beanTypeCache.put(mappedBean, beanList);
                            }
                            beanList.add(bean);
                        }
                    } catch (Exception exception) {
                        throw new OIMAdminException("Failed to process bean " + bean, exception);
                    }
                }
            }
        }
        logger.trace("Initialized Bean cache for connection {}", this);
    }

    private List<ObjectInstance> getBeansOfType(OIM_JMX_BEANS jmxBeans) {
        if (beanTypeCache.isEmpty()) {
            initializeBeanCache();
        }
        if (!beanTypeCache.containsKey(jmxBeans)) {
            Set<ObjectInstance> result = JMXUtils.getJMXBean(getConnection(), jmxBeans);
            if (result.isEmpty()) {
                return null;
            } else {
                List<ObjectInstance> resultAsList = new ArrayList<>(result);
                beanTypeCache.put(jmxBeans, resultAsList);
                if (result.size() == 1 && !Utils.isEmpty(jmxBeans.name)) {
                    beanCache.put(jmxBeans, resultAsList.get(0));
                }
                if (Utils.isEmpty(jmxBeans.name)) {
                    for (ObjectInstance resultItem : resultAsList) {
                        beanCache.put(new OIM_JMX_BEANS(resultItem), resultItem);
                    }
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
            Set<ObjectInstance> result = JMXUtils.getJMXBean(getConnection(), jmxBean);
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

    public <T> T getValue(OIM_JMX_BEANS bean, String attributeName) {
        if (bean == null || Utils.isEmpty(attributeName)) {
            logger.info("Either bean {} or requested attribute {} is null", bean, attributeName);
            return null;
        }
        ObjectInstance objectInstance = getBean(bean);
        if (objectInstance == null) {
            logger.warn("Could not locate the bean corresponding to {}", bean);
            return null;
        }
        return (T) JMXUtils.getValue(getConnection(), objectInstance, attributeName);
    }

    public void setValue(OIM_JMX_BEANS bean, String attributeName, Object value) {
        if (bean == null || Utils.isEmpty(attributeName)) {
            logger.info("Either bean {} or attribute to set {} is null", bean, attributeName);
            return;
        }
        ObjectInstance objectInstance = getBean(bean);
        if (objectInstance == null) {
            logger.warn("Could not locate the bean corresponding to {}", bean);
            return;
        }
        JMXUtils.setValue(getConnection(), objectInstance, attributeName, value);
    }

    public Object invoke(JMX_BEAN_METHOD method, Object... parameterValues) {
        if (method == null) {
            logger.info("Method to invoke is null");
            return null;
        }
        if (method.bean == null) {
            logger.info("Bean associated with method {} to invoke is null", method);
            return null;
        }
        ObjectInstance objectInstance = getBean(method.bean);
        if (objectInstance == null) {
            logger.warn("Could not locate the bean corresponding to {}", method.bean);
            return null;
        }
        return JMXUtils.invoke(getConnection(), objectInstance, method.methodName, method.methodParameterClass, parameterValues);
    }

    public void invoke(final OIM_JMX_BEANS bean, ProcessBeanType processBean) {
        if (bean == null) {
            logger.info("No bean was provided for invocation of {}", processBean);
            return;
        }
        if (bean.name == null) {
            List<ObjectInstance> objectInstances = getBeansOfType(bean);
            if (objectInstances != null) {
                for (ObjectInstance objectInstance : objectInstances) {
                    processBean.execute(new JMXUtils.ProcessingBeanImpl(getConnection(), objectInstance));
                }
            }
        } else {
            ObjectInstance objectInstance = getBean(bean);
            processBean.execute(new JMXUtils.ProcessingBeanImpl(getConnection(), objectInstance));
        }

    }

    public interface ProcessBeanType {

        void execute(ProcessingBean bean);
    }

    public interface ProcessingBean {
        OIM_JMX_BEANS getBean();

        Object getValue(String attributeName);

        void setValue(String attributeName, Object value);

        Object invoke(JMX_BEAN_METHOD method, Object... parameterValues);
    }

    public static class OIM_JMX_BEANS {

        private static final Set<String> beanNames = new HashSet<>();
        private static final Set<String> beanTypeNames = new HashSet<>();
        private static final Map<String, OIM_JMX_BEANS> beanMapping = new HashMap<>();
        private static final Map<String, OIM_JMX_BEANS> beanTypeMapping = new HashMap<>();

        public final String name;
        public final String type;
        private final String stringRepresentation;

        public OIM_JMX_BEANS(String name) {
            this(name, null);
        }

        public OIM_JMX_BEANS(ObjectInstance bean) {
            this(JMXUtils.getName(bean), JMXUtils.getType(bean));
        }

        public OIM_JMX_BEANS(String name, String type) {
            if (name == null && type == null)
                throw new NullPointerException("JMX Bean definition can not have both name and type as null value");
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

        @Override
        public String toString() {
            return stringRepresentation;
        }

        @Override
        public int hashCode() {
            return stringRepresentation.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof OIM_JMX_BEANS && (this == o || stringRepresentation.equalsIgnoreCase(((OIM_JMX_BEANS) o).stringRepresentation));
        }

    }

    public static class JMX_BEAN_METHOD {
        final OIM_JMX_BEANS bean;
        final String methodName;
        final String[] methodParameterClass;
        final String stringRepresentation;

        public JMX_BEAN_METHOD(OIM_JMX_BEANS bean, String methodName) {
            this(bean, methodName, new String[0]);
        }

        public JMX_BEAN_METHOD(OIM_JMX_BEANS bean, String methodName, String[] methodParameterClass) {
            if (bean == null || methodName == null || methodParameterClass == null)
                throw new NullPointerException("JMX Bean Method definition can not have null value");
            this.bean = bean;
            this.methodName = methodName;
            this.methodParameterClass = methodParameterClass;
            stringRepresentation = "JMX Bean Method [" + (bean.name == null ? "*:" + bean.type : bean.name) + "." + methodName + "()]";
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }

}
