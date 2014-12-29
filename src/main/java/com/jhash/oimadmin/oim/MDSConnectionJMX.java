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
import oracle.mds.lcm.client.*;
import oracle.mds.lcm.client.MetadataTransferManager.ASPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class MDSConnectionJMX extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(MDSConnectionJMX.class);
    protected String STRING_REPRESENTATION = "MDSConnectionJMX:";
    private JMXConnection jmxConnection = null;
    private ASPlatform platform = null;
    private List<MDSPartition> mdsPartitions = new ArrayList<MDSPartition>();

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
        JMXConnection tmpConnection = new JMXConnection();
        logger.debug("Trying to initialize JMX Connection.");
        tmpConnection.initialize(config);
        logger.debug("Trying to get JMX Server Connection.");
        jmxConnection = tmpConnection;
        STRING_REPRESENTATION += "(" + jmxConnection + ")";
    }

    public Set<MDSPartition> getMDSPartitions() {
        logger.debug("Trying to get all MDS Partitions...");
        Set<MDSPartition> partitions = new HashSet<MDSPartition>();
        logger.debug("Trying to get MBeanServerConnection");
        MBeanServerConnection connection = jmxConnection.getConnection();
        logger.debug("Trying to get all the servers running at the moment from {}", jmxConnection);
        Set<String> runtimeServers = jmxConnection.getRuntimeServers();
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
                    MDSPartition newPartitionIdentified = new MDSPartition(runtimeServer, applicationInfo);
                    partitions.add(newPartitionIdentified);
                    mdsPartitions.add(newPartitionIdentified);
                }
            } catch (Exception exception) {

            }
            logger.debug("Created MetadataTransferManager {}", mdsTransfer);

        }
        logger.debug("Returning MDS Partitions {}", partitions);
        return partitions;
    }

    private void importMetaData(MDSAppInfo application, TargetInfo target, String importFileLocation) throws Exception {
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
        File importFile = new File(importFileLocation);
        logger.debug("Trying to delete the imported file {}", importFileLocation);
        importFile.delete();
        logger.debug("Deleted the imported file", importFileLocation);
        logger.debug("Completed import of Meta data from MDS repository");
    }

    private void exportMetaData(MDSAppInfo application, TargetInfo target, String exportFileLocation) throws Exception {
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
            Thread.currentThread();
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

    protected void destroyConnection() {
        logger.debug("Trying to destroy MDS Connection");
        if (mdsPartitions != null && !mdsPartitions.isEmpty()) {
            for (MDSPartition partition : mdsPartitions) {
                try {
                    partition.destroy();
                } catch (Exception exception) {
                    logger.warn("Failed to destroy partition {} ", partition, exception);
                }
            }
        }
        if (this.jmxConnection != null) {
            logger.debug("Trying to destroy JMX connection {}", jmxConnection);
            jmxConnection.destroy();
            logger.debug("Destroyed JMX connection.");
            jmxConnection = null;
        }
        logger.debug("Destroyed MDS Connection");
    }

    public static class MDSFile {

        public final MDSPartition partition;
        public final JarFile jarFile;
        public final JarEntry file;
        private String content = null;

        public MDSFile(MDSPartition partition, JarFile jarFile, JarEntry file) {
            this.partition = partition;
            this.jarFile = jarFile;
            this.file = file;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }

    public class MDSPartition {
        public final String serverName;
        public final MDSAppInfo application;
        private final String stringValue;
        private String nameOfPartitionFilesExported;

        private MDSPartition(String serverName, MDSAppInfo application) {
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

        private String generateFileName(boolean export) {
            return config.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP + File.separator + "MDS"
                    + (export ? "Export" : "Import") + System.currentTimeMillis() + ".jar";
        }

        public void resetPartitionFiles() {
            File existingFile;
            try {
                if (nameOfPartitionFilesExported != null
                        && (existingFile = new File(nameOfPartitionFilesExported)).exists()) {
                    nameOfPartitionFilesExported = null;
                    existingFile.delete();
                }
            } catch (Exception exception) {
                throw new OIMAdminException("Failed to cleanup the existing partition file"
                        + nameOfPartitionFilesExported + " before reloading the new file for " + application, exception);
            }
        }

        public String getPartitionFiles() {
            logger.debug("Trying to get files present in MDS Partition {}", this);
            String fileName = null;
            if (nameOfPartitionFilesExported == null || (!(new File(nameOfPartitionFilesExported).exists()))) {
                fileName = generateFileName(true);
                logger.debug("Generated the dump file as {}", fileName);
                try {
                    MDSConnectionJMX.this.exportMetaData(application, new TargetInfo(serverName), fileName);
                    nameOfPartitionFilesExported = fileName;
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to export MDS repository " + application + " from location "
                            + serverName + " into file " + fileName, exception);
                }
            } else {
                fileName = nameOfPartitionFilesExported;
            }
            return fileName;
        }

        public void savePartitionFile(MDSFile file) {
            if (file == null || file.file == null || file.file.isDirectory() || file.content == null) {
                logger.debug("Nothing to do since file {} has invalid jar entry or content", file);
            } else {
                String fileName = generateFileName(false);
                try (JarOutputStream importFileOutputStream = new JarOutputStream(new FileOutputStream(fileName))) {
                    JarEntry newFileEntry = new JarEntry(file.file.getName());
                    newFileEntry.setTime(System.currentTimeMillis());
                    importFileOutputStream.putNextEntry(newFileEntry);
                    importFileOutputStream.write(file.content.getBytes());
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to create the jar file " + fileName, exception);
                }
                try {
                    MDSConnectionJMX.this.importMetaData(file.partition.application, new TargetInfo(
                            file.partition.serverName), fileName);
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to import file " + fileName + " into MDS repository "
                            + application + " in location " + serverName, exception);
                }
            }
        }

        public void destroy() {
            logger.debug("Trying to destroy MDS Partition {}", stringValue);
            File partitionFileExported;
            logger.debug("Trying to validate whether partition file {} associated with partition exists",
                    nameOfPartitionFilesExported);
            if (nameOfPartitionFilesExported != null
                    && (partitionFileExported = new File(nameOfPartitionFilesExported)).exists()) {

                logger.debug("Trying to delete partition file");
                partitionFileExported.delete();
                logger.debug("Deleted partition file");
            }
            logger.debug("Destroyed MDS Partition {}", stringValue);
        }
    }
}
