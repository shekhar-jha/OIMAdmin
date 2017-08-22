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

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.mds.MDSConnectionJMX;
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
    private final MDSConnectionJMX mdsConnectionJMX;
    private String partitionExportFileName;

    public MDSPartitionTreeNode(MDSPartition partition, MDSConnectionJMX mdsConnectionJMX, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.partition = partition;
        this.mdsConnectionJMX = mdsConnectionJMX;
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
        partitionExportFileName = partition.getPartitionFiles();
        try {
            JarFile jarFile = new JarFile(partitionExportFileName);
            generateMDSFileNode(Utils.createJarTree(partitionExportFileName), this, jarFile);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to generate the MDS file nodes for " + this, exception);
        }
        logger.debug("Initialized MDSPartitionTreeNode {}", this);
    }

    private void generateMDSFileNode(Map<String, Object> jarSubTree, ParentComponent parent, JarFile jarFile) {
        if (jarSubTree == null)
            return;
        if (parent == null)
            parent = this;
        for (Map.Entry<String, Object> jarEntry : jarSubTree.entrySet()) {
            if (jarEntry.getValue() instanceof Map) {
                generateMDSFileNode((Map) jarEntry.getValue(), new MDSFileTreeNode(jarEntry.getKey(), MDSPartitionTreeNode.this, parent).initialize(), jarFile);
            } else if (jarEntry.getValue() instanceof JarEntry) {
                new MDSFileTreeNode(jarEntry.getKey(), MDSPartitionTreeNode.this,
                        new MDSFile(mdsConnectionJMX, partition, jarFile, (JarEntry) jarEntry.getValue()), parent).initialize();
            } else {
                logger.warn("Failed to process jar entry {} identified in subtree {} generated from jar {}", new Object[]{jarEntry, jarSubTree, jarFile});
            }
        }
    }

    @Override
    public void destroyNode() {
        logger.debug("Destroying component {}", this);
        unregisterMenu(REFRESH);
        if (partitionExportFileName != null) {
            File exportFile = new File(partitionExportFileName);
            logger.trace("Validating whether file {} exists and is not a directory", partitionExportFileName);
            if (exportFile.exists() && exportFile.isFile()) {
                try {
                    logger.debug("Trying to delete file {}", partitionExportFileName);
                    exportFile.delete();
                    logger.debug("Deleted file");
                } catch (Exception exception) {
                    logger.warn("Failed to delete file " + partitionExportFileName, exception);
                }
            } else {
                logger.warn("Expected file {} containing export for MDS Partition {} to be a file.", partitionExportFileName, partition);
            }
            partitionExportFileName = null;
        }
        logger.debug("Destroyed component {}", this);
    }

}
