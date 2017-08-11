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
package com.jhash.oimadmin.ui.utils;

import com.jhash.oimadmin.ui.OIMAdmin;

import javax.swing.*;

public class MacOSXHandler {

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
    }

    public static void initialize(final OIMAdmin admin) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "sysadmin++");
        com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
        application.setQuitStrategy(com.apple.eawt.QuitStrategy.CLOSE_ALL_WINDOWS);
    }

    public static void registerMenuBar(JMenuBar menuBar) {
        com.apple.eawt.Application.getApplication().setDefaultMenuBar(menuBar);
    }

    public static void cleanup(final OIMAdmin admin) {
        System.exit(0);
    }
}
