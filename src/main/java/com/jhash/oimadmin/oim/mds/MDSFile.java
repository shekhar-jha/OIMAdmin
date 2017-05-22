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

package com.jhash.oimadmin.oim.mds;

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import oracle.mds.lcm.client.TargetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class MDSFile {

    private static final Logger logger = LoggerFactory.getLogger(MDSFile.class);
    private final MDSConnectionJMX mdsConnectionJMX;
    private final MDSPartition partition;
    private final JarFile jarFile;
    private final JarEntry file;
    private final String stringValue;
    private String content = null;

    public MDSFile(MDSConnectionJMX mdsConnectionJMX, MDSPartition partition, JarFile jarFile, JarEntry file) {
        this.mdsConnectionJMX = mdsConnectionJMX;
        this.partition = partition;
        this.jarFile = jarFile;
        this.file = file;
        this.stringValue = partition + ":" + file;
    }

    public String getContent() {
        if (content == null) {
            content = Utils.readFileInJar(jarFile, file);
        }
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String toString() {
        return stringValue;
    }

    public void save() {
        if (file == null || file.isDirectory() || content == null) {
            logger.debug("Nothing to do since file {} has invalid jar entry or content", file);
        } else {
            String fileName = mdsConnectionJMX.generateFileName(false);
            try {
                try (JarOutputStream importFileOutputStream = new JarOutputStream(new FileOutputStream(fileName))) {
                    JarEntry newFileEntry = new JarEntry(file.getName());
                    newFileEntry.setTime(System.currentTimeMillis());
                    importFileOutputStream.putNextEntry(newFileEntry);
                    importFileOutputStream.write(content.getBytes());
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to create the jar file " + fileName, exception);
                }
                try {
                    mdsConnectionJMX.importMetaData(partition.application, new TargetInfo(
                            partition.serverName), fileName);
                } catch (Exception exception) {
                    throw new OIMAdminException("Failed to import file " + fileName + " into MDS repository "
                            + partition.application + " in location " + partition.serverName, exception);
                }
            } finally {
                if (fileName != null) {
                    try {
                        new File(fileName).delete();
                    } catch (Exception fileDeleteException) {
                        logger.warn("Failed to delete the file " + fileName + " after saving the MDS File ", fileDeleteException);
                    }
                }
            }
        }
    }

}
