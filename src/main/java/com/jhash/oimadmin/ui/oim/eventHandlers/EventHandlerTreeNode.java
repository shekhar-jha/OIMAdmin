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

package com.jhash.oimadmin.ui.oim.eventHandlers;

import com.jhash.oimadmin.oim.eventHandlers.Manager;
import com.jhash.oimadmin.oim.eventHandlers.OperationDetail;
import com.jhash.oimadmin.ui.UIComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandlerTreeNode extends AbstractUIComponentTreeNode<EventHandlersTreeNode> implements UIComponent<EventHandlerDetails> {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlerTreeNode.class);
    private final OperationDetail eventHandlerDetails;
    private final Manager eventHandlerManager;
    private EventHandlerDetails eventHandlerDetailsUI;

    public EventHandlerTreeNode(Manager manager, OperationDetail eventHandlerDetails, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.eventHandlerDetails = eventHandlerDetails;
        this.eventHandlerManager = manager;
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing {}", this);
        eventHandlerDetailsUI = new EventHandlerDetails(eventHandlerManager, eventHandlerDetails, getName(), this);
        logger.debug("Initialized {}", this);
    }

    @Override
    public EventHandlerDetails getComponent() {
        return eventHandlerDetailsUI;
    }

    @Override
    public void destroyNode() {
        logger.debug("Destroying {}", this);
        if (eventHandlerDetailsUI != null) {
            try {
                eventHandlerDetailsUI.destroy();
            } catch (Exception exception) {
                logger.warn("Failed to destroy Event Handler Details UI " + eventHandlerDetailsUI, exception);
            }
            eventHandlerDetailsUI = null;
        }
        logger.debug("Destroyed {}", this);
    }

}
