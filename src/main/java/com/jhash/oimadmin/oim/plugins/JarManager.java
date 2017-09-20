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

package com.jhash.oimadmin.oim.plugins;

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import com.jhash.oimadmin.oim.DBConnection;
import com.jhash.oimadmin.oim.Details;
import com.jhash.oimadmin.oim.OIMConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JarManager {

    public static final String GET_ALL_JARS = "SELECT OJ_ID, OJ_TYPE, OJ_NAME FROM OIMHOME_JARS Order BY OJ_TYPE ASC";
    public static final String CREATE_JAR = "insert into OIMHOME_JARS(oj_id,oj_name,oj_type,oj_jar,created_on,created_by) values(OIMHOME_JARS_SEQ.NEXTVAL,?,?,?,?,?)";
    public static final String UPDATE_JAR = "update OIMHOME_JARS set oj_jar=?,updated_on=?,updated_by=? where oj_name=? and oj_type=?";
    public static final String DOWNLOAD_JAR = "select OJ_JAR from OIMHOME_JARS where oj_type=? and oj_name=?";
    private static final Logger logger = LoggerFactory.getLogger(JarManager.class);
    private final OIMConnection oimConnection;
    private final DBConnection dbConnection;

    public JarManager(OIMConnection oimConnection, DBConnection dbConnection) {
        logger.trace("Created Jar Manager with OIM Connection {} and DB Connection {}", new Object[]{oimConnection, dbConnection});
        this.oimConnection = oimConnection;
        this.dbConnection = dbConnection;
    }

    public Map<String, List<String>> getRegisteredJars() {
        logger.trace("Retrieving registered jars...");
        Map<String, List<String>> registeredJars = null;
        if (dbConnection != null) {
            logger.trace("Invoking SQL Query {}", GET_ALL_JARS);
            Details result = dbConnection.invokeSQL(GET_ALL_JARS);
            logger.trace("Retrieved SQL result as {}", result);
            registeredJars = new HashMap<>();
            if (result != null) {
                for (Map<String, Object> record : result) {
                    String type = (String) record.get("OJ_TYPE");
                    String name = (String) record.get("OJ_NAME");
                    if (!Utils.isEmpty(type) && !Utils.isEmpty(name)) {
                        if (!registeredJars.containsKey(type)) {
                            registeredJars.put(type, new ArrayList<String>());
                        }
                        registeredJars.get(type).add(name);
                    } else {
                        logger.warn("Invalid entry {}, {} was read while reading result for {}. Ignoring the entry.", new Object[]{type, name, GET_ALL_JARS});
                    }
                }
            } else {
                logger.debug("No result returned. Nothing to do.");
            }
        } else {
            logger.debug("No db connection available to retrieve Jar details.");
        }
        logger.trace("Retrieved registered jar as {}", registeredJars);
        return registeredJars;
    }

    public void registerJar(String type, File file) {
        logger.debug("Registering jar {} of type {}", file, type);
        try {
            if (isValid(type, file)) {
                byte[] jarBytes = readFile(file);
                dbConnection.invokeOperation(CREATE_JAR, file.getName(), type, jarBytes,
                        new java.sql.Date(Long.valueOf(new java.util.Date().getTime()).longValue()),
                        (int) oimConnection.getLoginUserIdentifier());
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to register jar " + file + " of type " + type, exception);
        }
        logger.debug("Registered jar.");
    }

    private boolean isValid(String type, File file) {
        if (Utils.isEmpty(type))
            return false;
        if (file == null) return false;
        if (!file.isFile())
            throw new OIMAdminException("Jar " + file + " is not a file.");
        if (!file.canRead())
            throw new OIMAdminException("Jar " + file + " can not be read.");
        return true;
    }

    private byte[] readFile(File file) {
        FileInputStream fileInputStream = null;
        byte[] jarBytes = null;
        try {
            fileInputStream = new FileInputStream(file);
            int byteCount = fileInputStream.available();
            jarBytes = new byte[byteCount];
            fileInputStream.read(jarBytes);
        } catch (Exception exception) {
            throw new OIMAdminException("Jar " + file + " could not be read.", exception);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Exception exception) {
                    logger.warn("Failed to close file input stream while reading jar " + file, exception);
                }
            }
        }
        return jarBytes;
    }

    public void updateJar(String type, String jarName, File file) {
        logger.debug("Updating jar {} of type {} and file {}", new Object[]{jarName, type, file});
        if (Utils.isEmpty(jarName)) {
            jarName = file.getName();
        }
        try {
            if (isValid(type, file)) {
                byte[] jarBytes = readFile(file);
                dbConnection.invokeOperation(UPDATE_JAR, jarBytes,
                        new java.sql.Date(Long.valueOf(new java.util.Date().getTime()).longValue()),
                        (int) oimConnection.getLoginUserIdentifier(), jarName, type
                );
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to update jar " + file + " of type " + type, exception);
        }
    }

    public void saveJar(String type, String jarName, File file) {
        logger.debug("Saving jar {} of type {} to file {}", new Object[]{jarName, type, file});
        if (Utils.isEmpty(type) || Utils.isEmpty(jarName) || file == null)
            return;
        FileOutputStream outputStream = null;
        try {
            if ((!file.exists() && file.createNewFile()) || (file.exists() && file.isFile() && file.canWrite())) {
                Details details = dbConnection.invokeSQL(DOWNLOAD_JAR, type, jarName);
                byte[] content;
                logger.debug("Details value {}", details.getItemAt(0));
                if (details != null && details.size() == 1
                        && (content = (byte[]) details.getItemAt(0, "OJ_JAR", null)) != null) {
                    outputStream = new FileOutputStream(file);
                    outputStream.write(content);
                } else {
                    throw new OIMAdminException("Failed to locate jar associated with jar " + jarName + " of type " + type);
                }
            } else {
                throw new OIMAdminException("File " + file + " is not valid or it could not be created/updated.");
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to save jar " + jarName + " of type " + type + " to " + file, exception);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception exception) {
                    logger.warn("Failed to close the file created to save downloaded jar " + jarName + " of type " + type + ". Ignoring the error.", exception);
                }
            }
        }
    }

    public void unregisterJar(String type, String name) {
        oimConnection.unregisterJar(type, name);
    }

    public void registerPlugin(byte[] pluginData) {
        oimConnection.registerPlugin(pluginData);
    }

    public void unregisterPlugin(String className) {
        oimConnection.unregisterPlugin(className);
    }

}
