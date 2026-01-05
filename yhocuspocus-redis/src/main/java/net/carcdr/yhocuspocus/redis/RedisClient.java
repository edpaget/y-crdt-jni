package net.carcdr.yhocuspocus.redis;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Abstract interface for Redis operations used by the Redis extension.
 *
 * <p>This interface abstracts the underlying Redis client library (Lettuce, Jedis, etc.)
 * to allow different implementations and easier testing.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
public interface RedisClient extends AutoCloseable {

    /**
     * Publishes a message to a channel.
     *
     * @param channel the channel to publish to
     * @param message the message bytes
     * @return a future that completes when the message is sent
     */
    CompletableFuture<Void> publish(String channel, byte[] message);

    /**
     * Subscribes to a channel.
     *
     * @param channel the channel to subscribe to
     * @param handler callback for received messages (channel, message)
     * @return a future that completes when subscribed
     */
    CompletableFuture<Void> subscribe(String channel, BiConsumer<String, byte[]> handler);

    /**
     * Unsubscribes from a channel.
     *
     * @param channel the channel to unsubscribe from
     * @return a future that completes when unsubscribed
     */
    CompletableFuture<Void> unsubscribe(String channel);

    /**
     * Gets a binary value by key.
     *
     * @param key the key
     * @return a future containing the value, or null if not found
     */
    CompletableFuture<byte[]> get(String key);

    /**
     * Sets a binary value by key.
     *
     * @param key the key
     * @param value the value
     * @return a future that completes when set
     */
    CompletableFuture<Void> set(String key, byte[] value);

    /**
     * Deletes a key.
     *
     * @param key the key to delete
     * @return a future that completes when deleted
     */
    CompletableFuture<Void> delete(String key);

    /**
     * Checks if the client is connected.
     *
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Closes the client and releases resources.
     */
    @Override
    void close();
}
