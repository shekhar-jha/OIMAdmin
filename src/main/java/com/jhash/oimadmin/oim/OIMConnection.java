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
import com.jhash.oimadmin.oim.orch.PublicProcessImpl;
import com.jhash.oimadmin.oim.request.Request;
import oracle.jrf.JrfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Blob;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class OIMConnection extends AbstractConnection {

    public static final String ATTR_OIM_HOME = "oim_home";
    public static final String ATTR_OIM_URL = "oim_url";
    public static final String ATTR_OIM_USER = "oim_user";
    public static final String ATTR_OIM_PWD = "oim_pwd";
    public static final String ATTR_OIM_LOGIN_CONFIG = "java.security.auth.login.config";
    public static final String ATTR_OIM_APP_SERVER_TYPE = "APPSERVER_TYPE";
    public static final String ATTR_OIM_APP_SERVER_TYPE_WLS = "wls";
    public static final String ATTR_EXPORT_DIR = "oim.export.dir";
    public static final String VAL_DEFAULT_OIM_HOME = System.getProperty("user.home") + "/.oimadm/";
    public static final String ATTR_OIM_VERSION = "oim_version";
    private static final Logger logger = LoggerFactory.getLogger(OIMConnection.class);
    private static Map<Config.OIM_VERSION, ClassLoader> oimClientClassLoaders = new HashMap<>();

    // TODO: Not thread safe
    private boolean isLogin = false;

    private Object oimClient = null;
    private Config.OIM_VERSION version = null;
    private String loginUser = null;

    public OIMConnection() {
        STRING_REPRESENTATION = "OIMConnection:";
    }

    public static ClassLoader getOIMClass(Configuration config, Config.OIM_VERSION version) {
        try {
            if (!oimClientClassLoaders.containsKey(version)) {
                synchronized (oimClientClassLoaders) {
                    logger.debug("Locating Base Directory for OIM Classes for version {}", version);
                    File oimBaseDirectory = null;
                    File oimClientJar = new File(JrfUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                    logger.debug("Located Path of Jar containing OIMClient as {}", oimClientJar);
                    if (oimClientJar == null) {
                        throw new NullPointerException("Failed to locate the path of OIMClient jar for version " + version);
                    }
                    oimBaseDirectory = oimClientJar.getParentFile();
                    logger.debug("Located Base Directory as {}", oimBaseDirectory);
                    if (oimBaseDirectory == null) {
                        throw new NullPointerException("Failed to locate the base directory for libraries of OIM version " + version);
                    }
                    List<URL> jarLocations = new ArrayList<>();
                    int index = 1;
                    int missedIndex = 0;
                    do {
                        String jarLocationKey = "sysadmin.classpath.version." + version + ".url." + index++;
                        String jarLocationValue = config.getProperty(jarLocationKey);
                        if (!Utils.isEmpty(jarLocationValue)) {
                            File jarLocationFile = new File(oimBaseDirectory, jarLocationValue);
                            if (jarLocationFile.exists() && jarLocationFile.isFile() && jarLocationFile.canRead()) {
                                logger.debug("Located Jar {} at {}", jarLocationValue, jarLocationFile);
                                jarLocations.add(jarLocationFile.toURI().toURL());
                            } else {
                                logger.warn("Failed to locate Jar {} at {}", jarLocationValue, jarLocationFile);
                            }
                        } else {
                            missedIndex++;
                        }
                    } while (missedIndex < 10);
                    if (!jarLocations.isEmpty()) {
                        logger.debug("System Classpath : {}", System.getProperty("java.class.path"));
                        ClassLoader classLoader = new URLClassLoader(jarLocations.toArray(new URL[]{}));
                        logger.info("Using CUSTOM class loader {} for version {}", classLoader, version);
                        oimClientClassLoaders.put(version, classLoader);
                    } else {
                        ClassLoader classLoader = JrfUtils.class.getClassLoader();
                        logger.info("Using DEFAULT class loader {} for version {}", classLoader, version);
                        oimClientClassLoaders.put(version, classLoader);
                    }
                }
            }
            return oimClientClassLoaders.get(version);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to load OIM Client Class", exception);
        }
    }

    public static List<File> getClassPath(Configuration config, Config.OIM_VERSION oimVersion) {
        List<File> classPath = new ArrayList<>();
        ClassLoader classLoader = getOIMClass(config, oimVersion);
        if (classLoader != null) {
            if (classLoader instanceof URLClassLoader) {
                URL[] jarLocations = ((URLClassLoader) classLoader).getURLs();
                if (jarLocations != null && jarLocations.length > 0) {
                    for (URL jarLocation : jarLocations) {
                        try {
                            File jarFile = new File(jarLocation.toURI());
                            classPath.add(jarFile);
                        } catch (URISyntaxException exception) {
                            logger.warn("Failed to create URI from URL " + jarLocation + " from class loader " + classLoader, exception);
                        }
                    }
                }
            }
        }
        return classPath;
    }

    public void initializeConnection(Configuration config) {
        logger.debug("Trying to initialize OIM using {}", config);
        initializeForAppServer(config);
        oimClient = initializeOIMClient(config);
        logger.debug("Initialized OIM");
    }

    private void initializeForAppServer(Configuration config) {
        PLATFORM platform = PLATFORM.fromString(config.getProperty(Connection.ATTR_CONN_PLATFORM));
        logger.debug("Trying to initialize for application server {}", platform);
        switch (platform) {
            case WEBLOGIC:
                String xlHome = config.getWorkArea();
                if (config.getProperty(ATTR_OIM_HOME) != null) {
                    xlHome = config.getProperty(ATTR_OIM_HOME, VAL_DEFAULT_OIM_HOME);
                } else if (
                        System.getProperties().contains("XL.HomeDir")) {
                    xlHome = System.getProperty("XL.HomeDir");
                }
                if (xlHome == null || xlHome.isEmpty())
                    throw new NullPointerException("Failed to locate the home directory that will contain /conf/authwl.conf.");
                String authWlFileLocation = xlHome + "/conf/authwl.conf";
                File authWlFile = new File(authWlFileLocation);
                if (!authWlFile.exists() || !authWlFile.canRead())
                    throw new NullPointerException("Failed to locate readable version of " + authWlFileLocation + ". Please validate");
                System.setProperty(ATTR_OIM_LOGIN_CONFIG, authWlFileLocation);
                logger.debug("Set property {}={}", ATTR_OIM_LOGIN_CONFIG, System.getProperty(ATTR_OIM_LOGIN_CONFIG));
                // Ensure that jndi.properties has been updated with latest weblogic
                // information. Not documented.
                System.setProperty(ATTR_OIM_APP_SERVER_TYPE, ATTR_OIM_APP_SERVER_TYPE_WLS);
                logger.debug("Set property {}={}", ATTR_OIM_APP_SERVER_TYPE, System.getProperty(ATTR_OIM_APP_SERVER_TYPE));
                // Undocumented.
                System.setProperty("weblogic.Name", "someName");
                logger.debug("Set property weblogic.Name={}", ATTR_OIM_APP_SERVER_TYPE, System.getProperty("weblogic.Name"));
                break;
            default:
                throw new UnsupportedOperationException("Application does not support any other platform except weblogic");
        }
    }

    private Object initializeOIMClient(Configuration config) {
        PLATFORM platform = PLATFORM.fromString(config.getProperty(Connection.ATTR_CONN_PLATFORM));
        Config.OIM_VERSION oimVersion = Config.OIM_VERSION.fromString(config.getProperty(ATTR_OIM_VERSION));
        try {
            Class<?> oimClient = getOIMClass(config, oimVersion).loadClass("oracle.iam.platform.OIMClient");
            logger.debug("Located OIM Client Class with classloader {} from {}", oimClient.getClassLoader(), oimClient.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            Constructor<?> oimClientConstructor = oimClient.getConstructor(Hashtable.class);
            switch (platform) {
                case WEBLOGIC:
                    Hashtable<String, String> env = new Hashtable<String, String>();
                    env.put("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
                    env.put("weblogic.jndi.WLInitialContextFactory", "weblogic.jndi.WLInitialContextFactory");
                    env.put("java.naming.provider.url", config.getProperty(ATTR_OIM_URL));
                    STRING_REPRESENTATION += config.getProperty(ATTR_OIM_URL);
                    logger.debug("Trying to create OIMClient with configuration {}", env);
                    version = oimVersion;
                    return oimClientConstructor.newInstance(env);
                default:
                    throw new UnsupportedOperationException("Application does not support any other platform except weblogic");
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to create OIM Client", exception);
        }
    }

    public ClassLoader getClassLoader() {
        return getOIMClass(config, version);
    }

    public boolean login() {
        return login(oimClient, config);
    }

    private boolean login(Object oimClient, Configuration config) {
        return login(oimClient, config.getProperty(ATTR_OIM_USER), config.getProperty(ATTR_OIM_PWD).toCharArray());
    }

    private boolean login(Object oimClient, String userName, char[] password) {
        boolean success = false;
        try {
            logger.debug("Trying to login user {}", userName);
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                Object roles = oimClient.getClass().getMethod("login", String.class, char[].class).invoke(oimClient, userName, password);
                logger.debug("Successfully performed login. Roles {}", roles);
                loginUser = userName;
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
            success = true;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to perform login for user " + userName, exception);
        }
        if (success)
            isLogin = true;
        return success;
    }

    public Class<?> getClass(String className) {
        if (className == null)
            return null;
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        ClassLoader oimClassLoader = getClassLoader();
        logger.debug("OIM Class Loader located {}", oimClassLoader);
        try {
            return oimClassLoader.loadClass(className);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to load class " + className + " using classLoader " + oimClassLoader, exception);
        }
    }

    private Object getService(String serviceClass) {
        try {
            Class<?> oimServiceClass = getClass(serviceClass);
            logger.debug("Loaded service class {} with classloader {} from {}", new Object[]{oimServiceClass, oimServiceClass.getClassLoader(), oimServiceClass.getProtectionDomain().getCodeSource().getLocation()});
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                Object service = oimClient.getClass().getMethod("getService", Class.class).invoke(oimClient, oimServiceClass);
                if (service != null) {
                    logger.debug("Loaded service as {} with class loader {}", service, service.getClass().getClassLoader());
                }
                return service;
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to load OIM service " + serviceClass, exception);
        }
    }

    public void registerPlugin(byte[] data) {
        if (data == null)
            throw new NullPointerException("The plugin that needs to be registered was provided as null");
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        Object platformService = getService("oracle.iam.platformservice.api.PlatformService");
        try {
            logger.debug("Trying to register plugin of size {} ", data.length);
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                platformService.getClass().getMethod("registerPlugin", byte[].class).invoke(platformService, data);
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
            logger.debug("Registered plugin");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to register plugin", exception);
        }
    }

    public void unregisterPlugin(String name) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        Object platformService = getService("oracle.iam.platformservice.api.PlatformService");
        try {
            logger.debug("Trying to unregister plugin {}", name);
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                platformService.getClass().getMethod("unRegisterPlugin", String.class).invoke(platformService, name);
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
            logger.debug("Unregistered plugin {}", name);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to unregister plugin " + name, exception);
        }
    }

    public void purgeCache(String cacheName) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        Object platformService = getService("oracle.iam.platformservice.api.PlatformUtilsService");
        if (Utils.isEmpty(cacheName))
            cacheName = "All";
        try {
            logger.debug("Trying to purge cache {}", cacheName);
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                platformService.getClass().getMethod("purgeCache", String.class).invoke(platformService, cacheName);
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
            logger.debug("Purged cache");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to purge cache " + cacheName, exception);
        }
    }

    public Request getRequestDetails(String requestId) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            Object requestService = getService("oracle.iam.request.api.RequestService");
            logger.debug("Loaded Request Service {}", requestService);
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                getClass("oracle.iam.request.vo.Request");
                return new Request(requestService.getClass().getMethod("getBasicRequestData", String.class).invoke(requestService, requestId), getClassLoader());
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Error occurred while retrieving request details for request id " + requestId, exception);
        }
    }

    public <T> T executeOrchestrationOperation(String method, Class[] parameterTypes, Object[] parameters) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            Object kernelService = getService("com.thortech.xl.systemverification.api.DDKernelService");
            logger.trace("Trying to invoke method {} with parameters {} on DDKernelService {}", new Object[]{method, parameters, kernelService});
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                Object result = kernelService.getClass().getMethod("invoke", String.class, Object[].class, Class[].class).invoke(kernelService, method, parameters, parameterTypes);
                logger.trace("Returned result {}", result);
                return (T) result;
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Error occurred while invoking method " + method + " on DDKernelService with parameters " + Arrays.toString(parameters), exception);
        }
    }

    public PublicProcessImpl getOrchestration(Blob orchestrationObject) {
        ObjectInputStream ins = null;
        try {
            Object readObject = Utils.getObjectInputStream(new GZIPInputStream(orchestrationObject.getBinaryStream()), getClassLoader()).readObject();
            return new PublicProcessImpl(readObject, getClassLoader());
        } catch (Exception e) {
            throw new OIMAdminException("Failed to read process object from Blob " + orchestrationObject, e);
        } finally {
            if (null != ins) {
                try {
                    ins.close();
                } catch (IOException e) {
                    logger.debug("Failed to close Object input stream " + ins, e);
                }
            }
        }

    }

    public void unregisterJar(String type, String jarName) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            Object platformUtilsService = getService("oracle.iam.platformservice.api.PlatformUtilsService");
            logger.debug("Loaded PlatformUtilsService Service {}", platformUtilsService);
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                Class jarElementClass = getClass("oracle.iam.platformservice.vo.JarElement");
                Object jarElement = jarElementClass.newInstance();
                Utils.getMethod(jarElementClass, "setName", String.class).invoke(jarElement, jarName);
                Utils.getMethod(jarElementClass, "setType", String.class).invoke(jarElement, type);
                Set jarElementSet = new HashSet();
                jarElementSet.add(jarElement);
                Utils.getMethod(platformUtilsService.getClass(), "deleteJars", Set.class).invoke(platformUtilsService, jarElementSet);
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to unregister jar " + jarName + " of type " + type, exception);
        }

    }

    public long getLoginUserIdentifier() {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            Object userManagerService = getService("oracle.iam.identity.usermgmt.api.UserManager");
            logger.debug("Loaded UserManager Service {}", userManagerService);
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                Object userObject = Utils.getMethod(userManagerService.getClass(), "getDetails", String.class, Set.class, boolean.class).invoke(userManagerService, loginUser, null, true);
                if (userObject != null) {
                    Object idValue = Utils.invoke(userObject, "getId");
                    if (idValue instanceof String) {
                        return Long.parseLong((String) idValue);
                    } else {
                        logger.warn("Did not locate any ID for authenticated user from user object " + userObject);
                    }
                } else {
                    logger.warn("No authenticated User detail was returned.");
                }
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to retrieve User ID for Login user", exception);
        }
        return -1L;
    }

    public void logout() {
        logout(oimClient);
    }

    private void logout(Object oimClient) {
        isLogin = false;
        if (oimClient != null) {
            try {
                logger.debug("Trying to perform logout");
                ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(getClassLoader());
                    oimClient.getClass().getMethod("logout").invoke(oimClient);
                } finally {
                    Thread.currentThread().setContextClassLoader(currentClassLoader);
                }
                logger.debug("Successfully performed logout");
            } catch (Exception exception) {
                throw new OIMAdminException("Error occurred while performing logout on client " + oimClient
                        + ". Ignoring", exception);
            }
        } else {
            logger.warn("OIM Client not available. Ignoring logout call");
        }
    }

    @Override
    public void destroyConnection() {
        logger.debug("Trying to destroy connection {}", this);
        if (oimClient != null) {
            if (isLogin)
                logout();
            oimClient = null;
        }
        logger.debug("Destroyed connection {}", this);
    }

    public static class OrchestrationFailedEventResponse {

        public OrchestrationFailedEventResponse[] values() {
            return null;
        }
    }

}
