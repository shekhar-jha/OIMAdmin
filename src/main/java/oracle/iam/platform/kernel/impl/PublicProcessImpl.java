package oracle.iam.platform.kernel.impl;

import oracle.iam.platform.kernel.*;
import oracle.iam.platform.kernel.Process;

import java.io.Serializable;
import java.util.List;

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
public class PublicProcessImpl {
    private ProcessImpl process;

    public PublicProcessImpl(Object process) {
        if (process == null)
            throw new NullPointerException("No process object passed.");
        this.process = (ProcessImpl) process;
    }

    public Object getTarget() {
        return process.getTarget();
    }

    public List<? extends Event> getEvents() {
        return process.getEvents();
    }

    public List<? extends Event> getOutOfBandEvents() {
        return process.getOutOfBandEvents();
    }

    public Serializable getResult() {
        return process.getResult();
    }

    public Stage getStopStage() {
        return process.getStopStage();
    }

    public Event getCurrentHandler() {
        return process.getCurrentHandler();
    }


    public PublicProcessImpl getParent() {
        ProcessImpl parentProcess = process.getParent();
        if (parentProcess == null)
            return null;
        return new PublicProcessImpl(parentProcess);
    }

    public boolean isRunning() {
        return process.isRunning();
    }

    public boolean isStoppable() {
        return process.isStoppable();
    }

    protected HandlerProvider getHandlerProvider() {
        return process.getHandlerProvider();
    }

    public boolean isObjectSaved() {
        return process.isObjectSaved();
    }

    public String getLogStatement(String methodName) {
        return process.getLogStatement(methodName);
    }

    public boolean hasDeferredChanges() {
        return process.hasDeferredChanges();
    }

    public boolean hasChildrenFromBulk() {
        return process.hasChildrenFromBulk();
    }

    public Id getProcessId() {
        return process.getProcessId();
    }

    public Process.Status getStatus() {
        return process.getStatus();
    }

    public String getOperation() {
        return process.getOperation();
    }

    public Stage getStage() {
        return process.getStage();
    }

    public Stage getStartStage() {
        return process.getStartStage();
    }

    public String getTargetType() {
        return process.getTargetType();
    }

    public ChangeType getChangeType() {
        return process.getChangeType();
    }

    public int getRetryCount() {
        return process.getRetryCount();
    }

    public Id getParentId() {
        return process.getParentId();
    }

    public Long getModifiedOn() {
        return process.getModifiedOn();
    }

    public Id getBulkParentId() {
        return process.getBulkParentId();
    }

    public Long getCreatedOn() {
        return process.getCreatedOn();
    }
}
