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
import com.jhash.oimadmin.oim.DBConnection;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.OIMConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class ConnectionTreeNode extends AbstractUIComponentTreeNode<ConnectionTreeNode.Connections> implements DisplayableNode<ConnectionDetails>, ContextMenuEnabledNode {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTreeNode.class);
    private Connections connections;
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
        initializeConnections();
        selectionTree.addChildNode(this, new MDSTreeNode(connections, "MDS Repository", configuration, selectionTree, displayArea));
        selectionTree.addChildNode(this, new EventHandlersTreeNode("Event Handlers", connections.<OIMConnection>getConnection(CONNECTION_TYPES.OIM), configuration, selectionTree, displayArea));
        selectionTree.addChildNode(this, new OIMAdminTreeNode.OIMAdminTreeNodeNoAction("Scheduled Tasks", this, selectionTree));
        DummyAdminTreeNode cacheNode = new DummyAdminTreeNode("Cache", configuration, selectionTree, displayArea);
        selectionTree.addChildNode(this, cacheNode);
        selectionTree.addChildNode(cacheNode, new OIMCacheNode("OIM Cache", connections.<OIMConnection>getConnection(CONNECTION_TYPES.OIM), configuration, selectionTree, displayArea).initialize());
        selectionTree.addChildNode(this, new OIMPerformanceTreeNode("Performance", configuration, selectionTree, displayArea));
        DummyAdminTreeNode trackerNode = new DummyAdminTreeNode("Track", configuration, selectionTree, displayArea);
        selectionTree.addChildNode(trackerNode, new DisplayComponentNode<>("Request", new TraceRequestDetails("Request", connections.<OIMConnection>getConnection(CONNECTION_TYPES.OIM), configuration, selectionTree, displayArea
        ), null, configuration, selectionTree, displayArea).initialize());
        selectionTree.addChildNode(trackerNode, new DisplayComponentNode<>("Orchestration", new TraceOrchestrationDetails("Orchestration", connections.<OIMConnection>getConnection(CONNECTION_TYPES.OIM), configuration, selectionTree, displayArea
        ), null, configuration, selectionTree, displayArea).initialize());
        selectionTree.addChildNode(this, trackerNode);
        logger.debug("Initialized {}", this);
    }

    private Connections initializeConnections() {
        Connections connections = new Connections();
        Exception failedConnection = null;
        Config.Configuration connectionConfiguration = configuration.getConfig().getConnectionDetails(name);
        try {
            OIMConnection connection = new OIMConnection();
            logger.debug("Trying to initialize OIM Connection");
            connection.initialize(connectionConfiguration);
            logger.debug("Trying to initialize OIM Connection");
            connection.login();
            connections.connections.put(CONNECTION_TYPES.OIM, connection);
        } catch (Exception exception) {
            failedConnection = exception;
        }
        try {
            JMXConnection tmpConnection = new JMXConnection();
            logger.debug("Trying to initialize JMX Connection.");
            tmpConnection.initialize(connectionConfiguration);
            connections.connections.put(CONNECTION_TYPES.JMX, tmpConnection);
        } catch (Exception exception) {
            failedConnection = exception;
        }
        try {
            DBConnection tmpConnection = new DBConnection();
            logger.debug("Trying to initialize DB Connection");
            tmpConnection.initialize(connectionConfiguration);
            connections.connections.put(CONNECTION_TYPES.DB, tmpConnection);
        } catch (Exception exception) {
            failedConnection = exception;
        }
        this.connections = connections;
        if (failedConnection != null)
            throw new OIMAdminException("Failed to initialize connections", failedConnection);
        return connections;
    }

    @Override
    public Connections getComponent() {
        return connections;
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
        if (connections != null) {
            for (CONNECTION_TYPES connectionType : connections.connections.keySet()) {
                try {
                    Connection connection = connections.connections.get(connectionType);
                    if (connection != null) {
                        logger.debug("Trying to destroy connections {}", connection);
                        connection.destroy();
                        logger.debug("Destroyed connections {}", connection);
                    }
                } catch (Exception exception) {
                    logger.warn("Failed to destroy connections " + connections + ". Ignoring error.", exception);
                }
            }
            connections = null;
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
                logger.debug("Adding Node for connections {}", oimConnectionName);
                addNewNode(oimConnectionName, configuration, selectionTree, displayArea);
            }
        }

    }

    public static class CONNECTION_TYPES<T extends Connection> {

        public static final CONNECTION_TYPES OIM = new CONNECTION_TYPES(OIMConnection.class);
        public static final CONNECTION_TYPES JMX = new CONNECTION_TYPES(JMXConnection.class);
        public static final CONNECTION_TYPES DB = new CONNECTION_TYPES(DBConnection.class);

        private final Class<T> connectionClass;

        CONNECTION_TYPES(Class<T> connectionClass) {
            this.connectionClass = connectionClass;
        }

        public static CONNECTION_TYPES[] values() {
            return new CONNECTION_TYPES[]{OIM, JMX, DB};
        }

        public Class<T> getConnectionClass() {
            return connectionClass;
        }
    }

    public static class Connections {

        private Map<CONNECTION_TYPES, Connection> connections;

        public <T extends Connection> T getConnection(CONNECTION_TYPES<T> connectionType) {
            if (connectionType == null)
                return null;
            return connectionType.connectionClass.cast(connections.get(connectionType));
        }

    }
}
