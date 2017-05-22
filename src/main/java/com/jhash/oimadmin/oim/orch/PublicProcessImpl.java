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

package com.jhash.oimadmin.oim.orch;

import com.jhash.oimadmin.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PublicProcessImpl {
    private static final Logger logger = LoggerFactory.getLogger(PublicProcessImpl.class);
    private final Object process;
    private final ClassLoader classLoader;

    public PublicProcessImpl(Object process, ClassLoader classLoader) {
        if (process == null)
            throw new NullPointerException("No process object passed.");
        this.process = process;
        this.classLoader = classLoader;
    }

    private List<Event> extractEvent(String methodName) {
        List<Object> events = Utils.invoke(process, methodName, new ArrayList<Object>());
        if (events != null) {
            List<Event> eventList = new ArrayList<>();
            for (Object event : events) {
                eventList.add(new Event(event, classLoader));
            }
            return eventList;
        }
        return null;
    }

    public Object getTarget() {
        Object target = Utils.invoke(process, "getTarget", "N/A");
        if (target != null) {
            try {
                Class orchestrationClass = classLoader.loadClass("oracle.iam.platform.kernel.vo.Orchestration");
                if (orchestrationClass.isAssignableFrom(target.getClass())) {
                    return new Orchestration(target, classLoader);
                }
            } catch (Exception exception) {
                logger.debug("Failed to load class oracle.iam.platform.kernel.vo.Orchestration", exception);
            }
        }
        return target;
    }

    public List<Event> getEvents() {
        return extractEvent("getEvents");
    }

    public List<Event> getOutOfBandEvents() {
        return extractEvent("getOutOfBandEvents");
    }

    public Serializable getResult() {
        return Event.getStringRepresentation(Utils.invoke(process, "getResult", "N/A"), classLoader);
    }

    public Event.Stage getStopStage() {
        return new Event.Stage(Utils.invoke(process, "getStopStage", null), classLoader);
    }

    public Event getCurrentHandler() {
        return new Event(Utils.invoke(process, "getCurrentHandler", null), classLoader);
    }


    public PublicProcessImpl getParent() {
        Object parentProcess = Utils.invoke(process, "getParent", null);
        if (parentProcess == null)
            return null;
        return new PublicProcessImpl(parentProcess, classLoader);
    }

    public boolean isRunning() {
        return Utils.invoke(process, "isRunning", false);
    }

    public boolean isStoppable() {
        return Utils.invoke(process, "isStoppable", false);
    }

    /*protected HandlerProvider getHandlerProvider() {
        return process.getHandlerProvider();
    }*/

    public boolean isObjectSaved() {
        return Utils.invoke(process, "isObjectSaved", false);
    }

    /*public String getLogStatement(String methodName) {
        return process.getLogStatement(methodName);
    }*/

    public boolean hasDeferredChanges() {
        return Utils.invoke(process, "hasDeferredChanges", false);
    }

    public boolean hasChildrenFromBulk() {
        return Utils.invoke(process, "hasChildrenFromBulk", false);
    }

    public Event.ID getProcessId() {
        return new Event.ID(Utils.invoke(process, "getProcessId", null), classLoader);
    }

    public Status getStatus() {
        return new Status(Utils.invoke(process, "getStatus", null), classLoader);
    }

    public String getOperation() {
        return Utils.invoke(process, "getOperation", "N/A");
    }

    public Event.Stage getStage() {
        return new Event.Stage(Utils.invoke(process, "getStage", null), classLoader);
    }

    public Event.Stage getStartStage() {
        return new Event.Stage(Utils.invoke(process, "getStartStage", null), classLoader);
    }

    public String getTargetType() {
        return Utils.invoke(process, "getTargetType", "N/A");
    }

    public String getChangeType() {
        Object changeType = Utils.invoke(process, "getChangeType", null);
        return changeType == null ? "null" : changeType.toString();
    }

    public int getRetryCount() {
        return Utils.invoke(process, "getRetryCount", -1);
    }

    public Event.ID getParentId() {
        return new Event.ID(Utils.invoke(process, "getParentId", null), classLoader);
    }

    public Long getModifiedOn() {
        return Utils.invoke(process, "getModifiedOn", -1L);
    }

    public Event.ID getBulkParentId() {
        return new Event.ID(Utils.invoke(process, "getBulkParentId", null), classLoader);
    }

    public Long getCreatedOn() {
        return Utils.invoke(process, "getCreatedOn", -1L);
    }


    public static class Status {
        private final Object status;
        private final ClassLoader classLoader;

        public Status(Object status, ClassLoader classLoader) {
            this.status = status;
            this.classLoader = classLoader;
        }

        public boolean isCompleted() {
            if (status == null)
                return false;
            return Utils.invoke(status, "isCompleted", false);
        }

        public boolean isCompletedWithFutherProcessingAllowed() {
            if (status == null)
                return false;
            return Utils.invoke(status, "isCompletedWithFutherProcessingAllowed", false);
        }

        public String toString() {
            if (status != null)
                return status.toString();
            return "null";
        }
    }

}
