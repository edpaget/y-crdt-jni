# Y-CRDT JNI

[![CI](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml/badge.svg)](https://github.com/edpaget/y-crdt-jni/actions/workflows/ci.yml)
[![License: Apache 2](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-latest-blue.svg)](https://carcdr.net/y-crdt-jni/)

Java bindings for [y-crdt](https://github.com/y-crdt/y-crdt) (yrs), providing CRDTs for collaborative editing on the JVM.

## Installation

Releases are published to [Maven Central](https://central.sonatype.com/namespace/net.carcdr) under the `net.carcdr` namespace. In-development `-SNAPSHOT` versions are published to [GitHub Packages](https://github.com/edpaget/y-crdt-jni/packages).

### Released versions (Maven Central)

Pull a single module:

```groovy
// Core interfaces (required)
implementation 'net.carcdr:ycrdt-core:<version>'

// JNI implementation (recommended) - Java 21+
implementation 'net.carcdr:ycrdt-jni:<version>'
```

Or import the BOM and omit individual version numbers to keep all modules aligned:

```groovy
dependencies {
    implementation platform('net.carcdr:ycrdt-bom:<version>')

    implementation 'net.carcdr:ycrdt-core'
    implementation 'net.carcdr:ycrdt-jni'
    implementation 'net.carcdr:yhocuspocus'
    // etc.
}
```

### Snapshot versions (GitHub Packages)

Snapshots are published to GitHub Packages on every push to `main`. To consume them, add the GitHub Packages Maven repository and credentials (a GitHub personal access token with `read:packages` scope):

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'GitHubPackages'
        url = uri('https://maven.pkg.github.com/edpaget/y-crdt-jni')
        credentials {
            username = System.getenv('GITHUB_ACTOR') ?: findProperty('gpr.user')
            password = System.getenv('GITHUB_TOKEN') ?: findProperty('gpr.key')
        }
    }
}

dependencies {
    implementation 'net.carcdr:ycrdt-core:0.1.0-SNAPSHOT'
    implementation 'net.carcdr:ycrdt-jni:0.1.0-SNAPSHOT'
}
```

## Usage

```java
import net.carcdr.ycrdt.*;

YBinding binding = YBindingFactory.auto();
try (YDoc doc = binding.createDoc()) {
    try (YText text = doc.getText("myText")) {
        text.push("Hello, ");
        text.push("World!");
        System.out.println(text.toString()); // "Hello, World!"
    }

    // Sync state to another document
    byte[] update = doc.encodeStateAsUpdate();
    try (YDoc doc2 = binding.createDoc()) {
        doc2.applyUpdate(update);
    }
}
```

See [ycrdt-jni/README.md](ycrdt-jni/README.md) for more examples.

## Modules

| Module | Description |
|--------|-------------|
| [ycrdt-core](ycrdt-core/) | Interfaces: YDoc, YText, YArray, YMap. No native deps. |
| [ycrdt-jni](ycrdt-jni/) | JNI implementation (Rust). Java 21+. |
| [ycrdt-panama](ycrdt-panama/) | Panama FFM implementation. Java 22+. Experimental. |
| [yprosemirror](yprosemirror/) | ProseMirror <-> Y-CRDT sync |
| [yhocuspocus](yhocuspocus/) | Collaborative editing server (transport-agnostic) |
| [yhocuspocus-websocket](yhocuspocus-websocket/) | WebSocket transport (Jetty 12) |
| [yhocuspocus-spring-websocket](yhocuspocus-spring-websocket/) | WebSocket transport (Spring) |
| [yhocuspocus-redis](yhocuspocus-redis/) | Redis extension for yhocuspocus |
| [examples](examples/) | Full-stack example apps (React + Tiptap + Java) |

## JNI vs Panama

| | ycrdt-jni | ycrdt-panama |
|--|-----------|--------------|
| Java | 21+ | 22+ |
| Native access | JNI (Rust) | Panama FFM (yffi) |
| JVM args | None | `--enable-native-access=ALL-UNNAMED` |
| XML types | Yes | Not yet |
| Maturity | More mature | Experimental |

```java
YBinding jni    = YBindingFactory.jni();    // explicit JNI
YBinding panama = YBindingFactory.panama(); // explicit Panama
YBinding auto   = YBindingFactory.auto();   // first available
```

## Documentation

- [API Reference (Javadoc)](https://carcdr.net/y-crdt-jni/)
- [Example apps](examples/)

## Development

Requires Java 21+ and Rust 1.70+ (for building from source). Gradle wrapper included.

```bash
./gradlew build          # build all modules
./gradlew test           # run all tests
./gradlew check          # tests + checkstyle
```

See [.claude/CLAUDE.md](.claude/CLAUDE.md) for development guidelines and per-module PLAN.md files for roadmaps.

## License

Apache License v2.0 -- see [LICENSE](LICENSE).

## Acknowledgments

- [y-crdt](https://github.com/y-crdt/y-crdt) -- Rust CRDT implementation
- [jni-rs](https://github.com/jni-rs/jni-rs) -- Rust JNI bindings
- [Hocuspocus](https://github.com/ueberdosis/hocuspocus) -- inspiration for yhocuspocus
