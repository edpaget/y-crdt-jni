package net.carcdr.yhocuspocus.extension;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for the extension system.
 */
public class ExtensionSystemTest {

    private List<String> executionOrder;

    @Before
    public void setUp() {
        executionOrder = new ArrayList<>();
    }

    @Test
    public void testExtensionPriority() {
        Extension highPriority = new Extension() {
            @Override
            public int priority() {
                return 500;
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
                return 100;
            }

            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                executionOrder.add("low");
                return CompletableFuture.completedFuture(null);
            }
        };

        List<Extension> extensions = new ArrayList<>();
        extensions.add(lowPriority);
        extensions.add(highPriority);

        // Sort by priority (higher first)
        extensions.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

        // Execute hooks
        OnConnectPayload payload = new OnConnectPayload("conn-1", new ConcurrentHashMap<>());
        extensions.forEach(ext -> ext.onConnect(payload).join());

        assertEquals("High priority should execute first", "high", executionOrder.get(0));
        assertEquals("Low priority should execute second", "low", executionOrder.get(1));
    }

    @Test
    public void testContextEnrichment() {
        Map<String, Object> context = new ConcurrentHashMap<>();

        Extension enricher = new Extension() {
            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                payload.getContext().put("userId", "user123");
                payload.getContext().put("role", "admin");
                return CompletableFuture.completedFuture(null);
            }
        };

        OnConnectPayload payload = new OnConnectPayload("conn-1", context);
        enricher.onConnect(payload).join();

        assertEquals("user123", context.get("userId"));
        assertEquals("admin", context.get("role"));
    }

    @Test
    public void testAuthenticationReadOnly() {
        Map<String, Object> context = new ConcurrentHashMap<>();

        Extension authExtension = new Extension() {
            @Override
            public CompletableFuture<Void> onAuthenticate(OnAuthenticatePayload payload) {
                if ("viewer".equals(payload.getToken())) {
                    payload.setReadOnly(true);
                }
                return CompletableFuture.completedFuture(null);
            }
        };

        OnAuthenticatePayload payload = new OnAuthenticatePayload(
            "conn-1", "doc1", "viewer", context
        );

        authExtension.onAuthenticate(payload).join();

        assertTrue("Should be read-only for viewer token", payload.isReadOnly());
    }

    @Test
    public void testDatabaseExtensionLoad() {
        InMemoryDatabaseExtension db = new InMemoryDatabaseExtension();

        // Pre-populate storage
        byte[] testData = new byte[] {1, 2, 3, 4, 5};
        db.saveToDatabase("doc1", testData);

        assertTrue("Document should exist", db.hasDocument("doc1"));
        assertEquals(1, db.getDocumentCount());

        byte[] loaded = db.loadFromDatabase("doc1");
        assertArrayEquals("Loaded data should match", testData, loaded);
    }

    @Test
    public void testDatabaseExtensionSave() {
        InMemoryDatabaseExtension db = new InMemoryDatabaseExtension();

        byte[] testData = new byte[] {10, 20, 30};
        db.saveToDatabase("doc2", testData);

        assertTrue("Document should be saved", db.hasDocument("doc2"));

        byte[] loaded = db.loadFromDatabase("doc2");
        assertArrayEquals("Saved data should persist", testData, loaded);
    }

    @Test
    public void testDatabaseExtensionClear() {
        InMemoryDatabaseExtension db = new InMemoryDatabaseExtension();

        db.saveToDatabase("doc1", new byte[] {1});
        db.saveToDatabase("doc2", new byte[] {2});

        assertEquals(2, db.getDocumentCount());

        db.clear();

        assertEquals(0, db.getDocumentCount());
        assertFalse(db.hasDocument("doc1"));
        assertFalse(db.hasDocument("doc2"));
    }

    @Test
    public void testDatabaseExtensionRemove() {
        InMemoryDatabaseExtension db = new InMemoryDatabaseExtension();

        db.saveToDatabase("doc1", new byte[] {1});
        db.saveToDatabase("doc2", new byte[] {2});

        assertEquals(2, db.getDocumentCount());

        db.removeDocument("doc1");

        assertEquals(1, db.getDocumentCount());
        assertFalse(db.hasDocument("doc1"));
        assertTrue(db.hasDocument("doc2"));
    }

    @Test
    public void testMultipleExtensionsExecute() {
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

        List<Extension> extensions = List.of(ext1, ext2, ext3);
        OnConnectPayload payload = new OnConnectPayload("conn-1", new ConcurrentHashMap<>());

        extensions.forEach(ext -> ext.onConnect(payload).join());

        assertEquals("All extensions should execute", 3, counter.get());
    }

    @Test
    public void testOnLoadDocumentStateWins() {
        byte[] firstState = new byte[] {1, 2, 3};
        byte[] secondState = new byte[] {4, 5, 6};

        Extension ext1 = new Extension() {
            @Override
            public CompletableFuture<Void> onLoadDocument(OnLoadDocumentPayload payload) {
                payload.setState(firstState);
                return CompletableFuture.completedFuture(null);
            }
        };

        Extension ext2 = new Extension() {
            @Override
            public CompletableFuture<Void> onLoadDocument(OnLoadDocumentPayload payload) {
                payload.setState(secondState);
                return CompletableFuture.completedFuture(null);
            }
        };

        OnLoadDocumentPayload payload = new OnLoadDocumentPayload(null, new ConcurrentHashMap<>());

        ext1.onLoadDocument(payload).join();
        ext2.onLoadDocument(payload).join();

        assertArrayEquals("First state should win", firstState, payload.getState());
    }

    @Test
    public void testOnDisconnectPayload() {
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put("userId", "user123");

        AtomicInteger disconnectCount = new AtomicInteger(0);

        Extension ext = new Extension() {
            @Override
            public CompletableFuture<Void> onDisconnect(OnDisconnectPayload payload) {
                assertEquals("conn-1", payload.getConnectionId());
                assertEquals("user123", payload.getContext().get("userId"));
                disconnectCount.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        OnDisconnectPayload payload = new OnDisconnectPayload("conn-1", context);
        ext.onDisconnect(payload).join();

        assertEquals(1, disconnectCount.get());
    }

    @Test
    public void testOnDestroyPayload() {
        AtomicInteger destroyCount = new AtomicInteger(0);

        Extension ext = new Extension() {
            @Override
            public CompletableFuture<Void> onDestroy(OnDestroyPayload payload) {
                destroyCount.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        OnDestroyPayload payload = new OnDestroyPayload();
        ext.onDestroy(payload).join();

        assertEquals(1, destroyCount.get());
    }

    @Test
    public void testDatabaseExtensionPriority() {
        InMemoryDatabaseExtension db = new InMemoryDatabaseExtension();

        assertEquals("Database extensions should have high priority", 500, db.priority());
    }

    @Test
    public void testOnStoreDocumentPayload() {
        byte[] state = new byte[] {1, 2, 3, 4, 5};
        AtomicInteger storeCount = new AtomicInteger(0);

        Extension ext = new Extension() {
            @Override
            public CompletableFuture<Void> onStoreDocument(OnStoreDocumentPayload payload) {
                assertArrayEquals(state, payload.getState());
                assertNotNull(payload.getContext());
                storeCount.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        OnStoreDocumentPayload payload = new OnStoreDocumentPayload(
            null,
            new ConcurrentHashMap<>(),
            state
        );

        ext.onStoreDocument(payload).join();

        assertEquals(1, storeCount.get());
    }

    @Test
    public void testBeforeAndAfterUnloadPayloads() {
        String docName = "test-doc";

        AtomicInteger beforeCount = new AtomicInteger(0);
        AtomicInteger afterCount = new AtomicInteger(0);

        Extension ext = new Extension() {
            @Override
            public CompletableFuture<Void> beforeUnloadDocument(BeforeUnloadDocumentPayload payload) {
                assertEquals(docName, payload.getDocumentName());
                beforeCount.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> afterUnloadDocument(AfterUnloadDocumentPayload payload) {
                assertEquals(docName, payload.getDocumentName());
                afterCount.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        BeforeUnloadDocumentPayload beforePayload = new BeforeUnloadDocumentPayload(null) {
            @Override
            public String getDocumentName() {
                return docName;
            }
        };

        AfterUnloadDocumentPayload afterPayload = new AfterUnloadDocumentPayload(docName);

        ext.beforeUnloadDocument(beforePayload).join();
        ext.afterUnloadDocument(afterPayload).join();

        assertEquals(1, beforeCount.get());
        assertEquals(1, afterCount.get());
    }
}
