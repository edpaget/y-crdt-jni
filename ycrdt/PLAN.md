# ycrdt Development Plan

## Next Tasks

### Production Readiness (v1.0.0)

1. **Performance Benchmarking** (See [plans/YCRDT_BENCHMARKS.md](../plans/YCRDT_BENCHMARKS.md))
   - Set up JMH (Java Microbenchmark Harness) infrastructure
   - Benchmark all CRDT types: YText, YArray, YMap, YXml
   - Measure JNI overhead vs native Rust
   - Compare transaction performance (implicit vs explicit)
   - Benchmark sync protocol operations
   - Identify and optimize bottlenecks
   - Establish baseline metrics for regression testing
   - Target: < 20% JNI overhead, > 100k ops/sec for basic operations

2. **Concurrent Access Patterns**
   - Test thread safety under concurrent load
   - Document threading model and constraints
   - Add concurrent access tests

3. **Maven Central Publishing**
   - Set up Maven Central account
   - Configure publishing in build.gradle
   - Create release process documentation
   - Publish first release candidate

4. **Documentation Enhancement**
   - Tutorial guides for common use cases
   - Best practices for memory management
   - Performance optimization guide
   - Migration guide from JavaScript Y.js

5. **Multi-Platform Distribution**
   - Automate cross-compilation for Linux/macOS/Windows
   - Create fat JAR with all native libraries
   - Test on all supported platforms

## Future Enhancements (Post-v1.0)

- Boolean and integer type support (currently only string/double)
- Nested collection types (arrays of arrays, maps of maps)
- Custom serialization formats (beyond JSON)
- Compression support for large updates
- Undo/redo support
- Diff/patch operations
