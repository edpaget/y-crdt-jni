package net.carcdr.ycrdt;

/**
 * Simple example demonstrating the usage of Y-CRDT JNI bindings.
 */
public final class Example {

    private Example() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void main(String[] args) {
        System.out.println("=== Y-CRDT JNI Example ===\n");

        // Example 1: Creating a document
        System.out.println("Example 1: Creating a YDoc");
        try (YDoc doc = new YDoc()) {
            System.out.println("  Client ID: " + doc.getClientId());
            System.out.println("  GUID: " + doc.getGuid());
            System.out.println();
        }

        // Example 2: Creating a document with a specific client ID
        System.out.println("Example 2: Creating a YDoc with specific client ID");
        try (YDoc doc = new YDoc(12345)) {
            System.out.println("  Client ID: " + doc.getClientId());
            System.out.println();
        }

        // Example 3: Synchronizing two documents
        System.out.println("Example 3: Synchronizing two documents");
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            System.out.println("  Doc1 Client ID: " + doc1.getClientId());
            System.out.println("  Doc2 Client ID: " + doc2.getClientId());

            // Get the state of doc1
            byte[] state1 = doc1.encodeStateAsUpdate();
            System.out.println("  Doc1 state size: " + state1.length + " bytes");

            // Apply doc1's state to doc2
            doc2.applyUpdate(state1);
            System.out.println("  Applied doc1's state to doc2");

            // Get the state of doc2
            byte[] state2 = doc2.encodeStateAsUpdate();
            System.out.println("  Doc2 state size: " + state2.length + " bytes");

            // Apply doc2's state to doc1
            doc1.applyUpdate(state2);
            System.out.println("  Applied doc2's state to doc1");

            System.out.println("  Documents are now synchronized!");
            System.out.println();
        }

        // Example 4: Demonstrating proper cleanup
        System.out.println("Example 4: Demonstrating proper cleanup");
        YDoc doc = new YDoc();
        System.out.println("  Created doc with client ID: " + doc.getClientId());
        System.out.println("  Is closed? " + doc.isClosed());

        doc.close();
        System.out.println("  Closed doc");
        System.out.println("  Is closed? " + doc.isClosed());
        System.out.println();

        System.out.println("=== All examples completed successfully! ===");
    }
}
