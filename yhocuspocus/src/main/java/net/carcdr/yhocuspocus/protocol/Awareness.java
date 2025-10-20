package net.carcdr.yhocuspocus.protocol;

import net.carcdr.ycrdt.YDoc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Awareness protocol for tracking user presence.
 *
 * <p>Manages user states (cursor position, selection, user info)
 * and synchronizes them across connections. The awareness protocol
 * is separate from the Y.CRDT document updates and provides ephemeral
 * state that doesn't need to be persisted.</p>
 *
 * <p>Typical awareness state includes:</p>
 * <ul>
 *   <li>User information (name, color, avatar)</li>
 *   <li>Cursor position and selection range</li>
 *   <li>Active status and last seen timestamp</li>
 * </ul>
 *
 * <p>Message format:</p>
 * <pre>
 * [numClients: varInt]
 * For each client:
 *   [clientId: varInt]
 *   [clock: varInt]
 *   [state: varString (JSON)]
 * </pre>
 *
 * @see <a href="https://docs.yjs.dev/getting-started/adding-awareness">Yjs Awareness</a>
 */
public class Awareness {

    private final YDoc doc;
    private final ConcurrentHashMap<Long, Map<String, Object>> states;
    private final ConcurrentHashMap<Long, Long> lastSeen;
    private final ConcurrentHashMap<Long, Long> clocks;
    private final ObjectMapper jsonMapper;

    /**
     * Creates a new awareness instance.
     *
     * @param doc the associated YDoc (currently unused, reserved for future use)
     */
    public Awareness(YDoc doc) {
        this.doc = doc;
        this.states = new ConcurrentHashMap<>();
        this.lastSeen = new ConcurrentHashMap<>();
        this.clocks = new ConcurrentHashMap<>();
        this.jsonMapper = new ObjectMapper();
    }

    /**
     * Applies an awareness update.
     *
     * <p>Updates are encoded as a series of client states.
     * Empty state (empty JSON string) indicates client removal.</p>
     *
     * @param update the encoded awareness update
     * @param origin the connection ID that sent the update
     * @throws IllegalArgumentException if update is invalid
     */
    public void applyUpdate(byte[] update, String origin) {
        if (update == null || update.length == 0) {
            return;
        }

        try {
            VarIntReader reader = new VarIntReader(update);
            long numClients = reader.readVarInt();

            for (long i = 0; i < numClients; i++) {
                long clientId = reader.readVarInt();
                long clock = reader.readVarInt();

                // Read state (JSON encoded)
                String stateJson = reader.readVarString();

                // Check if this is a removal (empty state)
                if (stateJson == null || stateJson.isEmpty() || "null".equals(stateJson)) {
                    // Client removed
                    states.remove(clientId);
                    lastSeen.remove(clientId);
                    clocks.remove(clientId);
                } else {
                    // Client updated - only accept if clock is newer
                    Long currentClock = clocks.get(clientId);
                    if (currentClock == null || clock > currentClock) {
                        Map<String, Object> state = parseJson(stateJson);
                        states.put(clientId, state);
                        lastSeen.put(clientId, System.currentTimeMillis());
                        clocks.put(clientId, clock);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse awareness update: "
                + e.getMessage(), e);
        }
    }

    /**
     * Gets all awareness states.
     *
     * <p>Returns encoded awareness update containing all current client states.</p>
     *
     * @return encoded awareness states
     */
    public byte[] getStates() {
        if (states.isEmpty()) {
            // Return empty awareness update
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(0);
            return writer.toByteArray();
        }

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(states.size());

        states.forEach((clientId, state) -> {
            writer.writeVarInt(clientId);
            long clock = clocks.getOrDefault(clientId, 0L);
            writer.writeVarInt(clock);
            writer.writeVarString(toJson(state));
        });

        return writer.toByteArray();
    }

    /**
     * Gets awareness state for a specific client.
     *
     * @param clientId the client ID
     * @return the client's awareness state, or null if not found
     */
    public Map<String, Object> getState(long clientId) {
        return states.get(clientId);
    }

    /**
     * Removes awareness states for disconnected clients.
     *
     * <p>Returns an awareness update encoding the removals,
     * which can be broadcast to other clients.</p>
     *
     * @param clientIds the client IDs to remove
     * @return encoded removal update
     */
    public byte[] removeStates(long[] clientIds) {
        if (clientIds == null || clientIds.length == 0) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(0);
            return writer.toByteArray();
        }

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(clientIds.length); // Number of clients being removed

        for (long clientId : clientIds) {
            states.remove(clientId);
            lastSeen.remove(clientId);
            long clock = clocks.getOrDefault(clientId, 0L) + 1;
            clocks.put(clientId, clock);

            writer.writeVarInt(clientId);
            writer.writeVarInt(clock);
            writer.writeVarString(""); // Empty string indicates removal
        }

        return writer.toByteArray();
    }

    /**
     * Gets the number of active clients.
     *
     * @return number of clients with awareness state
     */
    public int getClientCount() {
        return states.size();
    }

    /**
     * Parses JSON string to map.
     *
     * @param json the JSON string
     * @return parsed map
     */
    private Map<String, Object> parseJson(String json) {
        try {
            return jsonMapper.readValue(json,
                new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            // Return empty map on parse error
            return Map.of();
        }
    }

    /**
     * Converts map to JSON string.
     *
     * @param state the state map
     * @return JSON string
     */
    private String toJson(Map<String, Object> state) {
        try {
            return jsonMapper.writeValueAsString(state);
        } catch (Exception e) {
            // Return empty object on error
            return "{}";
        }
    }
}
