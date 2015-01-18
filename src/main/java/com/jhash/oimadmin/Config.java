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
    private String workArea = null;
    private String configurationLocation = null;

    public List<String> getConnectionNames() {
        return new ArrayList<String>(oimConnectionConfiguration.keySet());
    }

    public Configuration getConnectionDetails(String oimConnectionName) {
        Properties properties = oimConnectionConfiguration.get(oimConnectionName);
        if (properties == null)
            properties = new Properties();
        Configuration configuration = new Configuration(properties, this);
        return configuration;
    }

    public void load() {
        logger.debug("Starting Configuration loading....");
        workArea = setupWorkArea();
        if (workArea == null || workArea.isEmpty())
            throw new NullPointerException("Failed to setup work area for the application");
        InputStream propertyFileStream = null;
        String configLocation = DEF_CONFIG_LOC;
        try {
            String userConfigurationFileLocation = workArea + File.separator + VAL_WORK_AREA_CONF + File.separator + VAL_CONFIG_PROP_FILE_NAME;
            File userConfigurationFile = new File(userConfigurationFileLocation);
            logger.debug("Trying to validate whether configuration file {} exists and is readable", userConfigurationFileLocation);
            if (userConfigurationFile.exists() && userConfigurationFile.canRead()) {
                configLocation = userConfigurationFileLocation;
                logger.debug("Trying to create input stream for file in location {}", userConfigurationFileLocation);
                propertyFileStream = new FileInputStream(userConfigurationFile);
            } else {
                logger.debug("Trying to create input stream from location {}", configLocation);
                propertyFileStream = ClassLoader.getSystemResourceAsStream(configLocation);
            }
            Properties configuration = new Properties();
            logger.debug("Trying to load configuration using {}", propertyFileStream);
            configuration.load(propertyFileStream);
            processOIM(configuration);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to load configuration.", exception);
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
        configurationLocation = configLocation;
        logger.debug("Loaded configuration from location {}", configLocation);
    }

    private void processOIM(Properties configuration) {
        int totalConnections = -1;
        logger.debug("Trying to extract OIM connection details from read configuration");
        Map<Integer, Properties> oimConnectionConfigurationList = new HashMap<Integer, Properties>();
        for (String oimPropertyName : configuration.stringPropertyNames()) {
            logger.trace("Processing property {}", oimPropertyName);
            if (oimPropertyName != null && oimPropertyName.startsWith(ATTR_NAME_PREFIX)) {
                String[] keyValues = oimPropertyName.split("\\.");
                if (keyValues.length == 3) {
                    Properties oimProperty = null;
                    int configurationIndex = -1;
                    try {
                        configurationIndex = Integer.parseUnsignedInt(keyValues[1]);
                    }catch(NumberFormatException exception) {
                        logger.warn("Ignoring attribute name " + oimPropertyName + " in configuration (" + configurationLocation +") since it is not in <>.<number>.<> format", exception);
                        continue;
                    }
                    if (configurationIndex > totalConnections) {
                        logger.trace("Resetting total number of connections to {}", configurationIndex);
                        totalConnections = configurationIndex;
                    }
                    if (oimConnectionConfigurationList.containsKey(configurationIndex)) {
                        logger.trace("Located an existing configuration map for {}", configurationIndex);
                        oimProperty = oimConnectionConfigurationList.get(configurationIndex);
                    } else {
                        logger.trace("Creating a new configuration map for {}", configurationIndex);
                        oimProperty = new Properties();
                        oimConnectionConfigurationList.put(configurationIndex, oimProperty);
                    }
                    String key = keyValues[2];
                    String value = configuration.getProperty(oimPropertyName);
                    //TODO: Remove this if password printing is an issue.
                    logger.trace("Setting {}={}", key, value);
                    oimProperty.setProperty(key, value);
                } else {
                    logger.warn("Ignoring attribute name {} in configuration({}) since it is not in <>.<>.<> format", new Object[]{oimPropertyName, configurationLocation});
                }
            } else {
                logger.debug("Ignoring {} key since it does not start with {} i.e. not an OIM property",
                        oimPropertyName, ATTR_NAME_PREFIX);
            }
        }
        logger.debug("Trying to create a list of OIM ConnectionTreeNode in specified order");
        for (int counter = 0; counter <= totalConnections; counter++) {
            logger.trace("Trying to validate if configuration corresponding to counter {} has been loaded", counter);
            if (oimConnectionConfigurationList.containsKey(counter)) {
                logger.trace("Located configuration, trying to validate if it contains {}", Connection.ATTR_CONN_NAME);
                if (oimConnectionConfigurationList.get(counter) != null &&
                        oimConnectionConfigurationList.get(counter).getProperty(Connection.ATTR_CONN_NAME) != null) {
                    oimConnectionNames.add(oimConnectionConfigurationList.get(counter).getProperty(Connection.ATTR_CONN_NAME));
                } else {
                    logger.warn("Located configuration at index {} in configuration {} that does not contain {}", new Object[]{counter, configurationLocation, Connection.ATTR_CONN_NAME});
                    oimConnectionNames.add(null);
                }
            } else {
                logger.trace("Could not locate any configuration for counter {}", counter);
                oimConnectionNames.add(null);
            }
        }
        logger.debug("Created the list of OIM Connection as {}", oimConnectionNames);
        logger.debug("Create configuration map using the {} attribute as key", Connection.ATTR_CONN_NAME);
        for (Properties oimProperty : oimConnectionConfigurationList.values()) {
            logger.trace("Validating if property contains key {}", Connection.ATTR_CONN_NAME);
            if (oimProperty.containsKey(Connection.ATTR_CONN_NAME)) {
                String connectionName = oimProperty.getProperty(Connection.ATTR_CONN_NAME);
                logger.trace("Identified OIM Connection Configuration {}, adding it to configuration map", connectionName);
                oimConnectionConfiguration.put(connectionName, oimProperty);
            } else {
                logger.warn("Ignoring configuration loaded from {} since it does not have any associated name (i.e. {} attribute). Configuration {}", new Object[]{configurationLocation,
                        Connection.ATTR_CONN_NAME, oimProperty});
            }
        }
        logger.debug("Processed the configuration information to extract OIM connection details");
    }

    private String setupWorkArea() {
        logger.debug("Trying to setup work area for application");
        String userWorkArea = null;
        try {
            String userHome = null;
            logger.debug("Checking if {} or {} System property has been set and if set whether it points to valid location", ATTR_NAME_WORK_AREA, VAL_WORK_AREA_BASE_PREFIX_DEFAULT);
            if (System.getProperties().containsKey(ATTR_NAME_WORK_AREA)
                    && (userHome = System.getProperty(ATTR_NAME_WORK_AREA)) != null
                    && (new File(userHome)).exists()) {
                userWorkArea = userHome + File.separator + VAL_WORK_AREA_BASE;
                logger.info("Setting application work area to {}", userWorkArea);
            } else if (System.getProperties().containsKey(VAL_WORK_AREA_BASE_PREFIX_DEFAULT)
                    && (userHome = System.getProperty(VAL_WORK_AREA_BASE_PREFIX_DEFAULT)) != null
                    && (new File(userHome)).exists()) {
                userWorkArea = userHome + File.separator + VAL_WORK_AREA_BASE;
                logger.info("Setting application work area to {}", userWorkArea);
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
            validateBaseArea(configWorkArea);
            String configurationFileLocation = userWorkArea + File.separator + VAL_WORK_AREA_CONF + File.separator + VAL_CONFIG_PROP_FILE_NAME;
            File configFile = new File(configurationFileLocation);
            logger.debug("Trying to validate if configuration file {} has been initialized", configurationFileLocation);
            if (!configFile.exists()) {
                logger.trace("Trying to find location that contains the  {} class", Config.class);
                String jarLocation = Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                File name = new File(jarLocation);
                logger.trace("Trying to check if location is directory {} (if class is not packaged as jar)", jarLocation);
                if (!name.isDirectory()) {
                    logger.trace("Since location {} is a file(jar), assuming that application is running in production mode. Trying to locate directory containing jar file.", jarLocation);
                    String jarParentDirectory = name.getParent();
                    logger.trace("Trying to check if parent directory {} of location {} is valid", jarParentDirectory, jarLocation);
                    if (jarParentDirectory != null) {
                        String configurationZipFileLocation = jarParentDirectory + File.separator + VAL_CONF_FILE_ZIP;
                        File configurationZipFile = new File(configurationZipFileLocation);
                        logger.trace("Trying to check if directory {} contains a readable zip file {} (contains bootstrap files)", jarParentDirectory, VAL_CONF_FILE_ZIP);
                        if (configurationZipFile.exists() && configurationZipFile.canRead()) {
                            logger.info("Initializing application work area {} with bootstrap files {}", configWorkArea, configurationZipFileLocation);
                            Utils.extractJarFile(configWorkArea, configurationZipFileLocation);
                        } else {
                            throw new FileNotFoundException("Failed to locate bootstrap file " + configurationZipFileLocation + " for setting up directory " + userWorkArea + " as work area");
                        }
                    } else {
                        throw new NullPointerException("Failed to locate the directory containing the jar file " + jarLocation);
                    }
                } else {
                    logger.info("Running in test mode, will not try to setup configuration area");
                }
            } else {
                logger.info("Application configuration {} is already initialized.", configurationFileLocation);
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to setup the work area for the application in directory "
                    + userWorkArea, exception);
        }
        logger.debug("Completed setup of work area {} for application", userWorkArea);
        return userWorkArea;
    }

    private void validateBaseArea(String userWorkArea) {
        logger.trace("Entering validateBaseArea({})", userWorkArea);
        File workAreaBaseDirectory = new File(userWorkArea);
        logger.trace("Trying to check if location exists");
        if (!workAreaBaseDirectory.exists()) {
            logger.trace("Trying to create the directory since it does not exist");
            workAreaBaseDirectory.mkdirs();
        }
        logger.trace("Trying to validate if given location is a directory");
        if (!workAreaBaseDirectory.isDirectory()) {
            throw new InvalidParameterException("User work area " + userWorkArea + " is not a directory");
        }
        logger.trace("Trying to validate if given location can be read");
        if (!workAreaBaseDirectory.canRead()) {
            throw new InvalidParameterException("User work area " + userWorkArea + " can not be read");
        }
        logger.trace("Trying to validate if given location can be written to");
        if (!workAreaBaseDirectory.canWrite()) {
            throw new InvalidParameterException("User work area " + userWorkArea + " can not be written into.");
        }
        logger.trace("Leaving validateBaseArea()");
    }

    public String getWorkArea() {
        return workArea;
    }

    public void saveConfiguration(Configuration configuration) {
        logger.debug("Trying to save configuration {}", configuration);
        Properties configurationFile = new Properties();
        boolean newConfigurationSaved = false;
        int counter;
        String configurationBeingSaved = configuration.getProperty(Connection.ATTR_CONN_NAME);
        logger.debug("Trying to save configuration {}", configurationBeingSaved);
        for (counter = 0; counter < this.oimConnectionNames.size(); counter++) {
            logger.trace("Processing configuration number {}", counter);
            String name = oimConnectionNames.get(counter);
            if (name == null || name.isEmpty()) {
                logger.trace("Skipping configuration at {} since it has null or empty name", counter);
                continue;
            }
            Properties configurationDetail = null;
            logger.trace("Validating of the configuration being processed {} = configuration being saved {}", name, configurationBeingSaved);
            if (name.equals(configurationBeingSaved)) {
                logger.trace("Selecting new configuration and tracking that given configuration has been added");
                configurationDetail = ((configuration instanceof EditableConfiguration)?((EditableConfiguration)configuration).editableConfiguration:configuration.configuration);
                newConfigurationSaved = true;
            } else {
                logger.trace("Using existing configuration");
                configurationDetail = oimConnectionConfiguration.get(name);
            }
            logger.trace("Validating whether we identified applicable configuration");
            if (configurationDetail == null)
                throw new NullPointerException("Failed to locate configuration detail with name " + name);
            logger.trace("Adding selected configuration {} in to new configuration file being generated", configurationDetail);
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
                logger.debug("Trying to save configuration to {}", configurationLocation);
                configurationFile.store(new FileWriter(configurationFileObject), "Saved on " + new Date());
            } catch (Exception exception) {
                throw new OIMAdminException("Error occurred while saving updated configuration", exception);
            }
        } else {
            throw new OIMAdminException("Error occurred while saving updated configuration", new IOException("Configuration file " + configurationLocation + " either does not exist or is not writable. Can not save the configuration."));
        }
        logger.debug("Saved configuration {}", configuration);
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
            return getProperty(propertyName, null);
        }

        public String getProperty(String propertyName, String defaultValue) {
            return configuration.getProperty(propertyName, defaultValue);
        }

        public Config getConfig() {
            return config;
        }

    }

    public static class EditableConfiguration extends Configuration {

        private final Properties editableConfiguration;

        public EditableConfiguration(Configuration configuration) {
            super(configuration.configuration ,configuration.config);
            editableConfiguration =new Properties(configuration.configuration);
        }

        @Override
        public String getProperty(String propertyName, String defaultValue) {
            return editableConfiguration.getProperty(propertyName, defaultValue);
        }

        public void setProperty(String propertyName, String value) {
            editableConfiguration.setProperty(propertyName, value);
        }
    }
}
