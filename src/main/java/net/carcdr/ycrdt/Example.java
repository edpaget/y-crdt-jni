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

        example1CreateDoc();
        example2CreateDocWithClientId();
        example3SyncDocs();
        example4WorkWithYText();
        example5SyncYText();
        example6WorkWithYArray();
        example7SyncYArray();
        example8WorkWithYMap();
        example9SyncYMap();
        example10ProperCleanup();

        System.out.println("=== All examples completed successfully! ===");
    }

    private static void example1CreateDoc() {
        System.out.println("Example 1: Creating a YDoc");
        try (YDoc doc = new YDoc()) {
            System.out.println("  Client ID: " + doc.getClientId());
            System.out.println("  GUID: " + doc.getGuid());
            System.out.println();
        }
    }

    private static void example2CreateDocWithClientId() {
        System.out.println("Example 2: Creating a YDoc with specific client ID");
        try (YDoc doc = new YDoc(12345)) {
            System.out.println("  Client ID: " + doc.getClientId());
            System.out.println();
        }
    }

    private static void example3SyncDocs() {
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
    }

    private static void example4WorkWithYText() {
        System.out.println("Example 4: Working with YText");
        try (YDoc doc = new YDoc();
             YText text = doc.getText("mytext")) {

            System.out.println("  Initial text: '" + text.toString() + "'");
            System.out.println("  Initial length: " + text.length());

            text.push("Hello");
            System.out.println("  After push('Hello'): '" + text.toString() + "'");

            text.push(" World");
            System.out.println("  After push(' World'): '" + text.toString() + "'");

            text.insert(5, " Beautiful");
            System.out.println("  After insert(5, ' Beautiful'): '" + text.toString() + "'");

            text.delete(5, 10);
            System.out.println("  After delete(5, 10): '" + text.toString() + "'");

            System.out.println("  Final length: " + text.length());
            System.out.println();
        }
    }

    private static void example5SyncYText() {
        System.out.println("Example 5: Synchronizing YText between documents");
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc();
             YText text1 = doc1.getText("shared");
             YText text2 = doc2.getText("shared")) {

            System.out.println("  Doc1 Client ID: " + doc1.getClientId());
            System.out.println("  Doc2 Client ID: " + doc2.getClientId());

            // Make changes in doc1
            text1.push("Hello from Doc1");
            System.out.println("  Doc1 text: '" + text1.toString() + "'");

            // Make changes in doc2
            text2.push("Hello from Doc2");
            System.out.println("  Doc2 text: '" + text2.toString() + "'");

            // Sync doc1 to doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);
            System.out.println("  After syncing doc1 to doc2: '" + text2.toString() + "'");

            // Sync doc2 to doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);
            System.out.println("  After syncing doc2 to doc1: '" + text1.toString() + "'");

            System.out.println("  Documents are now synchronized!");
            System.out.println();
        }
    }

    private static void example6WorkWithYArray() {
        System.out.println("Example 6: Working with YArray");
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("myarray")) {

            System.out.println("  Initial array: " + array.toJson());
            System.out.println("  Initial length: " + array.length());

            array.pushString("Hello");
            System.out.println("  After pushString('Hello'): " + array.toJson());

            array.pushDouble(42.0);
            System.out.println("  After pushDouble(42.0): " + array.toJson());

            array.insertString(1, "World");
            System.out.println("  After insertString(1, 'World'): " + array.toJson());

            array.remove(0, 1);
            System.out.println("  After remove(0, 1): " + array.toJson());

            System.out.println("  Final length: " + array.length());
            System.out.println("  Element 0 (string): " + array.getString(0));
            System.out.println("  Element 1 (double): " + array.getDouble(1));
            System.out.println();
        }
    }

    private static void example7SyncYArray() {
        System.out.println("Example 7: Synchronizing YArray between documents");
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Build array in doc1
            try (YArray array1 = doc1.getArray("shared")) {
                array1.pushString("Item 1");
                array1.pushDouble(100.0);
            }

            System.out.println("  Doc1 array: " + doc1.getArray("shared").toJson());

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            try (YArray array2 = doc2.getArray("shared")) {
                System.out.println("  Doc2 array after sync: " + array2.toJson());
                System.out.println("  Arrays are synchronized!");
            }
            System.out.println();
        }
    }

    private static void example8WorkWithYMap() {
        System.out.println("Example 8: Working with YMap");
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("mymap")) {

            System.out.println("  Initial map: " + map.toJson());
            System.out.println("  Initial size: " + map.size());

            map.setString("name", "Alice");
            System.out.println("  After setString('name', 'Alice'): " + map.toJson());

            map.setDouble("age", 30.0);
            System.out.println("  After setDouble('age', 30.0): " + map.toJson());

            map.setString("city", "NYC");
            System.out.println("  After setString('city', 'NYC'): " + map.toJson());

            map.remove("city");
            System.out.println("  After remove('city'): " + map.toJson());

            System.out.println("  Final size: " + map.size());
            System.out.println("  Name: " + map.getString("name"));
            System.out.println("  Age: " + map.getDouble("age"));
            System.out.println("  Contains 'name': " + map.containsKey("name"));
            System.out.println("  Contains 'city': " + map.containsKey("city"));
            System.out.println();
        }
    }

    private static void example9SyncYMap() {
        System.out.println("Example 9: Synchronizing YMap between documents");
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Build map in doc1
            try (YMap map1 = doc1.getMap("shared")) {
                map1.setString("user", "Alice");
                map1.setDouble("score", 95.5);
            }

            System.out.println("  Doc1 map: " + doc1.getMap("shared").toJson());

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            try (YMap map2 = doc2.getMap("shared")) {
                System.out.println("  Doc2 map after sync: " + map2.toJson());
                System.out.println("  Maps are synchronized!");
            }
            System.out.println();
        }
    }

    private static void example10ProperCleanup() {
        System.out.println("Example 10: Demonstrating proper cleanup");
        YDoc doc = new YDoc();
        System.out.println("  Created doc with client ID: " + doc.getClientId());
        System.out.println("  Is closed? " + doc.isClosed());

        doc.close();
        System.out.println("  Closed doc");
        System.out.println("  Is closed? " + doc.isClosed());
        System.out.println();
    }
}
