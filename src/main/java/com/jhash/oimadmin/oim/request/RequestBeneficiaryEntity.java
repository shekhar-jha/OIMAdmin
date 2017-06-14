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

public class RequestBeneficiaryEntity {

    private final Object requestBeneficiaryEntity;
    private final ClassLoader classLoader;

    public RequestBeneficiaryEntity(Object requestBeneficiaryEntity, ClassLoader classLoader) {
        this.requestBeneficiaryEntity = requestBeneficiaryEntity;
        this.classLoader = classLoader;
    }

    public Long getKey() {
        return Utils.invoke(requestBeneficiaryEntity, "getKey", -1L);
    }

    public Beneficiary getBeneficiary() {
        Object beneficiary = Utils.invoke(requestBeneficiaryEntity, "getBeneficiary", null);
        if (beneficiary == null)
            return null;
        return new Beneficiary(beneficiary, classLoader);
    }

    public String getRequestEntityType() {
        Object entityType = Utils.invoke(requestBeneficiaryEntity, "getRequestEntityType", null);
        if (entityType != null)
            return Utils.invoke(entityType, "getValue", "N/A");
        return "Type not available";
    }

    public List<RequestBeneficiaryEntityAttribute> getEntityData() {
        List<Object> entityData = Utils.invoke(requestBeneficiaryEntity, "getEntityData", new ArrayList<Object>());
        if (entityData == null)
            return null;
        List<RequestBeneficiaryEntityAttribute> calculatedEntityData = new ArrayList<>();
        for (Object entityDataValue : entityData) {
            calculatedEntityData.add(new RequestBeneficiaryEntityAttribute(entityDataValue, classLoader));
        }
        return calculatedEntityData;
    }

    public String getEntitySubType() {
        return Utils.invoke(requestBeneficiaryEntity, "getEntitySubType", "N/A");
    }

    public String getEntityKey() {
        return Utils.invoke(requestBeneficiaryEntity, "getEntityKey", "N/A");
    }

    public String getOperation() {
        return Utils.invoke(requestBeneficiaryEntity, "getOperation", "N/A");
    }

    public List<RequestBeneficiaryEntityAttribute> getAdditionalEntityData() {
        List<Object> entityData = Utils.invoke(requestBeneficiaryEntity, "getAdditionalEntityData", new ArrayList<Object>());
        if (entityData == null)
            return null;
        List<RequestBeneficiaryEntityAttribute> calculatedEntityData = new ArrayList<>();
        for (Object entityAttribute : entityData) {
            calculatedEntityData.add(new RequestBeneficiaryEntityAttribute(entityAttribute, classLoader));
        }
        return calculatedEntityData;
    }

    public RequestBeneficiaryEntity getDependsOnEntity() {
        Object entity = Utils.invoke(requestBeneficiaryEntity, "getDependsOnEntity", null);
        if (entity == null)
            return null;
        return new RequestBeneficiaryEntity(entity, classLoader);
    }

    public static class RequestBeneficiaryEntityAttribute {

        private final Object requestBeneficiaryEntityAttribute;
        private final ClassLoader classLoader;

        public RequestBeneficiaryEntityAttribute(Object requestBeneficiaryEntityAttribute, ClassLoader classLoader) {
            this.requestBeneficiaryEntityAttribute = requestBeneficiaryEntityAttribute;
            this.classLoader = classLoader;
        }

        public boolean isAdditional() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "isAdditional", false);
        }

        public Map getMlsMap() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getMlsMap", new HashMap());
        }

        public String getDefaultMLSValue() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getDefaultMLSValue", "N/A");
        }

        public boolean isMLS() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "isMLS", false);
        }

        public boolean isMasked() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "isMasked", false);
        }

        public boolean isValueValid() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "isValueValid", false);
        }

        public String getActionHolder() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getActionHolder", "N/A");
        }

        public String getAction() {
            Object action = Utils.invoke(requestBeneficiaryEntityAttribute, "getAction", null);
            return action == null ? "" : action.toString();
        }

        public String getRowKey() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getRowKey", "N/A");
        }

        public RequestBeneficiaryEntityAttribute getParentAttribute() {
            Object parentRequestBeneficiaryEntityAttribute = Utils.invoke(requestBeneficiaryEntityAttribute, "getParentAttribute");
            if (parentRequestBeneficiaryEntityAttribute == null)
                return null;
            return new RequestBeneficiaryEntityAttribute(parentRequestBeneficiaryEntityAttribute, classLoader);
        }

        public RequestBeneficiaryEntity getEntity() {
            Object entity = Utils.invoke(requestBeneficiaryEntityAttribute, "getEntity", null);
            if (entity == null)
                return null;
            return new RequestBeneficiaryEntity(entity, classLoader);
        }

        public String getValueHolder() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getValueHolder", "N/A");
        }

        public String getTypeHolder() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getTypeHolder", "N/A");
        }

        public String getType() {
            Object type = Utils.invoke(requestBeneficiaryEntityAttribute, "getType");
            return type == null ? "null" : type.toString();
        }

        public Serializable getValue() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getValue", null);
        }

        public String getName() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "getName", "N/A");
        }

        public List<RequestBeneficiaryEntityAttribute> getChildAttributes() {
            List<Object> requestBeneficiaryEntityAttributes = Utils.invoke(requestBeneficiaryEntityAttribute, "getChildAttributes", new ArrayList<Object>());
            if (requestBeneficiaryEntityAttributes == null)
                return null;
            List<RequestBeneficiaryEntityAttribute> calculatedRequestBeneficiaryEntityAttributes = new ArrayList<>();
            for (Object requestBeneficiaryEntityAttribute : requestBeneficiaryEntityAttributes) {
                calculatedRequestBeneficiaryEntityAttributes.add(new RequestBeneficiaryEntityAttribute(requestBeneficiaryEntityAttribute, classLoader));
            }
            return calculatedRequestBeneficiaryEntityAttributes;
        }

        public boolean hasChild() {
            return Utils.invoke(requestBeneficiaryEntityAttribute, "hasChild", false);
        }

        public String toString() {
            return "{ [" + getAction() + "]" + getName() + " : " + getValue() + " (" + getType() + ") [" + (hasChild() ? getChildAttributes() : "") + "]" + "}";
        }
    }
}
