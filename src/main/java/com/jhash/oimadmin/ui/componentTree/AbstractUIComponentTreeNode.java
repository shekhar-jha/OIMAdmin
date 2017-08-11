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

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.events.Event;
import com.jhash.oimadmin.events.EventSource;
import com.jhash.oimadmin.service.Service;
import com.jhash.oimadmin.ui.UIComponent;
import com.jhash.oimadmin.ui.component.EventEnabledServiceComponentImpl;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.component.PublishableComponent;
import com.jhash.oimadmin.ui.menu.ContextMenuEnabledNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.utils.MacOSXHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractUIComponentTreeNode<R extends AbstractUIComponentTreeNode>
        extends EventEnabledServiceComponentImpl<R> implements UIComponentTree.Node<DefaultMutableTreeNode>, ContextMenuEnabledNode, ParentComponent<R>, PublishableComponent<R> {

    public static final String DUMMY_LEAF_NODE_NAME = "Loading...";
    public static final MenuHandler.MENU REFRESH = new MenuHandler.MENU("Refresh", MenuHandler.MENU.FILE,
            KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
    public static final MenuHandler.MENU OPEN = new MenuHandler.MENU("Open", MenuHandler.MENU.FILE,
            KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    public static final MenuHandler.MENU DELETE = new MenuHandler.MENU("Delete", MenuHandler.MENU.FILE,
            KeyStroke.getKeyStroke(MacOSXHandler.isMac() ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    private static final Logger logger = LoggerFactory.getLogger(AbstractUIComponentTreeNode.class);

    private DefaultMutableTreeNode treeNode;
    private JPopupMenu popupMenu;
    private Map<MenuHandler.MENU, JMenuItem> popupMenuMap = new HashMap<>();
    private Map<MenuHandler.MENU, MenuHandler.ActionHandler> actionHandlerMenuMap = new HashMap<>();
    private boolean isSelected = false;
    protected MenuHandler.Context nodeContext = new MenuHandler.Context() {
        @Override
        public boolean isActive() {
            return isSelected;
        }

        @Override
        public void displayMessage(String message) {
            AbstractUIComponentTreeNode.this.displayMessage(getName() + " Failed", message, null);
        }

        @Override
        public void displayMessage(String message, Exception exception) {
            AbstractUIComponentTreeNode.this.displayMessage(getName() + " Failed", message, exception);

        }
    };
    private boolean publish = true;
    private boolean published = false;

    public AbstractUIComponentTreeNode(String name, ParentComponent parent) {
        this(name, parent, Service.NOT_INITIALIZED);
    }


    public AbstractUIComponentTreeNode(String name, ParentComponent parentComponent, STATE status) {
        super(name, parentComponent, status);
        treeNode = new DefaultMutableTreeNode(this);
        if (isLoadable())
            new LoadingNode(this).initialize();
        if (this instanceof UIComponent) {
            registerMenu(OPEN, new MenuHandler.ActionHandler() {
                @Override
                public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                    handleNodeEvent(UIComponentTree.EVENT_TYPE.NODE_DISPLAY);
                }
            });
        }
    }

    public static void destroyChildNodes(UIComponentTree.Node node, UIComponentTree selectionTree) {
        logger.debug("Trying to destroy all the associated child nodes of {}", node);
        for (UIComponentTree.Node childNode : selectionTree.getChildNodes(node)) {
            if (childNode instanceof Service) {
                logger.trace("Trying to destroy child node {}", childNode);
                try {
                    ((Service) childNode).destroy();
                    logger.trace("Destroyed child node {}", childNode);
                } catch (Exception exception) {
                    logger.warn("Error occurred while destroying child node " + childNode + " of node " + node + ". Ignoring error", exception);
                }
            }
            selectionTree.removeChildNode(node, childNode);
        }
        logger.debug("Destroyed all child nodes of {}", node);
    }

    @Override
    public String toString() {
        return getName();
    }

    public DefaultMutableTreeNode getUIObject() {
        return treeNode;
    }

    public boolean isLoadable() {
        return getState() == Service.NOT_INITIALIZED;
    }

    public R setPublish(boolean publish) {
        if (getState() != NOT_INITIALIZED) {
            throw new OIMAdminException("Set publish must be set before the initialize has been called on the node.");
        }
        this.publish = publish;
        return (R) this;
    }

    public R publish() {
        if (publish && !published) {
            if (getParent() instanceof AbstractUIComponentTreeNode) {
                this.getUIComponentTree().addChildNode((AbstractUIComponentTreeNode) getParent(), this);
                published = true;
            }
        }
        return (R) this;
    }

    public UIComponentTree.Node getParentNode() {
        ParentComponent parentComponent = getParent();
        if (parentComponent instanceof UIComponentTree.Node)
            return (UIComponentTree.Node) parentComponent;
        else
            throw new OIMAdminException("The Parent node of the " + this + " node is NOT an instance of Node. It is " + parentComponent);
    }

    @Override
    public final void initializeComponent() {
        logger.debug("Initializing Node...");
        try {
            setupNode();
            UIComponentTree selectionTree = this.getUIComponentTree();
            List<UIComponentTree.Node> childNodes = selectionTree.getChildNodes(this);
            if (childNodes.size() >= 1 && childNodes.get(0) instanceof LoadingNode) {
                selectionTree.removeChildNode(this, childNodes.get(0));
            }
            publish();
            logger.debug("Initialized Node.");
        } catch (Exception exception) {
            logger.warn("Failed to initialize node " + this, exception);
            destroyChildNodes(this, getParent().getUIComponentTree());
            throw exception;
        }
    }

    public abstract void setupNode();

    public boolean handleEvent(EventSource parent, Event event) {
        return false;
    }

    public void handleNodeEvent(UIComponentTree.EVENT_TYPE event) {
        logger.trace("Handling {}", event);
        switch (event) {
            case NODE_EXPAND:
                if (isLoadable()) {
                    Utils.executeAsyncOperation("Node " + this + " [Initialization]", new Runnable() {
                        @Override
                        public void run() {
                            initialize();
                        }
                    });
                } else {
                    logger.debug("Node {} is not loadable.", this);
                }
                break;
            case NODE_DISPLAY:
                if (this instanceof UIComponent) {
                    final UIComponent<JComponent> uiComponent = ((UIComponent<UIComponent<JComponent>>) this).getComponent();
                    if (uiComponent instanceof Service) {
                        Utils.executeAsyncOperation("UI Component " + uiComponent + " [Initialization]", new Runnable() {
                            @Override
                            public void run() {
                                ((Service) uiComponent).initialize();
                                getDisplayArea().add(uiComponent);
                            }
                        });
                    } else if (uiComponent != null) {
                        getDisplayArea().add(uiComponent);
                    } else {
                        logger.debug("Node {} does not any UI Component ", this);
                    }
                } else {
                    logger.debug("Node {} does not have associated UI Component", this);
                }
                break;
            case NODE_SELECTED:
                isSelected = true;
                break;
            case NODE_DESELECTED:
                isSelected = false;
                break;
            default:
                logger.debug("Nothing to do for event {} on node {}", event, this);
                break;
        }
        logger.trace("Handled {}", event);
    }

    @Override
    public boolean hasContextMenu() {
        return popupMenu != null;
    }

    @Override
    public JPopupMenu getContextMenu() {
        return popupMenu;
    }

    public void registerMenu(final MenuHandler.MENU menu, final MenuHandler.ActionHandler actionHandler) {
        if (menu == null)
            return;
        if (popupMenuMap.containsKey(menu))
            throw new OIMAdminException("The Menu item " + menu + " is already registered. Please unregister the item for the node before re-registering it.");
        logger.debug("Registering menu {} with action handler {}", menu, actionHandler);
        getMenuHandler().register(menu, nodeContext, actionHandler);
        if (actionHandler != null) {
            if (popupMenu == null)
                popupMenu = new JPopupMenu();
            JMenuItem popupMenuItem = new JMenuItem(menu.getName());
            popupMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        logger.debug("Invoking action handler for menu {} with context {}", new Object[]{menu, nodeContext});
                        actionHandler.invoke(menu, nodeContext);
                        logger.debug("Invoked action handler.");
                    } catch (Exception exception) {
                        logger.warn("Invocation of menu " + menu + " using context "
                                + nodeContext + " failed. Exception", exception);
                        displayMessage("Failed to initialize " + getName(), "Action handler invocation failed.", exception);
                    }
                }
            });
            popupMenu.add(popupMenuItem);
            popupMenuMap.put(menu, popupMenuItem);
        }
        logger.debug("Registered menu");
    }

    public void unregisterMenu(final MenuHandler.MENU menu) {
        if (menu == null)
            return;
        if (actionHandlerMenuMap.containsKey(menu)) {
            getMenuHandler().unregister(menu, nodeContext, actionHandlerMenuMap.remove(menu));
        } else {
            getMenuHandler().unregister(menu, nodeContext, null);
        }
        if (popupMenu != null && popupMenuMap.containsKey(menu)) {
            popupMenu.remove(popupMenuMap.remove(menu));
        }
        if (popupMenuMap.isEmpty() && popupMenu != null) {
            popupMenu.removeAll();
            popupMenu = null;
        }
    }

    @Override
    public final void destroyComponent() {
        logger.trace("Destroying Node {}", this);
        this.isSelected = false;
        unregisterMenu(OPEN);
        destroyChildNodes(this, getUIComponentTree());
        try {
            destroyNode();
            logger.debug("Completed node specific destruction");
        } catch (Exception exception) {
            logger.warn("Failed to complete the node specific destruction process", exception);
        }
        super.destroyComponent();
        logger.trace("Destroyed Node.");
    }

    public abstract void destroyNode();


}
