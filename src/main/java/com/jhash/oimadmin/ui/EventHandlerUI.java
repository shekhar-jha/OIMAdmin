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
import com.jgoodies.jsdl.component.JGStripedTable;
import com.jgoodies.jsdl.component.JGTextField;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.OIMAdminTreeNode;
import com.jhash.oimadmin.oim.OIMConnection;
import com.jhash.oimadmin.oim.OIMJMXWrapper;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.zip.CRC32;
import java.util.zip.ZipOutputStream;

public class EventHandlerUI extends AbstractUIComponent {
    public static final String UI_COMPONENT_NAME = "EventHandler UI";
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerUI.class);

    @Override
    public String getName() {
        return UI_COMPONENT_NAME;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initialized Event Handler UI...");
    }

    private Object loadEventHandler(EventHandlerRootTreeNode node, JTree selectionTree, JTabbedPane displayArea) {
        DefaultTreeModel model = (DefaultTreeModel) selectionTree.getModel();
        OIMJMXWrapper tmpConnection = node.getValue();
        tmpConnection.initialize(node.configuration);
        Set<OIMJMXWrapper.OperationDetail> operations = tmpConnection.getOperations();
        OIMJMXWrapper.OperationDetail[] sortedOperationDetail = operations.toArray(new OIMJMXWrapper.OperationDetail[]{});
        Arrays.sort(sortedOperationDetail, new Comparator<OIMJMXWrapper.OperationDetail>() {

            @Override
            public int compare(OIMJMXWrapper.OperationDetail o1, OIMJMXWrapper.OperationDetail o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
        for (OIMJMXWrapper.OperationDetail operation : sortedOperationDetail) {
            OIMAdminTreeNode treeNode = new EventHandlerTreeNode(operation.description + "(" + operation.name + ")", operation, config,
                    node.configuration, selectionTree, displayArea);
            treeNode.setStatus(OIMAdminTreeNode.NODE_STATE.INITIALIZED);
            model.insertNodeInto(treeNode, node, node.getChildCount());
        }
        return tmpConnection;
    }

    public JComponent displayEventHandlerDetails(EventHandlerTreeNode node, JTree selectionTree, JTabbedPane displayArea) {
        logger.debug("Trying to display Event Handler details associated with node {}", node);
        Object eventHandlerDetailsObject = node.getValue();
        logger.debug("Trying to validate if value {} attached with node is instance of OperationDetail",
                eventHandlerDetailsObject, node);
        if (eventHandlerDetailsObject instanceof OIMJMXWrapper.OperationDetail) {
            final OIMJMXWrapper.OperationDetail eventHandlerDetails = (OIMJMXWrapper.OperationDetail) eventHandlerDetailsObject;
            TreeNode parentTreeNode;
            OIMAdminTreeNode parentNode;
            OIMJMXWrapper connection = null;
            logger.debug("Trying to validate whether the parent node (typically should be \"EventHandlers\" has OIMJMXWrapper connection associated");
            if (((parentTreeNode = node.getParent()) != null) && (parentTreeNode instanceof OIMAdminTreeNode)
                    && (parentNode = (OIMAdminTreeNode) parentTreeNode).getValue() != null
                    && parentNode.getValue() instanceof OIMJMXWrapper) {
                connection = (OIMJMXWrapper) parentNode.getValue();
                logger.debug(
                        "Trying to get all event handlers associated with event handler {} using OIMJMXWrapper {}",
                        eventHandlerDetails, connection);
                final OIMJMXWrapper.Details details = connection.getEventHandlers(eventHandlerDetails);

                DefaultTableModel tableModel = new DefaultTableModel(details.getData(), details.getColumns());
                JGStripedTable table = JGComponentFactory.getCurrent().createReadOnlyTable(tableModel);
                JLabel nameLabel = JGComponentFactory.getCurrent().createLabel();
                JLabel stageLabel = JGComponentFactory.getCurrent().createLabel();
                JLabel orderLabel = JGComponentFactory.getCurrent().createLabel();
                JLabel customLabel = JGComponentFactory.getCurrent().createLabel();
                JLabel conditionalLabel = JGComponentFactory.getCurrent().createLabel();
                JLabel offbandLabel = JGComponentFactory.getCurrent().createLabel();
                JLabel classNameLabel = JGComponentFactory.getCurrent().createLabel();
                JLabel locationLabel = JGComponentFactory.getCurrent().createLabel();
                table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting())
                            return;
                        int selectedIndex = table.getSelectedRow();
                        Map<String, Object> detail = details.getItemAt(selectedIndex);
                        nameLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.NAME.name).toString());
                        stageLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.STAGE.name).toString());
                        orderLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.ORDER.name).toString());
                        customLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.CUSTOM.name).toString());
                        conditionalLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.CONDITIONAL.name).toString());
                        offbandLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.OFFBAND.name).toString());
                        classNameLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.CLASS.name).toString());
                        locationLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.LOCATION.name).toString());
                    }
                });
                JPanel eventHandlerDetailPanel = new JPanel(new BorderLayout());
                JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
                FormLayout eventHandlerFormLayout = new FormLayout(
                        "right:pref, 3dlu, pref, 7dlu, right:pref, 3dlu, pref:grow",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu");
                eventHandlerFormLayout.setColumnGroups(new int[][]{{1, 5}});
                CellConstraints cellConstraint = new CellConstraints();
                PanelBuilder builder = new PanelBuilder(eventHandlerFormLayout);
                builder.addLabel("Name", cellConstraint.xy(1, 1));
                builder.add(nameLabel, cellConstraint.xy(3, 1));
                builder.addLabel("Custom", cellConstraint.xy(5, 1));
                builder.add(customLabel, cellConstraint.xy(7, 1));
                builder.addLabel("Stage", cellConstraint.xy(1, 3));
                builder.add(stageLabel, cellConstraint.xy(3, 3));
                builder.addLabel("Order", cellConstraint.xy(5, 3));
                builder.add(orderLabel, cellConstraint.xy(7, 3));
                builder.addLabel("Conditional", cellConstraint.xy(1, 5));
                builder.add(conditionalLabel, cellConstraint.xy(3, 5));
                builder.addLabel("Off Band", cellConstraint.xy(5, 5));
                builder.add(offbandLabel, cellConstraint.xy(7, 5));
                builder.addLabel("Location", cellConstraint.xy(1, 7));
                builder.add(locationLabel, cellConstraint.xyw(3, 7, 5));
                builder.addLabel("Class", cellConstraint.xy(1, 9));
                builder.add(classNameLabel, cellConstraint.xyw(3, 9, 5));

                splitPane.add(new JideScrollPane(table), 0);
                splitPane.add(builder.getPanel(), 1);
                splitPane.setProportionalLayout(true);
                splitPane.setProportions(new double[]{0.6});
                eventHandlerDetailPanel.add(splitPane, BorderLayout.CENTER);

                JideButton newHandlerButton = new JideButton("New Event Handler");
                newHandlerButton.setActionCommand("NEWEVENT");
                newHandlerButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            logger.debug("Invoked NEWEVENT");
                            executeDisplayService(node, displayArea, new ExecuteCommand<JComponent>() {
                                @Override
                                public JComponent run() {
                                    return displayNewEventHandler(node);
                                }
                            });
                            logger.debug("Completed command on node");
                        } catch (Exception exception) {
                            logger.warn("Failed to perform {} on the display panel for {}",
                                    new Object[]{e.getActionCommand(), node.name}, exception);
                        }
                    }
                });
                JPanel buttonPanel = new JPanel();
                buttonPanel.add(newHandlerButton);
                eventHandlerDetailPanel.add(buttonPanel, BorderLayout.NORTH);

                logger.debug("Returning the developed component {}", eventHandlerDetailPanel);
                return eventHandlerDetailPanel;
            } else {
                logger.debug("Failed to locate OIMJMXWrapper (located {}) connection in parent node {}", connection,
                        parentTreeNode);
            }
        } else {
            logger.debug("Value associated with node {} is not an instance of OperationDetail as expected. Located {}",
                    node, eventHandlerDetailsObject);
        }
        logger.debug("Returning null component");
        return null;
    }

    public JComponent displayNewEventHandler(OIMAdminTreeNode node) {
        logger.debug("Trying to create New Event Handler screen with node {}", node);
        if (node == null || node.type != OIMAdminTreeNode.NODE_TYPE.EVENT_HANDLER_OPERATION
                || (!(node.getValue() instanceof OIMJMXWrapper.OperationDetail))) {
            logger.debug("Nothing to do since the node {} is not of type {} or does not contain OperationDetail",
                    new Object[]{node, OIMAdminTreeNode.NODE_TYPE.EVENT_HANDLER_OPERATION, node.getValue()});
            return null;
        }
        OIMJMXWrapper.OperationDetail associatedOperation = (OIMJMXWrapper.OperationDetail) node.getValue();
        String[] associatedOperationSplitDetails = associatedOperation.name.split("-");
        JPanel newEventHandlerPanel = new JPanel(new BorderLayout());
        JTextArea sourceCode = JGComponentFactory.getCurrent().createTextArea();
        String templateString = Utils.readFile("templates/EventHandlerSourceCode");
        sourceCode.setText(templateString);
        JGTextField nameField = new JGTextField(20);
        nameField.setText("CustomEventHandler");
        nameField.setToolTipText("Name of event handler");
        JLabel orcTargetLabel = new JLabel("oracle.iam.platform.kernel.vo.EntityOrchestration");
        orcTargetLabel
                .setToolTipText("type of orchestration, such as Entity, MDS, Relation, Toplink orchestration.\n The default value is oracle.iam.platform.kernel.vo.EntityOrchestration. This is the only supported type for writing custom event handlers");
        JCheckBox syncCheckBox = new JCheckBox();
        syncCheckBox
                .setToolTipText("If set to TRUE (synchronous), then the kernel expects the event handler to return an EventResult.\n If set to FALSE (asynchronous), then you must return null as the event result and notify the kernel about the event result later.");
        JCheckBox txCheckBox = new JCheckBox();
        txCheckBox
                .setToolTipText("The tx attribute indicates whether or not the event handler should run in its own transaction.\n Supported values are TRUE or FALSE. By default, the value is FALSE.");
        JGTextField classNameText = new JGTextField(80);
        classNameText.setText("com.jhash.oim.eventhandler.CustomEventHandler");
        classNameText.setToolTipText("Full package name of the Java class that implements the event handler");
        JGTextField orderField = new JGTextField(20);
        orderField
                .setToolTipText("Identifies the order (or sequence) in which the event handler is executed.\n Order value is in the scope of entity, operation, and stage. Order value for each event handler in this scope must be unique. If there is a conflict, then the order in which these conflicted event handlers are executed is arbitrary."
                        + "\nSupported values are FIRST (same as Integer.MIN_VALUE), LAST (same as Integer.MAX_VALUE), or a numeral.");
        JComboBox<String> stageComboBox = new JComboBox<String>(new String[]{"", "preprocess", "action", "audit",
                "postprocess", "veto", "canceled"});

        final Set<String> entityNames = new HashSet<String>();
        entityNames.addAll(associatedOperation.getOperationDetails().keySet());
        entityNames.add("ANY");
        Set<String> operationNames = associatedOperation.getOperationDetails().get(associatedOperationSplitDetails[0]);
        JComboBox<String> entityType = new JComboBox<String>(entityNames.toArray(new String[0]));
        JComboBox<String> operationType = new JComboBox<String>(operationNames.toArray(new String[0]));
        entityType
                .setToolTipText("Identifies the type of entity the event handler is executed on. A value of ANY sets the event handler to execute on any entity.");
        operationType
                .setToolTipText("Identifies the type of operation the event handler is executed on. A value of ANY sets the event handler to execute on any operation.");
        entityType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String entityTypeSelected = (String) entityType.getSelectedItem();
                if (entityTypeSelected != null && entityNames.contains(entityTypeSelected)) {
                    Set<String> operations = null;
                    if (associatedOperation.getOperationDetails().containsKey(entityTypeSelected)) {
                        operations = associatedOperation.getOperationDetails().get(entityTypeSelected);
                    } else {
                        operations = new HashSet<String>();
                    }
                    operations.add("ANY");
                    operationType.setModel(new DefaultComboBoxModel<String>(operations.toArray(new String[0])));
                } else {
                    logger.trace("Nothing to do since the selected entity type {} is not recognized",
                            entityTypeSelected);
                }
            }
        });
        JComboBox<String> eventHandlerTypes = new JComboBox<String>(new String[]{"validation-handler",
                "action-handler", "failed-handler", "finalization-handler", "change-failed", "out-of-band-handler",
                "compensate-handler"});
        eventHandlerTypes.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public JComponent getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                           boolean cellHasFocus) {

                JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                switch (index) {
                    case 0:
                        list.setToolTipText("(Custom Supported) Identifies the validations that will be performed on the orchestration.");
                        break;
                    case 1:
                        list.setToolTipText("(Custom Supported- Pre/Post process only) Identifies the operations that will be performed at preprocess, postprocess, and action stages");
                        break;
                    case 2:
                        list.setToolTipText("Identifies the event handlers that will be executed if an event handler in the default flow fails.");
                        break;
                    case 3:
                        list.setToolTipText("Identifies the event handlers to execute at the end of the orchestration. Finalization is the last stage of any orchestration.");
                        break;
                    case 4:
                        list.setToolTipText("Identifies event handlers to be executed in parent orchestration upon consequence orchestration failures.");
                        break;
                    case 5:
                        list.setToolTipText("Defines the event handlers for out-of-band orchestration flows, such as veto and cancel.");
                        break;
                    case 6:
                        list.setToolTipText("Identifies the event handlers that will be executed in the compensation flow of the orchestration.");
                        break;
                    default:
                        list.setToolTipText("");
                        break;
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
        entityType.setSelectedItem(associatedOperationSplitDetails[0]);
        operationType.setSelectedItem(associatedOperationSplitDetails[1]);
        eventHandlerTypes.setSelectedItem("validation-handler");
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

        JideSplitPane sourceCodePanel = new JideSplitPane();
        sourceCodePanel.add(new JideScrollPane(sourceCode), 0);
        JTextArea compileResultTextArea = JGComponentFactory.getCurrent().createReadOnlyTextArea();
        JButton compileButton = JGComponentFactory.getCurrent().createButton("Compile..");
        String outputDirectory = config.getWorkArea() + Config.VAL_WORK_AREA_CLASSES;
        compileButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    DiagnosticCollector<JavaFileObject> returnedValue = Utils.compileJava(classNameText.getText(),
                            sourceCode.getText(), outputDirectory);

                    if (returnedValue != null) {
                        StringBuilder message = new StringBuilder();
                        for (Diagnostic<?> d : returnedValue.getDiagnostics()) {
                            switch (d.getKind()) {
                                case ERROR:
                                    message.append("ERROR: " + d.toString() + "\n");
                                default:
                                    message.append(d.toString() + "\n");
                            }
                        }
                        compileResultTextArea.setText(message.toString());
                    } else {
                        compileResultTextArea.setText("Compilation successful");
                    }

                } catch (Exception exception) {
                    logger.warn("Failed to perform operation associated with compile button", exception);
                }
            }
        });
        JPanel sourceCodeButtonPanel = new JPanel();
        sourceCodeButtonPanel.add(compileButton);
        JPanel sourceCodeControlPanel = new JPanel(new BorderLayout());
        sourceCodePanel.add(sourceCodeControlPanel, 1);
        sourceCodePanel.setProportionalLayout(true);
        sourceCodePanel.setProportions(new double[]{0.7});
        sourceCodeControlPanel.add(sourceCodeButtonPanel, BorderLayout.NORTH);
        sourceCodeControlPanel.add(new JideScrollPane(compileResultTextArea), BorderLayout.CENTER);

        JTextArea pluginxmlTextArea = new JTextArea(Utils.readFile("templates/pluginxml"));
        JPanel pluginxmlButtonPanel = new JPanel();
        JButton pluginxmlGenerateButton = JGComponentFactory.getCurrent().createButton("Generate..");
        pluginxmlGenerateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pluginxmlTextArea.setText(Utils.readFile("templates/pluginxml", new String[][]{
                        {"CLASSNAME", classNameText.getText()}, {"NAME", nameField.getText()}}));
            }
        });
        pluginxmlButtonPanel.add(pluginxmlGenerateButton);
        JPanel pluginxmlPanel = new JPanel(new BorderLayout());
        pluginxmlPanel.add(new JideScrollPane(pluginxmlTextArea), BorderLayout.CENTER);
        pluginxmlPanel.add(pluginxmlButtonPanel, BorderLayout.NORTH);

        JTextArea eventHandlerXMLTextArea = new JTextArea(Utils.readFile("templates/eventHandlerxml"));
        JPanel eventHandlerXMLButtonPanel = new JPanel();
        JButton eventHandlerXMLGenerateButton = JGComponentFactory.getCurrent().createButton("Generate..");
        eventHandlerXMLGenerateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                eventHandlerXMLTextArea.setText(Utils.readFile("templates/eventHandlerxml",
                        new String[][]{{"CLASS_NAME", classNameText.getText()}, {"NAME", nameField.getText()},
                                {"EVENT_HANDLER", (String) eventHandlerTypes.getSelectedItem()},
                                {"ENTITY", (String) entityType.getSelectedItem()},
                                {"OPERATION", (String) operationType.getSelectedItem()},
                                {"STAGE", (String) stageComboBox.getSelectedItem()},
                                {"ORDER", orderField.getText()},
                                {"SYNC", (syncCheckBox.isSelected() ? "TRUE" : "FALSE")}}));
            }
        });
        eventHandlerXMLButtonPanel.add(eventHandlerXMLGenerateButton);
        JPanel eventHandlerxmlPanel = new JPanel(new BorderLayout());
        eventHandlerxmlPanel.add(new JideScrollPane(eventHandlerXMLTextArea), BorderLayout.CENTER);
        eventHandlerxmlPanel.add(eventHandlerXMLButtonPanel, BorderLayout.NORTH);

        JideSplitPane configurationSplitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        configurationSplitPane.add(pluginxmlPanel, 0);
        configurationSplitPane.add(eventHandlerxmlPanel, 1);
        configurationSplitPane.setProportionalLayout(true);
        configurationSplitPane.setProportions(new double[]{0.5});

        JLabel jarFileLocationLabel = new JLabel();
        JButton generateJarFromClass = JGComponentFactory.getCurrent().createButton("Generate Jar");
        String jarFileLocation = config.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP + File.separator
                + "eventHandler" + System.currentTimeMillis() + ".jar";
        generateJarFromClass.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.generateJarFile(outputDirectory, jarFileLocation);
                jarFileLocationLabel.setText(jarFileLocation);
            }
        });
        JFileChooser selectJar = new JFileChooser(config.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP);
        JButton selectJarButton = JGComponentFactory.getCurrent().createButton("Select existing jar");
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
        JButton prepareButton = JGComponentFactory.getCurrent().createButton("Create Plugin");
        String jarFileName = config.getWorkArea() + File.separator + Config.VAL_WORK_AREA_TMP + File.separator
                + "EventHandlerPlugin" + System.currentTimeMillis() + ".zip";
        prepareButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CRC32 crc = new CRC32();
                try (ZipOutputStream jarFileOutputStream = new ZipOutputStream(new FileOutputStream(jarFileName))) {
                    jarFileOutputStream.setMethod(ZipOutputStream.STORED);
                    byte[] pluginXMLFileContent = pluginxmlTextArea.getText().getBytes();
                    JarEntry pluginXMLFileEntry = new JarEntry("plugin.xml");
                    pluginXMLFileEntry.setTime(System.currentTimeMillis());
                    pluginXMLFileEntry.setSize(pluginXMLFileContent.length);
                    pluginXMLFileEntry.setCompressedSize(pluginXMLFileContent.length);
                    crc.update(pluginXMLFileContent);
                    pluginXMLFileEntry.setCrc(crc.getValue());
                    jarFileOutputStream.putNextEntry(pluginXMLFileEntry);
                    jarFileOutputStream.write(pluginXMLFileContent);
                    jarFileOutputStream.closeEntry();

                    File eventHandlerJarFile = new File(jarFileLocationLabel.getText());
                    byte[] eventHandlerJarFilebytes = FileUtils.readFileToByteArray(eventHandlerJarFile);
                    JarEntry newFileEntry = new JarEntry("lib/EventHandler.jar");
                    newFileEntry.setTime(System.currentTimeMillis());
                    long fileLength = eventHandlerJarFilebytes.length;
                    newFileEntry.setSize(fileLength);
                    newFileEntry.setCompressedSize(fileLength);
                    crc = new CRC32();
                    crc.update(eventHandlerJarFilebytes);
                    newFileEntry.setCrc(crc.getValue());
                    jarFileOutputStream.putNextEntry(newFileEntry);
                    jarFileOutputStream.write(eventHandlerJarFilebytes);
                    jarFileOutputStream.closeEntry();

                    byte[] eventHandlerXMLFileEntryContent = eventHandlerXMLTextArea.getText().getBytes();
                    JarEntry eventHandlerXMLFileEntry = new JarEntry("META-INF/" + nameField.getText() + ".xml");
                    eventHandlerXMLFileEntry.setTime(System.currentTimeMillis());
                    eventHandlerXMLFileEntry.setSize(eventHandlerXMLFileEntryContent.length);
                    eventHandlerXMLFileEntry.setCompressedSize(eventHandlerXMLFileEntryContent.length);
                    crc = new CRC32();
                    crc.update(eventHandlerXMLFileEntryContent);
                    eventHandlerXMLFileEntry.setCrc(crc.getValue());
                    jarFileOutputStream.putNextEntry(eventHandlerXMLFileEntry);
                    jarFileOutputStream.write(eventHandlerXMLFileEntryContent);
                    jarFileOutputStream.closeEntry();
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to create the plugin " + jarFileName, exception);
                }
            }
        });
        JButton registerPlugin = JGComponentFactory.getCurrent().createButton("Register");
        registerPlugin.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TreeNode[] parents = node.getPath();
                for (TreeNode parent : parents) {
                    if (parent instanceof OIMAdminTreeNode && ((OIMAdminTreeNode) parent).type == OIMAdminTreeNode.NODE_TYPE.CONNECTION) {
                        OIMAdminTreeNode connectionNode = (OIMAdminTreeNode) parent;
                        if (connectionNode.getValue() != null && connectionNode.getValue() instanceof OIMConnection) {
                            OIMConnection oimConnection = (OIMConnection) connectionNode.getValue();
                            try {
                                oimConnection.registerPlugin(FileUtils.readFileToByteArray(new File(jarFileName)));
                            } catch (Exception exception) {
                                throw new OIMAdminException(
                                        "Failed to read file " + jarFileName + " into a byte array", exception);
                            }
                        } else {
                            logger.debug(
                                    "Nothing to do since connection node {} does not contain OIMConnection value {}",
                                    connectionNode, connectionNode.getValue());
                        }
                    } else {
                        logger.debug("Node in the path {} is not of type {}. Ignoring", parent, OIMAdminTreeNode.NODE_TYPE.CONNECTION);
                    }
                }
            }
        });
        JButton unregisterPlugin = JGComponentFactory.getCurrent().createButton("Unregister");
        unregisterPlugin.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TreeNode[] parents = node.getPath();
                for (TreeNode parent : parents) {
                    if (parent instanceof OIMAdminTreeNode && ((OIMAdminTreeNode) parent).type == OIMAdminTreeNode.NODE_TYPE.CONNECTION) {
                        OIMAdminTreeNode connectionNode = (OIMAdminTreeNode) parent;
                        if (connectionNode.getValue() != null && connectionNode.getValue() instanceof OIMConnection) {
                            OIMConnection oimConnection = (OIMConnection) connectionNode.getValue();
                            oimConnection.unregisterPlugin(nameField.getText());
                        } else {
                            logger.debug(
                                    "Nothing to do since connection node {} does not contain OIMConnection value {}",
                                    connectionNode, connectionNode.getValue());
                        }
                    } else {
                        logger.debug("Node in the path {} is not of type {}. Ignoring", parent, OIMAdminTreeNode.NODE_TYPE.CONNECTION);
                    }
                }
            }
        });

        JPanel packagePanel = FormBuilder.create().columns("right:pref, 3dlu, pref:grow")
                .rows("p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p").border(Borders.DIALOG)
                .add(generateJarFromClass).xy(1, 1).add(selectJarButton).xy(1, 3).add(jarFileLocationLabel).xy(3, 3)
                .add(prepareButton).xy(1, 5).add(registerPlugin).xy(1, 7).add(unregisterPlugin).xy(1, 9).build();

        JideTabbedPane newEventHandlerControlPane = new JideTabbedPane();
        newEventHandlerControlPane.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT);
        newEventHandlerControlPane.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
        newEventHandlerControlPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);
        newEventHandlerControlPane.setUseDefaultShowCloseButtonOnTab(false);
        newEventHandlerControlPane.setBoldActiveTab(true);
        newEventHandlerControlPane.setShowCloseButtonOnTab(true);
        newEventHandlerControlPane.addTab("Source Code", sourceCodePanel);
        newEventHandlerControlPane.addTab("Configure", configurationSplitPane);
        newEventHandlerControlPane.addTab("Package", packagePanel);

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
    public void destroyComponent() {
        logger.debug("Destroyed Event Handler UI {}", this);
    }

    @Override
    public String getStringRepresentation() {
        return UI_COMPONENT_NAME;
    }


    public static class EventHandlerRootTreeNode extends OIMAdminTreeNode {
        private final JTree selectionTree;
        private final JTabbedPane displayArea;
        private final Config config;
        private final OIMJMXWrapper connection;

        public EventHandlerRootTreeNode(String name, OIMJMXWrapper connection, Config config, Config.Configuration configuration, JTree selectionTree, JTabbedPane displayArea) {
            super(name, NODE_TYPE.EVENT_HANDLER, configuration);
            this.selectionTree = selectionTree;
            this.displayArea = displayArea;
            this.config = config;
            this.connection = connection;
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
            switch (event) {
                case NODE_EXPAND:
                    EventHandlerUI eventHandlerUI = config.getUIComponent(EventHandlerUI.class);
                    executeLoaderService(EventHandlerRootTreeNode.this, selectionTree, new Runnable() {
                        @Override
                        public void run() {
                            eventHandlerUI.loadEventHandler(EventHandlerRootTreeNode.this, selectionTree, displayArea);
                        }
                    });
                    break;
                default:
                    logger.debug("Nothing to do for event {} on node {}", event, this);
                    break;
            }
        }

        @Override
        public OIMJMXWrapper getValue() {
            return connection;
        }

    }

    public static class EventHandlerTreeNode extends OIMAdminTreeNode {
        private final JTree selectionTree;
        private final JTabbedPane displayArea;
        private final Config config;
        private final OIMJMXWrapper.OperationDetail operation;

        public EventHandlerTreeNode(String name, OIMJMXWrapper.OperationDetail operation, Config config, Config.Configuration configuration, JTree selectionTree, JTabbedPane displayArea) {
            super(name, NODE_TYPE.EVENT_HANDLER_OPERATION, configuration);
            this.selectionTree = selectionTree;
            this.displayArea = displayArea;
            this.config = config;
            this.operation = operation;
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
            switch (event) {
                case NODE_DISPLAY:
                    EventHandlerUI eventHandlerUI = config.getUIComponent(EventHandlerUI.class);
                    executeDisplayService(EventHandlerTreeNode.this, displayArea, new ExecuteCommand<JComponent>() {
                        @Override
                        public JComponent run() {
                            return eventHandlerUI.displayEventHandlerDetails(EventHandlerTreeNode.this, selectionTree, displayArea);
                        }
                    });
                    break;
                default:
                    logger.debug("Nothing to do for event {} on node {}", event, this);
                    break;
            }
        }

        @Override
        public OIMJMXWrapper.OperationDetail getValue() {
            return operation;
        }

    }

}
