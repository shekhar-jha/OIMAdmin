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

package com.jhash.oimadmin.ui.oim.eventHandlers;

import com.jhash.oimadmin.oim.eventHandlers.Manager;
import com.jhash.oimadmin.oim.eventHandlers.OperationDetail;
import com.jhash.oimadmin.oim.plugins.PluginManager;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

public class EventHandlersTreeNode extends AbstractUIComponentTreeNode<EventHandlersTreeNode> {

    public static final MenuHandler.MENU NEW_EVENT_HANDLER = new MenuHandler.MENU("Event Handler", MenuHandler.MENU.NEW, "New Event Handler");
    private static final Logger logger = LoggerFactory.getLogger(EventHandlersTreeNode.class);

    private final Manager eventManager;
    private final PluginManager pluginManager;
    private Set<OperationDetail> operations;

    public EventHandlersTreeNode(final Manager eventManager, final PluginManager pluginManager, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.eventManager = eventManager;
        this.pluginManager = pluginManager;
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing {}", this);
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
            new EventHandlerTreeNode(eventManager, operation, operation.description + "(" + operation.name + ")",
                    this).initialize();
        }
        registerMenu(NEW_EVENT_HANDLER, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                new EventHandlerUI(eventManager, pluginManager, "New EventHandler..", EventHandlersTreeNode.this)
                        .setDestroyComponentOnClose(true).setPublish(true).initialize();
            }
        });
        registerMenu(REFRESH, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                EventHandlersTreeNode.this.destroy(false);
                EventHandlersTreeNode.this.initialize();
            }
        });

    }

    @Override
    public void destroyNode() {
        unregisterMenu(NEW_EVENT_HANDLER);
        unregisterMenu(REFRESH);
        if (operations != null) {
            operations.clear();
            operations = null;
        }
    }

}
