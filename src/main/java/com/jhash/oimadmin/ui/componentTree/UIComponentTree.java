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

import java.util.List;

public interface UIComponentTree {

    void addChildNode(Node parent, Node child);

    List<Node> getChildNodes(Node parent);

    void removeChildNode(Node parent, Node child);

    ROOTAdminTreeNode getRootNode();

    enum EVENT_TYPE {
        NODE_EXPAND, NODE_COLLAPSE, NODE_DISPLAY, NODE_SELECTED, NODE_DESELECTED
    }

    interface Node<T> {
        T getUIObject();

        void handleNodeEvent(EVENT_TYPE event_type);
    }
}
