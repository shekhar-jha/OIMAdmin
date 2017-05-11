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

package com.jhash.oimadmin.oim.model.request;

import com.jhash.oimadmin.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Request {

    private final Object request;
    private final ClassLoader classLoader;

    public Request(Object request, ClassLoader classLoader) {
        this.request = request;
        this.classLoader = classLoader;
    }

    public String getRequestModelName() {
        return Utils.invoke(request, "getRequestModelName", "N/A");
    }

    public String getJustification() {
        return Utils.invoke(request, "getJustification", "N/A");
    }

    public String getRequestStatus() {
        return Utils.invoke(request, "getRequestStatus", "N/A");
    }

    public long getRequestStage() {
        return Utils.invoke(request, "getRequestStage", -1L);
    }

    public String getRequestID() {
        return Utils.invoke(request, "getRequestID", "N/A");
    }

    public Request getParentRequest() {
        return new Request(Utils.invoke(request, "getParentRequest", null), classLoader);
    }

    public List<Request> getChildRequests() {
        List<Object> result = Utils.invoke(request, "getChildRequests", new ArrayList<Object>());
        if (result == null)
            return null;
        List<Request> calculatedRequest = new ArrayList<>();
        for (Object request : result) {
            calculatedRequest.add(new Request(request, classLoader));
        }
        return calculatedRequest;
    }

    public Date getExecutionDate() {
        return Utils.invoke(request, "getExecutionDate", null);
    }

    public List<RequestEntity> getTargetEntities() {
        List<Object> targetEntities = Utils.invoke(request, "getTargetEntities", new ArrayList<Object>());
        if (targetEntities == null)
            return null;
        List<RequestEntity> calculatedTargetEntities = new ArrayList<>();
        for (Object targetEntity : targetEntities) {
            calculatedTargetEntities.add(new RequestEntity(targetEntity, classLoader));
        }
        return calculatedTargetEntities;
    }

    public List<Beneficiary> getBeneficiaries() {
        List<Object> beneficiaries = Utils.invoke(request, "getBeneficiaries", new ArrayList<Object>());
        if (beneficiaries == null)
            return null;
        List<Beneficiary> calculatedBeneficiaries = new ArrayList<>();
        for (Object beneficiary : beneficiaries) {
            calculatedBeneficiaries.add(new Beneficiary(beneficiary, classLoader));
        }
        return calculatedBeneficiaries;
    }

    public Long getRequestKey() {
        return Utils.invoke(request, "getRequestKey", -1L);
    }

    public String getRequesterKey() {
        return Utils.invoke(request, "getRequesterKey", "N/A");
    }

    public Date getCreationDate() {
        return Utils.invoke(request, "getCreationDate", null);
    }

    public String isParent() {
        return Utils.invoke(request, "isParent", "N/A");
    }

    public long getOrchID() {
        return Utils.invoke(request, "getOrchID", -1L);
    }

    public long getEventID() {
        return Utils.invoke(request, "getEventID", -1L);
    }

    public String getBeneficiaryType() {
        return Utils.invoke(request, "getBeneficiaryType", "N/A");
    }

    public List<ApprovalData> getApprovalData() {
        List<Object> approvalDatas = Utils.invoke(request, "getApprovalData", new ArrayList<Object>());
        if (approvalDatas == null)
            return null;
        List<ApprovalData> calculatedApprovalData = new ArrayList<>();
        for (Object approvalData : approvalDatas) {
            calculatedApprovalData.add(new ApprovalData(approvalData, classLoader));
        }
        return calculatedApprovalData;
    }

    public Date getEndDate() {
        return Utils.invoke(request, "getEndDate", null);
    }

    public List<TemplateAttribute> getTemplateAttributes() {
        List<Object> requestTemplateAttributes = Utils.invoke(request, "getTemplateAttributes", new ArrayList<Object>());
        if (requestTemplateAttributes == null)
            return null;
        List<TemplateAttribute> calculatedTemplateAttributes = new ArrayList<>();
        for (Object requestTemplateAttribute : requestTemplateAttributes) {
            calculatedTemplateAttributes.add(new TemplateAttribute(requestTemplateAttribute, classLoader));
        }
        return calculatedTemplateAttributes;
    }

    public String getReasonForFailure() {
        return Utils.invoke(request, "getReasonForFailure", "N/A");
    }

    public List<TemplateAttribute> getAdditionalAttributes() {
        List<Object> requestTemplateAttributes = Utils.invoke(request, "getAdditionalAttributes", new ArrayList<Object>());
        if (requestTemplateAttributes == null)
            return null;
        List<TemplateAttribute> calculatedTemplateAttributes = new ArrayList<>();
        for (Object requestTemplateAttribute : requestTemplateAttributes) {
            calculatedTemplateAttributes.add(new TemplateAttribute(requestTemplateAttribute, classLoader));
        }
        return calculatedTemplateAttributes;
    }

    public RequestContext getRequestContext() {
        Object context = Utils.invoke(request, "getRequestContext", null);
        try {
            if (context == null || (!(classLoader.loadClass("oracle.iam.request.vo.RequestContext").isAssignableFrom(context.getClass())))) {
                return null;
            }
            return new RequestContext(context, classLoader);
        } catch (Exception exception) {
            return null;
        }
    }

    public String getDependsOnRequestId() {
        return Utils.invoke(request, "getDependsOnRequestId", "N/A");
    }

    public boolean isDirectOperation() {
        return Utils.invoke(request, "isDirectOperation", false);
    }

    public static class RequestContext {

        private final Object requestContext;
        private final ClassLoader classLoader;

        public RequestContext(Object requestContext, ClassLoader classLoader) {
            this.requestContext = requestContext;
            this.classLoader = classLoader;
        }

        public String getRequestId() {
            return Utils.invoke(requestContext, "getRequestId", "N/A");
        }

        public String getLoginUserId() {
            return Utils.invoke(requestContext, "getLoginUserId", "N/A");
        }

        public String getLoginUserRole() {
            return Utils.invoke(requestContext, "getLoginUserRole", "N/A");
        }
    }
}
