# ycrdt Development Plan

## Next Tasks

### Production Readiness (v1.0.0)

1. **Performance Benchmarking** (In Progress - See [ycrdt-benchmarks/README.md](../ycrdt-benchmarks/README.md))
   - [x] JMH infrastructure configured (3 warmup, 5 measurement, 2 forks)
   - [x] Core types benchmarked: YText, YArray, YMap, YDoc, Sync (56 benchmarks)
   - [x] XML types benchmarked: YXmlFragment, YXmlElement (YXmlBenchmark.java)
   - [x] Observer performance benchmarked (ObserverBenchmark.java)
   - [x] Baseline metrics captured in baseline.json
   - [x] JNI vs Panama comparison automated via @Param
   - [ ] Run full benchmark suite and validate targets
   - Target: < 20% JNI overhead, > 100k ops/sec for basic operations

2. **Concurrent Access Patterns** (Completed)
   - Added ConcurrentAccessTest.java with 18 multi-threaded test scenarios
   - Validated synchronized access patterns (YText, YArray, YMap, transactions)
   - Tested concurrent document creation/destruction
   - Tested thread-per-document isolation pattern
   - Tested concurrent sync operations and differential sync
   - Tested observer callbacks under concurrent load
   - Verified high-contention scenarios (16 threads competing for single lock)
   - Threading model documented in JniYDoc JavaDoc

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
