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
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class UIJavaCompile extends AbstractUIComponent<JPanel> {

    private static final Logger logger = LoggerFactory.getLogger(UIJavaCompile.class);
    final JGTextField classNameText = new JGTextField();
    private JTextArea sourceCode = JGComponentFactory.getCurrent().createTextArea();
    private JTextArea compileResultTextArea = JGComponentFactory.getCurrent().createReadOnlyTextArea();
    private JButton compileButton = JGComponentFactory.getCurrent().createButton("Compile..");
    private JPanel javaCompileUI;
    private String outputDirectory;
    private String templatePrefix;
    private JComboBox<String> sourceCodeSelector;


    public UIJavaCompile(String name, String prefix, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, false, configuration, selectionTree, displayArea);
        templatePrefix = prefix + "-";
    }

    public boolean compile() {
        boolean successfulCompile = false;
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
            successfulCompile = true;
        }
        return successfulCompile;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        this.outputDirectory = configuration.getWorkArea() + Config.VAL_WORK_AREA_CLASSES + File.separator + System.currentTimeMillis();
        logger.debug("Compile output directory {}", outputDirectory);
        final File templateDirectory = new File(configuration.getWorkArea() + File.separator + Config.VAL_WORK_AREA_CONF + File.separator + "templates");
        logger.debug("Trying to validate template directory {} exists and is directory", templateDirectory);
        if (templateDirectory.exists() && templateDirectory.isDirectory()) {
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
        JPanel sourceCodeTextButtonPanel = FormBuilder.create().columns("right:pref, 3dlu, pref:grow")
                .rows("p, 2dlu, p, 2dlu")
                .addLabel("Class Name").xy(1, 1).add(classNameText).xy(3, 1)
                .addLabel("Template").xy(1, 3).add(sourceCodeSelector == null ? new JLabel("Not Available") : sourceCodeSelector).xy(3, 3)
                .build();
        sourceCodeTextPanel.add(sourceCodeTextButtonPanel, BorderLayout.NORTH);
        sourceCodeTextPanel.add(new JLabel(), BorderLayout.SOUTH);

        JPanel sourceCodeControlPanel = new JPanel(new BorderLayout());
        JPanel sourceCodeButtonPanel = FormBuilder.create().columns("center:pref")
                .rows("p")
                .add(compileButton).xy(1, 1).build();
        sourceCodeControlPanel.add(sourceCodeButtonPanel, BorderLayout.NORTH);
        sourceCodeControlPanel.add(new JideScrollPane(compileResultTextArea), BorderLayout.CENTER);
        sourceCodeControlPanel.add(new JLabel(), BorderLayout.SOUTH);

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
