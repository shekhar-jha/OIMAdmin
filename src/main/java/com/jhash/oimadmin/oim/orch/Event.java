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

public class Event {

    private static final Logger logger = LoggerFactory.getLogger(Event.class);
    private final Object event;
    private final ClassLoader classLoader;

    public Event(Object event, ClassLoader classLoader) {
        this.event = event;
        this.classLoader = classLoader;
    }

    public static String getStringRepresentation(Object value, ClassLoader classLoader) {
        if (value != null) {
            Class contextAwareClass = null;
            Class eventResult = null;
            try {
                contextAwareClass = classLoader.loadClass("oracle.iam.platform.context.ContextAware");
                eventResult = classLoader.loadClass("oracle.iam.platform.kernel.vo.EventResult");
            } catch (Exception exception) {
                logger.warn("Failed to load class oracle.iam.platform.context.ContextAware", exception);
            }
            if (value instanceof Exception) {
                return Utils.generateStringRepresentation((Exception) value);
            } else if (contextAwareClass != null && contextAwareClass.isAssignableFrom(value.getClass())) {
                StringBuilder contextValue = new StringBuilder();
                Object type = Utils.invoke(value, "getType", null);
                contextValue.append("Type: " + (type == null ? "N/A" : type));
                contextValue.append(System.lineSeparator());
                Object objectValue = Utils.invoke(value, "getObjectValue", null);
                contextValue.append("Value: " + (objectValue == null ? "N/A" : objectValue));
                return contextValue.toString();
            } else if (eventResult != null && eventResult.isAssignableFrom(value.getClass())) {
                StringBuilder resultValue = new StringBuilder();
                Object throwable = Utils.invoke(value, "getFailureReason", null);
                if (throwable != null) {
                    resultValue.append("Failure Reason: " + Utils.generateStringRepresentation((Throwable) throwable));
                } else {
                    resultValue.append("Failure Reason: ");
                }
                resultValue.append(System.lineSeparator());
                Object isVeto = Utils.invoke(value, "isVeto", null);
                if (isVeto != null) {
                    resultValue.append("Veto? : " + isVeto);
                } else {
                    resultValue.append("Veto?: ");
                }
                resultValue.append(System.lineSeparator());
                Object shouldProcessImmediateInSequence = Utils.invoke(value, "shouldProcessImmediateInSequence", null);
                if (shouldProcessImmediateInSequence != null) {
                    resultValue.append("Process Immediate In Sequence ? : " + shouldProcessImmediateInSequence);
                } else {
                    resultValue.append("Process Immediate In Sequence ?: ");
                }
                resultValue.append(System.lineSeparator());
                Object warnings = Utils.invoke(value, "getWarnings", null);
                if (warnings != null) {
                    resultValue.append("Warnings : " + warnings);
                } else {
                    resultValue.append("Warnings ?: ");
                }
                resultValue.append(System.lineSeparator());
                Object shouldProcessingWait = Utils.invoke(value, "shouldProcessingWait", null);
                if (shouldProcessingWait != null) {
                    resultValue.append("Should processing wait? : " + shouldProcessingWait);
                } else {
                    resultValue.append("Should processing wait ?: ");
                }
                resultValue.append(System.lineSeparator());
                Object getDeferredChanges = Utils.invoke(value, "getDeferredChanges", null);
                if (getDeferredChanges != null) {
                    resultValue.append("Deferred Changes : " + getDeferredChanges);
                } else {
                    resultValue.append("Deferred Changes: ");
                }
                resultValue.append(System.lineSeparator());
                Object getImmediateChanges = Utils.invoke(value, "getImmediateChanges", null);
                if (getImmediateChanges != null) {
                    resultValue.append("Immediate Changes : " + getImmediateChanges);
                } else {
                    resultValue.append("Immediate Changes : ");
                }
                resultValue.append(System.lineSeparator());
                return resultValue.toString();
            } else {
                return value.toString();
            }
        }
        return "No value";
    }

    public ID getEventId() {
        Object id = Utils.invoke(event, "getEventId", null);
        return new ID(id, classLoader);
    }

    public String getOperation() {
        return Utils.invoke(event, "getOperation", "N/A");
    }

    public int getOrder() {
        return Utils.invoke(event, "getOrder", -1);
    }

    public Stage getStage() {
        Object stage = Utils.invoke(event, "getStage", null);
        if (stage != null)
            return new Stage(stage, classLoader);
        return null;
    }

    public boolean isSync() {
        return Utils.invoke(event, "isSync", false);
    }

    public String getHandlerClass() {
        return Utils.invoke(event, "getHandlerClass", "N/A");
    }

    public Status getStatus() {
        return new Status(Utils.invoke(event, "getStatus", "N/A"), classLoader);
    }

    public String getResult() {
        return getStringRepresentation(Utils.invoke(event, "getResult", "N/A"), classLoader);
    }

    public static class Stage {

        private final Object stage;
        private final ClassLoader classLoader;
        private final String name;

        public Stage(Object stage, ClassLoader classLoader) {
            this.stage = stage;
            this.classLoader = classLoader;
            this.name = getName();
        }

        public String getName() {
            return stage == null ? "null" : Utils.invoke(stage, "getName", "N/A");
        }

        public boolean isFailedStage() {
            return stage == null ? false : Utils.invoke(stage, "isFailedStage", false);
        }

        public boolean isOutOfBandStage() {
            return stage == null ? false : Utils.invoke(stage, "isOutOfBandStage", false);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Status {
        private final Object status;
        private final ClassLoader classLoader;

        public Status(Object status, ClassLoader classLoader) {
            this.status = status;
            this.classLoader = classLoader;
        }

        public String toString() {
            if (status != null)
                return status.toString();
            return "null";
        }
    }

    public static class ID {
        private final Object id;
        private final ClassLoader classLoader;

        public ID(Object id, ClassLoader classLoader) {
            this.id = id;
            this.classLoader = classLoader;
        }

        public Long getId() {
            return id == null ? -1L : Utils.invoke(id, "getId", -1L);
        }

        public long getPrimitiveId() {
            return id == null ? -1L : Utils.invoke(id, "getPrimitiveId", -1L);
        }

        public String getName() {
            return id == null ? "Null" : Utils.invoke(id, "getName", "N/A");

        }

        public String toString() {
            return id == null ? "Null" : Utils.invoke(id, "toString", "N/A");
        }

    }
}
