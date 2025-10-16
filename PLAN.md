# Plan for Packaging y-crdt Rust Library for JVM

## Overview
This document outlines the plan for creating JNI bindings to expose the y-crdt (yrs) Rust library for use from the JVM (Java/Kotlin).

**Status:** Phases 1, 2, 3 Complete (All features implemented) | Phase 4 In Progress | Last Updated: 2025-10-16

## Current Status Summary

### ✅ Completed Features
- **Core CRDT Types:** YDoc, YText, YArray, YMap
- **Hierarchical XML:** YXmlText, YXmlElement, YXmlFragment with full tree navigation
  - Rich text formatting (insertWithAttributes, format)
  - Nested elements (childCount, insertElement, insertText, getChild, removeChild)
  - Ancestor lookup (getParent, getIndexInParent)
- **Subdocuments:** YDoc nesting within YMap and YArray
  - Hierarchical document structures (embed YDocs within collections)
  - Full CRDT type support within subdocuments
  - Synchronization of subdocument structures
- **Observer API:** Real-time change notifications for all CRDT types
  - Complete observer support for all 6 types (YText, YArray, YMap, YXmlFragment, YXmlElement, YXmlText)
  - YObserver interface with onChange callback
  - Type-specific change events (YTextChange, YArrayChange, YMapChange, YXmlElementChange)
  - YSubscription handles with AutoCloseable support
  - Thread-safe callbacks with proper JVM attachment
  - 51 comprehensive observer integration tests
- **Transaction Semantics:** Automatic transactional operations
  - All operations are automatically atomic (transaction per operation)
  - Observer callbacks fire after transaction commits
  - Transaction origin tracking in events
  - No manual transaction management needed
  - Aligns with CRDT best practices
- **Testing:** 342 total tests (36 Rust + 306 Java), 100% passing
  - 214 functional/integration tests
  - 25 memory stress tests (no leaks detected)
  - 16 subdocument tests
  - 51 observer integration tests
  - 16 advanced update encoding/decoding tests
- **Documentation:** Comprehensive JavaDoc, IMPLEMENTATION.md, published to GitHub Pages
- **Build System:** Gradle + Cargo integration, GitHub Actions CI/CD, test output configuration
- **Memory Management:** Closeable pattern, proper native resource cleanup, stress tested

### 🚧 In Progress
- **Phase 4:** Production readiness (stress testing, benchmarks, Maven Central)
- **YProseMirror:** ProseMirror integration (Phase 1 setup complete)
- **Build Artifacts:** Multi-platform distribution
- **Documentation:** Tutorial guides and best practices

## Implementation Phases

### Phase 1: Foundation ✅ COMPLETE
**Completed:** 2025-10-15

Core infrastructure for JNI bindings:
- ✅ YDoc with state encoding/decoding, client ID management
- ✅ Gradle build system with Rust integration
- ✅ NativeLoader for platform-specific library loading
- ✅ Memory management with Closeable pattern
- ✅ Comprehensive error handling

**Deliverables:**
- 13 YDoc tests (all passing)
- Native library builds (macOS tested)
- GitHub Actions CI/CD workflows

---

### Phase 2: Core CRDT Types ✅ COMPLETE
**Completed:** 2025-10-16

Implemented all core collaborative data types:

#### YText (Collaborative Text)
- 7 native methods: getText, destroy, length, toString, insert, push, delete
- Unicode/emoji support with Modified UTF-8 handling
- 23 comprehensive tests
- Examples in Example.java

#### YArray (Collaborative Array)
- 14 native methods: getArray, destroy, length, get/insert/push (String/Double/Doc), remove, toJson
- Mixed type support (strings, doubles, and subdocuments)
- 27 comprehensive tests
- JSON serialization
- Subdocument support (insertDoc, pushDoc, getDoc)

#### YMap (Collaborative Map)
- 14 native methods: getMap, destroy, size, get/set (String/Double/Doc), remove, containsKey, keys, clear, toJson
- Mixed type support (strings, doubles, and subdocuments)
- 30 comprehensive tests
- JSON serialization
- Subdocument support (setDoc, getDoc)

**Key Achievement:** Fixed critical bug in `encodeStateAsUpdate()` - now encodes against empty state vector for correct synchronization

**Build Status:** ✅ All 239 Java tests passing (198 functional + 25 stress + 16 subdocument), ✅ All 36 Rust tests passing

---

### Phase 3.5: Hierarchical XML ✅ COMPLETE
**Completed:** 2025-10-16

Implemented full hierarchical XML support with tree navigation:

#### YXmlText (Collaborative XML Text)
- **11 native methods** including:
  - Basic operations: getXmlText, destroy, length, toString, insert, push, delete
  - Rich text formatting: insertWithAttributes, format
  - Ancestor lookup: getParent, getIndexInParent
- **41 comprehensive tests** (14 formatting + 7 ancestor lookup)
- Supports arbitrary formatting attributes (bold, italic, color, custom)
- Unicode and emoji support

#### YXmlElement (Collaborative XML Element)
- **15 native methods** including:
  - Attributes: getTag, getAttribute, setAttribute, removeAttribute, getAttributeNames
  - Hierarchy: childCount, insertElement, insertText, getChild, removeChild
  - Ancestor lookup: getParent, getIndexInParent
- **55 comprehensive tests** (18 nested element + 12 ancestor lookup)
- Supports deeply nested XML structures (5+ levels tested)
- Polymorphic child/parent handling

#### YXmlFragment (XML Root Container)
- **9 native methods** for fragment operations
- Child node retrieval (getElement, getText)
- Direct XmlElementRef/XmlTextRef pointer architecture
- **9 comprehensive tests**

**Key Features:**
- Full tree navigation (upward via getParent, downward via getChild)
- Rich text formatting synchronized across documents
- Deeply nested structures with synchronization
- Mixed content (elements and text) support

**Use Cases Enabled:**
- HTML/SVG collaborative editors
- Rich text documents with formatting
- DOM-like tree manipulation
- Hierarchical document structures

---

### Phase 3.6: Subdocuments ✅ COMPLETE
**Completed:** 2025-10-16

Implemented full subdocument support for hierarchical document structures:

#### Subdocument Features
- **YMap Subdocuments:**
  - `setDoc(String key, YDoc subdoc)` - Insert subdocument
  - `getDoc(String key)` - Retrieve subdocument
  - 2 native methods

- **YArray Subdocuments:**
  - `insertDoc(int index, YDoc subdoc)` - Insert at specific index
  - `pushDoc(YDoc subdoc)` - Append to end
  - `getDoc(int index)` - Retrieve by index
  - 3 native methods

- **16 comprehensive tests** covering:
  - Basic insertion and retrieval
  - Multiple subdocuments
  - Nested structures
  - Synchronization
  - Mixed content (strings, doubles, subdocuments)
  - Stress testing (50+ subdocuments)

**Key Capabilities:**
- Hierarchical document composition
- Full CRDT type support within subdocuments (YText, YArray, YMap, YXml*)
- Proper memory management with Closeable pattern
- Synchronization of subdocument structures across clients

**Use Cases Enabled:**
- Modular document architecture
- Composable collaborative applications
- Organized complex data structures
- Multi-level document hierarchies

**Limitations:**
- A Doc can only be inserted as subdocument once (cannot reuse same instance)
- Content added before insertion may not persist (recommended: add after retrieval)

---

### Phase 3.7: Observer API ✅ COMPLETE
**Completed:** 2025-10-16

Implemented complete observer/event system for real-time change notifications:

#### Observer Features
- **Full Type Coverage:**
  - YText observer with YTextChange (INSERT, DELETE, RETAIN)
  - YArray observer with YArrayChange (INSERT, DELETE, RETAIN)
  - YMap observer with YMapChange (INSERT, DELETE, UPDATE)
  - YXmlElement observer with YXmlElementChange (attribute changes)
  - YXmlFragment observer with YArrayChange (child changes)
  - YXmlText observer with YTextChange (text changes)

- **API Components:**
  - `YObserver` - Functional interface for change callbacks
  - `YEvent` - Immutable event with target, changes, and origin
  - `YChange` - Abstract base for type-specific changes
  - `YSubscription` - AutoCloseable handle for observer lifecycle
  - Type-specific change classes with full change information

- **Implementation:**
  - Rust JNI integration with proper JVM thread attachment
  - Thread-safe observer registration and callback dispatch
  - Subscription ID-based observer tracking (prevents GC issues)
  - Exception isolation (observer errors don't affect others)
  - ConcurrentHashMap for thread-safe observer storage

- **51 comprehensive observer tests** covering:
  - Basic observation (events fired on changes)
  - Type-specific change details (inserts, deletes, attributes)
  - Multiple observers per object
  - Subscription lifecycle (close, auto-close)
  - Thread safety
  - Exception handling
  - Synchronization across documents

**Use Cases Enabled:**
- Real-time UI updates in collaborative editors
- Change tracking and auditing
- Reactive data flows
- ProseMirror bidirectional synchronization
- Custom conflict resolution logic

---

### Phase 3.8: Advanced Update Encoding/Decoding ✅ COMPLETE
**Completed:** 2025-10-16

Implemented complete update encoding/decoding system for efficient synchronization:

#### State Vector Support
- **`encodeStateVector()`** - Encode current document state as compact vector
- Logical timestamp representation of observed changes
- Used for differential synchronization between peers
- 2 comprehensive tests (empty document and with content)

#### Differential Updates
- **`encodeDiff(byte[] stateVector)`** - Generate updates containing only unseen changes
- More efficient than full document synchronization
- Reduces network bandwidth and storage requirements
- 4 comprehensive tests (basic, partial sync, error handling)

#### Update Merging
- **`mergeUpdates(byte[][] updates)`** - Combine multiple updates into single compact update
- Static utility method for batch processing
- Eliminates redundant operations during merge
- 5 comprehensive tests (basic, single update, null handling)

#### State Vector Extraction
- **`encodeStateVectorFromUpdate(byte[] update)`** - Extract state vector from encoded update
- Inspect update contents without applying to document
- Useful for synchronization protocols
- 3 comprehensive tests (basic, equivalence checking, null handling)

**API Components:**
- 4 new public methods in YDoc
- 4 new native JNI methods in Rust
- Full JavaDoc documentation with usage examples
- Comprehensive error handling and validation

**Implementation Details:**
- Uses yrs v1 encoding format (default and most compatible)
- Proper JNI array handling for byte[] parameters
- Thread-safe operations
- Memory-efficient byte array processing

**Test Coverage:**
- **16 new Java tests** covering:
  - State vector encoding (empty and with content)
  - Differential synchronization workflows
  - Partial synchronization scenarios
  - Update merging (single, multiple, error cases)
  - State vector extraction and equivalence
  - Real-world client-server sync workflow
  - Error handling (null inputs, closed documents)

**Use Cases Enabled:**
- Efficient peer-to-peer synchronization
- Client-server differential updates
- Offline-first applications with sync
- Update batching and compression
- Synchronization protocol implementation

---

### Phase 3.9: Transaction Semantics ✅ COMPLETE
**Status:** Built into architecture from the beginning

The y-crdt-jni library provides automatic transactional semantics for all operations:

#### Automatic Transaction Management
- **Every operation is transactional** - All CRDT operations (insert, delete, set, etc.) automatically create, execute, and commit a transaction
- **Atomic operations** - Each method call completes as an atomic unit
- **No manual transaction management needed** - Transactions are handled transparently by the library

#### Observer Integration
- **Observer callbacks triggered after commits** - All registered observers fire after a transaction successfully commits
- **Transaction origin tracking** - YEvent includes origin information for observers to identify the source of changes
- **Ordered callback execution** - Type-specific observers, deep observers, and update callbacks execute in defined order

#### Why Not Traditional Transactions?
- **CRDTs are append-only** - Operations cannot be rolled back once committed; they become part of the permanent operation history
- **No rollback semantics** - The nature of CRDTs means there's no concept of aborting a transaction
- **Automatic batching by yrs** - The underlying Rust library already optimizes transaction handling
- **Explicit batching would require API redesign** - Supporting user-defined transaction scopes would require all CRDT types to accept transaction parameters, which would be a major breaking change

#### Current Transaction Capabilities
- ✅ Automatic transaction creation and commit for every operation
- ✅ Observer notifications after transaction commits
- ✅ Transaction origin tracking in events
- ✅ Thread-safe transaction handling in Rust layer
- ✅ Memory-efficient transaction lifecycle management

#### What's Not Supported (By Design)
- ❌ Explicit `begin()` / `commit()` / `rollback()` API - Not applicable to CRDT architecture
- ❌ User-controlled transaction boundaries - Would require complete API redesign
- ❌ Nested transactions - Not supported by underlying yrs library
- ❌ Transaction isolation levels - CRDTs use different concurrency model

**Conclusion:** Transaction support is fully integrated into the architecture. The automatic transaction semantics provide the benefits of transactional operations (atomicity, observer consistency) without requiring manual transaction management. This aligns with CRDT best practices and the design of the underlying yrs library.

---

### Phase 3: Advanced Features ✅ COMPLETE

#### ✅ Completed
1. **Hierarchical XML Types** - Full support (see Phase 3.5 above)
2. **Subdocuments** - Full support (see Phase 3.6 above)
3. **Observer/Callback Support** - Full support (see Phase 3.7 above)
4. **Advanced Update Encoding/Decoding** - Full support (see Phase 3.8 above)
5. **Transaction Semantics** - Built into architecture (see Phase 3.9 above)

---

### Phase 4: Production Ready 🔜 TODO

**Target:** v1.0.0 release

1. **Complete Test Coverage**
   - ✅ Memory leak stress tests (25 tests - COMPLETE)
   - 🔜 Concurrent access patterns
   - 🔜 Performance benchmarks
   - 🔜 Integration tests

2. **Multi-Platform Builds**
   - CI/CD for Linux, macOS, Windows
   - Cross-compilation scripts
   - Automated testing on all platforms

3. **Documentation**
   - API reference (in progress)
   - Tutorial guides
   - Best practices
   - Troubleshooting section

4. **Distribution**
   - Maven Central publishing
   - Release process
   - Versioning strategy
   - Fat JAR vs platform-specific JARs

---

## Test Coverage

### Rust Tests: 36 total (100% passing)
- lib.rs + ydoc.rs: 3 tests
- ytext.rs: 4 tests
- yarray.rs: 6 tests (including 2 subdocument tests)
- ymap.rs: 5 tests (including 1 subdocument test)
- yxmltext.rs: 7 tests (including 3 formatting tests)
- yxmlelement.rs: 4 tests
- yxmlfragment.rs: 7 tests (including child retrieval)

### Java Tests: 306 total (100% passing)
- YDocTest: 29 tests (13 original + 16 advanced update encoding/decoding)
- YTextTest: 23 tests
- YArrayTest: 27 tests
- YMapTest: 30 tests
- YXmlTextTest: 41 tests (14 formatting + 7 ancestor lookup)
- YXmlElementTest: 55 tests (18 nested element + 12 ancestor lookup)
- YXmlFragmentTest: 9 tests
- StressTest: 25 tests (memory stress tests)
- SubdocumentTest: 16 tests (subdocument functionality)
- YTextObserverIntegrationTest: 14 tests
- YArrayObserverIntegrationTest: 13 tests
- YMapObserverIntegrationTest: 13 tests
- YXmlElementObserverIntegrationTest: 4 tests
- YXmlFragmentObserverIntegrationTest: 4 tests
- YXmlTextObserverIntegrationTest: 3 tests
- YObserverTest: 9 tests (basic observer API tests)

**Functional Test Coverage:** Creation, lifecycle, synchronization, error handling, Unicode/emoji, mixed types, XML attributes, child management, rich text formatting, ancestor lookup, bidirectional sync, complex editing sequences, subdocument insertion/retrieval, hierarchical structures, observer callbacks, change events, subscription lifecycle

**Stress Test Coverage:**
- Create/close cycles (1,000 iterations per type)
- Large documents (10,000 elements)
- Deep XML nesting (100+ levels)
- Wide XML trees (1,000 children)
- Many attributes (1,000 per element)
- Complex synchronization scenarios
- Combined multi-type operations
- **Result:** All tests passing, no memory leaks detected

---

## Build System

### Current Setup ✅
- **Multi-module Gradle project:**
  - `ycrdt` module: Core Y-CRDT JNI bindings with Rust native library
  - `yprosemirror` module: ProseMirror integration (depends on ycrdt)
- **Cargo:** cdylib library (libycrdt_jni) in ycrdt module
- **Gradle:** Rust integration tasks (buildRustLibrary, copyNativeLibrary, cleanRust, testRust)
- **NativeLoader:** Platform detection (Linux, macOS, Windows × x86_64/aarch64)
- **CI/CD:** GitHub Actions (Quick Check, CI, Release, Javadoc)
- **Publishing:** Separate Maven artifacts for each module

### Module Structure
```
y-crdt-jni/
├── build.gradle (root configuration)
├── settings.gradle (module declarations)
├── ycrdt/ (core CRDT bindings)
│   ├── build.gradle
│   ├── Cargo.toml
│   └── src/ (Java + Rust)
└── yprosemirror/ (ProseMirror integration)
    ├── build.gradle
    └── src/ (Java only)
```

### TODO 🚧
- Cross-compilation scripts
- Multi-platform JAR packaging
- Maven Central publishing for both modules

---

## Documentation

### ✅ Completed
- **README.md:** Quick start guide, API overview, examples
- **IMPLEMENTATION.md:** Technical implementation details
- **PLAN.md:** This document
- **JavaDoc:** All classes documented, published to GitHub Pages (https://carcdr.net/y-crdt-jni/)
- **Example.java:** 14 comprehensive examples
- **CHANGELOG.md:** Version history

### 🔜 TODO
- Tutorial guides
- Best practices documentation
- Troubleshooting section
- Performance optimization guide

---

## Technology Stack

### Rust
- **yrs:** v0.21.3 (Y-CRDT implementation)
- **jni:** v0.21.1 (JNI bindings)
- **Tooling:** cargo fmt, clippy (zero warnings)

### Java
- **Version:** Java 11+
- **Testing:** JUnit 4.13.2
- **Quality:** Checkstyle (Google Java Style Guide)

### Build
- **Gradle:** 8.x
- **CI/CD:** GitHub Actions

---

## Design Considerations

### Memory Management
- **Java:** Closeable pattern with try-with-resources
- **Rust:** Box ownership, proper cleanup in nativeDestroy()
- **Safety:** Pointer validation, closed state tracking

### Threading Model
- **Current:** Not thread-safe, requires external synchronization
- **Future:** Document-level locking, transaction boundaries

### Performance
- **Strategy:** Minimize JNI boundary crossings
- **Future:** Batching operations, zero-copy strategies

### Error Handling
- **Rust:** Validation, exception throwing
- **Java:** IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException, RuntimeException

---

## Success Criteria

### Phase 1 ✅ MET
- YDoc accessible from Java with full API
- Basic memory management working
- Build system functioning
- Tests passing, documentation complete

### Phase 2 ✅ MET
- All core types accessible (YText, YArray, YMap)
- Synchronization working
- Unicode support
- Comprehensive test coverage (80 tests)

### Phase 3.5 ✅ MET
- Hierarchical XML with full tree navigation
- Rich text formatting
- Complex tree synchronization
- Comprehensive test coverage (105 XML tests)

### Phase 3 ✅ MET
- ✅ Complete hierarchical XML API
- ✅ Subdocument support
- ✅ Observer/callback support (51 tests)
- 🔜 Transaction support (deferred to future)

### Overall Target 🎯
- ✅ All core y-crdt types accessible
- ✅ Subdocument support for hierarchical structures
- ✅ Observer API for real-time change notifications
- ✅ No memory leaks in stress tests (25 tests passing)
- 🔜 Performance overhead < 20% vs native Rust (not yet benchmarked)
- 🚧 Multi-platform support
- ✅ Comprehensive test coverage (>80%) - Currently 326 tests (100% passing)
- ✅ Production-ready documentation

---

## Recent Achievements (2025-10-16)

### YProseMirror Module Initialization ✅
- Set up yprosemirror module with prosemirror-kotlin dependencies
- Dependencies: model, state, transform (v1.1.13) + kotlin-stdlib (v1.9.22)
- Created package structure: net.carcdr.yprosemirror
- Implemented converter class stubs (ProseMirrorConverter, YCrdtConverter)
- Verified Kotlin-Java interoperability with 2 passing tests
- **Ready for Phase 1 implementation:** ProseMirror ↔ Y-CRDT conversion
- **Reference:** See `plans/YPROSEMIRROR_PLAN.md` for full implementation plan

### Observer API Implementation ✅
- Implemented complete observer/event system for all 6 CRDT types
- Java API: YObserver, YEvent, YChange hierarchy, YSubscription
- Rust JNI implementation with thread-safe JVM attachment
- Subscription-based observer lifecycle management
- 51 comprehensive integration tests covering all observer scenarios
- Thread-safe observer registration using ConcurrentHashMap
- Exception isolation preventing observer errors from affecting other observers
- **Result:** All 51 observer tests passing, total test count increased to 326 (36 Rust + 290 Java)

### Test Output Configuration ✅
- Added test logging configuration to ycrdt and yprosemirror modules
- Individual test pass/fail/skip status displayed
- Test summary showing total count, passed, failed, skipped
- Full exception traces on failures
- Gradle task dependencies properly configured (testRust runs before Java tests)
- Clear distinction between Rust tests (36) and Java tests (290)

### Development Guidelines ✅
- Created comprehensive .claude/CLAUDE.md for contributors
- Multi-module project structure documentation
- Per-module and all-modules command examples
- Test output explanation and troubleshooting
- Pre-commit checklist with all validation steps
- Documentation update guidelines
- Combined Rust + Java workflow examples

##

### Multi-Module Project Structure ✅
- Restructured as multi-module Gradle project
- Created `ycrdt` module with existing functionality
- Created `yprosemirror` module for ProseMirror integration
- Independent build and publish capabilities per module
- Maven artifacts: `net.carcdr:ycrdt` and `net.carcdr:yprosemirror`
- Updated .gitignore for multi-module structure
- Updated documentation (README, PLAN, CHANGELOG)

### Subdocument Support ✅
- Implemented full subdocument functionality for YMap and YArray
- YMap: `setDoc()`, `getDoc()` methods
- YArray: `insertDoc()`, `pushDoc()`, `getDoc()` methods
- 5 new native methods across YMap (2) and YArray (3)
- 16 comprehensive tests covering insertion, retrieval, nesting, synchronization
- Enables hierarchical document structures and composable architecture
- Full CRDT type support within subdocuments
- Proper memory management with Closeable pattern
- **Result:** All 16 tests passing, total test count increased to 275 (36 Rust + 239 Java)

### Memory Stress Tests ✅
- Added comprehensive stress test suite with 25 tests
- Create/close cycles: 1,000 iterations per type (YDoc, YText, YArray, YMap, XML types)
- Large documents: 10,000 elements in text/array/map
- Deep XML nesting: 100+ levels tested
- Wide XML trees: 1,000 children per element
- Many attributes: 1,000 attributes per element
- Complex synchronization: 100 document synchronizations
- Combined operations: Multiple types in single document
- **Result:** All 25 tests passing, zero memory leaks detected

### Ancestor Lookup for XML Nodes ✅
- Implemented getParent() and getIndexInParent() for YXmlElement and YXmlText
- Polymorphic parent handling (returns YXmlElement or YXmlFragment)
- 19 new tests (12 YXmlElement + 7 YXmlText)
- Parent references synchronized across documents
- Enables full upward tree navigation

### Nested Element Support ✅
- Hierarchical XML with childCount(), insertElement(), insertText(), getChild(), removeChild()
- Deeply nested structures (5+ levels tested)
- 18 comprehensive tests
- Enables HTML/SVG editors and complex document structures

### Rich Text Formatting ✅
- insertWithAttributes() and format() methods
- Arbitrary formatting attributes (bold, italic, color, custom)
- Format removal via null values
- 14 comprehensive tests
- Formatting synchronized across documents

---

## Future Enhancements (Post-v1.0)

### Advanced XML Features
- XPath/CSS selector support
- Event observers on subtrees
- Diff/patch operations
- Undo/redo support
- Alternative serialization formats (JSON, YAML)

### Performance
- Caching and index structures
- Zero-copy optimizations
- Batched operations API

### Additional CRDT Types
- Boolean, integer support
- Nested types (arrays of arrays)
- Custom type serialization

---

## Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history.

**Current Version:** Pre-release development (targeting v0.1.0)
**Dependencies:**
- yrs v0.21.3
- jni v0.21.1
- Java 11+
