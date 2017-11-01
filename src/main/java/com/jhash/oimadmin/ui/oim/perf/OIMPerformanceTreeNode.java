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

package com.jhash.oimadmin.ui.oim.perf;

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.perf.PerfConfiguration;
import com.jhash.oimadmin.oim.perf.PerfManager;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.componentTree.DisplayComponentNode;
import com.jhash.oimadmin.ui.menu.MenuHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OIMPerformanceTreeNode extends AbstractUIComponentTreeNode<OIMPerformanceTreeNode> {


    public static MenuHandler.MENU NEW_PERFORMANCE_DETAIL = new MenuHandler.MENU("Performance Detail", MenuHandler.MENU.NEW, "New Performance Details");
    private static final Logger logger = LoggerFactory.getLogger(OIMPerformanceTreeNode.class);
    private final PerfManager performanceManager;
    private Map<String, List<PerfConfiguration>> performanceItemDetails = new HashMap<>();
    private String oimServerName;
    private boolean oimServerPerformanceBeanEnabled;

    public OIMPerformanceTreeNode(PerfManager performanceManager, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        logger.debug("OIMPerformanceTreeNode({}, {})", new Object[]{name, parentComponent});
        this.performanceManager = performanceManager;
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing {}", this);
        final Map<String, Boolean> performanceConfiguration = performanceManager.performanceConfigurationForServer();
        if (performanceConfiguration.size() > 0) {
            Map<String, String> oimServerPerformanceConfiguration = new HashMap<>();
            for (Map.Entry<String, Boolean> performanceConfigForServer : performanceConfiguration.entrySet()) {
                oimServerPerformanceConfiguration.put(performanceConfigForServer.getKey() + " (" + (performanceConfigForServer.getValue() ? "Enabled" : "Disabled") + ")", performanceConfigForServer.getKey());
            }
            Object selectedServerValue = JOptionPane.showInputDialog(Frame.getFrames()[0], "Server to monitor for performance", " Performance", JOptionPane.QUESTION_MESSAGE, null, oimServerPerformanceConfiguration.keySet().toArray(), null);
            if (selectedServerValue instanceof String) {
                oimServerName = oimServerPerformanceConfiguration.get(selectedServerValue);
                if (oimServerName == null)
                    throw new OIMAdminException("Please provide valid server name");
            } else {
                throw new OIMAdminException("Please select a server for monitoring.");
            }
        } else {
            oimServerName = performanceConfiguration.keySet().toArray(new String[0])[0];
        }
        logger.debug("Performance needs to be monitored on server {}", oimServerName);
        if (!performanceConfiguration.get(oimServerName)) {
            oimServerPerformanceBeanEnabled = true;
            performanceManager.enablePerformance(oimServerName);
        }
        registerMenu(NEW_PERFORMANCE_DETAIL, new MenuHandler.ActionHandler() {
            @Override
            public void invoke(MenuHandler.MENU menuItem, MenuHandler.Context context) {
                Utils.executeAsyncOperation("New Performance Detail", new Runnable() {
                    @Override
                    public void run() {
                        List<PerfConfiguration> perfConfiguration = new ArrayList<PerfConfiguration>();
                        perfConfiguration.addAll(performanceManager.getPerformanceConfiguration(oimServerName));
                        new OIMPerformanceDetails(oimServerName, perfConfiguration, performanceManager, "New Performance", OIMPerformanceTreeNode.this)
                                .setDestroyComponentOnClose(true).setPublish(true).initialize();
                    }
                });
            }
        });
        performanceItemDetails = performanceManager.getPerformanceConfiguration(getConfiguration());
        for (Map.Entry<String, List<PerfConfiguration>> performanceItem : performanceItemDetails.entrySet()) {
            new DisplayComponentNode<>(performanceItem.getKey(),
                    new OIMPerformanceDetails(oimServerName, performanceItem.getValue(), performanceManager, performanceItem.getKey(), this),
                    this).initialize();
        }
        logger.debug("Initialized {}", this);
    }

    @Override
    public void destroyNode() {
        logger.debug("Destroying {}", this);
        if (oimServerName != null && oimServerPerformanceBeanEnabled) {
            performanceManager.setPerformance(oimServerName, false);
        }
        if (performanceItemDetails != null) {
            performanceItemDetails.clear();
        }
        logger.debug("Destroyed {}", this);
    }
}
