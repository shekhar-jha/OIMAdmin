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
import com.jhash.oimadmin.oim.OIMJMXWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

public class EventHandlersTreeNode extends AbstractUIComponentTreeNode<OIMJMXWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlersTreeNode.class);

    private OIMJMXWrapper connection;
    private Set<OIMJMXWrapper.OperationDetail> operations;

    public EventHandlersTreeNode(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        connection = new OIMJMXWrapper();
        connection.initialize(configuration);
        operations = connection.getOperations();
        OIMJMXWrapper.OperationDetail[] sortedOperationDetail = operations.toArray(new OIMJMXWrapper.OperationDetail[]{});
        logger.debug("Trying to sort the Event Handlers {} based on their names...", sortedOperationDetail);
        Arrays.sort(sortedOperationDetail, new Comparator<OIMJMXWrapper.OperationDetail>() {

            @Override
            public int compare(OIMJMXWrapper.OperationDetail o1, OIMJMXWrapper.OperationDetail o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
        logger.debug("Sorted the Event Handlers. Creating nodes for the operations {}", sortedOperationDetail);
        for (OIMJMXWrapper.OperationDetail operation : sortedOperationDetail) {
            EventHandlerTreeNode treeNode = new EventHandlerTreeNode(operation.description + "(" + operation.name + ")", operation, connection,
                    configuration, selectionTree, displayArea, NODE_STATE.INITIALIZED);
            selectionTree.addChildNode(this, treeNode);
        }
    }

    @Override
    public OIMJMXWrapper getComponent() {
        return connection;
    }

    @Override
    public void destroyComponent() {
        if (connection != null) {
            try {
                connection.destroy();
            } catch (Exception exception) {
                logger.warn("Failed to destroy JMX Connection {}. Ignoring error.", connection, exception);
            }
            connection = null;
        }
        if (operations != null) {
            operations.clear();
            operations = null;
        }
    }

}
