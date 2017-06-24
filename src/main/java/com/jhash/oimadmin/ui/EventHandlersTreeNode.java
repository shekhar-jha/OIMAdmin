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
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.eventHandlers.Manager;
import com.jhash.oimadmin.oim.eventHandlers.OperationDetail;
import com.jhash.oimadmin.oim.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class EventHandlersTreeNode extends AbstractUIComponentTreeNode<Manager> implements ContextMenuEnabledNode {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlersTreeNode.class);

    private final JPopupMenu eventHandlerMenu;
    private final JMenuItem newEventHandlerMenu;
    private final JMenuItem refreshMenu;
    private final List<EventHandlerUI> openedNewEventHandlers = new ArrayList<>();
    private final Manager eventManager;
    private Set<OperationDetail> operations;

    public EventHandlersTreeNode(final Manager eventManager, final PluginManager pluginManager, String name, final Config.Configuration configuration, final UIComponentTree selectionTree, final DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.eventManager = eventManager;
        eventHandlerMenu = new JPopupMenu();
        newEventHandlerMenu = new JMenuItem("New EventHandler...");
        newEventHandlerMenu.setEnabled(false);
        newEventHandlerMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EventHandlerUI eventHandlerUI = new EventHandlerUI(eventManager, pluginManager, "New EventHandler..", configuration, selectionTree, displayArea).setDestroyComponentOnClose(true);
                eventHandlerUI.initialize();
                openedNewEventHandlers.add(eventHandlerUI);
                displayArea.add(eventHandlerUI);
            }
        });
        refreshMenu = new JMenuItem("Reload..");
        refreshMenu.setEnabled(true);
        refreshMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("Started Reconnect Trigger");
                Utils.executeAsyncOperation("Reconnecting Connection", new Runnable() {
                    @Override
                    public void run() {
                        EventHandlersTreeNode.this.destroy();
                        EventHandlersTreeNode.this.initialize();
                    }
                });
                logger.debug("Completed Reconnect Trigger");

            }
        });
        eventHandlerMenu.add(newEventHandlerMenu);
        eventHandlerMenu.add(refreshMenu);
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        newEventHandlerMenu.setEnabled(true);
        operations = eventManager.getOperations();
        OperationDetail[] sortedOperationDetail = operations.toArray(new OperationDetail[]{});
        logger.debug("Trying to sort the Event Handlers {} based on their names...", sortedOperationDetail);
        Arrays.sort(sortedOperationDetail, new Comparator<OperationDetail>() {

            @Override
            public int compare(OperationDetail o1, OperationDetail o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
        logger.debug("Sorted the Event Handlers. Creating nodes for the operations {}", sortedOperationDetail);
        for (OperationDetail operation : sortedOperationDetail) {
            EventHandlerTreeNode treeNode = new EventHandlerTreeNode(eventManager, operation, operation.description + "(" + operation.name + ")",
                    configuration, selectionTree, displayArea);
            treeNode.initialize();
            selectionTree.addChildNode(this, treeNode);
        }
    }

    @Override
    public Manager getComponent() {
        return eventManager;
    }

    @Override
    public boolean hasContextMenu() {
        return true;
    }

    @Override
    public JPopupMenu getContextMenu() {
        return eventHandlerMenu;
    }

    @Override
    public void destroyComponent() {
        if (operations != null) {
            operations.clear();
            operations = null;
        }
        if (openedNewEventHandlers != null && !openedNewEventHandlers.isEmpty()) {
            for (EventHandlerUI eventHandlerUI : openedNewEventHandlers) {
                eventHandlerUI.destroy();
            }
            openedNewEventHandlers.clear();
        }
    }

}
