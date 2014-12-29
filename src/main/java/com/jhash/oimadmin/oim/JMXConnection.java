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
import com.jhash.oimadmin.Config.PLATFORM;
import com.jhash.oimadmin.Connection;
import com.jhash.oimadmin.OIMAdminException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JMXConnection extends AbstractConnection {

    public static final String ATTR_JMX_PROTOCOL = "jmx_protocol";
    public static final String ATTR_JMX_HOSTNAME = "jmx_host";
    public static final String ATTR_JMX_PORT = "jmx_port";
    public static final String ATTR_JMX_USER = "jmx_user";
    public static final String ATTR_JMX_PWD = "jmx_pwd";

    public static final String ATTR_WL_JMX_WLSERVERS = "com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean";

    private static final Logger logger = LoggerFactory.getLogger(JMXConnection.class);

    private JMXConnector jmxConnector = null;

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
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to create JMX Connector while initializing JMX Connection " + this,
                    exception);
        }
        isConnected = true;
        logger.debug("Initialized JMX Connection");
    }

    private JMXConnector createJMXConnector(Configuration config) {
        logger.debug("Trying to create JMX Connector using configuration {}", config);
        Map<String, Object> env = new HashMap<String, Object>();
        JMXServiceURL serviceUrl = null;
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
                int port = Integer.valueOf(portString).intValue();
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
        if (!isConnected)
            throw new IllegalStateException("The JMX Connection is not initialized");
        try {
            return jmxConnector.getMBeanServerConnection();
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to get connection to JMX Server " + name, exception);
        }
    }

    public Set<String> getRuntimeServers() {
        logger.debug("Trying to get name of servers running in the JMX server domain");
        Set<String> runtimeServerNames = new HashSet<String>();
        logger.debug("Trying to get MBeanServerConnection connection");
        MBeanServerConnection connection = getConnection();
        logger.debug("Trying to get attribute ServerRuntimes for JMX Bean {} on connection {}", ATTR_WL_JMX_WLSERVERS,
                connection);
        try {
            ObjectName[] serverRuntimeObjects = (ObjectName[]) (connection.getAttribute(new ObjectName(
                    ATTR_WL_JMX_WLSERVERS), "ServerRuntimes"));
            logger.debug("Trying to validate received runtime objects {}", serverRuntimeObjects);
            if (serverRuntimeObjects != null && serverRuntimeObjects.length > 0) {
                logger.debug("Processing all runtime objects");
                for (ObjectName serverRuntimeObject : serverRuntimeObjects) {
                    logger.debug("Trying to get attribute Name for JMX Bean {}", serverRuntimeObject);
                    String name = (String) connection.getAttribute(serverRuntimeObject, "Name");
                    logger.debug("Read attribute Name as {}", name);
                    runtimeServerNames.add(name);
                }
                logger.debug("Processed all runtime objects");
            } else {
                logger.debug("No Runtime Objects were returned");
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Could not locate servers running in the JMX server domain", exception);
        }
        return runtimeServerNames;
    }
}
