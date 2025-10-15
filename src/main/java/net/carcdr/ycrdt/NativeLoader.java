package net.carcdr.ycrdt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for loading the native y-crdt JNI library.
 *
 * <p>This class attempts to load the native library in the following order:</p>
 * <ol>
 *   <li>From the system library path (using System.loadLibrary)</li>
 *   <li>From the JAR resources (extracting to a temporary directory)</li>
 * </ol>
 */
final class NativeLoader {

    private NativeLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String LIBRARY_NAME = "ycrdt_jni";
    private static volatile boolean loaded = false;

    /**
     * Loads the native library. This method is idempotent - calling it multiple
     * times is safe.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    static synchronized void loadLibrary() {
        if (loaded) {
            return;
        }

        // Try loading from system library path first
        try {
            System.loadLibrary(LIBRARY_NAME);
            loaded = true;
            return;
        } catch (UnsatisfiedLinkError e) {
            // Fall through to try loading from JAR
        }

        // Try loading from JAR resources
        try {
            String libName = System.mapLibraryName(LIBRARY_NAME);
            String osName = getOsName();
            String osArch = getOsArch();

            // Try to find the library in the JAR
            String resourcePath = "/native/" + osName + "/" + osArch + "/" + libName;

            try (InputStream is = NativeLoader.class.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new UnsatisfiedLinkError(
                        "Native library not found in JAR: " + resourcePath +
                        " (OS: " + osName + ", Arch: " + osArch + ")"
                    );
                }

                // Extract to temporary directory
                Path tempDir = Files.createTempDirectory("ycrdt-jni-");
                tempDir.toFile().deleteOnExit();

                Path libPath = tempDir.resolve(libName);
                Files.copy(is, libPath, StandardCopyOption.REPLACE_EXISTING);
                libPath.toFile().deleteOnExit();

                // Make executable on Unix-like systems
                libPath.toFile().setExecutable(true);

                // Load the library
                System.load(libPath.toAbsolutePath().toString());
                loaded = true;
            }
        } catch (IOException e) {
            throw new UnsatisfiedLinkError(
                "Failed to extract and load native library: " + e.getMessage()
            );
        }
    }

    /**
     * Gets the normalized OS name for library loading.
     *
     * @return "linux", "macos", or "windows"
     */
    private static String getOsName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            return "linux";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return "macos";
        } else if (osName.contains("windows")) {
            return "windows";
        }
        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    /**
     * Gets the normalized architecture name for library loading.
     *
     * @return "x86_64" or "aarch64"
     */
    private static String getOsArch() {
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            return "x86_64";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            return "aarch64";
        }
        throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
    }
}
