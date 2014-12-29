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
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.jar.JarFile;

public class MDSFileUI extends AbstractUIComponent {

    public static final String UI_COMPONENT_NAME = "MDS File UI";
    private static final Logger logger = LoggerFactory.getLogger(MDSFileUI.class);

    @Override
    public String getName() {
        return UI_COMPONENT_NAME;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initialized MDS File UI...");
    }

    public JComponent displayMDSPartitionFile(OIMAdminTreeNode node, JTree selectionTree, JTabbedPane displayArea) {
        logger.debug("Trying to display MDS Partition file associated with node {}", node);
        Object mdsFileObject = node.getValue();
        logger.debug("Trying to validate if value {} attached with node is instance of MDSFile", mdsFileObject, node);
        if (mdsFileObject instanceof MDSConnectionJMX.MDSFile) {
            final MDSConnectionJMX.MDSFile mdsFile = (MDSConnectionJMX.MDSFile) mdsFileObject;
            StringBuilder readFileData = new StringBuilder();
            JarFile jarFile = mdsFile.jarFile;
            // TODO: is closing reader here ok or do we want to close jar file
            try (BufferedReader jarFileInputStream = new BufferedReader(new InputStreamReader(
                    jarFile.getInputStream(mdsFile.file)))) {
                String readLine;
                while ((readLine = jarFileInputStream.readLine()) != null) {
                    readFileData.append(readLine);
                    readFileData.append(System.lineSeparator());
                }
            } catch (Exception exception) {
                throw new OIMAdminException(
                        "Failed to read file " + mdsFile.file + " in jar file " + jarFile.getName(), exception);
            }
            JTextArea mdsFileTextArea = new JTextArea(readFileData.toString());
            JPanel mdsPartitionFilePanel = new JPanel(new BorderLayout());
            mdsPartitionFilePanel.add(new JideScrollPane(mdsFileTextArea), BorderLayout.CENTER);
            JideButton saveButton = new JideButton("Save");
            saveButton.setActionCommand("SAVE");
            saveButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        logger.debug("Trying to {} node {}", e.getActionCommand(), node.name);
                        saveButton.setEnabled(false);
                        String operand = mdsFileTextArea.getText();
                        MDSFileUI fileUI = config.getUIComponent(MDSFileUI.class);
                        runOperation(mdsPartitionFilePanel, new Runnable() {

                            @Override
                            public void run() {
                                saveMDSFile(node, operand, selectionTree, displayArea);
                            }
                        });
                        logger.debug("Completed command on node");
                    } catch (Exception exception) {
                        logger.warn("Failed to perform {} on the display panel for {}",
                                new Object[]{e.getActionCommand(), node.name}, exception);
                    }
                }
            });
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(saveButton);
            mdsPartitionFilePanel.add(buttonPanel, BorderLayout.NORTH);
            return mdsPartitionFilePanel;
        } else {
            return null;
        }
    }

    private void saveMDSFile(OIMAdminTreeNode node, Object operand, JTree selectionTree, JTabbedPane displayArea) {
        logger.debug("Trying to display MDS Partition file associated with node {}", node);
        Object mdsFileObject = node.getValue();
        if (operand == null)
            throw new NullPointerException("The content of MDS file to be saved is null");
        if (!(operand instanceof String)) {
            throw new InvalidParameterException("The operand is of type " + operand.getClass()
                    + " instead of String as expected.");
        }
        logger.debug("Trying to validate if value {} attached with node {} is instance of MDSFile", mdsFileObject, node);
        if (mdsFileObject instanceof MDSConnectionJMX.MDSFile) {
            MDSConnectionJMX.MDSFile mdsFile = (MDSConnectionJMX.MDSFile) mdsFileObject;
            logger.debug("Overwriting the content of MDS File");
            mdsFile.setContent((String) operand);
            logger.debug("Trying to save updated MDS File");
            mdsFile.partition.savePartitionFile(mdsFile);
            logger.debug("Saved. Trying to start the refresh of the MDS Partition tree");
            TreeNode[] parentNodes = node.getPath();
            OIMAdminTreeNode mdsPartitionNode = null;
            logger.debug("Trying to look up the MDS_PARTITION base node in the MDS File's hierarchy {}", parentNodes);
            for (int bubbleUp = (parentNodes.length - 1); bubbleUp >= 0; bubbleUp--) {
                if (parentNodes[bubbleUp] instanceof OIMAdminTreeNode) {
                    if (((OIMAdminTreeNode) parentNodes[bubbleUp]).type == OIMAdminTreeNode.NODE_TYPE.MDS_PARTITION) {
                        mdsPartitionNode = (OIMAdminTreeNode) parentNodes[bubbleUp];
                        logger.debug("Identified the MDS_PARTITION node as {} at level {}", mdsPartitionNode, bubbleUp);
                        break;
                    }
                }
            }
            DefaultTreeModel model = (DefaultTreeModel) selectionTree.getModel();
            if (mdsPartitionNode != null) {
                logger.debug("Resetting the identified partition node {}", mdsPartitionNode);
                resetNode(mdsPartitionNode, model);
                logger.debug("Inserting a {} node of type {} to indicate that refresh is going on",
                        OIMAdmin.DUMMY_LEAF_NODE_NAME, mdsPartitionNode.type);
                model.insertNodeInto(new OIMAdminTreeNode.OIMAdminTreeNodeNoAction(OIMAdmin.DUMMY_LEAF_NODE_NAME, mdsPartitionNode.type,
                        mdsPartitionNode.configuration), mdsPartitionNode, mdsPartitionNode.getChildCount());
                MDSConnectionJMX.MDSPartition mdsPartition = null;
                logger.debug("Trying to check whether the partition node {} has value {} as MDSPartition",
                        mdsPartitionNode, mdsPartitionNode.getValue());
                if (mdsPartitionNode.getValue() != null && mdsPartitionNode.getValue() instanceof MDSConnectionJMX.MDSPartition) {
                    mdsPartition = (MDSConnectionJMX.MDSPartition) mdsPartitionNode.getValue();
                    logger.debug("Trying to reset the current partition files since we are going to reload the configuration");
                    mdsPartition.resetPartitionFiles();
                    logger.debug("Partition files reset");
                }
                logger.debug("Trying to start the loader process for MDS Partition connection node {}",
                        mdsPartitionNode);
                final OIMAdminTreeNode mdsPartitionNode1 = mdsPartitionNode;
                executeLoaderService(mdsPartitionNode, selectionTree, new Runnable() {
                    @Override
                    public void run() {
                        MDSPartitionUI partitionUI = config.getUIComponent(MDSPartitionUI.class);
                        partitionUI.loadMDSPartitionConnectionNode(mdsPartitionNode1, selectionTree, displayArea);
                    }
                });
                logger.debug("Completed the setup of loader process for MDS partition connection node");
            } else {
                logger.debug("Could not locate any MDS_PARTITION node. Though it should not happen, ignoring it");
            }
        } else {
            logger.debug("Nothing to do since the MDS File is not attached with node {}", node);
        }
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroyed MDS component {}", this);
    }

    @Override
    public String getStringRepresentation() {
        return UI_COMPONENT_NAME;
    }

    public static class MDSFileAdminTreeNode extends OIMAdminTreeNode {

        private final JTabbedPane displayArea;
        private final JTree selectionTree;
        private final Config config;
        private final MDSConnectionJMX.MDSFile mdsFile;

        public MDSFileAdminTreeNode(String name, MDSConnectionJMX.MDSFile mdsFile, Config config, Config.Configuration configuration, JTree selectionTree, JTabbedPane displayArea) {
            super(name, NODE_TYPE.MDS, configuration);
            this.selectionTree = selectionTree;
            this.displayArea = displayArea;
            this.config = config;
            this.mdsFile = mdsFile;
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
            switch (event) {
                case NODE_DISPLAY:
                    executeDisplayService(MDSFileAdminTreeNode.this, displayArea, new ExecuteCommand<JComponent>() {
                        @Override
                        public JComponent run() {
                            MDSFileUI mdsUI = config.getUIComponent(MDSFileUI.class);
                            return mdsUI.displayMDSPartitionFile(MDSFileAdminTreeNode.this, selectionTree, displayArea);
                        }
                    });
                    break;
                default:
                    logger.debug("Nothing to do for event {} on node {}", event, MDSFileAdminTreeNode.this);
                    break;
            }
        }

        @Override
        public MDSConnectionJMX.MDSFile getValue() {
            return mdsFile;
        }

    }
}