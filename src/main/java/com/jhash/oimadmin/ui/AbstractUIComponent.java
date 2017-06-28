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

import com.jhash.oimadmin.*;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractUIComponent<T extends JComponent, W extends AbstractUIComponent<T, W>> extends JPanel implements UIComponent<JComponent> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractUIComponent.class);
    protected final String name;
    protected final Config.Configuration configuration;
    protected final UIComponentTree selectionTree;
    protected final DisplayArea displayArea;
    protected final AbstractUIComponent<?, ?> parent;
    private final String internalRepresentation;
    protected JComponent displayComponent;
    protected JPanel messageDisplayComponent;
    protected JPanel messagePanel;
    protected boolean publish;
    protected boolean destroyComponentOnClose = false;
    private COMPONENT_STATE status = COMPONENT_STATE.NOT_INITIALIZED;
    private Deque<AbstractUIComponent<?, ?>> childComponents = new ArrayDeque<>();
    private Map<ID, Callback> callbacks = new HashMap<>();

    public AbstractUIComponent(String name, Config.Configuration configuration, UIComponentTree selectionTree, DisplayArea displayArea) {
        this.name = name;
        this.configuration = configuration;
        this.selectionTree = selectionTree;
        this.displayArea = displayArea;
        parent = null;
        setPublish(true);
        internalRepresentation = "{COMPONENT: " + name + "[" + super.toString() + "]}";
    }

    public AbstractUIComponent(String name, AbstractUIComponent<?, ?> parent) {
        this.name = name;
        if (parent == null)
            throw new OIMAdminException("No parent was provided for the component being created. Can not continue.");
        this.configuration = parent.configuration;
        this.selectionTree = parent.selectionTree;
        this.displayArea = parent.displayArea;
        this.parent = parent;
        this.parent.registerEventListener(this);
        setPublish(false);
        internalRepresentation = parent.toString() + ">> {COMPONENT: " + name + "[" + super.toString() + "]}";
    }

    protected void registerEventListener(AbstractUIComponent childComponent) {
        logger.debug("Registering event listener {}", childComponent);
        if (childComponent == null)
            return;
        childComponents.addFirst(childComponent);
        logger.debug("Registered event listener.");
    }

    protected W registerCallback(ID id, Callback callback) {
        logger.debug("Registering callback {}={}", id, callback);
        if (callback != null && id != null) {
            callbacks.put(id, callback);
        }
        logger.debug("Registered callback");
        return (W) this;
    }

    public <I, O, CB extends Callback<I, O>> O executeCallback(ID<I, O, CB> id, I input) {
        if (callbacks.containsKey(id)) {
            return (O) callbacks.get(id).call(input);
        } else {
            return null;
        }
    }

    protected void unRegisterEventListener(AbstractUIComponent childComponent) {
        logger.debug("Unregister event listener {}", childComponent);
        if (childComponent == null)
            return;
        childComponents.remove(childComponent);
        logger.debug("Unregistered event listener.");
    }

    public void triggerEvent(AbstractUIComponent parent, COMPONENT_STATE parentState) {
        logger.debug("Received event {} from {}", parentState, parent);
        if (parentState == COMPONENT_STATE.INITIALIZATION_IN_PROGRESS && getStatus() == COMPONENT_STATE.NOT_INITIALIZED)
            initialize();
        if (parentState == COMPONENT_STATE.DESTRUCTION_IN_PROGRESS && getStatus() == COMPONENT_STATE.INITIALIZED)
            destroy();
        logger.debug("Processed event {} from {}", parentState, parent);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Config.Configuration getConfiguration() {
        return configuration;
    }

    public COMPONENT_STATE getStatus() {
        return status;
    }

    private void setStatus(COMPONENT_STATE status) {
        this.status = status;
        this.triggerEvent(this, status);
        for (AbstractUIComponent component : childComponents) {
            component.triggerEvent(this, status);
        }
    }

    public W setPublish(boolean publish) {
        if (getStatus() == COMPONENT_STATE.NOT_INITIALIZED) {
            this.publish = publish;
            return (W) this;
        } else {
            throw new IllegalStateException("The component " + this + " status is " + getStatus()
                    + ". setPublish can be called only if component is " + COMPONENT_STATE.NOT_INITIALIZED);
        }
    }

    public boolean isDestroyComponentOnClose() {
        return destroyComponentOnClose;
    }

    public W setDestroyComponentOnClose(boolean destroyComponentOnClose) {
        this.destroyComponentOnClose = destroyComponentOnClose;
        return (W) this;
    }

    @Override
    public W initialize() {
        logger.debug("Trying to initialize UI Component");
        COMPONENT_STATE status = getStatus();
        if (status == COMPONENT_STATE.INITIALIZATION_IN_PROGRESS) {
            logger.warn("Trying to initialize UI Component {} which is already being initialized, ignoring the trigger", this);
            return (W) this;
        }
        if (status == COMPONENT_STATE.INITIALIZED) {
            logger.debug("Nothing to do since component {} is already initialized.", this);
        } else {
            setStatus(COMPONENT_STATE.INITIALIZATION_IN_PROGRESS);
            try {
                initializeComponent();
                setStatus(COMPONENT_STATE.INITIALIZED);
            } catch (Exception exception) {
                logger.warn("Failed to initialize the component " + this, exception);
                logger.debug("Setting node status as ", COMPONENT_STATE.FAILED);
                setStatus(COMPONENT_STATE.FAILED);
            }
        }
        if (publish) {
            displayArea.add(this);
        } else {
            logger.debug("Not publishing the component.");
        }
        logger.debug("Initialized UI Component");
        return (W) this;
    }

    public abstract void initializeComponent();

    public abstract void destroyComponent();

    @Override
    public final synchronized JComponent getComponent() {
        if (displayComponent == null) {
            if (parent != null) {
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

    public void displayMessage(final String title, String message, Exception exception) {
        if (parent != null) {
            parent.displayMessage(title, message, exception);
        } else {
            if (messageDisplayComponent != null & messagePanel != null) {
                synchronized (messageDisplayComponent) {
                    messageDisplayComponent.add(new JideLabel("<html><b>" + title + "</b></html>"));
                    messageDisplayComponent.add(new JSeparator(SwingConstants.HORIZONTAL));
                    final JideLabel messageLabel = new JideLabel(message);
                    final String exceptionStackTrace;
                    if (exception != null) {
                        messageLabel.setText(messageLabel.getText() + " Cause : " + exception.getCause());
                        StringWriter exceptionAsStringWriter = new StringWriter();
                        exception.printStackTrace(new PrintWriter(exceptionAsStringWriter));
                        exceptionStackTrace = exceptionAsStringWriter.toString();
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
                            String toolTipText = messageLabel.getToolTipText();
                            String message = messageLabel.getText();
                            JOptionPane.showMessageDialog(AbstractUIComponent.this,
                                    message + (Utils.isEmpty(exceptionStackTrace) ? "" : System.lineSeparator() + exceptionStackTrace), title, JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    displayComponent.add(messagePanel, BorderLayout.NORTH);
                    displayComponent.revalidate();
                }
            } else {
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void destroy() {
        logger.debug("Trying to destroy {}", this);
        if (getStatus() == COMPONENT_STATE.DESTRUCTION_IN_PROGRESS) {
            logger.warn("Trying to destroy UI Component {} which is already being destroyed, ignoring the trigger", this);
            return;
        }
        if (getStatus() == COMPONENT_STATE.INITIALIZED) {
            logger.debug("Component in {} state, setting status to {} before destroying", getStatus(), COMPONENT_STATE.DESTRUCTION_IN_PROGRESS);
            setStatus(COMPONENT_STATE.DESTRUCTION_IN_PROGRESS);
            try {
                if (publish)
                    displayArea.remove(this);
                destroyComponent();
                logger.debug("Completed component destruction");
            } catch (Exception exception) {
                logger.warn("Failed to complete the component specific destruction process", exception);
            }
            logger.debug("Setting status to {}", COMPONENT_STATE.NOT_INITIALIZED);
            setStatus(COMPONENT_STATE.NOT_INITIALIZED);
        } else {
            logger.debug("Skipping destroy since the component is not in {} state", COMPONENT_STATE.INITIALIZED);
        }
    }

    @Override
    public String toString() {
        return internalRepresentation;
    }

    public interface Callback<I, O> {

        O call(I value);
    }

    public interface ID<I, O, T extends Callback<I, O>> {
    }

    public static class CLASS_ID<I, O, T extends Callback<I, O>> implements ID<I, O, T> {

    }

    public static class COMPONENT_STATE<M> {
        public static final COMPONENT_STATE NOT_INITIALIZED = new COMPONENT_STATE("NOT_INITIALIZED");
        public static final COMPONENT_STATE INITIALIZED = new COMPONENT_STATE("INITIALIZED");
        public static final COMPONENT_STATE INITIALIZED_NO_OP = new COMPONENT_STATE("INITIALIZED_NO_OP");
        public static final COMPONENT_STATE FAILED = new COMPONENT_STATE("FAILED");
        public static final COMPONENT_STATE INITIALIZATION_IN_PROGRESS = new COMPONENT_STATE("INITIALIZATION_IN_PROGRESS");
        public static final COMPONENT_STATE DESTRUCTION_IN_PROGRESS = new COMPONENT_STATE("DESTRUCTION_IN_PROGRESS");

        public final M stateDetails;
        private final String name;

        public COMPONENT_STATE(String name) {
            this(name, null);
        }

        public COMPONENT_STATE(String name, M stateDetails) {
            this.name = "COMPONENT STATE: " + name;
            this.stateDetails = stateDetails;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
