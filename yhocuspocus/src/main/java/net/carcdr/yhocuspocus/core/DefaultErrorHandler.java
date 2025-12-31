package net.carcdr.yhocuspocus.core;

import java.io.PrintStream;

/**
 * Default error handler that logs to stderr.
 *
 * <p>This implementation provides backwards-compatible behavior,
 * logging all errors to {@code System.err}.</p>
 *
 * @since 1.0.0
 */
public class DefaultErrorHandler implements ErrorHandler {

    private final PrintStream output;

    /**
     * Creates a default error handler that logs to stderr.
     */
    public DefaultErrorHandler() {
        this(System.err);
    }

    /**
     * Creates a default error handler that logs to the specified stream.
     *
     * <p>This constructor is primarily for testing purposes.</p>
     *
     * @param output the stream to write error messages to
     */
    DefaultErrorHandler(PrintStream output) {
        this.output = output;
    }

    @Override
    public void onStorageError(String documentName, Exception e) {
        output.println("Error storing document " + documentName + ": " + e);
    }

    @Override
    public void onHookError(String extensionName, String hookName, Exception e) {
        output.println("Error in " + extensionName + "." + hookName + ": " + e);
    }

    @Override
    public void onProtocolError(String connectionId, Exception e) {
        output.println("Protocol error for connection " + connectionId + ": " + e);
    }
}
