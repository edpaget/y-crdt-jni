# Implementation Details - ycrdt

Technical implementation details for the ycrdt JNI bindings.

## Architecture

The ycrdt module bridges Rust's Y-CRDT implementation (yrs) to the JVM through JNI:

1. **Rust JNI Layer** - Native methods interfacing Rust and Java
2. **Java API Layer** - Idiomatic Java classes with resource management
3. **Build System** - Gradle + Cargo integration

## Rust Implementation

### Configuration

Library type: `cdylib` (C-compatible dynamic library for JNI)

Dependencies:
- `jni` (v0.21.1) - JNI bindings for Rust
- `yrs` (v0.21.3) - Y-CRDT implementation

Optimizations:
- Link-time optimization enabled
- Optimization level 3 for release builds

### Core Utilities (`src/lib.rs`)

Helper functions for JNI operations:
- `to_jstring()` - Converts Rust strings to Java strings
- `throw_exception()` - Throws Java RuntimeException
- `to_java_ptr<T>()` - Converts Rust objects to Java pointers
- `from_java_ptr<T>()` - Converts Java pointers to Rust references
- `free_java_ptr<T>()` - Frees Rust objects

### Native Bindings

Each Y-CRDT type has a corresponding Rust module with JNI bindings:

- **`ydoc.rs`** - 7 native methods for YDoc (create, destroy, clientId, guid, encoding)
- **`ytext.rs`** - 7 native methods for YText (get, destroy, length, insert, push, delete, toString)
- **`yarray.rs`** - 11 native methods for YArray (get, destroy, length, insert, push, remove, getters, toJson)
- **`ymap.rs`** - 12 native methods for YMap (get, destroy, size, set, get, remove, containsKey, keys, clear, toJson)
- **`yxmltext.rs`** - 11 native methods for YXmlText (text ops + formatting + ancestor lookup)
- **`yxmlelement.rs`** - 15 native methods for YXmlElement (attributes + nesting + ancestor lookup)
- **`yxmlfragment.rs`** - 9 native methods for YXmlFragment (child management)

All methods validate pointers, handle errors, and use transactions automatically.

## Java Implementation

### Core Classes

All Java classes implement `Closeable` for resource management:

- **YDoc** - Main document with synchronization
- **YText** - Collaborative text editing
- **YArray** - Collaborative arrays
- **YMap** - Collaborative maps
- **YXmlText** - XML text with formatting
- **YXmlElement** - XML elements with nesting
- **YXmlFragment** - XML fragment containers

### Resource Management

```
Create → Use → Close → Disposed
         ↑      ↓
         └──────┘ (multiple operations allowed)
```

Safety mechanisms:
- `ensureNotClosed()` validation on all operations
- `IllegalStateException` thrown on closed objects
- Synchronized close prevents double-free
- Finalizer as safety net

### Memory Management

**Rust Side:**
- Objects wrapped in `Box` for heap allocation
- `Box::into_raw()` converts to pointer for Java
- `Box::from_raw()` reconstitutes for deallocation

**Java Side:**
- Native pointer stored as `long` field
- `close()` calls native `destroy` method
- `closed` flag prevents use-after-free

## Build System

### Gradle Configuration

Custom tasks for Rust integration:
- `buildRustLibrary` - Runs `cargo build --release`
- `copyNativeLibrary` - Copies library to resources
- `cleanRust` - Runs `cargo clean`
- `testRust` - Runs `cargo test`

Java compilation depends on Rust library build.

### Native Library Loading

`NativeLoader` utility class handles platform-specific loading:
1. Attempt to load from system library path
2. If that fails, extract from JAR resources
3. Load from temporary file

Supported platforms:
- Linux: `libycrdt_jni.so`
- macOS: `libycrdt_jni.dylib`
- Windows: `ycrdt_jni.dll`

## Type System Mapping

| Rust Type | JNI Type | Java Type |
|-----------|----------|-----------|
| `i64` | `jlong` | `long` |
| `i32` | `jint` | `int` |
| `f64` | `jdouble` | `double` |
| `bool` | `jboolean` | `boolean` |
| `String` | `jstring` | `String` |
| `Vec<u8>` | `jbyteArray` | `byte[]` |
| `*mut T` | `jlong` | `long` (pointer) |

Y-CRDT types use reference-counted shared references (TextRef, ArrayRef, MapRef, etc.).

## Error Handling

### Rust Strategy

- Pointer validation before dereferencing
- String conversion error handling
- Result types for operations
- Never panics across JNI boundary

### Java Strategy

Exception types:
- `IllegalStateException` - Closed object accessed
- `IllegalArgumentException` - Invalid parameters
- `IndexOutOfBoundsException` - Index out of range
- `RuntimeException` - Native errors from Rust

Validation order:
1. Closed state check
2. Null parameter check
3. Bounds/range check
4. Native call

## Testing

### Rust Tests (36 total)

Unit tests in each module covering:
- Creation and destruction
- Basic operations
- Type conversions
- Memory safety

### Java Tests (306 total)

Integration tests covering:
- Lifecycle and resource management
- All CRDT operations
- Unicode and emoji support
- Document synchronization
- Error handling
- Memory stress tests
- Subdocument operations
- Observer functionality
- Advanced sync operations

All tests pass with 100% success rate.

## Performance

### JNI Overhead

Cost breakdown:
- JNI call: ~10-50ns
- String conversion: ~100-500ns
- Object allocation: ~50-100ns
- Transaction: ~1-10µs

Optimization strategies:
- Batch operations where possible
- Avoid excessive string conversions
- Use transactions for multiple operations

### Memory Efficiency

- Zero-copy where possible
- Binary state encoding
- Shared references for Y types
- Minimal wrapper overhead

## Platform Support

Tested and supported platforms:
- Linux x86_64
- macOS x86_64 (Intel)
- macOS aarch64 (Apple Silicon)
- Windows x86_64

## Known Limitations

1. Single-threaded access (external synchronization required for core operations)
2. Limited type support in arrays/maps (strings, doubles, subdocuments only)
3. No explicit transaction API (operations are auto-transactional)
4. Platform builds not cross-compiled automatically

## Future Extensibility

To add new Y-CRDT types:
1. Create `src/ytype.rs` with native methods
2. Add to `lib.rs`
3. Create `YType.java` with Closeable pattern
4. Add getter to `YDoc.java`
5. Create test suite
6. Add examples

Potential features:
- Explicit transaction API
- Additional type support
- Cross-platform build automation
