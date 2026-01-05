package net.carcdr.yhocuspocus.redis;

import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Redis client implementation using Lettuce.
 *
 * <p>Uses binary codec for values to support Y-CRDT state storage.</p>
 */
public class LettuceRedisClient implements RedisClient {

    private final io.lettuce.core.RedisClient lettuceClient;
    private final io.lettuce.core.api.StatefulRedisConnection<String, byte[]> connection;
    private final StatefulRedisPubSubConnection<String, byte[]> pubSubConnection;
    private final Map<String, BiConsumer<String, byte[]>> subscriptionHandlers;
    private volatile boolean closed = false;

    /**
     * Creates a new Lettuce-based Redis client.
     *
     * @param host Redis host
     * @param port Redis port
     */
    public LettuceRedisClient(String host, int port) {
        this(RedisURI.create(host, port));
    }

    /**
     * Creates a new Lettuce-based Redis client.
     *
     * @param uri Redis URI
     */
    public LettuceRedisClient(RedisURI uri) {
        this.lettuceClient = io.lettuce.core.RedisClient.create(uri);
        this.connection = lettuceClient.connect(StringBinaryCodec.INSTANCE);
        this.pubSubConnection = lettuceClient.connectPubSub(StringBinaryCodec.INSTANCE);
        this.subscriptionHandlers = new ConcurrentHashMap<>();

        // Set up pub/sub listener
        pubSubConnection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, byte[] message) {
                BiConsumer<String, byte[]> handler = subscriptionHandlers.get(channel);
                if (handler != null) {
                    handler.accept(channel, message);
                }
            }
        });
    }

    /**
     * Creates a new Lettuce-based Redis client from an existing Lettuce client.
     *
     * <p>This constructor is useful for Spring integration where the client
     * is already configured.</p>
     *
     * @param lettuceClient the existing Lettuce client
     */
    public LettuceRedisClient(io.lettuce.core.RedisClient lettuceClient) {
        this.lettuceClient = lettuceClient;
        this.connection = lettuceClient.connect(StringBinaryCodec.INSTANCE);
        this.pubSubConnection = lettuceClient.connectPubSub(StringBinaryCodec.INSTANCE);
        this.subscriptionHandlers = new ConcurrentHashMap<>();

        pubSubConnection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, byte[] message) {
                BiConsumer<String, byte[]> handler = subscriptionHandlers.get(channel);
                if (handler != null) {
                    handler.accept(channel, message);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> publish(String channel, byte[] message) {
        return connection.async()
            .publish(channel, message)
            .toCompletableFuture()
            .thenApply(count -> null);
    }

    @Override
    public CompletableFuture<Void> subscribe(String channel, BiConsumer<String, byte[]> handler) {
        subscriptionHandlers.put(channel, handler);
        RedisPubSubAsyncCommands<String, byte[]> async = pubSubConnection.async();
        return async.subscribe(channel)
            .toCompletableFuture()
            .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(String channel) {
        subscriptionHandlers.remove(channel);
        RedisPubSubAsyncCommands<String, byte[]> async = pubSubConnection.async();
        return async.unsubscribe(channel)
            .toCompletableFuture()
            .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<byte[]> get(String key) {
        return connection.async()
            .get(key)
            .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> set(String key, byte[] value) {
        return connection.async()
            .set(key, value)
            .toCompletableFuture()
            .thenApply(ok -> null);
    }

    @Override
    public CompletableFuture<Void> delete(String key) {
        return connection.async()
            .del(key)
            .toCompletableFuture()
            .thenApply(count -> null);
    }

    @Override
    public boolean isConnected() {
        return !closed && connection.isOpen();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            subscriptionHandlers.clear();
            pubSubConnection.close();
            connection.close();
            lettuceClient.shutdown();
        }
    }

    /**
     * Custom codec that uses String keys and byte[] values.
     */
    private static final class StringBinaryCodec implements RedisCodec<String, byte[]> {

        static final StringBinaryCodec INSTANCE = new StringBinaryCodec();

        private final StringCodec stringCodec = StringCodec.UTF8;
        private final ByteArrayCodec byteArrayCodec = ByteArrayCodec.INSTANCE;

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return stringCodec.decodeKey(bytes);
        }

        @Override
        public byte[] decodeValue(ByteBuffer bytes) {
            return byteArrayCodec.decodeValue(bytes);
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public ByteBuffer encodeValue(byte[] value) {
            return ByteBuffer.wrap(value);
        }
    }
}
