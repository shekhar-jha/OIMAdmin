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

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.events.Event;
import com.jhash.oimadmin.events.EventConsumer;
import com.jhash.oimadmin.events.EventSource;
import com.jhash.oimadmin.service.Service;
import com.jhash.oimadmin.ui.component.BaseComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class VirtualNode<T extends VirtualNode.VirtualNodeComponent> implements Service<VirtualNode>, EventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VirtualNode.class);
    private final ParentComponent parentComponent;
    private final Class<T> bindingClass;
    private final Method initializeNodeComponent;
    private final Method destroyNodeComponent;

    private final List<MenuItem> registeredMenuItems = new ArrayList<>();
    private Service.STATE state = NOT_INITIALIZED;

    public VirtualNode(ParentComponent parentComponent, Class<T> bindingClass) {
        logger.trace("Entering VirtualNode({}, {})", parentComponent, bindingClass);
        if (parentComponent == null || bindingClass == null)
            throw new OIMAdminException("Either parent component (" + parentComponent + ") or Binding class (" + bindingClass + ") has not been provided.");
        destroyNodeComponent = Utils.getMethod(bindingClass, "destroyNodeComponent", VirtualNode.class);
        initializeNodeComponent = Utils.getMethod(bindingClass, "initializeNodeComponent", VirtualNode.class);
        if (VirtualNodeComponent.class.isAssignableFrom(bindingClass)
                && initializeNodeComponent != null && Modifier.isStatic(initializeNodeComponent.getModifiers()) && Modifier.isPublic(initializeNodeComponent.getModifiers())
                && (destroyNodeComponent == null || (Modifier.isStatic(destroyNodeComponent.getModifiers()) && Modifier.isPublic(destroyNodeComponent.getModifiers())))) {
            this.bindingClass = bindingClass;
        } else {
            throw new OIMAdminException("Please ensure that binding class (" + bindingClass
                    + ") for implements VirtualNodeComponent interface and contains 'static initializeNodeComponent(VirtualNode)' and optional 'static destroyNodeComponent(VirtualNode)' methods.");
        }
        this.parentComponent = parentComponent;
        parentComponent.registerEventListener(this);
        logger.trace("Exiting VirtualNode");
    }

    public ParentComponent getParentComponent() {
        return parentComponent;
    }

    public VirtualNode registerGlobalMenu(MenuHandler.MENU menu, MenuHandler.ActionHandler actionHandler) {
        logger.debug("Registering Handler {} for menu {} with global context", actionHandler, menu);
        MenuItem menuItem = new MenuItem(menu, new GlobalMenuContext(menu), actionHandler);
        registeredMenuItems.add(menuItem);
        return this;
    }

    @Override
    public void triggerEvent(EventSource source, Event event) {
        if (event == DESTROY && state == INITIALIZED)
            destroy();
        if (event == INITIALIZE && state == NOT_INITIALIZED)
            initialize();
    }

    @Override
    public VirtualNode<T> initialize() {
        //TODO: ensure common implementation with ServiceComponentImpl.
        logger.trace("Initializing...");
        state = INITIALIZATION_IN_PROGRESS;
        try {
            try {
                this.initializeNodeComponent.invoke(null, this);
            } catch (Exception exception) {
                throw new OIMAdminException("Initialization of binding class " + bindingClass + " failed.", exception);
            }
            if (!registeredMenuItems.isEmpty()) {
                for (MenuItem menuItem : registeredMenuItems) {
                    parentComponent.getMenuHandler().register(menuItem.menu, menuItem.context, menuItem.actionHandler);
                }
            }
            state = INITIALIZED;
            logger.trace("Initialized.");
        } catch (Exception exception) {
            logger.warn("Failed to initialize virtual node for binding class " + bindingClass, exception);
            state = FAILED;
        }
        return this;
    }

    @Override
    public STATE getState() {
        return state;
    }

    @Override
    public void destroy() {
        //TODO: ensure common implementation with ServiceComponentImpl.
        logger.trace("Destroying...");
        state = DESTRUCTION_IN_PROGRESS;
        try {
            if (destroyNodeComponent != null) {
                try {
                    this.destroyNodeComponent.invoke(null, this);
                } catch (Exception exception) {
                    logger.warn("Destruction of binding class " + bindingClass + " failed.", exception);
                }
            } else {
                logger.debug("No method destroyNodeComponent is associated with binding class {}", bindingClass);
            }
            if (!registeredMenuItems.isEmpty()) {
                for (MenuItem menuItem : registeredMenuItems) {
                    parentComponent.getMenuHandler().unregister(menuItem.menu, menuItem.context, menuItem.actionHandler);
                }
                registeredMenuItems.clear();
            }
        } catch (Exception exception) {
            logger.warn("Failed to destroy virtual node for binding class " + bindingClass, exception);
        } finally {
            state = NOT_INITIALIZED;
        }
        logger.trace("Destroyed.");
    }

    public interface VirtualNodeComponent {
        // TODO: With JDK 1.8 this can made static with basic implementation to be overwritten by implementing classes.
        // At this time it is assumed that VirtualNodeComponent implements these methods.
        //static void initializeNodeComponent(VirtualNode virtualNode);

        //static void destroyNodeComponent(VirtualNode virtualNode);
    }

    private static class MenuItem {
        public final MenuHandler.MENU menu;
        public final MenuHandler.Context context;
        public final MenuHandler.ActionHandler actionHandler;
        private final String stringRepresentation;

        public MenuItem(MenuHandler.MENU menu, MenuHandler.Context context, MenuHandler.ActionHandler actionHandler) {
            this.menu = menu;
            this.actionHandler = actionHandler;
            this.context = context;
            stringRepresentation = "MenuItem(" + menu + ", " + context + ", " + actionHandler + ")";
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }

    private class GlobalMenuContext extends MenuHandler.GlobalContext {

        private final MenuHandler.MENU menu;

        GlobalMenuContext(MenuHandler.MENU menu) {
            this.menu = menu;
        }

        @Override
        public void displayMessage(String message) {
            displayMessage(message, null);
        }

        @Override
        public void displayMessage(String message, Exception exception) {
            if (parentComponent instanceof BaseComponent)
                ((BaseComponent) parentComponent).displayMessage(menu + " Failed", message, exception);
            else
                UIUtils.displayMessage(menu + " Failed", message, exception);
        }
    }
}
