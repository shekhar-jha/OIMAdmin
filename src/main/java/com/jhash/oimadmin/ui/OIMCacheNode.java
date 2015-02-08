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

package com.jhash.oimadmin.ui;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.oim.MDSConnectionJMX;
import com.jhash.oimadmin.oim.OIMConnection;
import com.jidesoft.swing.JidePopupMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OIMCacheNode extends AbstractUIComponentTreeNode<Object> implements ContextMenuEnabledNode<JPopupMenu>,DisplayableNode<OIMCacheDetails> {

    private static final Logger logger = LoggerFactory.getLogger(OIMCacheNode.class);

    final private OIMConnection connection;
    private JPopupMenu popupMenu;
    private OIMCacheDetails cacheUI;

    public OIMCacheNode(String name, OIMConnection connection, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.connection = connection;
    }


    @Override
    public void initializeComponent() {
        JMenuItem purgeAll = new JMenuItem("Purge All");
        purgeAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connection.purgeCache(null);
            }
        });
        popupMenu = new JidePopupMenu();
        popupMenu.add(purgeAll);
        cacheUI = new OIMCacheDetails(name, configuration, selectionTree, displayArea);
    }


    public boolean hasContextMenu(){
        return popupMenu != null?true:false;
    }

    public JPopupMenu getContextMenu() {
        return popupMenu;
    }

    @Override
    public Object getComponent() {
        return null;
    }

    @Override
    public OIMCacheDetails getDisplayComponent() {
        return cacheUI;
    }

    @Override
    public void destroyComponent() {
        if (popupMenu != null) {
            popupMenu = null;
        }
        if (cacheUI != null) {
            cacheUI.destroy();
            cacheUI = null;
        }
    }

}
