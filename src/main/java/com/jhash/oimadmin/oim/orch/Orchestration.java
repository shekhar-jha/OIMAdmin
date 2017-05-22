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
import java.util.HashMap;

public class Orchestration {

    private static final Logger logger = LoggerFactory.getLogger(Orchestration.class);
    private final Object orchestration;
    private final ClassLoader classLoader;

    public Orchestration(Object orchestration, ClassLoader classLoader) {
        this.orchestration = orchestration;
        this.classLoader = classLoader;
    }

    public String getOperation() {
        return Utils.invoke(orchestration, "getOperation", "N/A");
    }

    public boolean isSync() {
        return Utils.invoke(orchestration, "isSync", false);
    }

    public Orchestration.Target getTarget() {
        Object target = Utils.invoke(orchestration, "getTarget", null);
        if (target != null) {
            try {
                Class orchestrationClass = classLoader.loadClass("oracle.iam.platform.kernel.vo.OrchestrationTarget");
                if (orchestrationClass.isAssignableFrom(target.getClass())) {
                    return new Orchestration.Target(target, classLoader);
                }
            } catch (Exception exception) {
                logger.debug("Failed to load class oracle.iam.platform.kernel.vo.Orchestration", exception);
            }
        }
        return null;
    }

    public <T extends Serializable> HashMap<String, T> getParameters() {
        return Utils.invoke(orchestration, "getParameters", new HashMap<String, T>());
    }

    public String getContextVal() {
        return Utils.invoke(orchestration, "getContextVal", "N/A");
    }

    public HashMap<String, Serializable> getInterEventData() {
        return Utils.invoke(orchestration, "getInterEventData", new HashMap<String, Serializable>());
    }

    public String[] getTargetUserIds() {
        return Utils.invoke(orchestration, "getTargetUserIds", new String[0]);
    }

    public boolean isPostProcessingAsync() {
        return Utils.invoke(orchestration, "isPostProcessingAsync", false);
    }

    public boolean isActionAuditInTransaction() {
        return Utils.invoke(orchestration, "isActionAuditInTransaction", false);
    }

    public Object isNonSequential() {
        return Utils.invoke(orchestration, "isNonSequential", "N/A in this version");
    }

    public static class Target {
        private final Object target;
        private final ClassLoader classLoader;

        public Target(Object target, ClassLoader classLoader) {
            this.target = target;
            this.classLoader = classLoader;
        }

        public String getType() {
            return Utils.invoke(target, "getType", "N/A");
        }

        public Object getExisting() {
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                return Utils.invoke(target, "getExisting", "N/A");
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        }

        public String getEntityId() {
            return Utils.invoke(target, "getEntityId", "N/A");
        }


        public Class<?> getDefaultActionHandler() {
            return Utils.invoke(target, "getDefaultActionHandler", null);
        }

        public Class<?> getDefaultValidator() {
            return Utils.invoke(target, "getDefaultValidator", null);
        }

        public String[] getAllEntityId() {
            return Utils.invoke(target, "getAllEntityId", new String[0]);
        }

        public Object[] getAllExisting() {
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                return Utils.invoke(target, "getAllExisting", new Object[0]);
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        }

    }

}
