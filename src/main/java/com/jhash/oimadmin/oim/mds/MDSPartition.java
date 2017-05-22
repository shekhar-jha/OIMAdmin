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

package com.jhash.oimadmin.oim.mds;

import com.jhash.oimadmin.OIMAdminException;
import oracle.mds.lcm.client.MDSAppInfo;
import oracle.mds.lcm.client.TargetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDSPartition {

    private static final Logger logger = LoggerFactory.getLogger(MDSPartition.class);
    public final String serverName;
    public final MDSAppInfo application;
    private final MDSConnectionJMX mdsConnectionJMX;
    private final String stringValue;

    public MDSPartition(MDSConnectionJMX mdsConnectionJMX, String serverName, MDSAppInfo application) {
        this.mdsConnectionJMX = mdsConnectionJMX;
        this.serverName = serverName;
        this.application = application;
        stringValue = application.getName() + "(" + serverName + ")";
    }

    public String getPartitionName() {
        return stringValue;
    }

    public String toString() {
        return stringValue;
    }

    public String getPartitionFiles() {
        logger.debug("Trying to get files present in MDS Partition {}", this);
        String fileName = mdsConnectionJMX.generateFileName(true);
        logger.debug("Generated the dump file as {}", fileName);
        try {
            mdsConnectionJMX.exportMetaData(application, new TargetInfo(serverName), fileName);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to export MDS repository " + application + " from location "
                    + serverName + " into file " + fileName, exception);
        }
        return fileName;
    }


    public void destroy() {
        logger.debug("Destroyed {}", this);
    }
}
