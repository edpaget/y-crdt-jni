# Implementation Details - ycrdt-panama

## Architecture

The module has three layers:

1. **FFM Bindings** (`Yrs.java`) -- Panama downcall handles to yffi C functions
2. **NativeLoader** -- Platform-specific library loading (system path or JAR resources)
3. **Panama* Classes** -- Implement ycrdt-core interfaces using the FFM bindings

```
ycrdt-core interfaces
        |
Panama* classes (PanamaYDoc, PanamaYText, ...)
        |
Yrs.java (2,400+ lines of downcall handles)
        |
yffi (C library wrapping Rust y-crdt v0.25.0)
```

## FFM Bindings (Yrs.java)

`Yrs.java` defines `MethodHandle` downcall stubs for every yffi function. Key patterns:

- **SymbolLookup**: Loads library symbols via `NativeLoader`
- **Linker.nativeLinker()**: Creates downcall handles for C functions
- **Upcall stubs**: Java-to-native callbacks for observers, created via `Linker.nativeLinker().upcallStub()`

### YInput Struct Workaround

The yffi library returns `YInput` structs by value (24 bytes). On ARM64 and x86_64, Panama's downcall mechanism only handles register-returned structs up to 16 bytes. The workaround: `YInput` structs are constructed manually in Java using `VarHandle` field access instead of calling native `yinput_*` functions.

See `PANAMA-STRUCT-CONVENTIONS.md` for details on the calling convention constraints.

## Memory Management

### Arena Strategy

| Arena Type | Used For |
|------------|----------|
| `Arena.ofAuto()` | Library loading (application lifetime) |
| `Arena.ofShared()` | Observer subscriptions (outlive individual objects) |
| `Arena.ofConfined()` | Temporary string allocations in method calls |

### Thread Safety

`PanamaYDoc` uses a `ReentrantReadWriteLock` (fair ordering) to serialize transaction access. The underlying yffi `ydoc_write_transaction()` is non-blocking and returns NULL on contention, so Java-side locking is required.

## Native Library

### Build Process

1. `scripts/build-yffi.sh` clones y-crdt (v0.25.0) and runs `cargo build --release` in the yffi directory
2. Gradle `buildYffi` task invokes the script
3. `copyNativeLibrary` packages the binary into JAR resources under `native/{os}/{arch}/`

### Platform Libraries

| Platform | Library |
|----------|---------|
| macOS | `libyrs.dylib` |
| Linux | `libyrs.so` |
| Windows | `yrs.dll` |

### Loading Order

1. System library path (`java.library.path`)
2. Extract from JAR resources to temp directory
3. Load via `SymbolLookup.libraryLookup()`

## Cross-Implementation Compatibility

Panama and JNI implementations produce binary-compatible state vectors and updates. `CrossImplementationSyncTest` verifies bidirectional sync between the two implementations.

## Known Issues

- **YInput tag crash**: JVM can crash with "unrecognized YInput tag: 99" in certain test sequences involving XML element/text observers. Root cause is likely a memory layout mismatch in the upcall stub for XML event callbacks.
- **Transaction contention**: yffi returns NULL from `ydoc_write_transaction()` when another transaction is active. The Java-side `ReentrantReadWriteLock` prevents this, but it means transactions are fully serialized.
