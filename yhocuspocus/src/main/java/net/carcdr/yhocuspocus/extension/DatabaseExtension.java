package net.carcdr.yhocuspocus.extension;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for database persistence extensions.
 *
 * <p>This class provides a convenient pattern for implementing document persistence.
 * Subclasses only need to implement {@link #loadFromDatabase(String)} and
 * {@link #saveToDatabase(String, byte[])}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class PostgresDatabaseExtension extends DatabaseExtension {
 *     private final DataSource dataSource;
 *
 *     {@literal @}Override
 *     protected byte[] loadFromDatabase(String documentName) {
 *         // Load from PostgreSQL
 *         try (Connection conn = dataSource.getConnection()) {
 *             // ... query logic
 *         }
 *     }
 *
 *     {@literal @}Override
 *     protected void saveToDatabase(String documentName, byte[] state) {
 *         // Save to PostgreSQL
 *         try (Connection conn = dataSource.getConnection()) {
 *             // ... insert/update logic
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public abstract class DatabaseExtension implements Extension {

    /**
     * Extension priority for database operations.
     * Higher priority ensures database extensions run before other extensions.
     *
     * @return priority value (default 500)
     */
    @Override
    public int priority() {
        return 500;
    }

    /**
     * Loads document state from the database.
     *
     * <p>This hook runs when a document is first accessed. If the document
     * exists in the database, this method should return its state. If not,
     * return null.</p>
     *
     * @param payload document load information
     * @return future that completes when load is done
     */
    @Override
    public CompletableFuture<Void> onLoadDocument(OnLoadDocumentPayload payload) {
        return CompletableFuture.runAsync(() -> {
            String documentName = payload.getDocument().getName();

            try {
                byte[] state = loadFromDatabase(documentName);

                if (state != null && state.length > 0) {
                    payload.setState(state);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load document: " + documentName, e);
            }
        });
    }

    /**
     * Persists document state to the database.
     *
     * <p>This hook runs after a quiet period (debounced) when the document
     * has been modified. Extensions should persist the provided state.</p>
     *
     * @param payload document and state information
     * @return future that completes when save is done
     */
    @Override
    public CompletableFuture<Void> onStoreDocument(OnStoreDocumentPayload payload) {
        return CompletableFuture.runAsync(() -> {
            String documentName = payload.getDocumentName();
            byte[] state = payload.getState();

            try {
                saveToDatabase(documentName, state);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save document: " + documentName, e);
            }
        });
    }

    /**
     * Loads document state from the persistence layer.
     *
     * <p>Implementations should query their storage backend and return
     * the Y-CRDT state bytes, or null if the document doesn't exist.</p>
     *
     * @param documentName name of the document to load
     * @return Y-CRDT state bytes, or null if not found
     * @throws Exception if an error occurs during loading
     */
    protected abstract byte[] loadFromDatabase(String documentName) throws Exception;

    /**
     * Saves document state to the persistence layer.
     *
     * <p>Implementations should store the Y-CRDT state bytes in their
     * storage backend, creating a new entry or updating an existing one.</p>
     *
     * @param documentName name of the document to save
     * @param state Y-CRDT state bytes
     * @throws Exception if an error occurs during saving
     */
    protected abstract void saveToDatabase(String documentName, byte[] state) throws Exception;
}
