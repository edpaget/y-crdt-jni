package net.carcdr.yhocuspocus.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock transport for testing.
 *
 * <p>Simulates a transport connection and records all sent messages
 * for verification in tests.</p>
 */
public class MockTransport implements Transport {

    private final String connectionId;
    private final List<byte[]> sentMessages;
    private boolean open;
    private int closeCode;
    private String closeReason;
    private ReceiveListener receiveListener;

    /**
     * Creates a new mock transport.
     */
    public MockTransport() {
        this.connectionId = UUID.randomUUID().toString();
        this.sentMessages = new CopyOnWriteArrayList<>();
        this.open = true;
    }

    @Override
    public CompletableFuture<Void> send(byte[] message) {
        if (!open) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Transport is closed")
            );
        }
        sentMessages.add(message);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public void close(int code, String reason) {
        this.open = false;
        this.closeCode = code;
        this.closeReason = reason;
    }

    @Override
    public void close() {
        close(1000, "Normal closure");
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getRemoteAddress() {
        return "mock://127.0.0.1";
    }

    @Override
    public void setReceiveListener(ReceiveListener listener) {
        this.receiveListener = listener;
    }

    /**
     * Simulates receiving a message (for testing).
     *
     * <p>This method is used by tests to simulate incoming messages
     * and trigger the receive listener.</p>
     *
     * @param data the message bytes to receive
     */
    public void receiveMessage(byte[] data) {
        if (receiveListener != null) {
            receiveListener.onMessage(data);
        }
    }

    /**
     * Gets all sent messages.
     *
     * @return list of sent message byte arrays
     */
    public List<byte[]> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }

    /**
     * Clears sent messages.
     */
    public void clearSentMessages() {
        sentMessages.clear();
    }

    /**
     * Gets the close code.
     *
     * @return close code
     */
    public int getCloseCode() {
        return closeCode;
    }

    /**
     * Gets the close reason.
     *
     * @return close reason
     */
    public String getCloseReason() {
        return closeReason;
    }
}
