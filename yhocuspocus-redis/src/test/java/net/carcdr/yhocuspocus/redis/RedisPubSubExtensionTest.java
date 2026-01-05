package net.carcdr.yhocuspocus.redis;

import net.carcdr.yhocuspocus.core.YDocument;
import net.carcdr.yhocuspocus.extension.AfterLoadDocumentPayload;
import net.carcdr.yhocuspocus.extension.BeforeUnloadDocumentPayload;
import net.carcdr.yhocuspocus.extension.OnChangePayload;
import net.carcdr.yhocuspocus.extension.OnDestroyPayload;
import net.carcdr.yhocuspocus.extension.OnStoreDocumentPayload;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RedisPubSubExtension.
 */
public class RedisPubSubExtensionTest {

    private RedisPubSubExtension extension;
    private MockRedisClient mockRedisClient;
    private RedisExtensionConfig config;

    @Before
    public void setUp() {
        mockRedisClient = new MockRedisClient();
        config = RedisExtensionConfig.builder()
            .prefix("test")
            .instanceId("test-instance")
            .build();
        extension = new RedisPubSubExtension(config, mockRedisClient);
    }

    @Test
    public void testPriorityIs50() {
        assertEquals("Priority should be 50", 50, extension.priority());
    }

    @Test
    public void testAfterLoadDocumentSubscribes() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("test-doc");

        AfterLoadDocumentPayload payload = new AfterLoadDocumentPayload(
            mockDoc, new ConcurrentHashMap<>());

        extension.afterLoadDocument(payload).join();

        assertTrue("Should subscribe to channel",
            mockRedisClient.subscribedChannels.contains("test:doc:test-doc"));
        assertTrue("Document should be tracked",
            extension.getSubscribedDocuments().contains("test-doc"));
    }

    @Test
    public void testOnChangePublishesUpdate() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("change-doc");

        byte[] update = new byte[]{0x01, 0x02, 0x03};
        OnChangePayload payload = new OnChangePayload(
            mockDoc, new ConcurrentHashMap<>(), update);

        extension.onChange(payload).join();

        assertEquals("Should publish to channel", 1, mockRedisClient.publishedMessages.size());
        MockRedisClient.PublishedMessage msg = mockRedisClient.publishedMessages.get(0);
        assertEquals("test:doc:change-doc", msg.channel);

        // Verify message is encoded with instance ID
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(msg.message);
        assertEquals("test-instance", decoded.instanceId());
    }

    @Test
    public void testOnStoreDocumentStoresState() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("store-doc");

        byte[] state = "document-state".getBytes();
        OnStoreDocumentPayload payload = new OnStoreDocumentPayload(
            mockDoc, new ConcurrentHashMap<>(), state);

        extension.onStoreDocument(payload).join();

        assertEquals("Should store state", state, mockRedisClient.storedValues.get("test:doc:store-doc:state"));
    }

    @Test
    public void testBeforeUnloadDocumentUnsubscribes() throws Exception {
        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("unload-doc");

        // First load the document
        AfterLoadDocumentPayload loadPayload = new AfterLoadDocumentPayload(
            mockDoc, new ConcurrentHashMap<>());
        extension.afterLoadDocument(loadPayload).join();

        assertTrue("Should be subscribed",
            extension.getSubscribedDocuments().contains("unload-doc"));

        // Then unload it
        BeforeUnloadDocumentPayload unloadPayload = new BeforeUnloadDocumentPayload(mockDoc);
        extension.beforeUnloadDocument(unloadPayload).join();

        assertFalse("Should no longer be subscribed",
            extension.getSubscribedDocuments().contains("unload-doc"));
        assertTrue("Should unsubscribe from channel",
            mockRedisClient.unsubscribedChannels.contains("test:doc:unload-doc"));
    }

    @Test
    public void testOnDestroyClosesClient() throws Exception {
        extension.onDestroy(new OnDestroyPayload()).join();

        assertTrue("Client should be closed", mockRedisClient.closed);
    }

    @Test
    public void testGetConfig() {
        assertEquals("Should return config", config, extension.getConfig());
    }

    @Test
    public void testIsConnected() {
        assertTrue("Should be connected initially", extension.isConnected());

        mockRedisClient.connected = false;
        assertFalse("Should reflect client state", extension.isConnected());
    }

    @Test
    public void testOnChangeSkipsWhenDisconnected() throws Exception {
        mockRedisClient.connected = false;

        YDocument mockDoc = mock(YDocument.class);
        when(mockDoc.getName()).thenReturn("disconnected-doc");

        OnChangePayload payload = new OnChangePayload(
            mockDoc, new ConcurrentHashMap<>(), new byte[]{0x01});

        extension.onChange(payload).join();

        assertTrue("Should not publish when disconnected",
            mockRedisClient.publishedMessages.isEmpty());
    }

    /**
     * Mock Redis client for testing.
     */
    private static final class MockRedisClient implements RedisClient {
        final List<String> subscribedChannels = new ArrayList<>();
        final List<String> unsubscribedChannels = new ArrayList<>();
        final List<PublishedMessage> publishedMessages = new ArrayList<>();
        final Map<String, byte[]> storedValues = new ConcurrentHashMap<>();
        boolean connected = true;
        boolean closed = false;

        @Override
        public CompletableFuture<Void> publish(String channel, byte[] message) {
            publishedMessages.add(new PublishedMessage(channel, message));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> subscribe(String channel, BiConsumer<String, byte[]> handler) {
            subscribedChannels.add(channel);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> unsubscribe(String channel) {
            unsubscribedChannels.add(channel);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<byte[]> get(String key) {
            return CompletableFuture.completedFuture(storedValues.get(key));
        }

        @Override
        public CompletableFuture<Void> set(String key, byte[] value) {
            storedValues.put(key, value);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> delete(String key) {
            storedValues.remove(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void close() {
            closed = true;
        }

        record PublishedMessage(String channel, byte[] message) { }
    }
}
