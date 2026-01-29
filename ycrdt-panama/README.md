# ycrdt-panama - Y-CRDT Panama FFM Bindings

Y-CRDT bindings using Java's [Foreign Function & Memory API](https://openjdk.org/jeps/454) (Panama FFM). Binds to the [yffi](https://github.com/y-crdt/y-crdt/tree/main/yffi) C library. Alternative to ycrdt-jni with no JNI code.

**Status: Experimental.** Core types work and are binary-compatible with ycrdt-jni for sync. XML observers and some formatting features are still in progress.

## Requirements

- Java 22+
- JVM arg: `--enable-native-access=ALL-UNNAMED`

## Installation

```groovy
implementation 'net.carcdr:ycrdt-core:0.1.0-SNAPSHOT'
implementation 'net.carcdr:ycrdt-panama:0.1.0-SNAPSHOT'
```

Build from source: `./gradlew :ycrdt-panama:build`

### JVM Configuration

```bash
java --enable-native-access=ALL-UNNAMED -jar myapp.jar
```

Or in Gradle:

```groovy
application {
    applicationDefaultJvmArgs = ['--enable-native-access=ALL-UNNAMED']
}
```

## Usage

```java
import net.carcdr.ycrdt.*;

YBinding binding = YBindingFactory.panama();
try (YDoc doc = binding.createDoc()) {
    try (YText text = doc.getText("shared")) {
        text.push("Hello from Panama!");
    }

    // Binary-compatible with JNI -- can sync between implementations
    byte[] update = doc.encodeStateAsUpdate();
}
```

All APIs match the ycrdt-core interfaces. See [ycrdt-jni README](../ycrdt-jni/README.md) for usage examples -- they work identically with `YBindingFactory.panama()`.

## Supported Types

| Type | Status |
|------|--------|
| YDoc | Full support (transactions, sync, state vectors) |
| YText | Full support (insert, delete, observers) |
| YArray | Full support (string, double, subdocument values) |
| YMap | Full support (string, double, subdocument values) |
| YXmlElement | Attributes and children work. Observers in progress. |
| YXmlText | Basic operations work. Formatting partially implemented. |
| YXmlFragment | Basic operations work. Observers in progress. |

## Known Limitations

1. XML observers may crash the JVM in certain sequences (memory layout issue with `YInput` struct)
2. Top-level `YXmlText` via `doc.getXmlText()` is not supported (throws `UnsupportedOperationException`)
3. Requires `--enable-native-access` JVM flag

## Documentation

- [Development Plan](PLAN.md)
- [ycrdt-core interfaces](../ycrdt-core/)
- [ycrdt-jni](../ycrdt-jni/) -- stable alternative using JNI

## License

Apache License 2.0
