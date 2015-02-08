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
import com.jgoodies.jsdl.component.JGStripedTable;
import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.oim.OIMJMXWrapper;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class EventHandlerDetails extends AbstractUIComponent<JPanel> {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlerDetails.class);

    private final OIMJMXWrapper.OperationDetail eventHandlerDetails;
    private final OIMJMXWrapper connection;

    private JPanel eventHandlerUI;
    private JGStripedTable table;
    private JLabel nameLabel = JGComponentFactory.getCurrent().createLabel();
    private JLabel stageLabel = JGComponentFactory.getCurrent().createLabel();
    private JLabel orderLabel = JGComponentFactory.getCurrent().createLabel();
    private JLabel customLabel = JGComponentFactory.getCurrent().createLabel();
    private JLabel conditionalLabel = JGComponentFactory.getCurrent().createLabel();
    private JLabel offBandLabel = JGComponentFactory.getCurrent().createLabel();
    private JTextField classNameLabel = JGComponentFactory.getCurrent().createTextField();
    private JTextField locationLabel = JGComponentFactory.getCurrent().createTextField();

    public EventHandlerDetails(String name, OIMJMXWrapper.OperationDetail eventHandlerDetails, OIMJMXWrapper connection, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.connection = connection;
        this.eventHandlerDetails = eventHandlerDetails;
    }

    @Override
    public void initializeComponent() {
        classNameLabel.setEditable(false);
        classNameLabel.setBackground(null);
        classNameLabel.setBorder(null);
        locationLabel.setEditable(false);
        locationLabel.setBackground(null);
        locationLabel.setBorder(null);
        OIMJMXWrapper.Details details = connection.getEventHandlers(eventHandlerDetails);
        DefaultTableModel tableModel = new DefaultTableModel(details.getData(), details.getColumns()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = JGComponentFactory.getCurrent().createReadOnlyTable(tableModel);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedIndex = table.getSelectedRow();
                Map<String, Object> detail = details.getItemAt(selectedIndex);
                nameLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.NAME.columnName).toString());
                stageLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.STAGE.columnName).toString());
                orderLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.ORDER.columnName).toString());
                customLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.CUSTOM.columnName).toString());
                conditionalLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.CONDITIONAL.columnName).toString());
                offBandLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.OFFBAND.columnName).toString());
                classNameLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.CLASS.columnName).toString());
                locationLabel.setText(detail.get(OIMJMXWrapper.EVENT_HANDLER_DETAILS.LOCATION.columnName).toString());
            }
        });
        eventHandlerUI = buildDetailScreen();
    }

    @Override
    public JPanel getComponent() {
        return eventHandlerUI;
    }

    private JPanel buildDetailScreen() {
        JPanel eventHandlerDetailPanel = new JPanel(new BorderLayout());
        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        FormLayout eventHandlerFormLayout = new FormLayout(
                "right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu");
        eventHandlerFormLayout.setColumnGroups(new int[][]{{1, 5}});
        CellConstraints cellConstraint = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(eventHandlerFormLayout);
        builder.addLabel("Name", cellConstraint.xy(1, 1));
        builder.add(nameLabel, cellConstraint.xy(3, 1));
        builder.addLabel("Custom", cellConstraint.xy(5, 1));
        builder.add(customLabel, cellConstraint.xy(7, 1));
        builder.addLabel("Stage", cellConstraint.xy(1, 3));
        builder.add(stageLabel, cellConstraint.xy(3, 3));
        builder.addLabel("Order", cellConstraint.xy(5, 3));
        builder.add(orderLabel, cellConstraint.xy(7, 3));
        builder.addLabel("Conditional", cellConstraint.xy(1, 5));
        builder.add(conditionalLabel, cellConstraint.xy(3, 5));
        builder.addLabel("Off Band", cellConstraint.xy(5, 5));
        builder.add(offBandLabel, cellConstraint.xy(7, 5));
        builder.addLabel("Location", cellConstraint.xy(1, 7));
        builder.add(locationLabel, cellConstraint.xyw(3, 7, 5));
        builder.addLabel("Class", cellConstraint.xy(1, 9));
        builder.add(classNameLabel, cellConstraint.xyw(3, 9, 5));

        splitPane.add(new JideScrollPane(table), 0);
        splitPane.add(builder.getPanel(), 1);
        splitPane.setProportionalLayout(true);
        splitPane.setProportions(new double[]{0.6});
        eventHandlerDetailPanel.add(splitPane, BorderLayout.CENTER);
        logger.debug("Returning the developed component {}", eventHandlerDetailPanel);
        return eventHandlerDetailPanel;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroyed {}", this);
    }

}
