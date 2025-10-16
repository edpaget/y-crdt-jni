# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project structure
- YDoc implementation with JNI bindings
  - Document creation, client ID management, GUID support
  - State encoding/decoding for synchronization
  - 13 comprehensive tests
- YText implementation with JNI bindings
  - Collaborative text editing with insert, push, delete operations
  - Unicode and emoji support with Modified UTF-8 handling
  - 23 comprehensive tests
- YArray implementation with JNI bindings
  - Collaborative array with support for strings and doubles
  - Push, insert, delete, and get operations
  - JSON serialization support
  - 27 comprehensive tests
- YMap implementation with JNI bindings
  - Collaborative map with support for strings and doubles
  - Set, get, remove, containsKey, keys, clear operations
  - JSON serialization support
  - 30 comprehensive tests
- YXmlText implementation with JNI bindings
  - Collaborative XML text editing with insert, push, delete operations
  - Unicode and emoji support
  - XmlFragmentRef-based implementation for proper CRDT synchronization
  - 20 comprehensive tests
- YXmlElement implementation with JNI bindings
  - Collaborative XML elements with attribute management
  - Get tag, get/set/remove attributes, get all attribute names
  - XML string representation support
  - XmlFragmentRef-based implementation for proper CRDT synchronization
  - 25 comprehensive tests
- YXmlText rich text formatting support
  - Insert text with formatting attributes (insertWithAttributes)
  - Apply formatting to existing text ranges (format)
  - Support for arbitrary formatting attributes (bold, italic, color, font, custom)
  - Format removal via null attribute values
  - Map<String, Object> to Attrs conversion with support for Boolean, Integer, Long, Double, String
  - Formatting synchronized across documents
  - 14 comprehensive formatting tests
- YXmlFragment implementation with JNI bindings
  - Hierarchical XML tree support with child node retrieval
  - Insert and remove element/text children at specific indices
  - Retrieve child nodes by index with type checking
  - Direct pointer architecture for efficient child access
  - XML string representation of entire fragment
  - 27 comprehensive tests
- YXmlElement nested element support
  - Insert element and text children at any index (insertElement, insertText)
  - Get child count (childCount)
  - Retrieve child nodes by index with type detection (getChild)
  - Remove children at specific indices (removeChild)
  - Support for deeply nested hierarchical XML structures
  - Polymorphic child handling (elements and text nodes)
  - 18 comprehensive nested element tests
- YXmlElement and YXmlText ancestor lookup support
  - Get parent node for any element or text node (getParent)
  - Get index within parent's children (getIndexInParent)
  - Navigate upward through XML tree hierarchy
  - Parent can be YXmlElement or YXmlFragment
  - Synchronized parent references across documents
  - 19 comprehensive ancestor lookup tests (12 for YXmlElement, 7 for YXmlText)
- Subdocument support for hierarchical document structures
  - YMap subdocument methods: setDoc(), getDoc()
  - YArray subdocument methods: insertDoc(), pushDoc(), getDoc()
  - 5 new native methods (2 for YMap, 3 for YArray)
  - Embed YDoc instances within YMap and YArray collections
  - Full CRDT type support within subdocuments (YText, YArray, YMap, YXml*)
  - Hierarchical document composition and modular architecture
  - Synchronization of subdocument structures across clients
  - Proper memory management with Closeable pattern
  - 16 comprehensive subdocument tests
  - Package-private YDoc constructor for wrapping native pointers
- Memory stress tests
  - 25 comprehensive stress tests for all CRDT types
  - Create/close cycles (1,000 iterations per type)
  - Large documents (10,000 elements)
  - Deep XML nesting (100+ levels)
  - Wide XML trees (1,000 children)
  - Many attributes (1,000 attributes per element)
  - Complex synchronization scenarios
  - Combined multi-type stress tests
  - All tests passing with no memory leaks detected
- Basic memory management with Closeable pattern across all types
- Native library loader with platform detection
- Multi-module Gradle build system with Rust integration
  - `ycrdt` module: Core Y-CRDT JNI bindings (Java + Rust)
  - `yprosemirror` module: ProseMirror integration (Java only, depends on ycrdt)
  - Independent build and publish capabilities per module
  - Maven artifacts: `net.carcdr:ycrdt` and `net.carcdr:yprosemirror`
- Observer API for real-time change notifications
  - Complete observer support for all 6 CRDT types (YText, YArray, YMap, YXmlFragment, YXmlElement, YXmlText)
  - YObserver interface with onChange callback
  - YEvent with typed change information
  - Type-specific change classes: YTextChange, YArrayChange, YMapChange, YXmlElementChange
  - YSubscription handles with AutoCloseable support
  - Thread-safe observer registration and callbacks
  - 51 comprehensive observer integration tests
  - Full Rust JNI implementation with proper JVM thread attachment
- Advanced Update Encoding/Decoding for efficient synchronization
  - State vector encoding: `encodeStateVector()` - compact representation of observed changes
  - Differential updates: `encodeDiff(byte[] stateVector)` - generate updates with only unseen changes
  - Update merging: `mergeUpdates(byte[][] updates)` - combine multiple updates into single compact update
  - State vector extraction: `encodeStateVectorFromUpdate(byte[] update)` - inspect update contents without applying
  - 4 new public methods in YDoc with full JavaDoc documentation
  - 4 new native JNI methods in Rust (encodeStateVector, encodeDiff, mergeUpdates, encodeStateVectorFromUpdate)
  - Uses yrs v1 encoding format for maximum compatibility
  - Enables efficient peer-to-peer synchronization, client-server differential updates, offline-first applications
  - 16 comprehensive tests covering state vectors, differential sync, update merging, error handling
- Automatic Transaction Semantics built into architecture
  - All CRDT operations are automatically transactional (atomic)
  - Each operation creates, executes, and commits a transaction transparently
  - Observer callbacks fire after transaction commits
  - Transaction origin tracking in YEvent for observer context
  - No manual transaction management needed - handled automatically
  - Aligns with CRDT best practices and yrs library design
  - Note: Traditional begin/commit/rollback not applicable to CRDT append-only architecture
- Comprehensive test suite (36 Rust tests, 306 Java tests - 100% passing)
  - 214 functional/integration tests
  - 25 memory stress tests
  - 16 subdocument tests
  - 51 observer integration tests
  - 16 advanced update encoding/decoding tests
- Example program with 14 examples demonstrating all features
- GitHub Actions CI/CD workflows (Quick Check, CI, Release, Javadoc)
- Multi-platform build support (Linux, macOS, Windows)
- Javadoc published to GitHub Pages
- Test output configuration showing individual test results and summaries
- Development guidelines in .claude/CLAUDE.md for contributors
- YProseMirror module initialization (Phase 1 setup)
  - Project structure with net.carcdr.yprosemirror package
  - ProseMirror dependencies: model, state, transform (v1.1.13)
  - Kotlin-Java interoperability configured with kotlin-stdlib
  - Initial converter class stubs (ProseMirrorConverter, YCrdtConverter)
  - Kotlin interop tests verifying prosemirror-kotlin integration
  - Ready for Phase 1 implementation (PM ↔ Y-CRDT conversion)

### Fixed
- Critical bug in `encodeStateAsUpdate()` - now encodes against empty state vector for correct synchronization
- Architecture unification - YXmlElement and YXmlText now use direct XmlElementRef/XmlTextRef pointers consistently across both root-level and child retrieval APIs

## [0.1.0] - TBD

### Added
- Initial release
- YDoc support with full CRDT operations
- YText support for collaborative text editing
- YArray support for collaborative arrays
- YMap support for collaborative maps
- YXmlText support for collaborative XML text
- YXmlElement support for collaborative XML elements with attributes
- Multi-platform native libraries (Linux, macOS, Windows)
- JAR distribution with embedded native libraries
- Comprehensive documentation and examples

## Future Releases

### [0.2.0] - Planned
- Additional type support (boolean, integer, nested types)
- Enhanced error reporting
- Performance optimizations

### [0.3.0] - Planned
- Observer/callback support
- Transaction support
- Additional XML features (nested elements, complex types)

### [1.0.0] - Planned
- Complete CRDT type coverage
- Production-ready performance
- Comprehensive documentation
- Maven Central publishing
