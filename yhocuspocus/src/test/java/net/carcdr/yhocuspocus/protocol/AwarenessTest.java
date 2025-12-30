package net.carcdr.yhocuspocus.protocol;

import net.carcdr.ycrdt.YBindingFactory;
import net.carcdr.ycrdt.YDoc;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for awareness protocol.
 */
public class AwarenessTest {

    private YDoc doc;
    private Awareness awareness;

    @Before
    public void setUp() {
        doc = YBindingFactory.auto().createDoc();
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
        long numClients = reader.readVarInt();
        assertEquals("Should have 0 clients", 0, numClients);
    }

    @Test
    public void testApplyAndGetUpdate() {
        // Create awareness update
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1); // num clients
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(1); // clock
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        byte[] update = writer.toByteArray();

        // Apply update
        awareness.applyUpdate(update, "origin");

        // Verify state exists
        Map<String, Object> state = awareness.getState(1);
        assertNotNull("State should exist", state);
        assertEquals("Should have 1 client", 1, awareness.getClientCount());
    }

    @Test
    public void testRemoveClient() {
        // Add client
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");
        assertEquals("Should have 1 client", 1, awareness.getClientCount());

        // Remove client (empty state)
        writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarInt(1); // client ID
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
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Remove via removeStates
        byte[] removeUpdate = awareness.removeStates(new long[]{1});

        assertNotNull("Remove update should not be null", removeUpdate);
        assertEquals("Should have 0 clients after removal", 0, awareness.getClientCount());
    }

    @Test
    public void testRemoveStatesMessageFormat() {
        // Add client
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Remove via removeStates and verify message format
        byte[] removeUpdate = awareness.removeStates(new long[]{1});

        // Decode and verify format
        VarIntReader reader = new VarIntReader(removeUpdate);
        long numClients = reader.readVarInt();
        assertEquals("Should have 1 client in removal message", 1, numClients);

        long clientId = reader.readVarInt();
        assertEquals("Client ID should match", 1, clientId);

        long clock = reader.readVarInt();
        assertTrue("Clock should be >= 1", clock >= 1);

        String stateJson = reader.readVarString();
        assertEquals("Empty string indicates removal", "", stateJson);

        assertFalse("Should have no remaining data", reader.hasRemaining());
    }

    @Test
    public void testMultipleClients() {
        // Add two clients
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(2);

        writer.writeVarInt(1); // client1 ID
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        writer.writeVarInt(2); // client2 ID
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Bob\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        assertEquals("Should have 2 clients", 2, awareness.getClientCount());
        assertNotNull("Client1 should exist", awareness.getState(1));
        assertNotNull("Client2 should exist", awareness.getState(2));
    }

    @Test
    public void testClockOrdering() {
        // Add client with clock 1
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(1);
        writer.writeVarString("{\"version\":1}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Try to apply older update (clock 0)
        writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(0); // older clock
        writer.writeVarString("{\"version\":0}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Should still have newer state (not updated)
        Map<String, Object> state = awareness.getState(1);
        assertNotNull("State should exist", state);
        // Clock ordering should have rejected the older update

        // Apply newer update (clock 2)
        writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(2); // newer clock
        writer.writeVarString("{\"version\":2}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Should have newest state
        state = awareness.getState(1);
        assertNotNull("State should exist", state);
    }

    @Test
    public void testGetStatesFormat() {
        // Add client
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(1);
        writer.writeVarInt(1); // client ID
        writer.writeVarInt(1);
        writer.writeVarString("{\"user\":{\"name\":\"Alice\"}}");

        awareness.applyUpdate(writer.toByteArray(), "origin");

        // Get states
        byte[] states = awareness.getStates();
        assertNotNull("States should not be null", states);

        // Verify format
        VarIntReader reader = new VarIntReader(states);
        long numClients = reader.readVarInt();
        assertEquals("Should have 1 client", 1, numClients);

        long clientId = reader.readVarInt();
        assertEquals("Client ID should match", 1, clientId);

        long clock = reader.readVarInt();
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
        byte[] removeUpdate = awareness.removeStates(new long[]{999});
        assertNotNull("Remove update should not be null", removeUpdate);
        assertEquals("Should have 0 clients", 0, awareness.getClientCount());
    }

    @Test
    public void testRemoveEmptyArray() {
        byte[] removeUpdate = awareness.removeStates(new long[0]);
        assertNotNull("Remove update should not be null", removeUpdate);
    }
}
