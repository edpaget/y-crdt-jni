package net.carcdr.yhocuspocus.redis;

import net.carcdr.yhocuspocus.core.YDocument;
import net.carcdr.yhocuspocus.extension.AfterLoadDocumentPayload;
import net.carcdr.yhocuspocus.extension.BeforeUnloadDocumentPayload;
import net.carcdr.yhocuspocus.extension.OnChangePayload;
import net.carcdr.yhocuspocus.extension.OnStoreDocumentPayload;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MockRedisExtension behavior.
 * Tests the extension lifecycle hooks in isolation.
 */
public class RedisExtensionIntegrationTest {

    private MockRedisExtension mockRedis;

    @Before
    public void setUp() {
        mockRedis = new MockRedisExtension();
    }

    @Test
    public void testSubscriptionOnDocumentLoad() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("test-doc");

        AfterLoadDocumentPayload payload = new AfterLoadDocumentPayload(
            mockDoc, new ConcurrentHashMap<>());

        mockRedis.afterLoadDocument(payload).join();

        assertTrue("Should subscribe to doc channel",
            mockRedis.getSubscribedChannels().contains("doc:test-doc"));
    }

    @Test
    public void testPublishOnChange() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("change-doc");

        byte[] update = "test-update".getBytes();
        OnChangePayload payload = new OnChangePayload(
            mockDoc, new ConcurrentHashMap<>(), update);

        mockRedis.onChange(payload).join();

        var updates = mockRedis.getUpdatesForDocument("change-doc");
        assertEquals("Should publish one update", 1, updates.size());
        assertEquals("change-doc", updates.get(0).documentName());
    }

    @Test
    public void testMultipleDocumentsSubscribeToDifferentChannels() throws Exception {
        YDocument mockDoc1 = mock(YDocument.class);
        when(mockDoc1.getName()).thenReturn("doc-alpha");

        YDocument mockDoc2 = mock(YDocument.class);
        when(mockDoc2.getName()).thenReturn("doc-beta");

        mockRedis.afterLoadDocument(new AfterLoadDocumentPayload(
            mockDoc1, new ConcurrentHashMap<>())).join();

        mockRedis.afterLoadDocument(new AfterLoadDocumentPayload(
            mockDoc2, new ConcurrentHashMap<>())).join();

        var channels = mockRedis.getSubscribedChannels();
        assertTrue("Should subscribe to doc-alpha", channels.contains("doc:doc-alpha"));
        assertTrue("Should subscribe to doc-beta", channels.contains("doc:doc-beta"));
        assertEquals("Should have two subscriptions", 2, channels.size());
    }

    @Test
    public void testStoreOnStoreDocument() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("store-doc");

        byte[] state = "document-state".getBytes();
        OnStoreDocumentPayload payload = new OnStoreDocumentPayload(
            mockDoc, new ConcurrentHashMap<>(), state);

        mockRedis.onStoreDocument(payload).join();

        byte[] storedState = mockRedis.getStoredState("store-doc");
        assertNotNull("State should be stored", storedState);
        assertEquals("document-state", new String(storedState));
    }

    @Test
    public void testUnsubscribeOnDocumentUnload() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("unload-doc");

        // First load the document
        mockRedis.afterLoadDocument(new AfterLoadDocumentPayload(
            mockDoc, new ConcurrentHashMap<>())).join();

        assertTrue("Should be subscribed",
            mockRedis.getSubscribedChannels().contains("doc:unload-doc"));

        // Then unload it
        mockRedis.beforeUnloadDocument(new BeforeUnloadDocumentPayload(mockDoc)).join();

        assertFalse("Should be unsubscribed",
            mockRedis.getSubscribedChannels().contains("doc:unload-doc"));
    }

    @Test
    public void testConnectionFailureIsRecoverable() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("fail-doc");

        // Start connected
        assertTrue("Should start connected", mockRedis.isConnected());

        // Disconnect
        mockRedis.disconnect();
        assertFalse("Should be disconnected", mockRedis.isConnected());

        // Operations should fail when disconnected
        try {
            mockRedis.afterLoadDocument(new AfterLoadDocumentPayload(
                mockDoc, new ConcurrentHashMap<>())).join();
        } catch (Exception e) {
            assertTrue("Should throw on disconnect",
                e.getCause().getMessage().contains("Redis connection failed"));
        }

        // Reconnect
        mockRedis.reconnect();
        assertTrue("Should be reconnected", mockRedis.isConnected());

        // Operations should work again
        mockRedis.afterLoadDocument(new AfterLoadDocumentPayload(
            mockDoc, new ConcurrentHashMap<>())).join();

        assertTrue("Should subscribe after reconnect",
            mockRedis.getSubscribedChannels().contains("doc:fail-doc"));
    }

    @Test
    public void testClearResetsAllState() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("clear-doc");

        // Add some state
        mockRedis.afterLoadDocument(new AfterLoadDocumentPayload(
            mockDoc, new ConcurrentHashMap<>())).join();

        mockRedis.onChange(new OnChangePayload(
            mockDoc, new ConcurrentHashMap<>(), "update".getBytes())).join();

        mockRedis.onStoreDocument(new OnStoreDocumentPayload(
            mockDoc, new ConcurrentHashMap<>(), "state".getBytes())).join();

        // Verify we have state
        assertFalse("Should have subscriptions", mockRedis.getSubscribedChannels().isEmpty());
        assertFalse("Should have updates", mockRedis.getPublishedUpdates().isEmpty());
        assertNotNull("Should have stored state", mockRedis.getStoredState("clear-doc"));

        // Clear
        mockRedis.clear();

        // Verify all cleared
        assertTrue("Subscriptions should be cleared",
            mockRedis.getSubscribedChannels().isEmpty());
        assertTrue("Updates should be cleared",
            mockRedis.getPublishedUpdates().isEmpty());
        assertNull("Stored state should be cleared",
            mockRedis.getStoredState("clear-doc"));
    }

    @Test
    public void testExtensionPriorityIsRespected() {
        assertEquals("Mock Redis extension should have priority 50", 50, mockRedis.priority());
    }
}
