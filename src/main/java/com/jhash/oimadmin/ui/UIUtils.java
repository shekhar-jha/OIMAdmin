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

import com.jgoodies.jsdl.component.JGComponentFactory;

import javax.swing.*;

public class UIUtils {

    public static JTextField createTextField() {
        JTextField textField = JGComponentFactory.getCurrent().createTextField();
        textField.setColumns(25);
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setBorder(null);
        return textField;
    }

    public static JFormattedTextField createDateField() {
        JFormattedTextField textField = JGComponentFactory.getCurrent().createDateField();
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setBorder(null);
        return textField;
    }

    public static JFormattedTextField createLongField() {
        JFormattedTextField textField = JGComponentFactory.getCurrent().createLongField();
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setBorder(null);
        return textField;
    }

    public static JTextArea createTextArea() {
        JTextArea textArea = JGComponentFactory.getCurrent().createReadOnlyTextArea();
        textArea.setRows(4);
        textArea.setColumns(25);
        textArea.setEditable(false);
        textArea.setBackground(null);
        textArea.setBorder(null);
        return textArea;
    }

    public static JCheckBox createBooleanCheckbox(String value) {
        JCheckBox checkBox = JGComponentFactory.getCurrent().createCheckBox(value);
        checkBox.setEnabled(false);
        return checkBox;
    }

}
