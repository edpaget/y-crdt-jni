# yhocuspocus-redis Implementation

Technical documentation for the Redis horizontal scaling extension.

## Architecture

### Module Structure

```
yhocuspocus-redis/
├── src/main/java/net/carcdr/yhocuspocus/redis/
│   ├── RedisExtensionConfig.java    # Configuration builder
│   ├── RedisClient.java             # Abstract client interface
│   ├── LettuceRedisClient.java      # Lettuce implementation
│   ├── RedisPubSubExtension.java    # Main extension
│   ├── MessageCodec.java            # Message encoding/decoding
│   └── AwarenessThrottler.java      # Rate limiting for awareness
└── src/test/java/...
```

### Design Decisions

#### Extension vs Transport

Redis synchronization is implemented as an Extension, not a Transport, because:

1. **Transport** handles client-to-server connections (WebSocket, HTTP, SSE)
2. **Extension** hooks into document lifecycle (load, change, store, unload)
3. Redis sync is server-to-server communication, fitting the Extension model
4. Extensions have access to `YDocument` for applying remote updates

#### Abstract RedisClient Interface

The `RedisClient` interface abstracts the underlying Redis library to:

- Allow different implementations (Lettuce, Jedis, Spring Data Redis)
- Enable easier testing with mock implementations
- Support future integration with Spring Boot auto-configuration

#### Message Encoding

Messages are encoded with an instance ID prefix to filter self-published messages:

```
+----------------+------------------+--------------+
| instanceIdLen  | instanceId       | payload      |
| (4 bytes)      | (variable)       | (variable)   |
+----------------+------------------+--------------+
```

This allows receivers to skip messages they originated, preventing infinite loops.

#### Pub/Sub vs Streams

This implementation uses Redis pub/sub for simplicity. Benefits:
- Low latency (push-based)
- Minimal memory footprint
- Proven pattern (used by Hocuspocus)

Limitations:
- No message durability (missed messages lost during disconnects)
- Relies on Y.js sync protocol for recovery
- Broadcast issue in Redis Cluster

See `plans/REDIS_HORIZONTAL_SCALING_DESIGN.md` for Streams alternative.

### Extension Hooks

| Hook | Implementation |
|------|----------------|
| `afterLoadDocument` | Subscribe to `{prefix}:doc:{name}` channel, cache document reference |
| `onChange` | Encode update with instance ID, publish to channel |
| `onStoreDocument` | Store state to `{prefix}:doc:{name}:state` key |
| `beforeUnloadDocument` | Unsubscribe from channel, remove from cache |
| `onDestroy` | Unsubscribe all, close Redis client |

### Remote Message Handling

When a message is received from Redis:

1. Decode the message to extract instance ID and payload
2. Check if message originated from this instance (skip if so)
3. Look up document from cache
4. Apply update to local document via `YDoc.applyUpdate()`
5. Broadcast to local clients via `YDocument.broadcastToAll()`

### Thread Safety

- `ConcurrentHashMap` for document cache and subscription tracking
- `RedisClient` implementations must be thread-safe
- Lettuce client is inherently thread-safe (non-blocking I/O)

## Testing

### Unit Tests

- `MessageCodecTest` - Encoding/decoding round-trips
- `RedisExtensionConfigTest` - Configuration builder
- `AwarenessThrottlerTest` - Rate limiting logic
- `RedisPubSubExtensionTest` - Extension lifecycle with mock client

### Integration Tests

- `RedisPubSubTest` - Lettuce and Jedis operations with real Redis
- `RedisExtensionIntegrationTest` - Full extension flow with MockRedisExtension

Integration tests use TestContainers and are automatically skipped if Docker is unavailable.

## Panama Support

The module uses `ycrdt-core` interfaces (not JNI directly), supporting both binding implementations:

```groovy
// Run tests with JNI binding (default)
./gradlew :yhocuspocus-redis:test

// Run tests with Panama binding
./gradlew :yhocuspocus-redis:test -PtestBinding=panama
```

Panama tests require Java 22+ and `--enable-native-access=ALL-UNNAMED` JVM flag.
