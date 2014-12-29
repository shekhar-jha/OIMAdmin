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

import oracle.mds.config.MDSConfig;
import oracle.mds.config.PConfig;
import oracle.mds.core.MDSInstance;
import oracle.mds.core.MDSSession;
import oracle.mds.naming.DocumentName;
import oracle.mds.naming.PackageName;
import oracle.mds.naming.ResourceName;
import oracle.mds.persistence.*;
import oracle.mds.persistence.stores.db.DBMetadataStore;
import oracle.mds.persistence.stores.file.FileMetadataStore;
import oracle.mds.query.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDSConnectionDB {
    private final static String MDS_CONNECTION_NAME = "ATeamMDSConnection";
    private static MDSInstance instance = null;
    private static String userName = "TEST_MDS";
    private static String password = "welcome1";
    private static String partition = "soa-infra";
    private static String dbUrl = "jdbc:oracle:thin:@ateam-hq53.us.oracle.com:1521:XE";

    public static List<ResourceName> queryMDS(MDSInstance mdsInstance, PackageName packageName) throws Exception {

        if (packageName == null)
            packageName = PackageName.createPackageName("/");

        // Use Query APIs to enumerate the subpackages or documents
        NameCondition condition = ConditionFactory.createNameCondition(packageName.getAbsoluteName(), "%"); //$NON-NLS-1$
        ResourceQuery query = QueryFactory.createResourceQuery(mdsInstance, condition);
        Iterator<QueryResult> contents = query.execute();

        List<ResourceName> resources = new ArrayList<ResourceName>();
        if (contents == null)
            return resources;

        QueryResult result;
        while (contents.hasNext()) {
            result = contents.next();
            if (result.getResultType() == QueryResult.ResultType.PACKAGE_RESULT) {
                PackageResult pack = (PackageResult) result;
                resources.add(pack.getPackageName());
            } else {
                DocumentResult doc = (DocumentResult) result;
                resources.add(doc.getDocumentName());
            }
        }

        return resources;
    }

    public static void createFolder(MDSInstance mdsInstance, PackageName packageName) throws Exception {
        MDSSession session = mdsInstance.createSession(null, null);
        PTransaction transaction = session.getPTransaction();
        transaction.createPackage(packageName);
        session.flushChanges();
    }

    public static void deleteResource(MDSInstance mdsInstance, ResourceName resource) throws Exception {
        MDSSession session = mdsInstance.createSession(null, null);
        PTransaction transaction = session.getPTransaction();
        PManager pManager = mdsInstance.getPersistenceManager();
        PContext pContext = session.getPContext();

        if (resource instanceof PackageName) {
            PPackage ppackage = pManager.getPackage(pContext, (PackageName) resource);
            transaction.deletePackage(ppackage, true);
        } else {
            PDocument pdocument = pManager.getDocument(pContext, (DocumentName) resource);
            transaction.deleteDocument(pdocument, true);
        }
        session.flushChanges();
    }

    private static MDSInstance initializeFileStore(String filePath, String partitionName, String connName)
            throws Exception {
        // Updatable true so that the store can import new stuff
        FileMetadataStore store = new FileMetadataStore(filePath, partitionName);
        PConfig pConfig = new PConfig(store);
        MDSConfig config = new MDSConfig(null, pConfig, null);
        MDSInstance.releaseInstance(connName);
        return MDSInstance.getOrCreateInstance(connName, config);
    }

    private static MDSInstance initializeDBStore(String username, String password, String dbURL, String partitionName,
                                                 String connName) throws Exception {
        DBMetadataStore store = new DBMetadataStore(username, password, dbURL, partitionName);
        PConfig pConfig = new PConfig(store);
        MDSConfig config = new MDSConfig(null, pConfig, null);
        MDSInstance.releaseInstance(connName);
        return MDSInstance.getOrCreateInstance(connName, config);
    }

    private static void recurse(MDSInstance instance, String pName, int level) throws Exception {
        PackageName packageName = null;
        if (pName != null) {
            // System.out.println("- Recursing on " + pName);
            packageName = PackageName.createPackageName(pName);
        }
        List<ResourceName> list = MDSConnectionDB.queryMDS(instance, packageName);
        for (ResourceName rn : list) {
            System.out.println(lPad("+-", " ", level) + rn.getLocalName());
            if (rn.isPackageName())
                recurse(instance, rn.getAbsoluteName(), level + 2);
        }
    }

    public static List<ResourceName> findResource(MDSInstance mdsInstance, String regExpr, boolean stopAtFirstMatch)
            throws Exception {
        List<ResourceName> rnList = new ArrayList<ResourceName>();
        Pattern pattern = Pattern.compile(regExpr);
        findRecursively(mdsInstance, pattern, null, rnList, stopAtFirstMatch);

        return rnList;
    }

    private static void findRecursively(MDSInstance instance, Pattern pattern, String pName, List<ResourceName> rnList,
                                        boolean stopWhenFound) throws Exception {
        PackageName packageName = null;
        if (pName != null) {
            // System.out.println("- Recursing on " + pName);
            packageName = PackageName.createPackageName(pName);
        }
        List<ResourceName> list = MDSConnectionDB.queryMDS(instance, packageName);
        for (ResourceName rn : list) {
            Matcher matcher = pattern.matcher(rn.getAbsoluteName());
            boolean matchFound = matcher.find();
            if (matchFound) {
                rnList.add(rn);
                if (stopWhenFound)
                    break;
            }
            if (rn.isPackageName())
                findRecursively(instance, pattern, rn.getAbsoluteName(), rnList, stopWhenFound);
        }
    }

    private static String lPad(String s, String p, int nb) {
        String pad = "";
        for (int i = 0; i < nb; i++)
            pad += p;
        return pad + s;
    }

    public static void setUserName(String userName) {
        MDSConnectionDB.userName = userName;
    }

    public static void setPassword(String password) {
        MDSConnectionDB.password = password;
    }

    public static void setPartition(String partition) {
        MDSConnectionDB.partition = partition;
    }

    public static void setDbUrl(String dbUrl) {
        MDSConnectionDB.dbUrl = dbUrl;
    }

    private final static synchronized MDSInstance getMDSInstance() throws Exception {
        if (instance == null) {
            instance = MDSConnectionDB.initializeDBStore(userName, password, dbUrl, partition, MDS_CONNECTION_NAME);
        }
        return instance;
    }

    public static void work() {
        try {
            MDSInstance mdsInstance = null;
            if (true) {
                mdsInstance = MDSConnectionDB.initializeDBStore("OIMDEV_MDS", "Oia3$Dtc3",
                        "jdbc:oracle:thin:@10.106.181.66:1467:OIAMD", "oim", MDS_CONNECTION_NAME);
            } else {
                mdsInstance = MDSConnectionDB.initializeFileStore("~/mds",
                        "soa-infra", MDS_CONNECTION_NAME);
            }
            // Recurse and display, from the root.
            if (true)
                recurse(mdsInstance, null, 0);

            // Find a resource
            if (false) {
                List<ResourceName> list = MDSConnectionDB.findResource(mdsInstance, "deployed-composites", false);
                System.out.println("List: (" + list.size() + " element(s))");
                for (ResourceName rn : list)
                    System.out.println("Found : " + rn.getAbsoluteName() + " (a "
                            + (rn.isPackageName() ? "package" : "document") + ")");
            }

            if (false)
                MDSConnectionDB.createFolder(mdsInstance, PackageName.createPackageName("/oliv"));

            if (false) {
                // Deleting folder
                List<ResourceName> list = MDSConnectionDB.findResource(mdsInstance, "oliv", false);
                System.out.println("List: (" + list.size() + " element(s))");
                for (ResourceName rn : list) {
                    System.out.println("Found : " + rn.getAbsoluteName() + " (a "
                            + (rn.isPackageName() ? "package" : "document") + ")");
                    if (rn.isPackageName() && rn.getAbsoluteName().equals("/oliv")) {
                        System.out.println("Deleting " + rn.getAbsoluteName());
                        MDSConnectionDB.deleteResource(mdsInstance, rn);
                    } else
                        System.out.println("Leaving " + rn.getAbsoluteName() + " alone.");
                }
            }
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        work();
    }

    public void createFolder(String folderName) throws Exception {
        try {
            System.out.println("Creating folder " + folderName);
            MDSConnectionDB.createFolder(getMDSInstance(), PackageName.createPackageName(folderName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFolder(String folderName) {
        try {
            List<ResourceName> list = MDSConnectionDB.findResource(getMDSInstance(), folderName, false);
            System.out.println("List: (" + list.size() + " element(s))");
            for (ResourceName rn : list) {
                System.out.println("Found : " + rn.getAbsoluteName() + " (a "
                        + (rn.isPackageName() ? "package" : "document") + ")");
                if (rn.isPackageName() && rn.getAbsoluteName().equals(folderName)) {
                    System.out.println("Deleting " + rn.getAbsoluteName());
                    MDSConnectionDB.deleteResource(getMDSInstance(), rn);
                } else
                    System.out.println("Leaving " + rn.getAbsoluteName() + " alone.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
