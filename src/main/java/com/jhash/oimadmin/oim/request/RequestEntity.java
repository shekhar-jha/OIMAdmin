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

package com.jhash.oimadmin.oim.request;

import com.jhash.oimadmin.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestEntity {

    private final Object requestEntity;
    private final ClassLoader classLoader;

    public RequestEntity(Object requestEntity, ClassLoader classLoader) {
        this.requestEntity = requestEntity;
        this.classLoader = classLoader;
    }

    public Request getRequest() {
        return new Request(Utils.invoke(requestEntity, "getRequest", classLoader), classLoader);
    }

    public String getRequestEntityType() {
        Object entityType = Utils.invoke(requestEntity, "getRequestEntityType", null);
        if (entityType != null)
            return Utils.invoke(requestEntity, "getValue", "N/A");
        return "Entity Type not available";
    }

    public List<RequestEntityAttribute> getEntityData() {
        List<Object> entityData = Utils.invoke(requestEntity, "getEntityData", new ArrayList<Object>());
        if (entityData == null)
            return null;
        List<RequestEntityAttribute> calculatedRequestEntityAttributes = new ArrayList<>();
        for (Object requestEntityAttribute : entityData) {
            calculatedRequestEntityAttributes.add(new RequestEntityAttribute(requestEntityAttribute, classLoader));
        }
        return calculatedRequestEntityAttributes;
    }

    public String getEntitySubType() {
        return Utils.invoke(requestEntity, "getEntitySubType", "N/A");
    }

    public String getEntityKey() {
        return Utils.invoke(requestEntity, "getEntityKey", "N/A");
    }

    public String getOperation() {
        return Utils.invoke(requestEntity, "getOperation", "N/A");
    }

    public List<RequestEntityAttribute> getAdditionalEntityData() {
        List<Object> entityData = Utils.invoke(requestEntity, "getAdditionalEntityData", new ArrayList<Object>());
        if (entityData == null)
            return null;
        List<RequestEntityAttribute> calculatedRequestEntityAttributes = new ArrayList<>();
        for (Object requestEntityAttribute : entityData) {
            calculatedRequestEntityAttributes.add(new RequestEntityAttribute(requestEntityAttribute, classLoader));
        }
        return calculatedRequestEntityAttributes;
    }

    public static class RequestEntityAttribute {
        private final Object requestEntityAttribute;
        private final ClassLoader classLoader;

        public RequestEntityAttribute(Object requestEntityAttribute, ClassLoader classLoader) {
            this.requestEntityAttribute = requestEntityAttribute;
            this.classLoader = classLoader;
        }

        public boolean hasChild() {
            return Utils.invoke(requestEntityAttribute, "hasChild", false);
        }

        public List<RequestEntityAttribute> getChildAttributes() {
            List<Object> childAttributes = Utils.invoke(requestEntityAttribute, "getChildAttributes", new ArrayList<Object>());
            if (childAttributes == null)
                return null;
            List<RequestEntityAttribute> calculatedRequestEntityAttributes = new ArrayList<>();
            for (Object entityAttribute : childAttributes) {
                calculatedRequestEntityAttributes.add(new RequestEntityAttribute(entityAttribute, classLoader));
            }
            return calculatedRequestEntityAttributes;
        }

        public String getName() {
            return Utils.invoke(requestEntityAttribute, "getName", "N/A");
        }

        public Serializable getValue() {
            return Utils.invoke(requestEntityAttribute, "getValue", "N/A");
        }

        public String getType() {
            Object type = Utils.invoke(requestEntityAttribute, "getType");
            return type == null ? "null" : type.toString();
        }

        public String getTypeHolder() {
            return Utils.invoke(requestEntityAttribute, "getTypeHolder", "N/A");
        }

        public Byte[] getValueHolderByteArray() {
            return Utils.invoke(requestEntityAttribute, "getValueHolderByteArray", new Byte[0]);
        }

        public char[] getValueHolderAsCharArray() {
            return Utils.invoke(requestEntityAttribute, "getValueHolderAsCharArray", new char[0]);
        }

        public RequestEntityAttribute getParentAttribute() {
            Object requestEntityAttributeObject = Utils.invoke(requestEntityAttribute, "getParentAttribute");
            if (requestEntityAttributeObject == null)
                return null;
            return new RequestEntityAttribute(requestEntityAttributeObject, classLoader);
        }

        public String getRowKey() {
            return Utils.invoke(requestEntityAttribute, "getRowKey", "N/A");
        }

        public String getAction() {
            Object action = Utils.invoke(requestEntityAttribute, "getAction");
            return action == null ? "null" : action.toString();
        }

        public String getActionHolder() {
            return Utils.invoke(requestEntityAttribute, "getActionHolder", "N/A");
        }

        public boolean isValueValid() {
            return Utils.invoke(requestEntityAttribute, "isValueValid", false);
        }

        public String getValueHolder() {
            return Utils.invoke(requestEntityAttribute, "getValueHolder", "N/A");
        }

        public Map getMlsMap() {
            return Utils.invoke(requestEntityAttribute, "getMlsMap", new HashMap());
        }

        public String getDefaultMLSValue() {
            return Utils.invoke(requestEntityAttribute, "getDefaultMLSValue", "N/A");
        }
    }
}
