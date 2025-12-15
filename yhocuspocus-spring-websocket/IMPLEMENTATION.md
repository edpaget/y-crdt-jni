# Implementation Details

Technical documentation for the yhocuspocus-spring-websocket module.

## Architecture

### Class Hierarchy

```
Transport (interface from yhocuspocus)
    |
    +-- SpringWebSocketTransport
            wraps: WebSocketSession

BinaryWebSocketHandler (Spring)
    |
    +-- SpringWebSocketHandler
            uses: YHocuspocus, SpringWebSocketTransport

WebSocketConfigurer (Spring)
    |
    +-- YHocuspocusWebSocketConfigurer
            creates: SpringWebSocketHandler
```

### Package Structure

```
net.carcdr.yhocuspocus.spring/
    websocket/
        SpringWebSocketTransport.java   # Transport implementation
        SpringWebSocketHandler.java     # WebSocket lifecycle handler
        YHocuspocusWebSocketConfigurer.java  # Spring configuration
    autoconfigure/
        YHocuspocusProperties.java      # @ConfigurationProperties
        YHocuspocusAutoConfiguration.java    # YHocuspocus bean
        YHocuspocusWebSocketAutoConfiguration.java  # WebSocket setup
```

## Core Components

### SpringWebSocketTransport

Adapts Spring's `WebSocketSession` to the yhocuspocus `Transport` interface.

**Thread Safety:**
- `AtomicBoolean closed` for connection state
- `volatile ReceiveListener` for safe listener updates
- Session methods are thread-safe per Spring documentation

**Key Implementation Details:**

```java
// Connection ID generation
private String generateConnectionId(WebSocketSession session) {
    return "spring-ws-" + session.getId() + "-" + System.currentTimeMillis();
}

// Async message sending
public CompletableFuture<Void> send(byte[] message) {
    return CompletableFuture.runAsync(() -> {
        session.sendMessage(new BinaryMessage(ByteBuffer.wrap(message)));
    });
}

// Close status mapping
private CloseStatus mapToCloseStatus(int code, String reason) {
    if (code >= 4000 && code <= 4999) {
        return new CloseStatus(code, reason);
    }
    return switch (code) {
        case 1000 -> CloseStatus.NORMAL.withReason(reason);
        case 1001 -> CloseStatus.GOING_AWAY.withReason(reason);
        // ...
    };
}
```

### SpringWebSocketHandler

Extends `BinaryWebSocketHandler` to handle WebSocket lifecycle events.

**Session Tracking:**
```java
// Per-session tracking maps
private final ConcurrentHashMap<String, SpringWebSocketTransport> transports;
private final ConcurrentHashMap<String, ClientConnection> connections;
```

**Lifecycle Flow:**
1. `afterConnectionEstablished()` - Create transport, register with YHocuspocus
2. `handleBinaryMessage()` - Forward to transport's `receiveMessage()`
3. `afterConnectionClosed()` - Close connection, cleanup maps
4. `handleTransportError()` - Handle errors, cleanup

**Context Building:**
```java
// Session attributes copied to YHocuspocus context
Map<String, Object> context = new ConcurrentHashMap<>();
context.put("remoteAddress", transport.getRemoteAddress());
context.put("sessionId", sessionId);
session.getAttributes().forEach(context::put);
```

### YHocuspocusWebSocketConfigurer

Implements `WebSocketConfigurer` for Spring registration.

**Builder Pattern:**
```java
YHocuspocusWebSocketConfigurer.builder()
    .server(hocuspocus)         // Required
    .path("/collaboration")     // Default: "/"
    .allowedOrigins("*")        // Default: "*"
    .addInterceptor(auth)       // Optional
    .build();
```

**Handler Registration:**
```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    var registration = registry.addHandler(handler, path)
        .setAllowedOrigins(allowedOrigins);
    if (!interceptors.isEmpty()) {
        registration.addInterceptors(interceptors.toArray(...));
    }
}
```

## Auto-Configuration

### Conditional Loading

```java
@AutoConfiguration(after = YHocuspocusAutoConfiguration.class)
@ConditionalOnWebApplication(type = SERVLET)  // Not WebFlux
@ConditionalOnClass(WebSocketConfigurer.class)
@ConditionalOnBean(YHocuspocus.class)
@EnableWebSocket
public class YHocuspocusWebSocketAutoConfiguration {
```

### Bean Creation Order

1. `YHocuspocusAutoConfiguration` creates `YHocuspocus` bean
2. Collects all `Extension` beans from context
3. `YHocuspocusWebSocketAutoConfiguration` creates `YHocuspocusWebSocketConfigurer`
4. Collects all `HandshakeInterceptor` beans from context

### Default Behavior

- If no `Extension` beans found: uses `InMemoryDatabaseExtension`
- If no `YHocuspocus` bean found: creates one with properties
- If no `YHocuspocusWebSocketConfigurer` found: creates one with properties

## Message Flow

```
Y.js Client
    |
    | WebSocket binary frame
    v
Spring WebSocket Container
    |
    v
SpringWebSocketHandler.handleBinaryMessage()
    |
    v
SpringWebSocketTransport.receiveMessage()
    |
    v
ReceiveListener.onMessage() [ClientConnection]
    |
    v
ClientConnection.handleMessage()
    |
    v
DocumentConnection / Sync Protocol
    |
    v
YDocument CRDT operations
    |
    v
Broadcast to other connections
```

## Testing Approach

### Unit Tests

- Mock `WebSocketSession` using Mockito
- Mock `YHocuspocus` and `ClientConnection`
- Test all public methods and edge cases
- Verify proper cleanup on close/error

### Integration Tests

For full integration testing in a Spring Boot test:

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class IntegrationTest {
    @LocalServerPort int port;

    @Test
    void testConnection() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(
            new TestHandler(),
            "ws://localhost:" + port + "/collaboration"
        ).get(5, TimeUnit.SECONDS);
        // ...
    }
}
```

## Comparison with Jetty Transport

| Aspect | Jetty (yhocuspocus-websocket) | Spring (this module) |
|--------|------------------------------|----------------------|
| Handler | `Session.Listener.AutoDemanding` | `BinaryWebSocketHandler` |
| Session | `org.eclipse.jetty.websocket.api.Session` | `WebSocketSession` |
| Server | Standalone `WebSocketServer` | Spring Boot managed |
| Config | Builder pattern | Auto-config + Builder |
| Lifecycle | Manual start/stop | Spring context managed |

## Known Limitations

1. **Servlet-based only**: Does not support Spring WebFlux (reactive)
2. **Single path**: One WebSocket path per configurer
3. **Binary only**: Text WebSocket messages are not supported

## Future Enhancements

See [PLAN.md](PLAN.md) for planned features.
