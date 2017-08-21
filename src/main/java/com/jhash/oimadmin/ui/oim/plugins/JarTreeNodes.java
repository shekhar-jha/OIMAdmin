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

package com.jhash.oimadmin.ui.oim.plugins;

import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.plugins.JarManager;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.componentTree.DummyAdminTreeNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JarTreeNodes extends AbstractUIComponentTreeNode<JarTreeNodes> {

    public static final MenuHandler.MENU NEW_JAR = new MenuHandler.MENU("Jar", MenuHandler.MENU.NEW, "New Jar");
    public static final MenuHandler.MENU UPDATE_JAR = new MenuHandler.MENU("Update", MenuHandler.MENU.RUN);
    private static final Logger logger = LoggerFactory.getLogger(JarTreeNodes.class);
    private final JarManager jarManager;
    private final Map<String, DummyAdminTreeNode> jarTypeNodes = new HashMap<>();
    private DummyAdminTreeNode dummyNode = null;

    public JarTreeNodes(JarManager jarManager, String name, ParentComponent parent) {
        super(name, parent);
        this.jarManager = jarManager;
        registerMenu(NEW_JAR, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                JFileChooser jarFileChooser = new JFileChooser();
                jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int returnedResult = jarFileChooser.showDialog(JFrame.getFrames()[0], "Select Jar..");
                if (returnedResult == JFileChooser.APPROVE_OPTION) {
                    File file = jarFileChooser.getSelectedFile();
                    Object selectedItem = JOptionPane.showInputDialog(Frame.getFrames()[0], "Type of Jar selected", " Type of Jar", JOptionPane.QUESTION_MESSAGE, null, new String[]{"JavaTasks", "ScheduleTask", "ThirdParty", "ICFBundle"}, "JavaTasks");
                    if (selectedItem instanceof String) {
                        logger.debug("Registering Jar {} of type {}", file, selectedItem);
                        JarTreeNodes.this.jarManager.registerJar((String) selectedItem, file);
                        addNodeForJar(getNodeForType((String) selectedItem), file.getName());
                        logger.debug("Registered Jar.");
                    } else {
                        displayMessage("Jar registration failed", "Failed to register jar " + file + " since no type was selected", null);
                    }
                }
            }
        });
    }

    public DummyAdminTreeNode getNodeForType(String type) {
        if (Utils.isEmpty(type))
            return null;
        if (!jarTypeNodes.containsKey(type)) {
            jarTypeNodes.put(type, new DummyAdminTreeNode(type, this).initialize());
        }
        return jarTypeNodes.get(type);
    }

    public DummyAdminTreeNode addNodeForJar(final DummyAdminTreeNode typeNode, final String jarName) {
        final DummyAdminTreeNode node = new DummyAdminTreeNode(jarName, typeNode).initialize();
        final String type = typeNode.getName();
        node.registerMenu(DELETE, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                logger.debug("Unregister jar {} of type {}", jarName, type);
                jarManager.unregisterJar(type, jarName);
                logger.debug("Unregistered jar.");
                node.destroy();
            }
        });
        node.registerMenu(UPDATE_JAR, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                logger.debug("Updating jar {} of type {}", jarName, type);
                JFileChooser jarFileChooser = new JFileChooser();
                jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int returnedResult = jarFileChooser.showDialog(JFrame.getFrames()[0], "Select Jar..");
                if (returnedResult == JFileChooser.APPROVE_OPTION) {
                    File file = jarFileChooser.getSelectedFile();
                    jarManager.updateJar(type, jarName, file);
                }
                logger.debug("Updated jar.");
            }
        });
        node.registerMenu(MenuHandler.MENU.SAVE, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                logger.debug("Saving jar {} of type {}", jarName, type);
                JFileChooser jarFileChooser = new JFileChooser();
                int returnedResult = jarFileChooser.showSaveDialog(JFrame.getFrames()[0]);
                if (returnedResult == JFileChooser.APPROVE_OPTION) {
                    File file = jarFileChooser.getSelectedFile();
                    jarManager.saveJar(type, jarName, file);
                }
                logger.debug("Saved jar.");
            }
        });
        if (dummyNode != null) {
            dummyNode.destroy();
            dummyNode = null;
        }
        return node;
    }

    @Override
    public void setupNode() {
        logger.debug("Setting up node {}", this);
        registerMenu(REFRESH, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                logger.debug("Refreshing Jar Listing...");
                JarTreeNodes.this.destroy(false);
                JarTreeNodes.this.initialize();
                logger.debug("Refreshed Jar Listing.");
            }
        });
        Map<String, List<String>> jarDetails = jarManager.getRegisteredJars();
        logger.debug("Located jars {}", jarDetails);
        if (jarDetails != null) {
            if (!jarDetails.isEmpty()) {
                for (Map.Entry<String, List<String>> jarItem : jarDetails.entrySet()) {
                    final String type = jarItem.getKey();
                    logger.trace("Processing type {}", type);
                    final DummyAdminTreeNode typeNode = getNodeForType(type);
                    List<String> jarNameList = jarItem.getValue();
                    if (jarNameList != null && jarNameList.size() > 0) {
                        for (String jarName : jarNameList) {
                            final String scopedJarName = jarName;
                            logger.trace("Processing Jar {}", scopedJarName);
                            addNodeForJar(typeNode, scopedJarName);
                        }
                    } else {
                        logger.debug("No Jars associated with type {}. Skipping..", type);
                    }
                }
            } else {
                dummyNode = new DummyAdminTreeNode("No Jars found.", this).initialize();
            }
        } else {
            dummyNode = new DummyAdminTreeNode("Not available", this).initialize();
        }
        logger.debug("Node setup completed.", this);
    }

    @Override
    public void destroyNode() {
        unregisterMenu(REFRESH);
        jarTypeNodes.clear();
        logger.debug("Node destruction completed.");
    }
}
