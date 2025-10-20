# yhocuspocus - Transport-Agnostic Collaborative Editing Server

Transport-agnostic collaborative editing server for Y-CRDT, inspired by [Hocuspocus](https://github.com/ueberdosis/hocuspocus) but designed for the JVM with any transport layer.

## Features

- **Transport-Agnostic**: Works with WebSocket, HTTP, SSE, or custom protocols
- **Y.js Compatible**: lib0-compatible message protocol
- **Document Lifecycle**: Automatic loading, unloading, and persistence
- **Connection Management**: Client authentication, keepalive, multiplexing
- **Sync Protocol**: Differential updates with state vectors
- **Awareness Protocol**: User presence tracking
- **Stateless Messaging**: Custom application-level messages
- **Extension System**: Hook-based customization with 12 lifecycle hooks
- **Production-Ready**: 122 comprehensive tests (119 core + 3 WebSocket), 100% passing

## Status

- ✅ Phase 1: Core Infrastructure (COMPLETE)
- ✅ Phase 2: Connection Management (COMPLETE)
- ✅ Phase 3: Sync Protocol (COMPLETE)
- ✅ Phase 4: Awareness & Stateless Messages (COMPLETE)
- ✅ Phase 5: Extension System (COMPLETE)
- ✅ Phase 6: Persistence & Debouncing (COMPLETE)
- ✅ Phase 7: WebSocket Transport (COMPLETE)
- ⏳ Phase 8: Testing & Documentation (In Progress)

See [YHOCUSPOCUS_PLAN.md](../plans/YHOCUSPOCUS_PLAN.md) for full roadmap.

## Requirements

- Java 21 or higher
- ycrdt module (included as dependency)

## Installation

Currently not published to Maven Central. Build from source:

```bash
./gradlew :yhocuspocus:build
```

The JAR will be in `yhocuspocus/build/libs/yhocuspocus.jar`.

## Quick Start

### Create a Server

```java
import net.carcdr.yhocuspocus.core.*;
import net.carcdr.yhocuspocus.transport.*;

public class CollaborativeServer {
    public static void main(String[] args) {
        // Create server
        YHocuspocus server = new YHocuspocus();

        // Handle incoming connection (your transport provides this)
        Transport transport = createTransport(); // Your implementation
        ClientConnection connection = server.handleConnection(
            transport,
            Map.of("userId", "user123")
        );

        // Server manages documents automatically
        // Connections sync with Y-CRDT documents
        // Documents unload when last connection closes
    }
}
```

### Implement a Transport

```java
import net.carcdr.yhocuspocus.transport.*;

public class MyTransport implements Transport {
    private final String connectionId;
    private final MyConnection underlying;

    @Override
    public CompletableFuture<Void> send(byte[] message) {
        return CompletableFuture.runAsync(() ->
            underlying.sendBytes(message)
        );
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public void close(int code, String reason) {
        underlying.close(code, reason);
    }

    @Override
    public boolean isOpen() {
        return underlying.isConnected();
    }

    @Override
    public String getRemoteAddress() {
        return underlying.getRemoteAddress();
    }

    @Override
    public void close() {
        close(1000, "Normal closure");
    }
}
```

### Message Flow

```
Client → Transport → ClientConnection
                     ↓
                 Authentication
                     ↓
              DocumentConnection
                     ↓
                 YDocument (Y-CRDT)
                     ↓
            Broadcast to other clients
```

## Architecture

### Core Components

- **YHocuspocus** - Main server orchestrator
  - Document lifecycle management
  - Connection routing
  - Extension execution (Phase 5)

- **ClientConnection** - Transport-level connection
  - One per client
  - Handles multiple documents
  - Authentication
  - Keepalive

- **DocumentConnection** - Per-document connection
  - One per document per client
  - Sync protocol handling
  - Awareness updates
  - Stateless messages

- **YDocument** - Document wrapper
  - Wraps YDoc from ycrdt module
  - Connection tracking
  - Awareness state
  - Broadcasting

### Protocol

YHocuspocus implements the Y.js message protocol using lib0-compatible encoding:

Message types:
- `SYNC` (0) - Y.js sync protocol
- `AWARENESS` (1) - User presence updates
- `AUTH` (2) - Authentication
- `QUERY_AWARENESS` (3) - Request awareness state
- `SYNC_REPLY` (4) - Sync response
- `STATELESS` (5) - Custom messages
- `BROADCAST_STATELESS` (6) - Broadcast custom messages
- `CLOSE` (7) - Graceful close
- `SYNC_STATUS` (8) - Sync acknowledgment

Message structure:
```
[documentName: varString]
[messageType: varUint]
[...payload]
```

### Sync Protocol

1. **Initial Sync**:
   - Client connects
   - Server sends full document state
   - Client applies state

2. **Incremental Updates**:
   - Client makes change
   - Client sends update
   - Server applies update
   - Server broadcasts to other clients
   - CRDT ensures convergence

3. **Differential Sync**:
   - Uses state vectors for efficiency
   - Only sends missing operations
   - Reduces bandwidth

## API

### YHocuspocus

Main server class:

```java
public class YHocuspocus implements AutoCloseable {
    public YHocuspocus();

    public ClientConnection handleConnection(
        Transport transport,
        Map<String, Object> initialContext
    );

    public YDocument getDocument(String name);

    public void close();
}
```

### Transport Interface

Abstraction for any transport layer:

```java
public interface Transport extends AutoCloseable {
    CompletableFuture<Void> send(byte[] message);
    String getConnectionId();
    void close(int code, String reason);
    boolean isOpen();
    String getRemoteAddress();
}
```

### MockTransport

Included for testing:

```java
public class MockTransport implements Transport {
    public List<byte[]> getSentMessages();
    public void receiveMessage(byte[] message);
}
```

## Testing

```bash
# Run all tests
./gradlew :yhocuspocus:test

# Run with checkstyle
./gradlew :yhocuspocus:check
```

Test coverage:
- 18 message protocol tests (encoding, decoding, message types)
- 18 variable-length encoding tests (lib0 compatibility)
- 11 lib0 encoding compatibility tests (edge cases, cross-platform)
- 17 VarInt encoding tests (basic encoding/decoding)
- 14 sync protocol tests (differential sync, state vectors)
- 12 awareness tests (presence, state management, clock ordering)
- 16 connection integration tests (lifecycle, multiplexing)
- 7 sync integration tests (CRDT merge, broadcasting, read-only)
- 6 awareness/stateless integration tests (presence, custom messages)
- 9 extension integration tests (hooks, persistence, context)
- 14 extension system tests (priority, payload, database)
- 3 WebSocket transport tests (server lifecycle, message transmission)

Total: 122 tests, 100% passing.

## Use Cases

### Collaborative Document Editing

- Multiple users editing same document
- Real-time synchronization
- Conflict resolution via CRDT

### Multiplayer Applications

- Shared game state
- Real-time collaboration
- State synchronization

### Multi-Device Sync

- Single user, multiple devices
- Automatic state synchronization
- Offline support (with extensions)

## Examples

See test files for usage examples:
- `ConnectionIntegrationTest.java` - Connection management
- `SyncIntegrationTest.java` - Sync protocol
- `AwarenessStatelessIntegrationTest.java` - Awareness and messaging

## Development

```bash
# Run checkstyle
./gradlew :yhocuspocus:checkstyleMain :yhocuspocus:checkstyleTest

# Build JAR
./gradlew :yhocuspocus:build
```

See [IMPLEMENTATION.md](IMPLEMENTATION.md) for technical details.

## Roadmap

### Completed (Phase 1-7)
- ✅ Phase 1: Core Infrastructure (transport abstraction, message protocol)
- ✅ Phase 2: Connection Management (auth, multiplexing, keepalive)
- ✅ Phase 3: Sync Protocol (Y.js compatible synchronization)
- ✅ Phase 4: Awareness & Stateless Messages (presence, custom messaging)
- ✅ Phase 5: Extension System (12 lifecycle hooks, DatabaseExtension)
- ✅ Phase 6: Persistence & Debouncing (debounced saving, immediate save on unload)
- ✅ Phase 7: WebSocket Transport (Jetty 12 reference implementation)

### In Progress
- ⏳ Phase 8: Testing & Documentation (comprehensive testing, API docs, user guide)

See [YHOCUSPOCUS_PLAN.md](../plans/YHOCUSPOCUS_PLAN.md) for details.

## Design Philosophy

### Transport Agnostic

Unlike Hocuspocus (WebSocket-centric), yhocuspocus works with any transport:
- WebSocket
- HTTP long-polling
- Server-Sent Events (SSE)
- Custom protocols

### Extension-Based

Planned extension system (Phase 5) enables:
- Custom authentication
- Database persistence
- Redis pub/sub for scaling
- Logging and metrics
- Custom message handling

### Production-Ready

- Comprehensive test coverage
- Checkstyle compliance
- JavaDoc documentation
- Race condition prevention
- Resource management

## Known Limitations

1. No horizontal scaling support yet (Redis extension planned for post-v1.0)
2. Documentation incomplete (Phase 8 in progress)
3. Single-instance only (no distributed locking)

## License

Apache License 2.0
