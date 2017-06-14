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

package com.jhash.oimadmin.oim;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Details implements Iterable<Map<String, Object>> {
    private List<Map<String, Object>> values;
    private String[] columnNames;

    public Details(List<Map<String, Object>> values, String[] columnNames) {
        this.values = values;
        this.columnNames = columnNames;
    }

    public Map<String, Object> getItemAt(int index) {
        return values.get(index);
    }

    public Object getItemAt(int index, String key, Object defaultValue) {
        Map<String, Object> item = values.get(index);
        if (item == null)
            return defaultValue;
        if (!item.containsKey(key))
            return defaultValue;
        Object value = item.get(key);
        if (value == null)
            return defaultValue;
        return value;
    }

    public Object[][] getData() {
        Object[][] data = new Object[values.size()][];
        int rowCounter = 0;
        for (Map<String, Object> value : values) {
            Object[] valueArray = new Object[columnNames.length];
            int columnCounter = 0;
            for (String columnName : columnNames) {
                valueArray[columnCounter++] = value.get(columnName);
            }
            data[rowCounter++] = valueArray;
        }
        return data;
    }

    public String[] getColumns() {
        return columnNames;
    }

    public int size() {
        return values.size();
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return values.iterator();
    }
}
