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
package com.jhash.oimadmin;

import com.jhash.oimadmin.ui.OIMAdmin;
import com.jhash.oimadmin.ui.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.InvalidParameterException;
import java.util.*;

public class Config {
    public static final String DEF_CONFIG_LOC = "config.properties";
    public static final String ATTR_NAME_PREFIX = "sysadmin.";
    public static final String ATTR_NAME_WORK_AREA = "workhome";
    public static final String VAL_WORK_AREA_BASE_PREFIX_DEFAULT = "user.home";
    public static final String VAL_WORK_AREA_BASE = ".oimadm";
    public static final String VAL_WORK_AREA_TMP = "tmp";
    public static final String VAL_WORK_AREA_CLASSES = File.separator + VAL_WORK_AREA_TMP + File.separator + "compile"
            + File.separator + "classes";
    public static final String VAL_WORK_AREA_CONF = File.separator + "conf";
    public static final String VAL_CONF_FILE_ZIP = "configuration.zip";
    public static final String VAL_CONFIG_PROP_FILE_NAME = "config.properties";

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private Map<String, Properties> oimConnectionConfiguration = new HashMap<String, Properties>();
    private List<String> oimConnectionNames = new ArrayList<>();
    private OIMAdmin oimAdmin;
    private String workArea = null;
    private String configurationLocation = null;

    public void load(OIMAdmin oimAdmin) {
        this.oimAdmin = oimAdmin;
        logger.debug("Trying to setup the work area for application");
        workArea = setupWorkArea();
        if (workArea == null || workArea.isEmpty())
            throw new NullPointerException("Failed to setup work area for the application");
        logger.debug("Trying to load configuration");
        configurationLocation = load();
        logger.debug("Loaded configuration");
    }

    private String load() {
        logger.debug("Trying to load configuration");
        InputStream propertyFileStream = null;
        String configLocation = DEF_CONFIG_LOC;
        try {
            String userConfigurationFileLocation = workArea + File.separator + VAL_WORK_AREA_CONF + File.separator + VAL_CONFIG_PROP_FILE_NAME;
            File userConfigurationFile = new File(userConfigurationFileLocation);
            logger.debug("Trying to validate whether configuration file {} exists and is readable");
            if (userConfigurationFile.exists() && userConfigurationFile.canRead()) {
                logger.debug("Trying to read configuration file from location {}", userConfigurationFileLocation);
                configLocation = userConfigurationFileLocation;
                propertyFileStream = new FileInputStream(userConfigurationFile);
            } else {
                logger.debug("Trying to read configuration from location {}", configLocation);
                propertyFileStream = ClassLoader.getSystemResourceAsStream(configLocation);
            }
            Properties configuration = new Properties();
            logger.debug("Trying to load configuration from location {}", configLocation);
            configuration.load(propertyFileStream);
            processOIM(configuration);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to load configuration file " + configLocation, exception);
        } finally {
            if (propertyFileStream != null) {
                try {
                    propertyFileStream.close();
                } catch (Exception exception) {
                    logger.warn("Error occurred while closing the input stream for configuration " + configLocation,
                            exception);
                }
            }
        }
        logger.debug("Loaded configuration from location {}", configLocation);
        return configLocation;
    }

    private void processOIM(Properties configuration) {
        int totalConnections = -1;
        logger.debug("Trying to process the configuration information to extract OIM connection details");
        Map<Integer, Properties> oimConnectionConfigurationList = new HashMap<Integer, Properties>();
        for (Object oimPropertyNameObject : configuration.keySet()) {
            String oimPropertyName = (String) oimPropertyNameObject;
            logger.debug("Processing property {}", oimPropertyName);
            if (oimPropertyName != null && oimPropertyName.startsWith(ATTR_NAME_PREFIX)) {
                String[] keyValues = oimPropertyName.split("\\.");
                if (keyValues.length == 3) {
                    int configurationIndex = Integer.parseUnsignedInt(keyValues[1]);
                    Properties oimProperty = null;
                    if (oimConnectionConfigurationList.containsKey(configurationIndex))
                        oimProperty = oimConnectionConfigurationList.get(configurationIndex);
                    else
                        oimProperty = new Properties();
                    oimProperty.setProperty(keyValues[2], configuration.getProperty(oimPropertyName));
                    oimConnectionConfigurationList.put(configurationIndex, oimProperty);

                    if (configurationIndex > totalConnections)
                        totalConnections = configurationIndex;
                } else {
                    logger.debug("Ignoring {} key since it is not in oim.<>.<> format", oimPropertyName);
                }
            } else {
                logger.debug("Ignoring {} key since it does not start with {} i.e. not an OIM property",
                        oimPropertyName, ATTR_NAME_PREFIX);
            }
        }
        logger.debug("Trying to create a list of OIM Connections in specified order");
        for (int counter = 0; counter <= totalConnections; counter++) {
            if (oimConnectionConfigurationList.containsKey(counter)) {
                if (oimConnectionConfigurationList.get(counter) != null &&
                        oimConnectionConfigurationList.get(counter).getProperty(Connection.ATTR_CONN_NAME) != null) {
                    oimConnectionNames.add(oimConnectionConfigurationList.get(counter).getProperty(Connection.ATTR_CONN_NAME));
                } else {
                    oimConnectionNames.add(null);
                }
            } else {
                oimConnectionNames.add(null);
            }
        }
        logger.debug("Created the list of OIM Connection as {}", oimConnectionNames);
        logger.debug(
                "Trying to create the configuration map using the {} attribute as key from the read configuration",
                Connection.ATTR_CONN_NAME);
        for (Properties oimProperty : oimConnectionConfigurationList.values()) {
            if (oimProperty.containsKey(Connection.ATTR_CONN_NAME)) {
                String connectionName = oimProperty.getProperty(Connection.ATTR_CONN_NAME);
                logger.debug("Identified OIM Connection Configuration {}", connectionName);
                oimConnectionConfiguration.put(connectionName, oimProperty);
            } else {
                logger.debug("Ignoring {} since it does not have any associated name i.e. key {}.", oimProperty,
                        Connection.ATTR_CONN_NAME);
            }
        }
        logger.debug("Processed the configuration information to extract OIM connection details");
    }

    private String setupWorkArea() {
        logger.debug("Trying to setup work area for application");
        String userWorkArea = null;
        try {
            logger.debug(
                    "Trying to validate if system property {} can be used as work area out of system properties {}",
                    VAL_WORK_AREA_BASE_PREFIX_DEFAULT, System.getProperties());
            String userHome = null;
            if (System.getProperties().containsKey(ATTR_NAME_WORK_AREA)
                    && (userHome = System.getProperty(ATTR_NAME_WORK_AREA)) != null
                    && (new File(userHome)).exists()) {
                userWorkArea = userHome + File.separator + VAL_WORK_AREA_BASE;
            } else if (System.getProperties().containsKey(VAL_WORK_AREA_BASE_PREFIX_DEFAULT)
                    && (userHome = System.getProperty(VAL_WORK_AREA_BASE_PREFIX_DEFAULT)) != null
                    && (new File(userHome)).exists()) {
                userWorkArea = userHome + File.separator + VAL_WORK_AREA_BASE;
            } else {
                throw new NullPointerException("Failed to locate a valid base folder (using attribute " + ATTR_NAME_WORK_AREA + " or " + VAL_WORK_AREA_BASE_PREFIX_DEFAULT + ") for managing configurations and temporary files");
            }
            logger.debug("User Home {} is available, trying to validate the work area{}", userHome, userWorkArea);
            validateBaseArea(userWorkArea);
            String tmpWorkArea = userWorkArea + File.separator + VAL_WORK_AREA_TMP;
            logger.debug("Validated work area, trying to validate the tmp work area {}", tmpWorkArea);
            validateBaseArea(tmpWorkArea);
            logger.debug("Validated tmp work area");
            String classWorkArea = userWorkArea + File.separator + VAL_WORK_AREA_CLASSES;
            logger.debug("Trying to validate the class work area {}", classWorkArea);
            validateBaseArea(classWorkArea);
            logger.debug("Validated class work area");
            String configWorkArea = userWorkArea;
            logger.debug("Trying to validate the configuration work area {}", configWorkArea);
            validateBaseArea(configWorkArea);
            logger.debug("Validated configuration work area");
            String configurationFileLocation = userWorkArea + File.separator + VAL_WORK_AREA_CONF + File.separator + VAL_CONFIG_PROP_FILE_NAME;
            File configFile = new File(configurationFileLocation);
            logger.debug("Trying to validate if configuration file {} has been initialized", configurationFileLocation);
            if (!configFile.exists()) {
                String jarLocation = Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                logger.debug("Trying to locate the directory of jar {} containing Config class", jarLocation);
                File name = new File(jarLocation);
                if (!name.isDirectory()) {
                    logger.debug("Assuming that application is running in production mode. Trying to locate directory.");
                    String jarParentDirectory = name.getParent();
                    if (jarParentDirectory != null) {
                        String configurationZipFileLocation = jarParentDirectory + File.separator + VAL_CONF_FILE_ZIP;
                        File configurationZipFile = new File(configurationZipFileLocation);
                        if (configurationZipFile.exists() && configurationZipFile.canRead()) {
                            Utils.extractJarFile(configWorkArea, configurationZipFileLocation);
                        } else {
                            logger.debug("Can not locate or read configuration zip file {}. Skipping unzip process", configurationZipFileLocation);
                        }
                    } else {
                        throw new NullPointerException("Failed to locate the directory containing the jar file " + jarLocation);
                    }
                } else {
                    logger.debug("Running in test mode, will not try to setup configuration area");
                }
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to setup the work area for the application in directory "
                    + userWorkArea, exception);
        }
        logger.debug("Completed setup of work area {} for application", userWorkArea);
        return userWorkArea;
    }

    private boolean validateBaseArea(String userWorkArea) {
        File workAreaBaseDirectory = new File(userWorkArea);
        if (!workAreaBaseDirectory.exists())
            workAreaBaseDirectory.mkdirs();
        if (!workAreaBaseDirectory.isDirectory()) {
            throw new InvalidParameterException("User work area " + userWorkArea + " is not a directory");
        }
        if (!workAreaBaseDirectory.canRead()) {
            throw new InvalidParameterException("User work area " + userWorkArea + " can not be read");
        }
        if (!workAreaBaseDirectory.canWrite()) {
            throw new InvalidParameterException("User work area " + userWorkArea + " can not be written into.");
        }
        return true;
    }

    public Configuration getConnectionDetails(String name) {
        if (oimConnectionConfiguration != null && oimConnectionConfiguration.containsKey(name))
            return new Configuration(oimConnectionConfiguration.get(name), this);
        else
            return new Configuration(new Properties(), this);
    }

    public Set<String> getConnectionNames() {
        if (oimConnectionConfiguration != null) {
            return oimConnectionConfiguration.keySet();
        } else {
            return new HashSet<String>();
        }
    }

    public <T extends UIComponent> T getUIComponent(Class<T> componentId) {
        return (T) oimAdmin.getUIComponent(componentId);
    }

    public String getWorkArea() {
        return workArea;
    }

    public void saveConfiguration(Configuration configuration) {
        Properties configurationFile = new Properties();
        boolean newConfigurationSaved = false;
        int counter;
        String configurationBeingSaved = configuration.getProperty(Connection.ATTR_CONN_NAME);
        for (counter = 0; counter < this.oimConnectionNames.size(); counter++) {
            String name = oimConnectionNames.get(counter);
            if (name == null || name.isEmpty())
                continue;
            Properties configurationDetail = null;
            if (name.equals(configurationBeingSaved)) {
                configurationDetail = configuration.configuration;
                newConfigurationSaved = true;
            } else {
                configurationDetail = oimConnectionConfiguration.get(name);
            }
            if (configurationDetail == null)
                throw new NullPointerException("Failed to locate configuration detail with name " + name);
            for (String attributeName : configurationDetail.stringPropertyNames()) {
                configurationFile.setProperty("sysadmin." + counter + "." + attributeName, configurationDetail.getProperty(attributeName));
            }
        }
        if (!newConfigurationSaved) {
            logger.trace("Adding the new configuration {} to configuration", configuration);
            Properties configurationDetail = configuration.configuration;
            for (String attributeName : configurationDetail.stringPropertyNames()) {
                configurationFile.setProperty("sysadmin." + counter + "." + attributeName, configurationDetail.getProperty(attributeName));
            }
        }
        File configurationFileObject = new File(configurationLocation);
        if (configurationFileObject.exists() && configurationFileObject.canWrite()) {
            try {
                configurationFile.store(new FileWriter(configurationFileObject), "Saved on " + new Date());
            } catch (Exception exception) {
                throw new OIMAdminException("Error occurred while saving updated configuration", exception);
            }
        } else {
            throw new OIMAdminException("Error occurred while saving updated configuration", new IOException("Configuration file " + configurationLocation + " either does not exist or is not writable. Can not save the configuration."));
        }
    }

    public enum PLATFORM {
        WEBSPHERE("websphere"), JBOSS("jboss"), UNKNOWN("unknown"), WEBLOGIC("weblogic");

        private final String name;

        private PLATFORM(String name) {
            this.name = name;
        }

        public static PLATFORM fromString(String platformName) {
            if (platformName == null || platformName.isEmpty())
                return PLATFORM.UNKNOWN;
            for (PLATFORM platform : PLATFORM.values()) {
                if (platformName.equalsIgnoreCase(platform.name))
                    return platform;
            }
            return PLATFORM.UNKNOWN;
        }

        public static List<String> valuesAsString() {
            ArrayList<String> valuesAsString = new ArrayList<>();
            for (PLATFORM platform : PLATFORM.values()) {
                valuesAsString.add(platform.name);
            }
            return valuesAsString;
        }
    }

    public static class Configuration {

        private final Properties configuration;
        private final Config config;

        private Configuration(Properties properties, Config config) {
            configuration = (Properties) properties.clone();
            this.config = config;
        }

        public String getWorkArea() {
            return config.getWorkArea();
        }

        public String getProperty(String propertyName) {
            return configuration.getProperty(propertyName);
        }

        public String getProperty(String propertyName, String defaultValue) {
            return configuration.getProperty(propertyName, defaultValue);
        }

        public void setProperty(String propertyName, String value) {
            configuration.setProperty(propertyName, value);
        }

        public Config getConfig() {
            return config;
        }

    }
}
