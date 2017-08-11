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

package com.jhash.oimadmin;

public class EnumClass {

    private final String identifier;
    private final Object comparator;

    protected EnumClass(String identifier) {
        this(identifier, identifier);
    }

    protected EnumClass(String identifier, Object comparator) {
        if (Utils.isEmpty(identifier))
            throw new OIMAdminException("The " + this.getClass() + " can not have null or empty identifier");
        if (comparator == null)
            throw new OIMAdminException("The " + this.getClass() + " can not have null or empty comparator");
        this.identifier = identifier;
        this.comparator = comparator;
    }

    public String getName() {
        return identifier;
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EnumClass && ((EnumClass) obj).comparator.equals(comparator);
    }

    @Override
    public int hashCode() {
        return comparator.hashCode();
    }

}
