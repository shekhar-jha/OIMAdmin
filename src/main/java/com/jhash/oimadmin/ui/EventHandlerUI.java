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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.jsdl.common.builder.FormBuilder;
import com.jgoodies.jsdl.component.JGComponentFactory;
import com.jgoodies.jsdl.component.JGTextField;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.OIMConnection;
import com.jhash.oimadmin.oim.OIMJMXWrapper;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

public class EventHandlerUI extends AbstractUIComponent<JPanel> {

    public static final String[] EVENT_HANDLER_STAGES = {"", "preprocess", "action", "audit",
            "postprocess", "veto", "canceled"};
    public static final String[] EVENT_HANDLER_TYPES = {"validation-handler",
            "action-handler", "failed-handler", "finalization-handler", "change-failed", "out-of-band-handler",
            "compensate-handler"};
    public static final String[] EVENT_HANDLER_TYPES_TOOL_TIPS = {
            "(Custom Supported) Identifies the validations that will be performed on the orchestration.",
            "(Custom Supported- Pre/Post process only) Identifies the operations that will be performed at preprocess, postprocess, and action stages",
            "Identifies the event handlers that will be executed if an event handler in the default flow fails.",
            "Identifies the event handlers to execute at the end of the orchestration. Finalization is the last stage of any orchestration.",
            "Identifies event handlers to be executed in parent orchestration upon consequence orchestration failures.",
            "Defines the event handlers for out-of-band orchestration flows, such as veto and cancel.",
            "Identifies the event handlers that will be executed in the compensation flow of the orchestration."
    };
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerUI.class);
    private final OIMJMXWrapper connection;
    private final OIMConnection oimConnection;

    private JGTextField nameField = new JGTextField(20);
    private JLabel orcTargetLabel = new JLabel("oracle.iam.platform.kernel.vo.EntityOrchestration");
    private JCheckBox syncCheckBox = new JCheckBox();
    private JCheckBox txCheckBox = new JCheckBox();
    private JGTextField classNameText = new JGTextField(80);
    private JGTextField orderField = new JGTextField(20);
    private JComboBox<String> stageComboBox = new JComboBox<String>(EVENT_HANDLER_STAGES);
    private JComboBox<String> entityType = new JComboBox<String>();
    private JComboBox<String> operationType = new JComboBox<String>();
    private JComboBox<String> eventHandlerTypes = new JComboBox<String>(EVENT_HANDLER_TYPES);
    private JPanel eventHandlerUI;
    private UIJavaCompile javaCompiler;
    private EventHandlerConfigurationPanel configurationPanel;
    private EventHandlerPackagePanel packagePanel;

    public EventHandlerUI(String name, OIMJMXWrapper connection, OIMConnection oimConnection, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.connection = connection;
        this.oimConnection = oimConnection;
    }

    @Override
    public boolean destroyComponentOnClose() {
        return true;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {} ...", this);
        javaCompiler = new UIJavaCompile("Source Code", "EventHandlerSource", configuration, selectionTree, displayArea);
        javaCompiler.initialize();
        nameField.setText("CustomEventHandler");
        nameField.setToolTipText("Name of event handler");
        orcTargetLabel
                .setToolTipText("type of orchestration, such as Entity, MDS, Relation, Toplink orchestration.\n The default value is oracle.iam.platform.kernel.vo.EntityOrchestration. This is the only supported type for writing custom event handlers");
        syncCheckBox
                .setToolTipText("If set to TRUE (synchronous), then the kernel expects the event handler to return an EventResult.\n If set to FALSE (asynchronous), then you must return null as the event result and notify the kernel about the event result later.");
        txCheckBox
                .setToolTipText("The tx attribute indicates whether or not the event handler should run in its own transaction.\n Supported values are TRUE or FALSE. By default, the value is FALSE.");
        classNameText.setText("com.jhash.oim.eventhandler.CustomEventHandler");
        classNameText.setToolTipText("Full package name of the Java class that implements the event handler");
        classNameText.getDocument().addDocumentListener(new UIJavaCompile.ConnectTextFieldListener(classNameText, javaCompiler.classNameText));
        orderField
                .setToolTipText("Identifies the order (or sequence) in which the event handler is executed.\n Order value is in the scope of entity, operation, and stage. Order value for each event handler in this scope must be unique. If there is a conflict, then the order in which these conflicted event handlers are executed is arbitrary."
                        + "\nSupported values are FIRST (same as Integer.MIN_VALUE), LAST (same as Integer.MAX_VALUE), or a numeral.");
        final Set<String> entityNames = new HashSet<String>();
        entityNames.addAll(OIMJMXWrapper.OperationDetail.getOperationDetails(connection).keySet());
        entityNames.add("ANY");
        String[] entityNamesArray = entityNames.toArray(new String[0]);
        entityType.setModel(new DefaultComboBoxModel<String>(entityNamesArray));
        entityType
                .setToolTipText("Identifies the type of entity the event handler is executed on. A value of ANY sets the event handler to execute on any entity.");
        operationType
                .setToolTipText("Identifies the type of operation the event handler is executed on. A value of ANY sets the event handler to execute on any operation.");
        entityType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String entityTypeSelected = (String) entityType.getSelectedItem();
                if (entityTypeSelected != null && entityNames.contains(entityTypeSelected)) {
                    try {
                        Set<String> operations = null;
                        Map<String, Set<String>> operationDetails = OIMJMXWrapper.OperationDetail.getOperationDetails(connection);
                        if (operationDetails.containsKey(entityTypeSelected)) {
                            operations = operationDetails.get(entityTypeSelected);
                        } else {
                            operations = new HashSet<String>();
                        }
                        operations.add("ANY");
                        operationType.setModel(new DefaultComboBoxModel<String>(operations.toArray(new String[0])));
                    } catch (Exception exception) {
                        displayMessage("Entity Type selection failed", "Failed to load operation details associated with " + entityTypeSelected, exception);
                    }
                } else {
                    logger.trace("Nothing to do since the selected entity type {} is not recognized",
                            entityTypeSelected);
                }
            }
        });
        eventHandlerTypes.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public JComponent getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                           boolean cellHasFocus) {

                JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (index >= 0 && index < EVENT_HANDLER_TYPES_TOOL_TIPS.length) {
                    list.setToolTipText(EVENT_HANDLER_TYPES_TOOL_TIPS[index]);
                } else {
                    list.setToolTipText("");
                }
                return comp;
            }
        });
        eventHandlerTypes.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedValue;
                if ((selectedValue = (String) eventHandlerTypes.getSelectedItem()) != null
                        && (!selectedValue.isEmpty())) {
                    if (selectedValue.equals("action-handler") || selectedValue.equals("change-failed")) {
                        syncCheckBox.setEnabled(true);
                    } else {
                        syncCheckBox.setEnabled(false);
                        syncCheckBox.setSelected(false);
                    }
                    if (selectedValue.equals("validation-handler")) {
                        syncCheckBox.setSelected(true);
                    }
                    if (Arrays.asList(
                            new String[]{"out-of-band-handler", "action-handler", "compensate-handler",
                                    "finalization-handler"}).contains(selectedValue)) {
                        txCheckBox.setEnabled(true);
                    } else {
                        txCheckBox.setEnabled(false);
                        txCheckBox.setSelected(false);
                    }
                    if (Arrays.asList(new String[]{"out-of-band-handler", "action-handler", "failed-handler"})
                            .contains(selectedValue)) {
                        stageComboBox.setEnabled(true);
                    } else {
                        stageComboBox.setEnabled(false);
                        stageComboBox.setSelectedItem("");
                    }
                } else {
                    logger.trace("Nothing to do since {} item has been selected", selectedValue);
                }
            }
        });
        entityType.setSelectedItem(entityNamesArray[0]);
        // TODO: Is this needed?
        // operationType.setSelectedItem(associatedOperationSplitDetails[1]);
        eventHandlerTypes.setSelectedItem(EVENT_HANDLER_TYPES[0]);
        javaCompiler.classNameText.setText(classNameText.getText());
        configurationPanel = new EventHandlerConfigurationPanel("Configure");
        configurationPanel.initialize();
        packagePanel = new EventHandlerPackagePanel("Package");
        packagePanel.initialize();
        eventHandlerUI = buildEventHandlerUI();
        logger.debug("Initialized {}", this);
    }

    public JPanel buildEventHandlerUI() {
        logger.debug("Trying to create New Event Handler screen");
        JPanel newEventHandlerPanel = new JPanel(new BorderLayout());
        FormLayout eventHandlerFormLayout = new FormLayout("right:pref, 3dlu, pref, 7dlu, right:pref, 3dlu, pref:grow",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu");
        eventHandlerFormLayout.setColumnGroups(new int[][]{{1, 5}});
        CellConstraints cellConstraint = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(eventHandlerFormLayout);
        builder.addLabel("Name", cellConstraint.xy(1, 1));
        builder.add(nameField, cellConstraint.xy(3, 1));
        builder.addLabel("Type", cellConstraint.xy(5, 1));
        builder.add(eventHandlerTypes, cellConstraint.xy(7, 1));

        builder.addLabel("Entity Type", cellConstraint.xy(1, 3));
        builder.add(entityType, cellConstraint.xy(3, 3));
        builder.addLabel("Operation", cellConstraint.xy(5, 3));
        builder.add(operationType, cellConstraint.xy(7, 3));

        builder.addLabel("Order", cellConstraint.xy(1, 5));
        builder.add(orderField, cellConstraint.xy(3, 5));
        builder.addLabel("Type of orchestration", cellConstraint.xy(5, 5));
        builder.add(orcTargetLabel, cellConstraint.xy(7, 5));

        builder.addLabel("Synchronous ?", cellConstraint.xy(1, 7));
        builder.add(syncCheckBox, cellConstraint.xy(3, 7));
        builder.addLabel("Internal Transaction ?", cellConstraint.xy(1, 9));
        builder.add(txCheckBox, cellConstraint.xy(3, 9));

        builder.addLabel("Stage", cellConstraint.xy(5, 7));
        builder.add(stageComboBox, cellConstraint.xy(7, 7));

        builder.addLabel("Class", cellConstraint.xy(1, 11));
        builder.add(classNameText, cellConstraint.xyw(3, 11, 5));
        // builder.addLabel("MDS Location", cellConstraint.xy(1, 13));
        // builder.add(new JGTextField("/custom/eventhandler.xml"),
        // cellConstraint.xyw(3, 13, 5));


        JideTabbedPane newEventHandlerControlPane = new JideTabbedPane();
        newEventHandlerControlPane.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT);
        newEventHandlerControlPane.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
        newEventHandlerControlPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);
        newEventHandlerControlPane.setUseDefaultShowCloseButtonOnTab(false);
        newEventHandlerControlPane.setBoldActiveTab(true);
        newEventHandlerControlPane.setShowCloseButtonOnTab(true);
        newEventHandlerControlPane.addTab(javaCompiler.getName(), javaCompiler.getComponent());
        newEventHandlerControlPane.addTab("Configure", configurationPanel.getComponent());
        newEventHandlerControlPane.addTab("Package", packagePanel.getComponent());

        JideSplitPane eventHandlerSplitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        eventHandlerSplitPane.add(builder.build(), 0);
        eventHandlerSplitPane.add(newEventHandlerControlPane, 1);
        eventHandlerSplitPane.setProportionalLayout(true);
        eventHandlerSplitPane.setProportions(new double[]{0.3});
        newEventHandlerPanel.add(eventHandlerSplitPane);
        logger.debug("Completed creation of New Event Handler screen");
        return newEventHandlerPanel;
    }

    @Override
    public JPanel getDisplayComponent() {
        return eventHandlerUI;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying {}...", this);
        if (javaCompiler != null) {
            javaCompiler.destroy();
            javaCompiler = null;
        }
        if (configurationPanel != null) {
            configurationPanel.destroy();
            configurationPanel = null;
        }
        if (packagePanel != null) {
            packagePanel.destroy();
            packagePanel = null;
        }
        logger.debug("Destroyed  {}", this);
    }

    public class EventHandlerConfigurationPanel extends AbstractUIComponent<JideSplitPane> {

        JideSplitPane configurationSplitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        private JTextArea pluginXMLTextArea = new JTextArea();
        private JButton pluginXMLGenerateButton = JGComponentFactory.getCurrent().createButton("Generate..");
        private JTextArea eventHandlerXMLTextArea = new JTextArea(Utils.readFile("templates/eventHandlerxml"));
        private JButton eventHandlerXMLGenerateButton = JGComponentFactory.getCurrent().createButton("Generate..");

        public EventHandlerConfigurationPanel(String name) {
            super(name, false, EventHandlerUI.this.configuration, EventHandlerUI.this.selectionTree, EventHandlerUI.this.displayArea);
        }

        @Override
        public void initializeComponent() {
            logger.debug("Initializing {}...", this);
            pluginXMLTextArea.setText(Utils.readFile("templates/pluginxml", configuration.getWorkArea()));

            JPanel pluginXMLButtonPanel = new JPanel();
            pluginXMLGenerateButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    pluginXMLTextArea.setText(Utils.processString(pluginXMLTextArea.getText(), new String[][]{
                            {"CLASSNAME", classNameText.getText()}, {"NAME", nameField.getText()}}));
                }
            });
            pluginXMLButtonPanel.add(pluginXMLGenerateButton);
            JPanel pluginXMLPanel = new JPanel(new BorderLayout());
            pluginXMLPanel.add(new JideScrollPane(pluginXMLTextArea), BorderLayout.CENTER);
            pluginXMLPanel.add(pluginXMLButtonPanel, BorderLayout.NORTH);

            eventHandlerXMLTextArea.setText(Utils.readFile("templates/eventHandlerxml"));
            JPanel eventHandlerXMLButtonPanel = new JPanel();
            eventHandlerXMLGenerateButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    eventHandlerXMLTextArea.setText(Utils.processString(eventHandlerXMLTextArea.getText(),
                            new String[][]{
                                    {"CLASS_NAME", classNameText.getText()},
                                    {"NAME", nameField.getText()},
                                    {"EVENT_HANDLER", (String) eventHandlerTypes.getSelectedItem()},
                                    {"ENTITY", (String) entityType.getSelectedItem()},
                                    {"OPERATION", (String) operationType.getSelectedItem()},
                                    {"STAGE", (String) stageComboBox.getSelectedItem()},
                                    {"ORDER", orderField.getText()},
                                    {"SYNC", (syncCheckBox.isSelected() ? "TRUE" : "FALSE")}}));
                }
            });
            eventHandlerXMLButtonPanel.add(eventHandlerXMLGenerateButton);
            JPanel eventHandlerXMLPanel = new JPanel(new BorderLayout());
            eventHandlerXMLPanel.add(new JideScrollPane(eventHandlerXMLTextArea), BorderLayout.CENTER);
            eventHandlerXMLPanel.add(eventHandlerXMLButtonPanel, BorderLayout.NORTH);

            configurationSplitPane.add(pluginXMLPanel, 0);
            configurationSplitPane.add(eventHandlerXMLPanel, 1);
            configurationSplitPane.setProportionalLayout(true);
            configurationSplitPane.setProportions(new double[]{0.5});
            logger.debug("Initialized {}", this);
        }

        @Override
        public JideSplitPane getDisplayComponent() {
            return configurationSplitPane;
        }

        @Override
        public void destroyComponent() {
            logger.debug("Destroyed {}...", this);
        }

    }

    public class EventHandlerPackagePanel extends AbstractUIComponent<JPanel> {

        JLabel jarFileLocationLabel = new JLabel();
        JLabel pluginFileLocationLabel = new JLabel();
        JButton generateJarFromClass = JGComponentFactory.getCurrent().createButton("Generate Jar");
        JButton selectJarButton = JGComponentFactory.getCurrent().createButton("Select existing jar");
        JFileChooser selectJar;
        JButton prepareButton = JGComponentFactory.getCurrent().createButton("Create Plugin");
        JButton selectPrepareButton = JGComponentFactory.getCurrent().createButton("Select existing plugin");
        JButton registerPlugin = JGComponentFactory.getCurrent().createButton("Register");
        JPanel packagePanel;
        JButton unregisterPlugin = JGComponentFactory.getCurrent().createButton("Unregister");

        String eventHandlerPluginZip;
        String eventHandlerCodeJar;


        public EventHandlerPackagePanel(String name) {
            super(name, false, EventHandlerUI.this.configuration, EventHandlerUI.this.selectionTree, EventHandlerUI.this.displayArea);
        }

        @Override
        public void initializeComponent() {
            logger.debug("Initializing {}...", this);
            eventHandlerCodeJar = configuration.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP + File.separator
                    + "eventHandler" + System.currentTimeMillis() + ".jar";
            generateJarFromClass.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        File eventHandlerCodeJarFile = new File(eventHandlerCodeJar);
                        if (eventHandlerCodeJarFile.exists()) {
                            FileUtils.forceDelete(eventHandlerCodeJarFile);
                        }
                        Utils.createJarFileFromDirectory(javaCompiler.getOutputDirectory(), eventHandlerCodeJar);
                        jarFileLocationLabel.setText(eventHandlerCodeJar);
                    } catch (Exception exception) {
                        displayMessage("Packaging Event handler jar Failed", "Failed to create jar " + eventHandlerCodeJar + " with Event Handler code available in " + javaCompiler.getOutputDirectory() + " directory", exception);
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
                        Map<String, byte[]> content = new HashMap<String, byte[]>();
                        content.put("plugin.xml", configurationPanel.pluginXMLTextArea.getText().getBytes());
                        File eventHandlerJarFile = new File(jarFileLocationLabel.getText());
                        content.put("lib/EventHandler.jar", FileUtils.readFileToByteArray(eventHandlerJarFile));
                        String eventHandlerDetailFile = "META-INF/" + nameField.getText() + ".xml";
                        content.put(eventHandlerDetailFile, configurationPanel.eventHandlerXMLTextArea.getText().getBytes());
                        Utils.createJarFileFromContent(content, new String[]{"plugin.xml", "lib/EventHandler.jar", eventHandlerDetailFile}, eventHandlerPluginZip);
                        pluginFileLocationLabel.setText(eventHandlerPluginZip);
                    } catch (Exception exception) {
                        displayMessage("Creating plugin zip failed", "Failed to create Event Handler Plugin zip file " + eventHandlerPluginZip, exception);
                    }
                }
            });
            registerPlugin.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        oimConnection.registerPlugin(FileUtils.readFileToByteArray(new File(pluginFileLocationLabel.getText())));
                    } catch (Exception exception) {
                        displayMessage("Plugin registeration failed", "Failed to register plugin " + pluginFileLocationLabel.getText(), exception);
                    }
                }
            });
            unregisterPlugin.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String pluginName = classNameText.getText();
                    try {
                        oimConnection.unregisterPlugin(pluginName);
                    } catch (Exception exception) {
                        displayMessage("Unregister plugin failed", "Failed to unregister plugin " + pluginName, exception);
                    }
                }
            });

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
                        eventHandlerCodeJarFile.delete();
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
                        eventHandlerPluginZipFile.delete();
                    }
                } catch (Exception exception) {
                    logger.warn("Could not delete event handler plugin zip file " + eventHandlerPluginZip, exception);
                }
                eventHandlerPluginZip = null;
            }
            logger.debug("Destroyed {}", this);
        }

    }
}
