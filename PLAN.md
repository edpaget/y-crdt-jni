# Plan for Packaging y-crdt Rust Library for JVM

## Overview
This document outlines the plan for creating JNI bindings to expose the y-crdt (yrs) Rust library for use from the JVM (Java/Kotlin).

**Status:** Phases 1, 2, and 3.5 Complete | Phase 3 In Progress | Last Updated: 2025-10-16

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
- **Testing:** 275 total tests (36 Rust + 239 Java), 100% passing
  - 198 functional/integration tests
  - 25 memory stress tests (no leaks detected)
  - 16 subdocument tests
- **Documentation:** Comprehensive JavaDoc, IMPLEMENTATION.md, published to GitHub Pages
- **Build System:** Gradle + Cargo integration, GitHub Actions CI/CD
- **Memory Management:** Closeable pattern, proper native resource cleanup, stress tested

### 🚧 In Progress
- **Phase 3:** Advanced features (observers, transactions, advanced state management)
- **Build Artifacts:** Multi-platform distribution

### 🔜 Planned
- **Phase 4:** Production readiness (stress testing, benchmarks, Maven Central)

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

### Phase 3: Advanced Features 🚧 IN PROGRESS

#### ✅ Completed
1. **Hierarchical XML Types** - Full support (see Phase 3.5 above)
2. **Subdocuments** - Full support (see Phase 3.6 above)

#### 🔜 TODO
3. **Advanced Update Encoding/Decoding**
   - State vectors
   - Differential updates
   - Update merging

4. **Observer/Callback Support**
   - Event subscription
   - Change notifications
   - Callback lifecycle management

5. **Transaction Support**
   - Transaction begin/commit/rollback
   - Batch operations
   - Transaction observers

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

### Java Tests: 239 total (100% passing)
- YDocTest: 13 tests
- YTextTest: 23 tests
- YArrayTest: 27 tests
- YMapTest: 30 tests
- YXmlTextTest: 41 tests (14 formatting + 7 ancestor lookup)
- YXmlElementTest: 55 tests (18 nested element + 12 ancestor lookup)
- YXmlFragmentTest: 9 tests
- StressTest: 25 tests (memory stress tests)
- SubdocumentTest: 16 tests (subdocument functionality)

**Functional Test Coverage:** Creation, lifecycle, synchronization, error handling, Unicode/emoji, mixed types, XML attributes, child management, rich text formatting, ancestor lookup, bidirectional sync, complex editing sequences, subdocument insertion/retrieval, hierarchical structures

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

### Phase 3 🚧 IN PROGRESS
- ✅ Complete hierarchical XML API
- ✅ Subdocument support
- 🔜 Observer/callback support
- 🔜 Transaction support

### Overall Target 🎯
- ✅ All core y-crdt types accessible
- ✅ Subdocument support for hierarchical structures
- ✅ No memory leaks in stress tests (25 tests passing)
- 🔜 Performance overhead < 20% vs native Rust (not yet benchmarked)
- 🚧 Multi-platform support
- ✅ Comprehensive test coverage (>80%) - Currently 275 tests (100% passing)
- ✅ Production-ready documentation

---

## Recent Achievements (2025-10-16)

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
