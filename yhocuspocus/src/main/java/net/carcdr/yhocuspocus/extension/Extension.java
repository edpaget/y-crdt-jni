package net.carcdr.yhocuspocus.extension;

import java.util.concurrent.CompletableFuture;

/**
 * Extension interface for customizing server behavior.
 *
 * <p>Extensions can hook into various lifecycle events to add
 * functionality like persistence, authentication, logging, etc.</p>
 *
 * <p>All hook methods are optional (default implementations do nothing).
 * Extensions are executed in priority order, with higher priority values
 * running first.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class LoggingExtension implements Extension {
 *     {@literal @}Override
 *     public int priority() {
 *         return 100;
 *     }
 *
 *     {@literal @}Override
 *     public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
 *         System.out.println("Client connected: " + payload.getConnectionId());
 *         return CompletableFuture.completedFuture(null);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface Extension {

    /**
     * Extension priority (higher = runs earlier).
     * Default: 100
     *
     * @return the priority value
     */
    default int priority() {
        return 100;
    }

    /**
     * Called when a client connection is established.
     *
     * <p>This hook runs before authentication. Extensions can inspect
     * or modify the connection context.</p>
     *
     * @param payload connection information
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> onConnect(OnConnectPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when a client attempts to authenticate for a document.
     *
     * <p>Extensions can reject authentication by throwing an exception
     * or enriching the context with user information.</p>
     *
     * @param payload authentication information
     * @return future that completes when authentication succeeds
     */
    default CompletableFuture<Void> onAuthenticate(OnAuthenticatePayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when a document is first created (before loading).
     *
     * <p>This hook runs once per document lifecycle, before any
     * persistence layer is queried.</p>
     *
     * @param payload document creation information
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> onCreateDocument(OnCreateDocumentPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when a document needs to be loaded from persistence.
     *
     * <p>Extensions should set the document state via
     * {@link OnLoadDocumentPayload#setState(byte[])} if they have
     * persisted data. The first extension to provide state wins.</p>
     *
     * @param payload document load information
     * @return future that completes when load is done
     */
    default CompletableFuture<Void> onLoadDocument(OnLoadDocumentPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called after a document has been loaded.
     *
     * <p>This hook runs after {@link #onLoadDocument} and after
     * the state has been applied to the document.</p>
     *
     * @param payload document information
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> afterLoadDocument(AfterLoadDocumentPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when a document is modified.
     *
     * <p>This hook is triggered for every change to the document.
     * Use for real-time notifications, logging, or triggering side effects.</p>
     *
     * @param payload change information including update bytes
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> onChange(OnChangePayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when a document should be persisted.
     *
     * <p>This hook is debounced - it won't be called immediately after
     * every change, but after a quiet period or maximum delay.</p>
     *
     * <p>Extensions should persist the state provided in the payload.</p>
     *
     * @param payload document and state information
     * @return future that completes when save is done
     */
    default CompletableFuture<Void> onStoreDocument(OnStoreDocumentPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called after a document has been stored.
     *
     * <p>This hook runs after {@link #onStoreDocument} completes successfully.</p>
     *
     * @param payload document information
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> afterStoreDocument(AfterStoreDocumentPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called before a document is unloaded from memory.
     *
     * <p>This happens when all connections to a document are closed.
     * Extensions can perform cleanup or final persistence.</p>
     *
     * @param payload document information
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> beforeUnloadDocument(BeforeUnloadDocumentPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called after a document has been unloaded.
     *
     * <p>The document is no longer accessible at this point.</p>
     *
     * @param payload document information
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> afterUnloadDocument(AfterUnloadDocumentPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when a client connection is closed.
     *
     * <p>This hook runs after all document connections have been cleaned up.</p>
     *
     * @param payload disconnection information
     * @return future that completes when hook is done
     */
    default CompletableFuture<Void> onDisconnect(OnDisconnectPayload payload) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when the server is shutting down.
     *
     * <p>Extensions should perform cleanup and release resources.</p>
     *
     * @param payload shutdown information
     * @return future that completes when cleanup is done
     */
    default CompletableFuture<Void> onDestroy(OnDestroyPayload payload) {
        return CompletableFuture.completedFuture(null);
    }
}
