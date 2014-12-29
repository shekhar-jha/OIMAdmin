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
import com.jhash.oimadmin.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class AbstractUIComponent implements UIComponent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractUIComponent.class);
    protected static ThreadFactory threadFactory = Executors.defaultThreadFactory();

    protected Config config = null;
    protected boolean isInitialized = false;

    public static OIMAdminTreeNode getNode(Object nodeObject) {
        logger.debug("Trying to validate whether we have received identifiable node detail");
        if (nodeObject != null && nodeObject instanceof OIMAdminTreeNode) {
            return ((OIMAdminTreeNode) nodeObject);
        } else {
            logger.warn(
                    "Failed to locate the node {} (Class: {}) being expanded. Ignoring expansion event.",
                    nodeObject, nodeObject == null ? "null" : nodeObject.getClass());
            return null;
        }
    }

    public static OIMAdminTreeNode addUninitializedNode(OIMAdminTreeNode parentNode, OIMAdminTreeNode treeNode, DefaultTreeModel model) {
        treeNode.add(new OIMAdminTreeNode.OIMAdminTreeNodeNoAction(OIMAdmin.DUMMY_LEAF_NODE_NAME, OIMAdminTreeNode.NODE_TYPE.DUMMY,
                treeNode.configuration));
        model.insertNodeInto(treeNode, parentNode, parentNode.getChildCount());
        return treeNode;

    }

    public static void executeLoaderService(OIMAdminTreeNode node, JTree connectionTree, Runnable loader) {
        if (node == null)
            throw new NullPointerException("Can not run loader since node that needs to be loaded is null.");
        if (connectionTree == null)
            throw new NullPointerException("Can not run loader since no connection tree is available.");
        if (loader == null)
            throw new NullPointerException("Can not run loader since loader is null.");
        DefaultTreeModel model = (DefaultTreeModel) connectionTree.getModel();
        logger.debug("Trying to check if node {} is not initialized", node.name);
        if (node.getStatus() == OIMAdminTreeNode.NODE_STATE.NOT_INITIALIZED) {
            logger.debug("Setting the status to {} to avoid multiple triggers of loading",
                    OIMAdminTreeNode.NODE_STATE.INITIALIZATION_IN_PROGRESS);
            node.setStatus(OIMAdminTreeNode.NODE_STATE.INITIALIZATION_IN_PROGRESS);
            logger.debug("Setting up the initialization of {}", node.name);
            Thread oimConnectionThread = threadFactory.newThread(new Runnable() {

                @Override
                public void run() {
                    try {
                        logger.debug("Trying to run process to initialize node {}", node);
                        loader.run();
                        logger.debug(
                                "Completed the process to initialize node {}. Trying to remove {} leaf node since the loading was successful.",
                                node, OIMAdmin.DUMMY_LEAF_NODE_NAME);
                        if (node.getChildCount() > 0) {
                            if (node.getFirstChild() instanceof OIMAdminTreeNode
                                    && ((OIMAdminTreeNode) node.getFirstChild()).name.equals(OIMAdmin.DUMMY_LEAF_NODE_NAME)) {
                                logger.debug("Removing {} from node {}", OIMAdmin.DUMMY_LEAF_NODE_NAME, node);
                                model.removeNodeFromParent((OIMAdminTreeNode) node.getFirstChild());
                            } else {
                                logger.debug(
                                        "The first entry {} in the loaded node {} is not instance of OIMAdminTreeNode or is not {}",
                                        new Object[]{node.getFirstChild(), node, OIMAdmin.DUMMY_LEAF_NODE_NAME});
                            }
                        } else {
                            logger.debug(
                                    "Nothing was loaded and the node {} does not have any dummy child node to remove though expected.",
                                    node);
                        }
                        node.setStatus(OIMAdminTreeNode.NODE_STATE.INITIALIZED);
                    } catch (Exception exception) {
                        logger.warn("Failed to initialize OIM Connection for " + node.name, exception);
                        logger.debug("Trying to delete all the child nodes of node");
                        while (node.getChildCount() > 0) {
                            model.removeNodeFromParent((OIMAdminTreeNode) node.getFirstChild());
                        }
                        logger.debug("Adding {} node as child node to inform user", OIMAdmin.DUMMY_LEAF_NODE_NAME_ERROR);
                        model.insertNodeInto(new OIMAdminTreeNode.OIMAdminTreeNodeNoAction(OIMAdmin.DUMMY_LEAF_NODE_NAME_ERROR, node.type,
                                node.configuration), node, node.getChildCount());
                        logger.debug("Setting node status as ", OIMAdminTreeNode.NODE_STATE.FAILED);
                        node.setStatus(OIMAdminTreeNode.NODE_STATE.FAILED);
                        logger.debug("Triggering the node expansion due to way tree expansion behaves once it looses all the nodes ");
                        connectionTree.expandPath(new TreePath(node.getPath()));
                    }
                }
            });
            oimConnectionThread.setDaemon(false);
            oimConnectionThread.setName("Loading " + node.type + "(" + node.name + ")");
            oimConnectionThread.start();
            logger.debug("Completed setup of {} node's initialization", node.name);
        } else {
            logger.debug("Nothing to do since the node is already initialized.");
        }
    }

    public static void executeDisplayService(OIMAdminTreeNode node, JTabbedPane displayTabbedPane, ExecuteCommand<? extends JComponent> displayUIComponent) {
        if (node == null)
            throw new NullPointerException("Can not execute display service since node that needs to be displayed is null.");
        if (displayTabbedPane == null)
            throw new NullPointerException("Can not run display service since tabbed pane for the display is null.");
        if (displayUIComponent == null)
            throw new NullPointerException("Can not run display service since displayUIComponent function is null.");
        logger.debug("Trying to check if node {} is initialized", node.name);
        if (node.isDisplayable()) {
            logger.debug("Setting up the display of {}", node.name);
            Thread displayThread = threadFactory.newThread(new Runnable() {

                @Override
                public void run() {
                    logger.debug("Trying to run process to display node {}", node);
                    try {
                        JComponent displayComponent = displayUIComponent.run();
                        if (displayComponent == null) {
                            logger.debug("Nothing was returned by display setup function to display");
                        } else {
                            logger.debug("Trying to add returned component {} as tab {}", displayComponent, node.name);
                            displayTabbedPane.addTab(node.name, displayComponent);
                            displayTabbedPane.setSelectedComponent(displayComponent);
                        }
                    } catch (Exception exception) {
                        logger.warn("Failed to initialize OIM Connection for " + node.name, exception);
                    }
                    logger.debug("Completed the process to display node {}", node);
                }
            });
            displayThread.setDaemon(false);
            displayThread.setName("Displaying " + node.type + "(" + node.name + ")");
            displayThread.start();
            logger.debug("Completed setup of {} node's display", node.name);
        } else {
            logger.debug("Nothing to do since the node is not in displayable state.");
        }
    }

    public static void resetNode(OIMAdminTreeNode node, DefaultTreeModel model) {
        logger.debug("Trying to delete all the child nodes of node");
        while (node.getChildCount() > 0) {
            model.removeNodeFromParent((OIMAdminTreeNode) node.getFirstChild());
        }
        logger.debug("Setting the node status to {}", OIMAdminTreeNode.NODE_STATE.NOT_INITIALIZED);
        node.setStatus(OIMAdminTreeNode.NODE_STATE.NOT_INITIALIZED);
    }

    public static void runOperation(JComponent displayComponent, Runnable executor) {
        if (executor == null)
            throw new NullPointerException("Can not run tab operation since executor function is null.");
        logger.debug("Executing {}. Associated displayed component {}", new Object[]{executor,
                displayComponent});
        Thread displayerThread = threadFactory.newThread(new Runnable() {

            @Override
            public void run() {
                try {
                    logger.debug("Trying to execute command");
                    if (displayComponent != null)
                        displayComponent.setEnabled(false);
                    executor.run();
                    if (displayComponent != null)
                        displayComponent.setEnabled(true);
                    logger.debug("Completed execution of command");
                } catch (Exception exception) {
                    logger.warn("Failed to execute command " + executor, exception);
                }
            }
        });
        displayerThread.setDaemon(false);
        displayerThread.setName("Command run: " + executor);
        displayerThread.start();
        logger.debug("Completed setup of command execution for node");
    }

    @Override
    public String toString() {
        return getStringRepresentation();
    }

    @Override
    public Config.Configuration getConfiguration() {
        return null;
    }

    @Override
    public void initialize(Config config) {
        logger.debug("Trying to initialize UI Component");
        if (config != null) {
            if (isInitialized) {
                logger.debug("Destroying existing component since it is already initialized.");
                destroy();
            }
            this.config = config;
            initializeComponent();
            isInitialized = true;
        } else {
            throw new NullPointerException(
                    "No configuration available for initialization. Please pass the configuration method.");
        }
        logger.debug("Initialized UI Component");
    }

    @Override
    public abstract String getName();

    public abstract void initializeComponent();

    public abstract void destroyComponent();

    public abstract String getStringRepresentation();

    @Override
    public void destroy() {
        isInitialized = false;
        destroyComponent();
    }

    public static interface EventProcessor {
        public void processEvent(OIMAdminTreeNode node, OIMAdminTreeNode.EVENT_TYPE event);
    }
}
