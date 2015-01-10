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
import com.jhash.oimadmin.oim.MDSConnectionJMX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDSFileTreeNode extends AbstractUIComponentTreeNode<MDSConnectionJMX.MDSFile> implements DisplayableNode<MDSFileDetails> {

    private static final Logger logger = LoggerFactory.getLogger(MDSFileTreeNode.class);
    private final MDSConnectionJMX.MDSFile mdsFile;
    private final MDSPartitionTreeNode associatedPartition;

    private MDSFileDetails mdsFileDetails;

    public MDSFileTreeNode(String name, MDSPartitionTreeNode associatedPartition, MDSConnectionJMX.MDSFile mdsFile, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.mdsFile = mdsFile;
        this.associatedPartition = associatedPartition;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing MDS File UI {}", this);
        mdsFileDetails = new MDSFileDetails(name, associatedPartition, mdsFile, configuration, selectionTree, displayArea);
        logger.debug("Initialized MDS File UI {} ...", this);
    }

    @Override
    public MDSFileDetails getDisplayComponent() {
        return mdsFileDetails;
    }

    @Override
    public MDSConnectionJMX.MDSFile getComponent() {
        return this.mdsFile;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying MDS component {}...", this);
        if (mdsFileDetails != null) {
            try {
                mdsFileDetails.destroy();
            } catch (Exception exception) {
                logger.warn("Failed to destroy MDS File UI " + this, exception);
            }
            mdsFileDetails = null;
        }
        logger.debug("Destroyed MDS component {}", this);
    }

}