# Spring Boot Collaborative Editor Example

Demonstrates the `yhocuspocus-spring-websocket` module with a collaborative text editor.

## Quick Start

```bash
# Terminal 1: Start Spring Boot backend
./gradlew :examples:spring-boot:backend:bootRun

# Terminal 2: Start frontend
cd examples/frontend
npm install
npm run dev
```

Open http://localhost:3000

## Configuration

All configuration is in `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 1234

yhocuspocus:
  path: /
  allowed-origins:
    - "http://localhost:3000"
  debounce: 2s
  max-debounce: 10s
```

## How It Works

1. Spring Boot auto-configures YHocuspocus via `yhocuspocus-spring-websocket`
2. WebSocket endpoint is registered at `ws://localhost:1234/`
3. Frontend connects using `@hocuspocus/provider`
4. Y.js handles CRDT-based conflict resolution

## Adding Custom Extensions

Create an `Extension` bean:

```java
@Configuration
public class CollaborationConfig {

    @Bean
    public Extension databaseExtension(DataSource dataSource) {
        return new JdbcDatabaseExtension(dataSource);
    }
}
```

## Comparison with Standalone Example

| Aspect | fullstack (Jetty) | spring-boot |
|--------|------------------|-------------|
| Setup | ~30 lines Java | ~10 lines + YAML |
| Config | Builder pattern | application.yml |
| Module | yhocuspocus-websocket | yhocuspocus-spring-websocket |
| Server lifecycle | Manual start/stop | Spring managed |
