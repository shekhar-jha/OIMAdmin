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

import com.jhash.oimadmin.oim.perf.PerfConfiguration;
import com.jhash.oimadmin.oim.perf.PerfManager;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.componentTree.AbstractUIComponentTreeNode;
import com.jhash.oimadmin.ui.componentTree.DisplayComponentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OIMPerformanceTreeNode extends AbstractUIComponentTreeNode<OIMPerformanceTreeNode> {


    private static final Logger logger = LoggerFactory.getLogger(OIMPerformanceTreeNode.class);
    private final PerfManager performanceManager;
    private Map<String, List<PerfConfiguration>> performanceItemDetails = new HashMap<>();

    public OIMPerformanceTreeNode(PerfManager performanceManager, String name, ParentComponent parentComponent) {
        super(name, parentComponent);
        logger.debug("OIMPerformanceTreeNode({}, {})", new Object[]{name, parentComponent});
        this.performanceManager = performanceManager;
    }

    @Override
    public void setupNode() {
        logger.debug("Initializing {}", this);
        performanceItemDetails = performanceManager.getPerformanceConfiguration(getConfiguration());
        for (Map.Entry<String, List<PerfConfiguration>> performanceItem : performanceItemDetails.entrySet()) {
            new DisplayComponentNode<>(performanceItem.getKey(),
                    new OIMPerformanceDetails(performanceItem.getValue(), performanceManager, performanceItem.getKey(), this),
                    this).initialize();
        }
        logger.debug("Initialized {}", this);
    }

    @Override
    public void destroyNode() {
        logger.debug("Destroying {}", this);
        if (performanceItemDetails != null) {
            performanceItemDetails.clear();
        }
        logger.debug("Destroyed {}", this);
    }
}
