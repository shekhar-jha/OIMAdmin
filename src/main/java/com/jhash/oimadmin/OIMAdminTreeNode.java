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

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class OIMAdminTreeNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;
    public final String name;
    public final OIMAdminTreeNode.NODE_TYPE type;
    public final Configuration configuration;
    private OIMAdminTreeNode.NODE_STATE status = OIMAdminTreeNode.NODE_STATE.NOT_INITIALIZED;

    public OIMAdminTreeNode(String name, OIMAdminTreeNode.NODE_TYPE type, Configuration configuration) {
        super(name);
        if (name == null || name.isEmpty())
            throw new NullPointerException("Can not create a tree node with value " + name);
        this.name = name;
        this.type = type;
        this.configuration = configuration;
    }

    public OIMAdminTreeNode.NODE_STATE getStatus() {
        return status;
    }

    public void setStatus(OIMAdminTreeNode.NODE_STATE status) {
        this.status = status;
    }

    public abstract <T> T getValue();

    public abstract void handleEvent(EVENT_TYPE event);

    public boolean isDisplayable() {
        return getStatus() == OIMAdminTreeNode.NODE_STATE.INITIALIZED;
    }

    public enum NODE_TYPE {
        CONNECTION, DUMMY, ROOT, SCHEDULED_TASK, MDS, MDS_PARTITION, MDS_FILE, EVENT_HANDLER, EVENT_HANDLER_OPERATION
    }

    public enum NODE_STATE {
        NOT_INITIALIZED, INITIALIZED, INITIALIZED_NO_OP, FAILED, INITIALIZATION_IN_PROGRESS
    }

    public enum EVENT_TYPE {
        NODE_EXPAND, NODE_COLLAPSE, NODE_DISPLAY
    }

    public static class OIMAdminTreeNodeNoAction extends OIMAdminTreeNode {

        public OIMAdminTreeNodeNoAction(String name, OIMAdminTreeNode.NODE_TYPE type, Configuration configuration) {
            super(name, type, configuration);
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
        }

        public <Object> Object getValue() {
            return null;
        }
    }
}