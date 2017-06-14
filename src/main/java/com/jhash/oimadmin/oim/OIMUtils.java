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

import com.jhash.oimadmin.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OIMUtils {

    public static final JMXConnection.OIM_JMX_BEANS OIM_VERSION_INFO_MBEAN_NAME = new JMXConnection.OIM_JMX_BEANS("oim#11.1.2.0.0", "EMIntegration");
    private static final Logger logger = LoggerFactory.getLogger(OIMUtils.class);

    public static Config.OIM_VERSION getVersion(JMXConnection jmxConnection) {
        Config.OIM_VERSION connectionOIMVersion;
        String versionValue = jmxConnection.getValue(OIM_VERSION_INFO_MBEAN_NAME, "Version");
        logger.info("Read OIM Version as {} for connection {}", versionValue, jmxConnection);
        if (versionValue.startsWith("11.1.2.3"))
            connectionOIMVersion = Config.OIM_VERSION.OIM11GR2PS3;
        else if (versionValue.startsWith("11.1.2.2"))
            connectionOIMVersion = Config.OIM_VERSION.OIM11GR2PS2;
        else
            connectionOIMVersion = Config.OIM_VERSION.UNKNOWN;
        return connectionOIMVersion;
    }

}
