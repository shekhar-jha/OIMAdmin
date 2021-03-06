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

package com.jhash.oimadmin.oim.perf;


import com.jhash.oimadmin.oim.JMXConnection;

public class PerfConfiguration {

    public static final String ATTR_NAME = "Name";
    public static final String ATTR_BEAN = "Bean";
    public static final String ATTR_BEAN_ATTRIBUTE = "BeanAttribute";
    public final JMXConnection.OIM_JMX_BEANS mBean;
    public final String attributeName;
    public final String displayName;

    public PerfConfiguration(String displayName, String componentBeanName, String attributeName) {
        this(displayName, new JMXConnection.OIM_JMX_BEANS(componentBeanName), attributeName);
    }

    public PerfConfiguration(String displayName, JMXConnection.OIM_JMX_BEANS oimJmxBeans, String attributeName) {
        this.mBean = oimJmxBeans;
        this.attributeName = attributeName;
        this.displayName = (displayName == null ? oimJmxBeans.name : displayName);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof PerfConfiguration) {
            if (this.mBean == ((PerfConfiguration) object).mBean || (this.mBean != null && this.mBean.equals(((PerfConfiguration) object).mBean))) {
                if ((this.attributeName == ((PerfConfiguration) object).attributeName) || (this.attributeName != null && this.attributeName.equalsIgnoreCase(((PerfConfiguration) object).attributeName)))
                    return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (mBean + ":" + attributeName).hashCode();
    }

    public enum DATA_POINT {
        MIN("_minTime"), MAX("_maxTime"), AVG("_avg"), COMPLETED_TRANSACTIONS("_completed"), TOTAL_TRANSACTION_TIME("_time");
        public String beanNameSuffix;

        DATA_POINT(String beanNameSuffix) {
            this.beanNameSuffix = beanNameSuffix;
        }
    }

}
