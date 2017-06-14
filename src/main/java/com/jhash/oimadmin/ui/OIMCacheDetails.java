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
import com.jhash.oimadmin.oim.Details;
import com.jhash.oimadmin.oim.cache.CacheManager;
import com.jhash.oimadmin.oim.cache.CacheManager.OIM_CACHE_ATTRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

public class OIMCacheDetails extends AbstractUIComponent<JComponent> {

    private static final Logger logger = LoggerFactory.getLogger(OIMCacheDetails.class);
    private static final String DEFAULT_CACHE_CONFIG = "Default Cache Configuration";

    private final CacheManager cacheManager;
    private final Map<OIM_CACHE_ATTRS, String> changedDefaultValues = new HashMap<>();
    private final Map<OIM_CACHE_ATTRS, String> changedCategoryValues = new HashMap<>();
    private JCheckBox clustered = JGComponentFactory.getCurrent().createCheckBox("Is Cache clustered?");
    private JCheckBox enabled = JGComponentFactory.getCurrent().createCheckBox("Is Cache Enabled?");
    private JTextField expirationTime = JGComponentFactory.getCurrent().createTextField();
    private JCheckBox threadLocalEnabled = JGComponentFactory.getCurrent().createCheckBox("ThreadLocalCacheEnabled?");
    private JTextField cacheProviderClass = JGComponentFactory.getCurrent().createTextField();
    private JTextField multiCastAddress = JGComponentFactory.getCurrent().createTextField();
    private JTextField multiCastConfig = JGComponentFactory.getCurrent().createTextField();
    private JTextField cacheSize = JGComponentFactory.getCurrent().createTextField();
    private JTable cacheCategoryTable;
    private JLabel cacheCategoryName = new JLabel();
    private JCheckBox cacheCategoryEnabled = JGComponentFactory.getCurrent().createCheckBox("Enabled?");
    private JTextField cacheCategoryExpires = JGComponentFactory.getCurrent().createTextField();
    private JButton saveDefaultValues = JGComponentFactory.getCurrent().createButton("Save\u2026");
    private JButton saveCategoryValues = JGComponentFactory.getCurrent().createButton("Save\u2026");
    private JButton resetDefaultValues = JGComponentFactory.getCurrent().createButton("Reset");

    private JComponent cacheDetailUI;

    public OIMCacheDetails(CacheManager cacheManager, String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        super(name, configuration, selectionTree, displayArea);
        this.cacheManager = cacheManager;
    }

    @Override
    public void initializeComponent() {
        logger.debug("Initializing {}", this);
        resetDefaultUI();
        clustered.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    changedDefaultValues.put(OIM_CACHE_ATTRS.CLUSTERED, "true");
                } else {
                    changedDefaultValues.put(OIM_CACHE_ATTRS.CLUSTERED, "false");
                }
            }
        });
        enabled.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    changedDefaultValues.put(OIM_CACHE_ATTRS.ENABLED, "true");
                } else {
                    changedDefaultValues.put(OIM_CACHE_ATTRS.ENABLED, "false");
                }
            }
        });
        expirationTime.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.ExpirationTime, expirationTime.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.ExpirationTime, expirationTime.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.ExpirationTime, expirationTime.getText());
            }
        });
        threadLocalEnabled.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    changedDefaultValues.put(OIM_CACHE_ATTRS.ThreadLocalCacheEnabled, "true");
                } else {
                    changedDefaultValues.put(OIM_CACHE_ATTRS.ThreadLocalCacheEnabled, "false");
                }
            }
        });
        cacheProviderClass.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.Provider, cacheProviderClass.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.Provider, cacheProviderClass.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.Provider, cacheProviderClass.getText());
            }
        });
        multiCastAddress.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.MulticastAddress, multiCastAddress.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.MulticastAddress, multiCastAddress.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.MulticastAddress, multiCastAddress.getText());
            }
        });
        multiCastConfig.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.MulticastConfig, multiCastConfig.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.MulticastConfig, multiCastConfig.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.MulticastConfig, multiCastConfig.getText());
            }
        });
        cacheSize.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.Size, cacheSize.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.Size, cacheSize.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changedDefaultValues.put(OIM_CACHE_ATTRS.Size, cacheSize.getText());
            }
        });
        final Details cacheCategory = cacheManager.getCacheCategories();
        DefaultTableModel tableModel = new DefaultTableModel(cacheCategory.getData(), cacheCategory.getColumns()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cacheCategoryEnabled.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    changedCategoryValues.put(OIM_CACHE_ATTRS.ENABLED, "true");
                } else {
                    changedCategoryValues.put(OIM_CACHE_ATTRS.ENABLED, "false");
                }
            }
        });
        cacheCategoryExpires.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedCategoryValues.put(OIM_CACHE_ATTRS.ExpirationTime, cacheCategoryExpires.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedCategoryValues.put(OIM_CACHE_ATTRS.ExpirationTime, cacheCategoryExpires.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changedCategoryValues.put(OIM_CACHE_ATTRS.ExpirationTime, cacheCategoryExpires.getText());
            }
        });
        cacheCategoryTable = JGComponentFactory.getCurrent().createReadOnlyTable(tableModel);
        cacheCategoryTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cacheCategoryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedIndex = cacheCategoryTable.getSelectedRow();
                Map<String, Object> detail = cacheCategory.getItemAt(selectedIndex);
                cacheCategoryName.setText(detail.get("Name").toString());
                cacheCategoryEnabled.setSelected((Boolean) detail.get("Enabled?"));
                cacheCategoryExpires.setText(detail.get("Expires in").toString());
                changedCategoryValues.clear();
            }
        });
        saveDefaultValues.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.executeAsyncOperation("Save Cache Defaults", new Runnable() {
                    @Override
                    public void run() {
                        for (OIM_CACHE_ATTRS cacheAttr : changedDefaultValues.keySet()) {
                            try {
                                cacheManager.setCacheDetails(null, cacheAttr, changedDefaultValues.get(cacheAttr));
                            } catch (Exception exception) {
                                displayMessage("Configuration update failed.", "Failed to set " + cacheAttr + " to " + changedDefaultValues.get(cacheAttr) + ". ", exception);
                            }
                        }
                        changedDefaultValues.clear();
                    }
                });
            }
        });
        saveCategoryValues.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.executeAsyncOperation("Save Cache Category Changes", new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int selectedRow = cacheCategoryTable.getSelectedRow();
                            Map<String, Object> detail = cacheCategory.getItemAt(selectedRow);
                            for (OIM_CACHE_ATTRS cacheAttr : changedCategoryValues.keySet()) {
                                String value = changedCategoryValues.get(cacheAttr);
                                cacheManager.setCacheDetails(detail, cacheAttr, value);
                                switch (cacheAttr) {
                                    case ENABLED:
                                        cacheCategoryTable.setValueAt(value, selectedRow, 1);
                                        detail.put("Enabled?", Boolean.valueOf(value));
                                        break;
                                    case ExpirationTime:
                                        cacheCategoryTable.setValueAt(value, selectedRow, 2);
                                        detail.put("Expires in", value);
                                        break;
                                }
                            }
                            changedCategoryValues.clear();
                        } catch (Exception exception) {
                            displayMessage("Configuration update failed.", "Failed to set changed cache configuration " + changedCategoryValues, exception);
                        }
                    }
                });
            }
        });
        resetDefaultValues.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.executeAsyncOperation("Reloading OIM Cache Details", new Runnable() {
                    @Override
                    public void run() {
                        resetDefaultUI();
                    }
                });
            }
        });
        cacheDetailUI = buildPanel();
        logger.debug("Initialized {}", this);
    }

    private void resetDefaultUI() {
        clustered.setSelected(cacheManager.<Boolean>getCacheDetails(OIM_CACHE_ATTRS.CLUSTERED));
        enabled.setSelected(cacheManager.<Boolean>getCacheDetails(OIM_CACHE_ATTRS.ENABLED));
        expirationTime.setText(cacheManager.<Integer>getCacheDetails(OIM_CACHE_ATTRS.ExpirationTime).toString());
        threadLocalEnabled.setSelected(cacheManager.<Boolean>getCacheDetails(OIM_CACHE_ATTRS.ThreadLocalCacheEnabled));
        cacheProviderClass.setText(cacheManager.<String>getCacheDetails(OIM_CACHE_ATTRS.Provider));
        multiCastAddress.setText(cacheManager.<String>getCacheDetails(OIM_CACHE_ATTRS.MulticastAddress));
        multiCastConfig.setText(cacheManager.<String>getCacheDetails(OIM_CACHE_ATTRS.MulticastConfig));
        cacheSize.setText(cacheManager.<Integer>getCacheDetails(OIM_CACHE_ATTRS.Size).toString());
    }

    private JComponent buildPanel() {
        return FormBuilder.create().columns("right:pref, 3dlu, pref:grow, 7dlu, right:pref, 3dlu, pref:grow, 3dlu")
                .rows("p, 3dlu, p, 7dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 5dlu, p, 3dlu, p, 3dlu, p, 3dlu, p")
                .add(enabled).xy(3, 1)
                .addLabel("Default Time to Expire?").xy(5, 1).add(expirationTime).xy(7, 1)
                .add(clustered).xy(3, 3)
                .add(threadLocalEnabled).xy(7, 3)
                .addLabel("Cache Provider Class").xy(1, 5).add(cacheProviderClass).xy(3, 5)
                .addLabel("Cache Size").xy(5, 5).add(cacheSize).xy(7, 5)
                .addLabel("Multicast Address").xy(1, 7).add(multiCastAddress).xy(3, 7)
                .addLabel("Multi-cast configuration").xy(5, 7).add(multiCastConfig).xy(7, 7)
                .add(saveDefaultValues).xy(3, 9).add(resetDefaultValues).xy(5, 9)
                .add(cacheCategoryTable).xyw(1, 11, 7)
                .addLabel("Name").xy(1, 13).add(cacheCategoryName).xyw(3, 13, 5)
                .add(cacheCategoryEnabled).xy(3, 15).addLabel("Time to Expire").xy(5, 15).add(cacheCategoryExpires).xy(7, 15)
                .add(saveCategoryValues).xy(3, 17)
                .build();
    }

    @Override
    public JComponent getDisplayComponent() {
        return cacheDetailUI;
    }

    @Override
    public void destroyComponent() {
        logger.debug("Destroying {}", this);
        logger.debug("Destroyed {}", this);
    }

}
