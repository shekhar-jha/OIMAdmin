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
import com.jhash.oimadmin.ui.UIComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class DisplayComponentNode<M extends DisplayComponentNode, T extends UIComponent<JComponent>> extends AbstractUIComponentTreeNode<M> implements UIComponent<T> {

    private static final Logger logger = LoggerFactory.getLogger(DisplayComponentNode.class);

    private T nodeComponent;

    public DisplayComponentNode(String name, T component, ParentComponent parent) {
        super(name, parent);
        logger.debug("DisplayComponentNode({}, {}, {})", new Object[]{name, component, parent});
        this.nodeComponent = component;
    }

    @Override
    public void setupNode() {
        logger.debug("Initialized display component node {}", this);
        //TODO: Do we need to initialize component here?
    }

    @Override
    public T getComponent() {
        return nodeComponent;
    }

    @Override
    public void destroyNode() {
        logger.debug("Trying to destroy {}", this);
        if (nodeComponent instanceof Service) {
            ((Service) nodeComponent).destroy();
        }
        logger.debug("Destroyed {}", this);
    }

}
