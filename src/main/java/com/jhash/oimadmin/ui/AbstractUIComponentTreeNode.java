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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractUIComponentTreeNode<T> extends OIMAdminTreeNode implements UIComponent<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractUIComponentTreeNode.class);

    protected final DisplayArea displayArea;
    private String stringRepresentation;


    public AbstractUIComponentTreeNode(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        this(name, configuration, selectionTree, displayArea, NODE_STATE.NOT_INITIALIZED);
    }


    public AbstractUIComponentTreeNode(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea, NODE_STATE status) {
        super(name, configuration, selectionTree, status);
        this.displayArea = displayArea;
        this.stringRepresentation = getClass().getName() + " [" + name + "]";
        if (isLoadable())
            add(new LoadingNode(this, selectionTree));
    }

    public void executeOperationService(final UIComponent component) {
        if (component != null) {
            Utils.executeAsyncOperation("Component " + component.getName() + " [Initialization]", new Runnable() {
                @Override
                public void run() {
                    component.initialize();
                }
            });
        } else {
            logger.warn("No UI Component was passed for initialization.");
        }
    }

    public boolean isDisplayable() {
        return getStatus() == OIMAdminTreeNode.NODE_STATE.INITIALIZED;
    }

    @Override
    public AbstractUIComponentTreeNode<T> initialize() {
        logger.debug("Trying to initialize UI Component");
        if (getStatus() == NODE_STATE.INITIALIZATION_IN_PROGRESS) {
            logger.warn("Node {} is already being initialized, ignoring the trigger", this);
            return this;
        }
        if (getStatus() == NODE_STATE.INITIALIZED) {
            logger.debug("Nothing to do since node {} is already initialized.", this);
            return this;
        }
        setStatus(NODE_STATE.INITIALIZATION_IN_PROGRESS);
        try {
            initializeComponent();
            setStatus(NODE_STATE.INITIALIZED);
            List<OIMAdminTreeNode> childNodes = this.selectionTree.getChildNodes(this);
            if (childNodes.get(0) instanceof LoadingNode) {
                selectionTree.removeChildNode(this, childNodes.get(0));
            }
            logger.debug("Initialized UI Component");
        } catch (Exception exception) {
            displayMessage("Failed to initialize " + name, "", exception);
            logger.warn("Failed to initialize component " + this, exception);
            destroyChildNodes();
            logger.debug("Setting node status as {}", OIMAdminTreeNode.NODE_STATE.FAILED);
            setStatus(OIMAdminTreeNode.NODE_STATE.FAILED);
        }
        return this;
    }

    public abstract void initializeComponent();

    @Override
    public void handleEvent(EVENT_TYPE event) {
        switch (event) {
            case NODE_EXPAND:
                if (isLoadable())
                    executeOperationService(this);
                break;
            case NODE_DISPLAY:
                if (this instanceof DisplayableNode) {
                    DisplayableNode displayableNode = (DisplayableNode) this;
                    if (displayableNode.isDisplayable())
                        executeOperationService(displayableNode.getDisplayComponent());
                }
                break;
            default:
                logger.debug("Nothing to do for event {} on node {}", event, this);
                break;
        }
    }

    public abstract void destroyComponent();

    @Override
    public void destroy() {
        logger.debug("Trying to destroy {}", this);
        if (getStatus() == NODE_STATE.INITIALIZED || getStatus() == NODE_STATE.INITIALIZED_NO_OP) {
            logger.debug("Node in {} state, setting status to {} before destroying", getStatus(), NODE_STATE.DESTRUCTION_IN_PROGRESS);
            setStatus(NODE_STATE.DESTRUCTION_IN_PROGRESS);
            destroyChildNodes();
            try {
                destroyComponent();
                logger.debug("Completed node specific destruction");
            } catch (Exception exception) {
                logger.warn("Failed to complete the node specific destruction process", exception);
            }
            logger.debug("Setting status to {}", NODE_STATE.NOT_INITIALIZED);
            setStatus(NODE_STATE.NOT_INITIALIZED);
        } else {
            logger.debug("Skipping destroy since the node is not in {} state", Arrays.asList(new Object[]{NODE_STATE.INITIALIZED, NODE_STATE.INITIALIZED_NO_OP}));
        }
    }

    public void destroyChildNodes() {
        logger.debug("Trying to destroy all the associated child nodes of {}", this);
        for (OIMAdminTreeNode childNode : selectionTree.getChildNodes(this)) {
            if (childNode instanceof UIComponent) {
                logger.trace("Trying to destroy child node {}", childNode);
                try {
                    ((UIComponent) childNode).destroy();
                    logger.trace("Destroyed child node {}", childNode);
                } catch (Exception exception) {
                    logger.warn("Error occurred while destroying child node " + childNode + " of node " + this + ". Ignoring error", exception);
                }
            }
            selectionTree.removeChildNode(this, childNode);
        }
        logger.debug("Destroyed all child nodes of {}", this);
    }

    public void displayMessage(String title, String message, Exception exception) {
        StringBuilder messageDetails = new StringBuilder(message);
        if (message == null || message.equalsIgnoreCase("")) {
            messageDetails.append(exception.getMessage());
        }
        messageDetails.append(System.lineSeparator());
        if (exception != null) {
            Throwable applicableException = exception;
            while (applicableException != null) {
                if (applicableException != exception) {
                    messageDetails.append(System.lineSeparator());
                    messageDetails.append("Caused By: ");
                }
                messageDetails.append(applicableException);
                messageDetails.append(System.lineSeparator());
                messageDetails.append(applicableException.getStackTrace()[0].toString());
                for (int stackTraceDepth = 1; stackTraceDepth < applicableException.getStackTrace().length; stackTraceDepth++) {
                    if (applicableException.getStackTrace()[stackTraceDepth].getClassName().startsWith("com.jhash.oimadmin")) {
                        messageDetails.append(System.lineSeparator());
                        messageDetails.append("...");
                        messageDetails.append(System.lineSeparator());
                        messageDetails.append(applicableException.getStackTrace()[stackTraceDepth].toString());
                        break;
                    }
                }
                applicableException = applicableException.getCause();
            }
        }
        JOptionPane.showMessageDialog(null, messageDetails.toString(), title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public String getStringRepresentation() {
        return stringRepresentation;
    }

    public static class DummyAdminTreeNode extends AbstractUIComponentTreeNode<Object> {

        private static final Logger logger = LoggerFactory.getLogger(DummyAdminTreeNode.class);

        //TODO: Work on making sure that Dummy node does not trigger NPE.
        public DummyAdminTreeNode(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
            super(name, configuration, selectionTree, displayArea, NODE_STATE.INITIALIZED_NO_OP);
            logger.trace("DummyAdminTreeNode()");
        }

        @Override
        public void initializeComponent() {
            logger.trace("Node initialized.");
        }

        @Override
        public void destroyComponent() {
            logger.trace("Node destroyed.");
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {

        }

        @Override
        public Object getComponent() {
            return null;
        }

    }

    public static class DisplayComponentNode<T extends UIComponent, W> extends AbstractUIComponentTreeNode<W> implements DisplayableNode<T> {

        private static final Logger logger = LoggerFactory.getLogger(DisplayComponentNode.class);

        private T displayComponent;
        private W nodeComponent;

        public DisplayComponentNode(String name, T displayComponent, W component, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
            super(name, configuration, selectionTree, displayArea);
            logger.debug("DisplayComponentNode({}, {}, {}, {}, {})", new Object[]{name, displayComponent, configuration, selectionTree, displayArea});
            this.displayComponent = displayComponent;
            this.nodeComponent = component;
        }

        @Override
        public void initializeComponent() {
            logger.debug("Initialized component {}", this);
        }

        @Override
        public W getComponent() {
            return nodeComponent;
        }

        @Override
        public T getDisplayComponent() {
            return displayComponent;
        }

        @Override
        public void destroyComponent() {
            logger.debug("Trying to destroy {}", this);
            if (displayComponent != null) {
                displayComponent.destroy();
            }
            logger.debug("Destroyed {}", this);
        }

    }

    public static class ROOTAdminTreeNode extends DummyAdminTreeNode {

        private static final Logger logger = LoggerFactory.getLogger(ROOTAdminTreeNode.class);

        //TODO: Work on making sure that Root node does not trigger NPE.
        public ROOTAdminTreeNode(String name) {
            super(name, null, null, null);
            logger.trace("ROOTAdminTreeNode()");
        }

        public void setUIComponentTree(UIComponentTree selectionTree) {
            logger.trace("Setting UIComponentTree for root node to {}", selectionTree);
            super.selectionTree = selectionTree;
        }

    }

}
