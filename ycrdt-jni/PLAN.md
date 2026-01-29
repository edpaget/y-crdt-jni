# ycrdt-jni Development Plan

## Next Tasks

### v1.0.0

1. **Performance Benchmarking** -- Run full benchmark suite and validate targets (< 20% JNI overhead, > 100k ops/sec for basic operations). See [ycrdt-benchmarks](../ycrdt-benchmarks/).

2. **Maven Central Publishing** -- Set up account, configure build.gradle, create release process, publish first RC.

3. **Documentation** -- Tutorial guides, memory management best practices, Y.js migration guide.

4. **Multi-Platform Testing** -- Validate on all supported platforms before release (Linux x86_64/aarch64, macOS x86_64/aarch64, Windows x86_64).

## Future Enhancements (Post-v1.0)

- Boolean and integer type support (currently only string/double)
- Nested collection types (arrays of arrays, maps of maps)
- Custom serialization formats (beyond JSON)
- Compression support for large updates
- Undo/redo support
- Diff/patch operations
