# yhocuspocus-websocket - WebSocket Transport

WebSocket transport for [yhocuspocus](../yhocuspocus/) using Jetty 12. Compatible with Y.js and Hocuspocus JavaScript clients.

## Installation

```groovy
implementation 'net.carcdr:yhocuspocus-websocket:0.1.0-SNAPSHOT'
```

Build from source: `./gradlew :yhocuspocus-websocket:build`

## Usage

```java
import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.extension.InMemoryDatabaseExtension;
import net.carcdr.yhocuspocus.websocket.WebSocketServer;
import java.time.Duration;

YHocuspocus hocuspocus = YHocuspocus.builder()
    .extension(new InMemoryDatabaseExtension())
    .debounce(Duration.ofSeconds(2))
    .maxDebounce(Duration.ofSeconds(10))
    .build();

WebSocketServer server = WebSocketServer.builder()
    .server(hocuspocus)
    .port(1234)
    .path("/")
    .pingInterval(Duration.ofSeconds(30))
    .build();

server.start();
```

### Builder Options

| Method | Default | Description |
|--------|---------|-------------|
| `server(YHocuspocus)` | *required* | YHocuspocus server instance |
| `port(int)` | `1234` | Port to listen on (0 = random) |
| `host(String)` | `null` | Host/interface to bind (null = all) |
| `path(String)` | `"/"` | WebSocket endpoint path |
| `pingInterval(Duration)` | `30s` | WebSocket ping interval |

Idle timeout is auto-calculated as 2x ping interval. Max message size is 10MB.

## JavaScript Client

```typescript
import { HocuspocusProvider } from '@hocuspocus/provider'
import * as Y from 'yjs'

const provider = new HocuspocusProvider({
  url: 'ws://localhost:1234',
  name: 'my-document',
  document: new Y.Doc(),
})
```

Also works with Tiptap's `Collaboration` extension. See [examples](../examples/) for a full-stack setup.

## Documentation

- [Technical Details](IMPLEMENTATION.md)
- [Development Plan](PLAN.md)
- [yhocuspocus core](../yhocuspocus/)

## License

Apache License 2.0
