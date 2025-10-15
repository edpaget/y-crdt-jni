# Y-CRDT JNI

[![CI](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml/badge.svg)](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-latest-blue.svg)](https://carcdr.net/y-crdt-jni/)

Java bindings for the [y-crdt](https://github.com/y-crdt/y-crdt) (yrs) Rust library, providing high-performance Conflict-free Replicated Data Types (CRDTs) for the JVM.

## Overview

Y-CRDT is a CRDT implementation that enables real-time collaborative editing with automatic conflict resolution. This library provides JNI bindings to make y-crdt available to Java, Kotlin, and other JVM languages.

## Features

- ✅ **YDoc**: Core document type with synchronization support
- 🚧 **YText**: Collaborative text editing (planned)
- 🚧 **YArray**: Collaborative arrays (planned)
- 🚧 **YMap**: Collaborative maps (planned)
- ✅ **Binary updates**: Efficient state synchronization
- ✅ **Memory safe**: Proper resource management with AutoCloseable

## Requirements

- Java 11 or higher
- Rust 1.70+ (for building from source)
- Gradle 7.0+ (for building)

## Building

### Prerequisites

- Java 11 or higher
- Rust 1.70 or higher
- Gradle 7.0+ (or use the included wrapper)

### Build the native library and Java classes

```bash
./gradlew build
```

This will:
1. Compile the Rust library using Cargo
2. Compile the Java classes
3. Run both Rust and Java tests
4. Package everything into a JAR

### Run tests

```bash
./gradlew test
```

### Clean build artifacts

```bash
./gradlew clean
```

### Build for specific platform

The build automatically detects your platform and builds the appropriate native library. The JAR will include libraries for the host platform only.

For multi-platform JARs, see the [CI/CD section](#cicd) below.

## Usage

### Basic Example

```java
import net.carcdr.ycrdt.YDoc;

public class Example {
    public static void main(String[] args) {
        // Create a new document
        try (YDoc doc = new YDoc()) {
            System.out.println("Client ID: " + doc.getClientId());
            System.out.println("GUID: " + doc.getGuid());
        }
    }
}
```

### Synchronizing Documents

```java
import net.carcdr.ycrdt.YDoc;

public class SyncExample {
    public static void main(String[] args) {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Get the state from doc1
            byte[] state1 = doc1.encodeStateAsUpdate();

            // Apply doc1's state to doc2
            doc2.applyUpdate(state1);

            // Documents are now synchronized!
        }
    }
}
```

### Creating Documents with Specific Client IDs

```java
try (YDoc doc = new YDoc(12345L)) {
    System.out.println("Client ID: " + doc.getClientId()); // 12345
}
```

## API Documentation

Full API documentation is available at [https://carcdr.net/y-crdt-jni/](https://carcdr.net/y-crdt-jni/)

### YDoc

The main document class that represents a Y-CRDT document.

#### Constructors

- `YDoc()` - Creates a new document with a random client ID
- `YDoc(long clientId)` - Creates a new document with a specific client ID

#### Methods

- `long getClientId()` - Returns the client ID of this document
- `String getGuid()` - Returns the globally unique identifier for this document
- `byte[] encodeStateAsUpdate()` - Encodes the current document state as a binary update
- `void applyUpdate(byte[] update)` - Applies a binary update to this document
- `void close()` - Releases native resources (called automatically with try-with-resources)
- `boolean isClosed()` - Checks if the document has been closed

For detailed API documentation, see the [Javadoc](https://carcdr.net/y-crdt-jni/).

## Project Structure

```
y-crdt-jni/
├── src/
│   ├── lib.rs                              # Rust library entry point
│   ├── ydoc.rs                             # YDoc JNI bindings
│   └── main/
│       └── java/
│           └── net/
│               └── carcdr/
│                   └── ycrdt/
│                       ├── YDoc.java        # Java YDoc class
│                       ├── NativeLoader.java # Native library loader
│                       └── Example.java     # Example usage
├── Cargo.toml                               # Rust dependencies
├── build.gradle                             # Gradle build configuration
└── PLAN.md                                  # Development roadmap
```

## Implementation Status

Based on [PLAN.md](PLAN.md):

### ✅ Phase 1: Foundation (Complete)
- [x] Configure Cargo.toml for cdylib
- [x] Add y-crdt dependency
- [x] Create basic JNI scaffolding
- [x] Implement YDoc wrapper
- [x] Create Java YDoc class
- [x] Set up build system
- [x] Add basic tests

### 🚧 Phase 2: Core Types (In Progress)
- [ ] Implement YText bindings
- [ ] Implement YArray bindings
- [ ] Implement YMap bindings
- [ ] Add comprehensive tests

### 🔜 Phase 3: Advanced Features (Planned)
- [ ] Add XML types support
- [ ] Implement observer/callback support
- [ ] Implement transaction support
- [ ] Add state vector handling

### 🔜 Phase 4: Production Ready (Planned)
- [ ] Complete test coverage
- [ ] Set up multi-platform builds (Linux, macOS, Windows)
- [ ] Create comprehensive documentation
- [ ] Publish to Maven Central

## Development

### Running the example

```bash
./gradlew run
```

### Building for release

```bash
./gradlew build -Prelease
```

### Cross-compilation

For cross-compilation to different platforms, see the [cross-compilation guide](docs/cross-compilation.md) (coming soon).

## CI/CD

This project uses GitHub Actions for continuous integration and deployment:

### Workflows

- **Quick Check** - Fast linting and formatting checks on every PR
- **CI** - Full test suite on Linux, macOS, and Windows
  - Builds native libraries for all platforms
  - Creates multi-platform JARs
  - Runs comprehensive tests
- **Release** - Automated release creation and artifact publishing
- **Javadoc** - Generates and publishes API documentation to GitHub Pages

### Downloading Pre-built Binaries

Pre-built multi-platform JARs are available from:
- [GitHub Actions artifacts](https://github.com/edpaget/y-crdt-jni/actions) (CI builds)
- [GitHub Releases](https://github.com/edpaget/y-crdt-jni/releases) (tagged releases)

### Building Multi-Platform JARs Locally

To build for all platforms, you'll need to set up cross-compilation toolchains. See [`.github/workflows/ci.yml`](.github/workflows/ci.yml) for the full build matrix.

## Contributing

Contributions are welcome! Please see [PLAN.md](PLAN.md) for the development roadmap and open issues.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [y-crdt](https://github.com/y-crdt/y-crdt) - The Rust CRDT implementation
- [jni-rs](https://github.com/jni-rs/jni-rs) - Rust JNI bindings
