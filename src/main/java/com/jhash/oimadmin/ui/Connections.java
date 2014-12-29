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
import com.jhash.oimadmin.OIMAdminTreeNode;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.OIMConnection;
import com.jhash.oimadmin.oim.OIMJMXWrapper;
import com.jhash.oimadmin.ui.OIMAdmin.RegisterUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Connections extends AbstractUIComponent implements RegisterUI {

    public static final String UI_COMPONENT_NAME = "Connections";
    private static final Logger logger = LoggerFactory.getLogger(Connections.class);
    private Map<OIMAdminTreeNode, Connection> connections = new HashMap<OIMAdminTreeNode, Connection>();

    @Override
    public String getName() {
        return UI_COMPONENT_NAME;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initialized Connections...");
    }

    @Override
    public void registerMenu(JMenuBar menu, Map<OIMAdmin.STANDARD_MENUS, JMenu> commonMenus, JTabbedPane displayArea) {
        if (commonMenus != null && commonMenus.containsKey(OIMAdmin.STANDARD_MENUS.NEW)) {
            JMenuItem newConnectionMenuItem = new JMenuItem("Connection");
            newConnectionMenuItem.setMnemonic('C');
            newConnectionMenuItem.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    displayArea.addTab("New Connection...", new ConnectionDetails(config.getConnectionDetails(""), displayArea));
                }
            });
            commonMenus.get(OIMAdmin.STANDARD_MENUS.NEW).add(newConnectionMenuItem);
        }
    }

    @Override
    public void registerSelectionTree(JTree selectionTree, JTabbedPane displayArea) {
        DefaultTreeModel model = (DefaultTreeModel) selectionTree.getModel();
        OIMAdminTreeNode rootNode = getNode(model.getRoot());
        if (rootNode == null)
            throw new NullPointerException("Failed to locate the root node for selection tree. Can not add any connections.");
        for (String oimConnectionName : config.getConnectionNames()) {
            logger.debug("Adding Node for connection {}", oimConnectionName);
            addUninitializedNode(rootNode, new ConnectionsAdminTreeNode(oimConnectionName, config.getConnectionDetails(oimConnectionName), selectionTree, displayArea), model);
        }
    }


    protected void loadOIMConnectionNode(ConnectionsAdminTreeNode node, JTree selectionTree, JTabbedPane displayArea) {
        DefaultTreeModel model = (DefaultTreeModel) selectionTree.getModel();
        OIMConnection tmpOIMConnection = node.getValue();
        tmpOIMConnection.initialize(node.configuration);
        tmpOIMConnection.login();
        addUninitializedNode(node, new MDS_UI.MDSAdminTreeNode("MDS Repository", config, node.configuration, selectionTree, displayArea), model);
        addUninitializedNode(node, new EventHandlerUI.EventHandlerRootTreeNode("Event Handlers", new OIMJMXWrapper(), config, node.configuration, selectionTree, displayArea), model);
        addUninitializedNode(node, new OIMAdminTreeNode.OIMAdminTreeNodeNoAction("Scheduled Tasks", OIMAdminTreeNode.NODE_TYPE.SCHEDULED_TASK, node.configuration), model);
        logger.debug("Trying to add connection {} to connections {}", tmpOIMConnection, connections);
        connections.put(node, tmpOIMConnection);
    }


    @Override
    public void destroyComponent() {
        logger.debug("Trying to destroy connections component {}", this);
        if (connections != null && !connections.isEmpty()) {
            for (Connection connection : connections.values()) {
                try {
                    connection.destroy();
                } catch (Exception exception) {
                    logger.warn("Failed to destroy connection " + connection + ". Ignoring error.", exception);
                }
            }
            connections.clear();
        }
        logger.debug("Destroyed connections component {}", this);
    }

    @Override
    public String getStringRepresentation() {
        return UI_COMPONENT_NAME + "[" + connections.keySet() + "]";
    }


    public static class ConnectionsAdminTreeNode extends OIMAdminTreeNode {

        private final JTree selectionTree;
        private final JTabbedPane displayArea;
        private OIMConnection connection;

        public ConnectionsAdminTreeNode(String name, Config.Configuration configuration, JTree selectionTree, JTabbedPane displayArea) {
            super(name, NODE_TYPE.CONNECTION, configuration);
            this.selectionTree = selectionTree;
            this.displayArea = displayArea;
            this.connection = new OIMConnection();
        }

        @Override
        public void handleEvent(EVENT_TYPE event) {
            switch (event) {
                case NODE_EXPAND:
                    executeLoaderService(this, selectionTree, new Runnable() {
                        @Override
                        public void run() {
                            Connections connections1 = configuration.getConfig().getUIComponent(Connections.class);
                            connections1.loadOIMConnectionNode(ConnectionsAdminTreeNode.this, selectionTree, displayArea);
                        }
                    });
                    break;
                case NODE_DISPLAY:
                    executeDisplayService(this, displayArea, new ExecuteCommand<JComponent>() {
                        @Override
                        public JComponent run() {
                            return new ConnectionDetails(ConnectionsAdminTreeNode.this.configuration, displayArea);
                        }
                    });
                    break;
                default:
                    logger.debug("Nothing to do for event {} on node {}", event, this);
                    break;
            }
        }

        @Override
        public OIMConnection getValue() {
            return connection;
        }

        @Override
        public boolean isDisplayable() {
            return true;
        }


    }

    public static class ConnectionDetails extends JPanel {

        private Config.Configuration configuration;
        private JTabbedPane displayArea;

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

        public ConnectionDetails(Config.Configuration configuration, JTabbedPane displayArea) {
            this.configuration = configuration;
            this.displayArea = displayArea;
            setLayout(new BorderLayout());
            add(generatePanel(), BorderLayout.CENTER);
            nameLabel.getDocument().addDocumentListener(new StandardDocumentListener(nameLabel, configuration, OIMConnection.ATTR_CONN_NAME));
            nameLabel.setText(configuration.getProperty(OIMConnection.ATTR_CONN_NAME));
            oimHome.getDocument().addDocumentListener(new StandardDocumentListener(oimHome, configuration, OIMConnection.ATTR_OIM_HOME));
            oimHome.setText(configuration.getProperty(OIMConnection.ATTR_OIM_HOME, System.getProperty("user.home") + "/.oimadm/"));
            oimURL.getDocument().addDocumentListener(new StandardDocumentListener(oimURL, configuration, OIMConnection.ATTR_OIM_URL));
            oimURL.setText(configuration.getProperty(OIMConnection.ATTR_OIM_URL, "t3://<hostname>:14000"));
            oimUser.getDocument().addDocumentListener(new StandardDocumentListener(oimUser, configuration, OIMConnection.ATTR_OIM_USER));
            oimUser.setText(configuration.getProperty(OIMConnection.ATTR_OIM_USER, "xelsysadm"));

            oimUserPassword.getDocument().addDocumentListener(new StandardDocumentListener(oimUserPassword, configuration, OIMConnection.ATTR_OIM_PWD));
            oimUserPassword.setText(configuration.getProperty(OIMConnection.ATTR_OIM_PWD));
            jmxProtocol.getDocument().addDocumentListener(new StandardDocumentListener(jmxProtocol, configuration, JMXConnection.ATTR_JMX_PROTOCOL));
            jmxProtocol.setText(configuration.getProperty(JMXConnection.ATTR_JMX_PROTOCOL, "t3"));
            jmxHostname.getDocument().addDocumentListener(new StandardDocumentListener(jmxHostname, configuration, JMXConnection.ATTR_JMX_HOSTNAME));
            jmxHostname.setText(configuration.getProperty(JMXConnection.ATTR_JMX_HOSTNAME));
            jmxPort.getDocument().addDocumentListener(new StandardDocumentListener(jmxPort, configuration, JMXConnection.ATTR_JMX_PORT));
            jmxPort.setText(configuration.getProperty(JMXConnection.ATTR_JMX_PORT, "7001"));
            jmxUser.getDocument().addDocumentListener(new StandardDocumentListener(jmxUser, configuration, JMXConnection.ATTR_JMX_USER));
            jmxUser.setText(configuration.getProperty(JMXConnection.ATTR_JMX_USER, "weblogic"));
            jmxUserPassword.getDocument().addDocumentListener(new StandardDocumentListener(jmxUserPassword, configuration, JMXConnection.ATTR_JMX_PWD));
            jmxUserPassword.setText(configuration.getProperty(JMXConnection.ATTR_JMX_PWD));
            platform.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object selectedItem = platform.getSelectedItem();
                    if (selectedItem != null)
                        configuration.setProperty(OIMConnection.ATTR_CONN_PLATFORM, selectedItem.toString());
                }
            });
            int selectedItemIndex = Config.PLATFORM.valuesAsString().indexOf(configuration.getProperty(OIMConnection.ATTR_CONN_PLATFORM, "weblogic"));
            logger.debug("Selected Item index {}", selectedItemIndex);
            platform.setSelectedIndex(selectedItemIndex);
        }

        private void handleChanges() {

        }

        private JPanel generatePanel() {
            FormLayout eventHandlerFormLayout = new FormLayout(
                    "3dlu, right:pref, 3dlu, pref:grow, 5dlu, right:pref, 3dlu, pref:grow, 5dlu",
                    "5dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 5dlu,p, 5dlu");
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
                    if (fileChooser.isShowing())
                        focusAfterShowFileChooser = true;
                    else
                        focusAfterShowFileChooser = false;
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
            JButton saveButton = JGComponentFactory.getCurrent().createButton("&Save");
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    configuration.getConfig().saveConfiguration(configuration);
                }
            });
            JButton cancelButton = JGComponentFactory.getCurrent().createButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    displayArea.remove(ConnectionDetails.this);
                }
            });
            builder.add(saveButton, cellConstraint.xy(4, 18));
            builder.add(cancelButton, cellConstraint.xy(6, 18));
            return builder.build();
        }

        private static class StandardDocumentListener implements DocumentListener {

            private Config.Configuration configuration;
            private String attributeName;
            private JTextField textField;

            public StandardDocumentListener(JTextField textField, Config.Configuration configuration, String attributeName) {
                this.configuration = configuration;
                this.textField = textField;
                this.attributeName = attributeName;
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                configuration.setProperty(attributeName, textField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                configuration.setProperty(attributeName, textField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                configuration.setProperty(attributeName, textField.getText());
            }

        }


    }

}
