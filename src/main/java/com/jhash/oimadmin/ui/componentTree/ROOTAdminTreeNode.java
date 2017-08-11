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

package com.jhash.oimadmin.ui.componentTree;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.events.Event;
import com.jhash.oimadmin.events.EventConsumer;
import com.jhash.oimadmin.events.EventManager;
import com.jhash.oimadmin.events.EventSource;
import com.jhash.oimadmin.service.Service;
import com.jhash.oimadmin.ui.DisplayArea;
import com.jhash.oimadmin.ui.component.BaseComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.oim.code.OIMClientUI;
import com.jhash.oimadmin.ui.oim.connection.ConnectionTreeNode;
import com.jhash.oimadmin.ui.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.tree.DefaultMutableTreeNode;

public class ROOTAdminTreeNode implements Service, UIComponentTree.Node<DefaultMutableTreeNode> {

    private static final Logger logger = LoggerFactory.getLogger(ROOTAdminTreeNode.class);
    private final EventManager eventManager = new EventManager();
    private DisplayArea displayArea;
    private UIComponentTree selectionTree;
    private MenuHandler menuHandler;
    private Config config;
    private String name;
    private STATE state = Service.INITIALIZED_NO_OP;
    private DefaultMutableTreeNode treeNode;

    public ROOTAdminTreeNode(String name, Config config) {
        logger.trace("ROOTAdminTreeNode()");
        this.name = name;
        this.config = config;
        this.treeNode = new DefaultMutableTreeNode(this);
    }

    @Override
    public String toString() {
        return name;
    }

    public ROOTAdminTreeNode setDisplayArea(DisplayArea displayArea) {
        logger.trace("Setting Display Area for root node to {}", displayArea);
        this.displayArea = displayArea;
        return this;
    }

    public ROOTAdminTreeNode setUIComponentTree(UIComponentTree selectionTree) {
        logger.trace("Setting UIComponentTree for root node to {}", selectionTree);
        this.selectionTree = selectionTree;
        return this;
    }

    public ROOTAdminTreeNode setMenuHandler(MenuHandler menuHandler) {
        logger.trace("Setting Menu Handler for root node to {}", menuHandler);
        this.menuHandler = menuHandler;
        return this;
    }

    @Override
    public ROOTAdminTreeNode initialize() {
        for (String connectionName : config.getConnectionNames()) {
            Config.Configuration configuration = config.getConnectionDetails(connectionName);
            ConnectionTreeNode connectionTreeNode = new ConnectionTreeNode(connectionName, generateParentComponent(configuration));
            selectionTree.addChildNode(this, connectionTreeNode);
        }
        eventManager.registerEventListener(new VirtualNode<>(generateParentComponent(config.getConnectionDetails("")), OIMClientUI.class).initialize());
        eventManager.registerEventListener(new VirtualNode<>(generateParentComponent(config.getConnectionDetails("")), ConnectionTreeNode.class).initialize());
        return this;
    }

    public ParentComponent generateParentComponent(Config.Configuration configuration) {
        VirtualConnectionRootNode virtualConnectionRootNode = new VirtualConnectionRootNode(this, configuration);
        eventManager.registerEventListener(virtualConnectionRootNode);
        return virtualConnectionRootNode;
    }

    @Override
    public STATE getState() {
        return state;
    }

    @Override
    public DefaultMutableTreeNode getUIObject() {
        return treeNode;
    }

    @Override
    public void handleNodeEvent(UIComponentTree.EVENT_TYPE event_type) {
        logger.debug("Ignoring event {}", event_type);
    }

    @Override
    public void destroy() {
        logger.trace("Entering destroy()");
        try {
            eventManager.triggerEvent(null, DESTROY);
        } catch (Exception exception) {
            logger.warn("Failed to destroy child nodes of ROOT Node", exception);
        }
        logger.trace("Exiting destroy()");
    }

    private static class VirtualConnectionRootNode implements BaseComponent, ParentComponent, EventConsumer, EventSource, UIComponentTree.Node<DefaultMutableTreeNode> {

        private final ROOTAdminTreeNode rootAdminTreeNode;
        private final Config.Configuration configuration;
        private final EventManager eventManager = new EventManager();
        private final String stringRepresentation;

        public VirtualConnectionRootNode(ROOTAdminTreeNode rootAdminTreeNode, Config.Configuration configuration) {
            this.rootAdminTreeNode = rootAdminTreeNode;
            this.configuration = configuration;
            this.stringRepresentation = "VirtualConnectionRootNode(" + configuration + ")";
        }

        @Override
        public ParentComponent getParent() {
            return null;
        }

        @Override
        public Config.Configuration getConfiguration() {
            return configuration;
        }

        @Override
        public UIComponentTree getUIComponentTree() {
            return rootAdminTreeNode.selectionTree;
        }

        @Override
        public DisplayArea getDisplayArea() {
            return rootAdminTreeNode.displayArea;
        }

        @Override
        public MenuHandler getMenuHandler() {
            return rootAdminTreeNode.menuHandler;
        }

        @Override
        public void displayMessage(String title, String message, Exception exception) {
            UIUtils.displayMessage(title, message, exception);
        }

        @Override
        public void triggerEvent(EventSource parent, Event event) {
            eventManager.triggerEvent(this, event);
        }

        @Override
        public VirtualConnectionRootNode registerEventListener(EventConsumer consumer) {
            eventManager.registerEventListener(consumer);
            return this;
        }

        @Override
        public VirtualConnectionRootNode registerEventListener(Event event, EventConsumer consumer) {
            eventManager.registerEventListener(event, consumer);
            return this;
        }

        @Override
        public VirtualConnectionRootNode unRegisterEventListener(EventConsumer component) {
            eventManager.unRegisterEventListener(component);
            return this;
        }

        @Override
        public VirtualConnectionRootNode unRegisterEventListener(Event event, EventConsumer component) {
            eventManager.unRegisterEventListener(event, component);
            return this;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        @Override
        public DefaultMutableTreeNode getUIObject() {
            return rootAdminTreeNode.treeNode;
        }

        @Override
        public void handleNodeEvent(UIComponentTree.EVENT_TYPE event_type) {
            logger.debug("Ignoring event {}", event_type);
        }
    }
}
