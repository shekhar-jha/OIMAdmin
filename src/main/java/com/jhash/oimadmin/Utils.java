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

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipOutputStream;

public class Utils {

    public final static ThreadFactory threadFactory = Executors.defaultThreadFactory();
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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
            logger.trace("Trying to read file as {}", fileName);
            File templateFile = new File(fileName);
            if (templateFile.exists() && templateFile.canRead()) {
                logger.trace("Trying to setup {} for reading", templateFile);
                try {
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
        } finally {
            if (templateStream != null) {
                try {
                    templateStream.close();
                } catch (Exception exception) {
                    logger.warn("Failed to close stream for file " + fileName + " in work area " + workArea, exception);
                }
            }
        }
        return templateString.toString();
    }

    public static String readFile(String fileName) {
        return readFile(fileName, null);
    }

    public static void createJarFileFromDirectory(String directory, String jarFileName) {
        logger.debug("Creating Jar file {} from directory {}", new Object[]{jarFileName, directory});
        File jarDirectory = new File(directory);
        try (JarOutputStream jarFileOutputStream = new JarOutputStream(new FileOutputStream(jarFileName))) {
            for (Iterator<File> fileIterator = FileUtils.iterateFiles(jarDirectory, null, true); fileIterator.hasNext(); ) {
                File inputFile = fileIterator.next();
                String inputFileName = inputFile.getAbsolutePath();
                logger.trace("Processing file {}", inputFileName);
                String directoryFileName = jarDirectory.getAbsolutePath();
                String relativeInputFileName = inputFileName.substring(directoryFileName.length() + 1);
                logger.trace("Identified entry name as {}", relativeInputFileName);
                if (!File.separator.equalsIgnoreCase("/")) {
                    relativeInputFileName = relativeInputFileName.replace(File.separator, "/");
                    logger.trace("Replaced the {} with {}. Updated jar entry is  {}", new Object[]{File.separator, "/", relativeInputFileName});
                }
                JarEntry newFileEntry = new JarEntry(relativeInputFileName);
                newFileEntry.setTime(System.currentTimeMillis());
                jarFileOutputStream.putNextEntry(newFileEntry);
                logger.trace("Extracting file content from {}", inputFile);
                byte[] content = FileUtils.readFileToByteArray(inputFile);
                logger.trace("Writing content of size {} to jar", content == null ? "null" : content.length);
                jarFileOutputStream.write(content);
                jarFileOutputStream.closeEntry();
            }
            jarFileOutputStream.flush();
        } catch (Exception exception) {
            throw new OIMAdminException(
                    "Failed to create the jar file " + jarFileName + " from directory " + directory, exception);
        }
        logger.debug("Created Jar file.");
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

    public static Map<JarEntryKey, Object> createJarTree(String jarFileName) {
        logger.debug("Creating jar tree for file {}", jarFileName);
        JarFile exportedFile = null;
        try {
            exportedFile = new JarFile(jarFileName);
            Map<JarEntryKey, Object> jarEntryTreeMap = new TreeMap<>(CASE_INSENSITIVE_COMPARATOR);
            for (Enumeration<JarEntry> jarEntryEnumeration = exportedFile.entries(); jarEntryEnumeration
                    .hasMoreElements(); ) {
                JarEntry jarEntry = jarEntryEnumeration.nextElement();
                String filePath = jarEntry.getName();
                logger.debug("Trying to process entry {} with path {}", jarEntry, filePath);
                String[] filePathComponents = filePath.split("/");
                logger.debug("Split the file name {} as {}", filePath, filePathComponents);
                Map<JarEntryKey, Object> applicableJarEntryTreeMap = jarEntryTreeMap;
                StringBuilder filePathIdentifier = new StringBuilder();
                for (int depth = 0; depth < filePathComponents.length - 1; depth++) {
                    filePathIdentifier.append(filePathComponents[depth]);
                    JarEntryKey jarEntryKey = new JarEntryKey(filePathComponents[depth], filePathIdentifier.toString());
                    logger.trace("Processing file path {}", filePathIdentifier.toString());
                    Object directoryAtDepth = applicableJarEntryTreeMap.get(jarEntryKey);
                    if (directoryAtDepth == null) {
                        directoryAtDepth = new TreeMap<>(Utils.CASE_INSENSITIVE_COMPARATOR);
                        logger.trace("Creating Jar Entry tree Map for {}", jarEntryKey);
                        applicableJarEntryTreeMap.put(jarEntryKey, directoryAtDepth);
                        applicableJarEntryTreeMap = (Map) directoryAtDepth;
                    } else if (directoryAtDepth instanceof Map) {
                        applicableJarEntryTreeMap = (Map) directoryAtDepth;
                    } else {
                        throw new OIMAdminException("Expected the entry " + filePath + " to contain " + filePathComponents[depth] + " as directory (represented by Map object) but found it to be " + directoryAtDepth + " of type " + directoryAtDepth.getClass());
                    }
                    filePathIdentifier.append("/");
                }
                JarEntryKey jarEntryKey = new JarEntryKey(filePathComponents[filePathComponents.length - 1], jarEntry.getName());
                if (applicableJarEntryTreeMap == null)
                    throw new OIMAdminException("Failed to locate application parent directory corresponding to path " + filePath);
                if (jarEntry.isDirectory()) {
                    logger.trace("Creating Jar Entry tree Map for {}", jarEntryKey);
                    applicableJarEntryTreeMap.put(jarEntryKey, new HashMap<>());
                } else {
                    logger.trace("Creating Jar Entry tree Map for {}", jarEntry);
                    applicableJarEntryTreeMap.put(jarEntryKey, jarEntry);
                }
            }
            logger.debug("Created Jar tree entry as {}", jarEntryTreeMap);
            return jarEntryTreeMap;
        } catch (OIMAdminException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to open the jar file " + jarFileName, exception);
        } finally {
            if (exportedFile != null) {
                try {
                    exportedFile.close();
                } catch (Exception exception) {
                    logger.warn("Failed to close jar file " + jarFileName + " while generating Jar tree map.", exception);
                }
            }
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

    public static void executeAsyncOperation(final String operationName, final Runnable operation) {
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
        return value == null || value.isEmpty();
    }

    public static String toString(Object objectValue) {
        if (objectValue != null) {
            return objectValue.toString();
        } else {
            return "";
        }
    }

    public static File getDirectoryContainingJarForClass(String className) {
        logger.debug("Locating directory containing jar file with class {}", className);
        try {
            String oimClientURL = Class.forName(className).getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File oimClientJar = new File(oimClientURL);
            if (oimClientJar.exists()) {
                File parentDirectory = oimClientJar.getParentFile();
                if (parentDirectory.exists() && parentDirectory.isDirectory()) {
                    return parentDirectory;
                }
                throw new FileNotFoundException("Failed to located directory " + parentDirectory);
            }
            throw new FileNotFoundException("Failed to located jar file " + oimClientURL);
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to locate directory of jar containing class" + className, exception);
        }
    }

    public static ObjectInputStream getObjectInputStream(InputStream inputStream, URL... jars) throws IOException {
        logger.debug("Creating new Input Stream with input stream {}, URLS {}", inputStream, jars);
        if (jars != null) {
            try {
                final ClassLoader urlClassLoader = new URLClassLoader(jars, null);
                return getObjectInputStream(inputStream, urlClassLoader);
            } catch (Exception exception) {
                logger.warn("Failed to create Class Loader with URL " + jars + ". Returning standard object input stream with input stream " + inputStream, exception);
                return new ObjectInputStream(inputStream);
            }
        } else {
            logger.debug("Returning standard object input stream since no additional urls were provided.");
            return new ObjectInputStream(inputStream);
        }
    }

    public static ObjectInputStream getObjectInputStream(InputStream inputStream, final ClassLoader classLoader) throws IOException {
        return new ObjectInputStream(inputStream) {

            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                logger.debug("Trying to resolve class using description {}", desc);
                String name = null;
                try {
                    name = desc.getName();
                    logger.debug("Trying to load class {} using URL Class Loader {}", name, classLoader);
                    Class<?> classValue = classLoader.loadClass(name);
                    logger.debug("Located class as {}", classValue);
                    return classValue;
                } catch (ClassNotFoundException ex) {
                    logger.debug("Failed to locate Class " + name + ". Invoking parent method.", ex);
                    return super.resolveClass(desc);
                }
            }
        };
    }

    public static <V> V getOrDefault(Map<?, V> map, Object key, V defaultValue) {
        if (map == null)
            return defaultValue;
        V v;
        return (((v = map.get(key)) != null) || map.containsKey(key)) ? v : defaultValue;
    }

    public static Object invoke(Object object, String methodName) {
        return invoke(object, methodName, null);
    }

    public static Method getMethod(Class<?> processClass, String methodName, Class... parameterTypes) {
        if (processClass == null || Utils.isEmpty(methodName))
            return null;
        try {
            return processClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {

        }
        Class applicableClass = processClass;
        while (applicableClass != null) {
            try {
                Method method = applicableClass.getDeclaredMethod(methodName, parameterTypes);
                if (method != null) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (NoSuchMethodException exception) {

            }
            applicableClass = applicableClass.getSuperclass();
        }
        return null;
    }

    public static <T> T invoke(Object object, String methodName, T defaultValue) {
        if (object == null || Utils.isEmpty(methodName))
            return defaultValue;
        Class applicableClass = object.getClass();
        Method method = getMethod(applicableClass, methodName);
        try {
            return (T) method.invoke(object);
        } catch (Exception exception) {
            logger.warn("Failed to invoke " + methodName + " on object of type " + object.getClass(), exception);
            return defaultValue;
        }
    }

    public static String generateStringRepresentation(Throwable throwable) {
        StringWriter output = new StringWriter();
        throwable.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    public static String extractExceptionDetails(Exception exception) {
        return extractExceptionDetails(exception, null).toString();
    }

    public static StringBuilder extractExceptionDetails(Exception exception, StringBuilder messageBuilder) {
        StringBuilder messageDetails = messageBuilder == null ? new StringBuilder() : messageBuilder;
        if (exception != null) {
            Throwable applicableException = exception;
            while (applicableException != null) {
                if (applicableException != exception) {
                    messageDetails.append(System.lineSeparator());
                    messageDetails.append("Caused By: ");
                }
                messageDetails.append(applicableException);
                messageDetails.append(System.lineSeparator());
                messageDetails.append(applicableException.getStackTrace()[0].toString());
                for (int stackTraceDepth = 1; stackTraceDepth < applicableException.getStackTrace().length; stackTraceDepth++) {
                    if (applicableException.getStackTrace()[stackTraceDepth].getClassName().startsWith("com.jhash.oimadmin")) {
                        messageDetails.append(System.lineSeparator());
                        messageDetails.append("...");
                        messageDetails.append(System.lineSeparator());
                        messageDetails.append(applicableException.getStackTrace()[stackTraceDepth].toString());
                        break;
                    }
                }
                applicableException = applicableException.getCause();
            }
        }
        return messageDetails;
    }

    public interface JarFileProcessor {

        void process(JarFile jarFile, JarEntry file);

    }

    public static final Comparator<JarEntryKey> CASE_INSENSITIVE_COMPARATOR = new Comparator<JarEntryKey>() {
        @Override
        public int compare(JarEntryKey o1, JarEntryKey o2) {
            if (o1 == null || o2 == null)
                throw new NullPointerException("Can not compare to Null Jar Entry Key. Provided values " + o1 + " compare to " + o2);
            return String.CASE_INSENSITIVE_ORDER.compare(o1.keyName, o2.keyName);
        }
    };

    public static class JarEntryKey {

        public final String keyName;
        public final String jarEntry;
        private final String toString;

        private JarEntryKey(String keyName, String jarEntry) {
            this.keyName = keyName;
            this.jarEntry = jarEntry;
            toString = "JarEntry(" + keyName + "," + jarEntry + ")";
        }

        @Override
        public String toString() {
            return toString;
        }
    }
}
