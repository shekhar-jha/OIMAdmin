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
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OrchestrationDetailUI<T extends JComponent> {
    private static final Logger logger = LoggerFactory.getLogger(OrchestrationDetailUI.class);
    private final DBConnection dbConnection;
    private final OIMConnection connection;
    private final AbstractUIComponent<T> parent;
    private JTextField orcProcessID = UIUtils.createTextField();
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
    private TraceRequestDetails.DetailsTable eventDetails;
    private JTextField eventErrorCode = UIUtils.createTextField();
    private JTextField eventErrorMessage = UIUtils.createTextField();
    private JTextArea eventResult = UIUtils.createTextArea();

    private JPanel orchestrationDetailsUIPanel;

    public OrchestrationDetailUI(DBConnection dbConnection, OIMConnection connection, AbstractUIComponent<T> parent) {
        this.dbConnection = dbConnection;
        this.connection = connection;
        this.parent = parent;
    }

    public OrchestrationDetailUI initialize() {
        JPanel contextPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, fill:p:grow")
                .add(new JideScrollPane(orcProcessContextValue)).xyw(2, 2, 7)
                .build();
        JCheckBox compensateCancelled = JGComponentFactory.getCurrent().createCheckBox("Compensate");
        JCheckBox cascadeCancelled = JGComponentFactory.getCurrent().createCheckBox("Cascade");

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
        JComboBox<FailedEventResult.Response> responseResult = new JComboBox<>(FailedEventResult.Response.values());
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
                    parent.displayMessage("Orchestration display failed", "Failed to display the Orchestration event based on event ", exception);
                }
            }
        });
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
        orchestrationDetailTabbedPanel.addTab("Orchestration Details", orchestrationDetailPanel);
        orchestrationDetailTabbedPanel.addTab("Context", contextPanel);
        orchestrationDetailTabbedPanel.addTab("Events", eventDetailsPanel);
        orchestrationDetailsUIPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, fill:p:grow")
                .addLabel("ID").xy(2, 2).add(orcProcessID).xy(4, 2).addLabel("Entity ID").xy(6, 2).add(orcProcessEntityID).xy(8, 2)
                .addLabel("Entity Type").xy(2, 4).add(orcProcessEntityType).xy(4, 4).addLabel("Operation").xy(6, 4).add(orcProcessOperation).xy(8, 4)
                .addLabel("Stage").xy(2, 6).add(orcProcessStage).xy(4, 6).addLabel("Change Type").xy(6, 6).add(orcProcessChangeType).xy(8, 6)
                .addLabel("Status").xy(2, 8).add(orcProcessStatus).xy(4, 8).addLabel("Sequence").xy(6, 8).add(orcProcessSequence).xy(8, 8)
                .addLabel("Bulk Parent ID").xy(2, 10).add(orcProcessBulkParentID).xy(4, 10).addLabel("Parent Process ID").xy(6, 10).add(orcProcessParentProcessID).xy(8, 10)
                .addLabel("De-process ID").xy(2, 12).add(orcProcessDeProcessID).xy(4, 12).addLabel("ECID").xy(6, 12).add(orcProcessECID).xy(8, 12)
                .addLabel("Retry").xy(2, 14).add(orcProcessRetry).xy(4, 14).addLabel("Sequence Entity").xy(6, 14).add(orcProcessSeqEntity).xy(8, 14)
                .addLabel("Created On").xy(2, 16).add(orcProcessCreatedOn).xy(4, 16).addLabel("Modified On").xy(6, 16).add(orcProcessModifiedOn).xy(8, 16)
                .add(orchestrationDetailTabbedPanel).xyw(2, 18, 7)
                .build();
        return this;
    }

    public JPanel getUIComponent() {
        return orchestrationDetailsUIPanel;
    }

    public void loadDetail(long orchestrationProcessID) {
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
        try {
            logger.debug("Trying to looking orchestration details for {}", orchestrationProcessID);
            Orchestration orchestrationObject = (Orchestration) connection.executeOrchestrationOperation("getOrchestration", new Class[]{long.class}, new Object[]{orchestrationProcessID});
            logger.debug("Retrieved orchestration details as {}", orchestrationObject);
            orcProcessObjContextVal.setText(orchestrationObject.getContextVal());
            orcProcessObjInterEventData.setText(Utils.toString(orchestrationObject.getInterEventData()));
            orcProcessObjNonSequential.setText("" + orchestrationObject.isNonSequential());
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
        try {
            OIMJMXWrapper.Details resultValues = dbConnection.invokeSQL(DBConnection.GET_ORCHESTRATION_PROCESS_EVENT_HANDLER_DETAILS, orchestrationProcessID);
            eventDetails.tableModel.setRowCount(0);
            for (Object[] record : resultValues.getData()) {
                Object data = null;
                if (record[9] instanceof Blob) {
                    data = new ObjectInputStream(((Blob) record[9]).getBinaryStream()).readObject();
                }
                eventDetails.tableModel.addRow(new Object[]{record[0], record[1], record[2], record[4], record[6], record[10], record[11], record[7], record[8], data});
            }
        } catch (Exception exception) {
            parent.displayMessage("Failed to extract Orchestration", "Failed to extract Orchestration Events for process ID " + orchestrationProcessID, exception);
        }


    }


}
