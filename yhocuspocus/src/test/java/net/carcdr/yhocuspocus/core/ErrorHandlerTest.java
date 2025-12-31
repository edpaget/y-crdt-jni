package net.carcdr.yhocuspocus.core;

import net.carcdr.yhocuspocus.extension.Extension;
import net.carcdr.yhocuspocus.extension.OnConnectPayload;
import net.carcdr.yhocuspocus.extension.OnStoreDocumentPayload;
import net.carcdr.yhocuspocus.extension.TestWaiter;
import net.carcdr.yhocuspocus.protocol.OutgoingMessage;
import net.carcdr.yhocuspocus.protocol.SyncProtocol;
import net.carcdr.yhocuspocus.transport.MockTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the ErrorHandler system.
 */
public class ErrorHandlerTest {

    private YHocuspocus server;
    private TestWaiter waiter;
    private List<String> capturedErrors;

    @Before
    public void setUp() {
        capturedErrors = new ArrayList<>();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    public void testDefaultErrorHandlerUsed() {
        server = YHocuspocus.builder().build();

        assertNotNull("Error handler should be set", server.getErrorHandler());
        assertTrue("Default handler should be DefaultErrorHandler",
                server.getErrorHandler() instanceof DefaultErrorHandler);
    }

    @Test
    public void testCustomErrorHandlerSet() {
        ErrorHandler custom = new ErrorHandler() { };
        server = YHocuspocus.builder()
                .errorHandler(custom)
                .build();

        assertEquals("Custom handler should be used", custom, server.getErrorHandler());
    }

    @Test
    public void testStorageErrorReported() throws Exception {
        waiter = new TestWaiter();

        // Extension that throws during store
        Extension failingStore = new Extension() {
            @Override
            public CompletableFuture<Void> onStoreDocument(OnStoreDocumentPayload payload) {
                throw new RuntimeException("Storage failed!");
            }
        };

        ErrorHandler capturingHandler = new ErrorHandler() {
            @Override
            public void onStorageError(String documentName, Exception e) {
                capturedErrors.add("storage:" + documentName);
            }

            @Override
            public void onHookError(String extensionName, String hookName, Exception e) {
                // Ignore hook errors for this test - we only care about storage errors
            }
        };

        server = YHocuspocus.builder()
                .extension(waiter)
                .extension(failingStore)
                .errorHandler(capturingHandler)
                .debounce(java.time.Duration.ofMillis(10))
                .maxDebounce(java.time.Duration.ofMillis(50))
                .build();

        // Create connection and document
        MockTransport transport = new MockTransport();
        server.handleConnection(transport, Map.of());

        // Use SyncStep1 to initiate sync and trigger document creation
        byte[] stateVector = new byte[0];
        byte[] syncStep1 = SyncProtocol.encodeSyncStep1(stateVector);
        transport.receiveMessage(OutgoingMessage.sync("test-doc", syncStep1).encode());

        // Wait for document to load
        assertTrue("Document should load",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should exist", doc);

        // Trigger a document change to schedule a save
        doc.getDoc().getText("test").insert(0, "hello");

        // Wait for debounced save to trigger and fail
        Thread.sleep(200);

        // Verify error was captured
        assertTrue("Storage error should be captured, got: " + capturedErrors,
                capturedErrors.stream().anyMatch(e -> e.contains("storage:test-doc")));
    }

    @Test
    public void testHookErrorReported() throws Exception {
        // Extension that throws during onConnect
        Extension failingExtension = new Extension() {
            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                throw new RuntimeException("Hook failed!");
            }
        };

        ErrorHandler capturingHandler = new ErrorHandler() {
            @Override
            public void onHookError(String extensionName, String hookName, Exception e) {
                capturedErrors.add("hook:" + extensionName + ":" + hookName);
            }
        };

        server = YHocuspocus.builder()
                .extension(failingExtension)
                .errorHandler(capturingHandler)
                .build();

        // Create connection - this triggers onConnect which will fail
        MockTransport transport = new MockTransport();
        server.handleConnection(transport, Map.of());

        // Wait a bit for async hook to execute
        Thread.sleep(100);

        // Verify hook error was captured (extension is anonymous class)
        assertTrue("Hook error should be captured",
                capturedErrors.stream().anyMatch(e -> e.startsWith("hook:")));
    }

    @Test
    public void testDefaultErrorHandlerOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream testStream = new PrintStream(baos);

        DefaultErrorHandler handler = new DefaultErrorHandler(testStream);

        handler.onStorageError("my-doc", new RuntimeException("Save failed"));
        handler.onHookError("MyExtension", "onConnect", new RuntimeException("Hook error"));
        handler.onProtocolError("conn-123", new RuntimeException("Protocol issue"));

        String output = baos.toString();
        assertTrue("Should contain storage error",
                output.contains("Error storing document my-doc"));
        assertTrue("Should contain hook error",
                output.contains("Error in MyExtension.onConnect"));
        assertTrue("Should contain protocol error",
                output.contains("Protocol error for connection conn-123"));
    }

    @Test
    public void testHookErrorIncludesExtensionName() throws Exception {
        final String[] capturedExtName = new String[1];
        final String[] capturedHookName = new String[1];

        // Named extension class for predictable name
        class NamedTestExtension implements Extension {
            @Override
            public CompletableFuture<Void> onConnect(OnConnectPayload payload) {
                throw new RuntimeException("Test failure");
            }
        }

        ErrorHandler capturingHandler = new ErrorHandler() {
            @Override
            public void onHookError(String extensionName, String hookName, Exception e) {
                capturedExtName[0] = extensionName;
                capturedHookName[0] = hookName;
            }
        };

        server = YHocuspocus.builder()
                .extension(new NamedTestExtension())
                .errorHandler(capturingHandler)
                .build();

        MockTransport transport = new MockTransport();
        server.handleConnection(transport, Map.of());

        // Wait for async hook
        Thread.sleep(100);

        assertEquals("Extension name should be class name",
                "NamedTestExtension", capturedExtName[0]);
    }
}
