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

package com.jhash.oimadmin.ui.utils;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public class ConnectTextFieldListener implements DocumentListener {

    private final Operation operation;

    public ConnectTextFieldListener(Operation operation) {
        this.operation = operation;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        if (operation != null) {
            operation.execute(e);
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        if (operation != null) {
            operation.execute(e);
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        if (operation != null) {
            operation.execute(e);
        }
    }

    public interface Operation {
        void execute(DocumentEvent event);
    }

    public static class SyncTextField implements Operation {

        public final JTextComponent source;
        public final JTextComponent destination;

        public SyncTextField(JTextComponent source, JTextComponent destination) {
            this.source = source;
            this.destination = destination;
        }

        public void execute(DocumentEvent event) {
            if (destination != null && source != null)
                destination.setText(source.getText());
        }

    }
}
