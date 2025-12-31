package net.carcdr.ycrdt;

/**
 * Functional interface for handling exceptions thrown by observers.
 *
 * <p>Observer callbacks should not throw exceptions, but if they do, this handler
 * provides a way for applications to intercept and handle those exceptions
 * (e.g., for logging or monitoring).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * doc.setObserverErrorHandler((exception, source) -> {
 *     logger.error("Observer error in {}: {}", source, exception.getMessage(), exception);
 * });
 * }</pre>
 *
 * @see YDoc#setObserverErrorHandler(ObserverErrorHandler)
 */
@FunctionalInterface
public interface ObserverErrorHandler {

    /**
     * Handles an exception thrown by an observer callback.
     *
     * <p>This method is called when an observer throws an exception during
     * event dispatch. The handler should not re-throw the exception, as this
     * could disrupt other observers from receiving the event.</p>
     *
     * @param exception the exception that was thrown
     * @param source the object that dispatched the event (e.g., YText, YArray, YDoc)
     */
    void handleError(Exception exception, Object source);
}
