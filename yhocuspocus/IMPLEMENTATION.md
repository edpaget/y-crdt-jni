# Implementation Details - yhocuspocus

Technical implementation details for the yhocuspocus collaborative editing server.

## Architecture

The yhocuspocus module implements a transport-agnostic collaborative editing server:

1. **Transport Layer** - Abstraction for WebSocket, HTTP, SSE, etc.
2. **Connection Management** - Client and document connections
3. **Protocol Layer** - lib0-compatible message encoding
4. **Sync Layer** - Y.js-compatible synchronization
5. **Awareness Layer** - User presence tracking

## Transport Abstraction

### Design Decision

Fully abstract the transport layer to support any protocol.

### Transport Interface

```java
public interface Transport extends AutoCloseable {
    CompletableFuture<Void> send(byte[] message);
    String getConnectionId();
    void close(int code, String reason);
    boolean isOpen();
    String getRemoteAddress();
}
```

### Handler Interfaces

- `TransportMessageHandler` - Receives messages
- `TransportEventHandler` - Handles lifecycle events
- `TransportFactory` - Creates transport instances

### Mock Implementation

`MockTransport` for testing:
- Stores sent messages in list
- Simulates message sending/receiving
- No actual network communication

## Protocol Layer

### Variable-Length Encoding

Implements lib0-compatible encoding for Y.js compatibility.

**VarIntWriter:**
- Writes integers as variable-length bytes
- Writes strings with length prefix
- Writes raw bytes

**VarIntReader:**
- Reads variable-length integers
- Reads length-prefixed strings
- Returns remaining bytes

### Message Types

```java
public enum MessageType {
    SYNC(0),
    AWARENESS(1),
    AUTH(2),
    QUERY_AWARENESS(3),
    SYNC_REPLY(4),
    STATELESS(5),
    BROADCAST_STATELESS(6),
    CLOSE(7),
    SYNC_STATUS(8);
}
```

### Message Structure

All messages have the same structure:
```
[documentName: varString]
[messageType: varUint]
[...payload: bytes]
```

### MessageDecoder

Parses incoming binary messages:
1. Read document name
2. Read message type
3. Extract payload
4. Return `IncomingMessage` object

### OutgoingMessage

Factory methods for creating messages:
- `sync(docName, payload)` - Sync message
- `awareness(docName, payload)` - Awareness update
- `stateless(docName, payload)` - Stateless message
- `broadcastStateless(docName, payload)` - Broadcast stateless
- `syncStatus(docName, synced)` - Sync status

## Connection Management

### YHocuspocus

Main server orchestrator:

**Responsibilities:**
- Document lifecycle (create, load, unload)
- Connection routing
- Race condition prevention

**Document Loading:**
- Uses `ConcurrentHashMap<String, YDocument>` for loaded documents
- Uses `ConcurrentHashMap<String, CompletableFuture<YDocument>>` for loading documents
- Prevents multiple simultaneous loads of same document

### ClientConnection

Transport-level connection:

**Responsibilities:**
- Authentication flow
- Message queueing during auth
- Keepalive mechanism
- Document multiplexing

**Lifecycle:**
```
Create → Authenticate → Route Messages → Close
```

**Message Queueing:**
- Messages queued until authentication completes
- Processed in order after auth succeeds
- Discarded if auth fails

**Keepalive:**
- Scheduled task every 30 seconds
- Checks if transport is open
- Closes connection if transport closed

### DocumentConnection

Per-document connection:

**Responsibilities:**
- Sync protocol handling
- Awareness updates
- Stateless messages
- Read-only mode enforcement

**Message Routing:**
- `SYNC` → `handleSync()`
- `AWARENESS` → `handleAwareness()`
- `QUERY_AWARENESS` → `handleQueryAwareness()`
- `STATELESS` → `handleStateless()`
- `BROADCAST_STATELESS` → `handleBroadcastStateless()`

### YDocument

Document wrapper:

**Responsibilities:**
- Wrap ycrdt YDoc
- Track connections
- Manage awareness state
- Broadcast messages

**Broadcasting:**
- `broadcast(message, exceptConnectionId)` - Send to all except one
- `broadcastStateless(payload, exceptConnectionId)` - Broadcast custom message

**Connection Tracking:**
- `addConnection(DocumentConnection)` - Track new connection
- `removeConnection(DocumentConnection)` - Remove and possibly unload

## Sync Protocol

### Overview

Implements Y.js sync protocol for efficient document synchronization.

### SyncProtocol Class

**Methods:**
- `encodeSyncStep1(stateVector)` - Create sync request
- `encodeSyncStep2(update)` - Create sync response
- `applySyncMessage(doc, payload)` - Handle sync message
- `hasChanges(payload)` - Check if payload has changes

### Sync Flow

#### Initial Sync

1. Client connects
2. Server creates empty state vector
3. Server encodes full document as update
4. Server sends SYNC_STEP2 to client
5. Client applies update

#### Incremental Updates

1. Client makes change
2. Client encodes change as update
3. Client sends UPDATE message
4. Server applies update
5. Server broadcasts update to others
6. Other clients apply update

### Differential Updates

Uses state vectors for efficiency:
- State vector represents known operations
- Diff contains only unknown operations
- Reduces bandwidth significantly

### Read-Only Mode

DocumentConnection can be set to read-only:
- Checks `hasChanges()` before applying sync
- Sends sync status `false` if rejected
- Prevents unauthorized modifications

## Awareness Protocol

### Purpose

Track user presence (cursor position, selection, user info).

### Awareness Class

**State:**
- `Map<String, Map<String, Object>>` - Client states
- `Map<String, Long>` - Last seen timestamps

**Methods:**
- `applyUpdate(update, origin)` - Apply awareness update
- `getStates()` - Get all client states
- `removeStates(clientIds)` - Remove disconnected clients
- `getClientCount()` - Count active clients

### Update Format

Awareness updates are encoded as:
```
[numClients: varInt]
[clientId: varString, clock: varInt, state: varString]+
```

Empty state string indicates client removal.

### Integration

- DocumentConnection forwards awareness updates
- YDocument stores awareness state
- Updates broadcast to all connections

## Stateless Messaging

### Purpose

Send custom application messages without affecting CRDT state.

### Types

1. **Stateless** - Echo back to sender
2. **Broadcast Stateless** - Send to all except sender

### Use Cases

- Chat messages
- Notifications
- Custom events
- Application-level commands

### Implementation

**handleStateless():**
- Receives message
- Echoes back to sender
- (Phase 5: run through extension hooks)

**handleBroadcastStateless():**
- Receives message
- Broadcasts to all except sender
- (Phase 5: run through extension hooks)

## Concurrency

### Thread Safety

**ConcurrentHashMap:**
- Document map
- Loading document map
- Connection map

**Synchronized:**
- Connection close operations
- Document state transitions

**AtomicBoolean:**
- Change loop prevention (yprosemirror)

### Race Conditions

**Document Loading:**
- Prevented by `loadingDocuments` map
- Multiple requests wait for same future
- Only one actual load occurs

**Connection Cleanup:**
- Synchronized close prevents double-free
- Connection count checked atomically

## Resource Management

### Lifecycle

All components implement `AutoCloseable`:
- Transport
- ClientConnection
- DocumentConnection
- YDocument
- Subscriptions

### Cleanup Order

1. Close DocumentConnection
2. Remove from YDocument
3. Unload YDocument if last connection
4. Close YDoc (ycrdt)
5. Close Transport

### Memory Management

- No native pointers in yhocuspocus (handled by ycrdt)
- Java objects managed by GC
- Closeable pattern prevents leaks

## Testing Strategy

### Unit Tests

**Protocol Tests (36):**
- VarInt encoding/decoding
- Message encoding/decoding
- Type conversions

**Sync Protocol Tests (21):**
- State vector encoding
- Update encoding
- Differential sync

**Awareness Tests (10):**
- State management
- Update application
- Client removal

### Integration Tests

**Connection Tests (8):**
- Authentication flow
- Message queueing
- Keepalive
- Multiplexing

**Sync Tests (7):**
- Initial sync
- Incremental updates
- Concurrent edits
- Read-only mode
- Broadcasting

**Awareness & Stateless Tests (6):**
- Awareness propagation
- Query awareness
- Stateless echo
- Broadcast stateless

### MockTransport

Test utility:
- Captures sent messages
- Simulates received messages
- No network overhead
- Deterministic behavior

## Performance Considerations

### Message Overhead

- Binary protocol minimizes size
- Variable-length encoding efficient
- State vectors reduce sync data

### Memory Usage

- Documents loaded on demand
- Documents unloaded when unused
- Awareness state bounded by connection count

### Concurrency

- Multiple connections handled concurrently
- Document operations single-threaded (CRDT requirement)
- Broadcasting parallelizable

## Design Decisions

### Transport Abstraction

**Decision:** Fully abstract transport layer

**Rationale:**
- Hocuspocus is WebSocket-centric
- Enterprise may prefer HTTP/SSE
- Easier testing with mocks
- Future-proof for new protocols

**Trade-offs:**
- Slightly more complex than WebSocket-only
- Need to implement keepalive per transport

### lib0 Compatibility

**Decision:** Use lib0-compatible encoding

**Rationale:**
- Ensures Y.js client compatibility
- Proven protocol design
- Ecosystem compatibility

**Trade-offs:**
- More complex than JSON
- Need custom encoder/decoder

### Document-Level Synchronization

**Decision:** Full document sync for initial, differential for updates

**Rationale:**
- Simplest approach for initial sync
- State vectors enable efficiency
- Matches Hocuspocus behavior

**Trade-offs:**
- Initial sync expensive for large documents
- Could optimize with lazy loading

### No Manual Transactions

**Decision:** Automatic transactions per operation

**Rationale:**
- Matches ycrdt design
- Simpler API
- CRDT doesn't need manual transactions

**Trade-offs:**
- Can't batch operations explicitly
- Each operation commits immediately

## Known Limitations

1. **No Extension System** - Phase 5 not implemented
2. **No Persistence** - Phase 6 not implemented
3. **No Debouncing** - Phase 6 not implemented
4. **No WebSocket Reference** - Phase 7 not implemented
5. **Single Instance** - No horizontal scaling support yet

## Future Enhancements

### Extension System (Phase 5)

- Hook-based customization
- Priority-ordered execution
- Context enrichment
- Database, Redis, auth extensions

### Persistence (Phase 6)

- Debounced document saving
- Max debounce enforcement
- Immediate save on unload
- Extension-based storage

### WebSocket Transport (Phase 7)

- Reference implementation
- Jakarta WebSocket API
- Jetty or Spring integration

### Horizontal Scaling

- Redis pub/sub
- Distributed locking
- State vector synchronization
- Cross-instance awareness

## References

- [Hocuspocus](https://github.com/ueberdosis/hocuspocus) - Reference TypeScript implementation
- [Y.js](https://docs.yjs.dev/) - Y-CRDT JavaScript implementation
- [lib0](https://github.com/dmonad/lib0) - Encoding library
- [y-crdt](https://github.com/y-crdt/y-crdt) - Rust Y-CRDT implementation
