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

package com.jhash.oimadmin.ui.menu;

import com.jhash.oimadmin.EnumClass;

import javax.swing.*;

public interface MenuHandler {

    void register(MENU menuItem);

    void register(MENU menuItem, Context context);

    void register(MENU menuItem, ActionHandler actionHandler);

    void register(MENU menuItem, Context context, ActionHandler actionHandler);

    void unregister(MENU menuItem, Context context, ActionHandler actionHandler);

    interface ActionHandler {
        void invoke(MENU menuItem, Context context);
    }

    interface Context {
        boolean isActive();

        void displayMessage(String message);

        void displayMessage(String message, Exception exception);
    }

    abstract class GlobalContext implements Context {
        public boolean isActive() {
            return true;
        }
    }

    class MENU extends EnumClass {
        public static MENU FILE = new MENU("File");
        public static MENU EDIT = new MENU("Edit");
        public static MENU NAVIGATE = new MENU("Navigate");
        public static MENU RUN = new MENU("Run");
        public static MENU WINDOWS = new MENU("Window");

        public static MENU NEW = new MENU("New", FILE);
        public static MENU SAVE = new MENU("Save", FILE);

        public final MENU parent;
        public final KeyStroke keyStroke;
        public final String popupMenuName;

        public MENU(String menuId) {
            this(menuId, null, null, menuId);
        }

        public MENU(String menuId, String popupMenuName) {
            this(menuId, null, null, popupMenuName);
        }

        public MENU(String menuId, MENU parent) {
            this(menuId, parent, null, menuId);
        }

        public MENU(String menuId, MENU parent, String popupMenuName) {
            this(menuId, parent, null, popupMenuName);
        }

        public MENU(String menuId, MENU parent, KeyStroke keyStroke) {
            this(menuId, parent, keyStroke, menuId);
        }

        public MENU(String menuId, MENU parent, KeyStroke keyStroke, String popupMenuName) {
            super(menuId, parent == null?menuId:menuId + "<" + parent);
            this.popupMenuName = popupMenuName;
            this.parent = parent;
            this.keyStroke = keyStroke;
        }

        public static MENU[] getRootMenus() {
            return new MENU[]{FILE, EDIT, NAVIGATE, RUN, WINDOWS};
        }
    }
}
