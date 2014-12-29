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
package com.jhash.oimadmin.oim;

import com.jhash.oimadmin.Config.Configuration;
import com.jhash.oimadmin.Connection;

public abstract class AbstractConnection implements Connection {

    protected Configuration config = null;
    protected String name = NAME_UNAVAILABLE;
    protected boolean isConnected = false;
    protected String STRING_REPRESENTATION = "AbstractConnection:";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return STRING_REPRESENTATION;
    }

    @Override
    public Configuration getConfiguration() {
        return config;
    }

    @Override
    public synchronized void initialize(Configuration config) {
        if (config != null) {
            if (isConnected) {
                destroy();
            }
            this.config = config;
            name = config.getProperty(ATTR_CONN_NAME);
            STRING_REPRESENTATION += name;
            initializeConnection(config);
            isConnected = true;
        } else {
            throw new NullPointerException(
                    "No configuration available for initialization.");
        }
    }

    protected abstract void initializeConnection(Configuration config);

    protected abstract void destroyConnection();

    @Override
    public synchronized void destroy() {
        isConnected = false;
        destroyConnection();
    }

}
