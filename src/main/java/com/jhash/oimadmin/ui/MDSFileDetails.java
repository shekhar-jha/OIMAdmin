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

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.MDSConnectionJMX;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MDSFileDetails extends AbstractUIComponent<JPanel> {

    private static final Logger logger = LoggerFactory.getLogger(MDSFileDetails.class);

    private final MDSConnectionJMX.MDSFile mdsFile;
    private final MDSPartitionTreeNode associatedPartition;

    private JPanel mdsPartitionFilePanel = new JPanel(new BorderLayout());
    private JTextArea mdsFileTextArea = new JTextArea();
    private JideButton saveButton = new JideButton("Save");

    public MDSFileDetails(String name, MDSPartitionTreeNode associatedPartition, MDSConnectionJMX.MDSFile mdsFile, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.mdsFile = mdsFile;
        this.associatedPartition = associatedPartition;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        logger.debug("Trying to read MDS File {}", mdsFile);
        String content = mdsFile.getContent();
        logger.trace("Read file content {} and setting it to text area {}", content, mdsFileTextArea);
        mdsFileTextArea.setText(content);
        mdsPartitionFilePanel.add(new JideScrollPane(mdsFileTextArea), BorderLayout.CENTER);
        saveButton.setActionCommand("SAVE");
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    logger.debug("Trying to setup Saving of MDS File {}", name);
                    saveButton.setEnabled(false);
                    Utils.executeAsyncOperation("MDS File " + name + " [Saving]", new Runnable() {

                        @Override
                        public void run() {
                            saveMDSFile();
                        }
                    });
                    logger.debug("Completed setup of MDS File saving.");
                } catch (Exception exception) {
                    logger.warn("Failed to setup Saving of MDS File {}",
                            name, exception);
                }
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        mdsPartitionFilePanel.add(buttonPanel, BorderLayout.NORTH);
        logger.debug("Initialized {}", this);
    }

    @Override
    public JPanel getComponent() {
        return mdsPartitionFilePanel;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroyed {}", this);
    }

    private void saveMDSFile() {
        logger.debug("Trying to get content of MDS File to be saved.");
        String operand = mdsFileTextArea.getText();
        logger.trace("Content {}", operand);
        logger.debug("Overwriting the content of MDS File {} with content", mdsFile);
        mdsFile.setContent(operand);
        logger.debug("Trying to save updated MDS File");
        mdsFile.save();
        logger.debug("Saved. Trying to start the refresh of the MDS Partition tree by destroying Partition Tree Node {}", associatedPartition);
        associatedPartition.destroy();
        logger.debug("Trying to initialize the MDS Partition tree node {}", associatedPartition);
        associatedPartition.initialize();
        logger.debug("Initialized MDS Partition tree node. Completed MDS File saving process");
    }
}
