package net.carcdr.yhocuspocus.websocket.examples;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.extension.InMemoryDatabaseExtension;
import net.carcdr.yhocuspocus.websocket.WebSocketServer;

import java.time.Duration;

/**
 * Simple example of running a YHocuspocus WebSocket server.
 *
 * <p>This example demonstrates:</p>
 * <ul>
 *   <li>Creating a YHocuspocus instance with an in-memory database</li>
 *   <li>Configuring persistence settings (debounce, maxDebounce)</li>
 *   <li>Starting a WebSocket server on port 1234</li>
 *   <li>Graceful shutdown with try-with-resources</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * java net.carcdr.yhocuspocus.websocket.examples.SimpleWebSocketServerExample
 * }</pre>
 *
 * <p>Then connect from a browser using Yjs:</p>
 * <pre>{@code
 * import * as Y from 'yjs'
 * import { WebsocketProvider } from 'y-websocket'
 *
 * const ydoc = new Y.Doc()
 * const provider = new WebsocketProvider('ws://localhost:1234', 'my-document', ydoc)
 *
 * const ytext = ydoc.getText('content')
 * ytext.insert(0, 'Hello from Yjs!')
 * }</pre>
 *
 * @since 1.0.0
 */
public final class SimpleWebSocketServerExample {

    private SimpleWebSocketServerExample() {
        // Utility class
    }

    /**
     * Main entry point.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if server fails to start or stop
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Starting YHocuspocus WebSocket Server...");

        // Create YHocuspocus instance with in-memory database
        try (YHocuspocus hocuspocus = YHocuspocus.builder()
            .extension(new InMemoryDatabaseExtension())
            .debounce(Duration.ofSeconds(2))      // Wait 2s of inactivity before saving
            .maxDebounce(Duration.ofSeconds(10))  // Force save after 10s even if active
            .build()) {

            // Create WebSocket server
            try (WebSocketServer server = WebSocketServer.builder()
                .server(hocuspocus)
                .port(1234)
                .path("/")
                .build()) {

                // Start server
                server.start();

                System.out.println("WebSocket server started on ws://localhost:1234/");
                System.out.println("Connect with Yjs:");
                System.out.println("  const provider = new WebsocketProvider("
                    + "'ws://localhost:1234', 'my-document', ydoc)");
                System.out.println();
                System.out.println("Press Ctrl+C to stop...");

                // Keep running until interrupted
                Thread.currentThread().join();
            }
        }
    }
}
