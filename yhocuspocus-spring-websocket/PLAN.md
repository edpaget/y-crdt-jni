# Future Development Plan

Planned enhancements for yhocuspocus-spring-websocket.

## Planned Features

### Spring WebFlux Support
- Reactive WebSocket transport for WebFlux applications
- Non-blocking message handling
- Backpressure support

### Multiple Endpoints
- Support for multiple WebSocket paths on same server
- Per-path configuration (different origins, interceptors)

### Metrics Integration
- Micrometer metrics for connection counts, message rates
- Spring Boot Actuator health indicator
- Custom metric tags support

### Security Integration
- Spring Security integration for authentication
- Method-level authorization on documents
- Rate limiting per connection/IP

### Session Management
- Redis-backed session sharing for horizontal scaling
- Sticky session support
- Session timeout configuration

### Compression
- WebSocket compression (permessage-deflate)
- Configurable compression threshold

## Not Planned

- SockJS fallback (WebSocket-only)
- STOMP protocol support (binary protocol only)
