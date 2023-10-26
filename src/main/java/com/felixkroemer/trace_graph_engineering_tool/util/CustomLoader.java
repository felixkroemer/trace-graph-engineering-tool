// Copyright 2010-2022 Google LLC
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.felixkroemer.trace_graph_engineering_tool.util;

import com.sun.jna.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Load native libraries needed for using ortools-java.
 */
public class CustomLoader {
    private static final String RESOURCE_PATH = "ortools-" + Platform.RESOURCE_PREFIX + "/jniortools.dll";

    /**
     * Try to locate the native libraries directory.
     */
    private static URL getNativeResourceURI() throws IOException {
        ClassLoader loader = CustomLoader.class.getClassLoader();
        URL resourceURL = loader.getResource(RESOURCE_PATH);
        Objects.requireNonNull(resourceURL, String.format("Resource %s was not found in ClassLoader %s",
                RESOURCE_PATH, loader));

        URI resourceURI;
        try {
            resourceURI = resourceURL.toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return resourceURL;
    }

    @FunctionalInterface
    private interface PathConsumer<T extends IOException> {
        void accept(Path path) throws T;
    }

    /**
     * Extract native resources in a temp directory.
     *
     * @param resourceURI Native resource location.
     * @return The directory path containing all extracted libraries.
     */
    private static Path unpackNativeResources(URL resourceURI) throws IOException {
        Path tempPath;
        tempPath = Files.createTempDirectory("ortools-java");
        tempPath.toFile().deleteOnExit();

        Bundle bundle = FrameworkUtil.getBundle(CustomLoader.class);

        if (bundle != null) {
            // Create an InputStream for the bundle resource
            try (InputStream inputStream = resourceURI.openStream()) {
                // Create an OutputStream to write to the host's file system
                try (OutputStream outputStream = new FileOutputStream(tempPath.resolve("jniortools.dll").toFile())) {
                    // Copy the data from the input stream to the output stream
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                // Handle any potential exceptions
                e.printStackTrace();
            }
        } else {
            // Handle the case where the bundle is not found
            System.err.println("Bundle not found.");
        }

/*        FileSystem fs;
        try {
            fs = FileSystems.getDefault();
        } catch (FileSystemAlreadyExistsException e) {
            fs = FileSystems.getFileSystem(resourceURI);
            if (fs == null) {
                throw new IllegalArgumentException();
            }
        }
        Path p = fs.provider().getPath(resourceURI);
        visitor.accept(p);*/
        return tempPath;
    }

    /**
     * Unpack and Load the native libraries needed for using ortools-java.
     */
    private static boolean loaded = false;

    public static synchronized void loadNativeLibraries() {
        if (!loaded) {
            try {
                // prints the name of the Operating System
                // System.out.println("OS: " + System.getProperty("os.name"));
                // System.out.println("Library: " + System.mapLibraryName("jniortools"));

                System.loadLibrary("jniortools");
                loaded = true;
                return;
            } catch (UnsatisfiedLinkError exception) {
                // Do nothing.
            }
            try {
                URL resourceURL = getNativeResourceURI();
                Path tempPath = unpackNativeResources(resourceURL);
                // Load the native library
                System.load(tempPath.resolve(System.mapLibraryName("jniortools")).toAbsolutePath().toString());
                loaded = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
