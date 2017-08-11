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

package com.jhash.oimadmin.ui.oim.code;

import com.jgoodies.jsdl.common.builder.FormBuilder;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.code.Java;
import com.jhash.oimadmin.ui.AbstractUIComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.VirtualNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.utils.ConnectTextFieldListener;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class OIMClientUI extends AbstractUIComponent<JPanel, OIMClientUI> implements VirtualNode.VirtualNodeComponent {

    public static final MenuHandler.MENU NEW_CLIENT = new MenuHandler.MENU("OIM Client", MenuHandler.MENU.NEW,
            KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
    private static final Logger logger = LoggerFactory.getLogger(OIMClientUI.class);
    private Java java = new Java();
    private JButton compileButton = new JButton("Compile");
    private JButton executeButton = new JButton("Run..");
    private JPanel oimClientUI;
    private JideTabbedPane compileAndRunControlPanel = new JideTabbedPane();
    private UIJavaCompile javaCompiler;
    private UIJavaRun javaRun;

    public OIMClientUI(String name, ParentComponent parent) {
        super(name, parent);
    }

    public static void initializeNodeComponent(final VirtualNode virtualNode) {
        virtualNode.registerGlobalMenu(NEW_CLIENT, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                new OIMClientUI("New OIM Client ...", virtualNode.getParentComponent()).setDestroyComponentOnClose(true).setPublish(true).initialize();
            }
        });
    }

    @Override
    public void setupDisplayComponent() {
        logger.debug("Initializing {}...", this);
        executeButton.setEnabled(false);
        javaCompiler = new UIJavaCompile(java, "OIMClientSource", "Compile", this)
                .registerEventListener(UIJavaCompile.COMPILE_UPDATE, new Callback<Boolean, Object>() {
                    @Override
                    public Object call(Boolean successfulCompile) {
                        if (successfulCompile) {
                            executeButton.setEnabled(true);
                            compileAndRunControlPanel.setSelectedComponent(javaRun.getComponent());
                            javaRun.setWorkingDirectory(javaCompiler.getOutputDirectory());
                            javaRun.setClassPath(java.getClassPath());
                        } else {
                            executeButton.setEnabled(false);
                        }
                        compileButton.setEnabled(true);
                        compileButton.setText("Compile");
                        return null;
                    }
                });
        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                compileButton.setText("Compiling...");
                compileButton.setEnabled(false);
                javaCompiler.compile();
            }
        });
        javaRun = new UIJavaRun("Execute", this);
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeButton.setText("Starting...");
                executeButton.setEnabled(false);
                try {
                    javaRun.run();
                    executeButton.setText("Running...");
                    if (javaRun.runningProcess != null) {
                        executeButton.setEnabled(false);
                        Utils.executeAsyncOperation("Waiting for Process " + javaRun.runningProcess, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    javaRun.runningProcess.waitFor();
                                    executeButton.setEnabled(true);
                                } catch (Exception exception) {
                                    displayMessage("Execution failed", "Failed to wait for process " + javaRun.runningProcess + " to complete", exception);
                                }
                                executeButton.setEnabled(true);
                                executeButton.setText("Run..");
                            }
                        });
                    }
                } catch (Exception exception) {
                    displayMessage("Execution failed", "Could not run application", exception);
                    executeButton.setEnabled(true);
                    executeButton.setText("Run..");
                }
            }
        });
        javaCompiler.classNameText.getDocument().addDocumentListener(new ConnectTextFieldListener(new ConnectTextFieldListener.SyncTextField(javaCompiler.classNameText, javaRun.mainClass)));
        javaCompiler.initialize();
        javaRun.initialize();
        oimClientUI = buildPanel();
        logger.debug("Initialized {}", this);
    }

    private JPanel buildPanel() {
        JPanel newOIMClientUIPanel = new JPanel(new BorderLayout());
        compileAndRunControlPanel.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT);
        compileAndRunControlPanel.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
        compileAndRunControlPanel.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);
        compileAndRunControlPanel.setUseDefaultShowCloseButtonOnTab(false);
        compileAndRunControlPanel.setBoldActiveTab(true);
        compileAndRunControlPanel.setShowCloseButtonOnTab(false);
        compileAndRunControlPanel.addTab(javaCompiler.getName(), javaCompiler.getComponent());
        compileAndRunControlPanel.addTab(javaRun.getName(), javaRun.getComponent());

        JPanel panel = FormBuilder.create().columns("pref:grow, 3dlu, right:pref, 7dlu, right:pref, 3dlu, pref:grow")
                .rows("p, 3dlu, p")
                .add(compileButton).xy(3, 3).add(executeButton).xy(5, 3)
                .build();
        JideSplitPane oimClientSplitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        oimClientSplitPane.add(panel, 0);
        oimClientSplitPane.add(compileAndRunControlPanel, 1);
        oimClientSplitPane.setProportionalLayout(true);
        oimClientSplitPane.setProportions(new double[]{0.15});
        newOIMClientUIPanel.add(oimClientSplitPane, BorderLayout.CENTER);
        return newOIMClientUIPanel;
    }

    @Override
    public void destroyDisplayComponent() {
        logger.debug("Destroying {}...", this);
        if (javaCompiler != null) {
            javaCompiler.destroy();
            javaCompiler = null;
        }
        if (javaRun != null) {
            javaRun.destroy();
            javaRun = null;
        }
        logger.debug("Destroyed {}", this);
    }

    @Override
    public JPanel getDisplayComponent() {
        return oimClientUI;
    }

}
