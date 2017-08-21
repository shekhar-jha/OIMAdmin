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

import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.mds.MDSConnectionJMX;
import com.jhash.oimadmin.oim.mds.MDSPartition;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.oim.connection.ConnectionTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class MDSTreeNode extends AbstractUIComponentTreeNode<MDSTreeNode> {

    private static final Logger logger = LoggerFactory.getLogger(MDSTreeNode.class);
    private final JMXConnection jmxConnection;
    private MDSConnectionJMX mdsConnection;
    private Set<MDSPartition> partitions;

    public MDSTreeNode(JMXConnection jmxConnection, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.jmxConnection = jmxConnection;
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing {} ...", this);
        mdsConnection = new MDSConnectionJMX(jmxConnection);
        mdsConnection.initialize(getConfiguration());
        partitions = mdsConnection.getMDSPartitions();
        for (MDSPartition partition : partitions) {
            new MDSPartitionTreeNode(partition, mdsConnection, partition.toString(), this).publish();
        }
        logger.debug("Initialized {} ...", this);
    }

    @Override
    public void destroyNode() {
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
