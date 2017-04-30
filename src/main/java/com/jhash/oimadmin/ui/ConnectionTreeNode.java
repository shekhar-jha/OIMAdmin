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

import com.jhash.oimadmin.*;
import com.jhash.oimadmin.oim.OIMConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class ConnectionTreeNode extends AbstractUIComponentTreeNode<OIMConnection> implements DisplayableNode<ConnectionDetails>, ContextMenuEnabledNode {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTreeNode.class);
    private OIMConnection connection;
    private ConnectionDetails connectionDetailsUI;
    private JPopupMenu popupMenu;
    private JMenuItem refreshMenu;
    private JMenuItem deleteConnectionMenuItem;

    public ConnectionTreeNode(String name, final Config.Configuration configuration, final UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        connectionDetailsUI = new ConnectionDetails(name, configuration, this, selectionTree, displayArea);
        refreshMenu = new JMenuItem("Reconnect");
        refreshMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("Started Reconnect Trigger");
                Utils.executeAsyncOperation("Reconnecting Connection", new Runnable() {
                    @Override
                    public void run() {
                        ConnectionTreeNode.this.destroy();
                        ConnectionTreeNode.this.initialize();
                    }
                });
                logger.debug("Completed Reconnect Trigger");
            }
        });
        deleteConnectionMenuItem = new JMenuItem("Delete");
        deleteConnectionMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("Started Delete Trigger");
                ConnectionTreeNode.this.destroy();
                configuration.getConfig().deleteConfiguration(configuration.getProperty(Connection.ATTR_CONN_NAME));
                selectionTree.removeChildNode((OIMAdminTreeNode) getParent(), ConnectionTreeNode.this);
                logger.debug("Completed Delete Trigger");
            }
        });
        popupMenu = new JPopupMenu();
        popupMenu.add(refreshMenu);
        popupMenu.add(deleteConnectionMenuItem);
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {} ...", this);
        connection = new OIMConnection();
        connection.initialize(configuration.getConfig().getConnectionDetails(name));
        connection.login();
        selectionTree.addChildNode(this, new MDSTreeNode("MDS Repository", configuration, selectionTree, displayArea));
        selectionTree.addChildNode(this, new EventHandlersTreeNode("Event Handlers", connection, configuration, selectionTree, displayArea));
        selectionTree.addChildNode(this, new OIMAdminTreeNode.OIMAdminTreeNodeNoAction("Scheduled Tasks", this, selectionTree));
        DummyAdminTreeNode cacheNode = new DummyAdminTreeNode("Cache", configuration, selectionTree, displayArea);
        selectionTree.addChildNode(this, cacheNode);
        selectionTree.addChildNode(cacheNode, new OIMCacheNode("OIM Cache", connection, configuration, selectionTree, displayArea).initialize());
        selectionTree.addChildNode(this, new OIMPerformanceTreeNode("Performance", configuration, selectionTree, displayArea));
        DummyAdminTreeNode trackerNode = new DummyAdminTreeNode("Track", configuration, selectionTree, displayArea);
        selectionTree.addChildNode(trackerNode, new DisplayComponentNode<>("Request", new TraceRequestDetails("Request", connection, configuration, selectionTree, displayArea
        ), null, configuration, selectionTree, displayArea).initialize());
        selectionTree.addChildNode(trackerNode, new DisplayComponentNode<>("Orchestration", new TraceOrchestrationDetails("Orchestration", connection, configuration, selectionTree, displayArea
        ), null, configuration, selectionTree, displayArea).initialize());
        selectionTree.addChildNode(this, trackerNode);
        logger.debug("Initialized {}", this);
    }

    @Override
    public OIMConnection getComponent() {
        return connection;
    }

    @Override
    public boolean isDisplayable() {
        return true;
    }

    @Override
    public ConnectionDetails getDisplayComponent() {
        return connectionDetailsUI;
    }

    @Override
    public boolean hasContextMenu() {
        return true;
    }

    @Override
    public JPopupMenu getContextMenu() {
        return popupMenu;
    }

    public void refreshUI() {
        logger.debug("Refreshing Connection Details UI {}", this);
        connectionDetailsUI = new ConnectionDetails(name, configuration.getConfig().getConnectionDetails(name), this, selectionTree, displayArea);
        logger.debug("Refreshed Connection Details UI");
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying {} ...", this);
        if (connection != null) {
            try {
                logger.debug("Trying to destroy connection {}", connection);
                connection.destroy();
                logger.debug("Destroyed connection {}", connection);
            } catch (Exception exception) {
                logger.warn("Failed to destroy connection " + connection + ". Ignoring error.", exception);
            }
            connection = null;
        }
        if (connectionDetailsUI != null) {
            connectionDetailsUI.destroy();
            logger.debug("Initializing a new Connection Detail UI");
            connectionDetailsUI = new ConnectionDetails(name, configuration.getConfig().getConnectionDetails(name), this, selectionTree, displayArea);
        }
        logger.debug("Destroyed {}", this);
    }

    public static class ConnectionsRegisterUI implements RegisterUI {

        public static void addNewNode(String oimConnectionName, Config configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
            OIMAdminTreeNode rootNode = selectionTree.getRootNode();
            selectionTree.addChildNode(rootNode, new ConnectionTreeNode(oimConnectionName, configuration.getConnectionDetails(oimConnectionName), selectionTree, displayArea));
        }

        @Override
        public void registerMenu(final Config configuration, JMenuBar menu, Map<OIMAdmin.STANDARD_MENUS, JMenu> commonMenus, final UIComponentTree selectionTree, final DisplayArea displayArea) {
            if (commonMenus != null && commonMenus.containsKey(OIMAdmin.STANDARD_MENUS.NEW)) {
                final JMenuItem newConnectionMenuItem = new JMenuItem("Connection");
                newConnectionMenuItem.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logger.trace("Processing action on menu {} ", newConnectionMenuItem);
                        ConnectionDetails connectionDetailUI = new ConnectionDetails("New Connection...", configuration, selectionTree, displayArea);
                        connectionDetailUI.initialize();
                        displayArea.add(connectionDetailUI);
                        logger.trace("Processed action on menu {} ", newConnectionMenuItem);
                    }
                });
                commonMenus.get(OIMAdmin.STANDARD_MENUS.NEW).add(newConnectionMenuItem);
            }
        }

        @Override
        public void registerSelectionTree(Config configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
            OIMAdminTreeNode rootNode = selectionTree.getRootNode();
            if (rootNode == null)
                throw new NullPointerException("Failed to locate the root node for selection tree. Can not add any connections.");
            for (String oimConnectionName : configuration.getConnectionNames()) {
                logger.debug("Adding Node for connection {}", oimConnectionName);
                addNewNode(oimConnectionName, configuration, selectionTree, displayArea);
            }
        }

    }
}
