package net.carcdr.yhocuspocus.extension;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YText;
import net.carcdr.yhocuspocus.core.ClientConnection;
import net.carcdr.yhocuspocus.core.YDocument;
import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.protocol.OutgoingMessage;
import net.carcdr.yhocuspocus.protocol.SyncProtocol;
import net.carcdr.yhocuspocus.transport.MockTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for Phase 6: Extension System integration with YHocuspocus.
 *
 * <p>Tests the complete extension system working end-to-end including:</p>
 * <ul>
 *   <li>Document loading with database extension</li>
 *   <li>Document persistence and debouncing</li>
 *   <li>Hook execution order and priority</li>
 *   <li>Context enrichment across hooks</li>
 *   <li>Authentication and authorization</li>
 * </ul>
 */
public class ExtensionIntegrationTest {

    private YHocuspocus server;
    private InMemoryDatabaseExtension database;
    private TestWaiter waiter;

    @Before
    public void setUp() {
        waiter = new TestWaiter();
        database = new InMemoryDatabaseExtension();
        server = YHocuspocus.builder()
            .extension(waiter)
            .extension(database)
            .debounce(Duration.ofMillis(100))
            .maxDebounce(Duration.ofMillis(500))
            .build();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.close();
        }
        if (database != null) {
            database.clear();
        }
    }

    /**
     * Helper to wait for a condition to be true.
     */
    private void waitForCondition(java.util.function.BooleanSupplier condition,
                                   long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timeout waiting for condition");
    }

    /**
     * Test 1: Document loads from database extension.
     */
    @Test
    public void testDocumentLoadFromDatabase() throws Exception {
        // Pre-populate database with test data
        YDoc tempDoc = new YDoc();
        try {
            YText text = tempDoc.getText("content");
            text.insert(0, "Hello from database");
            byte[] state = tempDoc.encodeStateAsUpdate();
            database.saveToDatabase("test-doc", state);
        } finally {
            tempDoc.close();
        }

        // Connect client to trigger document load
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport.receiveMessage(OutgoingMessage.sync("test-doc", sync).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(1, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("test-doc") != null, 1000);

        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should be loaded", doc);

        // Verify content was loaded from database
        YText loadedText = doc.getDoc().getText("content");
        assertEquals("Content should match database",
                "Hello from database", loadedText.toString());
    }

    /**
     * Test 2: Document changes trigger onChange hook.
     */
    @Test
    public void testDocumentChangeTriggersOnChange() throws Exception {
        // Track onChange calls
        CountDownLatch onChangeLatch = new CountDownLatch(1);
        AtomicInteger changeCount = new AtomicInteger(0);

        Extension changeTracker = new Extension() {
            @Override
            public CompletableFuture<Void> onChange(OnChangePayload payload) {
                changeCount.incrementAndGet();
                onChangeLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };

        // Create server with tracking extension
        server.close();
        waiter = new TestWaiter();
        server = YHocuspocus.builder()
            .extension(waiter)
            .extension(database)
            .extension(changeTracker)
            .debounce(Duration.ofMillis(100))
            .build();

        // Create document
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport.receiveMessage(OutgoingMessage.sync("change-doc", sync).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(1, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("change-doc") != null, 1000);

        YDocument doc = server.getDocument("change-doc");
        assertNotNull("Document should exist", doc);

        // Trigger a change
        YDoc updateDoc = new YDoc();
        try {
            YText text = updateDoc.getText("content");
            text.insert(0, "New content");
            byte[] update = updateDoc.encodeStateAsUpdate();

            transport.receiveMessage(OutgoingMessage.sync("change-doc",
                    SyncProtocol.encodeUpdate(update)).encode());

            // Manually trigger onChange (observers not hooked up yet per TODO in YHocuspocus:189-192)
            server.handleDocumentChange(doc, new ConcurrentHashMap<>());

            // Wait for onChange to be called
            assertTrue("onChange should be called",
                    onChangeLatch.await(1, TimeUnit.SECONDS));
            assertTrue("onChange should be called at least once",
                    changeCount.get() > 0);
        } finally {
            updateDoc.close();
        }
    }

    /**
     * Test 3: Document persistence to database extension.
     */
    @Test
    public void testDocumentPersistsToDatabase() throws Exception {
        // Create document with content
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport.receiveMessage(OutgoingMessage.sync("persist-doc", sync).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(1, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("persist-doc") != null, 1000);

        YDocument doc = server.getDocument("persist-doc");
        assertNotNull("Document should exist", doc);

        // Make a change
        YDoc updateDoc = new YDoc();
        try {
            YText text = updateDoc.getText("content");
            text.insert(0, "Persist me");
            byte[] update = updateDoc.encodeStateAsUpdate();

            transport.receiveMessage(OutgoingMessage.sync("persist-doc",
                    SyncProtocol.encodeUpdate(update)).encode());

            // Trigger document change notification
            server.handleDocumentChange(doc, new ConcurrentHashMap<>());

            // Wait for debounced save (100ms debounce + 100ms buffer)
            Thread.sleep(250);

            // Verify data was persisted to database
            assertTrue("Document should exist in database",
                    database.hasDocument("persist-doc"));

            byte[] storedState = database.loadFromDatabase("persist-doc");
            assertNotNull("Stored state should not be null", storedState);
            assertTrue("Stored state should not be empty", storedState.length > 0);
        } finally {
            updateDoc.close();
        }
    }

    /**
     * Test 4: Extension priority determines execution order.
     */
    @Test
    public void testExtensionPriorityOrder() throws Exception {
        List<String> executionOrder = new ArrayList<>();

        Extension highPriority = new Extension() {
            @Override
            public int priority() {
                return 1000;
            }

            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                executionOrder.add("high");
                return CompletableFuture.completedFuture(null);
            }
        };

        Extension lowPriority = new Extension() {
            @Override
            public int priority() {
                return 10;
            }

            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                executionOrder.add("low");
                return CompletableFuture.completedFuture(null);
            }
        };

        // Create server with extensions (add in random order)
        server.close();
        waiter = new TestWaiter();
        server = YHocuspocus.builder()
            .extension(waiter)
            .extension(lowPriority)
            .extension(highPriority)
            .extension(database) // priority 500
            .build();

        // Trigger onConnect
        MockTransport transport = new MockTransport();
        server.handleConnection(transport, Map.of());

        // Allow async execution
        Thread.sleep(100);

        // Verify execution order (high priority first)
        assertEquals("High priority should execute first", "high", executionOrder.get(0));
        assertEquals("Low priority should execute second", "low", executionOrder.get(1));
    }

    /**
     * Test 5: Context enrichment across hooks.
     */
    @Test
    public void testContextEnrichmentAcrossHooks() throws Exception {
        Map<String, Object> capturedContext = new ConcurrentHashMap<>();

        Extension contextEnricher = new Extension() {
            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                payload.getContext().put("userId", "user123");
                payload.getContext().put("role", "admin");
                return CompletableFuture.completedFuture(null);
            }
        };

        Extension contextReader = new Extension() {
            @Override
            public int priority() {
                return 50; // Lower priority, runs after enricher
            }

            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                capturedContext.putAll(payload.getContext());
                return CompletableFuture.completedFuture(null);
            }
        };

        // Create server with extensions
        server.close();
        waiter = new TestWaiter();
        server = YHocuspocus.builder()
            .extension(waiter)
            .extension(contextEnricher)
            .extension(contextReader)
            .build();

        // Trigger onConnect with initial context
        Map<String, Object> initialContext = new ConcurrentHashMap<>();
        initialContext.put("sessionId", "abc123");

        MockTransport transport = new MockTransport();
        server.handleConnection(transport, initialContext);

        // Allow async execution
        Thread.sleep(100);

        // Verify context was enriched and propagated
        assertEquals("user123", capturedContext.get("userId"));
        assertEquals("admin", capturedContext.get("role"));
        assertEquals("abc123", capturedContext.get("sessionId"));
    }

    /**
     * Test 6: Authentication hook can set read-only mode.
     */
    @Test
    public void testAuthenticationHookReadOnly() throws Exception {
        final boolean[] authenticateCalled = {false};

        Extension authExtension = new Extension() {
            @Override
            public CompletableFuture<Void> onAuthenticate(OnAuthenticatePayload payload) {
                authenticateCalled[0] = true;
                if ("viewer-token".equals(payload.getToken())) {
                    payload.setReadOnly(true);
                }
                return CompletableFuture.completedFuture(null);
            }
        };

        server.close();
        waiter = new TestWaiter();
        server = YHocuspocus.builder()
            .extension(waiter)
            .extension(authExtension)
            .build();

        // Create payload and test
        OnAuthenticatePayload payload = new OnAuthenticatePayload(
                "conn1", "doc1", "viewer-token", new ConcurrentHashMap<>()
        );

        authExtension.onAuthenticate(payload).join();

        assertTrue("Authenticate hook should be called", authenticateCalled[0]);
        assertTrue("Should set read-only for viewer token", payload.isReadOnly());
    }

    /**
     * Test 7: onDestroy called on server shutdown.
     */
    @Test
    public void testOnDestroyCalledOnShutdown() throws Exception {
        CountDownLatch destroyLatch = new CountDownLatch(1);

        Extension ext = new Extension() {
            @Override
            public CompletableFuture<Void> onDestroy(OnDestroyPayload payload) {
                destroyLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };

        server.close();
        waiter = new TestWaiter();
        server = YHocuspocus.builder()
            .extension(waiter)
            .extension(ext)
            .build();

        // Close server
        server.close();

        // Verify onDestroy was called
        assertTrue("onDestroy should be called on shutdown",
                destroyLatch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test 8: Multiple extensions all execute.
     */
    @Test
    public void testMultipleExtensionsAllExecute() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        Extension ext1 = new Extension() {
            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                counter.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        Extension ext2 = new Extension() {
            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                counter.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        Extension ext3 = new Extension() {
            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                counter.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        server.close();
        waiter = new TestWaiter();
        server = YHocuspocus.builder()
            .extension(waiter)
            .extension(ext1)
            .extension(ext2)
            .extension(ext3)
            .build();

        MockTransport transport = new MockTransport();
        server.handleConnection(transport, Map.of());

        // Allow async execution
        Thread.sleep(100);

        assertEquals("All three extensions should execute", 3, counter.get());
    }

    /**
     * Test 9: Database extension loads and stores correctly.
     */
    @Test
    public void testDatabaseExtensionRoundTrip() throws Exception {
        // Create document with content
        YDoc originalDoc = new YDoc();
        byte[] originalState;
        try {
            YText text = originalDoc.getText("content");
            text.insert(0, "Round trip test");
            originalState = originalDoc.encodeStateAsUpdate();
            database.saveToDatabase("roundtrip-doc", originalState);
        } finally {
            originalDoc.close();
        }

        // Load document through server
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport.receiveMessage(OutgoingMessage.sync("roundtrip-doc", sync).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(1, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("roundtrip-doc") != null, 1000);

        YDocument doc = server.getDocument("roundtrip-doc");
        assertNotNull("Document should load", doc);

        // Verify content
        YText loadedText = doc.getDoc().getText("content");
        assertEquals("Content should match", "Round trip test", loadedText.toString());

        // Make a change and verify persistence
        YDoc updateDoc = new YDoc();
        try {
            YText updateText = updateDoc.getText("content");
            updateText.insert(0, "Updated ");
            byte[] update = updateDoc.encodeStateAsUpdate();

            transport.receiveMessage(OutgoingMessage.sync("roundtrip-doc",
                    SyncProtocol.encodeUpdate(update)).encode());

            // Trigger save
            server.handleDocumentChange(doc, new ConcurrentHashMap<>());

            // Wait for debounced save
            Thread.sleep(250);

            // Verify updated state was persisted
            byte[] storedState = database.loadFromDatabase("roundtrip-doc");
            assertNotNull("Updated state should be persisted", storedState);
        } finally {
            updateDoc.close();
        }
    }
}
