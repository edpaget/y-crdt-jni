package net.carcdr.yhocuspocus.redis;

import net.carcdr.yhocuspocus.core.YDocument;
import net.carcdr.yhocuspocus.extension.AfterLoadDocumentPayload;
import net.carcdr.yhocuspocus.extension.BeforeUnloadDocumentPayload;
import net.carcdr.yhocuspocus.extension.Extension;
import net.carcdr.yhocuspocus.extension.OnChangePayload;
import net.carcdr.yhocuspocus.extension.OnDestroyPayload;
import net.carcdr.yhocuspocus.extension.OnStoreDocumentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis extension for horizontal scaling using pub/sub.
 *
 * <p>This extension synchronizes document updates across multiple server instances
 * using Redis pub/sub. When a document is modified on one instance, the update
 * is published to Redis and received by all other instances subscribed to that
 * document's channel.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * RedisClient redisClient = new LettuceRedisClient("localhost", 6379);
 * RedisExtensionConfig config = RedisExtensionConfig.builder()
 *     .prefix("myapp")
 *     .instanceId("instance-1")
 *     .build();
 *
 * RedisPubSubExtension extension = new RedisPubSubExtension(config, redisClient);
 *
 * YHocuspocus server = YHocuspocus.builder()
 *     .extension(extension)
 *     .build();
 * }</pre>
 *
 * <p>Key behaviors:</p>
 * <ul>
 *   <li>Subscribes to document channel when document is loaded</li>
 *   <li>Publishes updates to Redis when document changes locally</li>
 *   <li>Applies updates from other instances to local document</li>
 *   <li>Filters out self-published messages to avoid duplicate application</li>
 *   <li>Unsubscribes from channel when document is unloaded</li>
 * </ul>
 *
 * @see RedisExtensionConfig
 * @see RedisClient
 */
public class RedisPubSubExtension implements Extension {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPubSubExtension.class);

    private final RedisExtensionConfig config;
    private final RedisClient redisClient;
    private final Set<String> subscribedDocuments;
    private final Map<String, YDocument> documentCache;

    /**
     * Creates a new Redis pub/sub extension.
     *
     * @param config extension configuration
     * @param redisClient Redis client implementation
     */
    public RedisPubSubExtension(RedisExtensionConfig config, RedisClient redisClient) {
        this.config = config;
        this.redisClient = redisClient;
        this.subscribedDocuments = ConcurrentHashMap.newKeySet();
        this.documentCache = new ConcurrentHashMap<>();
    }

    @Override
    public int priority() {
        // Run before DatabaseExtension (priority 500) but after most other extensions
        return 50;
    }

    @Override
    public CompletableFuture<Void> afterLoadDocument(AfterLoadDocumentPayload payload) {
        YDocument document = payload.getDocument();
        String docName = document.getName();
        String channel = config.documentChannel(docName);

        LOG.debug("Subscribing to Redis channel for document: {}", docName);

        // Cache document reference for applying remote updates
        documentCache.put(docName, document);

        return redisClient.subscribe(channel, (ch, message) -> {
            handleRemoteMessage(docName, message);
        }).thenRun(() -> {
            subscribedDocuments.add(docName);
            LOG.debug("Subscribed to Redis channel: {}", channel);
        }).exceptionally(ex -> {
            LOG.error("Failed to subscribe to Redis channel: {}", channel, ex);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> onChange(OnChangePayload payload) {
        if (!redisClient.isConnected()) {
            LOG.warn("Redis not connected, skipping publish for document: {}",
                payload.getDocument().getName());
            return CompletableFuture.completedFuture(null);
        }

        String docName = payload.getDocument().getName();
        String channel = config.documentChannel(docName);
        byte[] update = payload.getUpdate();

        // Encode message with instance ID to allow filtering
        byte[] message = MessageCodec.encode(config.getInstanceId(), update);

        LOG.trace("Publishing update to Redis for document: {} ({} bytes)",
            docName, update.length);

        return redisClient.publish(channel, message)
            .exceptionally(ex -> {
                LOG.error("Failed to publish update to Redis for document: {}", docName, ex);
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> onStoreDocument(OnStoreDocumentPayload payload) {
        if (!redisClient.isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        String docName = payload.getDocumentName();
        byte[] state = payload.getState();
        String stateKey = config.stateKey(docName);

        LOG.debug("Storing document state to Redis: {} ({} bytes)", docName, state.length);

        return redisClient.set(stateKey, state)
            .exceptionally(ex -> {
                LOG.error("Failed to store document state to Redis: {}", docName, ex);
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> beforeUnloadDocument(BeforeUnloadDocumentPayload payload) {
        String docName = payload.getDocumentName();
        String channel = config.documentChannel(docName);

        LOG.debug("Unsubscribing from Redis channel for document: {}", docName);

        documentCache.remove(docName);
        subscribedDocuments.remove(docName);

        return redisClient.unsubscribe(channel)
            .exceptionally(ex -> {
                LOG.error("Failed to unsubscribe from Redis channel: {}", channel, ex);
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> onDestroy(OnDestroyPayload payload) {
        LOG.info("Shutting down Redis pub/sub extension");

        // Unsubscribe from all channels
        CompletableFuture<?>[] unsubscribes = subscribedDocuments.stream()
            .map(docName -> redisClient.unsubscribe(config.documentChannel(docName)))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(unsubscribes)
            .thenRun(() -> {
                subscribedDocuments.clear();
                documentCache.clear();
                redisClient.close();
            })
            .exceptionally(ex -> {
                LOG.error("Error during Redis extension shutdown", ex);
                return null;
            });
    }

    /**
     * Handles a message received from Redis.
     *
     * @param docName the document name
     * @param rawMessage the raw message bytes
     */
    private void handleRemoteMessage(String docName, byte[] rawMessage) {
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(rawMessage);
        if (decoded == null) {
            LOG.warn("Failed to decode Redis message for document: {}", docName);
            return;
        }

        // Skip self-published messages
        if (decoded.isFrom(config.getInstanceId())) {
            LOG.trace("Skipping self-published message for document: {}", docName);
            return;
        }

        YDocument document = documentCache.get(docName);
        if (document == null) {
            LOG.warn("Received update for unknown document: {}", docName);
            return;
        }

        LOG.trace("Applying remote update for document: {} from instance: {}",
            docName, decoded.instanceId());

        try {
            // Apply the update to the local document
            byte[] update = decoded.payload();
            document.getDoc().applyUpdate(update);

            // Broadcast to local clients (but not back to Redis)
            // Use SyncProtocol.encodeUpdate to create the proper sync message format
            byte[] syncPayload = net.carcdr.yhocuspocus.protocol.SyncProtocol.encodeUpdate(update);
            document.broadcastToAll(
                net.carcdr.yhocuspocus.protocol.OutgoingMessage
                    .sync(document.getName(), syncPayload)
                    .encode()
            );
        } catch (Exception e) {
            LOG.error("Failed to apply remote update for document: {}", docName, e);
        }
    }

    /**
     * Gets the set of currently subscribed document names.
     *
     * @return set of document names
     */
    public Set<String> getSubscribedDocuments() {
        return Set.copyOf(subscribedDocuments);
    }

    /**
     * Gets the configuration for this extension.
     *
     * @return the configuration
     */
    public RedisExtensionConfig getConfig() {
        return config;
    }

    /**
     * Checks if the Redis client is connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return redisClient.isConnected();
    }
}
