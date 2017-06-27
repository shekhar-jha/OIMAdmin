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

package com.jhash.oimadmin.oim.code;

import com.jhash.oimadmin.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by S0000004 on 6/26/17.
 */
public class CompileUnit {

    public final String className;

    public final String sourceCode;

    public final List<String> options = new ArrayList<>();

    public CompileUnit(String className, String sourceCode) {
        this.className = className;
        this.sourceCode = sourceCode;
    }

    public CompileUnit setTargetVersion(String version) {
        if (!Utils.isEmpty(version)) {
            options.add("-target");
            options.add(version);
        }
        return this;
    }

    public CompileUnit setSourceVersion(String version) {
        if (!Utils.isEmpty(version)) {
            options.add("-source");
            options.add(version);
        }
        return this;
    }

    public CompileUnit setOption(String... optionValues) {
        if (optionValues != null && optionValues.length > 0) {
            options.addAll(Arrays.asList(optionValues));
        }
        return this;
    }

    public String toString() {
        return "Class: " + className + ", Options: " + options;
    }
}
