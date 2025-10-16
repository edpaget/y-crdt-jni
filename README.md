# Y-CRDT JNI

[![CI](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml/badge.svg)](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml)
[![License: Apache 2](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-latest-blue.svg)](https://carcdr.net/y-crdt-jni/)

Java bindings for the [y-crdt](https://github.com/y-crdt/y-crdt) (yrs) Rust library, providing high-performance Conflict-free Replicated Data Types (CRDTs) for the JVM.

## Overview

Y-CRDT is a CRDT implementation that enables real-time collaborative editing with automatic conflict resolution. This library provides JNI bindings to make y-crdt available to Java, Kotlin, and other JVM languages.

## Features

- ✅ **YDoc**: Core document type with synchronization support
- ✅ **YText**: Collaborative text editing with Unicode support
- ✅ **YArray**: Collaborative arrays with mixed types and JSON serialization
- ✅ **YMap**: Collaborative maps with mixed types and JSON serialization
- ✅ **YXmlText**: Collaborative XML text nodes
- ✅ **YXmlElement**: Collaborative XML elements with attributes
- ✅ **YXmlFragment**: Hierarchical XML tree support with child node retrieval
- ✅ **Binary updates**: Efficient state synchronization
- ✅ **Memory safe**: Proper resource management with AutoCloseable
- ✅ **Multi-platform**: Linux, macOS, and Windows support

See [PLAN.md](PLAN.md) for the full development roadmap and [CHANGELOG.md](CHANGELOG.md) for detailed feature list.

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

## Quick Start

```java
import net.carcdr.ycrdt.*;

public class Example {
    public static void main(String[] args) {
        // Create a document
        try (YDoc doc = new YDoc()) {
            // Collaborative text editing
            try (YText text = doc.getText("myText")) {
                text.push("Hello, ");
                text.push("World!");
                System.out.println(text.toString()); // "Hello, World!"
            }

            // Collaborative map
            try (YMap map = doc.getMap("myMap")) {
                map.set("name", "Alice");
                map.set("age", 30.0);
                System.out.println(map.toJson()); // {"name":"Alice","age":30.0}
            }

            // Synchronize with another document
            byte[] update = doc.encodeStateAsUpdate();
            try (YDoc doc2 = new YDoc()) {
                doc2.applyUpdate(update);
                // doc2 now has the same state as doc
            }
        }
    }
}
```

For more examples, see the [Example.java](src/main/java/net/carcdr/ycrdt/Example.java) program with 14+ demonstrations.

## API Documentation

**Full API documentation**: [https://carcdr.net/y-crdt-jni/](https://carcdr.net/y-crdt-jni/)

**Implementation details**: See [IMPLEMENTATION.md](IMPLEMENTATION.md) for technical architecture and JNI binding details.

### Core Types

- **YDoc** - Main document class with synchronization support
- **YText** - Collaborative text with insert, push, delete operations
- **YArray** - Collaborative array supporting strings and doubles
- **YMap** - Collaborative map supporting strings and doubles
- **YXmlText** - XML text nodes with collaborative editing
- **YXmlElement** - XML elements with attribute management
- **YXmlFragment** - Hierarchical XML trees with child node access

All types implement `AutoCloseable` for automatic resource management.

## Project Status

**Current Version**: 0.1.0-SNAPSHOT

**Test Coverage**:
- 30 Rust unit tests (100% passing)
- 147 Java integration tests (100% passing)

**Build Status**: [![CI](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml/badge.svg)](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml)

See [PLAN.md](PLAN.md) for development roadmap and [CHANGELOG.md](CHANGELOG.md) for detailed change history.

## Development

```bash
# Run the example program
./gradlew run

# Run tests
./gradlew test

# Run Rust tests
cargo test

# Format and lint
cargo fmt
cargo clippy
./gradlew checkstyle
```

See [IMPLEMENTATION.md](IMPLEMENTATION.md) for build system details and architecture documentation.

## CI/CD

GitHub Actions workflows:
- **Quick Check** - Fast linting and formatting on every PR
- **CI** - Full test suite on Linux, macOS, and Windows with multi-platform JAR creation
- **Release** - Automated release creation and artifact publishing
- **Javadoc** - API documentation published to GitHub Pages

**Pre-built binaries**: Available from [GitHub Actions](https://github.com/edpaget/y-crdt-jni/actions) and [Releases](https://github.com/edpaget/y-crdt-jni/releases)

See [`.github/workflows/`](.github/workflows/) for workflow configurations.

## Contributing

Contributions are welcome! Please see [PLAN.md](PLAN.md) for the development roadmap and open issues.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [y-crdt](https://github.com/y-crdt/y-crdt) - The Rust CRDT implementation
- [jni-rs](https://github.com/jni-rs/jni-rs) - Rust JNI bindings
