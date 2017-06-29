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

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.jsdl.common.builder.FormBuilder;
import com.jgoodies.jsdl.component.JGComponentFactory;
import com.jgoodies.jsdl.component.JGTable;
import com.jgoodies.jsdl.component.JGTextArea;
import com.jgoodies.jsdl.component.JGTextField;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UIJavaRun extends AbstractUIComponent<JPanel, UIJavaRun> {

    private static final Logger logger = LoggerFactory.getLogger(UIJavaRun.class);

    final JGTextField mainClass = new JGTextField();
    final JGTextField workingDirectory = new JGTextField();
    final JGTextField classPaths = new JGTextField();
    final JGTextField vmLocation = new JGTextField();
    final JGTextArea output = new JGTextArea();
    final JGTextField input = new JGTextField();
    final JButton selectWorkingDirectory = new JideButton("...");
    final JButton selectVMLocation = new JideButton("...");
    JGTable vmOptions;
    JGTable programArguments;
    JGTable environmentVariable;
    Process runningProcess;
    private JPanel uiJavaRunPanel;

    public UIJavaRun(String name, AbstractUIComponent parent) {
        super(name, parent);
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        DefaultTableModel vmOptionsTableModel = new DefaultTableModel(new String[][]{
                {"-Dweblogic.Name=oim_server1"},
                {"-DAPPSERVER_TYPE=wls"},
                {"-Djava.security.auth.login.config=" + configuration.getWorkArea() + "/conf/authwl.conf"}
        }, new String[]{"VM Options"});
        vmOptionsTableModel.setRowCount(10);
        vmOptions = JGComponentFactory.getCurrent().createTable(vmOptionsTableModel);
        programArguments = JGComponentFactory.getCurrent().createTable(new DefaultTableModel(new String[]{"Arguments"}, 10));
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Name", "Value"}, 10);
        environmentVariable = JGComponentFactory.getCurrent().createTable(tableModel);
        environmentVariable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        File javaBinaryFile = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + ((System.getProperty("os.name").toLowerCase().startsWith("win")) ? "java.exe" : "java"));
        if (javaBinaryFile.exists()) {
            vmLocation.setText(javaBinaryFile.getAbsolutePath());
        }
        workingDirectory.setText(configuration.getWorkArea());
        final JFileChooser workingDirectoryChooser = new JFileChooser(configuration.getWorkArea());
        workingDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        workingDirectoryChooser.setDialogTitle("Working Directory...");
        selectWorkingDirectory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("Trying to select working directory for executing process");
                int response = workingDirectoryChooser.showDialog(UIJavaRun.this, "Select..");
                if (response == JFileChooser.APPROVE_OPTION) {
                    File workingDirectoryFile = workingDirectoryChooser.getSelectedFile();
                    workingDirectory.setText(workingDirectoryFile.getAbsolutePath());
                    logger.debug("Selected working directory for executing process as {}", workingDirectoryFile.getAbsolutePath());
                }
            }
        });
        final JFileChooser javaChooser = new JFileChooser();
        javaChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile() && f.canExecute() && f.getName().toLowerCase().startsWith("java");
            }

            @Override
            public String getDescription() {
                return "java";
            }
        });
        selectVMLocation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                javaChooser.setDialogTitle("Java executable...");
                File defaultFile = new File(vmLocation.getText());
                if (defaultFile.exists() && defaultFile.getParentFile().isDirectory())
                    javaChooser.setCurrentDirectory(defaultFile.getParentFile());
                int response = javaChooser.showDialog(UIJavaRun.this, "Select...");
                if (response == JFileChooser.APPROVE_OPTION) {
                    File selectedVMFile = javaChooser.getSelectedFile();
                    vmLocation.setText(selectedVMFile.getAbsolutePath());
                }
            }
        });
        uiJavaRunPanel = buildPanel();
        logger.debug("Initialized {}", this);
    }

    private JPanel buildPanel() {
        return FormBuilder.create().columns("right:pref, 3dlu, pref:grow, 2dlu, pref")
                .rows("p, 3dlu, [p,50dlu], 3dlu, [p,50dlu], 3dlu, p, 3dlu, p, 3dlu, [p,50dlu], 3dlu, p, 3dlu, p, 3dlu, p, 3dlu,p, 2dlu, fill:p:grow").border(Borders.DIALOG)
                .addLabel("Main Class").xy(1, 1).add(mainClass).xyw(3, 1, 3)
                .addLabel("VM Options").xy(1, 3).add(vmOptions).xyw(3, 3, 3)
                .addLabel("Program Arguments").xy(1, 5).add(programArguments).xyw(3, 5, 3)
                .addLabel("Working Directory").xy(1, 7).add(workingDirectory).xy(3, 7).add(selectWorkingDirectory).xy(5, 7)
                .addLabel("Environment Variables").xy(1, 9)
                .add(environmentVariable).xyw(3, 11, 3)
                .add("Class Path").xy(1, 13).add(classPaths).xyw(3, 13, 3)
                .add("Java Binary").xy(1, 15).add(vmLocation).xy(3, 15).add(selectVMLocation).xy(5, 15)
                .addSeparator("Output").xyw(1, 17, 5)
                .add(input).xyw(1, 19, 5)
                .add(new JideScrollPane(output)).xyw(1, 21, 5)
                .build();
    }

    public void setWorkingDirectory(String workingDirectory) {
        logger.trace("Setting work directory to {}", workingDirectory);
        this.workingDirectory.setText(workingDirectory);
    }

    public void setClassPath(List<File> classPath) {
        if (classPath != null && !classPath.isEmpty()) {
            StringBuilder classPathBuilder = new StringBuilder(".").append(File.pathSeparator);
            for (File classPathItem : classPath) {
                classPathBuilder.append(classPathItem.getAbsoluteFile()).append(File.pathSeparator);
            }
            classPathBuilder.append(System.getProperty("java.class.path"));
            this.classPaths.setText(classPathBuilder.toString());
        } else {
            this.classPaths.setText("");
        }
    }

    @Override
    public JPanel getDisplayComponent() {
        return uiJavaRunPanel;
    }

    public void run() {
        logger.debug("Trying to execute application, Resetting the output");
        output.setText("");
        List<String> commandArray = new ArrayList<>();
        String vmLocationString = vmLocation.getText();
        logger.debug("Adding JVM Location as {}", vmLocationString);
        commandArray.add(vmLocationString);
        logger.debug("Processing VM Options {}", vmOptions);
        for (int counter = 0; counter < vmOptions.getRowCount(); counter++) {
            logger.trace("Processing row {}", counter);
            Object value = vmOptions.getValueAt(counter, 0);
            logger.trace("Value read as {}", value);
            String valueToString;
            if (value != null && ((valueToString = value.toString()) != null) && !valueToString.isEmpty()) {
                logger.debug("Adding VM Option as {}", valueToString);
                commandArray.add(valueToString);
            }
        }
        String mainClassString = mainClass.getText();
        if (!Utils.isEmpty(mainClassString)) {
            logger.debug("Adding Main class as {}", mainClassString);
            commandArray.add(mainClassString);
        }
        logger.debug("Processing Program arguments {}", programArguments);
        for (int counter = 0; counter < programArguments.getRowCount(); counter++) {
            logger.trace("Processing row {}", counter);
            Object value = programArguments.getValueAt(counter, 0);
            logger.trace("Value read as {}", value);
            String valueToString;
            if (value != null && ((valueToString = value.toString()) != null) && !valueToString.isEmpty()) {
                logger.debug("Adding Program argument as {}", valueToString);
                commandArray.add(valueToString);
            }
        }
        logger.debug("Trying to create a new process builder with commands {}", commandArray);
        final ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
        String workingDirectoryString = workingDirectory.getText();
        if (!Utils.isEmpty(workingDirectoryString)) {
            logger.debug("Trying to create file for working directory {}", workingDirectoryString);
            File workingDirectoryFile = new File(workingDirectoryString);
            logger.debug("Setting working directory as {}", workingDirectoryString);
            processBuilder.directory(workingDirectoryFile);
        }
        TableModel model = environmentVariable.getModel();
        Map<String, String> environmentProperties = processBuilder.environment();
        logger.trace("Trying to update environment variables {} with model {}", environmentProperties, model);
        for (int rowCounter = 0; rowCounter < model.getRowCount(); rowCounter++) {
            logger.trace("Processing row {}", rowCounter);
            Object key = model.getValueAt(rowCounter, 0);
            logger.trace("Key : {}", key);
            Object value = model.getValueAt(rowCounter, 1);
            logger.trace("Value: {}", value);
            if (key != null && (!key.toString().isEmpty()) && value != null) {
                logger.debug("Adding {}={} to environment", key, value);
                environmentProperties.put(key.toString(), value.toString());
            }
        }
        logger.debug("Environment : {}", environmentProperties);
        StringBuilder classPathBuilder = new StringBuilder();
        String classPathString = classPaths.getText();
        if (!Utils.isEmpty(classPathString)) {
            classPathBuilder.append(classPathString);
        }
        String classPath = classPathBuilder.toString();
        if (!Utils.isEmpty(classPath)) {
            logger.debug("Adding class path CLASSPATH={}", classPath);
            processBuilder.environment().put("CLASSPATH", classPath);
        }
        logger.debug("Redirecting error to standard output");
        processBuilder.redirectErrorStream(true);
        try {
            logger.debug("Trying to start process {}", processBuilder.command());
            runningProcess = processBuilder.start();
            logger.debug("Attaching listener to write input to process's output stream");
            input.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String textToWrite = input.getText() + System.lineSeparator();
                    try {
                        runningProcess.getOutputStream().write(textToWrite.getBytes());
                        runningProcess.getOutputStream().flush();
                    } catch (Exception exception) {
                        displayMessage("Execution error", "Failed to write the input " + textToWrite + " to input of process " + processBuilder.command(), exception);
                    }
                    input.setText("");
                }
            });
            Utils.executeAsyncOperation("Display Process Output  [" + runningProcess + "]", new Runnable() {
                @Override
                public void run() {
                    logger.debug("Setting up buffered reader for process input stream {}", runningProcess.getInputStream());
                    BufferedReader dataReader = new BufferedReader(new InputStreamReader(runningProcess.getInputStream()));
                    String lineRead;
                    try {
                        logger.debug("Trying to read line first time");
                        while ((lineRead = dataReader.readLine()) != null) {
                            logger.trace("Read {}", lineRead);
                            output.append(lineRead);
                            output.append("\n");
                            logger.trace("Trying to read line");
                        }
                    } catch (Exception exception) {
                        displayMessage("Reader setup failed", "Error occurred while trying to read output from process " + processBuilder.command(), exception);
                    }
                    logger.debug("Completed the reading of the process output and displaying it to text area");
                }
            });
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to start process " + processBuilder.command(), exception);
        }
        logger.debug("Completed execution of the application");
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroyed {}", this);
    }

}
