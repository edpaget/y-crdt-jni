package net.carcdr.ycrdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Tests for the Y-CRDT Observer API.
 */
public class YObserverTest {

    @Test
    public void testYTextChangeConstructors() {
        // Test INSERT change
        YTextChange insert = new YTextChange("Hello", null);
        assertEquals(YChange.Type.INSERT, insert.getType());
        assertEquals("Hello", insert.getContent());
        assertEquals(5, insert.getLength());
        assertNotNull(insert.getAttributes());
        assertTrue(insert.getAttributes().isEmpty());

        // Test DELETE change
        YTextChange delete = new YTextChange(YChange.Type.DELETE, 10);
        assertEquals(YChange.Type.DELETE, delete.getType());
        assertEquals(null, delete.getContent());
        assertEquals(10, delete.getLength());

        // Test RETAIN change
        YTextChange retain = new YTextChange(YChange.Type.RETAIN, 5, null);
        assertEquals(YChange.Type.RETAIN, retain.getType());
        assertEquals(5, retain.getLength());
    }

    @Test
    public void testYArrayChangeConstructors() {
        // Test INSERT change
        List<Object> items = new ArrayList<>();
        items.add("apple");
        items.add("banana");
        YArrayChange insert = new YArrayChange(items);
        assertEquals(YChange.Type.INSERT, insert.getType());
        assertEquals(2, insert.getLength());
        assertEquals(2, insert.getItems().size());

        // Test DELETE change
        YArrayChange delete = new YArrayChange(YChange.Type.DELETE, 3);
        assertEquals(YChange.Type.DELETE, delete.getType());
        assertEquals(3, delete.getLength());
        assertTrue(delete.getItems().isEmpty());
    }

    @Test
    public void testYMapChangeConstructors() {
        // Test INSERT
        YMapChange insert = new YMapChange(YChange.Type.INSERT, "name", "Alice", null);
        assertEquals(YChange.Type.INSERT, insert.getType());
        assertEquals("name", insert.getKey());
        assertEquals("Alice", insert.getNewValue());
        assertEquals(null, insert.getOldValue());

        // Test ATTRIBUTE (update)
        YMapChange update = new YMapChange(
            YChange.Type.ATTRIBUTE, "age", 31, 30
        );
        assertEquals(YChange.Type.ATTRIBUTE, update.getType());
        assertEquals("age", update.getKey());
        assertEquals(31, update.getNewValue());
        assertEquals(30, update.getOldValue());
    }

    @Test
    public void testYEventConstruction() {
        List<YChange> changes = new ArrayList<>();
        changes.add(new YTextChange("test", null));

        YEvent event = new YEvent("target", changes, "test-origin");

        assertEquals("target", event.getTarget());
        assertEquals(1, event.getChanges().size());
        assertEquals("test-origin", event.getOrigin());
    }

    @Test
    public void testYSubscriptionBasics() {
        YObserver observer = event -> { };
        Object target = new Object();

        YSubscription sub = new YSubscription(1L, observer, target);

        assertEquals(1L, sub.getSubscriptionId());
        assertEquals(observer, sub.getObserver());
        assertEquals(target, sub.getTarget());
        assertFalse(sub.isClosed());
    }

    @Test
    public void testYTextObserveRegistration() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {

            // Should be able to register observer
            YObserver observer = event -> { };
            YSubscription sub = text.observe(observer);

            assertNotNull(sub);
            assertFalse(sub.isClosed());

            // Clean up
            sub.close();
            assertTrue(sub.isClosed());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testYTextObserveNullObserver() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.observe(null);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testYTextObserveAfterClose() {
        YDoc doc = new YDoc();
        YText text = doc.getText("test");
        text.close();

        // Should throw
        text.observe(event -> { });
    }
}
