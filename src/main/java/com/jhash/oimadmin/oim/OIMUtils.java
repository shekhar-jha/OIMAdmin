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
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.TabularData;
import java.util.Map;

public class OIMUtils {

    public static final JMXConnection.OIM_JMX_BEANS OIM_VERSION_INFO_MBEAN_NAME = new JMXConnection.OIM_JMX_BEANS("OIM", "EMIntegration");
    private static final Logger logger = LoggerFactory.getLogger(OIMUtils.class);

    public static Config.OIM_VERSION getVersion(String version) {
        Config.OIM_VERSION connectionOIMVersion = Config.OIM_VERSION.UNKNOWN;
        if (!Utils.isEmpty(version)) {
            if (version.startsWith("11.1.2.3"))
                connectionOIMVersion = Config.OIM_VERSION.OIM11GR2PS3;
            else if (version.startsWith("11.1.2.2"))
                connectionOIMVersion = Config.OIM_VERSION.OIM11GR2PS2;
        }
        return connectionOIMVersion;
    }

    public static OIMServerDetails getOIMServerDetails(JMXConnection jmxConnection) {
        Object oimDetailsObject = jmxConnection.getValue(OIM_VERSION_INFO_MBEAN_NAME, "EMInstanceProperties");
        if (oimDetailsObject instanceof TabularData) {
            Map<String, Object> oimServerDetails = JMXUtils.extractData((TabularData) oimDetailsObject);
            return new OIMServerDetails(oimServerDetails);
        } else {
            throw new OIMAdminException("Expected a tabular data but received " + oimDetailsObject + " while retrieving 'EMInstanceProperties' from " + OIM_VERSION_INFO_MBEAN_NAME);
        }
    }


    public static class OIMServerDetails {

        public final String Version;
        public final String OracleHome;
        public final String[] Servers;
        public final Map<String, Object> oimServerDetails;

        public OIMServerDetails(Map<String, Object> oimServerDetails) {
            this.Version = (String) oimServerDetails.get("version");
            this.OracleHome = (String) oimServerDetails.get("OracleHome");
            this.Servers = ((String) oimServerDetails.get("ServerNames")).split(",");
            this.oimServerDetails = oimServerDetails;
        }

    }
}
