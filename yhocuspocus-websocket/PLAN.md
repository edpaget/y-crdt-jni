# yhocuspocus-websocket Development Plan

## Next Tasks

### Configuration

- Configurable max message size (currently hardcoded to 10MB)
- Connection limits with rejection when full
- WebSocket permessage-deflate compression

### Monitoring

- Connection/message metrics (Micrometer/Prometheus)
- HTTP health check endpoint
- Structured logging for connection lifecycle

### Security

- SSL/TLS connector configuration
- Per-connection rate limiting
- IP allow/deny lists

### Performance

- Zero-copy ByteBuffer handling
- Benchmarking suite (throughput, latency, concurrent connections)

## Future Enhancements (Post-v1.0)

- HTTP/2 WebSocket support (Jetty 12)
- Multiple documents per WebSocket connection (multiplexing)
