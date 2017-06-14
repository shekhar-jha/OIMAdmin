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
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.oim.eventHandlers.OperationDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectInstance;
import javax.management.openmbean.CompositeData;
import java.util.*;

public class OIMJMXWrapper extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(OIMJMXWrapper.class);
    protected String STRING_REPRESENTATION = "OIMJMXWrapper";
    private JMXConnection jmxConnection = null;
    private Config.OIM_VERSION connectionOIMVersion = Config.OIM_VERSION.NOT_AVAILABLE;

    @Override
    protected void initializeConnection(Configuration config) {
        logger.debug("Trying to initialize OIM JMX Wrapper Connection using configuration {}", config);
        JMXConnection tmpConnection = new JMXConnection();
        logger.debug("Trying to initialize JMX Connection.");
        tmpConnection.initialize(config);
        logger.debug("Trying to get JMX Server Connection.");
        jmxConnection = tmpConnection;
        //connectionOIMVersion = getVersion();
        STRING_REPRESENTATION += "(" + jmxConnection + ")";
    }





    protected void destroyConnection() {
        logger.debug("Trying to destroy OIM JMX Connection");
        if (this.jmxConnection != null) {
            logger.debug("Trying to destroy JMX connection {}", jmxConnection);
            jmxConnection.destroy();
            logger.debug("Destroyed JMX connection.");
            jmxConnection = null;
        }
        logger.debug("Destroyed OIM JMX Connection");
    }


}
