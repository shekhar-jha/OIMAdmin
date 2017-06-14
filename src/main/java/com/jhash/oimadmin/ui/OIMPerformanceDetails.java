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
import com.jhash.oimadmin.oim.perf.PerfConfiguration;
import com.jhash.oimadmin.oim.perf.PerfManager;
import com.jhash.oimadmin.oim.perf.PerformanceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;

public class OIMPerformanceDetails extends AbstractUIComponent<JComponent> {

    private static final Logger logger = LoggerFactory.getLogger(OIMPerformanceDetails.class);
    private final PerfManager performanceManager;
    private final List<PerfConfiguration> performanceDetails;
    private JTable performanceTable;
    private JLabel minimum = JGComponentFactory.getCurrent().createLabel();
    private JLabel maximum = JGComponentFactory.getCurrent().createLabel();
    private JLabel average = JGComponentFactory.getCurrent().createLabel();
    private JLabel median = JGComponentFactory.getCurrent().createLabel();
    private JLabel transactionCount = JGComponentFactory.getCurrent().createLabel();
    private JLabel totalTime = JGComponentFactory.getCurrent().createLabel();
    private JButton startTracking = JGComponentFactory.getCurrent().createButton("Start");
    private JButton exportData = JGComponentFactory.getCurrent().createButton("Export");
    private JComponent performanceUI;
    private boolean isRecording = false;

    public OIMPerformanceDetails(List<PerfConfiguration> performanceMetrics, PerfManager performanceManager, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.performanceDetails = performanceMetrics;
        this.performanceManager = performanceManager;
    }

    @Override
    public void initializeComponent() {
        String[] rows = new String[performanceDetails.size()];
        for (int rowCounter = 0; rowCounter < performanceDetails.size(); rowCounter++) {
            rows[rowCounter] = performanceDetails.get(rowCounter).displayName;
        }
        final DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn("Items", rows);
        performanceTable = JGComponentFactory.getCurrent().createTable(tableModel);
        performanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        performanceTable.setCellSelectionEnabled(true);
        startTracking.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                startTracking.setText("Taking snapshot...");
                startTracking.setEnabled(false);
                if (isRecording) {
                    Utils.executeAsyncOperation("Loading Performance Details", new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int lastColumnAdded = tableModel.getColumnCount() - 1;
                                for (int rowCounter = 0; rowCounter < performanceDetails.size(); rowCounter++) {
                                    PerformanceData value = (PerformanceData) tableModel.getValueAt(rowCounter, lastColumnAdded);
                                    if (value != null) {
                                        value.endSnapshot = performanceManager.capturePerformanceData(performanceDetails.get(rowCounter));
                                    }
                                }
                                String[] columnNames = new String[lastColumnAdded + 1];
                                for (int columnCounter = 0; columnCounter < columnNames.length; columnCounter++) {
                                    columnNames[columnCounter] = tableModel.getColumnName(columnCounter);
                                }
                                columnNames[lastColumnAdded] = new Date().toString();
                                tableModel.setColumnIdentifiers(columnNames);
                            } catch (Exception exception) {
                                displayMessage("Loading performance detail failed", "Failed to take performance snapshot after stopping measurement", exception);
                            }
                            isRecording = false;
                            startTracking.setText("Start");
                            startTracking.setEnabled(true);
                        }
                    });
                } else {
                    Utils.executeAsyncOperation("Loading Performance Details", new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PerformanceData[] startingPerformanceDetails = new PerformanceData[performanceDetails.size()];
                                for (int rowCounter = 0; rowCounter < startingPerformanceDetails.length; rowCounter++) {
                                    startingPerformanceDetails[rowCounter] = new PerformanceData();
                                    startingPerformanceDetails[rowCounter].startSnapshot = performanceManager.capturePerformanceData(performanceDetails.get(rowCounter));
                                }
                                tableModel.addColumn("Recording", startingPerformanceDetails);
                                startTracking.setText("Stop");
                                isRecording = true;
                            } catch (Exception exception) {
                                displayMessage("Loading performance detail failed", "Could not take a starting snapshot of values", exception);
                                startTracking.setText("Start");
                                isRecording = false;
                            }
                            startTracking.setEnabled(true);
                        }
                    });
                }
            }
        });
        performanceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedRow = performanceTable.getSelectedRow();
                int selectedColumn = performanceTable.getSelectedColumn();
                if (selectedColumn == 0) {
                    logger.trace("The first column does not have any performance detail. Ignoring selection");
                    return;
                }
                if (selectedRow == -1 || selectedColumn == -1) {
                    logger.warn("Incorrect selection has been made or selection made has become invalid. Selected Row={}, Column={}", selectedRow, selectedColumn);
                    return;
                }
                PerformanceData detail = (PerformanceData) tableModel.getValueAt(selectedRow, selectedColumn);
                String endMinimumValue = "", endMaximumValue = "", endAvgValue = "", endCompletedValue = "", endTotal_timeValue = "";
                String startMinimumValue = "", startMaximumValue = "", startAvgValue = "", startCompletedValue = "", startTotal_timeValue = "";
                if (detail.endSnapshot != null) {
                    endMinimumValue = detail.endSnapshot.get(PerfConfiguration.DATA_POINT.MIN);
                    endMaximumValue = detail.endSnapshot.get(PerfConfiguration.DATA_POINT.MAX);
                    endAvgValue = detail.endSnapshot.get(PerfConfiguration.DATA_POINT.AVG);
                    endCompletedValue = detail.endSnapshot.get(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS);
                    endTotal_timeValue = detail.endSnapshot.get(PerfConfiguration.DATA_POINT.TOTAL_TRANSACTION_TIME);
                }
                if (detail.startSnapshot != null) {
                    startMinimumValue = detail.startSnapshot.get(PerfConfiguration.DATA_POINT.MIN);
                    startMaximumValue = detail.startSnapshot.get(PerfConfiguration.DATA_POINT.MAX);
                    startAvgValue = detail.startSnapshot.get(PerfConfiguration.DATA_POINT.AVG);
                    startCompletedValue = detail.startSnapshot.get(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS);
                    startTotal_timeValue = detail.startSnapshot.get(PerfConfiguration.DATA_POINT.TOTAL_TRANSACTION_TIME);
                }
                minimum.setText(endMinimumValue + ((!endMinimumValue.equals(startMinimumValue)) ? (" (" + startMinimumValue + ")") : ""));
                maximum.setText(endMaximumValue + ((!endMaximumValue.equals(startMaximumValue)) ? (" (" + startMaximumValue + ")") : ""));
                average.setText(endAvgValue + ((!endAvgValue.equals(startAvgValue)) ? (" (" + startAvgValue + ")") : ""));
                transactionCount.setText(endCompletedValue + ((!endCompletedValue.equals(startCompletedValue)) ? (" (" + startCompletedValue + ")") : ""));
                totalTime.setText(endTotal_timeValue + ((!endTotal_timeValue.equals(startTotal_timeValue)) ? (" (" + startTotal_timeValue + ")") : ""));
            }
        });
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        exportData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = fileChooser.showSaveDialog(OIMPerformanceDetails.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fileChooser.getSelectedFile();
                    try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(saveFile, false))) {
                        fileWriter.append("Items,");
                        int numberOfColumns = tableModel.getColumnCount();
                        for (int columnCounter = 1; columnCounter < numberOfColumns; columnCounter++) {
                            String columnName = tableModel.getColumnName(columnCounter);
                            fileWriter.append(columnName).append("-starting_minimum,");
                            fileWriter.append(columnName).append("-starting_maximum,");
                            fileWriter.append(columnName).append("-starting_average,");
                            fileWriter.append(columnName).append("-starting_transactions,");
                            fileWriter.append(columnName).append("-starting_total_time,");
                            fileWriter.append(columnName).append("-end_minimum,");
                            fileWriter.append(columnName).append("-end_maximum,");
                            fileWriter.append(columnName).append("-end_average,");
                            fileWriter.append(columnName).append("-end_transactions,");
                            fileWriter.append(columnName).append("-end_total_time,");
                        }
                        fileWriter.newLine();
                        for (int counter = 0; counter < tableModel.getRowCount(); counter++) {
                            for (int columnCounter = 0; columnCounter < numberOfColumns; columnCounter++) {
                                Object cellData = tableModel.getValueAt(counter, columnCounter);
                                if (columnCounter == 0) {
                                    fileWriter.append(cellData.toString()).append(",");
                                } else if (cellData instanceof PerformanceData) {
                                    PerformanceData detail = (PerformanceData) cellData;
                                    if (detail.startSnapshot != null) {
                                        fileWriter.append(detail.startSnapshot.get(PerfConfiguration.DATA_POINT.MIN)).append(",");
                                        fileWriter.append(detail.startSnapshot.get(PerfConfiguration.DATA_POINT.MAX)).append(",");
                                        fileWriter.append(detail.startSnapshot.get(PerfConfiguration.DATA_POINT.AVG)).append(",");
                                        fileWriter.append(detail.startSnapshot.get(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS)).append(",");
                                        fileWriter.append(detail.startSnapshot.get(PerfConfiguration.DATA_POINT.TOTAL_TRANSACTION_TIME)).append(",");
                                    } else {
                                        fileWriter.append(",,,,,");
                                    }
                                    if (detail.endSnapshot != null) {
                                        fileWriter.append(detail.endSnapshot.get(PerfConfiguration.DATA_POINT.MIN)).append(",");
                                        fileWriter.append(detail.endSnapshot.get(PerfConfiguration.DATA_POINT.MAX)).append(",");
                                        fileWriter.append(detail.endSnapshot.get(PerfConfiguration.DATA_POINT.AVG)).append(",");
                                        fileWriter.append(detail.endSnapshot.get(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS)).append(",");
                                        fileWriter.append(detail.endSnapshot.get(PerfConfiguration.DATA_POINT.TOTAL_TRANSACTION_TIME)).append(",");
                                    } else {
                                        fileWriter.append(",,,,,");
                                    }
                                } else {
                                    logger.debug("Skipping line since invalid data detected {}", cellData);
                                }
                            }
                            fileWriter.newLine();
                        }
                    } catch (Exception exception) {
                        displayMessage("Performance Data save failed", "Failed to save the Performance details to file " + saveFile, exception);
                    }
                }
            }
        });
        performanceUI = buildPanel();
    }

    private JComponent buildPanel() {
        return FormBuilder.create().columns("3dlu, right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("3dlu, p, 3dlu, p, 7dlu, p, 3dlu, p, 3dlu, p")
                .add(startTracking).xy(4, 2).add(exportData).xy(8, 2)
                .add(performanceTable).xyw(2, 4, 7)
                .addLabel("Minimum (ms)").xy(2, 6).add(minimum).xy(4, 6).addLabel("Maximum (ms)").xy(6, 6).add(maximum).xy(8, 6)
                .addLabel("Average (ms)").xy(2, 8).add(average).xy(4, 8).addLabel("Count").xy(6, 8).add(transactionCount).xy(8, 8)
                .addLabel("Median").xy(2, 10).add(median).xy(4, 10).addLabel("Total Time").xy(6, 10).add(totalTime).xy(8, 10)
                .build();
    }

    @Override
    public JComponent getDisplayComponent() {
        return performanceUI;
    }

    @Override
    public void destroyComponent() {

    }

}
