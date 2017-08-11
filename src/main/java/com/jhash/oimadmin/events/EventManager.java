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

package com.jhash.oimadmin.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class EventManager {

    public static final Event ALL = new Event("ALL");
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private Map<Event, Deque<EventConsumer>> eventConsumers = new HashMap<>();
    private ErrorHandler errorHandler;

    public EventManager setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public EventManager registerEventListener(EventConsumer childComponent) {
        registerEventListener(ALL, childComponent);
        return this;
    }

    public EventManager registerEventListener(Event event, EventConsumer childComponent) {
        logger.debug("Registering listener {} for event {}", childComponent, event);
        if (event == null || childComponent == null)
            return this;
        Deque<EventConsumer> registeredComponents = getRegisteredConsumers(event);
        registeredComponents.addFirst(childComponent);
        logger.debug("Registered event listener.");
        return this;
    }

    private Deque<EventConsumer> getRegisteredConsumers(Event event) {
        Deque<EventConsumer> registeredComponents = eventConsumers.get(event);
        if (registeredComponents == null) {
            registeredComponents = new ArrayDeque<>();
            eventConsumers.put(event, registeredComponents);
        }
        return registeredComponents;
    }

    public void triggerEvent(EventSource source, Event event) {
        logger.debug("Processing event trigger from {} with event {}", new Object[]{source, event});
        if (event == null)
            return;
        Deque<EventConsumer>[] registeredConsumers = new Deque[2];
        registeredConsumers[0] = eventConsumers.get(event);
        registeredConsumers[1] = eventConsumers.get(ALL);
        for (Deque<EventConsumer> listOfEventConsumers : registeredConsumers) {
            if (listOfEventConsumers != null && listOfEventConsumers.size() > 0) {
                for (EventConsumer component : listOfEventConsumers) {
                    try {
                        logger.trace("Triggering event on component {}", component);
                        component.triggerEvent(source, event);
                        logger.trace("Triggered event on component {}", component);
                    } catch (Exception exception) {
                        logger.warn("Catching event trigger processing for " + component + " with value " + source + " event " + event, exception);
                        if (errorHandler != null) {
                            try {
                                errorHandler.handleError(source, event, exception);
                            } catch (Exception errorHandlerException) {
                                logger.warn("Handling the error failed for " + component + " with value " + source + " event" + event + " Exception " + exception, errorHandlerException);
                            }
                        }
                    }
                }
            }
        }
        logger.debug("Processed event trigger.");
    }

    public EventManager unRegisterEventListener(EventConsumer childComponent) {
        logger.debug("Unregister event listener {}", childComponent);
        for (Map.Entry<Event, Deque<EventConsumer>> eventConsumerEntry : eventConsumers.entrySet()) {
            boolean wasRemoved = eventConsumerEntry.getValue().remove(childComponent);
            if (wasRemoved)
                logger.debug("Removed registration of component {} for event {}", childComponent, eventConsumerEntry.getKey());
        }
        logger.debug("Unregistered event listener.");
        return this;
    }

    public EventManager unRegisterEventListener(Event event, EventConsumer childComponent) {
        logger.debug("Unregister event listener {} for event {}", childComponent, event);
        Deque<EventConsumer> eventConsumerList = eventConsumers.get(event);
        if (eventConsumerList != null) {
            boolean wasRemoved = eventConsumerList.remove(childComponent);
            if (wasRemoved)
                logger.debug("Removed registration of component {} for event {}", childComponent, event);
        }
        logger.debug("Unregistered event listener.");
        return this;
    }

    public void clear() {
        logger.debug("Clearing the registered event handlers");
        eventConsumers.clear();
    }

    interface ErrorHandler {
        void handleError(EventSource source, Event event, Exception exception);
    }
}
