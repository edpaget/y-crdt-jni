# Implementation Details

This document describes the technical implementation of the y-crdt JNI bindings, providing an overview of the architecture, components, and design decisions.

## Architecture Overview

The project bridges Rust's Y-CRDT implementation (yrs) to the JVM through JNI (Java Native Interface), providing Java classes that wrap native Rust functionality. The architecture consists of three main layers:

1. **Rust JNI Layer** - Native methods that interface between Rust and Java
2. **Java API Layer** - Idiomatic Java classes with proper resource management
3. **Build System** - Gradle + Cargo integration for seamless builds

## Rust Implementation

### Configuration (`Cargo.toml`)

The project is configured as a `cdylib` (C-compatible dynamic library) to support JNI:

```toml
[lib]
crate-type = ["cdylib"]
name = "ycrdt_jni"
```

**Dependencies:**
- `jni` (v0.21.1) - JNI bindings for Rust
- `yrs` (v0.21.3) - Y-CRDT implementation

**Optimizations:**
- Link-time optimization (LTO) enabled
- Optimization level 3 for release builds
- Code generation units optimized for single-threaded compilation

### Core Utilities (`src/lib.rs`)

Provides foundational JNI helper functions used across all bindings:

**String Conversion:**
- `to_jstring(env, str) -> jstring` - Converts Rust strings to Java strings with proper error handling

**Exception Handling:**
- `throw_exception(env, message)` - Throws Java `RuntimeException` from Rust code

**Pointer Management:**
- `to_java_ptr<T>(obj) -> jlong` - Converts Rust objects to Java pointers using `Box::into_raw()`
- `from_java_ptr<T>(ptr) -> &T` - Converts Java pointers back to Rust references
- `free_java_ptr<T>(ptr)` - Frees Rust objects using `Box::from_raw()`

These functions ensure safe memory management across the JNI boundary.

### YDoc Bindings (`src/ydoc.rs`)

Implements the core document type with 7 native methods:

1. **`nativeCreate()`** - Creates a new YDoc with random client ID
2. **`nativeCreateWithClientId(clientId)`** - Creates YDoc with specific client ID
3. **`nativeDestroy(ptr)`** - Frees YDoc memory
4. **`nativeGetClientId(ptr)`** - Returns the document's client ID
5. **`nativeGetGuid(ptr)`** - Returns the document's GUID as a string
6. **`nativeEncodeStateAsUpdate(ptr)`** - Encodes full document state as byte array
7. **`nativeApplyUpdate(ptr, update)`** - Applies binary update to document

**Key Implementation Details:**
- All methods validate pointer arguments before dereferencing
- String conversions handle UTF-8 encoding properly
- State encoding uses empty state vector to export full document state
- Proper error handling with Java exceptions

**Testing:**
- 3 Rust unit tests covering creation, client ID, and state encoding

### YText Bindings (`src/ytext.rs`)

Implements collaborative text editing with 7 native methods:

1. **`nativeGetText(docPtr, name)`** - Gets or creates a YText instance
2. **`nativeDestroy(ptr)`** - Frees YText memory
3. **`nativeLength(docPtr, textPtr)`** - Returns text length in UTF-8 bytes
4. **`nativeToString(docPtr, textPtr)`** - Returns text content as string
5. **`nativeInsert(docPtr, textPtr, index, chunk)`** - Inserts text at index
6. **`nativePush(docPtr, textPtr, chunk)`** - Appends text to end
7. **`nativeDelete(docPtr, textPtr, index, length)`** - Deletes text range

**Key Implementation Details:**
- Uses `TextRef` for reference-counted access to shared text
- All operations require both document and text pointers
- Transactions created automatically for each operation
- Modified UTF-8 handling for Java string compatibility
- Unicode and emoji support verified

**Testing:**
- 4 Rust unit tests covering creation, insert/read, push, and delete

### YArray Bindings (`src/yarray.rs`)

Implements collaborative arrays with 11 native methods:

1. **`nativeGetArray(docPtr, name)`** - Gets or creates a YArray instance
2. **`nativeDestroy(ptr)`** - Frees YArray memory
3. **`nativeLength(docPtr, arrayPtr)`** - Returns array length
4. **`nativeGetString(docPtr, arrayPtr, index)`** - Gets string at index
5. **`nativeGetDouble(docPtr, arrayPtr, index)`** - Gets double at index
6. **`nativeInsertString(docPtr, arrayPtr, index, value)`** - Inserts string at index
7. **`nativeInsertDouble(docPtr, arrayPtr, index, value)`** - Inserts double at index
8. **`nativePushString(docPtr, arrayPtr, value)`** - Appends string to end
9. **`nativePushDouble(docPtr, arrayPtr, value)`** - Appends double to end
10. **`nativeRemove(docPtr, arrayPtr, index, length)`** - Removes range of elements
11. **`nativeToJson(docPtr, arrayPtr)`** - Serializes array to JSON string

**Key Implementation Details:**
- Uses `ArrayRef` for reference-counted access to shared array
- Supports mixed types (strings and doubles)
- Type-specific getters return appropriate defaults on type mismatch
- JSON serialization uses `ToJson` trait from yrs
- Result error handling for type conversions

**Testing:**
- 4 Rust unit tests covering creation, push/read, insert, and remove

## Java Implementation

### YDoc (`src/main/java/net/carcdr/ycrdt/YDoc.java`)

The main document class providing a Java-friendly API:

**Constructors:**
- `YDoc()` - Creates document with random client ID
- `YDoc(long clientId)` - Creates document with specific client ID (validates non-negative)

**Public Methods:**
- `long getClientId()` - Returns the client ID
- `String getGuid()` - Returns the document GUID
- `byte[] encodeStateAsUpdate()` - Exports document state for synchronization
- `void applyUpdate(byte[] update)` - Imports state from another document
- `YText getText(String name)` - Gets or creates a YText instance
- `YArray getArray(String name)` - Gets or creates a YArray instance
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if document is closed

**Design Features:**
- Implements `Closeable` for try-with-resources pattern
- Thread-safe close operation using synchronized block
- `ensureNotClosed()` validation on all operations
- Comprehensive JavaDoc with usage examples
- Finalizer as safety net for missed close() calls
- Proper null checking with meaningful exceptions

**Testing:**
- 13 comprehensive tests covering all functionality

### YText (`src/main/java/net/carcdr/ycrdt/YText.java`)

Collaborative text editing class:

**Public Methods:**
- `int length()` - Returns text length
- `String toString()` - Returns text content
- `void insert(int index, String chunk)` - Inserts text at index
- `void push(String chunk)` - Appends text to end
- `void delete(int index, int length)` - Deletes text range
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getText()`)
- Input validation with meaningful exceptions
- Index bounds checking
- Null checking for all string parameters
- Thread-safe close operation
- Comprehensive JavaDoc

**Testing:**
- 23 comprehensive tests including unicode, synchronization, and edge cases

### YArray (`src/main/java/net/carcdr/ycrdt/YArray.java`)

Collaborative array class supporting mixed types:

**Public Methods:**
- `int length()` - Returns array length
- `String getString(int index)` - Gets string at index (returns null if out of bounds)
- `double getDouble(int index)` - Gets double at index (returns 0.0 if out of bounds)
- `void insertString(int index, String value)` - Inserts string at index
- `void insertDouble(int index, double value)` - Inserts double at index
- `void pushString(String value)` - Appends string to end
- `void pushDouble(double value)` - Appends double to end
- `void remove(int index, int length)` - Removes range of elements
- `String toJson()` - Serializes array to JSON
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getArray()`)
- Type-specific methods for strings and doubles
- Comprehensive bounds checking
- Null checking for string parameters
- Thread-safe close operation
- JSON serialization for inspection/debugging
- Comprehensive JavaDoc

**Testing:**
- 27 comprehensive tests covering all operations, mixed types, and synchronization

### NativeLoader (`src/main/java/net/carcdr/ycrdt/NativeLoader.java`)

Utility class for loading platform-specific native libraries:

**Loading Strategy:**
1. Attempt to load from system library path (`System.loadLibrary()`)
2. If that fails, extract from JAR resources to temporary file
3. Load from temporary file location

**Platform Support:**
- **Operating Systems:** Linux, macOS (Darwin), Windows
- **Architectures:** x86_64 (amd64), aarch64 (arm64)
- **Naming Conventions:**
  - Linux: `libycrdt_jni.so`
  - macOS: `libycrdt_jni.dylib`
  - Windows: `ycrdt_jni.dll`

**Error Handling:**
- Detailed error messages for debugging
- Fallback mechanisms for different loading strategies
- Platform/architecture detection logging

### Example Program (`src/main/java/net/carcdr/ycrdt/Example.java`)

Comprehensive demonstration program with 8 examples:

1. **Creating a YDoc** - Basic document creation
2. **Creating YDoc with client ID** - Custom client ID
3. **Synchronizing documents** - Document state synchronization
4. **Working with YText** - Text editing operations
5. **Synchronizing YText** - Collaborative text editing between documents
6. **Working with YArray** - Array operations with mixed types
7. **Synchronizing YArray** - Collaborative array editing between documents
8. **Proper cleanup** - Resource management demonstration

**Code Organization:**
- Main method delegates to helper methods
- Each example is self-contained in a private static method
- Consistent output formatting
- Proper resource management with try-with-resources

## Build System

### Gradle Configuration (`build.gradle`)

Integrates Rust and Java builds seamlessly:

**Java Configuration:**
- Java 11 source/target compatibility
- JUnit 4.13.2 for testing
- Checkstyle for code quality

**Custom Gradle Tasks:**

1. **`buildRustLibrary`** - Executes `cargo build --release`
   - Compiles Rust code to native library
   - Uses release profile for optimization

2. **`copyNativeLibrary`** - Copies native library to resources
   - Depends on `buildRustLibrary`
   - Copies to `src/main/resources/native/`
   - Handles platform-specific naming

3. **`cleanRust`** - Executes `cargo clean`
   - Cleans Rust build artifacts

4. **`testRust`** - Executes `cargo test`
   - Runs Rust unit tests
   - Integrated into Java test task

**Build Pipeline:**
- Java compilation depends on Rust library build
- Tests run both Rust and Java test suites
- Clean task cleans both Rust and Java artifacts

### CI/CD (`github/workflows/`)

Automated workflows for quality assurance:

**Quick Check Workflow:**
- Rust formatting check (`cargo fmt --check`)
- Rust linting (`cargo clippy`)
- Fast feedback for pull requests

**CI Workflow:**
- Multi-platform builds (Ubuntu, macOS, Windows)
- Rust and Java test execution
- Checkstyle validation
- Runs on push and pull requests

**Release Workflow:**
- Triggered by version tags
- Builds native libraries for all platforms
- Creates GitHub releases with artifacts

**Javadoc Workflow:**
- Generates Javadoc HTML
- Publishes to GitHub Pages
- Automatic deployment on main branch updates

## Memory Management

### Rust Side

**Allocation:**
- Objects wrapped in `Box` for heap allocation
- `Box::into_raw()` converts to raw pointer for Java
- Pointer cast to `jlong` for safe transmission

**Deallocation:**
- Java calls native `destroy` method
- `Box::from_raw()` reconstitutes Box
- Box drops, freeing memory automatically

**Safety Guarantees:**
- All pointers validated before dereferencing
- Zero pointer checks prevent null dereference
- Proper exception handling on errors

### Java Side

**Resource Management:**
- All types implement `Closeable` interface
- `close()` method calls native `destroy`
- `closed` flag prevents use-after-free
- Synchronized close prevents double-free

**Safety Mechanisms:**
- `ensureNotClosed()` / `checkClosed()` on all operations
- IllegalStateException thrown on closed objects
- Finalizer as safety net (deprecated but functional)
- Try-with-resources pattern recommended

**Lifecycle:**
```
Create → Use → Close → Disposed
         ↑      ↓
         └──────┘ (multiple operations allowed)
```

## Type System Mapping

### Primitive Types

| Rust Type | JNI Type | Java Type |
|-----------|----------|-----------|
| `i64` | `jlong` | `long` |
| `i32` | `jint` | `int` |
| `f64` | `jdouble` | `double` |
| `bool` | `jboolean` | `boolean` |

### Complex Types

| Rust Type | Java Type | Conversion |
|-----------|-----------|------------|
| `String` | `String` | `to_jstring()` + `get_string()` |
| `Vec<u8>` | `byte[]` | JNI byte array functions |
| `*mut T` | `long` | Pointer as 64-bit integer |

### Y-CRDT Types

| yrs Type | Rust Binding | Java Class |
|----------|--------------|------------|
| `Doc` | Boxed ownership | `YDoc` |
| `TextRef` | Shared reference | `YText` |
| `ArrayRef` | Shared reference | `YArray` |

## Error Handling

### Rust Error Strategy

**Pointer Validation:**
```rust
if ptr == 0 {
    throw_exception(&mut env, "Invalid pointer");
    return default_value;
}
```

**String Conversion:**
```rust
let string = match env.get_string(&input) {
    Ok(s) => s,
    Err(_) => {
        throw_exception(&mut env, "Failed to get string");
        return;
    }
};
```

**Result Handling:**
- Type conversions return `Result<T, E>`
- Errors converted to null/default values or exceptions
- Never panics across JNI boundary

### Java Error Strategy

**Exception Types:**
- `IllegalStateException` - Closed object accessed
- `IllegalArgumentException` - Invalid parameters (null, negative)
- `IndexOutOfBoundsException` - Array/text index out of range
- `RuntimeException` - Native errors bubbled from Rust

**Validation Order:**
1. Closed state check
2. Null parameter check
3. Bounds/range check
4. Native call (may throw RuntimeException)

## Testing Strategy

### Rust Tests (12 total)

**Unit Tests:**
- `lib.rs` - Pointer conversion (1 test)
- `ydoc.rs` - Document operations (3 tests)
- `ytext.rs` - Text operations (4 tests)
- `yarray.rs` - Array operations (4 tests)

**Coverage:**
- Creation and destruction
- Basic operations
- Type conversions
- Memory safety

### Java Tests (63 total)

**YDocTest (13 tests):**
- Creation and lifecycle
- Client ID management
- State encoding/decoding
- Synchronization
- Error handling

**YTextTest (23 tests):**
- Text operations (insert, push, delete)
- Unicode and emoji support
- Synchronization
- Complex editing sequences
- Error handling and edge cases

**YArrayTest (27 tests):**
- Array operations (push, insert, remove)
- Mixed type handling
- JSON serialization
- Synchronization (unidirectional and bidirectional)
- Error handling and boundary conditions

**Test Quality:**
- 100% pass rate
- Integration tests verify Rust-Java communication
- Synchronization tests verify CRDT properties
- Resource management tests prevent leaks
- Error tests ensure proper exception handling

## Code Quality

### Rust Quality Tools

**Rustfmt:**
- Standard Rust formatting
- Enforced in CI/CD
- Run with `cargo fmt`

**Clippy:**
- Rust linter
- Zero warnings policy
- Run with `cargo clippy -- -D warnings`

### Java Quality Tools

**Checkstyle:**
- Google Java Style Guide
- Configured in `build.gradle`
- Enforced in CI/CD

**Quality Rules:**
- Maximum method length: 150 lines
- Proper JavaDoc on all public APIs
- Consistent naming conventions

## File Structure

```
y-crdt-jni/
├── Cargo.toml                                # Rust configuration
├── Cargo.lock                                # Rust dependencies lock
├── build.gradle                              # Gradle build configuration
├── settings.gradle                           # Gradle settings
├── gradlew / gradlew.bat                     # Gradle wrapper scripts
├── .gitignore                                # Git ignore rules
├── README.md                                 # User documentation
├── PLAN.md                                   # Project roadmap
├── CHANGELOG.md                              # Version history
├── IMPLEMENTATION.md                         # This file
├── src/
│   ├── lib.rs                                # Rust library entry point
│   ├── ydoc.rs                               # YDoc JNI bindings
│   ├── ytext.rs                              # YText JNI bindings
│   ├── yarray.rs                             # YArray JNI bindings
│   ├── main/java/net/carcdr/ycrdt/
│   │   ├── YDoc.java                         # YDoc Java wrapper
│   │   ├── YText.java                        # YText Java wrapper
│   │   ├── YArray.java                       # YArray Java wrapper
│   │   ├── NativeLoader.java                # Native library loader
│   │   └── Example.java                     # Example usage program
│   └── test/java/net/carcdr/ycrdt/
│       ├── YDocTest.java                     # YDoc test suite
│       ├── YTextTest.java                    # YText test suite
│       └── YArrayTest.java                   # YArray test suite
├── .github/
│   └── workflows/
│       ├── quickcheck.yml                    # Quick check workflow
│       ├── ci.yml                            # Main CI workflow
│       ├── release.yml                       # Release workflow
│       └── javadoc.yml                       # Javadoc publishing
└── target/                                   # Build artifacts (gitignored)
    ├── debug/                                # Debug builds
    │   └── libycrdt_jni.{so,dylib,dll}      # Debug native library
    └── release/                              # Release builds
        └── libycrdt_jni.{so,dylib,dll}      # Optimized native library
```

## Performance Considerations

### JNI Overhead

**Minimization Strategies:**
- Batch operations where possible
- Avoid excessive string conversions
- Use transactions for multiple operations
- Cache native pointers in Java objects

**Cost Breakdown:**
- JNI call overhead: ~10-50ns per call
- String conversion: ~100-500ns depending on length
- Object allocation: ~50-100ns
- Transaction overhead: ~1-10µs

### Memory Efficiency

**Rust Side:**
- Zero-copy where possible
- Efficient state encoding (binary format)
- Shared references for Y types (Rc/Arc)

**Java Side:**
- Minimal wrapper overhead (single pointer field)
- No data duplication (native holds data)
- Lazy object creation (on-demand)

## Security Considerations

### Memory Safety

**Rust Guarantees:**
- No buffer overflows (bounds checked)
- No use-after-free (Box ownership)
- No null pointer dereferences (validation)
- Thread-safe by default

**JNI Safety:**
- Pointer validation on every native call
- No raw pointer exposure to Java
- Exception-based error propagation
- No panics across JNI boundary

### Input Validation

**Rust Layer:**
- All pointers checked for zero
- String conversions error-handled
- Array bounds validated

**Java Layer:**
- Null parameter checking
- State validation (closed check)
- Range validation (index bounds)
- Type validation where applicable

## Platform Support

### Tested Platforms

- **macOS** (Darwin)
  - x86_64 (Intel)
  - aarch64 (Apple Silicon)

### Supported Platforms (via CI)

- **Linux** (Ubuntu)
  - x86_64

- **macOS**
  - x86_64
  - aarch64

- **Windows**
  - x86_64

### Platform-Specific Concerns

**macOS:**
- Uses `.dylib` extension
- System Integrity Protection may require code signing

**Linux:**
- Uses `.so` extension
- glibc version compatibility

**Windows:**
- Uses `.dll` extension
- Microsoft Visual C++ Runtime required

## Future Extensibility

### Adding New Types

To add a new Y-CRDT type (e.g., YMap):

1. Create `src/ymap.rs` with native methods
2. Add `mod ymap;` and `pub use ymap::*;` to `lib.rs`
3. Create `YMap.java` with Closeable pattern
4. Add `getMap(String)` method to `YDoc.java`
5. Create `YMapTest.java` with comprehensive tests
6. Add examples to `Example.java`

### Adding Features

**Observers/Callbacks:**
- Requires JNI global references
- Callback registration system
- Event dispatch mechanism

**Transactions:**
- Transaction handle management
- Begin/commit/rollback API
- Nested transaction support

**Additional Types:**
- Boolean, integer support in arrays
- Nested type support (arrays of arrays)
- Custom type serialization

## Known Limitations

1. **Single-threaded Access:** Documents not thread-safe; external synchronization required
2. **No Observer Support:** Cannot subscribe to document changes yet
3. **Limited Array Types:** Only strings and doubles supported
4. **No Transaction API:** Direct manipulation only
5. **Platform Builds:** Cross-compilation scripts not yet automated
6. **No Maven Publishing:** Library not in public repositories
