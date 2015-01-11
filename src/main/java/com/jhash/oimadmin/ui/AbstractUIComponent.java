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

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponent;
import com.jhash.oimadmin.UIComponentTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public abstract class AbstractUIComponent<T extends JComponent> extends JPanel implements UIComponent<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractUIComponent.class);
    protected final String name;
    protected final Config.Configuration configuration;
    protected final UIComponentTree selectionTree;
    protected final DisplayArea displayArea;
    protected final boolean publish;
    private COMPONENT_STATE status = COMPONENT_STATE.NOT_INITIALIZED;
    public AbstractUIComponent(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        this(name, true, configuration, selectionTree, displayArea);
    }


    public AbstractUIComponent(String name, boolean publish, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        this.name = name;
        this.configuration = configuration;
        this.selectionTree = selectionTree;
        this.displayArea = displayArea;
        this.publish = publish;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Config.Configuration getConfiguration() {
        return configuration;
    }

    public COMPONENT_STATE getStatus() {
        return status;
    }

    public void setStatus(COMPONENT_STATE status) {
        this.status = status;
    }

    public boolean destroyComponentOnClose() {
        return false;
    }

    @Override
    public UIComponent initialize() {
        logger.debug("Trying to initialize UI Component");
        if (getStatus() == COMPONENT_STATE.INITIALIZATION_IN_PROGRESS) {
            logger.warn("Trying to initialize UI Component {} which is already being initialized, ignoring the trigger", this);
            return this;
        }
        if (getStatus() == COMPONENT_STATE.INITIALIZED) {
            if (publish)
                displayArea.add(this);
            logger.debug("Nothing to do since component {} is already initialized.", this);
            return this;
        }
        setStatus(COMPONENT_STATE.INITIALIZATION_IN_PROGRESS);
        try {
            initializeComponent();
            if (publish)
                displayArea.add(this);
            setStatus(COMPONENT_STATE.INITIALIZED);
            logger.debug("Initialized UI Component");
        } catch (Exception exception) {
            logger.warn("Failed to initialize the component " + this, exception);
            logger.debug("Setting node status as ", COMPONENT_STATE.FAILED);
            setStatus(COMPONENT_STATE.FAILED);
        }
        return this;
    }

    public abstract void initializeComponent();

    public abstract void destroyComponent();

    @Override
    public abstract T getComponent();

    @Override
    public void destroy() {
        logger.debug("Trying to destroy {}", this);
        if (getStatus() == COMPONENT_STATE.INITIALIZED) {
            logger.debug("Component in {} state, setting status to {} before destroying", getStatus(), COMPONENT_STATE.DESTRUCTION_IN_PROGRESS);
            setStatus(COMPONENT_STATE.DESTRUCTION_IN_PROGRESS);
            try {
                if (publish)
                    displayArea.remove(this);
                destroyComponent();
                logger.debug("Completed component destruction");
            } catch (Exception exception) {
                logger.warn("Failed to complete the component specific destruction process", exception);
            }
            logger.debug("Setting status to {}", COMPONENT_STATE.NOT_INITIALIZED);
            setStatus(COMPONENT_STATE.NOT_INITIALIZED);
        } else {
            logger.debug("Skipping destroy since the component is not in {} state", COMPONENT_STATE.INITIALIZED);
        }
    }

    public enum COMPONENT_STATE {
        NOT_INITIALIZED, INITIALIZED, INITIALIZED_NO_OP, FAILED, INITIALIZATION_IN_PROGRESS, DESTRUCTION_IN_PROGRESS
    }
}
