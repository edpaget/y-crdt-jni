package net.carcdr.yhocuspocus.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for YHocuspocus Spring integration.
 *
 * <p>These properties can be configured in application.yml or application.properties:</p>
 *
 * <pre>{@code
 * yhocuspocus:
 *   path: /collaboration
 *   allowed-origins:
 *     - "http://localhost:3000"
 *   debounce: 2s
 *   max-debounce: 10s
 * }</pre>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "yhocuspocus")
public class YHocuspocusProperties {

    /**
     * WebSocket endpoint path.
     */
    private String path = "/";

    /**
     * Allowed origins for WebSocket connections.
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of("*"));

    /**
     * Debounce duration before saving document changes.
     */
    private Duration debounce = Duration.ofSeconds(2);

    /**
     * Maximum debounce duration before forcing a save.
     */
    private Duration maxDebounce = Duration.ofSeconds(10);

    /**
     * Gets the WebSocket endpoint path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the WebSocket endpoint path.
     *
     * @param path the path (must start with '/')
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the allowed origins.
     *
     * @return list of allowed origins
     */
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Sets the allowed origins.
     *
     * @param allowedOrigins list of allowed origins
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Gets the debounce duration.
     *
     * @return debounce duration
     */
    public Duration getDebounce() {
        return debounce;
    }

    /**
     * Sets the debounce duration.
     *
     * @param debounce debounce duration
     */
    public void setDebounce(Duration debounce) {
        this.debounce = debounce;
    }

    /**
     * Gets the maximum debounce duration.
     *
     * @return max debounce duration
     */
    public Duration getMaxDebounce() {
        return maxDebounce;
    }

    /**
     * Sets the maximum debounce duration.
     *
     * @param maxDebounce max debounce duration
     */
    public void setMaxDebounce(Duration maxDebounce) {
        this.maxDebounce = maxDebounce;
    }
}
