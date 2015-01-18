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
package com.jhash.oimadmin;

import com.jhash.oimadmin.Config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class OIMAdminTreeNode extends DefaultMutableTreeNode {

    public static final String DUMMY_LEAF_NODE_NAME = "Loading...";
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(OIMAdminTreeNode.class);
    protected final String name;
    protected final Configuration configuration;
    protected UIComponentTree selectionTree;
    //TODO: Should status be moved to Abstract class where the lifecycle actually is managed?
    private OIMAdminTreeNode.NODE_STATE status = OIMAdminTreeNode.NODE_STATE.NOT_INITIALIZED;

    public OIMAdminTreeNode(String name, Configuration configuration, UIComponentTree selectionTree, NODE_STATE status) {
        super(name);
        logger.trace("Entering OIMAdminTreeNode({}, {}, {})", new Object[]{name, configuration, selectionTree});
        if (name == null || name.isEmpty())
            throw new NullPointerException("Can not create a tree node with value " + name);
        this.name = name;
        this.configuration = configuration;
        this.selectionTree = selectionTree;
        this.status = status;
        logger.trace("Leaving OIMAdminTreeNode({}, {}, {})", new Object[]{name, configuration, selectionTree});
    }

    public Config.Configuration getConfiguration() {
        return configuration;
    }

    public String getName() {
        return name;
    }

    public OIMAdminTreeNode.NODE_STATE getStatus() {
        return status;
    }

    public void setStatus(OIMAdminTreeNode.NODE_STATE status) {
        this.status = status;
    }

    public abstract void handleEvent(EVENT_TYPE event);

    public boolean isLoadable() {
        return getStatus() == OIMAdminTreeNode.NODE_STATE.NOT_INITIALIZED;
    }

    public abstract String getStringRepresentation();

    @Override
    public String toString() {
        return name; //getStringRepresentation();
    }

    public enum NODE_STATE {
        NOT_INITIALIZED, INITIALIZED, INITIALIZED_NO_OP, FAILED, INITIALIZATION_IN_PROGRESS, DESTRUCTION_IN_PROGRESS
    }

    public enum EVENT_TYPE {
        NODE_EXPAND, NODE_COLLAPSE, NODE_DISPLAY
    }

    public static class OIMAdminTreeNodeNoAction extends OIMAdminTreeNode {

        private static final Logger logger = LoggerFactory.getLogger(OIMAdminTreeNodeNoAction.class);
        private String stringRepresentation;

        public OIMAdminTreeNodeNoAction(String name, OIMAdminTreeNode parentNode, UIComponentTree selectionTree) {
            super(name, parentNode.configuration, selectionTree, NODE_STATE.INITIALIZED_NO_OP);
            logger.trace("Entering OIMAdminTreeNodeNoAction({},{},{})", new java.lang.Object[]{name, parentNode, selectionTree});
            stringRepresentation = "OIMAdminTreeNodeNoAction[" + name + "]";
            logger.trace("Completed OIMAdminTreeNodeNoAction({},{},{})", new java.lang.Object[]{name, parentNode, selectionTree});
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
            logger.trace("Ignoring event {}", event);
        }

        @Override
        public String getStringRepresentation() {
            return stringRepresentation;
        }

        public Object getComponent() {
            logger.trace("Returning null value");
            return null;
        }
    }

    public static class DUMMYAdminTreeNode extends OIMAdminTreeNodeNoAction {

        private static final Logger logger = LoggerFactory.getLogger(DUMMYAdminTreeNode.class);

        public DUMMYAdminTreeNode(OIMAdminTreeNode parentNode, UIComponentTree selectionTree) {
            super(DUMMY_LEAF_NODE_NAME, parentNode, selectionTree);
            logger.trace("DUMMYAdminTreeNode({},{})", new Object[]{parentNode, selectionTree});
        }
    }

}