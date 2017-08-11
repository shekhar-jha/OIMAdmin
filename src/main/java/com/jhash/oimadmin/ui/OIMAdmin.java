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
import com.jhash.oimadmin.service.Service;
import com.jhash.oimadmin.ui.component.NamedComponent;
import com.jhash.oimadmin.ui.componentTree.ROOTAdminTreeNode;
import com.jhash.oimadmin.ui.componentTree.UIComponentTree;
import com.jhash.oimadmin.ui.menu.ContextMenuEnabledNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.utils.MacOSXHandler;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.*;
import java.util.*;

public class OIMAdmin extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(OIMAdmin.class);
    private Config config = new Config();
    private UIComponentTreeImpl componentTree = null;
    private DisplayAreaImpl displayArea = null;
    private MenuHandlerImpl menuHandler = null;
    private ROOTAdminTreeNode rootNode = null;

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
        if (MacOSXHandler.isMac()) {
            MacOSXHandler.initialize(this);
        }
        logger.debug("Loading configuration...");
        config.load();
        logger.debug("Configuration loaded.");
        try {
            String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
            logger.debug("Setting UI Manager look and feel to default look and feel for current system i.e. {}", systemLookAndFeel);
            UIManager.setLookAndFeel(systemLookAndFeel);
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


        // Initialize all the UI Components
        rootNode = new ROOTAdminTreeNode("ConnectionTreeNode", config);
        displayArea = initializeDisplayPane();
        componentTree = initializeSelectionTree();
        JMenuBar menuBar = new JMenuBar();
        menuHandler = new MenuHandlerImpl(menuBar);

        rootNode.setDisplayArea(displayArea).setUIComponentTree(componentTree).setMenuHandler(menuHandler).initialize();
        componentTree.connectionTree.expandPath(new TreePath(rootNode.getUIObject().getPath()));

        JideTabbedPane selectTabbedPane = initializeSelectionPane();
        selectTabbedPane.addTab("Server Instances", componentTree.connectionTree);
        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        splitPane.add(new JideScrollPane(selectTabbedPane), 0);
        splitPane.add(displayArea.displayArea, 1);
        splitPane.setProportionalLayout(true);
        splitPane.setProportions(new double[]{0.3});

        // Create the menu bar
        menuBar.setOpaque(true);
        menuBar.setPreferredSize(new java.awt.Dimension(200, 20));
        if (MacOSXHandler.isMac()) {
            MacOSXHandler.registerMenuBar(menuBar);
        } else {
            setJMenuBar(menuBar);
        }
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
        final JTree localConnectionTree = new JTree(rootNode.getUIObject());
        localConnectionTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                logger.trace("Processing tree ({}) expansion event {}", localConnectionTree, event);
                try {
                    TreePath treePath;
                    Object expandedNode;
                    Object expandedNodeObject;
                    if (event != null && (treePath = event.getPath()) != null
                            && (expandedNode = treePath.getLastPathComponent()) instanceof DefaultMutableTreeNode
                            && (expandedNodeObject = ((DefaultMutableTreeNode) expandedNode).getUserObject()) instanceof UIComponentTree.Node) {
                        ((UIComponentTree.Node) expandedNodeObject).handleNodeEvent(UIComponentTree.EVENT_TYPE.NODE_EXPAND);
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
        localConnectionTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent event) {
                logger.trace("Processing tree({}) selection event {}", localConnectionTree, event);
                try {
                    TreePath treePath;
                    Object selectedNode;
                    Object selectedNodeObject;
                    if (event != null && (treePath = event.getPath()) != null
                            && (selectedNode = treePath.getLastPathComponent()) instanceof DefaultMutableTreeNode
                            && (selectedNodeObject = ((DefaultMutableTreeNode) selectedNode).getUserObject()) instanceof UIComponentTree.Node) {
                        ((UIComponentTree.Node) selectedNodeObject).handleNodeEvent(UIComponentTree.EVENT_TYPE.NODE_SELECTED);
                    } else {
                        logger.warn(
                                "Failed to identify the specific location of the tree being operated upon. Event received {}, Path: {}, Source: {}",
                                new Object[]{event, (event == null) ? "null" : event.getPath(),
                                        (event == null) ? "null" : event.getSource()});
                    }
                    logger.trace("Handled new selected item");
                } catch (Exception exception) {
                    logger.warn("Failed to handle new selected item event " + event + " on tree " + localConnectionTree, exception);
                }
                try {
                    TreePath previousSelectedNodePath;
                    Object previouslySelectedNode;
                    Object previouslySelectedNodeObject;
                    if (event != null && (previousSelectedNodePath = event.getOldLeadSelectionPath()) != null
                            && (previouslySelectedNode = previousSelectedNodePath.getLastPathComponent()) instanceof DefaultMutableTreeNode
                            && (previouslySelectedNodeObject = ((DefaultMutableTreeNode) previouslySelectedNode).getUserObject()) instanceof UIComponentTree.Node) {
                        ((UIComponentTree.Node) previouslySelectedNodeObject).handleNodeEvent(UIComponentTree.EVENT_TYPE.NODE_DESELECTED);
                    } else {
                        logger.warn(
                                "Failed to identify the specific location of the tree being operated upon. Event received {}, Path: {}, Source: {}",
                                new Object[]{event, (event == null) ? "null" : event.getOldLeadSelectionPath(),
                                        (event == null) ? "null" : event.getSource()});
                    }
                } catch (Exception exception) {
                    logger.warn("Failed to handle old selected item event " + event + " on tree " + localConnectionTree, exception);
                }
                logger.trace("Processed selection event.");
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
                            Object clickedNode;
                            Object clickedNodeObject;
                            logger.trace("Trying to validate whether we have received identifiable node detail");
                            if ((clickedNode = selPath.getLastPathComponent()) instanceof DefaultMutableTreeNode
                                    && (clickedNodeObject = ((DefaultMutableTreeNode) clickedNode).getUserObject()) instanceof UIComponentTree.Node) {
                                ((UIComponentTree.Node) clickedNodeObject).handleNodeEvent(UIComponentTree.EVENT_TYPE.NODE_DISPLAY);
                            } else {
                                logger.trace("Failed to locate node that was double clicked from path {}", selPath);
                            }
                        } else {
                            logger.trace("The number of clicks {} is not 2 i.e. double click. Ignoring event",
                                    event.getClickCount());
                            logger.trace("Trying to check whether the event is a popup menu event");
                            handlePopupEvent(event, selPath);
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
                        handlePopupEvent(event, selPath);
                    } else {
                        logger.trace("The event occurred at a location that did not correspond to a row. Ignoring the event");
                    }
                    logger.trace("Processed mouse pressed event {} on tree {}", event, localConnectionTree);
                } catch (Exception exception) {
                    logger.warn("Failed to process mouse pressed event " + event + " on tree " + localConnectionTree, exception);
                }
            }

        });
        return new UIComponentTreeImpl(localConnectionTree);
    }

    private void handlePopupEvent(MouseEvent event, TreePath selPath) {
        if (event.isPopupTrigger()) {
            Object clickedNode = selPath.getLastPathComponent();
            Object clickedNodeObject;
            logger.trace("Trying to validate whether we have received identifiable node detail");
            if (clickedNode instanceof DefaultMutableTreeNode
                    && (clickedNodeObject = ((DefaultMutableTreeNode) clickedNode).getUserObject()) instanceof ContextMenuEnabledNode) {
                ContextMenuEnabledNode menuEnabledNode = (ContextMenuEnabledNode) clickedNodeObject;
                if (menuEnabledNode.hasContextMenu()) {
                    JPopupMenu popupMenu = menuEnabledNode.getContextMenu();
                    if (popupMenu != null) {
                        logger.debug("Showing popup {}", popupMenu);
                        popupMenu.show(event.getComponent(), event.getX(), event.getY());
                    } else {
                        logger.debug("No popup menu could be retrieved from node.");
                    }
                } else {
                    logger.debug("No popup menu associated with node.");
                }
            } else {
                logger.trace("Failed to locate node {} that was clicked", clickedNode);
            }
        } else {
            logger.trace("No popup event was detected. Ignoring event");
        }
    }

    public void destroy() {
        logger.debug("Trying to destroy OIMAdmin");
        if (componentTree != null)
            componentTree.getRootNode().destroy();
        componentTree = null;
        if (displayArea != null)
            displayArea.destroy();
        displayArea = null;
        if (menuHandler != null)
            menuHandler.destroy();
        menuHandler = null;
        if (MacOSXHandler.isMac())
            MacOSXHandler.cleanup(this);
        logger.debug("Destroyed OIMAdmin");
    }


    private static class UIComponentTreeImpl implements UIComponentTree {

        private Logger logger = LoggerFactory.getLogger(UIComponentTreeImpl.class);
        private JTree connectionTree;
        private DefaultTreeModel model;

        public UIComponentTreeImpl(JTree connectionTree) {
            this.connectionTree = connectionTree;
            this.model = (DefaultTreeModel) connectionTree.getModel();
        }

        @Override
        public void addChildNode(Node parent, Node child) {
            if (parent == null || child == null)
                return;
            Object parentUIObject;
            Object childUIObject = null;
            if ((parentUIObject = parent.getUIObject()) instanceof MutableTreeNode
                    && (childUIObject = child.getUIObject()) instanceof MutableTreeNode) {
                model.insertNodeInto((MutableTreeNode) childUIObject, (MutableTreeNode) parentUIObject, ((MutableTreeNode) parentUIObject).getChildCount());
            } else {
                throw new OIMAdminException("Expected parent " + parent + " and child " + child
                        + " node to have MutableTreeNode but found parent " + parentUIObject + " and child " + childUIObject);
            }
        }

        @Override
        public List<Node> getChildNodes(Node parent) {
            List<Node> childNodes = new ArrayList<>();
            if (parent == null)
                return childNodes;
            int numberOfChildNodes = model.getChildCount(parent.getUIObject());
            for (int counter = 0; counter < numberOfChildNodes; counter++) {
                Object nodeObject = model.getChild(parent.getUIObject(), counter);
                if (nodeObject == null)
                    throw new NullPointerException("Located a null node as child of node " + parent + " at index " + counter);
                Object nodeUserObject;
                if (nodeObject instanceof DefaultMutableTreeNode) {
                    if ((nodeUserObject = ((DefaultMutableTreeNode) nodeObject).getUserObject()) instanceof Node) {
                        childNodes.add((Node) nodeUserObject);
                    } else {
                        logger.warn("Located a node Object {} with user object of type {} instead of Node while enumerating child nodes of {}. Ignoring the node", new Object[]{nodeObject, (nodeUserObject != null ? nodeObject.getClass() : "null"), parent});
                    }
                } else {
                    logger.warn("Located a node {} of type {} instead of DefaultMutableTreeNode while enumerating child nodes of {}. Ignoring the node", new Object[]{nodeObject, nodeObject.getClass(), parent});
                }
            }
            return childNodes;
        }

        @Override
        public void removeChildNode(Node parent, Node child) {
            if (child != null) {
                Object childUIObject;
                if ((childUIObject = child.getUIObject()) instanceof MutableTreeNode) {
                    model.removeNodeFromParent((MutableTreeNode) childUIObject);
                } else {
                    throw new OIMAdminException("Found child node to remove " + child + " did not contain MutableTreeNode. Located: " + childUIObject);
                }
            }
        }

        @Override
        public ROOTAdminTreeNode getRootNode() {
            Object rootNode = model.getRoot();
            Object rootNodeObject;
            if (!(rootNode instanceof DefaultMutableTreeNode))
                throw new OIMAdminException("Located invalid root node of tree " + connectionTree + " as " + rootNode);
            if (((rootNodeObject = ((DefaultMutableTreeNode) rootNode).getUserObject()) instanceof ROOTAdminTreeNode)) {
                return ((ROOTAdminTreeNode) rootNodeObject);
            } else {
                throw new OIMAdminException("Expected class " + ROOTAdminTreeNode.class + " but found class "
                        + (rootNodeObject == null ? "null" : rootNodeObject.getClass()) + " for node " + rootNode);
            }
        }

    }

    private static class DisplayAreaImpl implements DisplayArea {

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
                    if (source instanceof JComponent && uiComponentToObjectMap.containsKey(source)) {
                        UIComponent<? extends JComponent> uiComponentObject = uiComponentToObjectMap.get(source);
                        if (uiComponentObject instanceof AbstractUIComponent) {
                            if (((AbstractUIComponent) uiComponentObject).isDestroyComponentOnClose()) {
                                ((AbstractUIComponent) uiComponentObject).destroy();
                            } else {
                                logger.trace("Component should not be destroyed on close. Just removing the component from display");
                                displayArea.remove((JComponent) source);
                            }
                        } else {
                            logger.trace("Even though expected, the object {} is not an instance of {}", uiComponentObject, AbstractUIComponent.class);
                        }
                    } else {
                        logger.trace("Could not locate UI component in {} for source {}", uiComponentToObjectMap, source);
                    }
                    logger.trace("Triggered close action on tab {}", source);
                }
            });
        }

        @Override
        public void add(UIComponent<? extends JComponent> component) {
            if (component != null) {
                String name = (component instanceof NamedComponent) ? ((NamedComponent) component).getName() : component.toString();
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
                if (component instanceof Service) {
                    try {
                        logger.debug("Trying to destroy {}", component);
                        ((Service) component).destroy();
                    } catch (Exception exception) {
                        logger.warn("Failed to destroy component " + component + ". Ignoring error.", exception);
                    }
                }
            }
            logger.debug("Destroyed {}", this);
        }
    }

    private static class MenuHandlerImpl implements MenuHandler {

        private final JMenuBar menuBar;
        private Map<MenuHandler.MENU, JMenuItem> menuMap = new HashMap<>();
        private Map<MENU, Map<Context, ActionHandler>> menuActionMap = new HashMap<>();

        public MenuHandlerImpl(JMenuBar menuBar) {
            this.menuBar = menuBar;
        }

        @Override
        public void register(MENU menuItem) {
            register(menuItem, null, null);
        }

        @Override
        public void register(MENU menuItem, Context context) {
            register(menuItem, context, null);
        }

        @Override
        public void register(MENU menuItem, ActionHandler actionHandler) {
            register(menuItem, null, actionHandler);
        }

        @Override
        public void register(final MENU menuItem, final Context context, final ActionHandler actionHandler) {
            if (menuItem == null)
                return;
            logger.debug("Registering menu {}", menuItem);
            if (!menuMap.containsKey(menuItem)) {
                logger.debug("New Menu identified.");
                JMenuItem menuObject = (actionHandler == null) ? new JMenu(menuItem.getName()) : new JMenuItem(menuItem.getName());
                if (menuItem.keyStroke != null) {
                    logger.debug("Setting accelerator {}", menuItem.keyStroke);
                    menuObject.setAccelerator(menuItem.keyStroke);
                }
                if (actionHandler != null && context != null) {
                    addActionHandler(menuItem, menuObject, context, actionHandler);
                }
                JMenuItem parentMenuItem = null;
                if (menuItem.parent != null) {
                    logger.debug("Locating parent {}", menuItem.parent);
                    parentMenuItem = menuMap.get(menuItem.parent);
                    if (parentMenuItem == null) {
                        register(menuItem.parent, null, null);
                        parentMenuItem = menuMap.get(menuItem.parent);
                    }
                }
                if (parentMenuItem != null) {
                    logger.debug("Adding menu to parent {}", parentMenuItem);
                    parentMenuItem.add(menuObject);
                } else {
                    logger.debug("Adding menu to menu bar.");
                    menuBar.add(menuObject);
                }
                menuMap.put(menuItem, menuObject);
            } else {
                logger.debug("Existing menu located.");
                JMenuItem registeredMenuItem = menuMap.get(menuItem);
                KeyStroke acceleratorKeyStroke = registeredMenuItem.getAccelerator();
                if (acceleratorKeyStroke != menuItem.keyStroke) { // Key strokes are immutable?
                    throw new OIMAdminException("An existing menu item has a different accelerator keystroke " + acceleratorKeyStroke + " compared to give menu " + menuItem);
                }
                if (actionHandler != null && context != null)
                    addActionHandler(menuItem, registeredMenuItem, context, actionHandler);
            }
            logger.debug("Registered menu {}", menuItem);
        }

        private void addActionHandler(final MENU menuItem, JMenuItem menuObject, Context context, ActionHandler actionHandler) {
            if (menuItem == null || context == null || actionHandler == null)
                return;
            Map<Context, ActionHandler> menuActionHandlerMapEntry = menuActionMap.get(menuItem);
            if (menuActionHandlerMapEntry == null) {
                menuActionHandlerMapEntry = new HashMap<>();
                menuActionMap.put(menuItem, menuActionHandlerMapEntry);
                menuObject.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logger.debug("Invoking menu action handler for {}", menuItem);
                        for (Map.Entry<Context, ActionHandler> actionHandlerEntry : menuActionMap.get(menuItem).entrySet()) {
                            Context context = actionHandlerEntry.getKey();
                            if (context != null && context.isActive()) {
                                try {
                                    logger.debug("Invoking action handler for menu {} with context {}", new Object[]{menuItem, context});
                                    actionHandlerEntry.getValue().invoke(menuItem, context);
                                    logger.debug("Invoked action handler.");
                                } catch (Exception exception) {
                                    logger.warn("Invocation of menu " + menuItem + " using context "
                                            + context + " failed. Exception", exception);
                                    context.displayMessage("Action handler invocation failed.", exception);
                                }
                            }
                        }
                        logger.debug("Completed menu action handler");
                    }
                });
            }
            menuActionHandlerMapEntry.put(context, actionHandler);
        }

        @Override
        public void unregister(MENU menuItem, Context context, ActionHandler actionHandler) {
            if (menuItem == null)
                return;
            if (menuMap.containsKey(menuItem)) {

            } else {
                logger.debug("The Menu {} is not registered.", menuItem);
            }
        }

        public void destroy() {
            this.menuActionMap.clear();
            for (Map.Entry<MENU, JMenuItem> menuItem : menuMap.entrySet()) {
                if (menuItem.getKey().parent == null) {
                    menuBar.remove(menuItem.getValue());
                }
            }
            this.menuMap.clear();
        }
    }
}