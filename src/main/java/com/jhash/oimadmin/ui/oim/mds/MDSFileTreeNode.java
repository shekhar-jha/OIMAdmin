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
package com.jhash.oimadmin.ui.oim.mds;

import com.jhash.oimadmin.oim.mds.MDSFile;
import com.jhash.oimadmin.ui.UIComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDSFileTreeNode extends AbstractUIComponentTreeNode<MDSFileTreeNode> implements UIComponent<MDSFileDetails> {

    private static final Logger logger = LoggerFactory.getLogger(MDSFileTreeNode.class);
    private final MDSFile mdsFile;
    private final MDSPartitionTreeNode associatedPartition;

    private MDSFileDetails mdsFileDetails;

    public MDSFileTreeNode(String name, MDSPartitionTreeNode associatedPartition, ParentComponent parentComponent) {
        super(name, parentComponent, INITIALIZED_NO_OP);
        this.mdsFile = null;
        this.associatedPartition = associatedPartition;
        unregisterMenu(OPEN);
    }

    public MDSFileTreeNode(String name, MDSPartitionTreeNode associatedPartition, MDSFile mdsFile, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.mdsFile = mdsFile;
        this.associatedPartition = associatedPartition;
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing MDS File UI {}", this);
        if (mdsFile != null) {
            mdsFileDetails = new MDSFileDetails(getName(), associatedPartition, mdsFile, this);
        }
        logger.debug("Initialized MDS File UI {} ...", this);
    }

    @Override
    public MDSFileDetails getComponent() {
        return mdsFileDetails;
    }

    @Override
    public void destroyNode() {
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