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

import com.jgoodies.jsdl.component.JGComponentFactory;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.ui.AbstractUIComponent;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

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

    public static void displayMessage(String title, String message, Exception exception) {
        StringBuilder messageDetails = new StringBuilder(message);
        if (Utils.isEmpty(message) && exception != null) {
            messageDetails.append(exception.getMessage());
        }
        messageDetails.append(System.lineSeparator());
        if (exception != null)
            messageDetails.append(Utils.extractExceptionDetails(exception));
        JOptionPane.showMessageDialog(null, messageDetails.toString(), title, JOptionPane.ERROR_MESSAGE);
    }

    public static File selectFile(Boolean directoryOnly, String buttonText) {
        JFileChooser jarFileChooser = new JFileChooser();
        if (directoryOnly == Boolean.TRUE)
            jarFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else if (directoryOnly == Boolean.FALSE)
            jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        else
            jarFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnedResult = jarFileChooser.showDialog(JFrame.getFrames()[0], Utils.isEmpty(buttonText) ? "Select..." : buttonText);
        if (returnedResult == JFileChooser.APPROVE_OPTION) {
            return jarFileChooser.getSelectedFile();
        } else {
            return null;
        }
    }

    public static class TextFieldCallback extends AbstractUIComponent.Callback<String, String> {

        private final JTextComponent value;

        public TextFieldCallback(JTextComponent item) {
            value = item;
        }

        public String call() {
            return call("");
        }

        @Override
        public String call(String defaultValue) {
            if (value == null)
                return defaultValue;
            return value.getText();
        }
    }

    public static class ComboBoxCallback<T> extends AbstractUIComponent.Callback<T, T> {

        private final JComboBox<T> value;
        private final Class<T> returnType;

        public ComboBoxCallback(JComboBox<T> value, Class<T> returnTypeClass) {
            this.value = value;
            this.returnType = returnTypeClass;
        }

        public T call() {
            return call(null);
        }

        @Override
        public T call(T defaultValue) {
            Object selectedItem = value.getSelectedItem();
            if (selectedItem == null)
                return defaultValue;
            if (returnType != null && returnType.isAssignableFrom(selectedItem.getClass()))
                return returnType.cast(selectedItem);
            else
                return defaultValue;
        }
    }

    public static class CheckBoxCallback<T> extends AbstractUIComponent.Callback<T, T> {
        private final JCheckBox value;
        private final List<T> mappedValue;

        public CheckBoxCallback(JCheckBox value, T mappedValueTrue, T mappedValueFalse) {
            this.value = value;
            this.mappedValue = Arrays.asList(mappedValueTrue, mappedValueFalse);
        }

        @Override
        public T call(T defaultValue) {
            if (value == null)
                return defaultValue;
            boolean isSelected = value.isSelected();
            if (isSelected)
                return mappedValue.get(0);
            else
                return mappedValue.get(1);
        }

    }

}
