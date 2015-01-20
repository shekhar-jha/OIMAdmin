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
import java.awt.event.*;
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

        OIMClientUI.OIMClientRegisterUI clientUI = new OIMClientUI.OIMClientRegisterUI();
        clientUI.registerMenu(config, menuBar, standardMenus, componentTree, displayArea);
        clientUI.registerSelectionTree(config, componentTree, displayArea);

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
        return new DisplayAreaImpl();
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
                    logger.warn("Failed to process tree expansion event " + event + " on tree " + localConnectionTree, exception);
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
                        if (event.getClickCount() >= 2) {
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
                            logger.trace("Trying to check whether the event is a popup menu event");
                            if (event.isPopupTrigger()) {
                                Object clickedNodeObject = selPath.getLastPathComponent();
                                NODE_STATE status = null;
                                logger.trace("Trying to validate whether we have received identifiable node detail");
                                if (clickedNodeObject != null && clickedNodeObject instanceof OIMAdminTreeNode) {
                                    if (clickedNodeObject instanceof ContextMenuEnabledNode) {
                                        ContextMenuEnabledNode menuEnabledNode = (ContextMenuEnabledNode) clickedNodeObject;
                                        if (menuEnabledNode.hasContextMenu()) {
                                            JPopupMenu popupMenu = menuEnabledNode.getContextMenu();
                                            if (popupMenu != null) {
                                                popupMenu.show(event.getComponent(), event.getX(), event.getY());
                                            }
                                        }
                                    }
                                } else {
                                    logger.trace("Failed to locate OIMAdminTreeNode node {} that was clicked", clickedNodeObject);
                                }

                            } else {
                                logger.trace("No popup event was detected. Ignoring event");
                            }
                        }
                    } else {
                        logger.trace("The event occurred at a location that did not correspond to a row. Ignoring the event");
                    }
                    logger.trace("Processed mouse pressed event {} on tree {}", event, localConnectionTree);
                } catch (Exception exception) {
                    logger.warn("Failed to process mouse pressed event " + event + " on tree " + localConnectionTree, exception);
                }
            }

            public void mouseReleased(MouseEvent event) {
                logger.trace("Processing mouse pressed event {} on tree {}", event, localConnectionTree);
                try {
                    logger.trace("Trying to locate the row of tree on which event occurred.");
                    int selRow = localConnectionTree.getRowForLocation(event.getX(), event.getY());
                    if (selRow != -1) {
                        logger.trace("Trying to locate the node on which event occurred");
                        TreePath selPath = localConnectionTree.getPathForLocation(event.getX(), event.getY());
                        logger.trace("Validating whether event is a popup trigger");
                        if (event.isPopupTrigger()) {
                            Object clickedNodeObject = selPath.getLastPathComponent();
                            logger.trace("Trying to validate whether we have received identifiable node detail");
                            if (clickedNodeObject != null && clickedNodeObject instanceof OIMAdminTreeNode) {
                                if (clickedNodeObject instanceof ContextMenuEnabledNode) {
                                    ContextMenuEnabledNode menuEnabledNode = (ContextMenuEnabledNode) clickedNodeObject;
                                    if (menuEnabledNode.hasContextMenu()) {
                                        JPopupMenu popupMenu = menuEnabledNode.getContextMenu();
                                        if (popupMenu != null) {
                                            popupMenu.show(event.getComponent(), event.getX(), event.getY());
                                        }
                                    }
                                }
                            } else {
                                logger.trace("Failed to locate OIMAdminTreeNode node {} that was clicked", clickedNodeObject);
                            }
                        } else {
                            logger.trace("No popup event was detected. Ignoring event");
                        }
                    } else {
                        logger.trace("The event occurred at a location that did not correspond to a row. Ignoring the event");
                    }
                    logger.trace("Processed mouse pressed event {} on tree {}", event, localConnectionTree);
                } catch (Exception exception) {
                    logger.warn("Failed to process mouse pressed event " + event + " on tree " + localConnectionTree, exception);
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
        displayArea.destroy();
        displayArea = null;
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

        @Override
        public void addChildNode(OIMAdminTreeNode parent, OIMAdminTreeNode child) {
            model.insertNodeInto(child, parent, parent.getChildCount());
        }

        @Override
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

        @Override
        public void removeChildNode(OIMAdminTreeNode parent, OIMAdminTreeNode child) {
            model.removeNodeFromParent(child);
        }

        @Override
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

        private static final Logger logger = LoggerFactory.getLogger(DisplayAreaImpl.class);
        private JideTabbedPane displayArea;
        private Map<JComponent, UIComponent<? extends JComponent>> uiComponentToObjectMap = new HashMap<>();
        private Map<UIComponent<? extends JComponent>, JComponent> objectToUIComponentMap = new HashMap<>();

        public DisplayAreaImpl() {
            // Setup the tabbed pane for displaying details
            displayArea = new JideTabbedPane();
            displayArea.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT);
            displayArea.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
            displayArea.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);
            displayArea.setUseDefaultShowCloseButtonOnTab(false);
            displayArea.setBoldActiveTab(true);
            displayArea.setShowCloseButtonOnTab(true);
            displayArea.setCloseAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object source = e.getSource();
                    logger.trace("Triggering close action on tab {}...", source);
                    if (uiComponentToObjectMap.containsKey(source)) {
                        UIComponent<? extends JComponent> uiComponentObject = uiComponentToObjectMap.get(source);
                        if (uiComponentObject instanceof AbstractUIComponent) {
                            if (((AbstractUIComponent) uiComponentObject).destroyComponentOnClose()) {
                                ((AbstractUIComponent) uiComponentObject).destroy();
                            } else {
                                logger.trace("Component should not be destroyed on close. Just removing the component from display");
                                displayArea.remove((JComponent) source);
                            }
                        } else {
                            logger.trace("Even though expected, the object {} is not an instance of {}", uiComponentObject, AbstractUIComponent.class);
                        }
                    } else {
                        logger.trace("Could not locate UI component in {}", uiComponentToObjectMap);
                    }
                    logger.trace("Triggered close action on tab {}", source);
                }
            });

        }

        @Override
        public void add(UIComponent<? extends JComponent> component) {
            if (component != null) {
                String name = component.getName();
                if (objectToUIComponentMap.containsKey(component)) {
                    JComponent uiComponent = objectToUIComponentMap.get(component);
                    logger.trace("Component {} is known to display area. Validating if the associated UI Component {} is being displayed", component, uiComponent);
                    if (displayArea.indexOfComponent(uiComponent) == -1) {
                        logger.trace("Adding the component {} with name {}", uiComponent, name);
                        displayArea.addTab(name, uiComponent);
                        displayArea.setSelectedComponent(uiComponent);
                    } else {
                        logger.trace("Component already present in tabbed pane, activating it ");
                        displayArea.setSelectedComponent(uiComponent);
                    }
                } else {
                    JComponent uiComponent = component.getComponent();
                    logger.trace("Adding the new component {} with name {}", uiComponent, name);
                    displayArea.addTab(name, uiComponent);
                    displayArea.setSelectedComponent(uiComponent);
                    logger.trace("Adding component to set of displayed components {}", objectToUIComponentMap);
                    objectToUIComponentMap.put(component, uiComponent);
                    logger.trace("Adding UI to set of displayed components {}", uiComponentToObjectMap);
                    uiComponentToObjectMap.put(uiComponent, component);
                }
            } else {
                logger.debug("Nothing to do since no component was passed for adding to display area.");
            }
        }

        @Override
        public void remove(UIComponent<? extends JComponent> component) {
            if (component != null) {
                logger.trace("Removing component {} from {}", component, objectToUIComponentMap);
                JComponent uiComponent = objectToUIComponentMap.remove(component);
                if (uiComponent != null) {
                    logger.trace("Trying to remove UI component {}", uiComponent);
                    displayArea.remove(uiComponent);
                    logger.trace("Removing component from the set of active components {}", component, uiComponentToObjectMap);
                    uiComponentToObjectMap.remove(uiComponent);
                } else {
                    logger.trace("No component or associated UI component located");
                }
            } else {
                logger.debug("Nothing to do since no component was passed to be removed from display area");
            }
        }

        public void destroy() {
            logger.debug("Destroying Display Area {}", this);
            Set<UIComponent> activeObjects = new HashSet<UIComponent>(objectToUIComponentMap.keySet());
            for (UIComponent component : activeObjects) {
                try {
                    logger.debug("Trying to destroy {}", component);
                    component.destroy();
                } catch (Exception exception) {
                    logger.warn("Failed to destroy component " + component + ". Ignoring error.", exception);
                }
            }
            logger.debug("Destroyed {}", this);
        }
    }
}