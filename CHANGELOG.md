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
- Basic memory management with Closeable pattern across all types
- Native library loader with platform detection
- Gradle build system with Rust integration
- Comprehensive test suite (12 Rust tests, 63 Java tests - 100% passing)
- Example program with 8 examples demonstrating all features
- GitHub Actions CI/CD workflows (Quick Check, CI, Release, Javadoc)
- Multi-platform build support (Linux, macOS, Windows)
- Javadoc published to GitHub Pages

### Fixed
- Critical bug in `encodeStateAsUpdate()` - now encodes against empty state vector for correct synchronization

## [0.1.0] - TBD

### Added
- Initial release
- YDoc support with full CRDT operations
- YText support for collaborative text editing
- YArray support for collaborative arrays
- Multi-platform native libraries (Linux, macOS, Windows)
- JAR distribution with embedded native libraries
- Comprehensive documentation and examples

## Future Releases

### [0.2.0] - Planned
- YMap implementation
- Additional type support (boolean, integer types)
- Enhanced error reporting

### [0.3.0] - Planned
- Observer/callback support
- Transaction support
- XML types (YXmlText, YXmlElement)

### [1.0.0] - Planned
- Complete CRDT type coverage
- Production-ready performance
- Comprehensive documentation
- Maven Central publishing
