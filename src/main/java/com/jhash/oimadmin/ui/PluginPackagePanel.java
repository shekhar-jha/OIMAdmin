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

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.jsdl.common.builder.FormBuilder;
import com.jgoodies.jsdl.component.JGComponentFactory;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.plugins.PluginManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PluginPackagePanel extends AbstractUIComponent<JPanel, PluginPackagePanel> {

    private static final Logger logger = LoggerFactory.getLogger(PluginPackagePanel.class);
    private final PluginManager pluginManager;
    private String eventHandlerPluginZip;
    private String eventHandlerCodeJar;
    private JLabel jarFileLocationLabel = new JLabel();
    private JLabel pluginFileLocationLabel = new JLabel();
    private JButton generateJarFromClass = JGComponentFactory.getCurrent().createButton("Generate Jar");
    private JButton selectJarButton = JGComponentFactory.getCurrent().createButton("Select existing jar");
    private JFileChooser selectJar;
    private JButton prepareButton = JGComponentFactory.getCurrent().createButton("Create Plugin");
    private JButton selectPrepareButton = JGComponentFactory.getCurrent().createButton("Select existing plugin");
    private JButton registerPlugin = JGComponentFactory.getCurrent().createButton("Register");
    private JPanel packagePanel;
    private JButton unregisterPlugin = JGComponentFactory.getCurrent().createButton("Unregister");

    public PluginPackagePanel(PluginManager pluginManager, String name, AbstractUIComponent parent) {
        super(name, parent);
        this.pluginManager = pluginManager;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}...", this);
        eventHandlerCodeJar = configuration.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP + File.separator
                + "eventHandler" + System.currentTimeMillis() + ".jar";
        generateJarFromClass.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String rootDirectory = executeCallback(EventHandlerUI.JAR_ROOT_FOLDER, null);
                try {
                    File eventHandlerCodeJarFile = new File(eventHandlerCodeJar);
                    if (eventHandlerCodeJarFile.exists()) {
                        FileUtils.forceDelete(eventHandlerCodeJarFile);
                    }
                    Utils.createJarFileFromDirectory(rootDirectory, eventHandlerCodeJar);
                    jarFileLocationLabel.setText(eventHandlerCodeJar);
                } catch (Exception exception) {
                    displayMessage("Packaging Event handler jar Failed", "Failed to create jar " + eventHandlerCodeJar + " with Event Handler code available in " + rootDirectory + " directory", exception);
                }
            }
        });
        selectJar = new JFileChooser(configuration.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP);
        selectJarButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int returnCode = selectJar.showDialog(null, "Select EventHandler Jar");
                switch (returnCode) {
                    case JFileChooser.APPROVE_OPTION:
                        jarFileLocationLabel.setText(selectJar.getSelectedFile().getAbsolutePath());
                        break;
                }
            }
        });
        selectPrepareButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnCode = selectJar.showDialog(null, "Select Plugin File");
                switch (returnCode) {
                    case JFileChooser.APPROVE_OPTION:
                        pluginFileLocationLabel.setText(selectJar.getSelectedFile().getAbsolutePath());
                        break;
                }
            }
        });
        eventHandlerPluginZip = configuration.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP + File.separator
                + "EventHandlerPlugin" + System.currentTimeMillis() + ".zip";
        prepareButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File eventHandlerPluginZipFile = new File(eventHandlerPluginZip);
                    if (eventHandlerPluginZipFile.exists()) {
                        FileUtils.forceDelete(eventHandlerPluginZipFile);
                    }
                    Map<String, byte[]> content = new HashMap<>();
                    content.put("plugin.xml", executeCallback(EventHandlerUI.PLUGIN_DEFINITION, "").getBytes());
                    File eventHandlerJarFile = new File(jarFileLocationLabel.getText());
                    content.put("lib/EventHandler.jar", FileUtils.readFileToByteArray(eventHandlerJarFile));
                    String eventHandlerDetailFile = "META-INF/" + executeCallback(EventHandlerUI.NAME, "") + ".xml";
                    content.put(eventHandlerDetailFile, executeCallback(EventHandlerUI.EVENT_HANDLER_DEF, "").getBytes());
                    Utils.createJarFileFromContent(content, new String[]{"plugin.xml", "lib/EventHandler.jar", eventHandlerDetailFile}, eventHandlerPluginZip);
                    pluginFileLocationLabel.setText(eventHandlerPluginZip);
                } catch (Exception exception) {
                    displayMessage("Creating plugin zip failed", "Failed to create Event Handler Plugin zip file " + eventHandlerPluginZip, exception);
                }
            }
        });
        if (pluginManager != null) {
            registerPlugin.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        pluginManager.registerPlugin(FileUtils.readFileToByteArray(new File(pluginFileLocationLabel.getText())));
                    } catch (Exception exception) {
                        displayMessage("Plugin registration failed", "Failed to register plugin " + pluginFileLocationLabel.getText(), exception);
                    }
                }
            });
        } else {
            registerPlugin.setEnabled(false);
            registerPlugin.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    displayMessage("Plugin registration failed", "No OIM Connection is available to register plugin.", null);
                }
            });
        }
        if (pluginManager != null) {
            unregisterPlugin.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String pluginName = executeCallback(EventHandlerUI.CLASSNAME, null);
                    try {
                        pluginManager.unregisterPlugin(pluginName);
                    } catch (Exception exception) {
                        displayMessage("Unregister plugin failed", "Failed to unregister plugin " + pluginName, exception);
                    }
                }
            });
        } else {
            unregisterPlugin.setEnabled(false);
            unregisterPlugin.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    displayMessage("Unregister plugin failed", "No OIM Connection is available to unregister plugin " + executeCallback(EventHandlerUI.CLASSNAME, "Not provided"), null);
                }
            });

        }

        packagePanel = FormBuilder.create().columns("right:pref, 3dlu, pref:grow")
                .rows("p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p").border(Borders.DIALOG)
                .add(generateJarFromClass).xy(1, 1).add(selectJarButton).xy(1, 3).add(jarFileLocationLabel).xy(3, 3)
                .add(prepareButton).xy(1, 5).add(selectPrepareButton).xy(1, 7).add(pluginFileLocationLabel).xy(3, 7)
                .add(registerPlugin).xy(1, 9).add(unregisterPlugin).xy(1, 11).build();
        logger.debug("Initialized {}", this);
    }

    @Override
    public JPanel getDisplayComponent() {
        return packagePanel;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying {}...", this);
        if (this.eventHandlerCodeJar != null) {
            try {
                File eventHandlerCodeJarFile = new File(eventHandlerCodeJar);
                if (eventHandlerCodeJarFile.exists() && eventHandlerCodeJarFile.isFile()) {
                    if (eventHandlerCodeJarFile.delete()) {
                        logger.debug("Successfully deleted file {}", eventHandlerCodeJarFile);
                    } else {
                        logger.warn("Failed to delete file {}", eventHandlerCodeJarFile);
                    }
                }
            } catch (Exception exception) {
                logger.warn("Could not delete jar file " + eventHandlerCodeJar + " that contains event handler's compiled code", exception);
            }
            eventHandlerCodeJar = null;
        }
        if (eventHandlerPluginZip != null) {
            try {
                File eventHandlerPluginZipFile = new File(eventHandlerPluginZip);
                if (eventHandlerPluginZipFile.exists() && eventHandlerPluginZipFile.isFile()) {
                    if (eventHandlerPluginZipFile.delete()) {
                        logger.debug("Successfully deleted file {}", eventHandlerPluginZipFile);
                    } else {
                        logger.warn("Failed to delete file {}", eventHandlerPluginZipFile);
                    }
                }
            } catch (Exception exception) {
                logger.warn("Could not delete event handler plugin zip file " + eventHandlerPluginZip, exception);
            }
            eventHandlerPluginZip = null;
        }
        logger.debug("Destroyed {}", this);
    }

}
