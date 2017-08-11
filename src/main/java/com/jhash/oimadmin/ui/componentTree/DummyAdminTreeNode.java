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

import com.jhash.oimadmin.service.Service;
import com.jhash.oimadmin.ui.component.ParentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyAdminTreeNode extends AbstractUIComponentTreeNode<DummyAdminTreeNode> {

    private static final Logger logger = LoggerFactory.getLogger(DummyAdminTreeNode.class);

    //TODO: Work on making sure that Dummy node does not trigger NPE.
    public DummyAdminTreeNode(String name, ParentComponent parent) {
        super(name, parent, Service.INITIALIZED_NO_OP);
        logger.trace("DummyAdminTreeNode()");
    }

    @Override
    public void setupNode() {
        logger.trace("Node initialized.");
    }

    @Override
    public void destroyNode() {
        logger.trace("Node destroyed.");
    }

    @Override
    public void handleNodeEvent(UIComponentTree.EVENT_TYPE event) {
        logger.trace("Ignoring event {}", event);
    }

}
