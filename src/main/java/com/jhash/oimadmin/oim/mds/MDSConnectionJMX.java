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

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.Config.Configuration;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.oim.AbstractConnection;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.WLUtils;
import oracle.mds.lcm.client.*;
import oracle.mds.lcm.client.MetadataTransferManager.ASPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MDSConnectionJMX extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(MDSConnectionJMX.class);
    private final JMXConnection jmxConnection;
    protected String STRING_REPRESENTATION = "MDSConnectionJMX:";
    private ASPlatform platform = null;

    public MDSConnectionJMX(JMXConnection jmxConnection) {
        this.jmxConnection = jmxConnection;
    }

    @Override
    protected void initializeConnection(Configuration config) {
        logger.debug("Trying to initialize MDS Connection using configuration {}", config);
        switch (Config.PLATFORM.fromString(config.getProperty(ATTR_CONN_PLATFORM))) {
            case WEBLOGIC:
                platform = ASPlatform.WEBLOGIC;
                break;
            case WEBSPHERE:
                platform = ASPlatform.WEBSPHERE;
                break;
            case JBOSS:
                platform = ASPlatform.JBOSS;
                break;
            case UNKNOWN:
            default:
                throw new UnsupportedOperationException("Platform is not supported.");
        }
        logger.debug("Platform was identified as {}", platform);
        STRING_REPRESENTATION += "(" + jmxConnection + ")";
    }

    public Set<MDSPartition> getMDSPartitions() {
        logger.debug("Trying to get all MDS Partitions...");
        Set<MDSPartition> partitions = new HashSet<MDSPartition>();
        logger.debug("Trying to get MBeanServerConnection");
        MBeanServerConnection connection = jmxConnection.getConnection();
        logger.debug("Trying to get all the servers running at the moment from {}", jmxConnection);
        Set<String> runtimeServers = WLUtils.getRuntimeServers(connection);
        logger.debug("Trying to process runtimeServers {}", runtimeServers);
        for (String runtimeServer : runtimeServers) {
            logger.debug("Trying to create new MetadataTransferManager with MBeanServerConnection {} and platform {}",
                    connection, platform);
            MetadataTransferManager mdsTransfer = new MetadataTransferManager(connection, platform);
            try {
                logger.debug("Trying to list names of MDS Applications partitions deployed on {}", runtimeServer);
                List<MDSAppInfo> applicationsInfo = mdsTransfer.listMDSAppNames(new TargetInfo(runtimeServer));
                logger.debug("Trying to process applications {}", applicationsInfo);
                for (MDSAppInfo applicationInfo : applicationsInfo) {
                    MDSPartition newPartitionIdentified = new MDSPartition(this, runtimeServer, applicationInfo);
                    partitions.add(newPartitionIdentified);
                }
            } catch (Exception exception) {
                logger.warn("Failed to list MDS Application Names from server " + runtimeServer, exception);
            }
            logger.debug("Created MetadataTransferManager {}", mdsTransfer);

        }
        logger.debug("Returning MDS Partitions {}", partitions);
        return partitions;
    }

    protected void importMetaData(MDSAppInfo application, TargetInfo target, String importFileLocation) throws Exception {
        logger.debug("Trying to import Meta data to MDS application {} at location {} from file {}", new Object[]{
                application, target, importFileLocation});
        if (!isConnected)
            throw new IllegalStateException("MDS Connection has not been initialized yet. Please initialize");
        logger.debug("Trying to get MBeanServerConnection from JMX Connector {}", jmxConnection);
        MBeanServerConnection serverConnection = jmxConnection.getConnection();
        logger.debug("Trying to create new MetadataTransferManager with MBeanServerConnection {} and platform {}",
                serverConnection, platform);
        MetadataTransferManager mdsTransfer = new MetadataTransferManager(serverConnection, platform);
        logger.debug("Created MetadataTransferManager {}", mdsTransfer);
        TransferParameters params = new TransferParameters();
        ProgressObject progress = mdsTransfer.importMetadata(application, target, importFileLocation, params);
        for (; (!progress.isCompleted()) && (!progress.isFailed()); Thread.sleep(100L)) {
            logger.trace("Importing metadata progress {}", progress.getStatus().getMessage());
            Thread.currentThread();
        }
        logger.debug("importMetadata result: {}", progress.getStatus().getMessage());
        logger.debug("  Summary info: {}", progress.getSummaryInfo());
        String docs = progress.getSuccessAttemptedDocumentsList();
        if (docs != null) {
            logger.trace("  Documents successfully processed: {}", docs);
        }
        docs = progress.getFailedDocumentsList();
        if (docs != null) {
            logger.warn("Failed to import the docs into application {} at location {} from {}. Docs {}", new Object[]{
                    application, target, importFileLocation, docs});
        }
        Exception ex = progress.getException();
        if (ex != null) {
            throw new OIMAdminException("Failed to import application " + application + " to location " + target
                    + " from " + importFileLocation, ex);
        }
        logger.debug("Completed import of Meta data from MDS repository");
    }

    protected void exportMetaData(MDSAppInfo application, TargetInfo target, String exportFileLocation) throws Exception {
        logger.debug("Trying to export Meta data from MDS application {} from location {} to file {}", new Object[]{
                application, target, exportFileLocation});
        if (!isConnected)
            throw new IllegalStateException("MDS Connection has not been initialized yet. Please initialize");
        logger.debug("Trying to get MBeanServerConnection from JMX Connector {}", jmxConnection);
        MBeanServerConnection serverConnection = jmxConnection.getConnection();
        logger.debug("Trying to create new MetadataTransferManager with MBeanServerConnection {} and platform {}",
                serverConnection, platform);
        MetadataTransferManager mdsTransfer = new MetadataTransferManager(serverConnection, platform);
        logger.debug("Created MetadataTransferManager {}", mdsTransfer);
        TransferParameters params = new TransferParameters();
        ProgressObject progress = mdsTransfer.exportMetadata(application, target, exportFileLocation, params);
        for (; (!progress.isCompleted()) && (!progress.isFailed()); Thread.sleep(100L)) {
            logger.trace("Exporting Meta data progress status {}", progress.getStatus().getMessage());
        }
        logger.debug("exportMetadata result: {}", progress.getStatus().getMessage());
        logger.debug("  Summary info: {}", progress.getSummaryInfo());
        String docs = progress.getSuccessAttemptedDocumentsList();
        if (docs != null) {
            logger.trace("  Documents successfully processed: {}", docs);
        }
        docs = progress.getFailedDocumentsList();
        if (docs != null) {
            logger.warn("Failed to export the docs while exporting application {} from location {} to {}. Docs {}",
                    new Object[]{application, target, exportFileLocation, docs});
        }
        Exception ex = progress.getException();
        if (ex != null) {
            throw new OIMAdminException("Failed to export application " + application + " from location " + target
                    + " to " + exportFileLocation, ex);
        }
        logger.debug("Completed export of Meta data from MDS repository");
    }

    public static JMXConnection.OIM_JMX_BEANS MDS_RUNTIME = new JMXConnection.OIM_JMX_BEANS(null, "MDSAppRuntime");
    public static JMXConnection.JMX_BEAN_METHOD MDS_DELETE = new JMXConnection.JMX_BEAN_METHOD(MDS_RUNTIME, "deleteMetadata", new String[]{
            String[].class.getName(), String[].class.getName(), boolean.class.getName(), boolean.class.getName(), boolean.class.getName(), boolean.class.getName()});

    protected void delete(final MDSAppInfo application, final String server, final String... mdsFiles) {
        logger.debug("Deleting files {} from MDS Application {} on server {}", new Object[]{mdsFiles, application, server});
        if (application == null || server == null || mdsFiles == null || mdsFiles.length == 0)
            return;
        jmxConnection.invoke(MDS_RUNTIME, new JMXConnection.ProcessBeanType() {
            @Override
            public void execute(JMXConnection.ProcessingBean bean) {
                if (bean == null) return;
                logger.trace("Matching bean {} to desired bean", bean.getProperties());
                if (bean.getProperties().containsKey("Location") && bean.getProperties().containsKey("Application") && bean.getProperties().containsKey("ApplicationVersion")) {
                    if (bean.getProperties().get("Location").equalsIgnoreCase(server) &&
                            bean.getProperties().get("Application").equalsIgnoreCase(application.getName()) &&
                            bean.getProperties().get("ApplicationVersion").equalsIgnoreCase(application.getVersion())) {
                        logger.debug("Trying to delete MDS files {} using bean {}", mdsFiles, bean);
                        bean.invoke(MDS_DELETE, mdsFiles, null, false, false, false, true);
                        logger.debug("Deleted MDS files");
                    }
                }
            }
        });
    }

    protected void destroyConnection() {
        logger.debug("Trying to destroy MDS Connection");
        logger.debug("Destroyed MDS Connection");
    }

}
