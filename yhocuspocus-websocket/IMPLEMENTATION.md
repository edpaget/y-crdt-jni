# Implementation Details - yhocuspocus-websocket

Technical implementation details for the WebSocket transport module.

## Architecture

The yhocuspocus-websocket module provides a production-ready WebSocket transport implementation for YHocuspocus using Jetty 12.

### Design Principles

1. **Transport Abstraction** - Implements yhocuspocus `Transport` interface
2. **Per-Connection Endpoints** - Each WebSocket connection gets its own endpoint instance
3. **Jetty 12 Core API** - Uses modern Jetty 12 WebSocket server API
4. **Builder Pattern** - Easy configuration with sensible defaults
5. **AutoCloseable** - Proper resource management

## Components

### 1. WebSocketServer

Main server class that manages the Jetty HTTP server and WebSocket container.

**Responsibilities:**
- HTTP server lifecycle (start/stop)
- WebSocket container configuration
- WebSocket upgrade handling
- Connection routing to yhocuspocus

**Key Features:**
- Builder pattern for configuration
- Dynamic port assignment (port 0 support)
- Automatic idle timeout based on ping interval
- Graceful shutdown
- Path-based WebSocket routing

**Implementation Details:**

```java
public final class WebSocketServer implements AutoCloseable {
    private final YHocuspocus hocuspocus;
    private final int port;
    private final String host;
    private final String path;
    private final Duration pingInterval;
    private final Server jettyServer;

    // Configuration
    - Default port: 1234
    - Default path: "/"
    - Default ping interval: 30 seconds
    - Idle timeout: 2× ping interval
    - Max message size: 10MB
    - Auto fragment: disabled
}
```

**WebSocket Upgrade Flow:**
1. HTTP request arrives at Jetty server
2. Handler checks if path matches configured path
3. If match, attempt WebSocket upgrade via `container.upgrade()`
4. Factory creates new `WebSocketEndpoint` instance
5. WebSocket connection established

### 2. WebSocketEndpoint

Per-connection WebSocket handler implementing Jetty's `Session.Listener.AutoDemanding`.

**Responsibilities:**
- WebSocket connection lifecycle
- Binary message handling
- Integration with YHocuspocus
- Transport creation
- Error handling

**Key Features:**
- One endpoint instance per WebSocket connection
- Creates `WebSocketTransport` on connection open
- Forwards binary messages to transport
- Handles connection close and errors
- Automatic demand (no backpressure needed)

**Implementation Details:**

```java
public class WebSocketEndpoint implements Session.Listener.AutoDemanding {
    private final YHocuspocus server;
    private Session session;
    private WebSocketTransport transport;
    private ClientConnection connection;

    // Lifecycle hooks:
    - onWebSocketOpen(): Create transport, register with yhocuspocus
    - onWebSocketBinary(): Forward binary data to transport
    - onWebSocketClose(): Clean up connection
    - onWebSocketError(): Handle errors, close connection
}
```

**Session-Per-Connection Pattern:**
- Each WebSocket connection gets a dedicated endpoint instance
- Endpoint maintains state for that connection
- No shared state between connections
- Simplifies concurrency (no cross-connection locking)

### 3. WebSocketTransport

Transport implementation bridging Jetty WebSocket and yhocuspocus.

**Responsibilities:**
- Implement yhocuspocus `Transport` interface
- Send binary messages over WebSocket
- Manage connection state
- Provide connection metadata

**Key Features:**
- CompletableFuture-based async message sending
- WebSocket close code mapping (1000-1015, 4000-4999)
- Thread-safe connection state (AtomicBoolean)
- Connection ID and remote address tracking

**Implementation Details:**

```java
public class WebSocketTransport implements Transport {
    private final Session session;
    private final String connectionId;
    private final AtomicBoolean closed;

    // Transport interface methods:
    - send(byte[]): Async send via CompletableFuture
    - getConnectionId(): Session-based unique ID
    - close(int, String): Map to WebSocket close codes
    - isOpen(): Check session state
    - getRemoteAddress(): From session properties
}
```

**Close Code Mapping:**
- Normal closure (1000)
- Going away (1001)
- Protocol error (1002)
- Server error (1011)
- Custom application codes (4000-4999)

## Message Flow

### Connection Establishment

```
1. Client connects to ws://localhost:1234/
2. Jetty receives HTTP Upgrade request
3. WebSocketServer handler checks path
4. Handler calls container.upgrade()
5. Factory creates WebSocketEndpoint instance
6. onWebSocketOpen() called
7. Creates WebSocketTransport
8. Calls server.handleConnection(transport, context)
9. YHocuspocus creates ClientConnection
10. Connection ready for messages
```

### Binary Message Handling

```
1. Client sends binary WebSocket frame
2. Jetty calls onWebSocketBinary(ByteBuffer, Callback)
3. Endpoint converts ByteBuffer to byte[]
4. Endpoint calls transport.receiveMessage(byte[])
5. Transport notifies registered message handler
6. Handler is ClientConnection.handleMessage()
7. ClientConnection decodes and routes message
8. Response sent back via transport.send()
9. Transport sends via session.sendBinary()
10. Client receives response
```

### Connection Close

```
1. Client closes WebSocket connection
2. Jetty calls onWebSocketClose(int, String)
3. Endpoint calls connection.close(code, reason)
4. ClientConnection closes all DocumentConnections
5. Each DocumentConnection removes itself from YDocument
6. YDocument unloads if no more connections
7. Transport closed
8. Resources cleaned up
```

## Jetty 12 Specifics

### Why Jetty 12?

1. **Modern API** - Clean, modern WebSocket API
2. **HTTP/2 Support** - Built-in HTTP/2
3. **Performance** - High-performance asynchronous I/O
4. **Core Module** - No Jakarta dependency (lighter weight)
5. **Active Development** - Well-maintained

### Session.Listener.AutoDemanding

```java
public interface Session.Listener.AutoDemanding extends Session.Listener
```

**Benefits:**
- Automatic demand management (no manual `session.demand()` calls)
- Simpler implementation
- Less error-prone
- Better for most use cases

**Alternative**: `Session.Listener` requires manual demand management

### Binary Message Handling

```java
@Override
public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
    // Convert ByteBuffer to byte[]
    byte[] data = new byte[payload.remaining()];
    payload.get(data);

    // Process message
    transport.receiveMessage(data);

    // Signal completion
    callback.succeed();  // or callback.fail(throwable)
}
```

**Callback Pattern:**
- MUST call `callback.succeed()` or `callback.fail()`
- Signals Jetty that message processing is complete
- Allows Jetty to manage backpressure
- Failure stops further messages until recovered

## Configuration

### Idle Timeout Calculation

```java
Duration idleTimeout = pingInterval.multipliedBy(2);
container.setIdleTimeout(idleTimeout);
```

**Rationale:**
- Ping sent every `pingInterval` seconds
- Allow time for network delay + pong response
- 2× provides buffer for network latency
- Jetty automatically closes stale connections

### Max Message Size

```java
container.setMaxBinaryMessageSize(10 * 1024 * 1024); // 10MB
```

**Considerations:**
- Large enough for typical Y.js updates
- Not unlimited (prevent DoS)
- Can be made configurable in future

### Auto Fragment

```java
container.setAutoFragment(false);
```

**Rationale:**
- Y.js messages should be sent as single frames
- Fragmentation adds overhead
- Client expects complete messages

## Thread Safety

### Endpoint Instances

- Each connection gets its own endpoint instance
- No shared state between connections
- No cross-connection synchronization needed

### Transport State

```java
private final AtomicBoolean closed = new AtomicBoolean(false);
```

- Thread-safe connection state
- Multiple threads can check `isOpen()`
- Only one thread can `close()`

### Jetty Threading

- Jetty uses thread pool for connections
- Different connections on different threads
- Same connection messages serialized by Jetty
- No need for per-connection locking

## Error Handling

### Connection Errors

```java
@Override
public void onWebSocketError(Throwable cause) {
    LOGGER.error("WebSocket error", cause);
    if (connection != null) {
        connection.close(1011, "Error: " + cause.getMessage());
    }
}
```

**Strategy:**
- Log error details
- Close connection gracefully
- Clean up resources
- Don't propagate to client

### Message Send Failures

```java
@Override
public CompletableFuture<Void> send(byte[] message) {
    return CompletableFuture.runAsync(() -> {
        try {
            session.sendBinary(ByteBuffer.wrap(message), Callback.NOOP);
        } catch (Exception e) {
            throw new CompletionException(
                new RuntimeException("Failed to send message", e)
            );
        }
    });
}
```

**Strategy:**
- Wrap in CompletableFuture
- Convert exceptions to CompletionException
- Caller can handle via `.exceptionally()`

## Testing Strategy

### Integration Tests

**WebSocketServerTest.java** - 3 tests:

1. **testServerStartsAndStops**
   - Verifies server lifecycle
   - Checks port binding
   - Confirms graceful shutdown

2. **testWebSocketConnectionEstablished**
   - Tests WebSocket upgrade
   - Verifies endpoint creation
   - Confirms connection registration

3. **testWebSocketBinaryMessageReceived**
   - Tests end-to-end message flow
   - Verifies binary data transmission
   - Confirms protocol message handling

**TestWebSocketListener:**
- Custom WebSocket client for testing
- Jetty 12 WebSocket client API
- Tracks received messages
- Provides sync/async message sending

### Manual Testing

See [example-fullstack](../example-fullstack/README.md) for browser-based testing with:
- Real Yjs client
- HocuspocusProvider
- Tiptap collaborative editing

## Performance Considerations

### Message Size

- Max 10MB per message
- Large documents may hit limit
- Consider compression for large updates
- Monitor message sizes in production

### Connection Limits

- No built-in connection limit
- Limited by JVM heap and file descriptors
- Configure in reverse proxy
- Monitor active connections

### Memory Usage

- Each connection: ~1KB overhead (endpoint + transport)
- Each document: variable (depends on content)
- YDoc memory managed by ycrdt module
- Use monitoring to track usage

## Future Enhancements

### Configurable Limits

```java
.maxMessageSize(20 * 1024 * 1024)
.maxConnections(1000)
```

### Compression

```java
.compression(true)
.compressionLevel(6)
```

### Metrics

```java
.metricsEnabled(true)
.metricsPath("/metrics")
```

### HTTP/2 Support

- Jetty 12 supports HTTP/2
- Can upgrade to support HTTP/2 WebSocket

## References

- [Jetty 12 WebSocket Documentation](https://eclipse.dev/jetty/documentation/jetty-12/programming-guide/index.html#pg-websocket)
- [WebSocket RFC 6455](https://tools.ietf.org/html/rfc6455)
- [YHocuspocus Transport Interface](../yhocuspocus/src/main/java/net/carcdr/yhocuspocus/transport/Transport.java)
