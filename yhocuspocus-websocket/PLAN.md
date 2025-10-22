# yhocuspocus-websocket Development Plan

## Next Tasks

### Configuration Enhancements

1. **Configurable Message Size Limit**
   - Add builder method for max message size
   - Currently hardcoded to 10MB
   - Make configurable per use case

2. **Connection Limits**
   - Add max connections configuration
   - Reject new connections when limit reached
   - Track active connection count

3. **Compression Support**
   - Add WebSocket permessage-deflate extension
   - Configurable compression level
   - Reduce bandwidth for large documents

### Monitoring & Observability

1. **Metrics**
   - Connection count (active, total)
   - Message count (sent, received)
   - Message size (average, max)
   - Error rates
   - Integration with Micrometer/Prometheus

2. **Health Check Endpoint**
   - HTTP health check at /health
   - Check server status
   - Check connection pool status
   - Integration with load balancers

3. **Structured Logging**
   - Connection lifecycle events
   - Message statistics
   - Error details with context
   - Configurable log levels

### Security

1. **TLS/SSL Support**
   - SSL connector configuration
   - Certificate management
   - Optional client certificate auth

2. **Rate Limiting**
   - Per-connection message rate limits
   - Per-IP connection limits
   - Configurable thresholds

3. **IP Whitelisting/Blacklisting**
   - Allow/deny lists
   - Integration with firewall rules

### Performance

1. **Connection Pooling**
   - Reuse endpoint instances
   - Reduce GC pressure
   - Benchmark improvements

2. **Zero-Copy Message Handling**
   - Direct ByteBuffer handling
   - Reduce memory copying
   - Benchmark improvements

3. **Benchmarking Suite**
   - Concurrent connection tests
   - Message throughput tests
   - Latency measurements
   - Comparison with Hocuspocus TypeScript

## Future Enhancements (Post-v1.0)

### Alternative Implementations

1. **Spring WebFlux Transport**
   - Alternative to Jetty
   - Spring Boot integration
   - Reactive streams

2. **Netty Transport**
   - Lower-level control
   - Custom protocol handling
   - Maximum performance

### Advanced Features

1. **HTTP/2 Support**
   - Leverage Jetty 12 HTTP/2
   - WebSocket over HTTP/2
   - Reduced latency

2. **Multiplexing**
   - Multiple documents per WebSocket
   - Reduce connection overhead
   - More complex routing

3. **Custom Protocol Extensions**
   - WebSocket subprotocol negotiation
   - Custom extensions
   - Binary protocol improvements
