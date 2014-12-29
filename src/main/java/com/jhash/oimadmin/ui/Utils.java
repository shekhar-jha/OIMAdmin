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

import com.jhash.oimadmin.OIMAdminException;
import org.apache.commons.io.FileUtils;

import javax.tools.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Utils {

    public static DiagnosticCollector<JavaFileObject> compileJava(String className, String code,
                                                                  String outputFileLocation) {
        File outputFileDirectory = new File(outputFileLocation);
        if (outputFileDirectory.exists() && outputFileDirectory.isDirectory()) {
            try {
                FileUtils.deleteDirectory(outputFileDirectory);
            } catch (IOException exception) {
                throw new OIMAdminException("Failed to delete directory " + outputFileLocation + " and its content",
                        exception);
            }
            outputFileDirectory.mkdirs();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<File> files = Arrays.asList(new File(outputFileLocation));
        boolean success = false;
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        try {
            JavaFileObject javaFileObject = new InMemoryJavaFileObject(className, code);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, files);

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                    Arrays.asList(new String[]{"-source", "1.6", "-target", "1.6"}), null,
                    Arrays.asList(javaFileObject));
            success = task.call();
            fileManager.close();
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to compile " + className, exception);
        }

        if (!success) {
            return diagnostics;
        } else {
            return null;
        }
    }

    public static String readFile(String fileName, String[][] replacements) {
        String readData = readFile(fileName);
        if (replacements != null && replacements.length > 0) {
            for (String[] replacement : replacements) {
                if (replacement.length == 2) {
                    readData = readData.replace("@@" + replacement[0] + "@@", replacement[1]);
                }
            }
        }
        return readData;
    }

    public static String readFile(String fileName) {
        InputStream templateStream = ClassLoader.getSystemResourceAsStream(fileName);
        byte[] readData = new byte[1000];
        StringBuilder templateString = new StringBuilder();
        try {
            int readDataSize;
            while ((readDataSize = templateStream.read(readData)) > 0) {
                templateString.append(new String(readData, 0, readDataSize, "UTF-8"));
            }
        } catch (IOException e1) {
            throw new OIMAdminException("Failed to read templates/EventHandlerConditional", e1);
        }
        return templateString.toString();
    }

    public static void generateJarFile(String directory, String jarFileName) {
        File jarDirectory = new File(directory);
        try (JarOutputStream jarFileOutputStream = new JarOutputStream(new FileOutputStream(jarFileName))) {
            for (Iterator<File> fileIterator = FileUtils.iterateFiles(jarDirectory, null, true); fileIterator.hasNext(); ) {
                File inputFile = fileIterator.next();
                String inputFileName = inputFile.getAbsolutePath();
                String directoryFileName = jarDirectory.getAbsolutePath();
                String relativeInputFileName = inputFileName.substring(directoryFileName.length() + 1);
                JarEntry newFileEntry = new JarEntry(relativeInputFileName);
                newFileEntry.setTime(System.currentTimeMillis());
                jarFileOutputStream.putNextEntry(newFileEntry);
                jarFileOutputStream.write(FileUtils.readFileToByteArray(inputFile));
            }
        } catch (Exception exception) {
            throw new OIMAdminException(
                    "Failed to create the jar file " + jarFileName + " from directory " + directory, exception);
        }
    }

    public static void extractJarFile(String directory, String jarFileName) {
        File baseDir = new File(directory);
        if (baseDir.exists()) {
            if (!baseDir.isDirectory()) {
                throw new InvalidParameterException("Destination directory " + directory + " to expand Jar file " + jarFileName + " is not a directory");
            }
            if (!baseDir.canWrite() || !baseDir.canWrite() || !baseDir.canExecute()) {
                throw new InvalidParameterException("Destination directory " + directory + " to expand Jar file " + jarFileName + " does not have rwx access for user");
            }
        } else {
            baseDir.mkdirs();
        }
        try (JarFile jar = new JarFile(jarFileName)) {
            Enumeration enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry file = (JarEntry) enumEntries.nextElement();
                File f = new File(directory + File.separator + file.getName());
                if (file.isDirectory()) { // if its a directory, create it
                    f.mkdirs();
                    continue;
                }
                try (java.io.InputStream is = jar.getInputStream(file); java.io.FileOutputStream fos = new java.io.FileOutputStream(f)) {
                    // get the input stream
                    while (is.available() > 0) {  // write contents of 'is' to 'fos'
                        fos.write(is.read());
                    }
                    fos.close();
                    is.close();
                } catch (Exception exception) {
                    throw new OIMAdminException(
                            "Failed to write the jar file entry " + file + " to location " + f, exception);
                }
            }
        } catch (Exception exception) {
            throw new OIMAdminException(
                    "Failed to extract jar file " + jarFileName + " to directory " + directory, exception);
        }
    }

    public static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private String contents = null;

        public InMemoryJavaFileObject(String className, String contents) throws Exception {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.contents = contents;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return contents;
        }
    }

}
