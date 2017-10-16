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
package com.jhash.oimadmin.ui.oim.mds;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.mds.MDSFile;
import com.jhash.oimadmin.oim.mds.MDSPartition;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MDSPartitionTreeNode extends AbstractUIComponentTreeNode<MDSPartitionTreeNode> {

    private static final Logger logger = LoggerFactory.getLogger(MDSPartitionTreeNode.class);
    private final MDSPartition partition;
    private JarFile partitionExportFile;
    private String partitionExportFileName;

    public MDSPartitionTreeNode(MDSPartition partition, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.partition = partition;
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing MDSPartitionTreeNode {} ...", this);
        registerMenu(REFRESH, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                destroy(false);
                initialize();
            }
        });
        logger.debug("Trying to get location of jar file containing MDS items for partition {}", partition);
        partitionExportFileName = generateFileName(true);
        partition.export(partitionExportFileName);
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(partitionExportFileName);
            generateMDSFileNode(Utils.createJarTree(partitionExportFileName), this, jarFile);
            partitionExportFile = jarFile;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to generate the MDS file nodes for " + this, exception);
        }
        logger.debug("Initialized MDSPartitionTreeNode {}", this);
    }

    protected String generateFileName(boolean export) {
        return getConfiguration().getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP + File.separator + "MDS"
                + (export ? "Export" : "Import") + System.currentTimeMillis() + ".jar";
    }

    public void save(MDSFile... mdsFiles) {
        if (mdsFiles == null || mdsFiles.length == 0)
            throw new OIMAdminException("No files were provided to save.");
        String importFileName = generateFileName(false);
        try {
            partition.importFile(importFileName, mdsFiles);
        } catch (Exception exception) {
            displayMessage("MDS Saving failed", "Failed to save MDS file(s)", exception);
        } finally {
            File file;
            if (importFileName != null && (file = new File(importFileName)).exists()) {
                try {
                    file.delete();
                } catch (Exception exception) {
                    logger.warn("Failed to delete MDS File " + importFileName, exception);
                }
            }
        }
    }

    private void generateMDSFileNode(Map<Utils.JarEntryKey, Object> jarSubTree, ParentComponent parent, JarFile jarFile) {
        if (jarSubTree == null)
            return;
        if (parent == null)
            parent = this;
        for (Map.Entry<Utils.JarEntryKey, Object> jarEntry : jarSubTree.entrySet()) {
            if (jarEntry.getValue() instanceof Map) {
                generateMDSFileNode((Map) jarEntry.getValue(), new MDSFileTreeNode(jarEntry.getKey().keyName, MDSPartitionTreeNode.this, parent).initialize(), jarFile);
            } else if (jarEntry.getValue() instanceof JarEntry) {
                new MDSFileTreeNode(jarEntry.getKey().keyName, MDSPartitionTreeNode.this,
                        new MDSFile(partition.getPartitionName(), jarFile, (JarEntry) jarEntry.getValue()), parent).initialize();
            } else {
                logger.warn("Failed to process jar entry {} identified in subtree {} generated from jar {}", new Object[]{jarEntry, jarSubTree, jarFile});
            }
        }
    }

    @Override
    public void destroyNode() {
        logger.debug("Destroying component {}", this);
        unregisterMenu(REFRESH);
        if (partitionExportFile != null) {
            try {
                logger.debug("Trying to close file {}", partitionExportFile);
                partitionExportFile.close();
                logger.debug("Closed file");
            } catch (Exception exception) {
                logger.warn("Failed to close file " + partitionExportFile, exception);
            }
            partitionExportFile = null;
        }
        if (partitionExportFileName != null) {
            File partitionExportFile = new File(partitionExportFileName);
            if (partitionExportFile.exists() && !partitionExportFile.isDirectory()) {
                try {
                    logger.debug("Trying to delete file {}", partitionExportFileName);
                    partitionExportFile.delete();
                    logger.debug("Deleted file");
                } catch (Exception exception) {
                    logger.warn("Failed to delete file " + partitionExportFileName, exception);
                }
            } else {
                logger.debug("File {} does not exists or is a directory.", partitionExportFileName);
            }
        }
        logger.debug("Destroyed component {}", this);
    }

}
