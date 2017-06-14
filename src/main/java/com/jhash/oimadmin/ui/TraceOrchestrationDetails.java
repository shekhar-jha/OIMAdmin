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

package com.jhash.oimadmin.ui;

import com.jgoodies.jsdl.common.builder.FormBuilder;
import com.jgoodies.jsdl.component.JGComponentFactory;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.orch.OrchManager;
import com.jidesoft.swing.JideTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TraceOrchestrationDetails extends AbstractUIComponent<JPanel> {

    private static final Logger logger = LoggerFactory.getLogger(TraceRequestDetails.class);
    private final boolean destroyOnClose;
    private final OrchManager orchestrationManager;
    private JTextField orchestrationID = JGComponentFactory.getCurrent().createTextField();
    private OrchestrationDetailUI orchestrationDetailPanel;
    private JPanel traceOrchestrationUI;

    public TraceOrchestrationDetails(OrchManager orchManager, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        this(orchManager, false, name, configuration, selectionTree, displayArea);
    }

    public TraceOrchestrationDetails(OrchManager orchManager, boolean destroyOnClose, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.destroyOnClose = destroyOnClose;
        this.orchestrationManager = orchManager;
    }

    @Override
    public boolean destroyComponentOnClose() {
        return destroyOnClose;
    }

    @Override
    public void initializeComponent() {
        JButton retrieve = JGComponentFactory.getCurrent().createButton("Retrieve..");
        retrieve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String orchestrationIDValueDuplicate = "N/A";
                try {
                    orchestrationIDValueDuplicate = orchestrationID.getText();
                    final String orchestrationIDValue = orchestrationID.getText();
                    if (!Utils.isEmpty(orchestrationIDValue)) {
                        Utils.executeAsyncOperation("Loading Orchestration Details " + orchestrationIDValue, new Runnable() {
                            @Override
                            public void run() {
                                retrieveOrchestrationDetails(orchestrationIDValue);
                            }
                        });
                    }
                } catch (Exception exception) {
                    displayMessage("Orchestration detail retrieval failed", "Failed to initiate retrieval of the orchestration Event: " + orchestrationIDValueDuplicate, exception);
                }
            }
        });
        JPanel searchCriteria = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p")
                .addLabel("Orchestration ID").xy(2, 2).add(orchestrationID).xy(4, 2).add(retrieve).xy(8, 2)
                .build();
        orchestrationDetailPanel = new OrchestrationDetailUI<>(orchestrationManager, this).initialize();
        JideTabbedPane tabbedPane = new JideTabbedPane();
        tabbedPane.addTab("Orchestration", orchestrationDetailPanel.getUIComponent());
        traceOrchestrationUI = new JPanel(new BorderLayout());
        traceOrchestrationUI.add(searchCriteria, BorderLayout.NORTH);
        traceOrchestrationUI.add(tabbedPane, BorderLayout.CENTER);
    }

    public void retrieveOrchestrationDetails(String orchestrationIDValue) {
        try {
            long orchestrationProcessID = Long.parseLong(orchestrationIDValue);
            orchestrationDetailPanel.loadDetail(orchestrationProcessID);
        } catch (Exception exception) {
            displayMessage("Failed to load orchestration details", "Failed to load orchestration details for " + orchestrationIDValue, exception);
        }
    }

    @Override
    public JPanel getDisplayComponent() {
        return traceOrchestrationUI;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying component {}", this);
        logger.debug("Destroyed component {}", this);
    }

}
