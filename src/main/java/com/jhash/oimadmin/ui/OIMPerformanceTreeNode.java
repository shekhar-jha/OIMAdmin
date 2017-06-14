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

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.UIComponentTree;
import com.jhash.oimadmin.oim.perf.PerfConfiguration;
import com.jhash.oimadmin.oim.perf.PerfManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OIMPerformanceTreeNode extends AbstractUIComponentTreeNode<PerfManager> implements DisplayableNode<OIMPerformanceDetails> {


    private static final Logger logger = LoggerFactory.getLogger(OIMPerformanceTreeNode.class);
    private final PerfManager performanceManager;
    private Map<String, List<PerfConfiguration>> performanceItemDetails = new HashMap<>();

    public OIMPerformanceTreeNode(PerfManager performanceManager, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        logger.debug("OIMPerformanceTreeNode({}, {}, {}, {})", new Object[]{name, configuration, selectionTree, displayArea});
        this.performanceManager = performanceManager;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        performanceItemDetails = performanceManager.getPerformanceConfiguration(configuration);
        for (Map.Entry<String, List<PerfConfiguration>> performanceItem : performanceItemDetails.entrySet()) {
            AbstractUIComponentTreeNode node = new AbstractUIComponentTreeNode.DisplayComponentNode<>(performanceItem.getKey(),
                    new OIMPerformanceDetails(performanceItem.getValue(), performanceManager, performanceItem.getKey(), configuration, selectionTree, displayArea),
                    performanceItem.getValue(), configuration, selectionTree, displayArea).initialize();
            logger.trace("Adding node {} for item {}", node, performanceItem);
            selectionTree.addChildNode(this, node);
        }
        logger.debug("Initialized {}", this);
    }

    @Override
    public PerfManager getComponent() {
        return performanceManager;
    }

    @Override
    public OIMPerformanceDetails getDisplayComponent() {
        return null;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying {}", this);
        if (performanceItemDetails != null) {
            performanceItemDetails.clear();
        }
        logger.debug("Destroyed {}", this);
    }
}
