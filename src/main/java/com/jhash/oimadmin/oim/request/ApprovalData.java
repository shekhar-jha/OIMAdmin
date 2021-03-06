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

public class ApprovalData {

    private final Object approvalData;
    private final ClassLoader classLoader;

    public ApprovalData(Object approvalData, ClassLoader classLoader) {
        this.approvalData = approvalData;
        this.classLoader = classLoader;
    }

    public long getRequestKey() {
        return Utils.invoke(approvalData, "getRequestKey", -1L);
    }

    public String getApprovalInstanceID() {
        return Utils.invoke(approvalData, "getApprovalInstanceID", "N/A");
    }

    public String getStatus() {
        return Utils.invoke(approvalData, "getStatus", "N/A");
    }

    public Long getApprovalKey() {
        return Utils.invoke(approvalData, "getApprovalKey", -1L);
    }

    public String getStage() {
        return Utils.invoke(approvalData, "getStage", "N/A");
    }

    public Request getRequest() {
        Object request = Utils.invoke(approvalData, "getRequest", null);
        if (request == null) {
            return null;
        }
        return new Request(request, classLoader);
    }
}
