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
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.OIMAdminTreeNode;
import com.jhash.oimadmin.oim.MDSConnectionJMX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MDSPartitionUI extends AbstractUIComponent {
    public static final String UI_COMPONENT_NAME = "MDSPartitionUI";
    private static final Logger logger = LoggerFactory.getLogger(MDSPartitionUI.class);

    @Override
    public String getName() {
        return UI_COMPONENT_NAME;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initialized Connections...");
    }


    public void loadMDSPartitionConnectionNode(OIMAdminTreeNode node, JTree selectionTree, JTabbedPane displayArea) {
        Object mdsPartitionObject = null;
        DefaultTreeModel model = (DefaultTreeModel) selectionTree.getModel();
        logger.debug("Trying to load MDS Partition details at node {} for model {}", node, model);
        if (node == null || node.getValue() == null
                || (!((mdsPartitionObject = node.getValue()) instanceof MDSConnectionJMX.MDSPartition))) {
            throw new NullPointerException("Failed to locate the MDSPartition " + mdsPartitionObject
                    + " associated with node " + node);
        }
        MDSConnectionJMX.MDSPartition mdsPartitionToBeProcessed = (MDSConnectionJMX.MDSPartition) mdsPartitionObject;
        logger.debug("Trying to load the partition files in tree structure");

        String fileName = mdsPartitionToBeProcessed.getPartitionFiles();
        OIMAdminTreeNode rootNodeOfMDSPartitionFiles = setupMDSFiles(fileName, mdsPartitionToBeProcessed, node, selectionTree, displayArea);
        logger.debug("Loaded the partition files as tree {}, Trying to add {} children to JTree",
                rootNodeOfMDSPartitionFiles, rootNodeOfMDSPartitionFiles.getChildCount());
        rootNodeOfMDSPartitionFiles.children();
        while (rootNodeOfMDSPartitionFiles.getChildCount() > 0) {
            logger.debug("Trying to get next child");
            OIMAdminTreeNode childNode = (OIMAdminTreeNode) rootNodeOfMDSPartitionFiles.getFirstChild();
            logger.debug("Adding child node {} to {} at {}", new Object[]{childNode, node, node.getChildCount()});
            model.insertNodeInto(childNode, node, node.getChildCount());
            logger.debug("Added child node");
        }
        logger.debug("Loaded MDS partition details for {} as {}", mdsPartitionToBeProcessed,
                rootNodeOfMDSPartitionFiles);
    }

    private OIMAdminTreeNode setupMDSFiles(String fileName, MDSConnectionJMX.MDSPartition mdsPartitionToBeProcessed, OIMAdminTreeNode node, JTree selectionTree, JTabbedPane displayArea) {
        logger.debug("Trying to open JarFile {} and process entries", fileName);
        OIMAdminTreeNode root = new OIMAdminTreeNode.OIMAdminTreeNodeNoAction("root", OIMAdminTreeNode.NODE_TYPE.ROOT, node.configuration);
        try {
            JarFile exportedFile = new JarFile(fileName);
            for (Enumeration<JarEntry> jarEntryEnumeration = exportedFile.entries(); jarEntryEnumeration
                    .hasMoreElements(); ) {
                JarEntry jarEntry = jarEntryEnumeration.nextElement();
                logger.debug("Trying to process entry {} ", jarEntry);
                String filePath = jarEntry.getName();
                logger.debug("Jar entry being processed {} ", filePath);
                String[] filePathComponents = filePath.split("/");
                logger.debug("Split the file name {} as {}", filePath, filePathComponents);
                OIMAdminTreeNode parentNode = root;
                for (int depthCounter = 0; depthCounter < filePathComponents.length; depthCounter++) {
                    logger.trace("Trying to process depth level {}", depthCounter);
                    OIMAdminTreeNode identifiedChildNode = null;
                    String filePathElement = filePathComponents[depthCounter];
                    logger.trace("File path element at depth {} is {}", depthCounter, filePathElement);
                    logger.trace("Trying to locate existing child node of {} with name {}", parentNode,
                            filePathElement);
                    for (Enumeration<?> parentNodeChildrenEnumeration = parentNode.children(); parentNodeChildrenEnumeration
                            .hasMoreElements(); ) {
                        OIMAdminTreeNode childNode = (OIMAdminTreeNode) parentNodeChildrenEnumeration.nextElement();
                        if (filePathElement.equals(childNode.name)) {
                            logger.debug("Located an existing child node");
                            identifiedChildNode = childNode;
                        }
                    }
                    logger.debug("Trying to validate if an existing child node with same name was identified");
                    if (identifiedChildNode == null) {
                        logger.debug("No existing child node was identified. Trying to create a new child node with name");
                        identifiedChildNode = new MDSFileUI.MDSFileAdminTreeNode(filePathElement, new MDSConnectionJMX.MDSFile(mdsPartitionToBeProcessed, exportedFile, jarEntry), config, node.configuration, selectionTree, displayArea);
                        logger.debug("Trying to check if file is a directory or directory in name of file.");
                        if (jarEntry.isDirectory() || depthCounter < (filePathComponents.length - 1)) {
                            logger.debug(
                                    "Setting the status to {} since a file directory will not have additional operations that needs to be triggered",
                                    OIMAdminTreeNode.NODE_STATE.INITIALIZED_NO_OP);
                            identifiedChildNode.setStatus(OIMAdminTreeNode.NODE_STATE.INITIALIZED_NO_OP);
                        } else {
                            logger.debug("Setting the status to initialized since this file represents file");
                            identifiedChildNode.setStatus(OIMAdminTreeNode.NODE_STATE.INITIALIZED);
                        }
                        logger.debug("Adding the child node {} to parent {}", identifiedChildNode, parentNode);
                        parentNode.add(identifiedChildNode);
                    }
                    logger.debug("Setting parent Node to new node since we are going to process next set of child nodes.");
                    parentNode = identifiedChildNode;
                }
            }
            logger.debug("Processed all the jar entries");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to open the jar file " + fileName, exception);
        }
        logger.debug("Got partition files as {} with {} children", root, root.getChildCount());
        return root;
    }

    @Override
    public void destroyComponent() {

    }

    @Override
    public String getStringRepresentation() {
        return UI_COMPONENT_NAME;
    }


    public static class MDSPartitionAdminTreeNode extends OIMAdminTreeNode {

        private final JTree selectionTree;
        private final JTabbedPane displayArea;
        private final MDSConnectionJMX.MDSPartition partition;
        private final Config config;

        public MDSPartitionAdminTreeNode(String name, MDSConnectionJMX.MDSPartition partition, Config config, Config.Configuration configuration, JTree selectionTree, JTabbedPane displayArea) {
            super(name, NODE_TYPE.MDS_PARTITION, configuration);
            this.selectionTree = selectionTree;
            this.displayArea = displayArea;
            this.partition = partition;
            this.config = config;
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
            switch (event) {
                case NODE_EXPAND:
                    MDSPartitionUI mdsPartitionUI = config.getUIComponent(MDSPartitionUI.class);
                    executeLoaderService(MDSPartitionAdminTreeNode.this, selectionTree, new Runnable() {
                        @Override
                        public void run() {
                            mdsPartitionUI.loadMDSPartitionConnectionNode(MDSPartitionAdminTreeNode.this, selectionTree, displayArea);
                        }
                    });
                    break;
                default:
                    logger.debug("Nothing to do for event {} on node {}", event, this);
                    break;
            }
        }

        @Override
        public MDSConnectionJMX.MDSPartition getValue() {
            return partition;
        }
    }
}
