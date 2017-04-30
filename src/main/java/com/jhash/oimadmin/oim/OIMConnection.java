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
package com.jhash.oimadmin.oim;

import com.jhash.oimadmin.Config.Configuration;
import com.jhash.oimadmin.Config.PLATFORM;
import com.jhash.oimadmin.Connection;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.thortech.xl.systemverification.api.DDKernelService;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Role;
import oracle.iam.platform.authopss.vo.OperationContext;
import oracle.iam.platform.kernel.impl.PublicProcessImpl;
import oracle.iam.platformservice.api.PlatformService;
import oracle.iam.platformservice.api.PlatformUtilsService;
import oracle.iam.request.api.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class OIMConnection extends AbstractConnection {

    public static final String ATTR_OIM_HOME = "oim_home";
    public static final String ATTR_OIM_URL = "oim_url";
    public static final String ATTR_OIM_USER = "oim_user";
    public static final String ATTR_OIM_PWD = "oim_pwd";
    public static final String ATTR_OIM_LOGIN_CONFIG = "java.security.auth.login.config";
    public static final String ATTR_OIM_APP_SERVER_TYPE = "APPSERVER_TYPE";
    public static final String ATTR_OIM_APP_SERVER_TYPE_WLS = "wls";
    public static final String ATTR_EXPORT_DIR = "oim.export.dir";
    public static final String VAL_DEFAULT_OIM_HOME = System.getProperty("user.home") + "/.oimadm/";
    public static final String ATTR_OIM_VERSION = "oim_version";

    private static final Logger logger = LoggerFactory.getLogger(OIMConnection.class);
    // TODO: Not thread safe
    private boolean isLogin = false;

    private OIMClient oimClient = null;

    public OIMConnection() {
        STRING_REPRESENTATION = "OIMConnection:";
    }

    public void initializeConnection(Configuration config) {
        logger.debug("Trying to initialize OIM using {}", config);
        initializeForAppServer(config);
        oimClient = initializeOIMClient(config);
        logger.debug("Initialized OIMCLI");
    }

    private void initializeForAppServer(Configuration config) {
        PLATFORM platform = PLATFORM.fromString(config.getProperty(Connection.ATTR_CONN_PLATFORM));
        logger.debug("Trying to initialize for application server {}", platform);
        switch (platform) {
            case WEBLOGIC:
                String xlHome = config.getWorkArea();
                if (config.getProperty(ATTR_OIM_HOME) != null) {
                    xlHome = config.getProperty(ATTR_OIM_HOME, VAL_DEFAULT_OIM_HOME);
                } else if (
                        System.getProperties().contains("XL.HomeDir")) {
                    xlHome = System.getProperty("XL.HomeDir");
                }
                if (xlHome == null || xlHome.isEmpty())
                    throw new NullPointerException("Failed to locate the home directory that will contain /conf/authwl.conf.");
                String authWlFileLocation = xlHome + "/conf/authwl.conf";
                File authWlFile = new File(authWlFileLocation);
                if (!authWlFile.exists() || !authWlFile.canRead())
                    throw new NullPointerException("Failed to locate readable version of " + authWlFileLocation + ". Please validate");
                System.setProperty(ATTR_OIM_LOGIN_CONFIG, authWlFileLocation);
                logger.debug("Set property {}={}", ATTR_OIM_LOGIN_CONFIG, System.getProperty(ATTR_OIM_LOGIN_CONFIG));
                // Ensure that jndi.properties has been updated with latest weblogic
                // information. Not documented.
                System.setProperty(ATTR_OIM_APP_SERVER_TYPE, ATTR_OIM_APP_SERVER_TYPE_WLS);
                logger.debug("Set property {}={}", ATTR_OIM_APP_SERVER_TYPE, System.getProperty(ATTR_OIM_APP_SERVER_TYPE));
                // Undocumented.
                System.setProperty("weblogic.Name", "someName");
                logger.debug("Set property weblogic.Name={}", ATTR_OIM_APP_SERVER_TYPE, System.getProperty("weblogic.Name"));
                break;
            default:
                throw new UnsupportedOperationException("Application does not support any other platform except weblogic");
        }
    }

    private OIMClient initializeOIMClient(Configuration config) {
        PLATFORM platform = PLATFORM.fromString(config.getProperty(Connection.ATTR_CONN_PLATFORM));
        switch (platform) {
            case WEBLOGIC:
                Hashtable<String, String> env = new Hashtable<String, String>();
                env.put("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
                env.put(OIMClient.WLS_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
                env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, config.getProperty(ATTR_OIM_URL));
                STRING_REPRESENTATION += config.getProperty(ATTR_OIM_URL);
                logger.debug("Trying to create OIMClient with configuration {}", env);
                return new OIMClient(env);
            default:
                throw new UnsupportedOperationException("Application does not support any other platform except weblogic");
        }
    }

    public boolean login() {
        return login(oimClient, config);
    }

    private boolean login(OIMClient oimClient, Configuration config) {
        return login(oimClient, config.getProperty(ATTR_OIM_USER), config.getProperty(ATTR_OIM_PWD).toCharArray());
    }

    private boolean login(OIMClient oimClient, String userName, char[] password) {
        boolean success = false;
        try {
            logger.debug("Trying to login user {}", userName);
            List<Role> roles = oimClient.login(userName, password);
            logger.debug("Successfully performed login. Roles {}", roles);
            success = true;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to perform login for user " + userName, exception);
        }
        if (success)
            isLogin = true;
        return success;
    }

    public void registerPlugin(byte[] data) {
        if (data == null)
            throw new NullPointerException("The plugin that needs to be registered was provided as null");
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        PlatformService platformService = oimClient.getService(PlatformService.class);
        try {
            logger.debug("Trying to register plugin of size {} ", data.length);
            platformService.registerPlugin(data);
            logger.debug("Registered plugin");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to register plugin", exception);
        }
    }

    public void unregisterPlugin(String name) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        PlatformService platformService = oimClient.getService(PlatformService.class);
        try {
            logger.debug("Trying to unregister plugin {}", name);
            platformService.unRegisterPlugin(name);
            logger.debug("Unregistered plugin {}", name);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to unregister plugin " + name, exception);
        }
    }

    public void purgeCache(String cacheName) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        PlatformUtilsService platformService = oimClient.getService(PlatformUtilsService.class);
        if (Utils.isEmpty(cacheName))
            cacheName = "All";
        try {
            logger.debug("Trying to purge cache {}", cacheName);
            platformService.purgeCache(cacheName);
            logger.debug("Purged cache");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to purge cache " + cacheName, exception);
        }
    }

    public Request getRequestDetails(String requestId) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            RequestService requestService = oimClient.getService(RequestService.class);
            Request request = new Request(requestService.getBasicRequestData(requestId));
            return request;
        } catch (Exception exception) {
            throw new OIMAdminException("Error occurred while retrieving request details for request id " + requestId, exception);
        }
    }

    public <T> T executeOrchestrationOperation(String method, Class[] parameterTypes, Object[] parameters) {
        if (!isLogin)
            throw new IllegalStateException("The OIM Connection " + this + " is not in a login state");
        try {
            DDKernelService kernelService = oimClient.getService(DDKernelService.class);
            logger.trace("Trying to invoke method {} with parameters {} on DDKernelService {}", new Object[]{method, parameters, kernelService});
            Object result = kernelService.invoke(method, parameters, parameterTypes);
            logger.trace("Returned result {}", result);
            return (T) result;
        } catch (Exception exception) {
            throw new OIMAdminException("Error occurred while invoking method " + method + " on DDKernelService with parameters " + Arrays.toString(parameters), exception);
        }
    }

    public PublicProcessImpl getOrchestration(Blob orchestrationObject) {
        ObjectInputStream ins = null;
        try {
            ins = new ObjectInputStream(new GZIPInputStream(orchestrationObject.getBinaryStream()));
            Object readObject = ins.readObject();
            return new PublicProcessImpl(readObject);
        } catch (Exception e) {
            throw new OIMAdminException("Failed to read process object from Blob " + orchestrationObject, e);
        } finally {
            if (null != ins) {
                try {
                    ins.close();
                } catch (IOException e) {
                    logger.debug("Failed to close Object input stream " + ins, e);
                }
            }
        }

    }

    public void logout() {
        logout(oimClient);
    }

    private void logout(OIMClient oimClient) {
        isLogin = false;
        if (oimClient != null) {
            try {
                logger.debug("Trying to perform logout");
                oimClient.logout();
                logger.debug("Successfully performed logout");
            } catch (Exception exception) {
                throw new OIMAdminException("Error occurred while performing logout on client " + oimClient
                        + ". Ignoring", exception);
            }
        } else {
            logger.warn("OIM Client not available. Ignoring logout call");
        }
    }

    @Override
    public void destroyConnection() {
        logger.debug("Trying to destroy connection {}", this);
        if (oimClient != null) {
            if (isLogin)
                logout();
            oimClient = null;
        }
        logger.debug("Destroyed connection {}", this);
    }

    public static class Request {

        private final oracle.iam.request.vo.Request request;

        public Request(oracle.iam.request.vo.Request request) {
            this.request = request;
        }

        public String getRequestModelName() {
            return request.getRequestModelName();
        }

        public String getJustification() {
            return request.getJustification();
        }

        public String getRequestStatus() {
            return request.getRequestStatus();
        }

        public long getRequestStage() {
            return request.getRequestStage();
        }

        public String getRequestID() {
            return request.getRequestID();
        }

        public Request getParentRequest() {
            return new Request(request.getParentRequest());
        }

        public List<Request> getChildRequests() {
            List<oracle.iam.request.vo.Request> result = request.getChildRequests();
            if (result == null)
                return null;
            List<Request> calculatedRequest = new ArrayList<>();
            for (oracle.iam.request.vo.Request request : result) {
                calculatedRequest.add(new Request(request));
            }
            return calculatedRequest;
        }

        public Date getExecutionDate() {
            return request.getExecutionDate();
        }

        public List<RequestEntity> getTargetEntities() {
            List<oracle.iam.request.vo.RequestEntity> targetEntities = request.getTargetEntities();
            if (targetEntities == null)
                return null;
            List<RequestEntity> calculatedTargetEntities = new ArrayList<>();
            for (oracle.iam.request.vo.RequestEntity requestEntity : targetEntities) {
                calculatedTargetEntities.add(new RequestEntity(requestEntity));
            }
            return calculatedTargetEntities;
        }

        public List<Beneficiary> getBeneficiaries() {
            List<oracle.iam.request.vo.Beneficiary> beneficiaries = request.getBeneficiaries();
            if (beneficiaries == null)
                return null;
            List<Beneficiary> calculatedBeneficiaries = new ArrayList<>();
            for (oracle.iam.request.vo.Beneficiary beneficiary : beneficiaries) {
                calculatedBeneficiaries.add(new Beneficiary(beneficiary));
            }
            return calculatedBeneficiaries;
        }

        public Long getRequestKey() {
            return request.getRequestKey();
        }

        public String getRequesterKey() {
            return request.getRequesterKey();
        }

        public Date getCreationDate() {
            return request.getCreationDate();
        }

        public String isParent() {
            return request.isParent();
        }

        public long getOrchID() {
            return request.getOrchID();
        }

        public long getEventID() {
            return request.getEventID();
        }

        public String getBeneficiaryType() {
            return request.getBeneficiaryType();
        }

        public List<ApprovalData> getApprovalData() {
            List<oracle.iam.request.vo.ApprovalData> approvalDatas = request.getApprovalData();
            if (approvalDatas == null)
                return null;
            List<ApprovalData> calculatedApprovalData = new ArrayList<>();
            for (oracle.iam.request.vo.ApprovalData approvalData : approvalDatas) {
                calculatedApprovalData.add(new ApprovalData(approvalData));
            }
            return calculatedApprovalData;
        }

        public Date getEndDate() {
            return request.getEndDate();
        }

        public List<TemplateAttribute> getTemplateAttributes() {
            List<oracle.iam.request.vo.RequestTemplateAttribute> requestTemplateAttributes = request.getTemplateAttributes();
            if (requestTemplateAttributes == null)
                return null;
            List<TemplateAttribute> calculatedTemplateAttributes = new ArrayList<>();
            for (oracle.iam.request.vo.RequestTemplateAttribute requestTemplateAttribute : requestTemplateAttributes) {
                calculatedTemplateAttributes.add(new TemplateAttribute(requestTemplateAttribute));
            }
            return calculatedTemplateAttributes;
        }

        public String getReasonForFailure() {
            return request.getReasonForFailure();
        }

        public List<TemplateAttribute> getAdditionalAttributes() {
            List<oracle.iam.request.vo.RequestTemplateAttribute> requestTemplateAttributes = request.getAdditionalAttributes();
            if (requestTemplateAttributes == null)
                return null;
            List<TemplateAttribute> calculatedTemplateAttributes = new ArrayList<>();
            for (oracle.iam.request.vo.RequestTemplateAttribute requestTemplateAttribute : requestTemplateAttributes) {
                calculatedTemplateAttributes.add(new TemplateAttribute(requestTemplateAttribute));
            }
            return calculatedTemplateAttributes;
        }

        public RequestContext getRequestContext() {
            OperationContext context = request.getRequestContext();
            if (context == null || (!(context instanceof RequestContext))) {
                return null;
            }
            return new RequestContext((oracle.iam.request.vo.RequestContext) context);
        }

        public String getDependsOnRequestId() {
            return request.getDependsOnRequestId();
        }

        public boolean isDirectOperation() {
            return request.isDirectOperation();
        }
    }

    public static class RequestContext {

        private final oracle.iam.request.vo.RequestContext requestContext;

        public RequestContext(oracle.iam.request.vo.RequestContext requestContext) {
            this.requestContext = requestContext;
        }

        public String getRequestId() {
            return requestContext.getRequestId();
        }

        public String getLoginUserId() {
            return requestContext.getLoginUserId();
        }

        public String getLoginUserRole() {
            return requestContext.getLoginUserRole();
        }
    }

    public static class TemplateAttribute {

        private final oracle.iam.request.vo.RequestTemplateAttribute templateAttribute;

        public TemplateAttribute(oracle.iam.request.vo.RequestTemplateAttribute templateAttribute) {
            this.templateAttribute = templateAttribute;
        }

        public String getName() {
            return templateAttribute.getName();
        }

        public String getType() {
            return templateAttribute.getType()==null?"null":templateAttribute.getType().toString();
        }

        public Object getValue() {
            return templateAttribute.getValue();
        }

        public String getTypeHolder() {
            return templateAttribute.getTypeHolder();
        }

        public String getValueHolder() {
            return templateAttribute.getValueHolder();
        }

        public char[] getValueHolderAsCharArray() {
            return templateAttribute.getValueHolderAsCharArray();
        }

        public Byte[] getValueHolderByteArray() {
            return templateAttribute.getValueHolderByteArray();
        }
    }

    public static class RequestEntity {

        private final oracle.iam.request.vo.RequestEntity requestEntity;

        public RequestEntity(oracle.iam.request.vo.RequestEntity requestEntity) {
            this.requestEntity = requestEntity;
        }

        public Request getRequest() {
            return new Request(requestEntity.getRequest());
        }

        public String getRequestEntityType() {
            return requestEntity.getRequestEntityType().getValue();
        }

        public List<RequestEntityAttribute> getEntityData() {
            List<oracle.iam.request.vo.RequestEntityAttribute> entityData = requestEntity.getEntityData();
            if (entityData == null)
                return null;
            List<RequestEntityAttribute> calculatedRequestEntityAttributes = new ArrayList<>();
            for (oracle.iam.request.vo.RequestEntityAttribute requestEntityAttribute : entityData) {
                calculatedRequestEntityAttributes.add(new RequestEntityAttribute(requestEntityAttribute));
            }
            return calculatedRequestEntityAttributes;
        }

        public String getEntitySubType() {
            return requestEntity.getEntitySubType();
        }

        public String getEntityKey() {
            return requestEntity.getEntityKey();
        }

        public String getOperation() {
            return requestEntity.getOperation();
        }

        public List<RequestEntityAttribute> getAdditionalEntityData() {
            List<oracle.iam.request.vo.RequestEntityAttribute> entityData = requestEntity.getAdditionalEntityData();
            if (entityData == null)
                return null;
            List<RequestEntityAttribute> calculatedRequestEntityAttributes = new ArrayList<>();
            for (oracle.iam.request.vo.RequestEntityAttribute requestEntityAttribute : entityData) {
                calculatedRequestEntityAttributes.add(new RequestEntityAttribute(requestEntityAttribute));
            }
            return calculatedRequestEntityAttributes;
        }
    }

    public static class RequestEntityAttribute {
        private final oracle.iam.request.vo.RequestEntityAttribute requestEntityAttribute;

        public RequestEntityAttribute(oracle.iam.request.vo.RequestEntityAttribute requestEntityAttribute) {
            this.requestEntityAttribute = requestEntityAttribute;
        }

        public boolean hasChild() {
            return requestEntityAttribute.hasChild();
        }

        public List<RequestEntityAttribute> getChildAttributes() {
            List<oracle.iam.request.vo.RequestEntityAttribute> childAttributes = requestEntityAttribute.getChildAttributes();
            if (childAttributes == null)
                return null;
            List<RequestEntityAttribute> calculatedRequestEntityAttributes = new ArrayList<>();
            for (oracle.iam.request.vo.RequestEntityAttribute entityAttribute : childAttributes) {
                calculatedRequestEntityAttributes.add(new RequestEntityAttribute(entityAttribute));
            }
            return calculatedRequestEntityAttributes;
        }

        public String getName() {
            return requestEntityAttribute.getName();
        }

        public Serializable getValue() {
            return requestEntityAttribute.getValue();
        }

        public String getType() {
            return requestEntityAttribute.getType() == null ? "null" : requestEntityAttribute.getType().toString();
        }

        public String getTypeHolder() {
            return requestEntityAttribute.getTypeHolder();
        }

        public Byte[] getValueHolderByteArray() {
            return requestEntityAttribute.getValueHolderByteArray();
        }

        public char[] getValueHolderAsCharArray() {
            return requestEntityAttribute.getValueHolderAsCharArray();
        }

        public RequestEntityAttribute getParentAttribute() {
            oracle.iam.request.vo.RequestEntityAttribute requestEntityAttributeObject = requestEntityAttribute.getParentAttribute();
            if (requestEntityAttributeObject == null)
                return null;
            return new RequestEntityAttribute(requestEntityAttributeObject);
        }

        public String getRowKey() {
            return requestEntityAttribute.getRowKey();
        }

        public String getAction() {
            return requestEntityAttribute.getAction() == null ? "null" : requestEntityAttribute.getAction().toString();
        }

        public String getActionHolder() {
            return requestEntityAttribute.getActionHolder();
        }

        public boolean isValueValid() {
            return requestEntityAttribute.isValueValid();
        }

        public String getValueHolder() {
            return requestEntityAttribute.getValueHolder();
        }

        public Map getMlsMap() {
            return requestEntityAttribute.getMlsMap();
        }

        public String getDefaultMLSValue() {
            return requestEntityAttribute.getDefaultMLSValue();
        }
    }

    public static class Beneficiary {

        private final oracle.iam.request.vo.Beneficiary beneficiary;

        public Beneficiary(oracle.iam.request.vo.Beneficiary beneficiary) {
            this.beneficiary = beneficiary;
        }

        public String getBeneficiaryType() {
            return beneficiary.getBeneficiaryType();
        }

        public Request getRequest() {
            oracle.iam.request.vo.Request request = beneficiary.getRequest();
            if (request == null)
                return null;
            return new Request(request);
        }

        public String getBeneficiaryKey() {
            return beneficiary.getBeneficiaryKey();
        }

        public List<RequestBeneficiaryEntity> getTargetEntities() {
            List<oracle.iam.request.vo.RequestBeneficiaryEntity> requestBeneficiaryEntities = beneficiary.getTargetEntities();
            if (requestBeneficiaryEntities == null)
                return null;
            List<RequestBeneficiaryEntity> calculatedBeneficiaryEntities = new ArrayList<>();
            for (oracle.iam.request.vo.RequestBeneficiaryEntity requestBeneficiaryEntity : requestBeneficiaryEntities) {
                calculatedBeneficiaryEntities.add(new RequestBeneficiaryEntity(requestBeneficiaryEntity));
            }
            return calculatedBeneficiaryEntities;
        }

        public HashMap<String, Object> getAttributes() {
            return beneficiary.getAttributes();
        }
    }

    public static class RequestBeneficiaryEntity {

        private final oracle.iam.request.vo.RequestBeneficiaryEntity requestBeneficiaryEntity;

        public RequestBeneficiaryEntity(oracle.iam.request.vo.RequestBeneficiaryEntity requestBeneficiaryEntity) {
            this.requestBeneficiaryEntity = requestBeneficiaryEntity;
        }

        public Long getKey() {
            return requestBeneficiaryEntity.getKey();
        }

        public Beneficiary getBeneficiary() {
            oracle.iam.request.vo.Beneficiary beneficiary = requestBeneficiaryEntity.getBeneficiary();
            if (beneficiary == null)
                return null;
            return new Beneficiary(beneficiary);
        }

        public String getRequestEntityType() {
            return requestBeneficiaryEntity.getRequestEntityType().getValue();
        }

        public List<RequestBeneficiaryEntityAttribute> getEntityData() {
            List<oracle.iam.request.vo.RequestBeneficiaryEntityAttribute> entityData = requestBeneficiaryEntity.getEntityData();
            if (entityData == null)
                return null;
            List<RequestBeneficiaryEntityAttribute> calculatedEntityData = new ArrayList<>();
            for (oracle.iam.request.vo.RequestBeneficiaryEntityAttribute entityDataValue : entityData) {
                calculatedEntityData.add(new RequestBeneficiaryEntityAttribute(entityDataValue));
            }
            return calculatedEntityData;
        }

        public String getEntitySubType() {
            return requestBeneficiaryEntity.getEntitySubType();
        }

        public String getEntityKey() {
            return requestBeneficiaryEntity.getEntityKey();
        }

        public String getOperation() {
            return requestBeneficiaryEntity.getOperation();
        }

        public List<RequestBeneficiaryEntityAttribute> getAdditionalEntityData() {
            List<oracle.iam.request.vo.RequestBeneficiaryEntityAttribute> entityData = requestBeneficiaryEntity.getAdditionalEntityData();
            if (entityData == null)
                return null;
            List<RequestBeneficiaryEntityAttribute> calculatedEntityData = new ArrayList<>();
            for (oracle.iam.request.vo.RequestBeneficiaryEntityAttribute entityAttribute : entityData) {
                calculatedEntityData.add(new RequestBeneficiaryEntityAttribute(entityAttribute));
            }
            return calculatedEntityData;
        }

        public RequestBeneficiaryEntity getDependsOnEntity() {
            oracle.iam.request.vo.RequestBeneficiaryEntity entity = requestBeneficiaryEntity.getDependsOnEntity();
            if (entity == null)
                return null;
            return new RequestBeneficiaryEntity(entity);
        }
    }

    public static class RequestBeneficiaryEntityAttribute {

        private final oracle.iam.request.vo.RequestBeneficiaryEntityAttribute requestBeneficiaryEntityAttribute;

        public RequestBeneficiaryEntityAttribute(oracle.iam.request.vo.RequestBeneficiaryEntityAttribute requestBeneficiaryEntityAttribute) {
            this.requestBeneficiaryEntityAttribute = requestBeneficiaryEntityAttribute;
        }

        public boolean isAdditional() {
            return requestBeneficiaryEntityAttribute.isAdditional();
        }

        public Map getMlsMap() {
            return requestBeneficiaryEntityAttribute.getMlsMap();
        }

        public String getDefaultMLSValue() {
            return requestBeneficiaryEntityAttribute.getDefaultMLSValue();
        }

        public boolean isMLS() {
            return requestBeneficiaryEntityAttribute.isMLS();
        }

        public boolean isMasked() {
            return requestBeneficiaryEntityAttribute.isMasked();
        }

        public boolean isValueValid() {
            return requestBeneficiaryEntityAttribute.isValueValid();
        }

        public String getActionHolder() {
            return requestBeneficiaryEntityAttribute.getActionHolder();
        }

        public String getAction() {
            return requestBeneficiaryEntityAttribute.getAction() == null ? "" : requestBeneficiaryEntityAttribute.getAction().toString();
        }

        public String getRowKey() {
            return requestBeneficiaryEntityAttribute.getRowKey();
        }

        public RequestBeneficiaryEntityAttribute getParentAttribute() {
            oracle.iam.request.vo.RequestBeneficiaryEntityAttribute parentRequestBeneficiaryEntityAttribute = requestBeneficiaryEntityAttribute.getParentAttribute();
            if (parentRequestBeneficiaryEntityAttribute == null)
                return null;
            return new RequestBeneficiaryEntityAttribute(parentRequestBeneficiaryEntityAttribute);
        }

        public RequestBeneficiaryEntity getEntity() {
            oracle.iam.request.vo.RequestBeneficiaryEntity entity = requestBeneficiaryEntityAttribute.getEntity();
            if (entity == null)
                return null;
            return new RequestBeneficiaryEntity(entity);
        }

        public String getValueHolder() {
            return requestBeneficiaryEntityAttribute.getValueHolder();
        }

        public String getTypeHolder() {
            return requestBeneficiaryEntityAttribute.getTypeHolder();
        }

        public String getType() {
            return requestBeneficiaryEntityAttribute.getType() == null ? "null" : requestBeneficiaryEntityAttribute.getType().toString();
        }

        public Serializable getValue() {
            return requestBeneficiaryEntityAttribute.getValue();
        }

        public String getName() {
            return requestBeneficiaryEntityAttribute.getName();
        }

        public List<RequestBeneficiaryEntityAttribute> getChildAttributes() {
            List<oracle.iam.request.vo.RequestBeneficiaryEntityAttribute> requestBeneficiaryEntityAttributes = requestBeneficiaryEntityAttribute.getChildAttributes();
            if (requestBeneficiaryEntityAttributes == null)
                return null;
            List<RequestBeneficiaryEntityAttribute> calculatedRequestBeneficiaryEntityAttributes = new ArrayList<>();
            for (oracle.iam.request.vo.RequestBeneficiaryEntityAttribute requestBeneficiaryEntityAttribute : requestBeneficiaryEntityAttributes) {
                calculatedRequestBeneficiaryEntityAttributes.add(new RequestBeneficiaryEntityAttribute(requestBeneficiaryEntityAttribute));
            }
            return calculatedRequestBeneficiaryEntityAttributes;
        }

        public boolean hasChild() {
            return requestBeneficiaryEntityAttribute.hasChild();
        }

        public String toString() {
            String value = "{ [" + getAction() + "]" + getName() + " : " + getValue() + " (" + getType() + ") [" + (hasChild() ? getChildAttributes() : "") + "]" + "}";
            return value;
        }
    }

    public static class ApprovalData {

        private final oracle.iam.request.vo.ApprovalData approvalData;

        public ApprovalData(oracle.iam.request.vo.ApprovalData approvalData) {
            this.approvalData = approvalData;
        }

        public long getRequestKey() {
            return approvalData.getRequestKey();
        }

        public String getApprovalInstanceID() {
            return approvalData.getApprovalInstanceID();
        }

        public String getStatus() {
            return approvalData.getStatus();
        }

        public Long getApprovalKey() {
            return approvalData.getApprovalKey();
        }

        public String getStage() {
            return approvalData.getStage();
        }

        public Request getRequest() {
            oracle.iam.request.vo.Request request = approvalData.getRequest();
            if (request == null) {
                return null;
            }
            return new Request(request);
        }
    }

}
