package net.carcdr.yhocuspocus.core;

/**
 * Handler for errors that occur during server operation.
 *
 * <p>This interface allows applications to integrate error handling with
 * their logging frameworks, metrics systems, or alerting infrastructure.</p>
 *
 * <p>All methods have default no-op implementations, so implementations
 * can override only the error types they care about.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * YHocuspocus server = YHocuspocus.builder()
 *     .errorHandler(new ErrorHandler() {
 *         @Override
 *         public void onStorageError(String documentName, Exception e) {
 *             logger.error("Failed to store document: {}", documentName, e);
 *             metrics.increment("storage.errors");
 *         }
 *     })
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ErrorHandler {

    /**
     * Called when an error occurs during document storage.
     *
     * <p>This is called when an exception is thrown during the
     * onStoreDocument or afterStoreDocument hooks.</p>
     *
     * @param documentName the name of the document being stored
     * @param e the exception that occurred
     */
    default void onStorageError(String documentName, Exception e) {
        // Default: no-op
    }

    /**
     * Called when an error occurs during hook execution.
     *
     * <p>This is called when an extension's hook method throws an exception.</p>
     *
     * @param extensionName the class name of the extension that failed
     * @param hookName the name of the hook method that failed
     * @param e the exception that occurred
     */
    default void onHookError(String extensionName, String hookName, Exception e) {
        // Default: no-op
    }

    /**
     * Called when an error occurs during protocol handling.
     *
     * <p>This is called when an error occurs while processing
     * a client message or protocol operation.</p>
     *
     * @param connectionId the connection ID where the error occurred
     * @param e the exception that occurred
     */
    default void onProtocolError(String connectionId, Exception e) {
        // Default: no-op
    }
}
