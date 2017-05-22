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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Beneficiary {

    private final Object beneficiary;
    private final ClassLoader classLoader;

    public Beneficiary(Object beneficiary, ClassLoader classLoader) {
        this.beneficiary = beneficiary;
        this.classLoader = classLoader;
    }

    public String getBeneficiaryType() {
        return Utils.invoke(beneficiary, "getBeneficiaryType", "N/A");
    }

    public Request getRequest() {
        Object request = Utils.invoke(beneficiary, "getRequest", null);
        if (request == null)
            return null;
        return new Request(request, classLoader);
    }

    public String getBeneficiaryKey() {
        return Utils.invoke(beneficiary, "getBeneficiaryKey", "N/A");
    }

    public List<RequestBeneficiaryEntity> getTargetEntities() {
        Object requestBeneficiaryEntities = Utils.invoke(beneficiary, "getTargetEntities");
        if (requestBeneficiaryEntities == null || !(requestBeneficiaryEntities instanceof List))
            return null;
        List<RequestBeneficiaryEntity> calculatedBeneficiaryEntities = new ArrayList<>();
        for (Object requestBeneficiaryEntity : (List) requestBeneficiaryEntities) {
            calculatedBeneficiaryEntities.add(new RequestBeneficiaryEntity(requestBeneficiaryEntity, classLoader));
        }
        return calculatedBeneficiaryEntities;
    }

    public HashMap<String, Object> getAttributes() {
        return Utils.invoke(beneficiary, "getAttributes", new HashMap<String, Object>());
    }
}
