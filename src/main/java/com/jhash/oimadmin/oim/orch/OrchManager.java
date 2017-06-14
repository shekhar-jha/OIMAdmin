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

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.DBConnection;
import com.jhash.oimadmin.oim.Details;
import com.jhash.oimadmin.oim.JMXConnection;
import com.jhash.oimadmin.oim.OIMConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrchManager {
    public static final String GET_ORCHESTRATION_PROCESS_DETAILS = "select * from ORCHPROCESS where ID = ?";
    public static final String GET_ORCHESTRATION_PROCESS_EVENT_HANDLER_DETAILS = "select * from ORCHEVENTS where PROCESSID = ? order by orchorder";
    public static final String CONTEXT_VAL = "CONTEXTVAL";
    public static final String ORCHESTRATION = "ORCHESTRATION";
    public static final String ORCH_EVENTS = "ORCHEVENTS";
    public static final String ORCHESTRATION_ERROR = "ORCHESTRATION_ERROR";

    private static final Logger logger = LoggerFactory.getLogger(OrchManager.class);
    private final JMXConnection jmxConnection;
    private final OIMConnection oimConnection;
    private final DBConnection dbConnection;

    public OrchManager(OIMConnection oimConnection, JMXConnection jmxConnection, DBConnection dbConnection) {
        this.jmxConnection = jmxConnection;
        this.oimConnection = oimConnection;
        this.dbConnection = dbConnection;
    }

    public Config.OIM_VERSION getVersion() {
        return jmxConnection.getVersion();
    }

    public void cancel(Long processId, boolean compensate, boolean cascade) {
        oimConnection.executeOrchestrationOperation("cancel", new Class[]{long.class, boolean.class, boolean.class},
                new Object[]{processId, compensate, cascade});
    }

    public void cancelPendingFailedEvent(long processId) {
        oimConnection.executeOrchestrationOperation("cancelPendingFailedEvent", new Class[]{long.class},
                new Object[]{processId});
    }

    public void handleFailedEvent(long processId, Object failedEventResponse) {
        try {
            Class<?> failedResponseClass = oimConnection.getClass("oracle.iam.platform.kernel.vo.FailedEventResult$Response");
            Class<?> failedEventResultClass = oimConnection.getClass("oracle.iam.platform.kernel.vo.FailedEventResult");
            Object result = failedEventResultClass.getDeclaredConstructor(failedResponseClass).newInstance(failedEventResponse);
            oimConnection.executeOrchestrationOperation("handleFailed", new Class[]{long.class, failedEventResultClass},
                    new Object[]{processId, result});
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to handle failed event for process id " + processId + " with response " + failedEventResponse, exception);
        }
    }

    public Object[] getAllowedResponsesForFailedEvent() {
        Class<?> failedResponseClass = oimConnection.getClass("oracle.iam.platform.kernel.vo.FailedEventResult$Response");
        return failedResponseClass.getEnumConstants();
    }

    public Details getOrchestrationProcessDetails(long orchestrationProcessID) {
        Details result = dbConnection.invokeSQL(GET_ORCHESTRATION_PROCESS_DETAILS, orchestrationProcessID);
        for (int processingValue = 0; processingValue < result.size(); processingValue++) {
            Map<String, Object> values = result.getItemAt(processingValue);
            if (values.containsKey(CONTEXT_VAL)) {
                Object contextValObject = values.get(CONTEXT_VAL);
                String contextValAsString = "";
                if (contextValObject != null) {
                    if (contextValObject instanceof Clob) {
                        Clob contextVal = (Clob) contextValObject;
                        try {
                            contextValAsString = contextVal.getSubString(1, (int) contextVal.length());
                        } catch (SQLException exception) {
                            logger.warn("Failed to extract context value while processing result of orchestration process " + orchestrationProcessID, exception);
                            contextValAsString = "Failed to extract value.. Error " + exception;
                        }
                    } else {
                        contextValAsString = contextValObject.toString();
                    }
                }
                values.put(CONTEXT_VAL, contextValAsString);
            }
            switch (getVersion()) {
                case OIM11GR2PS2: {
                    StringBuilder errorMessage = new StringBuilder();
                    Orchestration orchestrationObject = null;
                    try {
                        orchestrationObject = oimConnection.executeOrchestrationOperation("getOrchestration", new Class[]{long.class}, new Object[]{orchestrationProcessID});
                        if (orchestrationObject != null) {
                            values.put(ORCHESTRATION, orchestrationObject);
                        } else {
                            values.put(ORCHESTRATION_ERROR, "Failed to retrieve Orchestration for " + orchestrationProcessID);
                        }
                    } catch (Exception exception) {
                        Utils.extractExceptionDetails(exception, errorMessage.append("Failed to extract Orchestration for process ID " + orchestrationProcessID));
                    }
                    Details resultValues = dbConnection.invokeSQL(GET_ORCHESTRATION_PROCESS_EVENT_HANDLER_DETAILS, orchestrationProcessID);
                    List<Event11gR2PS2> eventDetails = new ArrayList<>();
                    for (Object[] record : resultValues.getData()) {
                        try {
                            eventDetails.add(new Event11gR2PS2(record, oimConnection.getClassLoader()));
                        } catch (Exception exception) {
                            Utils.extractExceptionDetails(exception, errorMessage.append("Failed to extract Event Details."));
                        }
                    }
                    values.put(ORCH_EVENTS, eventDetails);
                    String orchestrationErrorMessage = errorMessage.toString();
                    if (!Utils.isEmpty(orchestrationErrorMessage)) {
                        values.put(ORCHESTRATION_ERROR, orchestrationErrorMessage);
                    }
                    break;
                }
                default: {
                    PublicProcessImpl orchestrationProcessDetails = null;
                    Object orchestrationObject = values.remove(ORCHESTRATION);
                    if (orchestrationObject != null) {
                        if (orchestrationObject instanceof Blob) {
                            Blob orchestrationBlob = (Blob) orchestrationObject;
                            try {
                                orchestrationProcessDetails = oimConnection.getOrchestration(orchestrationBlob);
                            } catch (Exception exception) {
                                logger.warn("Failed to extract orchestration details from blob for process ID " + orchestrationProcessID, exception);
                                values.put(ORCHESTRATION_ERROR, "Orchestration details could not be retrieved. Error " + exception);
                            }
                        } else {
                            values.put(ORCHESTRATION_ERROR, "Orchestration details was not retrieved as a BLOB. Could not process further.");
                        }
                    }
                    if (orchestrationProcessDetails != null) {
                        values.put(ORCHESTRATION, orchestrationProcessDetails);
                    } else if (!values.containsKey(ORCHESTRATION_ERROR)) {
                        values.put(ORCHESTRATION_ERROR, "Orchestration details was not retrieved due to unknown error.");
                    }
                    break;
                }
            }
        }
        return result;
    }

}
