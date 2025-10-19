package net.carcdr.yhocuspocus.websocket;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Test WebSocket listener for integration tests.
 */
public class TestWebSocketListener implements Session.Listener {

    private final CompletableFuture<Session> connectFuture = new CompletableFuture<>();
    private final ConcurrentLinkedQueue<ByteBuffer> receivedMessages = new ConcurrentLinkedQueue<>();

    @Override
    public void onWebSocketOpen(Session session) {
        connectFuture.complete(session);
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        // Store a copy of the payload
        ByteBuffer copy = ByteBuffer.allocate(payload.remaining());
        copy.put(payload.duplicate());
        copy.flip();
        receivedMessages.add(copy);
        callback.succeed();
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        // Connection closed
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        connectFuture.completeExceptionally(cause);
    }

    /**
     * Gets the future that completes when the connection is established.
     */
    public CompletableFuture<Session> getConnectFuture() {
        return connectFuture;
    }

    /**
     * Gets the received messages.
     */
    public ConcurrentLinkedQueue<ByteBuffer> getReceivedMessages() {
        return receivedMessages;
    }

    /**
     * Waits for a message to be received.
     */
    public ByteBuffer waitForMessage(long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (receivedMessages.isEmpty()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                return null;
            }
            Thread.sleep(10);
        }
        return receivedMessages.poll();
    }
}
