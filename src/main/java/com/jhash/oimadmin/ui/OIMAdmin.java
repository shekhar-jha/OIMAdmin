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
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.OIMAdminTreeNode;
import com.jhash.oimadmin.OIMAdminTreeNode.NODE_STATE;
import com.jhash.oimadmin.UIComponent;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class OIMAdmin {

    public static final String DUMMY_LEAF_NODE_NAME = "Loading...";
    public static final String DUMMY_LEAF_NODE_NAME_ERROR = "Failed to load.";
    ;
    private static final Logger logger = LoggerFactory.getLogger(OIMAdmin.class);
    JMenuBar menuBar = new JMenuBar();
    private Config config = new Config();
    private JFrame mainWindow = new JFrame();
    private JTree connectionTree = null;
    private JideTabbedPane displayTabbedPane = null;
    private Map<STANDARD_MENUS, JMenu> standardMenus = new HashMap<STANDARD_MENUS, JMenu>();
    private Map<Class<? extends UIComponent>, UIComponent> oimUIComponents = new HashMap<>();

    public static void main(String[] args) {
        logger.debug("Entering Main...");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logger.debug("Creating the OIM Admin object...");
                OIMAdmin frame = new OIMAdmin();
                logger.debug("Initializing and displaying the OIM Admin object {}", frame);
                frame.initialize();
                logger.debug("Initialized OIM Admin object");
            }
        });
        logger.debug("Exiting Main.");
    }

    public void initialize() {
        logger.debug("Trying to validate whether application is running on Mac");
        if (System.getProperty("os.name").equals("Mac OS X")) {
            MacOSXHandler.initialize(this);
        }
        logger.debug("Trying to initialize OIM Admin. Loading configuration...");
        config.load(this);
        logger.debug("Configuration loaded.");
        try {
            logger.debug("Setting UI Manager look and feel to default look and feel for current system i.e. {}",
                    UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            logger.debug("Native look and feel set.");
        } catch (Exception exception) {
            logger.warn("Error occurred while setting the native look and feel. Ignoring the error.", exception);
        }
        logger.debug("Setting title, default close operation");
        mainWindow.setTitle("sysadmin++ 1.0");
        mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainWindow.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                destroy();
            }
        });


        // Create common menu items
        JMenu newMenu = new JMenu("New...");
        newMenu.setMnemonic('N');
        standardMenus.put(STANDARD_MENUS.NEW, newMenu);
        menuBar.add(newMenu);

        displayTabbedPane = getDisplayPane();
        JideTabbedPane selectTabbedPane = initializeSelectionPane();
        connectionTree = initializeSelectionTree();

        // Initialize the Connection UI.
        Connections connectionSetup = getUIComponent(Connections.class);
        connectionTree.expandPath(new TreePath(connectionTree.getModel().getRoot()));

        // Create the menu bar
        menuBar.setOpaque(true);
        menuBar.setPreferredSize(new Dimension(200, 20));

        selectTabbedPane.addTab("Server Instances", connectionTree);
        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        splitPane.add(new JideScrollPane(selectTabbedPane), 0);
        splitPane.add(displayTabbedPane, 1);
        splitPane.setProportionalLayout(true);
        splitPane.setProportions(new double[]{0.3});

        mainWindow.setJMenuBar(menuBar);
        mainWindow.getContentPane().add(splitPane);
        mainWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    private JideTabbedPane getDisplayPane() {
        // Setup the tabbed pane for displaying details
        JideTabbedPane displayTabbedPane = new JideTabbedPane();
        displayTabbedPane.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT);
        displayTabbedPane.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
        displayTabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);
        displayTabbedPane.setUseDefaultShowCloseButtonOnTab(false);
        displayTabbedPane.setBoldActiveTab(true);
        displayTabbedPane.setShowCloseButtonOnTab(true);
        return displayTabbedPane;
    }

    private JideTabbedPane initializeSelectionPane() {
        JideTabbedPane selectionTabbedPane = new JideTabbedPane();
        selectionTabbedPane.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT);
        selectionTabbedPane.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
        selectionTabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);
        selectionTabbedPane.setUseDefaultShowCloseButtonOnTab(false);
        selectionTabbedPane.setBoldActiveTab(true);
        selectionTabbedPane.setShowCloseButtonOnTab(false);
        // selectionTabbedPane.setTabColorProvider(JideTabbedPane.ONENOTE_COLOR_PROVIDER);
        return selectionTabbedPane;
    }

    private void processNodeEvent(Object operatedNodeObject, OIMAdminTreeNode.EVENT_TYPE evenType) {
        logger.debug("Processing Tree {} event on {}", new Object[]{evenType, operatedNodeObject});
        logger.debug("Trying to validate whether we have received identifiable node detail");
        if (operatedNodeObject != null && operatedNodeObject instanceof OIMAdminTreeNode) {
            OIMAdminTreeNode operatedNode = ((OIMAdminTreeNode) operatedNodeObject);
            operatedNode.handleEvent(evenType);
        } else {
            logger.warn(
                    "Failed to locate the actual node {} (Class: {}) being operated upon. Ignoring expansion event.",
                    operatedNodeObject, operatedNodeObject == null ? "null" : operatedNodeObject.getClass());
        }
        logger.debug("Processed Tree {} event on {} for {}", new Object[]{evenType, operatedNodeObject});
    }


    private JTree initializeSelectionTree() {
        OIMAdminTreeNode root = new OIMAdminTreeNode.OIMAdminTreeNodeNoAction("Connections", OIMAdminTreeNode.NODE_TYPE.ROOT,
                config.getConnectionDetails(""));
        JTree localConnectionTree = new JTree(root);
        localConnectionTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                if (event != null && event.getPath() != null && event.getPath().getLastPathComponent() != null) {
                    Object operatedNodeObject = event.getPath().getLastPathComponent();
                    processNodeEvent(operatedNodeObject, OIMAdminTreeNode.EVENT_TYPE.NODE_EXPAND);
                } else {
                    logger.warn(
                            "Failed to identify the specific location of the tree being operated upon. Event received {}, Path: {}, Source: {}",
                            new Object[]{event, (event == null) ? "null" : event.getPath(),
                                    (event == null) ? "null" : event.getSource()});
                }

            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                logger.debug("Ignoring tree collapsed event ");
            }
        });
        localConnectionTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                logger.debug("Starting handling of the mouse pressed event {}", event);
                int selRow = localConnectionTree.getRowForLocation(event.getX(), event.getY());
                TreePath selPath = localConnectionTree.getPathForLocation(event.getX(), event.getY());
                if (selRow != -1) {
                    logger.debug("Trying to check whether number of clicks {} is 2 i.e. it is a double click event",
                            event.getClickCount());
                    if (event.getClickCount() == 2) {
                        Object clickedNodeObject = selPath.getLastPathComponent();
                        OIMAdminTreeNode clickedNode;
                        NODE_STATE status = null;
                        logger.debug("Trying to validate whether we have received identifiable node detail");
                        if (clickedNodeObject != null
                                && clickedNodeObject instanceof OIMAdminTreeNode
                                && ((status = (clickedNode = (OIMAdminTreeNode) clickedNodeObject).getStatus()) == NODE_STATE.INITIALIZED || status == NODE_STATE.FAILED)) {
                            processNodeEvent(clickedNode, OIMAdminTreeNode.EVENT_TYPE.NODE_DISPLAY);
                        } else {
                            logger.debug("Failed to locate identifiable node {} in status {}", clickedNodeObject,
                                    status);
                        }
                    } else {
                        logger.trace("The number of clicks {} is not 2 i.e. double click. Ignoring event",
                                event.getClickCount());
                    }
                } else {
                    logger.trace("The event occurred at a location that did not correspond to a row. Ignoring the event");
                }
                logger.debug("Completed handling of the event {}", event);
            }
        });
        return localConnectionTree;
    }

    public <T extends UIComponent> T getUIComponent(Class<T> componentId) {
        if (!oimUIComponents.containsKey(componentId)) {
            try {
                UIComponent uiComponent = componentId.newInstance();
                uiComponent.initialize(config);
                if (uiComponent instanceof RegisterUI) {
                    ((RegisterUI) uiComponent).registerMenu(menuBar, standardMenus, displayTabbedPane);
                    ((RegisterUI) uiComponent).registerSelectionTree(connectionTree, displayTabbedPane);
                }
                oimUIComponents.put(componentId, uiComponent);
            } catch (Exception e) {
                throw new OIMAdminException("Failed to create new instance of class " + componentId + " and register it", e);
            }
        }
        return (T) oimUIComponents.get(componentId);
    }

    public void destroy() {
        logger.debug("Trying to destroy OIMAdmin");
        if (oimUIComponents != null && !oimUIComponents.isEmpty()) {
            for (Class<? extends UIComponent> componentClass : oimUIComponents.keySet()) {
                try {
                    oimUIComponents.get(componentClass).destroy();
                } catch (Exception e) {
                    logger.warn("Failed to destroy the component " + componentClass, e);
                }
            }
            oimUIComponents.clear();
        }
        TreeModel treeModel;
        Object root;
        if (this.connectionTree != null && (treeModel = connectionTree.getModel()) != null
                && (root = treeModel.getRoot()) != null && (root instanceof OIMAdminTreeNode)) {
            ((OIMAdminTreeNode) root).removeAllChildren();
        }
        connectionTree = null;
        logger.debug("Destroyed OIMAdmin");
    }

    public static enum STANDARD_MENUS {
        NEW
    }


    public static interface RegisterUI {
        public void registerMenu(JMenuBar menu, Map<STANDARD_MENUS, JMenu> commonMenus, JTabbedPane displayArea);

        public void registerSelectionTree(JTree selectionTree, JTabbedPane displayArea);
    }

}
