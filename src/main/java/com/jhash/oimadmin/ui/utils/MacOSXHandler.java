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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacOSXHandler {

    private static final Logger logger = LoggerFactory.getLogger(MacOSXHandler.class);
    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
    }

    public static void initialize(final OIMAdmin admin) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "sysadmin++");
        try {
            Class applicationClass = Class.forName("com.apple.eawt.Application");
            Class quitStrategyClass = Class.forName("com.apple.eawt.QuitStrategy");
            Object application = applicationClass.getMethod("getApplication").invoke(null);
            Object quitStrategyCloseAll = quitStrategyClass.getMethod("valueOf").invoke(null, "CLOSE_ALL_WINDOWS");
            applicationClass.getMethod("setQuitStrategy", quitStrategyClass).invoke(application, quitStrategyCloseAll);
        }catch (Exception exception) {
            logger.warn("Failed to initialize on Mac Platform.", exception);
        }
    }

    public static void registerMenuBar(javax.swing.JMenuBar menuBar) {
        try {
            Class applicationClass = Class.forName("com.apple.eawt.Application");
            Object application = applicationClass.getMethod("getApplication").invoke(null);
            if (application != null) {
                applicationClass.getMethod("setDefaultMenuBar", javax.swing.JMenuBar.class).invoke(application, menuBar);
            } else {
                logger.warn("Could not locate Application object on Mac platform.");
            }
        }catch (Exception exception) {
            logger.warn("Failed to set Default Menu Bar.", exception);
        }
    }

    public static void cleanup(final OIMAdmin admin) {
        System.exit(0);
    }
}
