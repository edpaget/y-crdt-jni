# ycrdt-panama Development Plan

## E2E Test Debugging Plan

### Problem Statement

Panama e2e tests fail with text truncation/interleaving during concurrent collaborative editing, while unit tests (including `CrossImplementationSyncTest`) pass. CI allows Panama tests to fail without blocking the build.

### Symptoms

- Text gets truncated or interleaved when multiple users edit simultaneously
- Affects collaborative editing scenarios with 2+ concurrent users
- Only manifests in async WebSocket-based e2e tests, not synchronous unit tests

### Key Differences Between Passing Unit Tests and Failing E2E Tests

| Aspect | Unit Tests | E2E Tests |
|--------|-----------|-----------|
| Timing | Synchronous | Async via WebSocket |
| Sync Mechanism | Direct `applyUpdate()` | `observeUpdateV1()` -> broadcast -> `applyUpdate()` |
| Concurrency | Sequential operations | Concurrent editing |
| Transport | None | WebSocket + yhocuspocus |

### Debugging Steps

#### Phase 1: Reproduce and Isolate

1. **Run e2e tests locally with Panama binding**
   ```bash
   cd examples/fullstack/e2e
   BACKEND=jetty BINDING=panama npx playwright test --headed
   ```

2. **Enable verbose logging in yhocuspocus**
   - Add logging for sync messages received/sent
   - Log update byte arrays (hex dump) at key points
   - Track connection state transitions

3. **Compare JNI vs Panama sync behavior**
   - Run same e2e test with JNI binding
   - Capture network traffic/sync messages from both runs
   - Compare update byte arrays for identical operations

#### Phase 2: Narrow Down the Cause

4. **Verify observer callback timing**
   - Add timestamp logging in `PanamaYSubscription.onUpdate()`
   - Check if callbacks fire at expected times
   - Verify update data is correctly read from native memory

5. **Check for race conditions in Panama implementation**
   - Review `Arena` lifecycle in `PanamaYSubscription`
   - Check thread-safety of `ThreadLocal<PanamaYTransaction>` in `PanamaYDoc`
   - Verify `MemorySegment.reinterpret()` in callback doesn't cause issues

6. **Test concurrent modifications isolated from WebSocket**
   - Write unit test that simulates rapid concurrent edits from multiple threads
   - Use `observeUpdateV1()` to capture and relay updates between docs
   - Compare behavior between JNI and Panama

#### Phase 3: Specific Areas to Investigate

7. **Transaction handling under concurrency**
   - `PanamaYDoc.beginTransactionInternal()` uses `ThreadLocal`
   - Check if concurrent transactions from observer callbacks cause issues
   - Verify transaction commit timing

8. **Memory management in callbacks**
   - `onUpdate()` reads from `MemorySegment data` parameter
   - Native memory may be freed after callback returns
   - Ensure data is fully copied before callback exits

9. **State vector / diff calculation**
   - Compare `encodeStateVector()` results between JNI and Panama
   - Compare `encodeDiff()` for identical scenarios
   - Check for off-by-one or encoding differences

#### Phase 4: Create Targeted Tests

10. **Add stress test for concurrent sync**
    ```java
    // Test multiple threads modifying docs and syncing via observers
    @Test
    public void testConcurrentSyncViaObservers() {
        // Create 2 docs with observers that relay updates
        // Spawn multiple threads making rapid edits
        // Verify both docs converge to same state
    }
    ```

11. **Add timing-sensitive test**
    - Simulate network latency in sync relay
    - Test with artificial delays between edit and sync

### Root Cause Identified

**Transaction creation semantics differ between JNI and Panama:**

| Aspect | JNI (Rust) | Panama (yffi) |
|--------|------------|---------------|
| Function | `transact_mut()` | `ydoc_write_transaction()` |
| Behavior | **Blocking** - waits for lock | **Non-blocking** - returns NULL |
| On contention | Caller waits | Caller gets NULL, throws exception |

**Evidence from logs:**
```
WebSocket connection closed: code=1011, reason=Failed to create transaction
```

Multiple concurrent WebSocket threads try to create transactions simultaneously. With yffi's non-blocking semantics, some threads get NULL and the connection is closed, causing update loss.

### Potential Fixes

1. **Retry with backoff** - When `ydocWriteTransaction()` returns NULL, retry with exponential backoff
2. **Document-level lock** - Add Java-level synchronization before calling yffi transaction functions
3. **Queue-based serialization** - Queue all operations and process them sequentially per document
4. **Use blocking yffi API** - Check if yffi has a blocking transaction function (unlikely)

### Previously Considered (Now Ruled Out)

1. ~~Observer callback memory lifetime~~ - Logs show callbacks fire correctly with valid data
2. ~~Arena lifecycle issue~~ - Subscription stays open during test
3. ~~State vector drift~~ - Updates are correct when they arrive

### Fix Applied

**Solution:** Added `ReentrantReadWriteLock` to `PanamaYDoc` to serialize transaction access.

**Changes:**
1. `PanamaYDoc.java`: Added `txnLock` field with fair ordering (`new ReentrantReadWriteLock(true)`)
2. `PanamaYDoc.beginTransactionInternal()`: Acquires write lock before calling yffi
3. `PanamaYTransaction.java`: Added `ownsWriteLock` flag
4. `PanamaYTransaction.close()`: Releases write lock after transaction commits

**Results:**
| Metric | Before Fix | After Fix |
|--------|-----------|-----------|
| E2E tests passed | 0-2 of 4 (non-deterministic) | 3 of 4 (consistent) |
| Sync truncation | Severe (95%+ data loss) | None observed |
| Transaction errors | Frequent "Failed to create transaction" | None |

**Comparison with JNI:** JNI passes 4/4 consistently. The remaining Panama flakiness appears to be a timing issue in the "new user joining" test, not a sync problem.

### Remaining Work

- [ ] Investigate timing-related flakiness in "new user joining sees existing content" test
- [ ] Consider using read lock for read-only operations (currently all ops use write lock)

---

## Remaining YXML Features

### 1. Observers (Partially Implemented)

XML type observers are implemented with basic functionality.

**Implemented:**
- `YXmlFragment.observe(YObserver)` - Fully working
- `YXmlElement.observe(YObserver)` - Implemented, child changes work
- `YXmlText.observe(YObserver)` - Implemented, text changes work

**Implementation Details:**
- `PanamaYXmlSubscription` handles upcall stubs for native-to-Java callbacks
- `PanamaYEvent` wraps native events with change lists
- Change classes: `PanamaYArrayChange` (child changes), `PanamaYXmlElementChange` (attributes), `PanamaYTextChange` (text deltas)

**Known Limitations:**
- Attribute change value extraction not implemented (native YOutput parsing complex)
- Tests limited to fragment observers due to native memory handling issues with element/text observers
- TODO: Implement full YOutput parsing for attribute old/new values

### 2. Text Formatting

Rich text formatting within XML text nodes.

**Missing:**
- `YXmlText.insertWithAttributes(txn, index, chunk, Map<String, Object>)`
- `YXmlText.format(txn, index, length, Map<String, Object>)`
- `YXmlText.getFormattingChunks()` / `getFormattingChunks(YTransaction)`

**Complexity:** High - requires:
- Constructing `YInput` map structures for attribute dictionaries
- Parsing delta/chunk output from yffi for `getFormattingChunks`
- Creating a `FormattingChunk` implementation class

### 3. Element Attributes

Currently implemented but may need verification.

**Implemented:**
- `setAttribute`, `getAttribute`, `removeAttribute`, `getAttributeNames`

**Potentially Missing:**
- `getTag()` for elements created via `insertElement` (currently only tested on fragments)

### 4. Top-Level YXmlText

yffi doesn't provide a direct way to create top-level XML text nodes.

**Current Status:** `PanamaYDoc.getXmlText()` throws `UnsupportedOperationException`

**Options:**
- Document as unsupported (users should use fragment + insertText)
- Investigate if yffi can support this via alternative approach

## Priority Order

1. **E2E Test Debugging** - Mostly resolved (transaction locking fix applied)
2. **Element Tag** - Verify existing implementation works for child elements
3. ~~**Observers**~~ - Basic implementation complete, attribute value parsing pending
4. **Text Formatting** - High effort, needed for rich text editing
5. **Top-Level YXmlText** - Low priority, workaround exists

## Other Pending Features

### Subdocuments
- `YArray.getDoc()`, `insertDoc()`, `pushDoc()`
- `YMap.getDoc()`, `setDoc()`

### Type-specific Observers
- `YText.observe(YObserver)`
- `YArray.observe(YObserver)`
- `YMap.observe(YObserver)`
