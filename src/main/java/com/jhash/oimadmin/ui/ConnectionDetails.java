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
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.Connection;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.DBConnection;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.OIMConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.io.File;

public class ConnectionDetails extends AbstractUIComponent<JPanel> {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionDetails.class);

    private final Config.EditableConfiguration connectionDetails;
    private JTextField nameLabel = JGComponentFactory.getCurrent().createTextField();
    private JComboBox<String> platform = new JComboBox<>(Config.PLATFORM.valuesAsString().toArray(new String[0]));
    private JTextField oimHome = JGComponentFactory.getCurrent().createTextField();
    private JTextField oimURL = JGComponentFactory.getCurrent().createTextField();
    private JTextField oimUser = JGComponentFactory.getCurrent().createTextField();
    private JTextField oimUserPassword = JGComponentFactory.getCurrent().createPasswordField();
    private JTextField jmxProtocol = JGComponentFactory.getCurrent().createTextField();
    private JTextField jmxHostname = JGComponentFactory.getCurrent().createTextField();
    private JTextField jmxPort = JGComponentFactory.getCurrent().createTextField();
    private JTextField jmxUser = JGComponentFactory.getCurrent().createTextField();
    private JTextField jmxUserPassword = JGComponentFactory.getCurrent().createPasswordField();
    private JTextField dbJDBCDriverClass = JGComponentFactory.getCurrent().createTextField();
    private JTextField dbJDBCURL = JGComponentFactory.getCurrent().createTextField();
    private JTextField dbUser = JGComponentFactory.getCurrent().createTextField();
    private JTextField dbPassword = JGComponentFactory.getCurrent().createPasswordField();
    private JCheckBox dbAutoCommit = JGComponentFactory.getCurrent().createCheckBox("Autocommit?");
    private JPanel displayComponent = null;
    private boolean isNewConnection = false;

    public ConnectionDetails(String name, Config configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration.getConnectionDetails(""), selectionTree, displayArea);
        connectionDetails = new Config.EditableConfiguration(this.configuration);
        isNewConnection = true;
    }

    public ConnectionDetails(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        connectionDetails = new Config.EditableConfiguration(configuration);
    }

    public boolean destroyComponentOnClose() {
        return isNewConnection;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}...", this);
        nameLabel.getDocument().addDocumentListener(new StandardDocumentListener(nameLabel, connectionDetails, OIMConnection.ATTR_CONN_NAME));
        nameLabel.setText(connectionDetails.getProperty(OIMConnection.ATTR_CONN_NAME));
        oimHome.getDocument().addDocumentListener(new StandardDocumentListener(oimHome, connectionDetails, OIMConnection.ATTR_OIM_HOME));
        oimHome.setText(connectionDetails.getProperty(OIMConnection.ATTR_OIM_HOME, OIMConnection.VAL_DEFAULT_OIM_HOME));
        oimURL.setColumns(20);
        oimURL.getDocument().addDocumentListener(new StandardDocumentListener(oimURL, connectionDetails, OIMConnection.ATTR_OIM_URL));
        oimURL.setText(connectionDetails.getProperty(OIMConnection.ATTR_OIM_URL, "t3://<hostname>:14000"));
        oimUser.getDocument().addDocumentListener(new StandardDocumentListener(oimUser, connectionDetails, OIMConnection.ATTR_OIM_USER));
        oimUser.setText(connectionDetails.getProperty(OIMConnection.ATTR_OIM_USER, "xelsysadm"));

        oimUserPassword.getDocument().addDocumentListener(new StandardDocumentListener(oimUserPassword, connectionDetails, OIMConnection.ATTR_OIM_PWD));
        oimUserPassword.setText(connectionDetails.getProperty(OIMConnection.ATTR_OIM_PWD));
        jmxProtocol.getDocument().addDocumentListener(new StandardDocumentListener(jmxProtocol, connectionDetails, JMXConnection.ATTR_JMX_PROTOCOL));
        jmxProtocol.setText(connectionDetails.getProperty(JMXConnection.ATTR_JMX_PROTOCOL, "t3"));
        jmxHostname.getDocument().addDocumentListener(new StandardDocumentListener(jmxHostname, connectionDetails, JMXConnection.ATTR_JMX_HOSTNAME));
        jmxHostname.setText(connectionDetails.getProperty(JMXConnection.ATTR_JMX_HOSTNAME));
        jmxPort.getDocument().addDocumentListener(new StandardDocumentListener(jmxPort, connectionDetails, JMXConnection.ATTR_JMX_PORT));
        jmxPort.setText(connectionDetails.getProperty(JMXConnection.ATTR_JMX_PORT, "7001"));
        jmxUser.getDocument().addDocumentListener(new StandardDocumentListener(jmxUser, connectionDetails, JMXConnection.ATTR_JMX_USER));
        jmxUser.setText(connectionDetails.getProperty(JMXConnection.ATTR_JMX_USER, "weblogic"));
        jmxUserPassword.getDocument().addDocumentListener(new StandardDocumentListener(jmxUserPassword, connectionDetails, JMXConnection.ATTR_JMX_PWD));
        jmxUserPassword.setText(connectionDetails.getProperty(JMXConnection.ATTR_JMX_PWD));
        platform.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = platform.getSelectedItem();
                if (selectedItem != null)
                    connectionDetails.setProperty(OIMConnection.ATTR_CONN_PLATFORM, selectedItem.toString());
            }
        });
        int selectedItemIndex = Config.PLATFORM.valuesAsString().indexOf(connectionDetails.getProperty(OIMConnection.ATTR_CONN_PLATFORM, "weblogic"));
        logger.debug("Selected Item index {}", selectedItemIndex);
        platform.setSelectedIndex(selectedItemIndex);
        dbJDBCDriverClass.getDocument().addDocumentListener(new StandardDocumentListener(dbJDBCDriverClass, connectionDetails, DBConnection.ATTR_DB_JDBC));
        dbJDBCDriverClass.setText(connectionDetails.getProperty(DBConnection.ATTR_DB_JDBC, "oracle.jdbc.driver.OracleDriver"));
        dbJDBCURL.setColumns(20);
        dbJDBCURL.getDocument().addDocumentListener(new StandardDocumentListener(dbJDBCURL, connectionDetails, DBConnection.ATTR_DB_URL));
        dbJDBCURL.setText(connectionDetails.getProperty(DBConnection.ATTR_DB_URL, "jdbc:oracle:thin:@<dbhost>:<dbport>:<service name>"));
        dbUser.getDocument().addDocumentListener(new StandardDocumentListener(dbUser, connectionDetails, DBConnection.ATTR_DB_USER));
        dbUser.setText(connectionDetails.getProperty(DBConnection.ATTR_DB_USER, "DEV_OIM"));
        dbPassword.getDocument().addDocumentListener(new StandardDocumentListener(dbPassword, connectionDetails, DBConnection.ATTR_DB_PWD));
        dbPassword.setText(connectionDetails.getProperty(DBConnection.ATTR_DB_PWD));
        dbAutoCommit.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    connectionDetails.setProperty(DBConnection.ATTR_DB_AUTOCOMMIT, "true");
                } else {
                    connectionDetails.setProperty(DBConnection.ATTR_DB_AUTOCOMMIT, "false");
                }
            }
        });
        dbAutoCommit.setSelected(Boolean.parseBoolean(connectionDetails.getProperty(DBConnection.ATTR_DB_AUTOCOMMIT)));
        displayComponent = buildComponent();
        logger.debug("Initialized {}", this);
    }

    @Override
    public JPanel getDisplayComponent() {
        return displayComponent;
    }

    private JPanel buildComponent() {
        FormLayout eventHandlerFormLayout = new FormLayout(
                "3dlu, right:pref, 3dlu, pref:grow, 5dlu, right:pref, 3dlu, pref:grow, 5dlu",
                "5dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu,p, 3dlu, p, 3dlu, p, 3dlu, p,3dlu, p, 3dlu, p ");
        eventHandlerFormLayout.setColumnGroups(new int[][]{{2, 6}});
        CellConstraints cellConstraint = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(eventHandlerFormLayout);
        builder.addLabel("Name", cellConstraint.xy(2, 2));
        builder.add(nameLabel, cellConstraint.xy(4, 2));
        builder.addLabel("OIM Home Directory", cellConstraint.xy(6, 2));
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        oimHome.addFocusListener(new FocusListener() {
            boolean focusAfterShowFileChooser = false;

            @Override
            public void focusGained(FocusEvent e) {
                if (focusAfterShowFileChooser)
                    return;
                fileChooser.setSelectedFile(new File(oimHome.getText()));
                int returnedResult = fileChooser.showOpenDialog(ConnectionDetails.this);
                if (returnedResult == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    oimHome.setText(file.getAbsolutePath());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                focusAfterShowFileChooser = fileChooser.isShowing();
            }
        });
        builder.add(oimHome, cellConstraint.xy(8, 2));

        builder.addSeparator("OIM Connection", cellConstraint.xyw(2, 4, 7));
        builder.addLabel("Server Platform", cellConstraint.xy(2, 6));
        builder.add(platform, cellConstraint.xy(4, 6));
        builder.addLabel("OIM Server URL", cellConstraint.xy(6, 6));
        builder.add(oimURL, cellConstraint.xy(8, 6));

        builder.addLabel("User", cellConstraint.xy(2, 8));
        builder.add(oimUser, cellConstraint.xy(4, 8));
        builder.addLabel("Password", cellConstraint.xy(6, 8));
        builder.add(oimUserPassword, cellConstraint.xy(8, 8));

        builder.addSeparator("Weblogic Admin Server (JMX)", cellConstraint.xyw(2, 10, 7));
        builder.addLabel("Protocol", cellConstraint.xy(2, 12));
        builder.add(jmxProtocol, cellConstraint.xy(4, 12));

        builder.addLabel("Host name", cellConstraint.xy(2, 14));
        builder.add(jmxHostname, cellConstraint.xy(4, 14));
        builder.addLabel("Port", cellConstraint.xy(6, 14));
        builder.add(jmxPort, cellConstraint.xy(8, 14));

        builder.addLabel("User", cellConstraint.xy(2, 16));
        builder.add(jmxUser, cellConstraint.xy(4, 16));
        builder.addLabel("Password", cellConstraint.xy(6, 16));
        builder.add(jmxUserPassword, cellConstraint.xy(8, 16));

        builder.addSeparator("Database", cellConstraint.xyw(2, 18, 5));
        JButton testDBButton = JGComponentFactory.getCurrent().createButton("Test");
        testDBButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.executeAsyncOperation("Test Database Connection", new Runnable() {
                    public void run() {
                        logger.debug("Trying to test connection using configuration {}", connectionDetails);
                        DBConnection connection = new DBConnection();
                        try {
                            connection.initialize(connectionDetails);
                            displayMessage("Database Connection", "Successfully connected to database", null);
                        } catch (Exception exception) {
                            displayMessage("Database Connection Failed", "Failed to create data base connection using details", exception);
                        }
                        try {
                            connection.destroy();
                        } catch (Exception exception) {
                            logger.warn("Failed to destroy new connection. Ignoring error", exception);
                        }
                        logger.debug("Tested connection");
                    }
                });
            }
        });
        builder.add(testDBButton, cellConstraint.xy(8, 18));

        builder.addLabel("Driver Class", cellConstraint.xy(2, 20));
        builder.add(dbJDBCDriverClass, cellConstraint.xy(4, 20));
        builder.addLabel("JDBC URL", cellConstraint.xy(6, 20));
        builder.add(dbJDBCURL, cellConstraint.xy(8, 20));

        builder.addLabel("User", cellConstraint.xy(2, 22));
        builder.add(dbUser, cellConstraint.xy(4, 22));
        builder.addLabel("Password", cellConstraint.xy(6, 22));
        builder.add(dbPassword, cellConstraint.xy(8, 22));

        builder.add(dbAutoCommit, cellConstraint.xy(4, 24));


        JButton saveButton = JGComponentFactory.getCurrent().createButton("&Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("Trying to save configuration {}", connectionDetails);
                connectionDetails.getConfig().saveConfiguration(connectionDetails, false);
                String connectionName = connectionDetails.getProperty(Connection.ATTR_CONN_NAME);
                if (isNewConnection)
                    ConnectionTreeNode.ConnectionsRegisterUI.addNewNode(connectionName, connectionDetails.getConfig(), selectionTree, displayArea);
                destroy();
                logger.debug("Saved configuration");
            }
        });
        JButton cancelButton = JGComponentFactory.getCurrent().createButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                destroy();
            }
        });
        builder.add(saveButton, cellConstraint.xy(4, 26));
        builder.add(cancelButton, cellConstraint.xy(6, 26));
        return builder.build();
    }


    @Override
    public void destroyComponent() {
    }


    private static class StandardDocumentListener implements DocumentListener {

        private Config.EditableConfiguration connectionDetails;
        private String attributeName;
        private JTextField textField;

        public StandardDocumentListener(JTextField textField, Config.EditableConfiguration configuration, String attributeName) {
            this.connectionDetails = configuration;
            this.textField = textField;
            this.attributeName = attributeName;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            String value = textField.getText();
            logger.trace("Setting property {} to {}", attributeName, value);
            connectionDetails.setProperty(attributeName, value);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            String value = textField.getText();
            logger.trace("Setting property {} to {}", attributeName, value);
            connectionDetails.setProperty(attributeName, textField.getText());
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            String value = textField.getText();
            logger.trace("Setting property {} to {}", attributeName, value);
            connectionDetails.setProperty(attributeName, textField.getText());
        }

    }


}
