package net.carcdr.yhocuspocus.redis;

import io.lettuce.core.XReadArgs;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Redis pub/sub functionality.
 * Demonstrates both Lettuce and Jedis usage.
 */
public class RedisPubSubTest extends RedisIntegrationTestBase {

    private static final String TEST_CHANNEL = "doc:test-doc";

    @Test
    public void testLettucePubSub() throws Exception {
        // Set up subscriber with Lettuce
        var pubSub = lettuceClient.pubSub();
        pubSub.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                waiter.onMessage(channel, message.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void subscribed(String channel, long count) {
                waiter.onSubscribed(channel);
            }
        });
        pubSub.async().subscribe(TEST_CHANNEL);

        // Wait for subscription
        assertTrue("Should subscribe", waiter.awaitSubscription(5, TimeUnit.SECONDS));

        // Publish message
        lettuceClient.sync().publish(TEST_CHANNEL, "test-update-data");

        // Wait for message
        assertTrue("Should receive message", waiter.awaitMessage(5, TimeUnit.SECONDS));

        RedisTestWaiter.ReceivedMessage msg = waiter.pollMessage(100, TimeUnit.MILLISECONDS);
        assertNotNull("Message should not be null", msg);
        assertEquals(TEST_CHANNEL, msg.channel());
        assertEquals("test-update-data", new String(msg.message(), StandardCharsets.UTF_8));
    }

    @Test
    public void testJedisOperations() throws Exception {
        // Test basic Jedis operations
        var jedis = jedisClient.get();

        // Store document state
        String stateKey = "doc:test-doc:state";
        byte[] state = "document-state-bytes".getBytes(StandardCharsets.UTF_8);
        jedis.set(stateKey.getBytes(StandardCharsets.UTF_8), state);

        // Retrieve state
        byte[] retrieved = jedis.get(stateKey.getBytes(StandardCharsets.UTF_8));
        assertNotNull("State should be retrievable", retrieved);
        assertEquals("document-state-bytes", new String(retrieved, StandardCharsets.UTF_8));
    }

    @Test
    public void testStreamsWithLettuce() throws Exception {
        String streamKey = "doc:test-doc:updates";

        // Add to stream
        var commands = lettuceClient.sync();
        String messageId = commands.xadd(streamKey,
            "update", "binary-data",
            "origin", "instance-1");

        assertNotNull("Message ID should be returned", messageId);
        assertTrue("Message ID should have format", messageId.contains("-"));

        // Read from stream
        var messages = commands.xread(
            XReadArgs.StreamOffset.from(streamKey, "0"));

        assertEquals("Should have one message", 1, messages.size());
        assertEquals("Should be from correct stream", streamKey, messages.get(0).getStream());
    }

    @Test
    public void testLettuceKeyOperations() throws Exception {
        var commands = lettuceClient.sync();

        // Test string operations
        commands.set("test-key", "test-value");
        assertEquals("test-value", commands.get("test-key"));

        // Test key expiration
        commands.setex("expiring-key", 10, "expires-soon");
        Long ttl = commands.ttl("expiring-key");
        assertTrue("TTL should be positive", ttl > 0);
        assertTrue("TTL should be at most 10", ttl <= 10);

        // Test key deletion
        commands.del("test-key");
        String value = commands.get("test-key");
        assertEquals("Deleted key should return null", null, value);
    }

    @Test
    public void testJedisBinaryOperations() throws Exception {
        var jedis = jedisClient.get();

        // Test binary key/value storage (for Y-CRDT state)
        byte[] key = "binary-doc".getBytes(StandardCharsets.UTF_8);
        byte[] value = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF};

        jedis.set(key, value);
        byte[] retrieved = jedis.get(key);

        assertNotNull("Binary value should be retrievable", retrieved);
        assertEquals("Length should match", value.length, retrieved.length);
        for (int i = 0; i < value.length; i++) {
            assertEquals("Byte " + i + " should match", value[i], retrieved[i]);
        }
    }
}
