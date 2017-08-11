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

package com.jhash.oimadmin.ui.component;

import com.jhash.oimadmin.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventEnabledServiceComponentImpl<T extends EventEnabledServiceComponentImpl> extends ServiceComponentImpl<T> implements EventConsumer, EventListenerRegistrar<T> {

    private static final Logger logger = LoggerFactory.getLogger(EventEnabledServiceComponentImpl.class);
    private final EventManager eventManager = new EventManager();

    public EventEnabledServiceComponentImpl(String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        parentComponent.registerEventListener(this);
    }

    public EventEnabledServiceComponentImpl(String name, ParentComponent parentComponent, STATE state) {
        super(name, parentComponent, state);
        parentComponent.registerEventListener(this);
    }

    @Override
    public void triggerEvent(EventSource parent, Event event) {
        logger.debug("Received event {} from {}", event, parent);
        if (event == INITIALIZE && getState() == NOT_INITIALIZED)
            initialize();
        if (event == DESTROY && getState() == INITIALIZED)
            destroy();
        boolean dontPropagate = false;
        try {
            dontPropagate = handleEvent(parent, event);
        } catch (Exception exception) {
            logger.warn("Failed to handle event " + event + " from " + parent, exception);
        } finally {
            if (!dontPropagate) {
                eventManager.triggerEvent(parent, event);
            }
        }
        logger.debug("Processed event {} from {}", event, parent);
    }

    public abstract boolean handleEvent(EventSource parent, Event event);

    @Override
    public T registerEventListener(EventConsumer childComponent) {
        eventManager.registerEventListener(childComponent);
        return (T) this;
    }

    @Override
    public T registerEventListener(Event event, EventConsumer childComponent) {
        eventManager.registerEventListener(event, childComponent);
        return (T) this;
    }

    @Override
    public T unRegisterEventListener(EventConsumer childComponent) {
        eventManager.unRegisterEventListener(childComponent);
        return (T) this;
    }

    @Override
    public T unRegisterEventListener(Event event, EventConsumer childComponent) {
        eventManager.unRegisterEventListener(event, childComponent);
        return (T) this;
    }


    @Override
    public void initializeComponent() {

    }

    @Override
    public void destroyComponent() {
        try {
            eventManager.clear();
        } catch (Exception exception) {
            logger.warn("Failed to clear event manager " + eventManager, exception);
        }
    }
}
