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
- Basic memory management with Closeable pattern across all types
- Native library loader with platform detection
- Gradle build system with Rust integration
- Comprehensive test suite (33 Rust tests, 198 Java tests - 100% passing)
- Example program with 14 examples demonstrating all features
- GitHub Actions CI/CD workflows (Quick Check, CI, Release, Javadoc)
- Multi-platform build support (Linux, macOS, Windows)
- Javadoc published to GitHub Pages

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
