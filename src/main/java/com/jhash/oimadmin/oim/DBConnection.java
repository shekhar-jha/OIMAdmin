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

package com.jhash.oimadmin.oim;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBConnection extends AbstractConnection {

    public static final String ATTR_DB_JDBC = "db_jdbc";
    public static final String ATTR_DB_URL = "db_url";
    public static final String ATTR_DB_USER = "db_user";
    public static final String ATTR_DB_PWD = "db_pwd";
    public static final String ATTR_DB_AUTOCOMMIT = "db_auto_commit";
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private Connection dbConnection;

    public DBConnection() {
        STRING_REPRESENTATION = "DatabaseConnection:";
    }

    @Override
    protected void initializeConnection(Config.Configuration configuration) {
        logger.debug("Trying to initialize Database using {}", configuration);
        STRING_REPRESENTATION += configuration.getProperty(ATTR_CONN_NAME, "Unknown");
        String jdbcDriver;
        if ((jdbcDriver = configuration.getProperty(ATTR_DB_JDBC)) != null) {
            try {
                Class.forName(jdbcDriver);
            } catch (Exception error) {
                throw new OIMAdminException("Failed to load JDBC Driver " + jdbcDriver + " for database connection " + this, error);
            }
        } else {
            throw new NullPointerException("Could not locate attribute " + ATTR_DB_JDBC + " in configuration for database connection " + this);
        }
        if (Utils.isEmpty(configuration.getProperty(ATTR_DB_URL)))
            throw new NullPointerException("Could not locate attribute " + ATTR_DB_URL + " in configuration for database connection " + this);
        if (Utils.isEmpty(configuration.getProperty(ATTR_DB_USER)))
            throw new NullPointerException("Could not locate attribute " + ATTR_DB_USER + " in configuration for database connection " + this);
        if (Utils.isEmpty(configuration.getProperty(ATTR_DB_PWD)))
            throw new NullPointerException("Could not locate attribute " + ATTR_DB_PWD + " in configuration for database connection " + this);
        try {
            dbConnection = DriverManager.getConnection(configuration.getProperty(ATTR_DB_URL), configuration.getProperty(ATTR_DB_USER), configuration.getProperty(ATTR_DB_PWD));
            STRING_REPRESENTATION += "{" + configuration.getProperty(ATTR_DB_URL) + "}";
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to connect to database " + this, exception);
        }
        String autoCommitValue;
        if (!Utils.isEmpty(autoCommitValue = configuration.getProperty(ATTR_DB_AUTOCOMMIT))) {
            try {
                dbConnection.setAutoCommit(Boolean.getBoolean(autoCommitValue));
            } catch (Exception exception) {
                logger.warn("Failed to set autocommit to " + autoCommitValue, exception);
            }
        }
        logger.debug("Initialized Database.");
    }

    public Details invokeSQL(String sqlID, Object... parameterValues) {
        return invokeSQL(false, false, sqlID, parameterValues);
    }

    public Details invokeSQL(boolean suppressException, String sqlID, Object... parameterValues) {
        return invokeSQL(suppressException, false, sqlID, parameterValues);
    }

    public void invokeOperation(String sqlID, Object... parameterValues) {
        invokeSQL(false, true, sqlID, parameterValues);
    }

    public void invokeOperation(boolean suppressException, String sqlID, Object... parameterValues) {
        invokeSQL(suppressException, true, sqlID, parameterValues);
    }

    public Details invokeSQL(boolean suppressException, boolean operation, String sqlID, Object... parameterValues) {
        logger.trace("Trying to invoke SQL {} with parameters {}", sqlID, parameterValues);
        try (PreparedStatement preparedStatement = operation ? dbConnection.prepareStatement(sqlID, PreparedStatement.RETURN_GENERATED_KEYS) : dbConnection.prepareStatement(sqlID)) {
            ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
            if (parameterValues != null && parameterMetaData.getParameterCount() != parameterValues.length) {
                logger.warn("The number of values {} do not match number of parameters {} of SQL Statement {}", new Object[]{parameterValues.length, parameterMetaData.getParameterCount(), sqlID});
            }
            int parameterIndexCounter = 1;
            for (Object parameterValue : parameterValues) {
                preparedStatement.setObject(parameterIndexCounter, parameterValue);
                parameterIndexCounter++;
            }
            try {
                if (operation) {
                    int affectedRows = preparedStatement.executeUpdate();
                    logger.debug("Affected rows of executed prepared statement is {}", affectedRows);
                    //TODO: handle generated keys preparedStatement.getGeneratedKeys();
                    dbConnection.commit();
                } else {
                    ResultSet result = preparedStatement.executeQuery();
                    ResultSetMetaData resultSetMetaData = preparedStatement.getMetaData();
                    int totalColumnsInResult = resultSetMetaData.getColumnCount();
                    List<String> columnNames = new ArrayList<>();
                    for (int columnCounter = 1; columnCounter <= totalColumnsInResult; columnCounter++) {
                        columnNames.add(resultSetMetaData.getColumnName(columnCounter));
                    }
                    List<Map<String, Object>> resultData = new ArrayList<>();
                    while (result.next()) {
                        Map<String, Object> record = new HashMap<>();
                        for (int columnCounter = 1; columnCounter <= totalColumnsInResult; columnCounter++) {
                            String columnName = resultSetMetaData.getColumnLabel(columnCounter);
                            Object columnValue = result.getObject(columnCounter);
                            if (columnValue instanceof Blob) {
                                byte[] blobBytes = ((Blob)columnValue).getBytes(1L, (int)((Blob)columnValue).length());
                                record.put(columnName, blobBytes);
                            } else {
                                record.put(columnName, columnValue);
                            }
                        }
                        resultData.add(record);
                    }
                    return new Details(resultData, columnNames.toArray(new String[0]));
                }
            } catch (Exception exception) {
                if (suppressException)
                    logger.warn("Failed to read result for SQL " + sqlID, exception);
                else
                    throw new OIMAdminException("Failed to read result", exception);
            }
        } catch (Exception exception) {
            if (suppressException)
                logger.warn("Failed to execute SQL " + sqlID + " with parameter " + parameterValues, exception);
            else
                throw new OIMAdminException("Failed to execute SQL " + sqlID + " with parameter " + parameterValues, exception);
        }
        return null;
    }

    @Override
    protected void destroyConnection() {
        logger.debug("Trying to destroy connection {}", this);
        if (dbConnection != null) {
            try {
                dbConnection.commit();
            } catch (Exception exception) {
                logger.warn("Failed to commit transaction in database " + this + ". Ignoring the same.", exception);
            }
            try {
                dbConnection.close();
            } catch (Exception exception) {
                logger.warn("Failed to close the connection " + dbConnection + " to database " + this + ". Ignoring the same", exception);
            }
        }
        logger.debug("Destroyed connection {}", this);
    }
}
