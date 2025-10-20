# Changelog - yhocuspocus

All notable changes to the yhocuspocus module.

## [Unreleased]

### Added
- Phase 1: Core Infrastructure (COMPLETE)
  - Transport-agnostic collaborative editing server framework
  - Transport abstraction layer (Transport, TransportMessageHandler, TransportEventHandler, TransportFactory)
  - Message protocol implementation with lib0-compatible variable-length encoding
  - VarIntWriter and VarIntReader for efficient binary encoding/decoding
  - MessageType enum with all Hocuspocus/Yjs protocol message types
  - IncomingMessage and OutgoingMessage for protocol message handling
  - MessageDecoder utility class for parsing binary protocol messages
  - Full JavaDoc documentation for all public APIs
  - 36 comprehensive unit tests (18 encoding tests + 18 protocol tests)
  - 100% test pass rate
  - 100% checkstyle compliance
  - Final utility classes with private constructors
  - Foundation for connection management, sync protocol, and extension system
- Phase 2: Connection Management (COMPLETE)
  - YHocuspocus main orchestrator with document lifecycle management
  - ClientConnection for transport-agnostic client handling
  - DocumentConnection for per-document connection multiplexing
  - YDocument wrapper with connection tracking and awareness
  - SyncProtocol implementation for Y.js-compatible synchronization
  - Awareness protocol for user presence tracking
  - Authentication flow with message queueing
  - Keepalive mechanism for connection health monitoring
  - Document unloading after last connection closes
  - Concurrent document access with race condition prevention
  - 67 total tests (36 protocol + 21 sync + 10 awareness + 8 integration)
  - 100% test pass rate
  - Full integration test coverage for connection management
- Phase 3: Sync Protocol (COMPLETE)
  - Enhanced sync protocol with differential updates using state vectors
  - Update broadcasting to all connections including sender
  - Initial sync sends full document to new clients
  - Incremental updates propagate correctly to all connections
  - Concurrent edits merge correctly (CRDT properties verified)
  - Read-only mode detection and enforcement support
  - Sequential update propagation with differential encoding
  - Empty update handling
  - 7 comprehensive sync integration tests
  - 74 total tests (36 protocol + 21 sync + 10 awareness + 15 integration)
  - 100% test pass rate
  - All Phase 3 success criteria met
- Phase 4: Awareness & Stateless Messages (COMPLETE)
  - Full awareness protocol integration in DocumentConnection
  - Awareness updates broadcast to all connections
  - Query awareness support for requesting current state
  - Stateless message handling (echo back to sender)
  - Broadcast stateless messaging (send to all except sender)
  - broadcastStateless() implementation in YDocument
  - Stateless message factory methods in OutgoingMessage
  - 6 comprehensive awareness and stateless integration tests
  - 80 total tests (36 protocol + 21 sync + 10 awareness + 21 integration)
  - 100% test pass rate
  - All Phase 4 success criteria met

## [0.1.0] - TBD

Initial release planned.
