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
import com.jgoodies.jsdl.component.JGStripedTable;
import com.jgoodies.jsdl.component.renderer.JGBooleanTableCellRenderer;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.orch.OrchManager;
import com.jhash.oimadmin.oim.request.*;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class TraceRequestDetails extends AbstractUIComponent<JPanel, TraceRequestDetails> {

    private static final Logger logger = LoggerFactory.getLogger(TraceRequestDetails.class);

    private final RequestManager requestManager;
    private final OrchManager orchestrationManager;
    private JTextField beneficiaryType = UIUtils.createTextField();
    private JFormattedTextField creationDate = UIUtils.createDateField();
    private JTextField getDependsOnRequestId = UIUtils.createTextField();
    private JFormattedTextField getEndDate = UIUtils.createDateField();
    private JFormattedTextField getExecutionDate = UIUtils.createDateField();
    private JTextArea getReasonForFailure = UIUtils.createTextArea();
    private JFormattedTextField getRequesterKey = UIUtils.createLongField();
    private JTextField getRequestModelName = UIUtils.createTextField();
    private JFormattedTextField getRequestStage = UIUtils.createLongField();
    private JTextField getRequestStatus = UIUtils.createTextField();
    private JTextField getRequestID = UIUtils.createTextField();
    private JFormattedTextField getRequestKey = UIUtils.createLongField();
    private JTextArea getJustification = UIUtils.createTextArea();
    private JTextArea getRequestContext = UIUtils.createTextArea();
    private DetailsTable additionalAttributesTable;
    private DetailsTable approvalDataTable;
    private DetailsTable beneficiaryTable;
    private DetailsTable targetEntitiesOfBeneficiaryTable;
    private DetailsTable beneficiaryTargetEntityValuesTable;
    private DetailsTable beneficiaryTargetEntityAdditionalValuesTable;
    private DetailsTable targetEntitiesTable;
    private DetailsTable targetEntityValuesTable;
    private DetailsTable targetEntityAdditionalValuesTable;
    private DetailsTable templateAttributesTable;
    private DetailsTable childRequestTable;
    private OrchestrationDetailUI orchestrationDetailPanel;

    private JTextField requestID = JGComponentFactory.getCurrent().createTextField();
    private JPanel traceRequestUI;


    public TraceRequestDetails(RequestManager requestManager, OrchManager orchManager, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.requestManager = requestManager;
        this.orchestrationManager = orchManager;
    }

    private void retrieveRequestDetails(String requestIDValue) {
        try {
            Request request = requestManager.getRequestDetails(requestIDValue);

            getRequestID.setText(request.getRequestID());
            getRequestKey.setValue(request.getRequestKey());
            getDependsOnRequestId.setText(request.getDependsOnRequestId());
            getRequesterKey.setText(request.getRequesterKey());

            beneficiaryType.setText(request.getBeneficiaryType());
            getRequestModelName.setText(request.getRequestModelName());
            getRequestStage.setValue(request.getRequestStage());
            getRequestStatus.setText(request.getRequestStatus());

            creationDate.setValue(request.getCreationDate());
            getExecutionDate.setValue(request.getExecutionDate());
            getEndDate.setValue(request.getEndDate());
            getReasonForFailure.setText(request.getReasonForFailure());

            getJustification.setText(request.getJustification());
            if (request.getRequestContext() != null) {
                Request.RequestContext requestContext = request.getRequestContext();
                getRequestContext.setText("Login User: " + requestContext.getLoginUserId());
                getRequestContext.append(System.lineSeparator());
                getRequestContext.append("Login User's Role: " + requestContext.getLoginUserRole());
                getRequestContext.append(System.lineSeparator());
                getRequestContext.append("Request ID: " + requestContext.getRequestId());
                getRequestContext.append(System.lineSeparator());
            } else {
                getRequestContext.setText("");
            }
            additionalAttributesTable.tableModel.setRowCount(0);
            for (TemplateAttribute attribute : request.getAdditionalAttributes()) {
                additionalAttributesTable.tableModel.addRow(new Object[]{attribute.getName(), attribute.getTypeHolder(), attribute.getValue()});
            }
            templateAttributesTable.tableModel.setRowCount(0);
            for (TemplateAttribute attribute : request.getTemplateAttributes()) {
                templateAttributesTable.tableModel.addRow(new Object[]{attribute.getName(), attribute.getTypeHolder(), attribute.getValue()});
            }
            approvalDataTable.tableModel.setRowCount(0);
            for (ApprovalData approvalData : request.getApprovalData()) {
                approvalDataTable.tableModel.addRow(new Object[]{approvalData.getApprovalInstanceID(), approvalData.getApprovalKey(), approvalData.getStage(), approvalData.getStatus()});
            }
            beneficiaryTable.tableModel.setRowCount(0);
            for (Beneficiary beneficiary : request.getBeneficiaries()) {
                beneficiaryTable.tableModel.addRow(new Object[]{beneficiary.getBeneficiaryKey(), beneficiary.getBeneficiaryType(), beneficiary.getAttributes(), beneficiary.getTargetEntities()});
            }
            targetEntitiesOfBeneficiaryTable.tableModel.setRowCount(0);
            beneficiaryTargetEntityValuesTable.tableModel.setRowCount(0);
            beneficiaryTargetEntityAdditionalValuesTable.tableModel.setRowCount(0);
            targetEntitiesTable.tableModel.setRowCount(0);
            for (RequestEntity entity : request.getTargetEntities()) {
                targetEntitiesTable.tableModel.addRow(new Object[]{entity.getEntityKey(), entity.getRequestEntityType(), entity.getEntitySubType(), entity.getOperation(), entity.getEntityData(), entity.getAdditionalEntityData()});
            }
            targetEntityValuesTable.tableModel.setRowCount(0);
            targetEntityAdditionalValuesTable.tableModel.setRowCount(0);
            childRequestTable.tableModel.setRowCount(0);
            for (Request childRequest : request.getChildRequests()) {
                childRequestTable.tableModel.addRow(new Object[]{childRequest.getRequestID(), childRequest.getRequestModelName(), childRequest.getRequestStatus()});
            }
            if (orchestrationDetailPanel != null) orchestrationDetailPanel.loadDetail(request.getOrchID());
            //request.getEventID();
        } catch (Exception exception) {
            displayMessage("Failed to load request details", "Failed to process request detail retrieval for " + requestIDValue, exception);
            reset();
        }
    }

    public void reset() {
        getRequestID.setText("");
        getRequestKey.setText("");
        getDependsOnRequestId.setText("");
        getRequesterKey.setText("");

        beneficiaryType.setText("");
        getRequestModelName.setText("");
        getRequestStage.setText("");
        getRequestStatus.setText("");

        creationDate.setText("");
        getExecutionDate.setText("");
        getEndDate.setText("");
        getReasonForFailure.setText("");

        getJustification.setText("");
        getRequestContext.setText("");
        additionalAttributesTable.tableModel.setRowCount(0);
        templateAttributesTable.tableModel.setRowCount(0);
        approvalDataTable.tableModel.setRowCount(0);
        beneficiaryTable.tableModel.setRowCount(0);
        targetEntitiesOfBeneficiaryTable.tableModel.setRowCount(0);
        beneficiaryTargetEntityValuesTable.tableModel.setRowCount(0);
        beneficiaryTargetEntityAdditionalValuesTable.tableModel.setRowCount(0);
        targetEntitiesTable.tableModel.setRowCount(0);
        targetEntityValuesTable.tableModel.setRowCount(0);
        targetEntityAdditionalValuesTable.tableModel.setRowCount(0);
        childRequestTable.tableModel.setRowCount(0);
        if (orchestrationDetailPanel != null) orchestrationDetailPanel.reset();
    }

    @Override
    public void initializeComponent() {
        additionalAttributesTable = new DetailsTable(new String[]{"Name", "Type", "Value"}, this);
        approvalDataTable = new DetailsTable(new String[]{"Instance ID", "Key", "Stage", "Status"}, this);
        beneficiaryTable = new DetailsTable(new String[]{"Key", "Type", "Attributes", "Target Entities"}, this);
        targetEntitiesOfBeneficiaryTable = new DetailsTable(new String[]{"Key", "Type", "Sub-type",
                "Operation", "Attributes", "Additional Attributes"}, this);
        beneficiaryTargetEntityValuesTable = new DetailsTable(new String[]{"Row Key", "Action", "Name", "Type", "Value",
                "Parent", "Child", "Default", "MLS Map"}, this);
        beneficiaryTargetEntityAdditionalValuesTable = new DetailsTable(new String[]{"Row Key", "Action", "Name", "Type", "Value",
                "Parent", "Child", "Default", "MLS Map"}, this);
        beneficiaryTable.removeColumn(beneficiaryTable.getColumn("Target Entities"));
        beneficiaryTable.addActionListener(3, targetEntitiesOfBeneficiaryTable, RequestBeneficiaryEntity.class, new RowExtractor<RequestBeneficiaryEntity>() {
            @Override
            public Object[] getRowDetails(RequestBeneficiaryEntity entity) {
                return new Object[]{entity.getEntityKey(), entity.getRequestEntityType(),
                        entity.getEntitySubType(), entity.getOperation(),
                        entity.getEntityData(), entity.getAdditionalEntityData()};
            }
        });
        targetEntitiesOfBeneficiaryTable.addActionListener(4, beneficiaryTargetEntityValuesTable, RequestBeneficiaryEntity.RequestBeneficiaryEntityAttribute.class, new RowExtractor<RequestBeneficiaryEntity.RequestBeneficiaryEntityAttribute>() {
            @Override
            public Object[] getRowDetails(RequestBeneficiaryEntity.RequestBeneficiaryEntityAttribute entity) {
                return new Object[]{
                        entity.getRowKey(), entity.getActionHolder(),
                        entity.getName(), entity.getTypeHolder(), entity.getValue(),
                        entity.getParentAttribute(), entity.getChildAttributes(), entity.getDefaultMLSValue(), entity.getMlsMap()
                };
            }
        });
        targetEntitiesOfBeneficiaryTable.addActionListener(5, beneficiaryTargetEntityAdditionalValuesTable, RequestBeneficiaryEntity.RequestBeneficiaryEntityAttribute.class, new RowExtractor<RequestBeneficiaryEntity.RequestBeneficiaryEntityAttribute>() {
            @Override
            public Object[] getRowDetails(RequestBeneficiaryEntity.RequestBeneficiaryEntityAttribute entity) {
                return new Object[]{
                        entity.getRowKey(), entity.getActionHolder(),
                        entity.getName(), entity.getTypeHolder(), entity.getValue(),
                        entity.getParentAttribute(), entity.getChildAttributes(), entity.getDefaultMLSValue(), entity.getMlsMap()
                };
            }
        });

        targetEntitiesTable = new DetailsTable(new String[]{"Key", "Type", "Sub-type", "Operation", "Attributes", "Additional Attributes"}, this);
        targetEntityValuesTable = new DetailsTable(new String[]{"Row Key", "Action", "Name", "Type", "Value",
                "Parent", "Child", "Default", "MLS Map"}, this);
        targetEntityAdditionalValuesTable = new DetailsTable(new String[]{"Row Key", "Action", "Name", "Type", "Value",
                "Parent", "Child", "Default", "MLS Map"}, this);
        targetEntitiesTable.addActionListener(4, targetEntityValuesTable, RequestEntity.RequestEntityAttribute.class, new RowExtractor<RequestEntity.RequestEntityAttribute>() {
            @Override
            public Object[] getRowDetails(RequestEntity.RequestEntityAttribute targetEntityValue) {
                return new Object[]{
                        targetEntityValue.getRowKey(), targetEntityValue.getActionHolder(),
                        targetEntityValue.getName(), targetEntityValue.getTypeHolder(),
                        targetEntityValue.getValueHolder(), targetEntityValue.getParentAttribute(),
                        targetEntityValue.getChildAttributes(), targetEntityValue.getDefaultMLSValue(),
                        targetEntityValue.getMlsMap()
                };
            }
        });
        targetEntitiesTable.addActionListener(5, targetEntityAdditionalValuesTable, RequestEntity.RequestEntityAttribute.class, new RowExtractor<RequestEntity.RequestEntityAttribute>() {
            @Override
            public Object[] getRowDetails(RequestEntity.RequestEntityAttribute targetEntityValue) {
                return new Object[]{
                        targetEntityValue.getRowKey(), targetEntityValue.getActionHolder(),
                        targetEntityValue.getName(), targetEntityValue.getTypeHolder(),
                        targetEntityValue.getValueHolder(), targetEntityValue.getParentAttribute(),
                        targetEntityValue.getChildAttributes(), targetEntityValue.getDefaultMLSValue(),
                        targetEntityValue.getMlsMap()
                };
            }
        });

        templateAttributesTable = new DetailsTable(new String[]{"Name", "Type", "Value", "Target"}, this);
        childRequestTable = new DetailsTable(new String[]{"ID", "Model Name", "Status"}, this);
        JButton retrieve = JGComponentFactory.getCurrent().createButton("Retrieve..");
        retrieve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final String requestIDValue = requestID.getText();
                    if (!Utils.isEmpty(requestIDValue)) {
                        Utils.executeAsyncOperation("Loading Request Details " + requestIDValue, new Runnable() {
                            @Override
                            public void run() {
                                retrieveRequestDetails(requestIDValue);
                            }
                        });
                    }
                } catch (Exception exception) {
                    displayMessage("Failed to initiate request detail loading", "Failed to initiate retrieval of the request details Event: " + e, exception);
                }
            }
        });
        JPanel searchCriteria = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p")
                .addLabel("Request ID").xy(2, 2).add(requestID).xy(4, 2).add(retrieve).xy(8, 2)
                .build();
        JPanel requestAttributesDetailsPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 2dlu, [p,100dlu], 2dlu, p, 2dlu, [p,100dlu], 2dlu, p")
                .addSeparator("Request Attributes").xyw(2, 2, 7)
                .add(additionalAttributesTable).xyw(2, 4, 7)
                .addSeparator("Template Attributes").xyw(2, 6, 7)
                .add(templateAttributesTable).xyw(2, 8, 7)
                .build();
        JPanel beneficiariesDetailsPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, [p,50dlu], 2dlu, p, 2dlu, [p,50dlu], 2dlu, p, 2dlu, [p,70dlu], 2dlu, p, 2dlu, [p,50dlu], 2dlu")
                .add(beneficiaryTable).xyw(2, 2, 7)
                .addSeparator("Targets").xyw(2, 4, 7)
                .add(targetEntitiesOfBeneficiaryTable).xyw(2, 6, 7)
                .addSeparator("Entity Data").xyw(2, 8, 7)
                .add(beneficiaryTargetEntityValuesTable).xyw(2, 10, 7)
                .addSeparator("Additional Entity Data").xyw(2, 12, 7)
                .add(beneficiaryTargetEntityAdditionalValuesTable).xyw(2, 14, 7)
                .build();
        JPanel targetsDetailsPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, [p,70dlu], 5dlu, p, 2dlu, [p,100dlu], 5dlu, p, 2dlu, [p,70dlu]")
                .add(targetEntitiesTable).xyw(2, 2, 7)
                .addSeparator("Target Attributes").xyw(2, 4, 7)
                .add(targetEntityValuesTable).xyw(2, 6, 7) // add attribute details
                .addSeparator("Additional Attributes").xyw(2, 8, 7)
                .add(targetEntityAdditionalValuesTable).xyw(2, 10, 7) // add attribute details
                .build();
        JButton retrieveSelectedRequest = JGComponentFactory.getCurrent().createButton("Retrieve Selected Request");
        retrieveSelectedRequest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int selectedRow = childRequestTable.getSelectedRow();
                    if (selectedRow == -1) {
                        logger.trace("Nothing to do since no item is selected");
                        return;
                    }
                    final String requestId = (String) childRequestTable.tableModel.getValueAt(selectedRow, 0);
                    Utils.executeAsyncOperation("Loading Child Request [" + requestId + "]", new Runnable() {
                        @Override
                        public void run() {
                            try {
                                TraceRequestDetails childRequestDetails = new TraceRequestDetails(requestManager, orchestrationManager, "Request (" + requestId + ")", configuration, selectionTree, displayArea).setDestroyComponentOnClose(true).initialize();
                                childRequestDetails.retrieveRequestDetails(requestId);
                            } catch (Exception exception) {
                                displayMessage("Failed to load child request detail", "Failed to load child request details for request " + requestId, exception);
                            }
                        }
                    });
                } catch (Exception exception) {
                    displayMessage("Failed to initiate process retrieval", "Failed to process retrieval of child request event " + e, exception);
                }
            }
        });

        JPanel childRequestPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 5dlu, [p,100dlu], 5dlu")
                .add(retrieveSelectedRequest).xy(2, 2)
                .add(childRequestTable).xyw(2, 4, 7)
                .build();
        JPanel approvalDetailPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 5dlu, [p,100dlu], 5dlu")
                .add(approvalDataTable).xyw(2, 4, 7)
                .build();
        JideTabbedPane requestDetailsTabbedPane = new JideTabbedPane();
        requestDetailsTabbedPane.addTab("Request Attributes", requestAttributesDetailsPanel);
        requestDetailsTabbedPane.addTab("Beneficiaries", beneficiariesDetailsPanel);
        requestDetailsTabbedPane.addTab("Targets", targetsDetailsPanel);
        requestDetailsTabbedPane.addTab("Child Requests", childRequestPanel);
        JPanel requestDetailPanel = FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, [p,70], 2dlu, [p,70], 2dlu, fill:p:grow, 2dlu")
                .addLabel("Request ID").xy(2, 2).add(getRequestID).xy(4, 2).addLabel("Request Key").xy(6, 2).add(getRequestKey).xy(8, 2)
                .addLabel("Depends On").xy(2, 4).add(getDependsOnRequestId).xy(4, 4).addLabel("Requester Key").xy(6, 4).add(getRequesterKey).xy(8, 4)
                .addLabel("Beneficiary Type").xy(2, 6).add(beneficiaryType).xy(4, 6).addLabel("Model").xy(6, 6).add(getRequestModelName).xy(8, 6)
                .addLabel("Stage").xy(2, 8).add(getRequestStage).xy(4, 8).addLabel("Status").xy(6, 8).add(getRequestStatus).xy(8, 8)
                .addLabel("Create Date").xy(2, 10).add(creationDate).xy(4, 10).addLabel("Execution Date").xy(6, 10).add(getExecutionDate).xy(8, 10)
                .addLabel("End Date").xy(2, 12).add(getEndDate).xy(4, 12)
                .addLabel("Reason for Failure").xy(2, 14).add(new JideScrollPane(getReasonForFailure)).xyw(4, 14, 5)
                .addLabel("Justification").xy(2, 16).add(new JideScrollPane(getJustification)).xy(4, 16).addLabel("Request Context").xy(6, 16).add(new JideScrollPane(getRequestContext)).xy(8, 16)
                .add(requestDetailsTabbedPane).xyw(2, 18, 7)
                .build();
        JideTabbedPane tabbedPane = new JideTabbedPane();
        tabbedPane.addTab("Request Detail", requestDetailPanel);
        if (orchestrationManager != null) {
            orchestrationDetailPanel = new OrchestrationDetailUI(orchestrationManager, this).initialize();
            tabbedPane.addTab("Orchestration", orchestrationDetailPanel.getDisplayComponent());
        }
        tabbedPane.addTab("Approval Detail", approvalDetailPanel);
        traceRequestUI = new JPanel(new BorderLayout());
        traceRequestUI.add(searchCriteria, BorderLayout.NORTH);
        traceRequestUI.add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public JPanel getDisplayComponent() {
        return traceRequestUI;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying component {}", this);
        logger.debug("Destroyed component {}", this);
    }

    public interface RowExtractor<T> {

        Object[] getRowDetails(T dataObject);
    }

    public static class DetailsTable extends JGStripedTable {

        public final DefaultTableModel tableModel;
        public final AbstractUIComponent parent;

        public DetailsTable(String[] columnNames, AbstractUIComponent parent) {
            super(new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });
            tableModel = (DefaultTableModel) super.getModel();
            super.setDefaultRenderer(Boolean.class, new JGBooleanTableCellRenderer());
            for (String columnName : columnNames) {
                tableModel.addColumn(columnName);
            }
            this.parent = parent;
        }

        public <T> void addActionListener(final int columnIndex, final DetailsTable childTable, final Class<T> rowType, final RowExtractor<T> extractor) {
            getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    try {
                        if (e.getValueIsAdjusting())
                            return;
                        int selectedRow = getSelectedRow();
                        if (selectedRow == -1) {
                            logger.warn("Incorrect selection has been made or selection made has become invalid. Selected Row={}", selectedRow);
                            return;
                        }
                        childTable.tableModel.setRowCount(0);
                        Object targetEntitiesObject = tableModel.getValueAt(selectedRow, columnIndex);
                        if (targetEntitiesObject != null && targetEntitiesObject instanceof List) {
                            List<?> targetEntities = (List) targetEntitiesObject;
                            for (Object targetEntity : targetEntities) {
                                if (rowType.isAssignableFrom(targetEntity.getClass())) {
                                    T entity = rowType.cast(targetEntity);
                                    Object[] rowDetails = extractor.getRowDetails(entity);
                                    childTable.tableModel.addRow(rowDetails);
                                } else {
                                    logger.debug("Failed to locate an instance of {} in list {}, found {}", new Object[]{rowType, targetEntities, targetEntity});
                                }
                            }
                        } else {
                            logger.debug("Expected the column {} to have List of target entities but found {}", columnIndex, targetEntitiesObject);
                        }
                    } catch (Exception exception) {
                        parent.displayMessage("Row selection failed", "Failed to process row selection event " + e, exception);
                    }
                }
            });
        }
    }

}
