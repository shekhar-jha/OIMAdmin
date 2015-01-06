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
import com.jhash.oimadmin.OIMAdminTreeNode.NODE_STATE;
import com.jhash.oimadmin.UIComponent;
import com.jhash.oimadmin.UIComponentTree;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

public class OIMAdmin extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(OIMAdmin.class);
    private Config config = new Config();
    private UIComponentTreeImpl componentTree = null;
    private DisplayAreaImpl displayArea = null;

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
        logger.debug("Loading configuration...");
        config.load();
        logger.debug("Configuration loaded.");
        try {
            logger.debug("Setting UI Manager look and feel to default look and feel for current system i.e. {}",
                    UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            logger.debug("Native look and feel set.");
        } catch (Exception exception) {
            logger.warn("Error occurred while setting the native look and feel. Ignoring the error.", exception);
        }
        logger.debug("Setting title and default close operation");
        setTitle("sysadmin++");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                logger.debug("Started WindowClosed trigger processing...");
                destroy();
                logger.debug("Processed WindowClosed trigger");
            }
        });


        // Create common menu items
        JMenu newMenu = new JMenu("New...");
        newMenu.setMnemonic('N');
        Map<STANDARD_MENUS, JMenu> standardMenus = new HashMap<STANDARD_MENUS, JMenu>();
        standardMenus.put(STANDARD_MENUS.NEW, newMenu);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(newMenu);

        // Initialize all the UI Components
        displayArea = initializeDisplayPane();
        JideTabbedPane selectTabbedPane = initializeSelectionPane();
        componentTree = initializeSelectionTree();

        // Initialize the Connection UI.
        ConnectionTreeNode.ConnectionsRegisterUI connectionsUI = new ConnectionTreeNode.ConnectionsRegisterUI();
        connectionsUI.registerMenu(config, menuBar, standardMenus, componentTree, displayArea);
        connectionsUI.registerSelectionTree(config, componentTree, displayArea);
        componentTree.connectionTree.expandPath(new TreePath(componentTree.connectionTree.getModel().getRoot()));

        // Create the menu bar
        menuBar.setOpaque(true);
        menuBar.setPreferredSize(new java.awt.Dimension(200, 20));

        selectTabbedPane.addTab("Server Instances", componentTree.connectionTree);
        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        splitPane.add(new JideScrollPane(selectTabbedPane), 0);
        splitPane.add(displayArea.displayArea, 1);
        splitPane.setProportionalLayout(true);
        splitPane.setProportions(new double[]{0.3});

        setJMenuBar(menuBar);
        getContentPane().add(splitPane);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        pack();
        setVisible(true);
    }

    private DisplayAreaImpl initializeDisplayPane() {
        // Setup the tabbed pane for displaying details
        JideTabbedPane displayTabbedPane = new JideTabbedPane();
        displayTabbedPane.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT);
        displayTabbedPane.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
        displayTabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);
        displayTabbedPane.setUseDefaultShowCloseButtonOnTab(false);
        displayTabbedPane.setBoldActiveTab(true);
        displayTabbedPane.setShowCloseButtonOnTab(true);
        return new DisplayAreaImpl(displayTabbedPane);
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


    private UIComponentTreeImpl initializeSelectionTree() {
        AbstractUIComponentTreeNode.ROOTAdminTreeNode root = new AbstractUIComponentTreeNode.ROOTAdminTreeNode("ConnectionTreeNode");
        JTree localConnectionTree = new JTree(root);
        localConnectionTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                logger.trace("Processing tree ({}) expansion event {}", localConnectionTree, event);
                try {
                    if (event != null && event.getPath() != null && event.getPath().getLastPathComponent() != null) {
                        OIMAdminTreeNode operatedNodeObject = (OIMAdminTreeNode) event.getPath().getLastPathComponent();
                        operatedNodeObject.handleEvent(OIMAdminTreeNode.EVENT_TYPE.NODE_EXPAND);
                    } else {
                        logger.warn(
                                "Failed to identify the specific location of the tree being operated upon. Event received {}, Path: {}, Source: {}",
                                new Object[]{event, (event == null) ? "null" : event.getPath(),
                                        (event == null) ? "null" : event.getSource()});
                    }
                    logger.trace("Processed tree expansion event", event);
                } catch (Exception exception) {
                    logger.warn("Failed to process tree expansion event {} on tree {}", new Object[]{event, localConnectionTree}, exception);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                logger.trace("Ignoring tree collapsed event {} on tree {}", event, localConnectionTree);
            }
        });
        localConnectionTree.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent event) {
                logger.trace("Processing mouse pressed event {} on tree {}", event, localConnectionTree);
                try {
                    int selRow = localConnectionTree.getRowForLocation(event.getX(), event.getY());
                    TreePath selPath = localConnectionTree.getPathForLocation(event.getX(), event.getY());
                    if (selRow != -1) {
                        logger.trace("Trying to check whether number of clicks {} is 2 i.e. it is a double click event",
                                event.getClickCount());
                        if (event.getClickCount() == 2) {
                            Object clickedNodeObject = selPath.getLastPathComponent();
                            NODE_STATE status = null;
                            logger.trace("Trying to validate whether we have received identifiable node detail");
                            if (clickedNodeObject != null && clickedNodeObject instanceof OIMAdminTreeNode) {
                                ((OIMAdminTreeNode) clickedNodeObject).handleEvent(OIMAdminTreeNode.EVENT_TYPE.NODE_DISPLAY);
                            } else {
                                logger.trace("Failed to locate OIMAdminTreeNode node {} that was clicked", clickedNodeObject);
                            }
                        } else {
                            logger.trace("The number of clicks {} is not 2 i.e. double click. Ignoring event",
                                    event.getClickCount());
                        }
                    } else {
                        logger.trace("The event occurred at a location that did not correspond to a row. Ignoring the event");
                    }
                    logger.trace("Processed mouse pressed event {} on tree {}", event, localConnectionTree);
                } catch (Exception exception) {
                    logger.warn("Failed to process mouse pressed event {} on tree {}", new Object[]{event, localConnectionTree}, exception);
                }
            }
        });
        UIComponentTreeImpl localComponentTree = new UIComponentTreeImpl(localConnectionTree);
        root.setUIComponentTree(localComponentTree);
        return localComponentTree;
    }

    public void destroy() {
        logger.debug("Trying to destroy OIMAdmin");
        ((AbstractUIComponentTreeNode.ROOTAdminTreeNode) componentTree.getRootNode()).destroy();
        componentTree = null;
        //TODO: Destroy displayArea
        logger.debug("Destroyed OIMAdmin");
    }

    public static enum STANDARD_MENUS {
        NEW
    }


    public static class UIComponentTreeImpl implements UIComponentTree {

        private Logger logger = LoggerFactory.getLogger(UIComponentTreeImpl.class);
        private JTree connectionTree;
        private DefaultTreeModel model;

        public UIComponentTreeImpl(JTree connectionTree) {
            this.connectionTree = connectionTree;
            this.model = (DefaultTreeModel) connectionTree.getModel();
        }

        public void addChildNode(OIMAdminTreeNode parent, OIMAdminTreeNode child) {
            model.insertNodeInto(child, parent, parent.getChildCount());
        }

        public List<OIMAdminTreeNode> getChildNodes(OIMAdminTreeNode parent) {
            List<OIMAdminTreeNode> childNodes = new ArrayList<>();
            int numberOfChildNodes = model.getChildCount(parent);
            for (int counter = 0; counter < numberOfChildNodes; counter++) {
                Object nodeObject = model.getChild(parent, counter);
                if (nodeObject == null)
                    throw new NullPointerException("Located a null node as child of node " + parent + " at index " + counter);
                if (nodeObject instanceof OIMAdminTreeNode) {
                    childNodes.add((OIMAdminTreeNode) nodeObject);
                } else {
                    logger.warn("Located a node {} of type {} instead of OIMAdminTreeNode while enumerating child nodes of {}. Ignoring the node", new Object[]{nodeObject, nodeObject.getClass(), parent});
                }
            }
            return childNodes;
        }

        public void removeChildNode(OIMAdminTreeNode parent, OIMAdminTreeNode child) {
            model.removeNodeFromParent(child);
        }

        public OIMAdminTreeNode getRootNode() {
            Object nodeObject = model.getRoot();
            if (nodeObject == null)
                throw new NullPointerException("Located a null root node of tree " + connectionTree);
            if (nodeObject instanceof OIMAdminTreeNode) {
                return ((OIMAdminTreeNode) nodeObject);
            } else {
                throw new ClassCastException("Expected class " + OIMAdminTreeNode.class + " but found class " + nodeObject.getClass() + " for node " + nodeObject);
            }
        }

    }

    public static class DisplayAreaImpl implements DisplayArea {

        private JTabbedPane displayArea;
        private Set<UIComponent<? extends JComponent>> addedComponents;

        public DisplayAreaImpl(JTabbedPane displayArea) {
            this.displayArea = displayArea;
        }

        public void add(UIComponent<? extends JComponent> component) {
            if (addedComponents.contains(component)) {
                displayArea.setSelectedComponent(component.getComponent());
            } else {
                displayArea.addTab(component.getName(), component.getComponent());
                addedComponents.add(component);
            }
        }

        @Override
        public void remove(UIComponent<? extends JComponent> component) {
            displayArea.remove(component.getComponent());
            addedComponents.remove(component);
        }

    }
}