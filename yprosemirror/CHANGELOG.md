# Changelog - yprosemirror

All notable changes to the yprosemirror module.

## [Unreleased]

### Added
- Phase 1: Core Conversion (COMPLETE)
  - Project structure with net.carcdr.yprosemirror package
  - ProseMirror dependencies: model, state, transform (v1.1.13)
  - Kotlin-Java interoperability configured with kotlin-stdlib
  - ProseMirrorConverter for converting ProseMirror documents to Y-CRDT XML structures
    - nodeToYXml() converts ProseMirror Node to YXmlFragment
    - fragmentToYXml() converts ProseMirror Fragment to YXmlFragment
    - prosemirrorToYDoc() convenience method for document conversion
    - Preserves node hierarchy, attributes, text content, and marks (formatting)
    - Marks converted to Y-CRDT text formatting attributes
    - Recursive tree traversal for nested structures
  - YCrdtConverter for converting Y-CRDT XML structures to ProseMirror documents
    - yXmlToNode() converts YXmlFragment to ProseMirror Node
    - yXmlElementToNode() converts YXmlElement to ProseMirror Node
    - yDocToProsemirror() convenience method for document retrieval
    - Preserves XML hierarchy, tags, attributes, and text content
    - Formatting attributes converted to ProseMirror marks
    - Support for nested elements and complex document structures
  - YXmlFragment.getChild() generic method for cleaner API (returns YXmlElement or YXmlText)
  - 7 comprehensive tests demonstrating Y-CRDT structure creation
    - Simple document structures (paragraph with text)
    - Nested elements (div > heading > text)
    - Elements with attributes (headings with level, id)
    - Multiple sibling elements (multiple paragraphs)
    - Text formatting attributes (bold, italic marks)
    - Complex document structures (headings + paragraphs)
    - Document synchronization across YDocs
  - 9 total tests passing (7 conversion + 2 Kotlin interop)
  - 100% checkstyle compliance
  - Full JavaDoc documentation for all public APIs

## [0.1.0] - TBD

Initial release planned.
