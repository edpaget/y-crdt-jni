# yhocuspocus-spring-websocket - Spring WebSocket Transport

Spring WebSocket transport for YHocuspocus, providing integration with Spring Boot applications.

## Features

- Spring MVC WebSocket integration via `BinaryWebSocketHandler`
- Spring Boot auto-configuration with `application.yml` support
- Builder pattern for manual configuration
- Handshake interceptor support for authentication
- Compatible with Y.js WebSocket clients

## Requirements

- Java 21 or higher
- Spring Boot 3.x / Spring Framework 6.x
- yhocuspocus module (included as dependency)

## Installation

Currently not published to Maven Central. Build from source:

```bash
./gradlew :yhocuspocus-spring-websocket:build
```

The JAR will be in `yhocuspocus-spring-websocket/build/libs/yhocuspocus-spring-websocket.jar`.

## Quick Start

### Auto-Configuration (Recommended)

Add the dependency to your Spring Boot project and configure via `application.yml`:

```yaml
yhocuspocus:
  path: /collaboration
  allowed-origins:
    - "http://localhost:3000"
  debounce: 2s
  max-debounce: 10s
```

That's it. The auto-configuration creates a `YHocuspocus` server and registers the WebSocket handler automatically.

### Custom Extension Bean

Provide your own persistence extension:

```java
@Configuration
public class CollaborationConfig {

    @Bean
    public Extension databaseExtension(DataSource dataSource) {
        return new JdbcDatabaseExtension(dataSource);
    }
}
```

The auto-configuration will automatically pick up any `Extension` beans.

### Manual Configuration

For full control, disable auto-configuration and configure manually:

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Bean
    public YHocuspocus hocuspocus() {
        return YHocuspocus.builder()
            .extension(new InMemoryDatabaseExtension())
            .debounce(Duration.ofSeconds(2))
            .build();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new SpringWebSocketHandler(hocuspocus()), "/collaboration")
            .setAllowedOrigins("*");
    }
}
```

Or use the builder:

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig {

    @Bean
    public YHocuspocus hocuspocus() {
        return YHocuspocus.builder().build();
    }

    @Bean
    public YHocuspocusWebSocketConfigurer webSocketConfigurer(YHocuspocus server) {
        return YHocuspocusWebSocketConfigurer.builder()
            .server(server)
            .path("/collaboration")
            .allowedOrigins("http://localhost:3000")
            .addInterceptor(new AuthenticationInterceptor())
            .build();
    }
}
```

## Authentication

Use a `HandshakeInterceptor` for authentication:

```java
@Component
public class AuthenticationInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String token = extractToken(request);
        if (token != null && validateToken(token)) {
            attributes.put("userId", extractUserId(token));
            attributes.put("authenticated", true);
            return true;
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No-op
    }

    private String extractToken(ServerHttpRequest request) {
        // Extract from query params or headers
        return request.getURI().getQuery(); // Simplified
    }
}
```

Session attributes set by interceptors are copied to the YHocuspocus connection context.

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `yhocuspocus.path` | `/` | WebSocket endpoint path |
| `yhocuspocus.allowed-origins` | `*` | CORS allowed origins |
| `yhocuspocus.debounce` | `2s` | Debounce before persisting changes |
| `yhocuspocus.max-debounce` | `10s` | Maximum debounce before forced persist |

## JavaScript Client Example

```javascript
import * as Y from 'yjs'
import { WebsocketProvider } from 'y-websocket'

const ydoc = new Y.Doc()
const provider = new WebsocketProvider(
    'ws://localhost:8080',
    'my-document',
    ydoc
)

// Use shared types
const ymap = ydoc.getMap('state')
ymap.set('counter', 0)

// Observe changes
ymap.observe(event => {
    console.log('State changed:', ymap.toJSON())
})
```

## Testing

```bash
# Run all tests
./gradlew :yhocuspocus-spring-websocket:test

# Run with checkstyle
./gradlew :yhocuspocus-spring-websocket:check
```

## Test Coverage

- 19 SpringWebSocketTransport tests
- 13 SpringWebSocketHandler tests
- 10 YHocuspocusWebSocketConfigurer tests
- Total: 42 tests, 100% passing

## Architecture

```
Spring Boot Application
    |
    +-- YHocuspocusWebSocketConfigurer (WebSocketConfigurer)
    |       |
    |       +-- SpringWebSocketHandler (BinaryWebSocketHandler)
    |               |
    |               +-- SpringWebSocketTransport (Transport)
    |                       |
    +-- YHocuspocus Server ----+
            |
            +-- Documents
            +-- Connections
            +-- Extensions
```

## License

Apache License 2.0
