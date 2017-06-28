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

import com.jgoodies.jsdl.common.builder.FormBuilder;
import com.jgoodies.jsdl.component.JGComponentFactory;
import com.jgoodies.jsdl.component.JGTextField;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.OIMConnection;
import com.jhash.oimadmin.oim.code.CompileUnit;
import com.jhash.oimadmin.oim.code.Java;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class UIJavaCompile extends AbstractUIComponent<JPanel, UIJavaCompile> {

    private static final Logger logger = LoggerFactory.getLogger(UIJavaCompile.class);
    public static ID<Boolean, Object, Callback<Boolean, Object>> COMPILE_UPDATE = new CLASS_ID<>();
    final JGTextField classNameText = new JGTextField();
    private final Java java;
    private final String templatePrefix;
    private final Config.OIM_VERSION oimVersion;
    private JTextArea sourceCode = JGComponentFactory.getCurrent().createTextArea();
    private JTextArea compileResultTextArea = JGComponentFactory.getCurrent().createReadOnlyTextArea();
    private JButton compileButton = JGComponentFactory.getCurrent().createButton("Compile..");
    private JButton saveButton = JGComponentFactory.getCurrent().createButton("Save");
    private JButton saveAsButton = JGComponentFactory.getCurrent().createButton("Save As..");
    private JGTextField additionalClassPath = new JGTextField();
    private JPanel javaCompileUI;
    private JComboBox<String> sourceCodeSelector;
    private JComboBox<Config.OIM_VERSION> oimVersionSelector;
    private String outputDirectory;
    private File templateDirectory;

    public UIJavaCompile(Java java, String prefix, String name, AbstractUIComponent parent) {
        this(java, null, prefix, name, parent);
    }

    public UIJavaCompile(Java java, Config.OIM_VERSION oimVersion, String prefix, String name, AbstractUIComponent parent) {
        super(name, parent);
        templatePrefix = prefix + "-";
        this.oimVersion = oimVersion;
        this.java = java;
    }

    public void compile() {
        compileButton.setText("Compiling...");
        compileButton.setEnabled(false);
        Utils.executeAsyncOperation("Compiling Java Client", new Runnable() {
            @Override
            public void run() {
                try {
                    boolean successfulCompile = false;
                    java.util.List<File> classPath = OIMConnection.getClassPath(configuration, (Config.OIM_VERSION) oimVersionSelector.getSelectedItem());
                    logger.debug("Updating classpath to {}", classPath);
                    java.setClassPath(classPath);
                    String additionalClassPathValue = additionalClassPath.getText();
                    if (!Utils.isEmpty(additionalClassPathValue)) {
                        String[] classPathItems = additionalClassPathValue.split(File.pathSeparator);
                        for (String classPathItem : classPathItems) {
                            if (!Utils.isEmpty(classPathItem)) {
                                File classPathItemFile = new File(classPathItem);
                                if (classPathItemFile.exists() && classPathItemFile.canRead()) {
                                    java.addClassPath(classPathItemFile);
                                } else {
                                    displayMessage("Compilation Warning", "Classpath entry " + classPathItem + " is not valid. Ignoring it for compilation.");
                                }
                            }
                        }
                    }
                    String result = java.compile(new CompileUnit(classNameText.getText(), sourceCode.getText()), outputDirectory);
                    if (result != null) {
                        compileResultTextArea.setText(result);
                    } else {
                        compileResultTextArea.setText("Compilation successful");
                        successfulCompile = true;
                    }
                    executeCallback(COMPILE_UPDATE, successfulCompile);
                } catch (Exception exception) {
                    displayMessage("Compilation failed", "Failed to compile source code", exception);
                }
                compileButton.setEnabled(true);
                compileButton.setText("Compile");
            }
        });
    }

    public boolean save(String templateName, String code) {
        String errorTitle = "Saving template failed";
        if (templateDirectory != null) {
            FileWriter templateFileWriter = null;
            try {
                File templateFileToSave = new File(templateDirectory, templatePrefix + templateName);
                if (templateFileToSave.exists()) {
                    if (templateFileToSave.isDirectory()) {
                        displayMessage(errorTitle, "An existing directory with same name exists at "
                                + templateFileToSave.getAbsolutePath()
                                + ". Please rename the template or delete existing directory before proceeding.");
                        return false;
                    }
                    if (!templateFileToSave.canWrite()) {
                        displayMessage(errorTitle, "The file " + templateFileToSave.getAbsolutePath() + " can not be overwritten.");
                        return false;
                    }
                }
                templateFileWriter = new FileWriter(templateFileToSave, false);
                templateFileWriter.write(code);
                templateFileWriter.flush();
                return true;
            } catch (Exception exception) {
                displayMessage(errorTitle, "Failed to save template " + templateName + " due to error.", exception);
                if (templateFileWriter != null) {
                    try {
                        templateFileWriter.close();
                    } catch (Exception fileCloseException) {
                        logger.debug("Failed to close template file", fileCloseException);
                    }
                }
            }
        } else {
            displayMessage(errorTitle, "Failed to save template " + templateName + " due to missing template directory.");
        }
        return false;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        this.outputDirectory = configuration.getWorkArea() + Config.VAL_WORK_AREA_CLASSES + File.separator + System.currentTimeMillis();
        logger.debug("Compile output directory {}", outputDirectory);
        final File templateDirectory = new File(configuration.getWorkArea() + File.separator + Config.VAL_WORK_AREA_CONF + File.separator + "templates");
        logger.debug("Trying to validate template directory {} exists and is directory", templateDirectory);
        if (templateDirectory.exists() && templateDirectory.isDirectory()) {
            this.templateDirectory = templateDirectory;
            logger.debug("Trying to list files in directory");
            String[] listOfFile = templateDirectory.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    logger.trace("Validating file {}", name);
                    if (name.startsWith(templatePrefix)) {
                        logger.trace("File {} begins with prefix", name);
                        return true;
                    }
                    return false;
                }
            });
            java.util.List<String> fixedListOfFile = new ArrayList<>();
            logger.debug("Extract class name and display name from file names {}", listOfFile);
            for (String fileName : listOfFile) {
                String fileSuffix = fileName.replaceAll(templatePrefix, "");
                logger.trace("Adding class {} to list", fileSuffix);
                fixedListOfFile.add(fileSuffix);
            }
            if (fixedListOfFile != null && fixedListOfFile.size() > 0) {
                logger.debug("Creating combo-box with values {}", fixedListOfFile);
                sourceCodeSelector = new JComboBox<String>(fixedListOfFile.toArray(new String[0]));
                sourceCodeSelector.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logger.debug("Event {} triggered on combo box {}", e, sourceCodeSelector);
                        String sourceCodeSelected = (String) sourceCodeSelector.getSelectedItem();
                        if (sourceCodeSelector != null) {
                            logger.debug("Trying to read file for selected source code {}", sourceCodeSelected);
                            String readData = Utils.readFile(templatePrefix + sourceCodeSelected, templateDirectory.getAbsolutePath());
                            sourceCode.setText(readData);
                            classNameText.setText(sourceCodeSelected);
                        }
                    }
                });
                sourceCodeSelector.setSelectedIndex(0);
            }
        }
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sourceCodeTemplateName = (String) sourceCodeSelector.getSelectedItem();
                String sourceCodeText = sourceCode.getText();
                save(sourceCodeTemplateName, sourceCodeText);
            }
        });
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newTemplateName = JOptionPane.showInputDialog(UIJavaCompile.this, "Template name");
                if (Utils.isEmpty(newTemplateName))
                    displayMessage("Invalid template", "Please provide valid template name.");
                if (save(newTemplateName, sourceCode.getText())) {
                    sourceCodeSelector.addItem(newTemplateName);
                    sourceCodeSelector.setSelectedItem(newTemplateName);
                }
            }
        });
        Config.OIM_VERSION[] versions = Config.OIM_VERSION.values();
        oimVersionSelector = new JComboBox<>(versions);
        if (oimVersion != null) {
            oimVersionSelector.setSelectedItem(oimVersion);
        } else {
            oimVersionSelector.setSelectedItem(Config.OIM_VERSION.LATEST);
        }
        compileButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    logger.debug("Triggered action {} on {}", e, compileButton);
                    compile();
                    logger.debug("Completed action {} on {}", e, compileButton);
                } catch (Exception exception) {
                    displayMessage("Compilation failed", "Failed to compile", exception);
                }
            }
        });
        javaCompileUI = buildCodePanel();
    }

    private JPanel buildCodePanel() {
        JPanel sourceCodeTextPanel = new JPanel(new BorderLayout());
        sourceCodeTextPanel.add(new JideScrollPane(sourceCode), BorderLayout.CENTER);
        JPanel sourceCodeSelectorButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Component applicationSourceCodeSelector = null;
        if (sourceCodeSelector != null) {
            applicationSourceCodeSelector = sourceCodeSelector;
            sourceCodeSelectorButtonPanel.add(saveButton);
            sourceCodeSelectorButtonPanel.add(saveAsButton);
        } else {
            applicationSourceCodeSelector = new JLabel("Not Available");
        }
        JPanel versionSelectorPanel = new JPanel();
        versionSelectorPanel.add(oimVersionSelector);
        JPanel sourceCodeTextButtonPanel = FormBuilder.create().columns("right:pref, 3dlu, pref, 3dlu, right:pref, 3dlu, pref:grow")
                .rows("p, 2dlu, p, 2dlu, p, 2dlu")
                .addLabel("Class Name").xy(1, 1).add(classNameText).xyw(3, 1, 5)
                .addLabel("Template").xy(1, 3).add(applicationSourceCodeSelector).xy(3, 3)
                .add(sourceCodeSelectorButtonPanel).xyw(5, 3, 3)
                .addLabel("OIM Version").xy(1, 5).add(oimVersionSelector).xy(3, 5)
                .addLabel("Additional Classpath").xy(5, 5).add(additionalClassPath).xy(7, 5)
                .build();
        sourceCodeTextPanel.add(sourceCodeTextButtonPanel, BorderLayout.NORTH);
        sourceCodeTextPanel.add(new JLabel(" "), BorderLayout.SOUTH);

        JPanel sourceCodeControlPanel = new JPanel(new BorderLayout());
        JPanel sourceCodeButtonPanel = FormBuilder.create().columns("center:pref")
                .rows("p")
                .add(compileButton).xy(1, 1).build();
        sourceCodeControlPanel.add(sourceCodeButtonPanel, BorderLayout.NORTH);
        sourceCodeControlPanel.add(new JideScrollPane(compileResultTextArea), BorderLayout.CENTER);
        sourceCodeControlPanel.add(new JLabel("  "), BorderLayout.SOUTH);

        JideSplitPane sourceCodePanel = new JideSplitPane();
        sourceCodePanel.add(sourceCodeTextPanel, 0);
        sourceCodePanel.add(sourceCodeControlPanel, 1);
        sourceCodePanel.setProportionalLayout(true);
        sourceCodePanel.setProportions(new double[]{0.7});
        return sourceCodePanel;
    }

    @Override
    public JPanel getDisplayComponent() {
        return javaCompileUI;
    }

    public String getOutputDirectory() {
        return this.outputDirectory;
    }

    @Override
    public void destroyComponent() {
        if (outputDirectory != null) {
            try {
                File outputDirectoryFile = new File(outputDirectory);
                if (outputDirectoryFile.exists()) {
                    FileUtils.forceDelete(outputDirectoryFile);
                }
            } catch (Exception exception) {
                logger.warn("Failed to delete directory " + outputDirectory + ". Ignoring error", exception);
            }
            outputDirectory = null;
        }
        if (javaCompileUI != null) {
            javaCompileUI = null;
        }
    }

    public static class ConnectTextFieldListener implements DocumentListener {

        private JTextField source;
        private JTextField destination;

        public ConnectTextFieldListener(JTextField source, JTextField destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (destination != null) {
                destination.setText(source.getText());
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (destination != null) {
                destination.setText(source.getText());
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (destination != null) {
                destination.setText(source.getText());
            }
        }
    }

}
