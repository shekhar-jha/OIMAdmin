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
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class UIJavaCompile extends AbstractUIComponent<JPanel> {

    private static final Logger logger = LoggerFactory.getLogger(UIJavaCompile.class);

    private JTextArea sourceCode = JGComponentFactory.getCurrent().createTextArea();
    private JTextArea compileResultTextArea = JGComponentFactory.getCurrent().createReadOnlyTextArea();
    private JButton compileButton = JGComponentFactory.getCurrent().createButton("Compile..");
    private JGTextField classNameText = new JGTextField(80);
    private JPanel javaCompileUI;
    private String outputDirectory;


    public UIJavaCompile(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, false, configuration, selectionTree, displayArea);
    }

    @Override
    public void initializeComponent() {
        this.outputDirectory = configuration.getWorkArea() + Config.VAL_WORK_AREA_CLASSES + File.separator + System.currentTimeMillis();
        String templateString = Utils.readFile("templates/EventHandlerSourceCode");
        sourceCode.setText(templateString);
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
        javaCompileUI = buildCodePanel();
    }

    private JPanel buildCodePanel() {
        JPanel sourceCodeTextPanel = new JPanel(new BorderLayout());
        sourceCodeTextPanel.add(new JideScrollPane(sourceCode), BorderLayout.CENTER);
        JPanel sourceCodeTextButtonPanel = new JPanel();
        sourceCodeTextButtonPanel.add(classNameText);
        sourceCodeTextPanel.add(sourceCodeTextButtonPanel);

        JPanel sourceCodeControlPanel = new JPanel(new BorderLayout());
        JPanel sourceCodeButtonPanel = new JPanel();
        sourceCodeButtonPanel.add(compileButton);
        sourceCodeControlPanel.add(sourceCodeButtonPanel, BorderLayout.NORTH);
        sourceCodeControlPanel.add(new JideScrollPane(compileResultTextArea), BorderLayout.CENTER);

        JideSplitPane sourceCodePanel = new JideSplitPane();
        sourceCodePanel.add(sourceCodeTextPanel, 0);
        sourceCodePanel.add(sourceCodeControlPanel, 1);
        sourceCodePanel.setProportionalLayout(true);
        sourceCodePanel.setProportions(new double[]{0.7});
        return sourceCodePanel;
    }

    @Override
    public JPanel getComponent() {
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
                logger.warn("Failed to delete directory {}. Ignoring error", outputDirectory, exception);
            }
            outputDirectory = null;
        }
        if (javaCompileUI != null) {
            javaCompileUI = null;
        }
    }

}
