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

Transaction management infrastructure:
- `TransactionPtr` - Send-safe wrapper for transaction pointers
- `ACTIVE_TRANSACTIONS` - Global HashMap storing active transactions by ID
- `TRANSACTION_COUNTER` - Atomic counter for unique transaction IDs
- `next_transaction_id()` - Generates unique transaction identifiers
- `store_transaction()` - Stores transaction with unique ID
- `get_transaction_mut()` - Retrieves mutable transaction reference (lock-free)
- `remove_transaction()` - Removes and commits/drops transaction

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

Rust unit tests validate JNI bindings. Java integration tests cover CRDT operations, Unicode, synchronization, memory stress, subdocuments, observers, transactions, and concurrent access. See [ycrdt-benchmarks](../ycrdt-benchmarks/) for performance data.

## Platform Support

- Linux x86_64, aarch64
- macOS x86_64 (Intel), aarch64 (Apple Silicon)
- Windows x86_64

## Known Limitations

1. External synchronization required for concurrent access to the same document
2. Limited type support in arrays/maps (strings, doubles, subdocuments only)
3. Nested transactions not supported (one active transaction per document)
