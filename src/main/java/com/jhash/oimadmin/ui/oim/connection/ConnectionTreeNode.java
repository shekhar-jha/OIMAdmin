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

package com.jhash.oimadmin.ui.oim.connection;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.Connection;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.DBConnection;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.OIMConnection;
import com.jhash.oimadmin.oim.cache.CacheManager;
import com.jhash.oimadmin.oim.eventHandlers.Manager;
import com.jhash.oimadmin.oim.orch.OrchManager;
import com.jhash.oimadmin.oim.perf.PerfManager;
import com.jhash.oimadmin.oim.plugins.JarManager;
import com.jhash.oimadmin.oim.plugins.PluginManager;
import com.jhash.oimadmin.oim.request.RequestManager;
import com.jhash.oimadmin.ui.UIComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.componentTree.DisplayComponentNode;
import com.jhash.oimadmin.ui.componentTree.DummyAdminTreeNode;
import com.jhash.oimadmin.ui.componentTree.VirtualNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.oim.cache.OIMCacheNode;
import com.jhash.oimadmin.ui.oim.eventHandlers.EventHandlersTreeNode;
import com.jhash.oimadmin.ui.oim.mds.MDSTreeNode;
import com.jhash.oimadmin.ui.oim.orch.TraceOrchestrationDetails;
import com.jhash.oimadmin.ui.oim.perf.OIMPerformanceTreeNode;
import com.jhash.oimadmin.ui.oim.plugins.JarTreeNodes;
import com.jhash.oimadmin.ui.oim.request.TraceRequestDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ConnectionTreeNode extends AbstractUIComponentTreeNode<ConnectionTreeNode> implements VirtualNode.VirtualNodeComponent, UIComponent<ConnectionDetails> {

    public static final MenuHandler.MENU NEW_CONNECTION = new MenuHandler.MENU("Connection", MenuHandler.MENU.NEW);
    private static final Logger logger = LoggerFactory.getLogger(ConnectionTreeNode.class);
    private Connections connections;
    private ConnectionDetails connectionDetailsUI;

    public ConnectionTreeNode(String name, ParentComponent parent) {
        super(name, parent);
        registerMenu(REFRESH, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                logger.debug("Started Reconnect Trigger");
                Utils.executeAsyncOperation("Reconnecting Connection", new Runnable() {
                    @Override
                    public void run() {
                        ConnectionTreeNode.this.destroy(false);
                        ConnectionTreeNode.this.initialize();
                    }
                });
                logger.debug("Completed Reconnect Trigger");
            }
        });
        registerMenu(DELETE, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                logger.debug("Started Delete Trigger");
                ConnectionTreeNode.this.destroy();
                getConfiguration().getConfig().deleteConfiguration(getConfiguration().getProperty(Connection.ATTR_CONN_NAME));
                logger.debug("Completed Delete Trigger");
            }
        });
        // UI Component has to be present to be able to display it before node initialization.
        connectionDetailsUI = new ConnectionDetails(getName(), this, this);
    }

    public static void initializeNodeComponent(final VirtualNode virtualNode) {
        virtualNode.registerGlobalMenu(NEW_CONNECTION, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                logger.trace("Processing action on menu {} ", menuItem);
                new ConnectionDetails("New Connection...", virtualNode.getParentComponent()).initialize();
                logger.trace("Processed action on menu {} ", menuItem);
            }
        });
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing {} ...", this);
        this.connections = initializeConnections(getName(), getConfiguration());
        if (connections.isEmpty())
            return;
        if (connections.contains(CONNECTION_TYPES.JMX)) {
            new MDSTreeNode(connections.getConnection(CONNECTION_TYPES.JMX), "MDS Repository", this).publish();
        }
        Manager eventManager = null;
        if (connections.contains(CONNECTION_TYPES.JMX)) {
            eventManager = new Manager(connections.getConnection(CONNECTION_TYPES.JMX));
            PluginManager pluginManager = null;
            if (connections.contains(CONNECTION_TYPES.OIM))
                pluginManager = new PluginManager(connections.getConnection(CONNECTION_TYPES.OIM));
            new EventHandlersTreeNode(eventManager, pluginManager, "Event Handlers", this).publish();
        }
        new DummyAdminTreeNode("Scheduled Tasks", this).initialize();
        DummyAdminTreeNode cacheNode = new DummyAdminTreeNode("Cache", this);
        boolean cacheItemsAdded = false;
        if (connections.contains(CONNECTION_TYPES.JMX)) {
            CacheManager cacheManager = new CacheManager(connections.getConnection(CONNECTION_TYPES.JMX));
            new OIMCacheNode(cacheManager, connections.getConnection(CONNECTION_TYPES.OIM), "OIM Cache", cacheNode).initialize();
            cacheItemsAdded = true;
        }
        if (cacheItemsAdded)
            cacheNode.initialize();
        if (eventManager != null && connections.contains(CONNECTION_TYPES.JMX)) {
            PerfManager perfManager = new PerfManager(eventManager, connections.getConnection(CONNECTION_TYPES.JMX));
            new OIMPerformanceTreeNode(perfManager, "Performance", this).publish();
        }
        DummyAdminTreeNode trackerNode = new DummyAdminTreeNode("Track", this);
        boolean trackerItemAdded = false;
        OrchManager orchManager = null;
        if (connections.contains(CONNECTION_TYPES.OIM, CONNECTION_TYPES.JMX, CONNECTION_TYPES.DB)) {
            orchManager = new OrchManager(connections.getConnection(CONNECTION_TYPES.OIM), connections.getConnection(CONNECTION_TYPES.JMX), connections.getConnection(CONNECTION_TYPES.DB));
        }
        if (connections.contains(CONNECTION_TYPES.OIM)) {
            RequestManager requestManager = new RequestManager(connections.getConnection(CONNECTION_TYPES.OIM));
            new DisplayComponentNode<>("Request",
                    new TraceRequestDetails(requestManager, orchManager, "Request", this),
                    trackerNode).initialize();
            trackerItemAdded = true;
        }
        if (orchManager != null) {
            new DisplayComponentNode<>("Orchestration",
                    new TraceOrchestrationDetails(orchManager, "Orchestration", this),
                    trackerNode).initialize();
            trackerItemAdded = true;
        }
        if (trackerItemAdded)
            trackerNode.initialize();
        DummyAdminTreeNode codeNode = new DummyAdminTreeNode("Code", this);
        boolean codeItemAdded = false;
        if (connections.contains(CONNECTION_TYPES.OIM, CONNECTION_TYPES.DB)) {
            JarManager jarManager = new JarManager(connections.getConnection(CONNECTION_TYPES.OIM), connections.getConnection(CONNECTION_TYPES.DB));
            new JarTreeNodes(jarManager, "OIM Jar", codeNode).publish();
            codeItemAdded = true;
        }
        if (codeItemAdded)
            codeNode.initialize();
        logger.debug("Initialized {}", this);
    }

    private Connections initializeConnections(String name, Config.Configuration configuration) {
        Connections connections = new Connections();
        Config.Configuration connectionConfiguration = configuration.getConfig().getConnectionDetails(name);
        try {
            OIMConnection connection = new OIMConnection();
            logger.debug("Trying to initialize OIM Connection");
            connection.initialize(connectionConfiguration);
            logger.debug("Trying to initialize OIM Connection");
            connection.login();
            connections.connections.put(CONNECTION_TYPES.OIM, connection);
        } catch (Exception exception) {
            displayMessage("OIM Connection failed", "Failed to connect to OIM", exception);
        }
        try {
            JMXConnection tmpConnection = new JMXConnection();
            logger.debug("Trying to initialize JMX Connection.");
            tmpConnection.initialize(connectionConfiguration);
            connections.connections.put(CONNECTION_TYPES.JMX, tmpConnection);
        } catch (Exception exception) {
            displayMessage("JMX Connection failed", "Failed to connect to JMX", exception);
        }
        try {
            DBConnection tmpConnection = new DBConnection();
            logger.debug("Trying to initialize DB Connection");
            tmpConnection.initialize(connectionConfiguration);
            connections.connections.put(CONNECTION_TYPES.DB, tmpConnection);
        } catch (Exception exception) {
            displayMessage("Database Connection failed", "Failed to connect to Database", exception);
        }
        return connections;
    }

    @Override
    public ConnectionDetails getComponent() {
        return connectionDetailsUI;
    }

    public void refreshUI() {
        logger.debug("Refreshing Connection Details UI {}", this);
        if (connectionDetailsUI != null) {
            connectionDetailsUI.destroy();
            connectionDetailsUI = null;
        }
        logger.debug("Creating new Connection details UI for {}", getName());
        connectionDetailsUI = new ConnectionDetails(getName(), this, this);
        logger.debug("Refreshed Connection Details UI");
    }

    @Override
    public void destroyNode() {
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
            connectionDetailsUI = new ConnectionDetails(getName(), this, this);
        }
        logger.debug("Destroyed {}", this);
    }

    public static class CONNECTION_TYPES<T extends Connection> {

        public static final CONNECTION_TYPES<OIMConnection> OIM = new CONNECTION_TYPES<>(OIMConnection.class);
        public static final CONNECTION_TYPES<JMXConnection> JMX = new CONNECTION_TYPES<>(JMXConnection.class);
        public static final CONNECTION_TYPES<DBConnection> DB = new CONNECTION_TYPES<>(DBConnection.class);

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

        private Map<CONNECTION_TYPES, Connection> connections = new HashMap<>();

        public <T extends Connection> T getConnection(CONNECTION_TYPES<T> connectionType) {
            if (connectionType == null)
                return null;
            return connectionType.connectionClass.cast(connections.get(connectionType));
        }

        public boolean isEmpty() {
            return connections.isEmpty();
        }

        public boolean contains(CONNECTION_TYPES... connectionTypes) {
            if (connectionTypes == null)
                return false;
            for (CONNECTION_TYPES connectionType : connectionTypes) {
                if (!connections.containsKey(connectionType))
                    return false;
            }
            return true;
        }

    }
}
