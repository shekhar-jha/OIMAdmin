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

package com.jhash.oimadmin.oim.eventHandlers;

import com.jhash.oimadmin.oim.Details;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OperationDetail implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map<Manager, Map<String, Set<String>>> allowedOperations = new HashMap<Manager, Map<String, Set<String>>>();
    public final String name;
    public final String entity;
    public final String operation;
    public final String description;
    private Manager connection;

    public OperationDetail(String name, String description, Manager connection) {
        this.name = name;
        this.description = description;
        this.connection = connection;
        String[] nameSplits = name.split("-");
        if (nameSplits == null || nameSplits.length != 2) {
            throw new InvalidParameterException("The name " + name + " does not have two components with - inbetween");
        } else {
            entity = nameSplits[0];
            operation = nameSplits[1];
            Map<String, Set<String>> allowedEntityOperationsForConnection = null;
            if (allowedOperations.containsKey(connection)) {
                allowedEntityOperationsForConnection = allowedOperations.get(connection);
            } else {
                allowedEntityOperationsForConnection = new HashMap<String, Set<String>>();
                allowedOperations.put(connection, allowedEntityOperationsForConnection);
            }
            Set<String> allowedOperationsForEntityConnection = null;
            if (allowedEntityOperationsForConnection.containsKey(nameSplits[0])) {
                allowedOperationsForEntityConnection = allowedEntityOperationsForConnection.get(nameSplits[0]);
            } else {
                allowedOperationsForEntityConnection = new HashSet<String>();
                allowedEntityOperationsForConnection.put(nameSplits[0], allowedOperationsForEntityConnection);
            }
            allowedOperationsForEntityConnection.add(nameSplits[1]);
        }
    }

    public OperationDetail(String entity, String operation, String description, Manager connection) {
        this.entity = entity;
        this.operation = operation;
        this.name = entity + "-" + operation;
        this.description = description;
        this.connection = connection;
        Map<String, Set<String>> allowedEntityOperationsForConnection = null;
        if (allowedOperations.containsKey(connection)) {
            allowedEntityOperationsForConnection = allowedOperations.get(connection);
        } else {
            allowedEntityOperationsForConnection = new HashMap<String, Set<String>>();
            allowedOperations.put(connection, allowedEntityOperationsForConnection);
        }
        Set<String> allowedOperationsForEntityConnection = null;
        if (allowedEntityOperationsForConnection.containsKey(entity)) {
            allowedOperationsForEntityConnection = allowedEntityOperationsForConnection.get(entity);
        } else {
            allowedOperationsForEntityConnection = new HashSet<String>();
            allowedEntityOperationsForConnection.put(entity, allowedOperationsForEntityConnection);
        }
        allowedOperationsForEntityConnection.add(operation);
    }


    public static Map<String, Set<String>> getOperationDetails(Manager connection) {
        return allowedOperations.get(connection);
    }

    public Details getEventHandlers(OperationDetail operation) {
        return connection.getEventHandlers(operation);
    }
}
