package net.carcdr.example;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.extension.InMemoryDatabaseExtension;
import net.carcdr.yhocuspocus.websocket.WebSocketServer;

import java.time.Duration;

/**
 * Example collaborative editing server using YHocuspocus with WebSocket transport.
 *
 * <p>This server demonstrates a complete setup with:
 * <ul>
 *   <li>In-memory document storage</li>
 *   <li>WebSocket transport on port 1234</li>
 *   <li>Debounced persistence (2s idle, max 10s)</li>
 *   <li>Compatible with Yjs JavaScript clients</li>
 * </ul>
 *
 * <p>To connect from a browser:</p>
 * <pre>{@code
 * import { HocuspocusProvider } from '@hocuspocus/provider'
 *
 * const provider = new HocuspocusProvider({
 *   url: 'ws://localhost:1234',
 *   name: 'my-document',
 * })
 * }</pre>
 *
 * @since 1.0.0
 */
public final class CollaborativeServerMain {

    private CollaborativeServerMain() {
        // Utility class
    }

    /**
     * Main entry point for the collaborative editing server.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if server fails to start
     */
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  YHocuspocus Collaborative Editing Server");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();

        // Create YHocuspocus instance with in-memory storage
        try (YHocuspocus hocuspocus = YHocuspocus.builder()
            .extension(new InMemoryDatabaseExtension())
            .debounce(Duration.ofSeconds(2))      // Save after 2s of inactivity
            .maxDebounce(Duration.ofSeconds(10))  // Force save after 10s max
            .build()) {

            System.out.println("✓ YHocuspocus server initialized");
            System.out.println("  - Storage: In-Memory");
            System.out.println("  - Debounce: 2s / Max: 10s");
            System.out.println();

            // Create WebSocket server
            try (WebSocketServer server = WebSocketServer.builder()
                .server(hocuspocus)
                .port(1234)
                .path("/")
                .pingInterval(Duration.ofSeconds(30))  // Keepalive ping every 30s
                .build()) {

                // Start the server
                server.start();

                System.out.println("✓ WebSocket server started");
                System.out.println("  - Address: ws://localhost:1234/");
                System.out.println("  - Max connections: Unlimited");
                System.out.println("  - Max message size: 10MB");
                System.out.println();
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println("Server is ready! Open the frontend to start editing.");
                System.out.println();
                System.out.println("Frontend: http://localhost:3000");
                System.out.println("Press Ctrl+C to stop the server...");
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println();

                // Keep the server running until interrupted
                Thread.currentThread().join();
            }
        } catch (InterruptedException e) {
            System.out.println();
            System.out.println("Server shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}
