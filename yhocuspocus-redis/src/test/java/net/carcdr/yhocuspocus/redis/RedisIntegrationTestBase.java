package net.carcdr.yhocuspocus.redis;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.testcontainers.DockerClientFactory;

import java.util.function.BooleanSupplier;

/**
 * Base class for Redis integration tests.
 *
 * <p>Manages shared TestContainer lifecycle and provides Redis client setup.
 * Subclasses can use either Lettuce or Jedis clients.</p>
 *
 * <p>Tests are automatically skipped if Docker is not available.</p>
 */
public abstract class RedisIntegrationTestBase {

    protected static RedisTestContainer redisContainer;
    private static boolean dockerAvailable;

    protected RedisClientFactory.LettuceTestClient lettuceClient;
    protected RedisClientFactory.JedisTestClient jedisClient;
    protected RedisTestWaiter waiter;

    @BeforeClass
    public static void setUpRedis() {
        // Check if Docker is available before attempting to start container
        dockerAvailable = isDockerAvailable();
        Assume.assumeTrue("Docker is not available, skipping Redis integration tests",
            dockerAvailable);

        redisContainer = RedisTestContainer.shared();
    }

    /**
     * Checks if Docker is available on this system.
     *
     * @return true if Docker is available and responsive
     */
    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    @AfterClass
    public static void tearDownRedis() {
        // Keep shared container running for other tests
        // Call RedisTestContainer.stopShared() in suite cleanup if needed
    }

    @Before
    public void setUp() throws Exception {
        waiter = new RedisTestWaiter();

        // Create clients - tests choose which to use
        lettuceClient = RedisClientFactory.lettuce(redisContainer);
        jedisClient = RedisClientFactory.jedis(redisContainer);

        // Clean Redis state before each test
        lettuceClient.flushAll();

        // Call subclass setup
        setUpTest();
    }

    @After
    public void tearDown() throws Exception {
        // Call subclass cleanup
        tearDownTest();

        // Clean up clients
        if (lettuceClient != null) {
            lettuceClient.close();
        }
        if (jedisClient != null) {
            jedisClient.close();
        }
    }

    /**
     * Override for test-specific setup.
     * Called after Redis clients are created and database is flushed.
     *
     * @throws Exception if setup fails
     */
    protected void setUpTest() throws Exception {
        // Default: no additional setup
    }

    /**
     * Override for test-specific cleanup.
     * Called before Redis clients are closed.
     *
     * @throws Exception if cleanup fails
     */
    protected void tearDownTest() throws Exception {
        // Default: no additional cleanup
    }

    /**
     * Gets the Redis host for direct connection.
     *
     * @return Redis host
     */
    protected String getRedisHost() {
        return redisContainer.getHost();
    }

    /**
     * Gets the Redis port for direct connection.
     *
     * @return Redis port
     */
    protected int getRedisPort() {
        return redisContainer.getPort();
    }

    /**
     * Helper to wait for a condition with timeout.
     *
     * @param condition condition to check
     * @param timeoutMs timeout in milliseconds
     * @throws InterruptedException if interrupted
     * @throws AssertionError if timeout reached
     */
    protected void waitForCondition(BooleanSupplier condition,
                                     long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timeout waiting for condition after " + timeoutMs + "ms");
    }
}
