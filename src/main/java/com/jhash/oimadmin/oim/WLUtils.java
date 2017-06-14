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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.HashSet;
import java.util.Set;

public class WLUtils {

    private static final Logger logger = LoggerFactory.getLogger(WLUtils.class);
    private static final String ATTR_WL_JMX_WLSERVERS = "com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean";

    public static Set<String> getRuntimeServers(MBeanServerConnection connection) {
        logger.debug("Trying to get name of servers running in the JMX server domain");
        Set<String> runtimeServerNames = new HashSet<>();
        logger.debug("Trying to get MBeanServerConnection connection");
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
