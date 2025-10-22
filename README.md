# Y-CRDT JNI

[![CI](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml/badge.svg)](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml)
[![License: Apache 2](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-latest-blue.svg)](https://carcdr.net/y-crdt-jni/)

Java bindings for the [y-crdt](https://github.com/y-crdt/y-crdt) (yrs) Rust library, providing Conflict-free Replicated Data Types (CRDTs) for the JVM.

## Overview

Y-CRDT is a CRDT implementation that enables real-time collaborative editing with automatic conflict resolution. This library provides JNI bindings to make y-crdt available to Java, Kotlin, and other JVM languages.

## Modules

This project consists of multiple modules, each with its own purpose and documentation:

### Core Library

- **[ycrdt](ycrdt/README.md)** - Core Y-CRDT JNI bindings with Rust native library
  - Provides: YDoc, YText, YArray, YMap, YXmlText, YXmlElement, YXmlFragment
  - CRDT types with subdocument support
  - Observer API for change notifications
  - Binary state synchronization
  - [Technical Details](ycrdt/IMPLEMENTATION.md)

### Integrations

- **[yprosemirror](yprosemirror/README.md)** - ProseMirror integration for collaborative rich-text editing
  - Bidirectional sync between ProseMirror and Y-CRDT
  - Change loop prevention
  - Position mapping
  - [Technical Details](yprosemirror/IMPLEMENTATION.md)

### Server

- **[yhocuspocus](yhocuspocus/README.md)** - Transport-agnostic collaborative editing server
  - Y.js-compatible sync protocol
  - Extension system with 12 lifecycle hooks
  - Debounced persistence
  - Awareness protocol (user presence)
  - [Technical Details](yhocuspocus/IMPLEMENTATION.md)

- **[yhocuspocus-websocket](yhocuspocus-websocket/README.md)** - WebSocket transport for yhocuspocus
  - Jetty 12 WebSocket server implementation
  - Reference implementation
  - Yjs/Hocuspocus compatibility
  - [Technical Details](yhocuspocus-websocket/IMPLEMENTATION.md)

### Examples

- **[example-fullstack](example-fullstack/README.md)** - Full-stack collaborative editor example
  - Backend: Java YHocuspocus WebSocket server
  - Frontend: React + TypeScript + Tiptap editor
  - Real-time collaboration with collaborative cursors
  - Demonstrates complete integration

## Quick Start

### Installation

Build from source (Maven Central publishing planned):

```bash
./gradlew build
```

### Example Usage

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

For more examples, see [ycrdt/README.md](ycrdt/README.md).

## Features

- **CRDT Types**: YDoc, YText, YArray, YMap, YXmlText, YXmlElement, YXmlFragment
- **Subdocuments**: Embed YDocs within collections for hierarchical structures
- **Observer API**: Change notifications for all CRDT types
- **Binary Updates**: State synchronization
- **ProseMirror Integration**: Bidirectional sync with ProseMirror editors
- **Collaborative Server**: yhocuspocus server with WebSocket transport
- **Memory Management**: Resource management with AutoCloseable
- **Multi-platform**: Linux, macOS, and Windows support

## Requirements

- Java 21 or higher
- Rust 1.70+ (for building from source)
- Gradle 7.0+ (included via wrapper)

## Building

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :ycrdt:build
./gradlew :yprosemirror:build
./gradlew :yhocuspocus:build

# Run tests
./gradlew test

# Run the example
./gradlew :example-fullstack:backend:run
```

See individual module documentation for module-specific build instructions.

## Project Status

**Current Version**: 0.1.0-SNAPSHOT

**Test Coverage**:
- 36 Rust unit tests (100% passing)
- 239 Java tests in ycrdt (100% passing)
- 22 Java tests in yprosemirror (100% passing)
- 122 Java tests in yhocuspocus + yhocuspocus-websocket (100% passing)

**Total**: 419 tests, 100% passing

## Documentation

- **API Reference**: [https://carcdr.net/y-crdt-jni/](https://carcdr.net/y-crdt-jni/)
- **Module Documentation**: See each module's README.md and IMPLEMENTATION.md
- **Example Application**: [example-fullstack/README.md](example-fullstack/README.md)
- **Development Guide**: [.claude/CLAUDE.md](.claude/CLAUDE.md)

## CI/CD

GitHub Actions workflows:
- **Quick Check** - Linting and formatting on every PR
- **CI** - Test suite on Linux, macOS, and Windows
- **Release** - Automated release creation and artifact publishing
- **Javadoc** - API documentation published to GitHub Pages

Pre-built binaries available from [GitHub Actions](https://github.com/edpaget/y-crdt-jni/actions) and [Releases](https://github.com/edpaget/y-crdt-jni/releases).

## Contributing

Contributions are welcome. See each module's documentation for development plans:
- [ycrdt/PLAN.md](ycrdt/PLAN.md)
- [yprosemirror/PLAN.md](yprosemirror/PLAN.md)
- [yhocuspocus/PLAN.md](yhocuspocus/PLAN.md)
- [yhocuspocus-websocket/PLAN.md](yhocuspocus-websocket/PLAN.md)

Also see [.claude/CLAUDE.md](.claude/CLAUDE.md) for development guidelines.

## License

This project is licensed under the Apache License v2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [y-crdt](https://github.com/y-crdt/y-crdt) - The Rust CRDT implementation
- [jni-rs](https://github.com/jni-rs/jni-rs) - Rust JNI bindings
- [Hocuspocus](https://github.com/ueberdosis/hocuspocus) - Inspiration for yhocuspocus server
