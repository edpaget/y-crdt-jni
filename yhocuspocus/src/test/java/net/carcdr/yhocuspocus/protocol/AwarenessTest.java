package net.carcdr.yhocuspocus.protocol;

import net.carcdr.ycrdt.YDoc;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for awareness protocol.
 */
public class AwarenessTest {

    private YDoc doc;
    private Awareness awareness;

    @Before
    public void setUp() {
        doc = new YDoc();
        awareness = new Awareness(doc);
    }

    @After
    public void tearDown() {
        if (doc != null) {
            doc.close();
        }
    }

    @Test
    public void testEmptyAwareness() {
        byte[] states = awareness.getStates();

        assertNotNull("States should not be null", states);

        // Decode and verify empty
        VarIntReader reader = new VarIntReader(states);
        int numClients = reader.readVarInt();
        assertEquals("Should have 0 clients", 0, numClients);
    }

    @Test
    public void testApplyAndGetUpdate() {
        // Create awareness update
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1); // num clients
        writer.writeVarString("client1");
        writer.writeVarInt(1); // clock
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        byte[] update = writer.toByteArray();

        // Apply update
        awareness.applyUpdate(update, "origin");

        // Verify state exists
        Map<String, Object> state = awareness.getState("client1");
        assertNotNull("State should exist", state);
        assertEquals("Should have 1 client", 1, awareness.getClientCount());
    }

    @Test
    public void testRemoveClient() {
        // Add client
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarString("client1");
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");
        assertEquals("Should have 1 client", 1, awareness.getClientCount());

        // Remove client (empty state)
        writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarString("client1");
        writer.writeVarInt(2);
        writer.writeVarString(""); // empty = remove

        awareness.applyUpdate(writer.toByteArray(), "origin");
        assertEquals("Should have 0 clients", 0, awareness.getClientCount());
    }

    @Test
    public void testRemoveStates() {
        // Add client
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarString("client1");
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Remove via removeStates
        byte[] removeUpdate = awareness.removeStates(new String[]{"client1"});

        assertNotNull("Remove update should not be null", removeUpdate);
        assertEquals("Should have 0 clients after removal", 0, awareness.getClientCount());
    }

    @Test
    public void testMultipleClients() {
        // Add two clients
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(2);

        writer.writeVarString("client1");
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        writer.writeVarString("client2");
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Bob\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        assertEquals("Should have 2 clients", 2, awareness.getClientCount());
        assertNotNull("Client1 should exist", awareness.getState("client1"));
        assertNotNull("Client2 should exist", awareness.getState("client2"));
    }

    @Test
    public void testClockOrdering() {
        // Add client with clock 1
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarString("client1");
        writer.writeVarInt(1);
        writer.writeVarString("{\"version\":1}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Try to apply older update (clock 0)
        writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarString("client1");
        writer.writeVarInt(0); // older clock
        writer.writeVarString("{\"version\":0}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Should still have newer state (not updated)
        Map<String, Object> state = awareness.getState("client1");
        assertNotNull("State should exist", state);
        // Clock ordering should have rejected the older update

        // Apply newer update (clock 2)
        writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarString("client1");
        writer.writeVarInt(2); // newer clock
        writer.writeVarString("{\"version\":2}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Should have newest state
        state = awareness.getState("client1");
        assertNotNull("State should exist", state);
    }

    @Test
    public void testGetStatesFormat() {
        // Add client
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarString("client1");
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Get states
        byte[] states = awareness.getStates();
        assertNotNull("States should not be null", states);

        // Verify format
        VarIntReader reader = new VarIntReader(states);
        int numClients = reader.readVarInt();
        assertEquals("Should have 1 client", 1, numClients);

        String clientId = reader.readVarString();
        assertEquals("Client ID should match", "client1", clientId);

        int clock = reader.readVarInt();
        assertTrue("Clock should be >= 1", clock >= 1);

        String stateJson = reader.readVarString();
        assertNotNull("State JSON should not be null", stateJson);
        assertTrue("State should contain user data", stateJson.contains("user"));
    }

    @Test
    public void testEmptyUpdateIgnored() {
        awareness.applyUpdate(new byte[0], "origin");
        assertEquals("Should still have 0 clients", 0, awareness.getClientCount());
    }

    @Test
    public void testNullUpdateIgnored() {
        awareness.applyUpdate(null, "origin");
        assertEquals("Should still have 0 clients", 0, awareness.getClientCount());
    }

    @Test
    public void testRemoveNonexistentClient() {
        byte[] removeUpdate = awareness.removeStates(new String[]{"nonexistent"});
        assertNotNull("Remove update should not be null", removeUpdate);
        assertEquals("Should have 0 clients", 0, awareness.getClientCount());
    }

    @Test
    public void testRemoveEmptyArray() {
        byte[] removeUpdate = awareness.removeStates(new String[0]);
        assertNotNull("Remove update should not be null", removeUpdate);
    }
}
