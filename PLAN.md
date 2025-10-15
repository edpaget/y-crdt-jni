# Plan for Packaging y-crdt Rust Library for JVM

## Overview
This document outlines the plan for creating JNI bindings to expose the y-crdt (yrs) Rust library for use from the JVM (Java/Kotlin).

**Status:** Phase 2 In Progress (YText Complete) ✅ | Last Updated: 2025-10-15

## Progress Summary

- ✅ **Phase 1: Foundation** - COMPLETE
- 🚧 **Phase 2: Core Types** - IN PROGRESS (YText ✅, YArray 🔜, YMap 🔜)
- 🔜 **Phase 3: Advanced Features** - Not Started
- 🔜 **Phase 4: Production Ready** - Not Started

## Recent Updates (2025-10-15)

### YText Implementation ✅ COMPLETE
- Implemented full collaborative text editing support
- 7 native JNI methods (get, destroy, length, toString, insert, push, delete)
- Comprehensive Java API with Closeable pattern
- 23 comprehensive tests covering all operations, edge cases, and synchronization
- Unicode/emoji support with Modified UTF-8 handling
- Examples added to Example.java

### Critical Bug Fix 🐛 FIXED
- **Issue:** `encodeStateAsUpdate()` was encoding against the document's own state vector, resulting in empty updates
- **Fix:** Changed to encode against an empty state vector to get the full document state
- **Impact:** Document synchronization now works correctly across all types (YDoc, YText)

## 1. Project Structure Setup ✅ COMPLETE
- ✅ Convert the Rust project from a binary to a `cdylib` library in `Cargo.toml`
- ✅ Add `y-crdt` (yrs) dependency to access the CRDT functionality (v0.21.3)
- ✅ Set up proper library configuration for JNI
- ✅ Organize source code into logical modules (lib.rs, ydoc.rs)
- ✅ Package name: `net.carcdr.ycrdt`

## 2. Rust JNI Bridge Layer (In Progress 🚧)
- ✅ Create JNI wrapper functions that expose y-crdt functionality
- Implement core CRDT types:
  - ✅ `YDoc` - the main document (COMPLETE)
  - ✅ `YText` - collaborative text (COMPLETE)
  - 🔜 `YArray` - collaborative array (TODO)
  - 🔜 `YMap` - collaborative map (TODO)
  - 🔜 `YXmlText`, `YXmlElement` - collaborative XML structures (TODO)
- ✅ Handle memory management carefully (Rust ownership + JVM GC)
- ✅ Implement proper error handling and exception throwing to JVM
- ✅ Create helper functions for type conversions between Rust and Java

### YDoc Implementation Details (src/ydoc.rs)
- ✅ `nativeCreate()` - Create new document
- ✅ `nativeCreateWithClientId(long)` - Create with specific client ID
- ✅ `nativeDestroy(long)` - Free native memory
- ✅ `nativeGetClientId(long)` - Get client ID
- ✅ `nativeGetGuid(long)` - Get document GUID
- ✅ `nativeEncodeStateAsUpdate(long)` - Serialize state (fixed critical bug - now encodes against empty state vector)
- ✅ `nativeApplyUpdate(long, byte[])` - Apply binary updates

### YText Implementation Details (src/ytext.rs) ✅ NEW
- ✅ `nativeGetText(long, String)` - Get or create YText instance
- ✅ `nativeDestroy(long)` - Free YText memory
- ✅ `nativeLength(long, long)` - Get text length
- ✅ `nativeToString(long, long)` - Get text content as string
- ✅ `nativeInsert(long, long, int, String)` - Insert text at index
- ✅ `nativePush(long, long, String)` - Append text to end
- ✅ `nativeDelete(long, long, int, int)` - Delete range of text
- ✅ Fixed Java Modified UTF-8 string handling for Unicode support
- ✅ 4 Rust unit tests (all passing)

## 3. Java/Kotlin API Layer (In Progress 🚧)
- ✅ Create Java classes that mirror the Rust types
  - ✅ `YDoc.java` - Main document class (COMPLETE)
  - ✅ `YText.java` - Text type (COMPLETE)
  - 🔜 `YArray.java` - Array type (TODO)
  - 🔜 `YMap.java` - Map type (TODO)
- ✅ Design idiomatic Java API that wraps native calls
- ✅ Implement builder patterns where appropriate
- ✅ Add proper Java documentation (Javadoc)
- ✅ Consider fluent API design for better developer experience

### YDoc.java Features
- ✅ Implements `Closeable` for try-with-resources
- ✅ Two constructors: default and with client ID
- ✅ Thread-safe close() operation
- ✅ State validation (isClosed() checks)
- ✅ Comprehensive JavaDoc with examples
- ✅ Proper exception handling
- ✅ Finalizer as safety net
- ✅ `getText(String name)` method to create/get YText instances

### YText.java Features ✅ NEW
- ✅ Implements `Closeable` for proper resource management
- ✅ Full API: `length()`, `toString()`, `insert()`, `push()`, `delete()`, `close()`, `isClosed()`
- ✅ Input validation with meaningful exceptions
- ✅ Thread-safe close() operation
- ✅ Comprehensive JavaDoc with examples
- ✅ Package-private constructor (created via YDoc.getText())
- ✅ Unicode support (emoji, international characters)
- ✅ 23 comprehensive tests (all passing)

## 4. Build System Integration (Partial ✅)
- ✅ Set up `cargo` build to produce platform-specific shared libraries (.so, .dylib, .dll)
- ✅ Configure Gradle build system for Java side
  - ✅ `build.gradle` with Rust integration
  - ✅ `buildRustLibrary` task
  - ✅ `copyNativeLibrary` task
  - ✅ `cleanRust` and `testRust` tasks
- ✅ Implement resource loading to bundle native libraries in JAR
  - ✅ `NativeLoader.java` with platform/arch detection
  - ✅ Support for loading from JAR or system path
- ✅ Support multiple platforms (Linux, macOS, Windows) with appropriate architectures (x86_64, aarch64)
  - ✅ Platform detection implemented
  - 🚧 Cross-compilation scripts (TODO)
- 🚧 Create build scripts to automate cross-compilation (TODO)

## 5. Memory Management Strategy ✅ COMPLETE
- ✅ Implement `AutoCloseable`/`Closeable` pattern in Java for deterministic cleanup
- 🔜 Use JNI `GlobalRef` for long-lived objects (not needed yet for YDoc)
- ✅ Implement proper cleanup in native layer with destructor functions
  - ✅ `nativeDestroy()` properly frees Rust memory
  - ✅ Pointer conversions use Box::into_raw/from_raw
- ✅ Consider using Java `Cleaner` or finalizers as safety net
  - ✅ Finalizer implemented in YDoc
- ✅ Document lifecycle expectations for API users
  - ✅ JavaDoc explains resource management
  - ✅ Examples show try-with-resources pattern
- ✅ Implement reference counting or ownership tracking as needed
  - ✅ Simple ownership model: Java owns native pointer
  - ✅ Closed flag prevents use-after-free

## 6. Testing Infrastructure (In Progress 🚧)
- ✅ Write Rust unit tests for JNI functions
  - ✅ 8 tests total (all passing)
  - ✅ 4 tests in lib.rs and ydoc.rs
  - ✅ 4 tests in ytext.rs (NEW)
  - ✅ Tests for pointer conversion, doc creation, client ID, state encoding
  - ✅ Tests for text creation, insert/read, push, delete (NEW)
- ✅ Create Java integration tests
  - ✅ 36 tests total (all passing - 100% success rate)
  - ✅ `YDocTest.java` with 13 comprehensive tests
  - ✅ `YTextTest.java` with 23 comprehensive tests (NEW)
  - ✅ Tests cover creation, lifecycle, synchronization, error handling
  - ✅ Unicode/emoji support tests (NEW)
  - ✅ Complex editing sequence tests (NEW)
  - ✅ Bidirectional sync tests (NEW)
- 🔜 Test memory leak scenarios with stress tests (TODO)
- 🔜 Test concurrent access patterns (y-crdt's strength) (TODO)
- 🔜 Add benchmarks to track performance (TODO)
- ✅ Test error handling and exception propagation
  - ✅ Tests for closed documents, null updates, negative IDs
  - ✅ Tests for null chunks, index out of bounds (NEW)

## 7. Build Artifacts & Distribution (In Progress 🚧)
- 🚧 Create multi-platform JAR with native libraries embedded (partial - gradle task ready)
- ✅ Set up CI/CD for building across platforms (GitHub Actions workflows created)
  - ✅ Quick Check workflow (formatting, linting, compilation)
  - ✅ CI workflow (multi-platform testing and building)
  - ✅ Release workflow (automated releases on tags)
  - ✅ Javadoc workflow (publishes to GitHub Pages)
- 🔜 Consider publishing to Maven Central (TODO)
- ✅ Document version compatibility between Rust and Java APIs
  - ✅ yrs v0.21.3
  - ✅ jni v0.21.1
  - ✅ Java 11+
- 🔜 Create release process and versioning strategy (TODO)
- 🔜 Consider fat JAR vs platform-specific JARs (TODO)

## 8. Documentation & Examples (In Progress 🚧)
- ✅ Write README with quick start guide
  - ✅ `README.md` with comprehensive documentation
  - ✅ Overview, features, requirements, build instructions
  - ✅ API documentation and usage examples
  - ✅ Javadoc badge linking to published API docs
- ✅ Create example projects showing common use cases:
  - ✅ Basic document creation and manipulation (`Example.java`)
  - ✅ Real-time synchronization between clients (examples in README and Example.java)
  - ✅ YText collaborative editing examples (Example.java Examples 4-5) (NEW)
  - ✅ YText synchronization between documents (NEW)
  - 🔜 Persistence and loading (TODO - not yet implemented)
  - 🔜 Integration with popular frameworks (TODO)
- ✅ Document thread safety guarantees
  - ✅ JavaDoc states YDoc is not thread-safe
  - ✅ Recommends external synchronization
- 🔜 Provide migration guides if applicable (N/A - first version)
- 🔜 Include troubleshooting section (TODO)

### Additional Documentation
- ✅ `PLAN.md` - This document with full roadmap (updated)
- ✅ `IMPLEMENTATION.md` - Technical implementation details
- ✅ Comprehensive JavaDoc in all classes (YDoc, YText)
- ✅ Javadoc published to GitHub Pages (https://carcdr.net/y-crdt-jni/)
- ✅ Code quality setup documented (Checkstyle, Clippy)
- ✅ Development guidelines in `.claude/Claude.md`

## Key Design Considerations

### Threading Model
y-crdt operations need careful synchronization when called from multiple Java threads. Consider:
- Document-level locking strategy
- Thread-safe transaction boundaries
- Concurrent read/write patterns

### Serialization
Expose y-crdt's update encoding/decoding for network sync:
- Binary update format
- State vector handling
- Snapshot/incremental update support

### Callbacks/Observers
Implement JVM callbacks for document change notifications:
- Observer pattern for change events
- Event filtering and subscription
- Proper lifecycle management for callback objects

### Performance
Minimize JNI boundary crossings by batching operations where possible:
- Batch reads/writes when feasible
- Efficient data structure conversions
- Consider zero-copy strategies for large data

### Error Handling
Map Rust panics and Results to appropriate Java exceptions:
- Define custom exception hierarchy
- Preserve error context and stack traces
- Handle both recoverable and unrecoverable errors

## Implementation Phases

### Phase 1: Foundation ✅ COMPLETE
1. ✅ Configure Cargo.toml for cdylib
   - Added `crate-type = ["cdylib"]`
   - Set library name to `ycrdt_jni`
   - Configured release optimizations
2. ✅ Add y-crdt dependency
   - yrs v0.21.3
   - jni v0.21.1
3. ✅ Create basic JNI scaffolding
   - `src/lib.rs` with helper functions
   - `src/ydoc.rs` with YDoc bindings
   - Proper error handling and type conversions
4. ✅ Implement YDoc wrapper
   - All core YDoc methods implemented
   - Java wrapper class with Closeable pattern
   - NativeLoader for library loading
   - Comprehensive tests (Rust + Java)
   - Example program
   - Full documentation

**Completed:** 2025-10-15
**Build Status:** ✅ All tests passing (8 Rust tests, 36 Java tests)
**Artifacts:** libycrdt_jni.dylib (macOS), ready for other platforms

### Phase 2: Core Types 🚧 IN PROGRESS (YText Complete)
1. ✅ Implement YText bindings **COMPLETE**
   - ✅ Created `src/ytext.rs` with JNI methods
   - ✅ Implemented insert, delete, push operations
   - ✅ Added `YText.java` wrapper class with Closeable pattern
   - ✅ Wrote 23 comprehensive tests (all passing)
   - ✅ Fixed Unicode/Modified UTF-8 string handling
   - ✅ Added examples in `Example.java`
   - ✅ Full JavaDoc documentation
2. 🔜 Implement YArray bindings **TODO**
   - Create `src/yarray.rs` with JNI methods
   - Implement push, insert, delete, get operations
   - Add `YArray.java` wrapper class
   - Write tests for array operations
3. 🔜 Implement YMap bindings **TODO**
   - Create `src/ymap.rs` with JNI methods
   - Implement set, get, delete, keys, values operations
   - Add `YMap.java` wrapper class
   - Write tests for map operations
4. ✅ Add basic Java wrapper classes **PARTIAL**
   - ✅ Consistent Closeable pattern across types
   - ✅ Shared error handling approach
   - ✅ Consistent API patterns (YDoc, YText)

**Status:** YText Complete (1 of 3 types)
**Completed:** 2025-10-15
**Build Status:** ✅ All 36 Java tests passing, ✅ All 8 Rust tests passing
**Next Step:** Begin with YArray or YMap implementation

### Phase 3: Advanced Features 🔜 TODO
1. 🔜 Add XML types support
   - YXmlText and YXmlElement bindings
2. 🔜 Implement update encoding/decoding
   - State vectors
   - Differential updates
   - Update merging
3. 🔜 Add observer/callback support
   - Event subscription
   - Change notifications
   - Callback lifecycle management
4. 🔜 Implement transaction support
   - Transaction begin/commit/rollback
   - Batch operations
   - Transaction observers

**Status:** Not Started
**Dependencies:** Requires Phase 2 completion

### Phase 4: Production Ready 🔜 TODO
1. 🔜 Complete test coverage
   - Memory leak tests
   - Concurrent access tests
   - Performance benchmarks
   - Integration tests
2. 🔜 Set up multi-platform builds
   - CI/CD for Linux, macOS, Windows
   - Cross-compilation scripts
   - Automated testing on all platforms
3. 🔜 Create comprehensive documentation
   - API reference
   - Tutorial guides
   - Best practices
   - Troubleshooting
4. 🔜 Publish initial release
   - Maven Central publishing
   - Release notes
   - Version tagging
   - Migration guides

**Status:** Not Started
**Target:** v1.0.0 release

## Success Criteria

### Phase 1 Criteria ✅ MET
- ✅ YDoc accessible from Java with full API
- ✅ Basic memory management working (no leaks detected in basic tests)
- ✅ Build system functioning for host platform
- ✅ Tests passing (8 Rust, 36 Java tests)
- ✅ Documentation complete for Phase 1 scope

### Phase 2 Criteria (In Progress)
- ✅ YText accessible from Java with full API (COMPLETE)
- 🔜 YArray accessible from Java (TODO)
- 🔜 YMap accessible from Java (TODO)
- ✅ Synchronization working between documents (COMPLETE)
- ✅ Unicode support working (COMPLETE)
- ✅ Comprehensive test coverage for YText (23 tests, 100% passing)

### Overall Success Criteria (Target)
- 🚧 All core y-crdt types accessible from Java (YDoc ✅, YText ✅, YArray 🔜, YMap 🔜)
- 🔜 No memory leaks in stress tests (basic tests passing, stress tests TODO)
- 🔜 Performance overhead < 20% vs native Rust (not yet benchmarked)
- 🚧 Support for all major platforms (architecture ready, cross-compilation TODO)
- 🚧 Comprehensive test coverage (>80%) (currently good coverage for YDoc and YText)
- ✅ Production-ready documentation (for implemented features)
