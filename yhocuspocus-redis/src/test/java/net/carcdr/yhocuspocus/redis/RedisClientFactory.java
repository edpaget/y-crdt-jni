package net.carcdr.yhocuspocus.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import redis.clients.jedis.JedisPooled;

/**
 * Factory for creating Redis clients in tests.
 * Supports both Lettuce and Jedis to ensure the extension works with either.
 */
public final class RedisClientFactory {

    private RedisClientFactory() {
        // Utility class
    }

    /**
     * Creates a Lettuce synchronous client.
     *
     * @param container the Redis container
     * @return Lettuce client wrapper
     */
    public static LettuceTestClient lettuce(RedisTestContainer container) {
        RedisClient client = RedisClient.create(container.getRedisUri());
        return new LettuceTestClient(client);
    }

    /**
     * Creates a Jedis client.
     *
     * @param container the Redis container
     * @return Jedis client wrapper
     */
    public static JedisTestClient jedis(RedisTestContainer container) {
        JedisPooled jedis = new JedisPooled(container.getHost(), container.getPort());
        return new JedisTestClient(jedis);
    }

    /**
     * Lettuce client wrapper for testing.
     */
    public static final class LettuceTestClient implements AutoCloseable {
        private final RedisClient client;
        private final StatefulRedisConnection<String, String> connection;
        private StatefulRedisPubSubConnection<String, String> pubSubConnection;

        LettuceTestClient(RedisClient client) {
            this.client = client;
            this.connection = client.connect();
        }

        /**
         * Gets synchronous Redis commands.
         *
         * @return Redis commands
         */
        public RedisCommands<String, String> sync() {
            return connection.sync();
        }

        /**
         * Gets the raw Lettuce client for advanced operations.
         *
         * @return Lettuce RedisClient
         */
        public RedisClient getClient() {
            return client;
        }

        /**
         * Gets or creates a pub/sub connection.
         *
         * @return pub/sub connection
         */
        public StatefulRedisPubSubConnection<String, String> pubSub() {
            if (pubSubConnection == null) {
                pubSubConnection = client.connectPubSub();
            }
            return pubSubConnection;
        }

        /**
         * Flushes all data from Redis.
         */
        public void flushAll() {
            connection.sync().flushall();
        }

        @Override
        public void close() {
            if (pubSubConnection != null) {
                pubSubConnection.close();
            }
            connection.close();
            client.shutdown();
        }
    }

    /**
     * Jedis client wrapper for testing.
     */
    public static final class JedisTestClient implements AutoCloseable {
        private final JedisPooled jedis;

        JedisTestClient(JedisPooled jedis) {
            this.jedis = jedis;
        }

        /**
         * Gets the Jedis client.
         *
         * @return Jedis client
         */
        public JedisPooled get() {
            return jedis;
        }

        /**
         * Flushes all data from Redis.
         */
        public void flushAll() {
            jedis.flushAll();
        }

        @Override
        public void close() {
            jedis.close();
        }
    }
}
