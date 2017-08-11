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

import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.mds.MDSConnectionJMX;
import com.jhash.oimadmin.oim.mds.MDSFile;
import com.jhash.oimadmin.oim.mds.MDSPartition;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.componentTree.UIComponentTree;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
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
                destroy();
                initialize();
            }
        });
        logger.debug("Trying to get location of jar file containing MDS items for partition {}", partition);
        partitionExportFileName = partition.getPartitionFiles();
        generateTreeFromExportedJarFile(partitionExportFileName);
        logger.debug("Initialized MDSPartitionTreeNode {}", this);
    }

    private void generateTreeFromExportedJarFile(String fileName) {
        logger.debug("Trying to open JarFile {} and process entries", fileName);
        //TODO: Create Util method for generating tree Map (Sorted) from Jar file
        Utils.processJarFile(fileName, new Utils.JarFileProcessor() {

            @Override
            public void process(JarFile jarFile, JarEntry jarEntry) {
                String filePath = jarEntry.getName();
                logger.debug("Jar entry being processed {} ", filePath);
                String[] filePathComponents = filePath.split("/");
                logger.debug("Split the file name {} as {}", filePath, filePathComponents);
                AbstractUIComponentTreeNode parentNode = MDSPartitionTreeNode.this;
                for (int depthCounter = 0; depthCounter < filePathComponents.length; depthCounter++) {
                    logger.trace("Trying to process depth level {}", depthCounter);
                    MDSFileTreeNode identifiedChildNode = null;
                    String filePathElement = filePathComponents[depthCounter];
                    logger.trace("File path element at depth {} is {}", depthCounter, filePathElement);
                    logger.trace("Trying to locate existing child node of {} with name {}", parentNode,
                            filePathElement);
                    List<UIComponentTree.Node> childNodes = getUIComponentTree().getChildNodes(parentNode);
                    for (UIComponentTree.Node childNodeObject : childNodes) {
                        if (childNodeObject instanceof MDSFileTreeNode) {
                            MDSFileTreeNode childNode = (MDSFileTreeNode) childNodeObject;
                            if (filePathElement.equals(childNode.getName())) {
                                logger.debug("Located an existing child node");
                                identifiedChildNode = childNode;
                            }
                        }
                    }
                    logger.debug("Trying to validate if an existing child node with same name was identified");
                    if (identifiedChildNode == null) {
                        logger.debug("Trying to check if file is a directory or directory in name of file.");
                        if (jarEntry.isDirectory() || depthCounter < (filePathComponents.length - 1)) {
                            logger.debug("Creating as directory node with {} status", INITIALIZED_NO_OP);
                            identifiedChildNode = new MDSFileTreeNode(filePathElement, MDSPartitionTreeNode.this, new MDSFile(mdsConnectionJMX, partition, jarFile, jarEntry), parentNode);
                            identifiedChildNode.initialize();
                            identifiedChildNode.setFolder(true);
                        } else {
                            logger.debug("Creating as file node with {} status", INITIALIZED);
                            identifiedChildNode = new MDSFileTreeNode(filePathElement, MDSPartitionTreeNode.this, new MDSFile(mdsConnectionJMX, partition, jarFile, jarEntry), parentNode);
                            identifiedChildNode.initialize();
                        }
                    }
                    logger.debug("Setting parent Node to new node since we are going to process next set of child nodes.");
                    parentNode = identifiedChildNode;
                }
            }
        });
        logger.debug("Populated partition files ");
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
