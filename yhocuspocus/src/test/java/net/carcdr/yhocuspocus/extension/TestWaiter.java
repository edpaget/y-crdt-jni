package net.carcdr.yhocuspocus.extension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test helper extension that provides synchronization points for async operations.
 *
 * <p>This extension eliminates the need for Thread.sleep() and polling in tests by
 * providing CountDownLatches that can be awaited for specific lifecycle events.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * TestWaiter waiter = new TestWaiter();
 * YHocuspocus server = YHocuspocus.builder()
 *     .extension(waiter)
 *     .build();
 *
 * // Trigger document creation
 * connection.handleMessage(...);
 *
 * // Wait for document to be created (instead of polling)
 * waiter.awaitDocumentCreated(1, TimeUnit.SECONDS);
 * }</pre>
 */
public class TestWaiter implements Extension {

    private CountDownLatch connectLatch = new CountDownLatch(1);
    private CountDownLatch createDocumentLatch = new CountDownLatch(1);
    private CountDownLatch loadDocumentLatch = new CountDownLatch(1);
    private CountDownLatch afterLoadDocumentLatch = new CountDownLatch(1);
    private CountDownLatch changeLatch = new CountDownLatch(1);
    private CountDownLatch storeLatch = new CountDownLatch(1);
    private CountDownLatch afterStoreLatch = new CountDownLatch(1);
    private CountDownLatch disconnectLatch = new CountDownLatch(1);
    private CountDownLatch destroyLatch = new CountDownLatch(1);

    @Override
    public int priority() {
        // Use very low priority so we run last and see final state
        return 1;
    }

    @Override
    public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
        connectLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onCreateDocument(OnCreateDocumentPayload payload) {
        createDocumentLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onLoadDocument(OnLoadDocumentPayload payload) {
        loadDocumentLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> afterLoadDocument(AfterLoadDocumentPayload payload) {
        afterLoadDocumentLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onChange(OnChangePayload payload) {
        changeLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onStoreDocument(OnStoreDocumentPayload payload) {
        storeLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> afterStoreDocument(AfterStoreDocumentPayload payload) {
        afterStoreLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onDisconnect(OnDisconnectPayload payload) {
        disconnectLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onDestroy(OnDestroyPayload payload) {
        destroyLatch.countDown();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Waits for onConnect to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitConnect(long timeout, TimeUnit unit) throws InterruptedException {
        return connectLatch.await(timeout, unit);
    }

    /**
     * Waits for onCreateDocument to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitDocumentCreated(long timeout, TimeUnit unit) throws InterruptedException {
        return createDocumentLatch.await(timeout, unit);
    }

    /**
     * Waits for onLoadDocument to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitDocumentLoaded(long timeout, TimeUnit unit) throws InterruptedException {
        return loadDocumentLatch.await(timeout, unit);
    }

    /**
     * Waits for afterLoadDocument to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitAfterLoadDocument(long timeout, TimeUnit unit) throws InterruptedException {
        return afterLoadDocumentLatch.await(timeout, unit);
    }

    /**
     * Waits for onChange to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitChange(long timeout, TimeUnit unit) throws InterruptedException {
        return changeLatch.await(timeout, unit);
    }

    /**
     * Waits for onStoreDocument to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitStore(long timeout, TimeUnit unit) throws InterruptedException {
        return storeLatch.await(timeout, unit);
    }

    /**
     * Waits for afterStoreDocument to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitAfterStore(long timeout, TimeUnit unit) throws InterruptedException {
        return afterStoreLatch.await(timeout, unit);
    }

    /**
     * Waits for onDisconnect to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitDisconnect(long timeout, TimeUnit unit) throws InterruptedException {
        return disconnectLatch.await(timeout, unit);
    }

    /**
     * Waits for onDestroy to be called.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if event occurred within timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitDestroy(long timeout, TimeUnit unit) throws InterruptedException {
        return destroyLatch.await(timeout, unit);
    }

    /**
     * Resets a specific latch for reuse.
     *
     * @param count the number of events to wait for
     */
    public void resetConnectLatch(int count) {
        connectLatch = new CountDownLatch(count);
    }

    /**
     * Resets document creation latch for reuse.
     *
     * @param count the number of events to wait for
     */
    public void resetCreateDocumentLatch(int count) {
        createDocumentLatch = new CountDownLatch(count);
    }

    /**
     * Resets after document load latch for reuse.
     *
     * @param count the number of events to wait for
     */
    public void resetAfterLoadDocumentLoatch(int count) {
        afterLoadDocumentLatch = new CountDownLatch(count);
    }

    /**
     * Resets change latch for reuse.
     *
     * @param count the number of events to wait for
     */
    public void resetChangeLatch(int count) {
        changeLatch = new CountDownLatch(count);
    }

    /**
     * Resets store latch for reuse.
     *
     * @param count the number of events to wait for
     */
    public void resetStoreLatch(int count) {
        storeLatch = new CountDownLatch(count);
    }

    /**
     * Resets disconnect latch for reuse.
     *
     * @param count the number of events to wait for
     */
    public void resetDisconnectLatch(int count) {
        disconnectLatch = new CountDownLatch(count);
    }

    /**
     * Resets destroy latch for reuse.
     *
     * @param count the number of events to wait for
     */
    public void resetDestroyLatch(int count) {
        destroyLatch = new CountDownLatch(count);
    }
}
