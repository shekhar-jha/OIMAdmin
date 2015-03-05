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
package com.jhash.oimadmin;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipOutputStream;

public class Utils {

    public final static ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static DiagnosticCollector<JavaFileObject> compileJava(String className, String code,
                                                                  String outputFileLocation) {
        logger.trace("Entering compileJava({},{},{})", new Object[]{className, code, outputFileLocation});
        File outputFileDirectory = new File(outputFileLocation);
        logger.trace("Validating if the output location {} exists and is a directory", outputFileLocation);
        if (outputFileDirectory.exists()) {
            if (outputFileDirectory.isDirectory()) {
                try {
                    logger.trace("Deleting the directory and its content");
                    FileUtils.deleteDirectory(outputFileDirectory);
                } catch (IOException exception) {
                    throw new OIMAdminException("Failed to delete directory " + outputFileLocation + " and its content",
                            exception);
                }
            } else {
                throw new InvalidParameterException("The location " + outputFileLocation + " was expected to be a directory but it is a file.");
            }
        }
        logger.trace("Creating destination directory for compiled class file");
        outputFileDirectory.mkdirs();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new NullPointerException("Failed to locate a java compiler. Please ensure that application is being run using JDK (Java Development Kit) and NOT JRE (Java Runtime Environment) ");
        }
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<File> files = Arrays.asList(new File(outputFileLocation));
        boolean success = false;
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        try {
            JavaFileObject javaFileObject = new InMemoryJavaFileObject(className, code);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, files);

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                    Arrays.asList("-source", "1.6", "-target", "1.6"), null,
                    Arrays.asList(javaFileObject));
            success = task.call();
            fileManager.close();
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to compile " + className, exception);
        }

        if (!success) {
            logger.trace("Exiting compileJava(): Return Value {}", diagnostics);
            return diagnostics;
        } else {
            logger.trace("Exiting compileJava(): Return Value null");
            return null;
        }
    }

    public static String processString(String content, String[][] replacements) {
        if (replacements != null && replacements.length > 0) {
            for (String[] replacement : replacements) {
                if (replacement.length == 2) {
                    content = content.replace("@@" + replacement[0] + "@@", replacement[1]);
                }
            }
        }
        return content;
    }

    public static String readFile(String fileName, String workArea) {
        logger.trace("readFile({},{})", fileName, workArea);
        InputStream templateStream = null;
        File workAreaDirectory;
        logger.trace("Trying to validate whether workarea {} exists and is a directory", workArea);
        if (workArea != null && (workAreaDirectory = new File(workArea)) != null && workAreaDirectory.exists() && workAreaDirectory.isDirectory()) {
            File templateFile = new File(workAreaDirectory + File.separator + fileName);
            logger.trace("Trying to validate if file {} exists", templateFile);
            if (templateFile.exists() && templateFile.canRead()) {
                try {
                    logger.trace("Trying to setup {} for reading", templateFile);
                    templateStream = new FileInputStream(templateFile);
                    logger.trace("File can be read using {}", templateStream);
                } catch (IOException exception) {
                    throw new OIMAdminException("Could not read " + templateFile.getAbsolutePath(), exception);
                }
            }
        }
        if (templateStream == null) {
            logger.trace("Trying to setup {} for reading from system classpath ", fileName);
            templateStream = ClassLoader.getSystemResourceAsStream(fileName);
        }
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

    public static String readFile(String fileName) {
        return readFile(fileName, null);
    }

    public static void createJarFileFromDirectory(String directory, String jarFileName) {
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

    public static void createJarFileFromContent(Map<String, byte[]> content, String[] fileSequence, String jarFileName) {
        logger.trace("createJarFileFromContent({},{})", content, jarFileName);
        logger.trace("Trying to create a new jar file");
        try (ZipOutputStream jarFileOutputStream = new ZipOutputStream(new FileOutputStream(jarFileName))) {
            jarFileOutputStream.setMethod(ZipOutputStream.STORED);
            for (String jarItem : fileSequence) {
                logger.trace("Processing item {}", jarItem);
                byte[] fileContent = content.get(jarItem);
                if (fileContent == null)
                    throw new NullPointerException("Failed to locate content for file " + jarItem);
                JarEntry pluginXMLFileEntry = new JarEntry(jarItem);
                pluginXMLFileEntry.setTime(System.currentTimeMillis());
                pluginXMLFileEntry.setSize(fileContent.length);
                pluginXMLFileEntry.setCompressedSize(fileContent.length);
                CRC32 crc = new CRC32();
                crc.update(fileContent);
                pluginXMLFileEntry.setCrc(crc.getValue());
                jarFileOutputStream.putNextEntry(pluginXMLFileEntry);
                jarFileOutputStream.write(fileContent);
                jarFileOutputStream.closeEntry();
            }
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to create the Jar file " + jarFileName, exception);
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

    public static void processJarFile(String jarFileName, JarFileProcessor processor) {
        try {
            JarFile exportedFile = new JarFile(jarFileName);
            for (Enumeration<JarEntry> jarEntryEnumeration = exportedFile.entries(); jarEntryEnumeration
                    .hasMoreElements(); ) {
                JarEntry jarEntry = jarEntryEnumeration.nextElement();
                logger.debug("Trying to process entry {} ", jarEntry);
                processor.process(exportedFile, jarEntry);
            }
            logger.debug("Processed all the jar entries");
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to open the jar file " + jarFileName, exception);
        }
    }

    public static String readFileInJar(JarFile jarFile, JarEntry file) {
        StringBuilder readFileData = new StringBuilder();
        try (BufferedReader jarFileInputStream = new BufferedReader(new InputStreamReader(
                jarFile.getInputStream(file)))) {
            String readLine;
            while ((readLine = jarFileInputStream.readLine()) != null) {
                readFileData.append(readLine);
                readFileData.append(System.lineSeparator());
            }
        } catch (Exception exception) {
            throw new OIMAdminException(
                    "Failed to read file " + file + " in jar file " + jarFile.getName(), exception);
        }
        return readFileData.toString();
    }

    public static void executeAsyncOperation(String operationName, Runnable operation) {
        logger.debug("Setting up execution of {} in separate thread", operationName);
        Thread oimConnectionThread = threadFactory.newThread(new Runnable() {

            @Override
            public void run() {
                try {
                    logger.debug("Trying to run operation {}", operationName);
                    operation.run();
                    logger.debug("Completed operation {}.", operationName);
                } catch (Exception exception) {
                    logger.warn("Failed to run operation " + operationName, exception);
                }
            }
        });
        oimConnectionThread.setDaemon(true);
        oimConnectionThread.setName(operationName);
        oimConnectionThread.start();
        logger.debug("Completed setup of execution of {} in separate thread", operationName);
    }

    public static boolean isEmpty(String value) {
        if (value == null || value.isEmpty())
            return true;
        else
            return false;
    }

    public static String toString(Object objectValue) {
        if (objectValue != null) {
            return objectValue.toString();
        } else {
            return "";
        }
    }

    @FunctionalInterface
    public static interface JarFileProcessor {

        public void process(JarFile jarFile, JarEntry file);

    }

    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
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
