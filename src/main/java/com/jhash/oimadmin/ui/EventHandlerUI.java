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
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.jsdl.component.JGComponentFactory;
import com.jgoodies.jsdl.component.JGTextField;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.code.Java;
import com.jhash.oimadmin.oim.eventHandlers.Manager;
import com.jhash.oimadmin.oim.plugins.PluginManager;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventHandlerUI extends AbstractUIComponent<JPanel, EventHandlerUI> {

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
    public static final ID<String, String, UIUtils.TextFieldCallback> CLASSNAME = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.TextFieldCallback> NAME = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.ComboBoxCallback<String>> EVENT_HANDLERTYPE = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.ComboBoxCallback<String>> ENTITY_TYPE = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.ComboBoxCallback<String>> OPERATION_TYPE = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.ComboBoxCallback<String>> STAGE = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.TextFieldCallback> ORDER = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.CheckBoxCallback<String>> SYNC = new CLASS_ID<>();
    public static final ID<String, String, Callback<String, String>> JAR_ROOT_FOLDER = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.TextFieldCallback> EVENT_HANDLER_DEF = new CLASS_ID<>();
    public static final ID<String, String, UIUtils.TextFieldCallback> PLUGIN_DEFINITION = new CLASS_ID<>();
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerUI.class);

    private final Manager eventHandlerManager;
    private final PluginManager pluginManager;
    private Java java;
    private JGTextField nameField = new JGTextField(20);
    private JLabel orcTargetLabel = new JLabel("oracle.iam.platform.kernel.vo.EntityOrchestration");
    private JCheckBox syncCheckBox = new JCheckBox();
    private JCheckBox txCheckBox = new JCheckBox();
    private JGTextField classNameText = new JGTextField(80);
    private JGTextField orderField = new JGTextField(20);
    private JComboBox<String> stageComboBox = new JComboBox<>(EVENT_HANDLER_STAGES);
    private JComboBox<String> entityType = new JComboBox<>();
    private JComboBox<String> operationType = new JComboBox<>();
    private JComboBox<String> eventHandlerTypes = new JComboBox<>(EVENT_HANDLER_TYPES);
    private JPanel eventHandlerUI;
    private UIJavaCompile javaCompiler;
    private EventHandlerConfigurationPanel configurationPanel;
    private PluginPackagePanel packagePanel;

    public EventHandlerUI(Manager manager, PluginManager pluginManager, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.eventHandlerManager = manager;
        this.pluginManager = pluginManager;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {} ...", this);
        java = new Java();
        javaCompiler = new UIJavaCompile(java, eventHandlerManager.getVersion(), "EventHandlerSource", "Source Code", this).initialize();
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
        final Map<String, Set<String>> operationDetails = eventHandlerManager.getAvailableEventHandlers();
        final Set<String> entityNames = new HashSet<>();
        entityNames.addAll(operationDetails.keySet());
        entityNames.add("ANY");
        String[] entityNamesArray = entityNames.toArray(new String[0]);
        entityType.setModel(new DefaultComboBoxModel<>(entityNamesArray));
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
                        Set<String> operations;
                        if (operationDetails.containsKey(entityTypeSelected)) {
                            operations = operationDetails.get(entityTypeSelected);
                        } else {
                            operations = new HashSet<>();
                        }
                        operations.add("ANY");
                        operationType.setModel(new DefaultComboBoxModel<>(operations.toArray(new String[0])));
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
        configurationPanel = new EventHandlerConfigurationPanel("Configure", this)
                .registerCallback(CLASSNAME, new UIUtils.TextFieldCallback(classNameText))
                .registerCallback(NAME, new UIUtils.TextFieldCallback(nameField))
                .registerCallback(EVENT_HANDLERTYPE, new UIUtils.ComboBoxCallback<>(eventHandlerTypes, String.class))
                .registerCallback(ENTITY_TYPE, new UIUtils.ComboBoxCallback<>(entityType, String.class))
                .registerCallback(OPERATION_TYPE, new UIUtils.ComboBoxCallback<>(operationType, String.class))
                .registerCallback(STAGE, new UIUtils.ComboBoxCallback<>(stageComboBox, String.class))
                .registerCallback(ORDER, new UIUtils.TextFieldCallback(orderField))
                .registerCallback(SYNC, new UIUtils.CheckBoxCallback<>(syncCheckBox, "TRUE", "FALSE"))
                .initialize();
        configurationPanel.initialize();
        packagePanel = new PluginPackagePanel(pluginManager, "Package", this)
                .registerCallback(CLASSNAME, new UIUtils.TextFieldCallback(classNameText))
                .registerCallback(NAME, new UIUtils.TextFieldCallback(nameField))
                .registerCallback(CLASSNAME, new UIUtils.TextFieldCallback(classNameText))
                .registerCallback(EVENT_HANDLER_DEF, new UIUtils.TextFieldCallback(configurationPanel.eventHandlerXMLTextArea))
                .registerCallback(PLUGIN_DEFINITION, new UIUtils.TextFieldCallback(configurationPanel.pluginXMLTextArea))
                .registerCallback(JAR_ROOT_FOLDER, new Callback<String, String>() {
                    @Override
                    public String call(String value) {
                        return javaCompiler.getOutputDirectory();
                    }
                })
                .initialize();
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

    public static class EventHandlerConfigurationPanel extends AbstractUIComponent<JideSplitPane, EventHandlerConfigurationPanel> {

        JideSplitPane configurationSplitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        private JTextArea pluginXMLTextArea = new JTextArea();
        private JButton pluginXMLGenerateButton = JGComponentFactory.getCurrent().createButton("Generate..");
        private JTextArea eventHandlerXMLTextArea = new JTextArea(Utils.readFile("templates/eventHandlerxml"));
        private JButton eventHandlerXMLGenerateButton = JGComponentFactory.getCurrent().createButton("Generate..");

        public EventHandlerConfigurationPanel(String name, AbstractUIComponent parent) {
            super(name, parent);
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
                            {"CLASSNAME", executeCallback(CLASSNAME, "")}, {"NAME", executeCallback(NAME, "")
                    }}));
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
                                    {"CLASS_NAME", executeCallback(CLASSNAME, "")},
                                    {"NAME", executeCallback(NAME, "")},
                                    {"EVENT_HANDLER", executeCallback(EVENT_HANDLERTYPE, "")},
                                    {"ENTITY", executeCallback(ENTITY_TYPE, "")},
                                    {"OPERATION", executeCallback(OPERATION_TYPE, "")},
                                    {"STAGE", executeCallback(STAGE, "")},
                                    {"ORDER", executeCallback(ORDER, "")},
                                    {"SYNC", executeCallback(SYNC, "")}}));
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

}
