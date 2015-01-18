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
import com.jhash.oimadmin.oim.MDSConnectionJMX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class MDSTreeNode extends AbstractUIComponentTreeNode<MDSConnectionJMX> {

    private static final Logger logger = LoggerFactory.getLogger(MDSTreeNode.class);
    private MDSConnectionJMX mdsConnection;
    private Set<MDSConnectionJMX.MDSPartition> partitions;

    public MDSTreeNode(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {} ...", this);
        mdsConnection = new MDSConnectionJMX();
        mdsConnection.initialize(configuration);
        partitions = mdsConnection.getMDSPartitions();
        for (MDSConnectionJMX.MDSPartition partition : partitions) {
            MDSPartitionTreeNode createdNode = new MDSPartitionTreeNode(partition.toString(), partition, configuration, selectionTree, displayArea);
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
            for (MDSConnectionJMX.MDSPartition partition : partitions) {
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
