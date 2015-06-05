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
import com.jhash.oimadmin.Utils;
import com.thortech.xl.systemverification.api.DDKernelService;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Role;
import oracle.iam.platformservice.api.PlatformService;
import oracle.iam.platformservice.api.PlatformUtilsService;
import oracle.iam.request.api.RequestService;
import oracle.iam.request.vo.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

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

    private static final Logger logger = LoggerFactory.getLogger(OIMConnection.class);
    // TODO: Not thread safe
    private boolean isLogin = false;

    private OIMClient oimClient = null;

    public OIMConnection() {
        STRING_REPRESENTATION = "OIMConnection:";
    }

    public void initializeConnection(Configuration config) {
        logger.debug("Trying to initialize OIM using {}", config);
        initializeForAppServer(config);
        oimClient = initializeOIMClient(config);
        logger.debug("Initialized OIMCLI");
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

    private OIMClient initializeOIMClient(Configuration config) {
        PLATFORM platform = PLATFORM.fromString(config.getProperty(Connection.ATTR_CONN_PLATFORM));
        switch (platform) {
            case WEBLOGIC:
                Hashtable<String, String> env = new Hashtable<String, String>();
                env.put("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
                env.put(OIMClient.WLS_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
                env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, config.getProperty(ATTR_OIM_URL));
                STRING_REPRESENTATION += config.getProperty(ATTR_OIM_URL);
                logger.debug("Trying to create OIMClient with configuration {}", env);
                return new OIMClient(env);
            default:
                throw new UnsupportedOperationException("Application does not support any other platform except weblogic");
        }
    }

    public boolean login() {
        return login(oimClient, config);
    }

    private boolean login(OIMClient oimClient, Configuration config) {
        return login(oimClient, config.getProperty(ATTR_OIM_USER), config.getProperty(ATTR_OIM_PWD).toCharArray());
    }

    private boolean login(OIMClient oimClient, String userName, char[] password) {
        boolean success = false;
        try {
            logger.debug("Trying to login user {}", userName);
            List<Role> roles = oimClient.login(userName, password);
            logger.debug("Successfully performed login. Roles {}", roles);
            success = true;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to perform login for user " + userName, exception);
        }
        if (success)
            isLogin = true;
        return success;
    }

    public void registerPlugin(byte[] data) {
        if (data == null)
            throw new NullPointerException("The plugin that needs to be registered was provided as null");
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        PlatformService platformService = oimClient.getService(PlatformService.class);
        try {
            logger.debug("Trying to register plugin of size {} ", data.length);
            platformService.registerPlugin(data);
            logger.debug("Registered plugin");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to register plugin", exception);
        }
    }

    public void unregisterPlugin(String name) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        PlatformService platformService = oimClient.getService(PlatformService.class);
        try {
            logger.debug("Trying to unregister plugin {}", name);
            platformService.unRegisterPlugin(name);
            logger.debug("Unregistered plugin {}", name);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to unregister plugin " + name, exception);
        }
    }

    public void purgeCache(String cacheName) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        PlatformUtilsService platformService = oimClient.getService(PlatformUtilsService.class);
        if (Utils.isEmpty(cacheName))
            cacheName = "All";
        try {
            logger.debug("Trying to purge cache {}", cacheName);
            platformService.purgeCache(cacheName);
            logger.debug("Purged cache");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to purge cache " + cacheName, exception);
        }
    }

    public Request getRequestDetails(String requestId) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            RequestService requestService = oimClient.getService(RequestService.class);
            Request request = requestService.getBasicRequestData(requestId);
            return request;
        } catch (Exception exception) {
            throw new OIMAdminException("Error occurred while retrieving request details for request id " + requestId, exception);
        }
    }

    public <T> T executeOrchestrationOperation(String method, Class[] parameterTypes, Object[] parameters) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            DDKernelService kernelService = oimClient.getService(DDKernelService.class);
            logger.trace("Trying to invoke method {} with parameters {} on DDKernelService {}", new Object[]{method, parameters, kernelService});
            Object result = kernelService.invoke(method, parameters, parameterTypes);
            logger.trace("Returned result {}", result);
            return (T) result;
        } catch (Exception exception) {
            throw new OIMAdminException("Error occurred while invoking method " + method + " on DDKernelService with parameters " + Arrays.toString(parameters), exception);
        }
    }

    public void getTaskDetails() {

    }

    public void logout() {
        logout(oimClient);
    }

    private void logout(OIMClient oimClient) {
        isLogin = false;
        if (oimClient != null) {
            try {
                logger.debug("Trying to perform logout");
                oimClient.logout();
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

}
