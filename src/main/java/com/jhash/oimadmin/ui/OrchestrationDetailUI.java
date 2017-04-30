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
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.DBConnection;
import com.jhash.oimadmin.oim.OIMConnection;
import com.jhash.oimadmin.oim.OIMJMXWrapper;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideTabbedPane;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.Event;
import oracle.iam.platform.kernel.impl.PublicProcessImpl;
import oracle.iam.platform.kernel.vo.FailedEventResult;
import oracle.iam.platform.kernel.vo.Orchestration;
import oracle.iam.platform.kernel.vo.OrchestrationTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OrchestrationDetailUI<T extends JComponent> {
    private static final Logger logger = LoggerFactory.getLogger(OrchestrationDetailUI.class);
    private final DBConnection dbConnection;
    private final OIMJMXWrapper oimjmxWrapper;
    private final OIMConnection connection;
    private final AbstractUIComponent<T> parent;
    private JTextField orcProcessID = UIUtils.createTextField();
    private JTextField orcProcessName = UIUtils.createTextField();
    private JTextField orcProcessECID = UIUtils.createTextField();
    private JTextField orcProcessBulkParentID = UIUtils.createTextField();
    private JTextField orcProcessStatus = UIUtils.createTextField();
    private JTextField orcProcessParentProcessID = UIUtils.createTextField();
    private JTextField orcProcessDeProcessID = UIUtils.createTextField();
    private JTextField orcProcessEntityID = UIUtils.createTextField();
    private JTextField orcProcessSequence = UIUtils.createTextField();
    private JTextField orcProcessEntityType = UIUtils.createTextField();
    private JTextField orcProcessOperation = UIUtils.createTextField();
    private JTextField orcProcessStage = UIUtils.createTextField();
    private JTextField orcProcessChangeType = UIUtils.createTextField();
    private JTextField orcProcessRetry = UIUtils.createTextField();
    private JTextField orcProcessTarget = UIUtils.createTextField();
    private JTextField orcProcessSeqEntity = UIUtils.createTextField();
    private JTextArea orcProcessContextValue = UIUtils.createTextArea();
    private JTextField orcProcessCreatedOn = UIUtils.createTextField();
    private JTextField orcProcessModifiedOn = UIUtils.createTextField();
    private JTextField orcProcessObjOperation = UIUtils.createTextField();
    private JTextField orcProcessObjSync = UIUtils.createTextField();
    private JTextArea orcProcessObjTarget = UIUtils.createTextArea();
    private JTextArea orcProcessObjContextVal = UIUtils.createTextArea();
    private JTextArea orcProcessObjParameters = UIUtils.createTextArea();
    private JTextArea orcProcessObjInterEventData = UIUtils.createTextArea();
    private JTextField orcProcessObjTargetUserIds = UIUtils.createTextField();
    private JTextField orcProcessObjSaveCount = UIUtils.createTextField();
    private JTextField orcProcessObjNonSequential = UIUtils.createTextField();

    private JTextField processObjStatusName = UIUtils.createTextField();
    private JTextField processObjStatusType = UIUtils.createTextField();
    private JTextField processObjStageName = UIUtils.createTextField();
    private JTextField processObjStageType = UIUtils.createTextField();
    private JTextField processObjBulkParentID = UIUtils.createTextField();
    private JTextField processObjChangeType = UIUtils.createTextField();
    private JTextField processObjCreatedOn = UIUtils.createTextField();
    private JTextField processObjCurrentHandler = UIUtils.createTextField();
    private JTextField processObjLogStatement = UIUtils.createTextField();
    private JTextField processObjLogStatementMethod = JGComponentFactory.getCurrent().createTextField();
    private JTextField processObjModifiedOn = UIUtils.createTextField();
    private JTextField processObjOperation = UIUtils.createTextField();
    private JTextArea processObjOutOfBandEvents = UIUtils.createTextArea();
    private JTextField processObjParentProcessID = UIUtils.createTextField();
    private JTextArea processObjResult = UIUtils.createTextArea();
    private JTextField processObjRetryCount = UIUtils.createTextField();
    private JTextField processObjStartStage = UIUtils.createTextField();
    private JTextField processObjStopStage = UIUtils.createTextField();
    private JTextField processObjTargetType = UIUtils.createTextField();
    private JCheckBox processObjHasChildrenFromBulk = UIUtils.createBooleanCheckbox("_");
    private JCheckBox processObjHasDifferedChanges = UIUtils.createBooleanCheckbox("_");
    private JCheckBox processObjObjectSaved = UIUtils.createBooleanCheckbox("_");
    private JCheckBox processObjObjectRunning = UIUtils.createBooleanCheckbox("_");
    private JCheckBox processObjObjectStoppable = UIUtils.createBooleanCheckbox("_");

    private TraceRequestDetails.DetailsTable eventDetails;
    private JTextField eventErrorCode = UIUtils.createTextField();
    private JTextField eventErrorMessage = UIUtils.createTextField();
    private JTextArea eventResult = UIUtils.createTextArea();

    private JPanel orchestrationDetailsUIPanel;

    public OrchestrationDetailUI(DBConnection dbConnection, OIMJMXWrapper oimjmxWrapper, OIMConnection connection, AbstractUIComponent<T> parent) {
        this.dbConnection = dbConnection;
        this.oimjmxWrapper = oimjmxWrapper;
        this.connection = connection;
        this.parent = parent;
    }

    public OrchestrationDetailUI initialize() {
        JPanel contextPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, fill:p:grow")
                .add(new JideScrollPane(orcProcessContextValue)).xyw(2, 2, 7)
                .build();
        final JCheckBox compensateCancelled = JGComponentFactory.getCurrent().createCheckBox("Compensate");
        final JCheckBox cascadeCancelled = JGComponentFactory.getCurrent().createCheckBox("Cascade");

        JButton cancelButton = JGComponentFactory.getCurrent().createButton("Execute");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.executeAsyncOperation("Executing Orchestration Cancellation", new Runnable() {
                    @Override
                    public void run() {
                        String orcProcessIDValue = "N/A";
                        try {
                            orcProcessIDValue = orcProcessID.getText();
                            long processId = Long.parseLong(orcProcessIDValue);
                            boolean compensate = compensateCancelled.isSelected();
                            boolean cascade = cascadeCancelled.isSelected();
                            logger.trace("Trying to invoke cancel method with parameters process id={}, compensate={}, cascade={}", new Object[]{processId, compensate, cascade});
                            connection.executeOrchestrationOperation("cancel", new Class[]{long.class, boolean.class, boolean.class},
                                    new Object[]{processId, compensate, cascade});
                        } catch (Exception exception) {
                            parent.displayMessage("Orchestration Cancellation failed", "Failed to cancel orchestration process " + orcProcessIDValue, exception);
                        }
                    }
                });
            }
        });
        JButton cancelPendingFailedButton = JGComponentFactory.getCurrent().createButton("Execute");
        cancelPendingFailedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.executeAsyncOperation("Executing Failed Orchestration Cancellation", new Runnable() {
                    @Override
                    public void run() {
                        String orcProcessIDValue = "N/A";
                        try {
                            orcProcessIDValue = orcProcessID.getText();
                            long processId = Long.parseLong(orcProcessIDValue);
                            logger.trace("Trying to invoke cancel method with parameters process id={}", new Object[]{processId});
                            connection.executeOrchestrationOperation("cancelPendingFailedEvent", new Class[]{long.class},
                                    new Object[]{processId});
                        } catch (Exception exception) {
                            parent.displayMessage("Orchestration cancellation failed", "Failed to cancel failed orchestration process " + orcProcessIDValue, exception);
                        }
                    }
                });
            }
        });
        final JComboBox<FailedEventResult.Response> responseResult = new JComboBox<>(FailedEventResult.Response.values());
        JButton handleFailedButton = JGComponentFactory.getCurrent().createButton("Execute");
        handleFailedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.executeAsyncOperation("Executing Handle Failed Orchestration", new Runnable() {
                    @Override
                    public void run() {
                        FailedEventResult.Response response = FailedEventResult.Response.NULL;
                        String orcProcessIDValue = "N/A";
                        try {
                            orcProcessIDValue = orcProcessID.getText();
                            long processId = Long.parseLong(orcProcessIDValue);
                            response = (FailedEventResult.Response) responseResult.getSelectedItem();
                            FailedEventResult result = new FailedEventResult(response);
                            logger.trace("Trying to invoke handleFailed method with parameters process id={}, result=", new Object[]{processId, response});
                            connection.executeOrchestrationOperation("handleFailed", new Class[]{long.class, FailedEventResult.class},
                                    new Object[]{processId, result});
                        } catch (Exception exception) {
                            parent.displayMessage("Failed to handle failed orchestration", "Failed to set response " + response + " on event " + orcProcessIDValue, exception);
                        }
                    }
                });
            }
        });
        switch (oimjmxWrapper.getVersion()) {
            case OIM11GR2PS2:
                eventDetails = new TraceRequestDetails.DetailsTable(new String[]{"ID", "Name", "Status", "Stage",
                        "Retry", "Start Time", "End Time", "Error Code", "Error Message", "Result"}, parent);
                eventDetails.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        try {
                            if (e.getValueIsAdjusting())
                                return;
                            int selectedRow = eventDetails.getSelectedRow();
                            if (selectedRow == -1) {
                                logger.warn("Incorrect selection has been made or selection made has become invalid. Selected Row={}", selectedRow);
                                return;
                            }
                            String errorCode = (String) eventDetails.getValueAt(selectedRow, 7);
                            String errorMessage = (String) eventDetails.getValueAt(selectedRow, 8);
                            Object result = eventDetails.getValueAt(selectedRow, 9);
                            eventErrorCode.setText(errorCode);
                            eventErrorMessage.setText(errorMessage);
                            if (result != null) {
                                if (result instanceof Exception) {
                                    StringWriter output = new StringWriter();
                                    ((Exception) result).printStackTrace(new PrintWriter(output));
                                    eventResult.setText(output.toString());
                                } else if (result instanceof ContextAware) {
                                    ContextAware contextAwareResult = ((ContextAware) result);
                                    eventResult.setText("Type: " + contextAwareResult.getType());
                                    eventResult.append(System.lineSeparator());
                                    eventResult.append("Value: " + contextAwareResult.getObjectValue());
                                } else {
                                    eventResult.setText(result.toString());
                                }
                            } else {
                                eventResult.setText("");
                            }
                        } catch (Exception exception) {
                            parent.displayMessage("Event Result display failed", "Failed to display the Orchestration result for selected event", exception);
                        }
                    }
                });
                break;
            case OIM11GR2PS3:
            default:
                eventDetails = new TraceRequestDetails.DetailsTable(new String[]{"Order", "ID", "Name", "Operation", "Status", "Stage", "Is sync", "Handler Class",
                        "Result"}, parent);
                eventDetails.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        try {
                            if (e.getValueIsAdjusting())
                                return;
                            int selectedRow = eventDetails.getSelectedRow();
                            if (selectedRow == -1) {
                                logger.warn("Incorrect selection has been made or selection made has become invalid. Selected Row={}", selectedRow);
                                return;
                            }
                            Object result = eventDetails.getValueAt(selectedRow, 8);
                            eventErrorCode.setText("");
                            eventErrorMessage.setText("");
                            if (result != null) {
                                if (result instanceof Exception) {
                                    StringWriter output = new StringWriter();
                                    ((Exception) result).printStackTrace(new PrintWriter(output));
                                    eventResult.setText(output.toString());
                                } else if (result instanceof ContextAware) {
                                    ContextAware contextAwareResult = ((ContextAware) result);
                                    eventResult.setText("Type: " + contextAwareResult.getType());
                                    eventResult.append(System.lineSeparator());
                                    eventResult.append("Value: " + contextAwareResult.getObjectValue());
                                } else {
                                    eventResult.setText(result.toString());
                                }
                            } else {
                                eventResult.setText("");
                            }
                        } catch (Exception exception) {
                            parent.displayMessage("Event Result display failed", "Failed to display the Orchestration result for selected event", exception);
                        }
                    }
                });
                break;
        }
        processObjLogStatementMethod.setColumns(10);
        JPanel processDetailPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 5dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 5dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, [p,70], 2dlu, [p,70], 2dlu, [p,70]")
                .addLabel("Bulk Parent Process ID").xy(2, 2).add(processObjBulkParentID).xy(4, 2).addLabel("Parent Process ID").xy(6, 2).add(processObjParentProcessID).xy(8, 2)
                .addLabel("Status").xy(2, 4).add(processObjStatusName).xy(4, 4).addLabel("Status Type").xy(6, 4).add(processObjStatusType).xy(8, 4)
                .addLabel("Stage").xy(2, 6).add(processObjStageName).xy(4, 6).addLabel("Stage Type").xy(6, 6).add(processObjStageType).xy(8, 6)
                .addLabel("Type of Change").xy(2, 8).add(processObjChangeType).xy(4, 8).addLabel("Operation").xy(6, 8).add(processObjOperation).xy(8, 8)
                .addLabel("Start Stage").xy(2, 10).add(processObjStartStage).xy(4, 10).addLabel("Stop Stage").xy(6, 10).add(processObjStopStage).xy(8, 10)
                .addLabel("Created On").xy(2, 12).add(processObjCreatedOn).xy(4, 12).addLabel("Modified On").xy(6, 12).add(processObjModifiedOn).xy(8, 12)
                .addLabel("Target Type").xy(2, 14).add(processObjTargetType).xy(4, 14).addLabel("Retry Count").xy(6, 14).add(processObjRetryCount).xy(8, 14)
                .addLabel("Has Children from Bulk?").xy(2,16).add(processObjHasChildrenFromBulk).xy(4, 16).addLabel("Has deffered changes").xy(6,16).add(processObjHasDifferedChanges).xy(8, 16)
                .addLabel("Saved?").xy(2,18).add(processObjObjectSaved).xy(4, 18).addLabel("Running?").xy(6,18).add(processObjObjectRunning).xy(8, 18)
                .addLabel("Stoppable?").xy(2,20).add(processObjObjectStoppable).xy(4, 20)
                .addLabel("Log Statement").xy(2, 22).add(processObjLogStatementMethod).xy(4, 22).add(processObjLogStatement).xyw(6, 22,3)
                .addLabel("Current Handler").xy(2, 24).add(processObjCurrentHandler).xyw(4, 24,5)
                .addLabel("Out of band Events").xy(2, 26).add(processObjOutOfBandEvents).xyw(4, 26, 5)
                .addLabel("Result").xy(2, 28).add(processObjResult).xyw(4, 28, 5)
                .build();

        JPanel orchestrationDetailPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 5dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 2dlu, p, 2dlu, [p,70], 2dlu, [p,70], 2dlu, [p,70], 2dlu, [p,70], 2dlu, p, 5dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p")
                .addLabel("Non-sequential?").xy(2, 2).add(orcProcessObjNonSequential).xy(4, 2).addLabel("Sync?").xy(6, 2).add(orcProcessObjSync).xy(8, 2)
                .addLabel("Operation").xy(2, 4).add(orcProcessObjOperation).xy(4, 4).addLabel("Save Count").xy(6, 4).add(orcProcessObjSaveCount).xy(8, 4)
                .addLabel("Target").xy(2, 6).add(new JideScrollPane(orcProcessObjTarget)).xyw(4, 6, 5)
                .addLabel("Context Value").xy(2, 8).add(new JideScrollPane(orcProcessObjContextVal)).xyw(4, 8, 5)
                .addLabel("Inter-event Data").xy(2, 10).add(new JideScrollPane(orcProcessObjInterEventData)).xyw(4, 10, 5)
                .addLabel("Parameters").xy(2, 12).add(new JideScrollPane(orcProcessObjParameters)).xyw(4, 12, 5)
                .addLabel("Target User IDs").xy(2, 14).add(orcProcessObjTargetUserIds).xyw(4, 14, 5)
                .addSeparator("Cancel Orchestration").xyw(2, 16, 7)
                .add(compensateCancelled).xy(4, 18).add(cascadeCancelled).xy(6, 18).add(cancelButton).xy(8, 18)
                .addSeparator("Cancel Failed Orchestration").xyw(2, 20, 7)
                .add(cancelPendingFailedButton).xy(8, 22)
                .addSeparator("Handle Failed process").xyw(2, 24, 7)
                .add(responseResult).xy(4, 26).add(handleFailedButton).xy(8, 26)
                .build();
        JPanel eventDetailsPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 5dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, [p,250], 2dlu, p, 2dlu, p, 2dlu, fill:p:grow, 2dlu, p, 2dlu")
                .add(eventDetails).xyw(2, 2, 7)
                .addLabel("Error Code").xy(2, 4).add(eventErrorCode).xy(4, 4).addLabel("Error Message").xy(6, 4).add(eventErrorMessage).xy(8, 4)
                .addLabel("Result").xy(2, 6)
                .add(new JideScrollPane(eventResult)).xyw(2, 8, 7)
                .build();

        JideTabbedPane orchestrationDetailTabbedPanel = new JideTabbedPane();
        switch (oimjmxWrapper.getVersion()) {
            case OIM11GR2PS2:
                orchestrationDetailTabbedPanel.addTab("Orchestration Details", orchestrationDetailPanel);
                orchestrationDetailTabbedPanel.addTab("Context", contextPanel);
                orchestrationDetailTabbedPanel.addTab("Events", eventDetailsPanel);
                break;
            case OIM11GR2PS3:
                orchestrationDetailTabbedPanel.addTab("Process Details", processDetailPanel);
                orchestrationDetailTabbedPanel.addTab("Orchestration Details", orchestrationDetailPanel);
                orchestrationDetailTabbedPanel.addTab("Events", eventDetailsPanel);
                break;
        }
        orchestrationDetailsUIPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, fill:p:grow")
                .addLabel("ID").xy(2, 2).add(orcProcessID).xy(4, 2).addLabel("Name").xy(6, 2).add(orcProcessName).xy(8, 2)
                .addLabel("Entity ID").xy(2, 4).add(orcProcessEntityID).xy(4, 4).addLabel("Entity Type").xy(6, 4).add(orcProcessEntityType).xy(8, 4)
                .addLabel("Operation").xy(2, 6).add(orcProcessOperation).xy(4, 6).addLabel("Status").xy(6, 6).add(orcProcessStatus).xy(8, 6)
                .addLabel("Stage").xy(2, 8).add(orcProcessStage).xy(4, 8).addLabel("Change Type").xy(6, 8).add(orcProcessChangeType).xy(8, 8)
                .addLabel("Bulk Parent ID").xy(2, 10).add(orcProcessBulkParentID).xy(4, 10).addLabel("Parent Process ID").xy(6, 10).add(orcProcessParentProcessID).xy(8, 10)
                .addLabel("De-process ID").xy(2, 12).add(orcProcessDeProcessID).xy(4, 12).addLabel("ECID").xy(6, 12).add(orcProcessECID).xy(8, 12)
                .addLabel("Retry").xy(2, 14).add(orcProcessRetry).xy(4, 14).addLabel("Sequence Entity").xy(6, 14).add(orcProcessSeqEntity).xy(8, 14)
                .addLabel("Sequence").xy(2, 16).add(orcProcessSequence).xy(4, 16)
                .addLabel("Created On").xy(2, 18).add(orcProcessCreatedOn).xy(4, 18).addLabel("Modified On").xy(6, 18).add(orcProcessModifiedOn).xy(8, 18)
                .add(orchestrationDetailTabbedPanel).xyw(2, 20, 7)
                .build();
        return this;
    }

    public JPanel getUIComponent() {
        return orchestrationDetailsUIPanel;
    }

    public void reset() {
        resetBaseDetails();
        resetProcessDetails();
        resetOrchestrationDetails();
        orcProcessContextValue.setText("");
    }

    private void resetEventDetails() {
        eventDetails.tableModel.setRowCount(0);
        eventResult.setText("");
        eventErrorCode.setText("");
        eventErrorMessage.setText("");

    }

    public void loadDetail(long orchestrationProcessID) {
        switch (oimjmxWrapper.getVersion()) {
            case OIM11GR2PS2:
                loadDetailOIM11gR2PS2(orchestrationProcessID);
                break;
            case OIM11GR2PS3:
            default:
                loadDetailOIM11gR2PS3(orchestrationProcessID);
                break;
        }
    }

    public void loadDetailOIM11gR2PS3(long orchestrationProcessID) {
        Map<String, Object> values = new HashMap<>();
        try {
            OIMJMXWrapper.Details result = dbConnection.invokeSQL(DBConnection.GET_ORCHESTRATION_PROCESS_DETAILS, orchestrationProcessID);
            if (result.size() == 1) {
                values = result.getItemAt(0);
            } else {
                logger.warn("Expected 1 entry but received {}", result);
            }
            logger.debug("Result Values: {}", values);
            orcProcessEntityID.setText((String) values.get("ENTITYID"));
            orcProcessDeProcessID.setText(Utils.toString(values.get("DEPPROCESSID"))); //????
            orcProcessCreatedOn.setText(Utils.toString(values.get("CREATEDON")));
            orcProcessStatus.setText((String) values.get("STAGE"));
            orcProcessChangeType.setText((String) values.get("CHANGETYPE"));
            orcProcessRetry.setText(Utils.toString(values.get("RETRY")));
            orcProcessModifiedOn.setText(Utils.toString(values.get("MODIFIEDON")));
            orcProcessEntityType.setText((String) values.get("ENTITYTYPE"));
            orcProcessTarget.setText((String) values.get("ORCHTARGET"));
            orcProcessBulkParentID.setText(Utils.toString(values.get("BULKPARENTID")));
            orcProcessStatus.setText((String) values.get("STATUS"));
            orcProcessOperation.setText((String) values.get("OPERATION"));
            orcProcessParentProcessID.setText(Utils.toString(values.get("PARENTPROCESSID")));
            orcProcessID.setText(Utils.toString(values.get("ID")));
            orcProcessName.setText(Utils.toString(values.get("NAME")));
            orcProcessSeqEntity.setText("N/A for this version");
            orcProcessSequence.setText("N/A for this version");
            orcProcessECID.setText("N/A for this version");

            orcProcessObjContextVal.setText((String) values.get("CONTEXTVAL"));
            Object orchestrationObject = values.get("ORCHESTRATION");
            if (orchestrationObject != null) {
                if (orchestrationObject instanceof Blob) {
                    Blob orchestrationBlob = (Blob) orchestrationObject;
                    try {
                        PublicProcessImpl process = connection.getOrchestration(orchestrationBlob);
                        updateProcessDetails(process);
                        Orchestration orchestration = (Orchestration) process.getTarget();
                        updateOrchestrationDetails(orchestrationProcessID, orchestration);
                        try {
                            eventDetails.tableModel.setRowCount(0);
                            for (Event record : process.getEvents()) {
                                eventDetails.tableModel.addRow(new Object[]{record.getOrder(), record.getEventId().getId(), record.getEventId().getName(),
                                        record.getOperation(), record.getStatus(), record.getStage(), record.isSync(), record.getResult()});
                            }
                        } catch (Exception exception) {
                            parent.displayMessage("Failed to extract event details", "Failed to extract Orchestration Events for process ID " + orchestrationProcessID, exception);
                            resetEventDetails();
                        }
                    } catch (Exception exception) {
                        parent.displayMessage("Failed to extract process details", "Failed to extract process details for process ID " + orchestrationProcessID, exception);
                        resetOrchestrationDetails();
                        resetProcessDetails();
                        resetEventDetails();
                    }
                }
            } else {
                resetProcessDetails();
                resetOrchestrationDetails();
                resetEventDetails();
            }
        } catch (Exception exception) {
            parent.displayMessage("Orchestration retrieval failed", "Failed to get orchestration details for process ID  " + orchestrationProcessID, exception);
            resetBaseDetails();
            resetProcessDetails();
            resetOrchestrationDetails();
            resetEventDetails();
        }
    }

    public void resetBaseDetails() {
        orcProcessEntityID.setText("");
        orcProcessDeProcessID.setText(""); //????
        orcProcessCreatedOn.setText("");
        orcProcessStatus.setText("");
        orcProcessChangeType.setText("");
        orcProcessRetry.setText("");
        orcProcessModifiedOn.setText("");
        orcProcessEntityType.setText("");
        orcProcessTarget.setText("");
        orcProcessBulkParentID.setText("");
        orcProcessStatus.setText("");
        orcProcessOperation.setText("");
        orcProcessParentProcessID.setText("");
        orcProcessID.setText("");
        switch (oimjmxWrapper.getVersion()) {
            case OIM11GR2PS2:
                orcProcessSeqEntity.setText("");
                orcProcessSequence.setText("");
                orcProcessECID.setText("");
                orcProcessName.setText("N/A for this version");
                break;
            case OIM11GR2PS3:
            default:
                orcProcessSeqEntity.setText("N/A for this version");
                orcProcessSequence.setText("N/A for this version");
                orcProcessECID.setText("N/A for this version");
                orcProcessName.setText("");
                break;
        }
    }

    public void resetProcessDetails() {
        processObjBulkParentID.setText("");
        processObjParentProcessID.setText("");
        processObjChangeType.setText("");
        processObjOperation.setText("");
        processObjStatusName.setText("");
        processObjStatusType.setText("");
        processObjStageName.setText("");
        processObjStageType.setText("");
        processObjCreatedOn.setText("");
        processObjModifiedOn.setText("");
        processObjStartStage.setText("");
        processObjStopStage.setText("");
        processObjRetryCount.setText("");
        processObjTargetType.setText("");
        processObjCurrentHandler.setText("");
        processObjHasChildrenFromBulk.setSelected(false);
        processObjHasDifferedChanges.setSelected(false);
        processObjObjectSaved.setSelected(false);
        processObjObjectRunning.setSelected(false);
        processObjObjectStoppable.setSelected(false);
        processObjOutOfBandEvents.setText("");
        processObjResult.setText("");
    }

    private void updateProcessDetails(PublicProcessImpl process) {
        try {
            processObjBulkParentID.setText(process.getBulkParentId() != null ? process.getBulkParentId().toString() : "N/A");
            processObjParentProcessID.setText(process.getParentId() != null ? process.getParentId().toString() : "N/A");
            processObjChangeType.setText("" + process.getChangeType());
            processObjOperation.setText("" + process.getOperation());
            processObjStatusName.setText("" + process.getStatus());
            processObjStatusType.setText(process.getStatus() != null ?
                    ((process.getStatus().isCompleted() ? "Completed" : "") + " " + (process.getStatus().isCompletedWithFutherProcessingAllowed() ? " With further processing" : ""))
                    : "");
            processObjStageName.setText("" + process.getStage());
            processObjStageType.setText(process.getStage() != null ?
                    ((process.getStage().isFailedStage() ? "Failed" : "") + " " + (process.getStage().isOutOfBandStage() ? "Out of band" : ""))
                    : "");
            processObjCreatedOn.setText("" + new Date(process.getCreatedOn()));
            processObjModifiedOn.setText("" + new Date(process.getModifiedOn()));
            processObjStartStage.setText("" + process.getStartStage());
            processObjStopStage.setText("" + process.getStopStage());
            processObjRetryCount.setText("" + process.getRetryCount());
            processObjTargetType.setText("" + process.getTargetType());
            processObjCurrentHandler.setText("" + process.getCurrentHandler().getEventId());
            processObjHasChildrenFromBulk.setSelected(process.hasChildrenFromBulk());
            processObjHasDifferedChanges.setSelected(process.hasDeferredChanges());
            processObjObjectSaved.setSelected(process.isObjectSaved());
            processObjObjectRunning.setSelected(process.isRunning());
            processObjObjectStoppable.setSelected(process.isStoppable());
            processObjOutOfBandEvents.setText("");
            for (Event event : process.getOutOfBandEvents()) {
                processObjOutOfBandEvents.append("" + event.getEventId());
                processObjOutOfBandEvents.append(System.lineSeparator());
            }
            processObjResult.setText("" + process.getResult());
        } catch (Exception exception) {
            parent.displayMessage("Process display failed", "Failed to display process details for process ID " + process.getProcessId(), exception);
            resetProcessDetails();
        }
    }

    public void loadDetailOIM11gR2PS2(long orchestrationProcessID) {
        Map<String, Object> values = new HashMap<>();
        try {
            OIMJMXWrapper.Details result = dbConnection.invokeSQL(DBConnection.GET_ORCHESTRATION_PROCESS_DETAILS, orchestrationProcessID);
            if (result.size() == 1) {
                values = result.getItemAt(0);
            } else {
                logger.warn("Expected 1 entry but received {}", result);
            }
        } catch (Exception exception) {
            parent.displayMessage("Orchestration retrieval failed", "Failed to get orchestration details for process ID  " + orchestrationProcessID, exception);
            resetBaseDetails();
            orcProcessContextValue.setText("");
            resetOrchestrationDetails();
            resetEventDetails();
        }
        logger.debug("Result Values: {}", values);
        orcProcessEntityID.setText((String) values.get("ENTITYID"));
        orcProcessDeProcessID.setText(Utils.toString(values.get("DEPPROCESSID")));
        orcProcessCreatedOn.setText(Utils.toString(values.get("CREATEDON")));
        orcProcessStatus.setText((String) values.get("STAGE"));
        orcProcessSeqEntity.setText((String) values.get("SEQENTITY"));
        orcProcessChangeType.setText((String) values.get("CHANGETYPE"));
        orcProcessRetry.setText(Utils.toString(values.get("RETRY")));
        orcProcessModifiedOn.setText(Utils.toString(values.get("MODIFIEDON")));
        orcProcessEntityType.setText((String) values.get("ENTITYTYPE"));
        orcProcessTarget.setText((String) values.get("ORCHTARGET"));
        orcProcessBulkParentID.setText(Utils.toString(values.get("BULKPARENTID")));
        orcProcessStatus.setText((String) values.get("STATUS"));
        orcProcessOperation.setText((String) values.get("OPERATION"));
        orcProcessSequence.setText(Utils.toString(values.get("SEQUENCE")));
        orcProcessECID.setText((String) values.get("ECID"));
        orcProcessParentProcessID.setText(Utils.toString(values.get("PARENTPROCESSID")));
        orcProcessID.setText(Utils.toString(values.get("ID")));
        orcProcessName.setText("N/A for this version");
        Object contextValObject = values.get("CONTEXTVAL");
        if (contextValObject instanceof Clob) {
            Clob contextVal = (Clob) contextValObject;
            if (contextVal != null) {
                try {
                    orcProcessContextValue.setText(contextVal.getSubString(1, (int) contextVal.length()));
                } catch (SQLException exception) {
                    logger.warn("Failed to extract context value while processing result of orchestration process " + orchestrationProcessID, exception);
                    orcProcessContextValue.setText("");
                }
            } else {
                orcProcessContextValue.setText("");
            }
        } else if (contextValObject != null) {
            orcProcessContextValue.setText(contextValObject.toString());
        }
        logger.debug("Trying to looking orchestration details for {}", orchestrationProcessID);
        Orchestration orchestrationObject = null;
        try {
            orchestrationObject = connection.executeOrchestrationOperation("getOrchestration", new Class[]{long.class}, new Object[]{orchestrationProcessID});
            updateOrchestrationDetails(orchestrationProcessID, orchestrationObject);
        } catch (Exception exception) {
            parent.displayMessage("Orchestration extraction failed", "Failed to extract Orchestration for process ID " + orchestrationProcessID, exception);
            resetOrchestrationDetails();
        }
        try {
            OIMJMXWrapper.Details resultValues = dbConnection.invokeSQL(DBConnection.GET_ORCHESTRATION_PROCESS_EVENT_HANDLER_DETAILS, orchestrationProcessID);
            eventDetails.tableModel.setRowCount(0);
            for (Object[] record : resultValues.getData()) {
                Object data = null;
                if (record[9] instanceof Blob) {
                    File jarParentDirectory = Utils.getDirectoryContainingJarForClass("oracle.iam.platform.OIMClient");
                    data = Utils.getObjectInputStream(((Blob) record[9]).getBinaryStream(),
                            new File(jarParentDirectory, "oimclient.jar").toURI().toURL(),
                            new File(jarParentDirectory, "iam-platform-kernel.jar").toURI().toURL()
                    ).readObject();
                }
                eventDetails.tableModel.addRow(new Object[]{record[0], record[1], record[2], record[4], record[6], record[10], record[11], record[7], record[8], data});
            }
        } catch (Exception exception) {
            parent.displayMessage("Failed to extract Orchestration", "Failed to extract Orchestration Events for process ID " + orchestrationProcessID, exception);
            resetEventDetails();
        }
    }

    private void resetOrchestrationDetails() {
        orcProcessObjContextVal.setText("");
        orcProcessObjInterEventData.setText("");
        orcProcessObjNonSequential.setText("");
        orcProcessObjOperation.setText("");
        orcProcessObjParameters.setText("");
        orcProcessObjSaveCount.setText("");
        orcProcessObjSync.setText("");
        orcProcessObjTargetUserIds.setText("");
        orcProcessObjTarget.setText("");
    }

    private void updateOrchestrationDetails(long orchestrationProcessID, Orchestration orchestrationObject) {
        try {
            logger.debug("Trying to looking orchestration details for {}", orchestrationProcessID);
            logger.debug("Retrieved orchestration details as {}", orchestrationObject);
            orcProcessObjContextVal.setText(orchestrationObject.getContextVal());
            orcProcessObjInterEventData.setText(Utils.toString(orchestrationObject.getInterEventData()));
            logger.debug("Trying to locate isNonSequential method on the object received");
            try {
                if (orchestrationObject.getClass().getMethod("isNonSequential") != null) {
                    Object isNonSequentialResultObject = orchestrationObject.getClass().getMethod("isNonSequential").invoke(orchestrationObject);
                    if (isNonSequentialResultObject != null && isNonSequentialResultObject instanceof Boolean) {
                        orcProcessObjNonSequential.setText("" + isNonSequentialResultObject);
                    } else {
                        logger.debug("The result of isNonSequential invocation on {} was {}", new Object[]{orchestrationObject, isNonSequentialResultObject});
                    }
                } else {
                    logger.debug("Failed to locate isNonSequential method on object {}", orchestrationObject);
                    orcProcessObjNonSequential.setText("N/A for this version");
                }
            } catch (NoSuchMethodException exception) {
                orcProcessObjNonSequential.setText("N/A for this version");
            }
            orcProcessObjOperation.setText(orchestrationObject.getOperation());
            orcProcessObjParameters.setText(Utils.toString(orchestrationObject.getParameters()));
            //orcProcessObjSaveCount.setText(orchestrationObject.()==null?"null":orchestrationObject.getParameters().toString());
            orcProcessObjSync.setText("" + orchestrationObject.isSync());
            orcProcessObjTargetUserIds.setText(Arrays.toString(orchestrationObject.getTargetUserIds()));
            if (orchestrationObject.getTarget() != null) {
                OrchestrationTarget target = orchestrationObject.getTarget();
                StringBuilder orcTargetDetails = new StringBuilder();
                orcTargetDetails.append("Type         : " + target.getType());
                orcTargetDetails.append(System.lineSeparator());
                orcTargetDetails.append("Entity ID    : " + target.getEntityId());
                orcTargetDetails.append(System.lineSeparator());
                orcTargetDetails.append("All Entity ID: " + Arrays.toString(target.getAllEntityId()));
                orcTargetDetails.append(System.lineSeparator());
                orcTargetDetails.append("Default Action Handler: " + target.getDefaultActionHandler());
                orcTargetDetails.append(System.lineSeparator());
                orcTargetDetails.append("Default Validator: " + target.getDefaultValidator());
                try {
                    orcTargetDetails.append(System.lineSeparator());
                    orcTargetDetails.append("Existing: " + target.getExisting());
                } catch (Error exception) {
                    logger.warn("Failed to retrieve the value of Existing for target " + target + ". Ignoring & continuing", exception);
                    orcTargetDetails.append("Existing: <Failed to extract>");
                }
                try {
                    orcTargetDetails.append(System.lineSeparator());
                    orcTargetDetails.append("All Existing : " + Arrays.toString(target.getAllExisting()));
                } catch (Error exception) {
                    logger.warn("Failed to retrieve the value of All Existing for target " + target + ". Ignoring & continuing", exception);
                    orcTargetDetails.append("All Existing: <Failed to extract>");
                }
                orcTargetDetails.append(System.lineSeparator());
                orcProcessObjTarget.setText(orcTargetDetails.toString());
            } else {
                orcProcessObjTarget.setText("");
            }
        } catch (Exception exception) {
            parent.displayMessage("Orchestration extraction failed", "Failed to extract Orchestration for process ID " + orchestrationProcessID, exception);
            resetOrchestrationDetails();
        }
    }
}
