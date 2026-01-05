package net.carcdr.yhocuspocus.redis;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for RedisExtensionConfig.
 */
public class RedisExtensionConfigTest {

    @Test
    public void testDefaultConfiguration() {
        RedisExtensionConfig config = RedisExtensionConfig.defaults();

        assertEquals("Default prefix", "yhocuspocus", config.getPrefix());
        assertNotNull("Instance ID should be generated", config.getInstanceId());
        assertFalse("Instance ID should not be empty", config.getInstanceId().isEmpty());
        assertEquals("Default awareness throttle", Duration.ofMillis(100), config.getAwarenessThrottle());
        assertTrue("Awareness should be enabled by default", config.isAwarenessEnabled());
    }

    @Test
    public void testBuilderWithCustomValues() {
        RedisExtensionConfig config = RedisExtensionConfig.builder()
            .prefix("myapp")
            .instanceId("instance-123")
            .awarenessThrottle(Duration.ofMillis(50))
            .awarenessEnabled(false)
            .build();

        assertEquals("Custom prefix", "myapp", config.getPrefix());
        assertEquals("Custom instance ID", "instance-123", config.getInstanceId());
        assertEquals("Custom throttle", Duration.ofMillis(50), config.getAwarenessThrottle());
        assertFalse("Awareness disabled", config.isAwarenessEnabled());
    }

    @Test
    public void testDocumentChannel() {
        RedisExtensionConfig config = RedisExtensionConfig.builder()
            .prefix("myapp")
            .build();

        assertEquals("myapp:doc:test-doc", config.documentChannel("test-doc"));
        assertEquals("myapp:doc:another", config.documentChannel("another"));
    }

    @Test
    public void testAwarenessChannel() {
        RedisExtensionConfig config = RedisExtensionConfig.builder()
            .prefix("custom")
            .build();

        assertEquals("custom:awareness:doc1", config.awarenessChannel("doc1"));
    }

    @Test
    public void testStateKey() {
        RedisExtensionConfig config = RedisExtensionConfig.builder()
            .prefix("test")
            .build();

        assertEquals("test:doc:mydoc:state", config.stateKey("mydoc"));
    }

    @Test
    public void testVectorKey() {
        RedisExtensionConfig config = RedisExtensionConfig.builder()
            .prefix("app")
            .build();

        assertEquals("app:doc:document:vector", config.vectorKey("document"));
    }

    @Test
    public void testGeneratedInstanceIdIsUnique() {
        RedisExtensionConfig config1 = RedisExtensionConfig.defaults();
        RedisExtensionConfig config2 = RedisExtensionConfig.defaults();

        assertFalse("Generated instance IDs should be unique",
            config1.getInstanceId().equals(config2.getInstanceId()));
    }

    @Test(expected = NullPointerException.class)
    public void testNullPrefixThrows() {
        RedisExtensionConfig.builder()
            .prefix(null)
            .build();
    }

    @Test(expected = NullPointerException.class)
    public void testNullThrottleThrows() {
        RedisExtensionConfig.builder()
            .awarenessThrottle(null)
            .build();
    }
}
