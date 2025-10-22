# yhocuspocus-websocket - WebSocket Transport for YHocuspocus

WebSocket transport implementation for the yhocuspocus collaborative editing server, built on Jetty 12.

## Overview

This module provides a WebSocket server implementation that integrates with YHocuspocus, enabling real-time collaborative editing over WebSocket connections. Compatible with JavaScript Yjs/Hocuspocus clients.

## Features

- **Jetty 12 WebSocket Server** - WebSocket implementation using Jetty
- **Builder Pattern** - Configuration with sensible defaults
- **Automatic Keepalive** - Built-in ping/pong for connection health monitoring
- **Dynamic Port Assignment** - Support for port 0 (OS-assigned random port)
- **Graceful Shutdown** - Cleanup of connections and resources
- **Configurable Timeouts** - Idle timeout and message size limits
- **Testing** - 3 integration tests, 100% passing
- **Yjs Compatible** - Works with JavaScript @hocuspocus/provider

## Quick Start

### Basic Usage

```java
import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.extension.InMemoryDatabaseExtension;
import net.carcdr.yhocuspocus.websocket.WebSocketServer;
import java.time.Duration;

public class Server {
    public static void main(String[] args) throws Exception {
        // Create YHocuspocus server
        YHocuspocus hocuspocus = YHocuspocus.builder()
            .extension(new InMemoryDatabaseExtension())
            .debounce(Duration.ofSeconds(2))
            .maxDebounce(Duration.ofSeconds(10))
            .build();

        // Create WebSocket server
        WebSocketServer server = WebSocketServer.builder()
            .server(hocuspocus)
            .port(1234)
            .path("/")
            .pingInterval(Duration.ofSeconds(30))
            .build();

        // Start server
        server.start();

        System.out.println("WebSocket server running on ws://localhost:1234");

        // Keep server running
        Thread.currentThread().join();
    }
}
```

### With Custom Configuration

```java
WebSocketServer server = WebSocketServer.builder()
    .server(hocuspocus)
    .host("0.0.0.0")              // Bind to all interfaces
    .port(8080)                    // Custom port
    .path("/collaboration")        // Custom path
    .pingInterval(Duration.ofSeconds(20))  // Faster keepalive
    .build();
```

### Try-With-Resources

```java
try (YHocuspocus hocuspocus = YHocuspocus.builder()
        .extension(new InMemoryDatabaseExtension())
        .build();
     WebSocketServer server = WebSocketServer.builder()
        .server(hocuspocus)
        .port(1234)
        .build()) {

    server.start();
    Thread.currentThread().join();
}
```

## Configuration Options

### Builder Methods

| Method | Default | Description |
|--------|---------|-------------|
| `server(YHocuspocus)` | *required* | YHocuspocus server instance |
| `port(int)` | `1234` | Port to listen on (0 = random port) |
| `host(String)` | `null` | Host/interface to bind (null = all interfaces) |
| `path(String)` | `"/"` | WebSocket endpoint path |
| `pingInterval(Duration)` | `30 seconds` | WebSocket ping interval for keepalive |

### Runtime Configuration

- **Max Message Size**: 10MB (hardcoded, configurable in future)
- **Idle Timeout**: 2× ping interval (auto-calculated)
- **Auto Fragment**: Disabled (messages sent as single frames)

## Client Connection

### JavaScript/TypeScript (Yjs)

```typescript
import { HocuspocusProvider } from '@hocuspocus/provider'
import * as Y from 'yjs'

const doc = new Y.Doc()

const provider = new HocuspocusProvider({
  url: 'ws://localhost:1234',
  name: 'my-document',
  document: doc,
})
```

### JavaScript/TypeScript (Tiptap)

```typescript
import { useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Collaboration from '@tiptap/extension-collaboration'
import { HocuspocusProvider } from '@hocuspocus/provider'

const provider = new HocuspocusProvider({
  url: 'ws://localhost:1234',
  name: 'my-document',
})

const editor = useEditor({
  extensions: [
    StarterKit,
    Collaboration.configure({
      document: provider.document,
    }),
  ],
})
```

## Architecture

### Components

1. **WebSocketServer** - Main server class with builder pattern
   - Manages Jetty HTTP server lifecycle
   - Configures WebSocket container
   - Handles WebSocket upgrade requests

2. **WebSocketEndpoint** - Per-connection WebSocket handler
   - Implements Jetty `Session.Listener.AutoDemanding`
   - Creates `WebSocketTransport` for each connection
   - Integrates with YHocuspocus via `handleConnection()`

3. **WebSocketTransport** - Transport implementation
   - Implements yhocuspocus `Transport` interface
   - Handles binary message sending/receiving
   - Manages connection state and lifecycle

### Message Flow

```
Client                WebSocketServer         WebSocketEndpoint       YHocuspocus
  │                          │                        │                    │
  │─────WebSocket────────────>│                       │                    │
  │     Upgrade              │                        │                    │
  │                          │──────onCreate──────────>│                   │
  │                          │                        │                    │
  │                          │                        │──handleConnection──>│
  │                          │                        │                    │
  │─────Binary Message───────────────────────────────>│                    │
  │                          │                        │                    │
  │                          │                        │──receiveMessage────>│
  │                          │                        │                    │
  │<────Binary Message─────────────────────────────────────────────────────│
  │                          │                        │                    │
```

## Testing

```bash
# Run WebSocket transport tests
./gradlew :yhocuspocus-websocket:test

# Run all yhocuspocus tests (including WebSocket)
./gradlew :yhocuspocus:test :yhocuspocus-websocket:test
```

**Test Coverage**: 3 integration tests
- Server lifecycle (start/stop)
- WebSocket connection establishment
- Binary message transmission

## Requirements

- Java 21 or higher
- yhocuspocus module (included as dependency)
- Jetty 12.0.13 (managed by Gradle)

## Dependencies

```groovy
dependencies {
    api project(':yhocuspocus')

    // Jetty 12 WebSocket
    implementation 'org.eclipse.jetty.websocket:jetty-websocket-jetty-server:12.0.13'
    implementation 'org.eclipse.jetty.websocket:jetty-websocket-jetty-api:12.0.13'
    implementation 'org.eclipse.jetty:jetty-server:12.0.13'

    // Logging
    implementation 'org.slf4j:slf4j-api:2.0.13'
}
```

## Deployment

### Recommended Setup

1. **Reverse Proxy** - Use nginx or Apache in front of Jetty
   ```nginx
   location /collaboration {
       proxy_pass http://localhost:1234/;
       proxy_http_version 1.1;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection "upgrade";
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
   }
   ```

2. **TLS/SSL** - Enable HTTPS for secure WebSocket (wss://)
   - Configure TLS in reverse proxy (recommended)
   - Or configure Jetty SSL connector directly

3. **Resource Limits**
   - Set appropriate JVM heap size (`-Xmx`)
   - Configure connection limits in reverse proxy
   - Monitor memory usage and connection counts

4. **Logging**
   - Configure SLF4J with logback or log4j2
   - Set appropriate log levels for production
   - Enable access logs for monitoring

### Example Server

```java
public class Server {
    public static void main(String[] args) {
        // Get configuration from environment
        String host = System.getenv().getOrDefault("HOST", "0.0.0.0");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "1234"));
        String path = System.getenv().getOrDefault("WS_PATH", "/");

        // Create server with extensions
        YHocuspocus hocuspocus = YHocuspocus.builder()
            .extension(new PostgresDatabaseExtension())  // Persistent storage
            .extension(new AuthenticationExtension())     // User authentication
            .extension(new MetricsExtension())           // Monitoring
            .debounce(Duration.ofSeconds(5))
            .maxDebounce(Duration.ofSeconds(30))
            .build();

        // Create WebSocket server
        try (WebSocketServer server = WebSocketServer.builder()
                .server(hocuspocus)
                .host(host)
                .port(port)
                .path(path)
                .build()) {

            server.start();

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```

## Troubleshooting

### Port Already in Use

```
java.net.BindException: Address already in use
```

**Solution**: Change port or kill process using the port
```bash
# Find process using port 1234
lsof -i :1234

# Kill process
kill -9 <PID>
```

### WebSocket Connection Refused

**Possible Causes**:
- Server not started
- Wrong port or path
- Firewall blocking connection

**Solution**: Check server logs and verify URL

### Connection Drops Frequently

**Possible Causes**:
- Ping interval too long
- Network instability
- Reverse proxy timeout

**Solution**: Reduce ping interval or adjust proxy timeouts

## Examples

See the [example-fullstack](../example-fullstack/README.md) project for a complete working example with:
- Backend: Java WebSocket server
- Frontend: React + TypeScript + Tiptap
- Real-time collaborative editing

## Documentation

- **Implementation Details**: [IMPLEMENTATION.md](IMPLEMENTATION.md)
- **Development Plan**: [PLAN.md](PLAN.md)
- **YHocuspocus Core**: [../yhocuspocus/README.md](../yhocuspocus/README.md)

## License

Apache License 2.0 - See [LICENSE](../LICENSE) file for details.
