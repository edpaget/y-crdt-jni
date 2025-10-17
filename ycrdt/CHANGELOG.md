# Changelog - ycrdt

All notable changes to the ycrdt module.

## [Unreleased]

### Added
- YDoc implementation with JNI bindings
  - Document creation with random or specified client ID
  - State encoding/decoding for synchronization
  - GUID support
  - 13 comprehensive tests
- YText implementation with JNI bindings
  - Collaborative text editing (insert, push, delete operations)
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
  - Rich text formatting support with insertWithAttributes() and format()
  - Support for arbitrary formatting attributes (bold, italic, color, font, custom)
  - Map<String, Object> to Attrs conversion with Boolean, Integer, Long, Double, String support
  - Format removal via null attribute values
  - Ancestor lookup support (getParent, getIndexInParent)
  - 41 comprehensive tests (20 basic + 14 formatting + 7 ancestor lookup)
- YXmlElement implementation with JNI bindings
  - Collaborative XML elements with attribute management
  - Get tag, get/set/remove attributes, get all attribute names
  - XML string representation support
  - XmlFragmentRef-based implementation for proper CRDT synchronization
  - Nested element support (insertElement, insertText, getChild, removeChild, childCount)
  - Support for deeply nested hierarchical XML structures
  - Polymorphic child handling (elements and text nodes)
  - Ancestor lookup support (getParent, getIndexInParent)
  - 55 comprehensive tests (25 basic + 18 nested + 12 ancestor lookup)
- YXmlFragment implementation with JNI bindings
  - Hierarchical XML tree support with child node retrieval
  - Insert and remove element/text children at specific indices
  - Retrieve child nodes by index with type checking
  - Direct pointer architecture for efficient child access
  - XML string representation of entire fragment
  - 27 comprehensive tests
- Subdocument support for hierarchical document structures
  - YMap subdocument methods: setDoc(), getDoc()
  - YArray subdocument methods: insertDoc(), pushDoc(), getDoc()
  - 5 new native methods (2 for YMap, 3 for YArray)
  - Embed YDoc instances within YMap and YArray collections
  - Full CRDT type support within subdocuments
  - Hierarchical document composition and modular architecture
  - Synchronization of subdocument structures across clients
  - Proper memory management with Closeable pattern
  - 16 comprehensive subdocument tests
  - Package-private YDoc constructor for wrapping native pointers
- Observer API for real-time change notifications
  - Complete observer support for all 6 CRDT types
  - YObserver interface with onChange callback
  - YEvent with typed change information
  - Type-specific change classes: YTextChange, YArrayChange, YMapChange, YXmlElementChange
  - YSubscription handles with AutoCloseable support
  - Thread-safe observer registration and callbacks
  - 51 comprehensive observer integration tests
  - Full Rust JNI implementation with proper JVM thread attachment
- Advanced Update Encoding/Decoding for efficient synchronization
  - State vector encoding: encodeStateVector()
  - Differential updates: encodeDiff(byte[] stateVector)
  - Update merging: mergeUpdates(byte[][] updates)
  - State vector extraction: encodeStateVectorFromUpdate(byte[] update)
  - 4 new public methods in YDoc with full JavaDoc documentation
  - 4 new native JNI methods in Rust
  - Uses yrs v1 encoding format for maximum compatibility
  - Enables efficient peer-to-peer synchronization and offline-first applications
  - 16 comprehensive tests
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
- Transaction API for batching operations (Phase 1 - Core Infrastructure)
  - YTransaction class implementing AutoCloseable for automatic resource management
  - YDoc.beginTransaction() - create explicit transactions
  - YDoc.transaction(Consumer) - callback-based transaction execution
  - YTransaction.commit() and rollback() methods
  - Thread-safe transaction storage using global HashMap with unique IDs
  - Proper transaction lifecycle management (create, use, commit/rollback)
  - 17 comprehensive transaction tests covering all lifecycle scenarios
  - Automatic transaction semantics still available (each operation creates implicit transaction)
  - Documented limitation: nested transactions not supported (yrs TransactionMut constraint)
  - Performance benefits: fewer JNI calls, single observer notification, efficient update encoding
  - Phase 2 (YText integration) ready for implementation
- Native library loader with platform detection
- Multi-platform build support (Linux, macOS, Windows)
- Example program with 14 examples demonstrating all features
- GitHub Actions CI/CD workflows (Quick Check, CI, Release, Javadoc)
- Javadoc published to GitHub Pages
- Test output configuration showing individual test results
- Development guidelines in .claude/CLAUDE.md

### Fixed
- Critical bug in encodeStateAsUpdate() - now encodes against empty state vector
- Architecture unification - YXmlElement and YXmlText now use direct XmlElementRef/XmlTextRef pointers consistently

## [0.1.0] - TBD

Initial release planned.
