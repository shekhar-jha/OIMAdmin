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

import com.jhash.oimadmin.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public abstract class ServiceComponentImpl<T extends ServiceComponentImpl> extends BaseComponentImpl<T> implements Service<T>, ParentComponent<T> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceComponentImpl.class);
    private STATE state = NOT_INITIALIZED;

    public ServiceComponentImpl(String name, ParentComponent parentComponent) {
        this(name, parentComponent, NOT_INITIALIZED);
    }

    public ServiceComponentImpl(String name, ParentComponent parentComponent, STATE state) {
        super(name, parentComponent);
        this.state = state;
    }

    @Override
    public T initialize() {
        logger.debug("Trying to initialize component");
        if (getState() == Service.INITIALIZATION_IN_PROGRESS) {
            logger.warn("Service {} is already being initialized, ignoring the trigger", this);
            return (T) this;
        }
        if (getState() == Service.INITIALIZED) {
            logger.debug("Nothing to do since service {} is already initialized.", this);
            return (T) this;
        }
        setState(Service.INITIALIZATION_IN_PROGRESS);
        try {
            initializeComponent();
            setState(Service.INITIALIZED);
            logger.debug("Initialized UI Component");
        } catch (Exception exception) {
            displayMessage("Failed to initialize " + getName(), "", exception);
            logger.warn("Failed to initialize component " + this, exception);
            logger.debug("Setting node status as {}", Service.FAILED);
            setState(Service.FAILED);
        }
        return (T) this;
    }

    public abstract void initializeComponent();

    @Override
    public STATE getState() {
        return state;
    }

    protected T setState(STATE state) {
        this.state = state;
        return (T) this;
    }

    @Override
    public void destroy() {
        logger.debug("Trying to destroy {}", this);
        STATE currentState = getState();
        if (currentState == Service.INITIALIZED || currentState == Service.INITIALIZED_NO_OP) {
            logger.debug("Service in {} state, setting status to {} before destroying", currentState, Service.DESTRUCTION_IN_PROGRESS);
            setState(DESTRUCTION_IN_PROGRESS);
            try {
                destroyComponent();
                logger.debug("Completed destruction");
            } catch (Exception exception) {
                logger.warn("Failed to complete the node specific destruction process", exception);
            }
            logger.debug("Setting status to {}", Service.NOT_INITIALIZED);
            setState(Service.NOT_INITIALIZED);
        } else {
            logger.debug("Skipping destroy since the node is not in {} state", Arrays.asList(Service.INITIALIZED, Service.INITIALIZED_NO_OP));
        }
    }

    public abstract void destroyComponent();
}
