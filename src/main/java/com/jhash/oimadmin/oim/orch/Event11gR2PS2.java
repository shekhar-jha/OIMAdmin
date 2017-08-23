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

package com.jhash.oimadmin.oim.orch;

import com.jhash.oimadmin.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

public class Event11gR2PS2 {

    private static final Logger logger = LoggerFactory.getLogger(Event11gR2PS2.class);
    private final Object[] eventDetails;
    private final Object data;

    public Event11gR2PS2(Object[] eventDetails, ClassLoader classLoader) {
        this.eventDetails = eventDetails;
        Object data = eventDetails[9];
        if (eventDetails[9] instanceof byte[]) {
            try {
                data = Utils.getObjectInputStream(new ByteArrayInputStream((byte[]) eventDetails[9]), classLoader).readObject();
            } catch (Exception exception) {
                logger.warn("Failed to extract result from the Event details", exception);
                data = eventDetails[9];
            }

        }
        this.data = data;
    }

    public Object getID() {
        return eventDetails[0];
    }

    public Object getName() {
        return eventDetails[1];
    }

    public Object getStatus() {
        return eventDetails[2];
    }

    public Object getStage() {
        return eventDetails[4];
    }

    public Object getRetry() {
        return eventDetails[6];
    }

    public Object getStartTime() {
        return eventDetails[10];
    }

    public Object getEndTime() {
        return eventDetails[11];
    }

    public Object getErrorCode() {
        return eventDetails[7];
    }

    public Object getErrorMessage() {
        return eventDetails[8];
    }

    public Object getResult() {
        return data;
    }
}
