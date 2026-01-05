package net.carcdr.yhocuspocus.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for Redis extensions.
 *
 * <p>Use the builder pattern to create instances:</p>
 * <pre>{@code
 * RedisExtensionConfig config = RedisExtensionConfig.builder()
 *     .prefix("myapp")
 *     .instanceId("instance-1")
 *     .build();
 * }</pre>
 */
public final class RedisExtensionConfig {

    private final String prefix;
    private final String instanceId;
    private final Duration awarenessThrottle;
    private final boolean awarenessEnabled;

    private RedisExtensionConfig(Builder builder) {
        this.prefix = builder.prefix;
        this.instanceId = builder.instanceId != null
            ? builder.instanceId
            : UUID.randomUUID().toString();
        this.awarenessThrottle = builder.awarenessThrottle;
        this.awarenessEnabled = builder.awarenessEnabled;
    }

    /**
     * Creates a new builder for RedisExtensionConfig.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration.
     *
     * @return default configuration
     */
    public static RedisExtensionConfig defaults() {
        return builder().build();
    }

    /**
     * Gets the key prefix for Redis keys.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Gets the unique instance ID for this server.
     *
     * @return the instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the minimum interval between awareness updates.
     *
     * @return the throttle duration
     */
    public Duration getAwarenessThrottle() {
        return awarenessThrottle;
    }

    /**
     * Checks if cross-instance awareness sync is enabled.
     *
     * @return true if awareness sync is enabled
     */
    public boolean isAwarenessEnabled() {
        return awarenessEnabled;
    }

    /**
     * Generates the channel name for document updates.
     *
     * @param documentName the document name
     * @return the channel name
     */
    public String documentChannel(String documentName) {
        return prefix + ":doc:" + documentName;
    }

    /**
     * Generates the channel name for awareness updates.
     *
     * @param documentName the document name
     * @return the awareness channel name
     */
    public String awarenessChannel(String documentName) {
        return prefix + ":awareness:" + documentName;
    }

    /**
     * Generates the key for document state storage.
     *
     * @param documentName the document name
     * @return the state key
     */
    public String stateKey(String documentName) {
        return prefix + ":doc:" + documentName + ":state";
    }

    /**
     * Generates the key for document state vector storage.
     *
     * @param documentName the document name
     * @return the state vector key
     */
    public String vectorKey(String documentName) {
        return prefix + ":doc:" + documentName + ":vector";
    }

    /**
     * Builder for RedisExtensionConfig.
     */
    public static final class Builder {
        private String prefix = "yhocuspocus";
        private String instanceId;
        private Duration awarenessThrottle = Duration.ofMillis(100);
        private boolean awarenessEnabled = true;

        private Builder() {
        }

        /**
         * Sets the Redis key prefix.
         *
         * @param prefix the prefix (defaults to "yhocuspocus")
         * @return this builder
         */
        public Builder prefix(String prefix) {
            this.prefix = Objects.requireNonNull(prefix, "prefix must not be null");
            return this;
        }

        /**
         * Sets the unique instance ID for this server.
         *
         * <p>If not set, a random UUID will be generated.</p>
         *
         * @param instanceId the instance ID
         * @return this builder
         */
        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * Sets the minimum interval between awareness updates.
         *
         * @param throttle the throttle duration (defaults to 100ms)
         * @return this builder
         */
        public Builder awarenessThrottle(Duration throttle) {
            this.awarenessThrottle = Objects.requireNonNull(throttle,
                "awarenessThrottle must not be null");
            return this;
        }

        /**
         * Enables or disables cross-instance awareness sync.
         *
         * @param enabled true to enable awareness sync (default: true)
         * @return this builder
         */
        public Builder awarenessEnabled(boolean enabled) {
            this.awarenessEnabled = enabled;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the configuration
         */
        public RedisExtensionConfig build() {
            return new RedisExtensionConfig(this);
        }
    }
}
