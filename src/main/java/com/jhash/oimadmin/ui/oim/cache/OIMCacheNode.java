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

package com.jhash.oimadmin.ui.oim.cache;

import com.jhash.oimadmin.oim.OIMConnection;
import com.jhash.oimadmin.oim.cache.CacheManager;
import com.jhash.oimadmin.ui.UIComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class OIMCacheNode extends AbstractUIComponentTreeNode<OIMCacheNode> implements UIComponent<OIMCacheDetails>{

    public static final MenuHandler.MENU PURGE_ALL = new MenuHandler.MENU("Purge All", MenuHandler.MENU.RUN, KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
    private static final Logger logger = LoggerFactory.getLogger(OIMCacheNode.class);
    final private OIMConnection connection;
    final private CacheManager cacheManager;
    private OIMCacheDetails cacheUI;

    public OIMCacheNode(CacheManager cacheManager, OIMConnection connection, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.cacheManager = cacheManager;
        this.connection = connection;
    }


    @Override
    public void setupNode() {
        if (connection != null) {
            registerMenu(PURGE_ALL, new MenuHandler.ActionHandler() {
                @Override
                public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                    connection.purgeCache(null);
                }
            });
        }
        cacheUI = new OIMCacheDetails(cacheManager, getName(), this);
    }


    @Override
    public OIMCacheDetails getComponent() {
        return cacheUI;
    }

    @Override
    public void destroyNode() {
        if (cacheUI != null) {
            cacheUI.destroy();
            cacheUI = null;
        }
    }

}
