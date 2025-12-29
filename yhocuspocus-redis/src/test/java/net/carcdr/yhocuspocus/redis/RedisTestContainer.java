package net.carcdr.yhocuspocus.redis;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Manages a Redis TestContainer for integration tests.
 *
 * <p>Provides a shared Redis instance that can be used across multiple tests.
 * Supports both singleton pattern for test suites and per-test instances.</p>
 */
public final class RedisTestContainer {

    private static final String REDIS_IMAGE = "redis:7.2-alpine";
    private static final int REDIS_PORT = 6379;

    private static GenericContainer<?> sharedInstance;

    private final GenericContainer<?> container;
    private final boolean shared;

    private RedisTestContainer(GenericContainer<?> container, boolean shared) {
        this.container = container;
        this.shared = shared;
    }

    /**
     * Creates a new Redis container for a single test.
     * Container is started automatically.
     *
     * @return new container instance
     */
    public static RedisTestContainer create() {
        GenericContainer<?> container = new GenericContainer<>(
            DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_PORT);
        container.start();
        return new RedisTestContainer(container, false);
    }

    /**
     * Gets or creates a shared Redis container.
     * Suitable for test suites where container startup overhead matters.
     *
     * @return shared container instance
     */
    public static synchronized RedisTestContainer shared() {
        if (sharedInstance == null || !sharedInstance.isRunning()) {
            sharedInstance = new GenericContainer<>(
                DockerImageName.parse(REDIS_IMAGE))
                .withExposedPorts(REDIS_PORT);
            sharedInstance.start();
        }
        return new RedisTestContainer(sharedInstance, true);
    }

    /**
     * Gets the Redis host.
     *
     * @return container host
     */
    public String getHost() {
        return container.getHost();
    }

    /**
     * Gets the mapped Redis port.
     *
     * @return mapped port number
     */
    public int getPort() {
        return container.getMappedPort(REDIS_PORT);
    }

    /**
     * Gets the Redis connection URI.
     *
     * @return URI in format redis://host:port
     */
    public String getRedisUri() {
        return "redis://" + getHost() + ":" + getPort();
    }

    /**
     * Checks if the container is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return container.isRunning();
    }

    /**
     * Stops the container.
     * Has no effect on shared containers.
     */
    public void stop() {
        if (!shared && container.isRunning()) {
            container.stop();
        }
    }

    /**
     * Forces shutdown of shared container.
     * Call this from test suite cleanup.
     */
    public static synchronized void stopShared() {
        if (sharedInstance != null && sharedInstance.isRunning()) {
            sharedInstance.stop();
            sharedInstance = null;
        }
    }
}
