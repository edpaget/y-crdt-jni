package net.carcdr.yhocuspocus.extension;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory database extension for testing and development.
 *
 * <p>This extension stores document state in memory using a ConcurrentHashMap.
 * It is useful for testing and development, but should not be used in production
 * as data is lost when the server restarts.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * YHocuspocus server = YHocuspocus.builder()
 *     .extension(new InMemoryDatabaseExtension())
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class InMemoryDatabaseExtension extends DatabaseExtension {

    private final ConcurrentHashMap<String, byte[]> storage;

    /**
     * Creates a new in-memory database extension.
     */
    public InMemoryDatabaseExtension() {
        this.storage = new ConcurrentHashMap<>();
    }

    /**
     * Loads document state from memory.
     *
     * @param documentName name of the document to load
     * @return Y-CRDT state bytes, or null if not found
     */
    @Override
    protected byte[] loadFromDatabase(String documentName) {
        return storage.get(documentName);
    }

    /**
     * Saves document state to memory.
     *
     * @param documentName name of the document to save
     * @param state Y-CRDT state bytes
     */
    @Override
    protected void saveToDatabase(String documentName, byte[] state) {
        if (state != null && state.length > 0) {
            storage.put(documentName, state);
        }
    }

    /**
     * Removes a document from storage.
     *
     * <p>This is a utility method not part of the standard DatabaseExtension API.</p>
     *
     * @param documentName name of the document to remove
     */
    public void removeDocument(String documentName) {
        storage.remove(documentName);
    }

    /**
     * Clears all stored documents.
     *
     * <p>This is a utility method useful for testing.</p>
     */
    public void clear() {
        storage.clear();
    }

    /**
     * Checks if a document exists in storage.
     *
     * @param documentName document name to check
     * @return true if document exists
     */
    public boolean hasDocument(String documentName) {
        return storage.containsKey(documentName);
    }

    /**
     * Gets the number of stored documents.
     *
     * @return document count
     */
    public int getDocumentCount() {
        return storage.size();
    }
}
