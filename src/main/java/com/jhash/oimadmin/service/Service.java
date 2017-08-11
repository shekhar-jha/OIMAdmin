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

package com.jhash.oimadmin.service;

import com.jhash.oimadmin.EnumClass;
import com.jhash.oimadmin.events.Event;

public interface Service<T extends Service> {

    Event INITIALIZE = new Event("INITIALIZE");
    Event DESTROY = new Event("DESTROY");

    STATE NOT_INITIALIZED = new STATE("NOT_INITIALIZED");
    STATE INITIALIZED = new STATE("INITIALIZED");
    STATE INITIALIZED_NO_OP = new STATE("INITIALIZED_NO_OP");
    STATE FAILED = new STATE("FAILED");
    STATE INITIALIZATION_IN_PROGRESS = new STATE("INITIALIZATION_IN_PROGRESS");
    STATE DESTRUCTION_IN_PROGRESS = new STATE("DESTRUCTION_IN_PROGRESS");

    T initialize();

    STATE getState();

    void destroy();

    class STATE extends EnumClass {
        public STATE(String state) {
            super(state);
        }
    }
}
