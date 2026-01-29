# yhocuspocus-redis

Redis extension for horizontal scaling of yhocuspocus instances. Document updates are published via Redis pub/sub and received by all other instances subscribed to that document's channel.

## Features

- Pub/sub-based document synchronization across server instances
- Automatic subscription management (subscribe on load, unsubscribe on unload)
- Instance ID filtering to avoid self-message processing
- Configurable key prefixes for multi-tenant deployments
- Document state storage in Redis
- Awareness throttling for cursor position updates
- Supports both JNI and Panama Y-CRDT bindings

## Installation

Add to your `build.gradle`:

```groovy
dependencies {
    implementation 'net.carcdr:yhocuspocus-redis:0.1.0'
}
```

## Usage

### Basic Setup

```java
import net.carcdr.yhocuspocus.redis.*;
import net.carcdr.yhocuspocus.core.YHocuspocus;

// Create Redis client
RedisClient redisClient = new LettuceRedisClient("localhost", 6379);

// Configure the extension
RedisExtensionConfig config = RedisExtensionConfig.builder()
    .prefix("myapp")                        // Redis key prefix
    .instanceId("server-1")                 // Unique instance ID
    .awarenessEnabled(true)                 // Enable awareness sync
    .awarenessThrottle(Duration.ofMillis(100))  // Throttle awareness updates
    .build();

// Create the extension
RedisPubSubExtension redisExtension = new RedisPubSubExtension(config, redisClient);

// Add to YHocuspocus server
YHocuspocus server = YHocuspocus.builder()
    .extension(redisExtension)
    .build();
```

### Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `prefix` | `yhocuspocus` | Prefix for all Redis keys |
| `instanceId` | Generated UUID | Unique identifier for this server instance |
| `awarenessEnabled` | `true` | Enable cross-instance awareness sync |
| `awarenessThrottle` | `100ms` | Minimum interval between awareness updates |

### Redis Key Structure

The extension uses the following key patterns:

- `{prefix}:doc:{documentName}` - Pub/sub channel for document updates
- `{prefix}:awareness:{documentName}` - Pub/sub channel for awareness updates
- `{prefix}:doc:{documentName}:state` - Document state storage
- `{prefix}:doc:{documentName}:vector` - State vector storage

### Custom Redis Client

You can provide your own `RedisClient` implementation:

```java
public class MyRedisClient implements RedisClient {
    // Implement the interface methods...
}

RedisClient client = new MyRedisClient();
RedisPubSubExtension extension = new RedisPubSubExtension(config, client);
```

## Architecture

```
                     Load Balancer
                          |
        +-----------------+------------------+
        |                 |                  |
   +----v----+       +----v----+        +----v----+
   |Instance |       |Instance |        |Instance |
   |    A    |       |    B    |        |    C    |
   +----+----+       +----+----+        +----+----+
        |                 |                  |
        +-----------------+------------------+
                          |
                     +----v----+
                     |  Redis  |
                     +---------+
```

When a client connected to Instance A makes an edit:
1. Instance A applies the update locally
2. Instance A publishes the update to Redis
3. Instances B and C receive the update via subscription
4. Instances B and C apply the update and broadcast to their local clients

## Testing

The module includes both unit tests and integration tests:

```bash
# Run all tests
./gradlew :yhocuspocus-redis:test

# Run with Panama binding (requires Java 22+)
./gradlew :yhocuspocus-redis:test -PtestBinding=panama
```

Integration tests use TestContainers to spin up a real Redis instance.

## Extension Lifecycle

| Hook | Behavior |
|------|----------|
| `afterLoadDocument` | Subscribe to document channel |
| `onChange` | Publish update to Redis |
| `onStoreDocument` | Store document state in Redis |
| `beforeUnloadDocument` | Unsubscribe from channel |
| `onDestroy` | Close Redis connections |

## Dependencies

- `io.lettuce:lettuce-core` - Async Redis client
- `yhocuspocus` - Core collaborative server
- `ycrdt-core` - Y-CRDT interfaces (implementation selected at runtime)
