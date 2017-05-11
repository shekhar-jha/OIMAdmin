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

public class TemplateAttribute {

    private final Object templateAttribute;
    private final ClassLoader classLoader;

    public TemplateAttribute(Object templateAttribute, ClassLoader classLoader) {
        this.templateAttribute = templateAttribute;
        this.classLoader = classLoader;
    }

    public String getName() {
        return Utils.invoke(templateAttribute, "getName", "N/A");
    }

    public String getType() {
        Object type = Utils.invoke(templateAttribute, "getType");
        return type == null ? "null" : type.toString();
    }

    public Object getValue() {
        return Utils.invoke(templateAttribute, "getValue", "N/A");
    }

    public String getTypeHolder() {
        return Utils.invoke(templateAttribute, "getTypeHolder", "N/A");
    }

    public String getValueHolder() {
        return Utils.invoke(templateAttribute, "getValueHolder", "N/A");
    }

    public char[] getValueHolderAsCharArray() {
        return Utils.invoke(templateAttribute, "getValueHolderAsCharArray", new char[0]);
    }

    public Byte[] getValueHolderByteArray() {
        return Utils.invoke(templateAttribute, "getValueHolderByteArray", new Byte[0]);
    }
}
