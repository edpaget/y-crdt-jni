# yhocuspocus-redis Development Plan

Future development tasks for the Redis horizontal scaling extension.

## Pending Tasks

### Redis Streams Implementation

- Implement `RedisStreamsExtension` for durable message sync
- Add consumer group management
- Implement stream trimming and snapshot creation
- Support catch-up after disconnect via pending message redelivery

### Distributed Locking

- Implement document ownership model with Redlock
- Add ownership heartbeat mechanism
- Coordinate persistence to prevent duplicate writes
- Implement ownership transfer on document unload

### Awareness Synchronization

- Add awareness pub/sub channel handling
- Integrate `AwarenessThrottler` into extension
- Support cross-instance cursor position sync
- Handle awareness on connect/disconnect

### Resilience Features

- Implement circuit breaker for Redis unavailability
- Add retry queue for failed publishes
- Support graceful degradation (local-only mode)
- Add reconnection handling with message catch-up

### Spring Boot Integration

- Create `yhocuspocus-redis-spring-boot-starter` module
- Add auto-configuration for Redis extension
- Support `application.yml` configuration properties
- Integrate with Spring Data Redis (`RedisConnectionFactory`)

### Monitoring

- Add metrics for publish/subscribe latency
- Track message counts and queue depths
- Expose health check endpoint
- Support distributed tracing

### Redis Cluster Support

- Test with Redis Cluster configuration
- Document cluster-specific considerations
- Consider sharded pub/sub for Redis 7.0+
