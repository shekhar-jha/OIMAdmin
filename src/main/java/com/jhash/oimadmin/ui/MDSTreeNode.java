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
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.mds.MDSConnectionJMX;
import com.jhash.oimadmin.oim.mds.MDSPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class MDSTreeNode extends AbstractUIComponentTreeNode<MDSConnectionJMX> {

    private static final Logger logger = LoggerFactory.getLogger(MDSTreeNode.class);
    private final ConnectionTreeNode.Connections connections;
    private MDSConnectionJMX mdsConnection;
    private Set<MDSPartition> partitions;

    public MDSTreeNode(ConnectionTreeNode.Connections connections, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.connections = connections;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {} ...", this);
        JMXConnection jmxConnection = connections.getConnection(ConnectionTreeNode.CONNECTION_TYPES.JMX);
        if (jmxConnection == null)
            throw new NullPointerException("No JMX Connection is available.");
        mdsConnection = new MDSConnectionJMX(jmxConnection);
        mdsConnection.initialize(configuration);
        partitions = mdsConnection.getMDSPartitions();
        for (MDSPartition partition : partitions) {
            MDSPartitionTreeNode createdNode = new MDSPartitionTreeNode(partition, mdsConnection, partition.toString(), configuration, selectionTree, displayArea);
            selectionTree.addChildNode(this, createdNode);
        }
        logger.debug("Initialized {} ...", this);
    }

    @Override
    public MDSConnectionJMX getComponent() {
        return mdsConnection;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying {} ...", this);
        if (partitions != null) {
            for (MDSPartition partition : partitions) {
                try {
                    partition.destroy();
                } catch (Exception exception) {
                    logger.warn("Failed to destroy MDS Partition " + partition + ". Ignoring error", exception);
                }
            }
            partitions = null;
        }
        if (mdsConnection != null) {
            try {
                logger.debug("Trying to destroy MDS Connection {}", mdsConnection);
                mdsConnection.destroy();
                logger.debug("Destroyed MDS Connection {}", mdsConnection);
            } catch (Exception exception) {
                logger.warn("Failed to destroy MDS Connection " + mdsConnection + ". Ignoring error.", exception);
            }
            mdsConnection = null;
        }
        logger.debug("Destroyed {}", this);
    }

}
