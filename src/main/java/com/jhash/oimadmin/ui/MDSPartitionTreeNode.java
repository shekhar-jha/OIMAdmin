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
package com.jhash.oimadmin.ui;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminTreeNode;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.MDSConnectionJMX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MDSPartitionTreeNode extends AbstractUIComponentTreeNode<MDSConnectionJMX.MDSPartition> {

    private static final Logger logger = LoggerFactory.getLogger(MDSPartitionTreeNode.class);
    private final MDSConnectionJMX.MDSPartition partition;
    private String partitionExportFileName;

    public MDSPartitionTreeNode(String name, MDSConnectionJMX.MDSPartition partition, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.partition = partition;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing MDSPartitionTreeNode {} ...", this);
        logger.debug("Trying to get location of jar file containing MDS items for partition {}", partition);
        partitionExportFileName = partition.getPartitionFiles();
        OIMAdminTreeNode rootNodeOfMDSPartitionFiles = generateTreeFromExportedJarFile(partitionExportFileName);
        logger.debug("Loaded the partition files as tree {}, Trying to add {} children to JTree",
                rootNodeOfMDSPartitionFiles, rootNodeOfMDSPartitionFiles.getChildCount());
        while (rootNodeOfMDSPartitionFiles.getChildCount() > 0) {
            logger.debug("Trying to get next child");
            OIMAdminTreeNode childNode = (OIMAdminTreeNode) rootNodeOfMDSPartitionFiles.getFirstChild();
            logger.debug("Adding child node {} to {} at {}", new Object[]{childNode, this, getChildCount()});
            selectionTree.addChildNode(this, childNode);
            logger.debug("Added child node to {} and this automatically removes it from other tree {}", this, rootNodeOfMDSPartitionFiles);
        }
        logger.debug("Initialized MDSPartitionTreeNode {}", this);
    }

    @Override
    public MDSConnectionJMX.MDSPartition getComponent() {
        return partition;
    }

    private OIMAdminTreeNode generateTreeFromExportedJarFile(String fileName) {
        logger.debug("Trying to open JarFile {} and process entries", fileName);
        OIMAdminTreeNode root = new ROOTAdminTreeNode("root");
        //TODO: Create Util method for generating tree from Jar file
        Utils.processJarFile(fileName, new Utils.JarFileProcessor() {

            @Override
            public void process(JarFile jarFile, JarEntry jarEntry) {
                String filePath = jarEntry.getName();
                logger.debug("Jar entry being processed {} ", filePath);
                String[] filePathComponents = filePath.split("/");
                logger.debug("Split the file name {} as {}", filePath, filePathComponents);
                OIMAdminTreeNode parentNode = root;
                for (int depthCounter = 0; depthCounter < filePathComponents.length; depthCounter++) {
                    logger.trace("Trying to process depth level {}", depthCounter);
                    MDSFileTreeNode identifiedChildNode = null;
                    String filePathElement = filePathComponents[depthCounter];
                    logger.trace("File path element at depth {} is {}", depthCounter, filePathElement);
                    logger.trace("Trying to locate existing child node of {} with name {}", parentNode,
                            filePathElement);
                    for (Enumeration<?> parentNodeChildrenEnumeration = parentNode.children(); parentNodeChildrenEnumeration
                            .hasMoreElements(); ) {
                        MDSFileTreeNode childNode = (MDSFileTreeNode) parentNodeChildrenEnumeration.nextElement();
                        if (filePathElement.equals(childNode.getName())) {
                            logger.debug("Located an existing child node");
                            identifiedChildNode = childNode;
                        }
                    }
                    logger.debug("Trying to validate if an existing child node with same name was identified");
                    if (identifiedChildNode == null) {
                        logger.debug("Trying to check if file is a directory or directory in name of file.");
                        if (jarEntry.isDirectory() || depthCounter < (filePathComponents.length - 1)) {
                            logger.debug("Creating as directory node with {} status", OIMAdminTreeNode.NODE_STATE.INITIALIZED_NO_OP);
                            identifiedChildNode = new MDSFileTreeNode(filePathElement, MDSPartitionTreeNode.this, new MDSConnectionJMX.MDSFile(partition, jarFile, jarEntry), configuration, selectionTree, displayArea);
                            identifiedChildNode.initialize();
                            identifiedChildNode.setStatus(NODE_STATE.INITIALIZED_NO_OP);
                        } else {
                            logger.debug("Creating as file node with {} status", NODE_STATE.INITIALIZED);
                            identifiedChildNode = new MDSFileTreeNode(filePathElement, MDSPartitionTreeNode.this, new MDSConnectionJMX.MDSFile(partition, jarFile, jarEntry), configuration, selectionTree, displayArea);
                            identifiedChildNode.initialize();
                        }
                        logger.debug("Adding the child node {} to parent {}", identifiedChildNode, parentNode);
                        parentNode.add(identifiedChildNode);
                    }
                    logger.debug("Setting parent Node to new node since we are going to process next set of child nodes.");
                    parentNode = identifiedChildNode;
                }
            }
        });
        logger.debug("Got partition files as {} with {} children", root, root.getChildCount());
        return root;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying component {}", this);
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
