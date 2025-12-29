package net.carcdr.yhocuspocus.redis;

import net.carcdr.yhocuspocus.extension.AfterLoadDocumentPayload;
import net.carcdr.yhocuspocus.extension.BeforeUnloadDocumentPayload;
import net.carcdr.yhocuspocus.extension.Extension;
import net.carcdr.yhocuspocus.extension.OnChangePayload;
import net.carcdr.yhocuspocus.extension.OnStoreDocumentPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock Redis extension for testing Redis integration behavior.
 *
 * <p>Records all operations for verification without requiring actual Redis.</p>
 */
public class MockRedisExtension implements Extension {

    private final List<PublishedUpdate> publishedUpdates = new CopyOnWriteArrayList<>();
    private final Set<String> subscribedChannels = ConcurrentHashMap.newKeySet();
    private final Map<String, byte[]> storedStates = new ConcurrentHashMap<>();
    private boolean connected = true;

    @Override
    public int priority() {
        return 50; // Run before DatabaseExtension
    }

    @Override
    public CompletableFuture<Void> afterLoadDocument(AfterLoadDocumentPayload payload) {
        if (!connected) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Redis connection failed"));
        }
        String docName = payload.getDocument().getName();
        subscribedChannels.add("doc:" + docName);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onChange(OnChangePayload payload) {
        if (!connected) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Redis connection failed"));
        }
        String docName = payload.getDocument().getName();
        publishedUpdates.add(new PublishedUpdate(docName, payload.getUpdate()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onStoreDocument(OnStoreDocumentPayload payload) {
        if (!connected) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Redis connection failed"));
        }
        storedStates.put(payload.getDocumentName(), payload.getState());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> beforeUnloadDocument(BeforeUnloadDocumentPayload payload) {
        String docName = payload.getDocument().getName();
        subscribedChannels.remove("doc:" + docName);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Gets all published updates.
     *
     * @return list of published updates
     */
    public List<PublishedUpdate> getPublishedUpdates() {
        return new ArrayList<>(publishedUpdates);
    }

    /**
     * Gets published updates for a specific document.
     *
     * @param documentName document name
     * @return list of updates for the document
     */
    public List<PublishedUpdate> getUpdatesForDocument(String documentName) {
        return publishedUpdates.stream()
            .filter(u -> u.documentName().equals(documentName))
            .toList();
    }

    /**
     * Gets currently subscribed channels.
     *
     * @return set of channel names
     */
    public Set<String> getSubscribedChannels() {
        return Set.copyOf(subscribedChannels);
    }

    /**
     * Gets stored state for a document.
     *
     * @param documentName document name
     * @return stored state bytes, or null if not stored
     */
    public byte[] getStoredState(String documentName) {
        return storedStates.get(documentName);
    }

    /**
     * Clears all recorded data.
     */
    public void clear() {
        publishedUpdates.clear();
        subscribedChannels.clear();
        storedStates.clear();
    }

    /**
     * Simulates Redis connection failure.
     */
    public void disconnect() {
        connected = false;
    }

    /**
     * Simulates Redis reconnection.
     */
    public void reconnect() {
        connected = true;
    }

    /**
     * Checks if connected to Redis.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Record of a published update.
     *
     * @param documentName the document name
     * @param update the update bytes
     */
    public record PublishedUpdate(String documentName, byte[] update) { }
}
