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

package com.jhash.oimadmin.oim.code;

import com.jhash.oimadmin.OIMAdminException;
import com.jhash.oimadmin.Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Java {

    private static final Logger logger = LoggerFactory.getLogger(Java.class);
    private List<File> classPath = new ArrayList<>();

    public Java() {
    }

    public List<File> getClassPath() {
        return Collections.unmodifiableList(this.classPath);
    }

    public Java setClassPath(List<File> classPath) {
        this.classPath.clear();
        if (classPath != null) {
            this.classPath.addAll(classPath);
        }
        return this;
    }

    public Java addClassPath(File... classPath) {
        if (classPath != null) {
            this.classPath.addAll(Arrays.asList(classPath));
        }
        return this;
    }

    public Java addClassPath(List<File> classPath) {
        if (classPath != null) {
            this.classPath.addAll(classPath);
        }
        return this;
    }

    public String compile(CompileUnit compileUnit, String outputFileLocation) {
        logger.trace("Entering compile({},{})", new Object[]{compileUnit, outputFileLocation});
        if (compileUnit == null || Utils.isEmpty(outputFileLocation))
            throw new OIMAdminException("Missing source code details " + compileUnit + " or output folder " + outputFileLocation);
        File outputFileDirectory = new File(outputFileLocation);
        logger.trace("Validating if the output location {} exists and is a directory", outputFileLocation);
        if (outputFileDirectory.exists()) {
            if (outputFileDirectory.isDirectory()) {
                try {
                    logger.trace("Deleting the directory and its content");
                    FileUtils.forceDelete(outputFileDirectory);
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
            throw new OIMAdminException("Failed to locate a java compiler. Please ensure that application is being run using JDK (Java Development Kit) and NOT JRE (Java Runtime Environment) ");
        }
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<File> files = Arrays.asList(new File(outputFileLocation));
        boolean success = false;
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        try {
            JavaFileObject javaFileObject = new InMemoryJavaFileObject(compileUnit.className, compileUnit.sourceCode);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, files);
            if (classPath != null && !classPath.isEmpty()) {
                logger.debug("Setting classpath {}", classPath);
                fileManager.setLocation(StandardLocation.CLASS_PATH, classPath);
            }
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                    compileUnit.options, null,
                    Arrays.asList(javaFileObject));
            success = task.call();
            fileManager.close();
        } catch (Exception exception) {
            throw new OIMAdminException("Failed to compile " + compileUnit.className, exception);
        }

        if (!success) {
            logger.trace("Exiting compileJava(): Return Value {}", diagnostics);
            StringBuilder message = new StringBuilder();
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                switch (d.getKind()) {
                    case ERROR:
                        message.append("ERROR: ").append(d).append(Utils.LINE_SEPARATOR);
                    default:
                        message.append(d).append(Utils.LINE_SEPARATOR);
                }
            }
            String messageAsString = message.toString();
            return messageAsString;
        }
        return null;
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
