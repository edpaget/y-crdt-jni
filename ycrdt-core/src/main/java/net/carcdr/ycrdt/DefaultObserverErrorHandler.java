package net.carcdr.ycrdt;

/**
 * Default implementation of {@link ObserverErrorHandler} that prints errors to stderr.
 *
 * <p>This handler provides backwards-compatible behavior by printing exception
 * messages and stack traces to the standard error stream.</p>
 *
 * <p>For production applications, consider implementing a custom handler that
 * integrates with your logging framework:</p>
 * <pre>{@code
 * doc.setObserverErrorHandler((exception, source) -> {
 *     logger.error("Observer error in {}: {}", source, exception.getMessage(), exception);
 * });
 * }</pre>
 *
 * @see ObserverErrorHandler
 * @see YDoc#setObserverErrorHandler(ObserverErrorHandler)
 */
public final class DefaultObserverErrorHandler implements ObserverErrorHandler {

    /**
     * Singleton instance of the default handler.
     */
    public static final DefaultObserverErrorHandler INSTANCE = new DefaultObserverErrorHandler();

    /**
     * Creates a new DefaultObserverErrorHandler.
     */
    public DefaultObserverErrorHandler() {
    }

    /**
     * Handles an observer exception by printing to stderr.
     *
     * @param exception the exception that was thrown
     * @param source the object that dispatched the event
     */
    @Override
    public void handleError(Exception exception, Object source) {
        System.err.println("Observer threw exception: " + exception.getMessage());
        exception.printStackTrace();
    }
}
