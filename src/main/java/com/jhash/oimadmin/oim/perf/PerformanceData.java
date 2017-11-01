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

import java.util.HashMap;
import java.util.Map;

public class PerformanceData {
    public Snapshot startSnapshot;
    public Snapshot endSnapshot;

    @Override
    public String toString() {
        if (endSnapshot != null && startSnapshot != null
                && endSnapshot.get(PerfConfiguration.DATA_POINT.TOTAL_TRANSACTION_TIME) != null
                && endSnapshot.get(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS) != null) {
            long totalTransactionsDuringTest = endSnapshot.getAsLong(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS) - startSnapshot.getAsLong(PerfConfiguration.DATA_POINT.COMPLETED_TRANSACTIONS);
            if (totalTransactionsDuringTest > 0) {
                return "" + ((endSnapshot.getAsLong(PerfConfiguration.DATA_POINT.TOTAL_TRANSACTION_TIME) - startSnapshot.getAsLong(PerfConfiguration.DATA_POINT.TOTAL_TRANSACTION_TIME))
                        / totalTransactionsDuringTest);
            } else {
                return "No Change";
            }
        } else if (endSnapshot != null && endSnapshot.get(PerfConfiguration.DATA_POINT.AVG) != null) {
            return endSnapshot.get(PerfConfiguration.DATA_POINT.AVG);
        } else if (startSnapshot != null && startSnapshot.get(PerfConfiguration.DATA_POINT.AVG) != null) {
            return startSnapshot.get(PerfConfiguration.DATA_POINT.AVG);
        } else {
            return "";
        }
    }

    public static class Snapshot {
        private Map<PerfConfiguration.DATA_POINT, Object> snapShot;

        public Snapshot(Map<PerfConfiguration.DATA_POINT, Object> snapShot) {
            this.snapShot = snapShot == null ? new HashMap<PerfConfiguration.DATA_POINT, Object>() : snapShot;
        }

        public String get(PerfConfiguration.DATA_POINT data_point) {
            if (data_point == null)
                return "";
            Object value;
            if ((value = snapShot.get(data_point)) != null)
                return value.toString();
            else
                return "";
        }

        public long getAsLong(PerfConfiguration.DATA_POINT data_point) {
            if (data_point == null)
                return 0;
            Object value;
            if ((value = snapShot.get(data_point)) instanceof Long)
                return (long) value;
            else if (value instanceof Integer) {
                return (long) (int) (Integer) value;
            } else if (value instanceof String) {
                try {
                    Long valueAsLong = Long.parseLong((String) value);
                    if (valueAsLong != null)
                        return valueAsLong;
                    else
                        return 0;
                } catch (Exception exception) {
                    return 0;
                }
            } else
                return 0;

        }
    }
}
