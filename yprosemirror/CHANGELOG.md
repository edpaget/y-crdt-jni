# Changelog - yprosemirror

All notable changes to the yprosemirror module.

## [Unreleased]

### Added
- Phase 5.1: Incremental Updates (2025-10-19)
  - `DocumentDiff` utility class for detecting changes between ProseMirror documents
    - Change types: Insert, Delete, Replace
    - Uses YPosition for accurate position tracking
    - Immutable change objects
  - `IncrementalUpdater` for applying differential changes to Y-CRDT
    - Processes changes in reverse order for position accuracy
    - Navigates Y-CRDT tree using YPosition
    - Efficient partial document updates
  - Enhanced `YProseMirrorBinding` with incremental update support
    - New constructor with `useIncrementalUpdates` boolean parameter
    - Tracks `lastDocument` for diffing
    - Backward compatible (default behavior unchanged)
    - Dual mode: full replacement or incremental updates
  - 14 comprehensive tests in IncrementalUpdateTest
  - 54 total tests passing (7 conversion + 2 interop + 18 position + 13 binding + 14 incremental)

- Phase 3: Position Tracking (2025-10-17)
  - `YPosition` class for position tracking through document tree
    - Path-based navigation (array of indices)
    - Offset for text positions
    - Immutability and value equality
    - Child navigation: child(index), parent()
  - 18 comprehensive tests in YPositionTest

- Phase 2: Bidirectional Synchronization (2025-10-16)
  - `YProseMirrorBinding` for two-way sync between ProseMirror and Y-CRDT
    - Observer-based change detection
    - Prevents infinite update loops
    - AutoCloseable for proper resource management
    - Thread-safe implementation
  - 13 comprehensive tests in YProseMirrorBindingTest

- Phase 1: Core Conversion (2025-10-16)
  - Project structure with net.carcdr.yprosemirror package
  - ProseMirror dependencies: model, state, transform (v1.1.13)
  - Kotlin-Java interoperability configured with kotlin-stdlib
  - `ProseMirrorConverter` for converting ProseMirror documents to Y-CRDT XML structures
    - nodeToYXml() converts ProseMirror Node to YXmlFragment
    - fragmentToYXml() converts ProseMirror Fragment to YXmlFragment
    - prosemirrorToYDoc() convenience method for document conversion
    - Preserves node hierarchy, attributes, text content, and marks (formatting)
    - Marks converted to Y-CRDT text formatting attributes
    - Recursive tree traversal for nested structures
  - `YCrdtConverter` for converting Y-CRDT XML structures to ProseMirror documents
    - yXmlToNode() converts YXmlFragment to ProseMirror Node
    - yXmlElementToNode() converts YXmlElement to ProseMirror Node
    - yDocToProsemirror() convenience method for document retrieval
    - Preserves XML hierarchy, tags, attributes, and text content
    - Formatting attributes converted to ProseMirror marks
    - Support for nested elements and complex document structures
  - YXmlFragment.getChild() generic method for cleaner API (returns YXmlElement or YXmlText)
  - 7 comprehensive tests in ConversionTest
  - 2 Kotlin interoperability tests
  - 100% checkstyle compliance
  - Full JavaDoc documentation for all public APIs

## [0.1.0] - TBD

Initial release planned.
