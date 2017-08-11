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

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.ui.DisplayArea;
import com.jhash.oimadmin.ui.componentTree.UIComponentTree;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseComponentImpl<T extends BaseComponentImpl> implements NamedComponent, BaseComponent {

    private static final Logger logger = LoggerFactory.getLogger(BaseComponentImpl.class);
    private final String name;
    private final ParentComponent parentComponent;
    private final String internalRepresentation;

    public BaseComponentImpl(String name, ParentComponent parentComponent) {
        if (parentComponent == null) {
            throw new OIMAdminException("No parent component provided to create component " + this.getClass());
        }
        if (Utils.isEmpty(name))
            throw new OIMAdminException("Name of the component " + this.getClass() + " can not be null");
        this.name = name;
        this.parentComponent = parentComponent;
        internalRepresentation = parentComponent.toString() + "> " + name + "[" + super.toString() + "]}";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStringRepresentation() {
        return internalRepresentation;
    }

    @Override
    public String toString() {
        return internalRepresentation;
    }

    @Override
    public ParentComponent getParent() {
        return parentComponent;
    }

    @Override
    public Config.Configuration getConfiguration() {
        return parentComponent.getConfiguration();
    }

    @Override
    public UIComponentTree getUIComponentTree() {
        return parentComponent.getUIComponentTree();
    }

    @Override
    public DisplayArea getDisplayArea() {
        return parentComponent.getDisplayArea();
    }

    @Override
    public MenuHandler getMenuHandler() {
        return parentComponent.getMenuHandler();
    }

    @Override
    public void displayMessage(String title, String message, Exception exception) {
        UIUtils.displayMessage(title, message, exception);
    }

}
