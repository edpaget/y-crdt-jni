package net.carcdr.ycrdt.panama.ffi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for loading the native yffi library for Panama FFM access.
 *
 * <p>This class attempts to load the native library in the following order:</p>
 * <ol>
 *   <li>From the system library path</li>
 *   <li>From the JAR resources (extracting to a temporary directory)</li>
 * </ol>
 */
public final class NativeLoader {

    private NativeLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String LIBRARY_NAME = "yrs";
    private static volatile SymbolLookup lookup;
    private static volatile Arena arena;

    /**
     * Returns the symbol lookup for the native library.
     * Loads the library if not already loaded.
     *
     * @return the symbol lookup for native functions
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    public static synchronized SymbolLookup getSymbolLookup() {
        if (lookup == null) {
            loadLibrary();
        }
        return lookup;
    }

    /**
     * Loads the native library.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    private static void loadLibrary() {
        // Create an arena that will live for the duration of the application
        arena = Arena.ofAuto();

        // Try loading from system library path first
        Path systemPath = findSystemLibrary();
        if (systemPath != null) {
            lookup = SymbolLookup.libraryLookup(systemPath, arena);
            return;
        }

        // Try loading from JAR resources
        try {
            Path extractedPath = extractLibraryFromJar();
            lookup = SymbolLookup.libraryLookup(extractedPath, arena);
        } catch (IOException e) {
            throw new UnsatisfiedLinkError(
                "Failed to extract and load native library: " + e.getMessage()
            );
        }
    }

    /**
     * Attempts to find the library in system library paths.
     *
     * @return path to the library if found, null otherwise
     */
    private static Path findSystemLibrary() {
        String libName = getLibraryFileName();
        String libPath = System.getProperty("java.library.path");
        if (libPath != null) {
            for (String dir : libPath.split(System.getProperty("path.separator"))) {
                Path path = Path.of(dir, libName);
                if (Files.exists(path)) {
                    return path;
                }
            }
        }
        return null;
    }

    /**
     * Extracts the native library from JAR resources to a temporary file.
     *
     * @return path to the extracted library
     * @throws IOException if extraction fails
     */
    private static Path extractLibraryFromJar() throws IOException {
        String libName = getLibraryFileName();
        String osName = getOsName();
        String osArch = getOsArch();

        String resourcePath = "/native/" + osName + "/" + osArch + "/" + libName;

        try (InputStream is = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new UnsatisfiedLinkError(
                    "Native library not found in JAR: " + resourcePath
                    + " (OS: " + osName + ", Arch: " + osArch + ")"
                );
            }

            Path tempDir = Files.createTempDirectory("ycrdt-panama-");
            tempDir.toFile().deleteOnExit();

            Path libPath = tempDir.resolve(libName);
            Files.copy(is, libPath, StandardCopyOption.REPLACE_EXISTING);
            libPath.toFile().deleteOnExit();

            // Make executable on Unix-like systems
            libPath.toFile().setExecutable(true);

            return libPath;
        }
    }

    /**
     * Gets the platform-specific library file name.
     *
     * @return the library file name
     */
    private static String getLibraryFileName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac") || osName.contains("darwin")) {
            return "lib" + LIBRARY_NAME + ".dylib";
        } else if (osName.contains("windows")) {
            return LIBRARY_NAME + ".dll";
        } else {
            return "lib" + LIBRARY_NAME + ".so";
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
