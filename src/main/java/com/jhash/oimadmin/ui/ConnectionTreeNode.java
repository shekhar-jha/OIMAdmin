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
import com.jhash.oimadmin.OIMAdminTreeNode;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.OIMConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class ConnectionTreeNode extends AbstractUIComponentTreeNode<OIMConnection> implements DisplayableNode<ConnectionDetails>, ContextMenuEnabledNode{

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTreeNode.class);
    private OIMConnection connection;
    private ConnectionDetails connectionDetailsUI;
    private JPopupMenu popupMenu;
    private JMenuItem refreshMenu;

    public ConnectionTreeNode(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        connectionDetailsUI = new ConnectionDetails(name, configuration, selectionTree, displayArea);
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
        popupMenu = new JPopupMenu();
        popupMenu.add(refreshMenu);
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {} ...", this);
        connection = new OIMConnection();
        connection.initialize(configuration);
        connection.login();
        selectionTree.addChildNode(this, new MDSTreeNode("MDS Repository", configuration, selectionTree, displayArea));
        selectionTree.addChildNode(this, new EventHandlersTreeNode("Event Handlers", connection, configuration, selectionTree, displayArea));
        selectionTree.addChildNode(this, new OIMAdminTreeNode.OIMAdminTreeNodeNoAction("Scheduled Tasks", this, selectionTree));
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
            connectionDetailsUI = new ConnectionDetails(name, configuration, selectionTree, displayArea);
        }
        logger.debug("Destroyed {}", this);
    }

    public static class ConnectionsRegisterUI implements RegisterUI {

        @Override
        public void registerMenu(Config configuration, JMenuBar menu, Map<OIMAdmin.STANDARD_MENUS, JMenu> commonMenus, UIComponentTree selectionTree, DisplayArea displayArea) {
            if (commonMenus != null && commonMenus.containsKey(OIMAdmin.STANDARD_MENUS.NEW)) {
                JMenuItem newConnectionMenuItem = new JMenuItem("Connection");
                newConnectionMenuItem.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logger.trace("Processing action on menu {} ", newConnectionMenuItem);
                        ConnectionDetails connectionDetailUI = new ConnectionDetails("New Connection...",configuration, selectionTree, displayArea);
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
                selectionTree.addChildNode(rootNode, new ConnectionTreeNode(oimConnectionName, configuration.getConnectionDetails(oimConnectionName), selectionTree, displayArea));
            }
        }

    }
}
