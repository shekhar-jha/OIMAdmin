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

import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.events.Event;
import com.jhash.oimadmin.events.EventConsumer;
import com.jhash.oimadmin.events.EventSource;
import com.jhash.oimadmin.ui.component.EventEnabledServiceComponentImpl;
import com.jhash.oimadmin.ui.component.ParentComponent;
import com.jhash.oimadmin.ui.component.PublishableComponent;
import com.jhash.oimadmin.ui.utils.UIUtils;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideLabel;
import org.jdesktop.swingx.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class AbstractUIComponent<T extends JComponent, W extends AbstractUIComponent<T, W>> extends EventEnabledServiceComponentImpl<W> implements UIComponent<JComponent>, PublishableComponent<W> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractUIComponent.class);
    protected JComponent displayComponent;
    protected JPanel messageDisplayComponent;
    protected JPanel messagePanel;
    protected boolean publish = false;
    protected boolean destroyComponentOnClose = false;

    public AbstractUIComponent(String name, ParentComponent parent) {
        super(name, parent);
    }


    @Override
    public boolean handleEvent(EventSource parent, Event event) {
        //TODO:
        return false;
    }

    public <I, O> O executeCallback(CallbackEvent<I, O> event, I input) {
        CallbackSource<I, O> callbackSource = new CallbackSource<>(input);
        triggerEvent(callbackSource, event);
        return callbackSource.output;
    }

    public W setPublish(boolean publish) {
        if (getState() == NOT_INITIALIZED) {
            this.publish = publish;
            return (W) this;
        } else {
            throw new IllegalStateException("The component " + this + " status is " + getState()
                    + ". setPublish can be called only if component is " + NOT_INITIALIZED);
        }
    }

    public W publish() {
        if (publish) {
            getDisplayArea().add(this);
        } else {
            logger.debug("Not publishing the component.");
        }
        return (W) this;
    }

    public boolean isDestroyComponentOnClose() {
        return destroyComponentOnClose;
    }

    public W setDestroyComponentOnClose(boolean destroyComponentOnClose) {
        this.destroyComponentOnClose = destroyComponentOnClose;
        return (W) this;
    }

    @Override
    public final void initializeComponent() {
        logger.debug("Trying to initialize UI Component");
        super.initializeComponent();
        setupDisplayComponent();
        publish();
        logger.debug("Initialized UI Component");
    }

    public abstract void setupDisplayComponent();


    @Override
    public final synchronized JComponent getComponent() {
        if (displayComponent == null) {
            if (getParent() instanceof AbstractUIComponent) {
                displayComponent = getDisplayComponent();
            } else {
                JPanel packagedComponent = new JPanel(new BorderLayout());
                packagedComponent.add(getDisplayComponent(), BorderLayout.CENTER);
                messagePanel = new JPanel(new BorderLayout());
                JideButton closeButton = new JideButton("X");
                closeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        messageDisplayComponent.removeAll();
                        displayComponent.remove(messagePanel);
                        displayComponent.revalidate();
                    }
                });
                messageDisplayComponent = new JPanel(new VerticalLayout());
                messagePanel.add(messageDisplayComponent, BorderLayout.CENTER);
                messagePanel.add(closeButton, BorderLayout.EAST);
                displayComponent = packagedComponent;
            }
        }
        return displayComponent;
    }

    public abstract T getDisplayComponent();

    public void displayMessage(String title, String message) {
        displayMessage(title, message, null);
    }

    @Override
    public void displayMessage(final String title, String message, Exception exception) {
        ParentComponent parent = getParent();
        if (parent instanceof AbstractUIComponent) {
            ((AbstractUIComponent) parent).displayMessage(title, message, exception);
        } else {
            if (messageDisplayComponent != null & messagePanel != null) {
                synchronized (messageDisplayComponent) {
                    messageDisplayComponent.add(new JideLabel("<html><b>" + title + "</b></html>"));
                    messageDisplayComponent.add(new JSeparator(SwingConstants.HORIZONTAL));
                    final JideLabel messageLabel = new JideLabel(message);
                    final String exceptionStackTrace;
                    if (exception != null) {
                        messageLabel.setText(messageLabel.getText() + " Cause : " + exception.getCause());
                        exceptionStackTrace = Utils.extractExceptionDetails(exception);
                        String htmlStackexceptionStackTrace = "<html>" + exceptionStackTrace.replace(System.lineSeparator(), "<br/>") + "</html>";
                        messageLabel.setToolTipText(htmlStackexceptionStackTrace);
                    } else {
                        exceptionStackTrace = null;
                    }
                    messageDisplayComponent.add(messageLabel);
                    messageLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            super.mouseClicked(e);
                            String message = messageLabel.getText();
                            JOptionPane.showMessageDialog(messageLabel,
                                    message + (Utils.isEmpty(exceptionStackTrace) ? "" : System.lineSeparator() + exceptionStackTrace), title, JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    displayComponent.add(messagePanel, BorderLayout.NORTH);
                    displayComponent.revalidate();
                }
            } else {
                UIUtils.displayMessage(title, message, null);
            }
        }
    }

    public abstract void destroyDisplayComponent();

    @Override
    public final void destroyComponent() {
        logger.debug("Trying to destroy ui component {}", this);
        getDisplayArea().remove(this);
        try {
            destroyDisplayComponent();
            logger.debug("Completed ui component destruction");
        } catch (Exception exception) {
            logger.warn("Failed to complete the component specific destruction process", exception);
            throw exception;
        }
    }

    interface CallbackEventSource<I, O> extends EventSource {

        I getInput();

        void setOutput(O output);
    }

    public static class CallbackSource<I, O> implements CallbackEventSource<I, O> {

        private I input;
        private O output;

        public CallbackSource(I input) {
            this.input = input;
        }

        @Override
        public I getInput() {
            return input;
        }

        public O getOutput() {
            return output;
        }

        @Override
        public void setOutput(O output) {
            this.output = output;
        }
    }

    public static class CallbackEvent<I, O> extends Event {

        public CallbackEvent(String name) {
            super(name);
        }
    }

    public static abstract class Callback<I, O> implements EventConsumer {

        public void triggerEvent(EventSource source, Event event) {
            if (event instanceof CallbackEvent && source instanceof CallbackEventSource) {
                CallbackEventSource<I, O> callbackEventSource = (CallbackEventSource<I, O>) source;
                callbackEventSource.setOutput(call(callbackEventSource.getInput()));
            }
        }

        public abstract O call(I value);
    }
}
