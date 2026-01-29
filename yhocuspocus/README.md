# yhocuspocus - Collaborative Editing Server

Transport-agnostic collaborative editing server for Y-CRDT, inspired by [Hocuspocus](https://github.com/ueberdosis/hocuspocus). Works with any transport layer (WebSocket, HTTP, SSE, custom protocols).

## Features

- Y.js-compatible sync protocol (lib0 encoding)
- Extension system with 12 lifecycle hooks
- Awareness protocol (user presence tracking)
- Debounced persistence via extension hooks
- Stateless custom messaging
- Connection authentication and multiplexing

## Installation

```groovy
implementation 'net.carcdr:yhocuspocus:0.1.0-SNAPSHOT'
```

Build from source: `./gradlew :yhocuspocus:build`

## Usage

### Create a Server

```java
import net.carcdr.yhocuspocus.core.*;
import net.carcdr.yhocuspocus.extension.*;
import java.time.Duration;

YHocuspocus server = YHocuspocus.builder()
    .extension(new InMemoryDatabaseExtension())
    .debounce(Duration.ofSeconds(2))
    .maxDebounce(Duration.ofSeconds(10))
    .build();

// Handle incoming connection (your transport provides this)
Transport transport = createTransport();
ClientConnection connection = server.handleConnection(
    transport, Map.of("userId", "user123")
);
```

### Implement a Transport

```java
import net.carcdr.yhocuspocus.transport.*;

public class MyTransport implements Transport {
    private final String connectionId;
    private final MyConnection underlying;

    @Override
    public CompletableFuture<Void> send(byte[] message) {
        return CompletableFuture.runAsync(() -> underlying.sendBytes(message));
    }

    @Override public String getConnectionId() { return connectionId; }
    @Override public boolean isOpen() { return underlying.isConnected(); }
    @Override public String getRemoteAddress() { return underlying.getRemoteAddress(); }
    @Override public void close(int code, String reason) { underlying.close(code, reason); }
    @Override public void close() { close(1000, "Normal closure"); }
}
```

### Extension Hooks

Extensions can hook into any of these lifecycle events:

| Hook | When |
|------|------|
| `onConfigure` | Server startup |
| `onListen` | Server ready |
| `onDestroy` | Server shutdown |
| `onConnect` | Client connects |
| `onAuthenticate` | Authentication check |
| `onDisconnect` | Client disconnects |
| `onLoadDocument` | Document loaded |
| `afterLoadDocument` | After document loaded |
| `onChange` | Document changed |
| `onStoreDocument` | Persist document |
| `beforeUnloadDocument` | Before document unloaded |
| `onStateless` | Custom message received |

### API

```java
public class YHocuspocus implements AutoCloseable {
    public static Builder builder();
    public ClientConnection handleConnection(Transport transport, Map<String, Object> context);
    public YDocument getDocument(String name);
    public void close();
}

public interface Transport extends AutoCloseable {
    CompletableFuture<Void> send(byte[] message);
    String getConnectionId();
    void close(int code, String reason);
    boolean isOpen();
    String getRemoteAddress();
}
```

### MockTransport (for testing)

```java
MockTransport mock = new MockTransport();
ClientConnection conn = server.handleConnection(mock, Map.of());
mock.receiveMessage(syncMessage);
List<byte[]> responses = mock.getSentMessages();
```

## Transport Implementations

- [yhocuspocus-websocket](../yhocuspocus-websocket/) -- Jetty 12
- [yhocuspocus-spring-websocket](../yhocuspocus-spring-websocket/) -- Spring Boot

## Documentation

- [Technical Details](IMPLEMENTATION.md)
- [Development Plan](PLAN.md)

## Known Limitations

1. No horizontal scaling yet (see [yhocuspocus-redis](../yhocuspocus-redis/) for pub/sub sync)
2. Single-instance only (no distributed locking)

## License

Apache License 2.0
