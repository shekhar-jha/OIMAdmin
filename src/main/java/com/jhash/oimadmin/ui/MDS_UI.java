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
import com.jhash.oimadmin.Connection;
import com.jhash.oimadmin.OIMAdminTreeNode;
import com.jhash.oimadmin.oim.MDSConnectionJMX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MDS_UI extends AbstractUIComponent {

    public static final String UI_COMPONENT_NAME = "MDS UI";
    private static final Logger logger = LoggerFactory.getLogger(MDS_UI.class);
    private Map<OIMAdminTreeNode, Connection> connections = new HashMap<OIMAdminTreeNode, Connection>();

    @Override
    public String getName() {
        return UI_COMPONENT_NAME;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initialized MDS UI...");
    }

    public void loadMDSConnectionNode(OIMAdminTreeNode node, JTree selectionTree, JTabbedPane displayArea) {
        DefaultTreeModel model = (DefaultTreeModel) selectionTree.getModel();
        MDSConnectionJMX tmpConnection = node.getValue();
        tmpConnection.initialize(node.configuration);
        Set<MDSConnectionJMX.MDSPartition> partitions = tmpConnection.getMDSPartitions();
        for (MDSConnectionJMX.MDSPartition partition : partitions) {
            OIMAdminTreeNode createdNode = addUninitializedNode(node, new MDSPartitionUI.MDSPartitionAdminTreeNode(partition.toString(), partition, config, node.configuration, selectionTree, displayArea), model);
        }
        connections.put(node, tmpConnection);
    }


    @Override
    public void destroyComponent() {
        logger.debug("Trying to destroy MDS component {}", this);
        if (connections != null && !connections.isEmpty()) {
            for (Connection connection : connections.values()) {
                try {
                    connection.destroy();
                } catch (Exception exception) {
                    logger.warn("Failed to destroy connection " + connection + ". Ignoring error.", exception);
                }
            }
            connections.clear();
        }
        logger.debug("Destroyed MDS component {}", this);
    }

    @Override
    public String getStringRepresentation() {
        return UI_COMPONENT_NAME + "[" + connections.keySet() + "]";
    }


    public static class MDSAdminTreeNode extends OIMAdminTreeNode {

        private final JTabbedPane displayArea;
        private final JTree selectionTree;
        private final Config config;
        private final MDSConnectionJMX mdsConnection;

        public MDSAdminTreeNode(String name, Config config, Config.Configuration configuration, JTree selectionTree, JTabbedPane displayArea) {
            super(name, NODE_TYPE.MDS, configuration);
            this.selectionTree = selectionTree;
            this.displayArea = displayArea;
            this.config = config;
            this.mdsConnection = new MDSConnectionJMX();
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
            switch (event) {
                case NODE_EXPAND:
                    executeLoaderService(MDSAdminTreeNode.this, selectionTree, new Runnable() {
                        @Override
                        public void run() {
                            MDS_UI mdsUI = config.getUIComponent(MDS_UI.class);
                            mdsUI.loadMDSConnectionNode(MDSAdminTreeNode.this, selectionTree, displayArea);
                        }
                    });
                    break;
                default:
                    logger.debug("Nothing to do for event {} on node {}", event, MDSAdminTreeNode.this);
                    break;
            }
        }

        @Override
        public MDSConnectionJMX getValue() {
            return mdsConnection;
        }
    }
}
