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

import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.mds.MDSFile;
import com.jhash.oimadmin.ui.UIComponent;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import com.jhash.oimadmin.ui.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.jar.JarEntry;

public class MDSFileTreeNode extends AbstractUIComponentTreeNode<MDSFileTreeNode> implements UIComponent<MDSFileDetails> {

    public static final MenuHandler.MENU NEW_MDS_FILE = new MenuHandler.MENU("MDS File", MenuHandler.MENU.NEW, "New MDS File");
    private static final Logger logger = LoggerFactory.getLogger(MDSFileTreeNode.class);
    private final MDSFile mdsFile;
    private final MDSPartitionTreeNode associatedPartition;

    private MDSFileDetails mdsFileDetails;

    public MDSFileTreeNode(String name, final String jarEntry, final MDSPartitionTreeNode associatedPartition, ParentComponent parentComponent) {
        super(name, parentComponent, INITIALIZED_NO_OP);
        this.mdsFile = null;
        this.associatedPartition = associatedPartition;
        registerMenu(NEW_MDS_FILE, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                try {
                    File file = UIUtils.selectFile(Boolean.FALSE, "Select MDS File...");
                    if (file != null && file.exists() && file.isFile() && file.canRead()) {
                        String content = Utils.readFile(file.getAbsolutePath());
                        logger.debug("Creating a new MDS File {} under {}", file.getName(), jarEntry);
                        final MDSFile newFile = new MDSFile(associatedPartition.getName(), null, new JarEntry(jarEntry + "/" + file.getName()));
                        newFile.setContent(content);
                        Utils.executeAsyncOperation("MDS File " +getName() + " [Saving]", new Runnable() {
                            @Override
                            public void run() {
                                logger.debug("Trying to save MDS File {}", newFile);
                                associatedPartition.save(newFile);
                                logger.debug("Saved. Trying to start the refresh of the MDS Partition tree by destroying Partition Tree Node {}", associatedPartition);
                                associatedPartition.destroy(false);
                                logger.debug("Trying to initialize the MDS Partition tree node {}", associatedPartition);
                                associatedPartition.initialize();
                                logger.debug("Initialized MDS Partition tree node. Completed MDS File saving process");
                            }
                        });
                    } else {
                        displayMessage("New MDS File", "Selected MDS File is not valid.", null);
                    }
                } catch (Exception exception) {
                    displayMessage("New MDS File", "Failed to save new MDS File.", exception);
                }
            }
        });
        unregisterMenu(OPEN);
    }

    public MDSFileTreeNode(String name, final MDSPartitionTreeNode associatedPartition, final MDSFile mdsFile, ParentComponent parentComponent) {
        super(name, parentComponent);
        this.mdsFile = mdsFile;
        this.associatedPartition = associatedPartition;
        registerMenu(DELETE, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                Utils.executeAsyncOperation("MDS File " +getName() + " [Deleting]", new Runnable() {
                    @Override
                    public void run() {
                        String fileToDelete = "/" + mdsFile.getFile().getName(); // file name is without / at start.
                        logger.debug("Trying to delete MDS File {}", fileToDelete);
                        associatedPartition.delete(fileToDelete);
                        logger.debug("Saved. Trying to start the refresh of the MDS Partition tree by destroying Partition Tree Node {}", associatedPartition);
                        associatedPartition.destroy(false);
                        logger.debug("Trying to initialize the MDS Partition tree node {}", associatedPartition);
                        associatedPartition.initialize();
                        logger.debug("Initialized MDS Partition tree node. Completed MDS File saving process");
                    }
                });
            }
        });
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