/*
 * Copyright 2014 Shekhar Jha
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
package com.jhash.oimadmin.ui;

public class MacOSXHandler {

    public static void initialize(OIMAdmin admin) {
        com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
        application.setQuitStrategy(com.apple.eawt.QuitStrategy.CLOSE_ALL_WINDOWS);
    }
}
