# ycrdt - Y-CRDT JNI Bindings

Java bindings for the [y-crdt](https://github.com/y-crdt/y-crdt) Rust library, providing Conflict-free Replicated Data Types (CRDTs) for the JVM.

## Features

- **YDoc**: Core document type with synchronization support
- **YText**: Collaborative text editing with Unicode support
- **YArray**: Collaborative arrays with mixed types (strings, doubles, subdocuments)
- **YMap**: Collaborative maps with mixed types (strings, doubles, subdocuments)
- **YXmlText**: Collaborative XML text nodes with rich formatting
- **YXmlElement**: Collaborative XML elements with attributes and nesting
- **YXmlFragment**: Hierarchical XML tree support
- **Subdocuments**: Embed YDocs within YMap and YArray
- **Observer API**: Real-time change notifications for all types
- **Transaction API**: Batch multiple operations for better performance
- **Advanced Sync**: State vectors, differential updates, update merging
- **Memory Safe**: Proper resource management with AutoCloseable
- **Multi-platform**: Linux, macOS, Windows support

## Requirements

- Java 21 or higher
- Rust 1.70+ (for building from source)

## Installation

Currently not published to Maven Central. Build from source:

```bash
./gradlew :ycrdt:build
```

The JAR will be in `ycrdt/build/libs/ycrdt.jar`.

## Quick Start

### Using YBindingFactory (Recommended)

```java
import net.carcdr.ycrdt.*;

public class Example {
    public static void main(String[] args) {
        // Get the JNI binding
        YBinding binding = YBindingFactory.jni();

        // Create a document
        try (YDoc doc = binding.createDoc()) {
            // Collaborative text editing
            try (YText text = doc.getText("myText")) {
                text.push("Hello, ");
                text.push("World!");
                System.out.println(text.toString()); // "Hello, World!"
            }

            // Collaborative map
            try (YMap map = doc.getMap("myMap")) {
                map.setString("name", "Alice");
                map.setDouble("age", 30.0);
                System.out.println(map.toJson()); // {"name":"Alice","age":30.0}
            }

            // Synchronize with another document
            byte[] update = doc.encodeStateAsUpdate();
            try (YDoc doc2 = binding.createDoc()) {
                doc2.applyUpdate(update);
                // doc2 now has the same state as doc
            }
        }
    }
}
```

### Direct Instantiation

You can also use the JNI implementation directly:

```java
import net.carcdr.ycrdt.jni.JniYDoc;

try (JniYDoc doc = new JniYDoc()) {
    // ... use doc directly
}
```

## Usage Examples

### Text Editing

```java
try (YDoc doc = new YDoc()) {
    try (YText text = doc.getText("article")) {
        text.push("The quick brown fox");
        text.insert(4, "very ");  // "The very quick brown fox"
        text.delete(14, 6);       // "The very quick fox"
    }
}
```

### Arrays

```java
try (YDoc doc = new YDoc()) {
    try (YArray array = doc.getArray("list")) {
        array.pushString("apple");
        array.pushDouble(42.0);
        array.insertString(1, "banana");
        System.out.println(array.toJson()); // ["apple","banana",42.0]
    }
}
```

### Maps

```java
try (YDoc doc = new YDoc()) {
    try (YMap map = doc.getMap("user")) {
        map.setString("name", "Alice");
        map.setDouble("score", 95.5);
        System.out.println(map.getString("name")); // "Alice"
        System.out.println(map.containsKey("name")); // true
    }
}
```

### XML with Formatting

```java
try (YDoc doc = new YDoc()) {
    try (YXmlElement div = doc.getXmlElement("document")) {
        div.setAttribute("class", "container");

        try (YXmlText text = div.insertText(0)) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("bold", true);
            text.insertWithAttributes(0, "Important", attrs);
        }

        System.out.println(div.toString());
        // <div class="container"><text><bold>Important</bold></text></div>
    }
}
```

### Subdocuments

```java
try (YDoc doc = new YDoc()) {
    try (YMap map = doc.getMap("pages")) {
        // Create a subdocument
        try (YDoc pageDoc = new YDoc()) {
            try (YText content = pageDoc.getText("content")) {
                content.push("Page content");
            }
            map.setDoc("page1", pageDoc);
        }

        // Retrieve subdocument
        try (YDoc retrieved = map.getDoc("page1")) {
            try (YText content = retrieved.getText("content")) {
                System.out.println(content.toString()); // "Page content"
            }
        }
    }
}
```

### Observers

```java
try (YDoc doc = new YDoc()) {
    try (YText text = doc.getText("myText")) {
        // Subscribe to changes
        try (YSubscription sub = text.observe(event -> {
            System.out.println("Text changed!");
            for (Object change : event.getChanges()) {
                YTextChange tc = (YTextChange) change;
                System.out.println("Type: " + tc.getType());
                System.out.println("Content: " + tc.getInsert());
            }
        })) {
            // Make changes
            text.push("Hello");
            text.push(", World!");

            // Subscription automatically unsubscribed when closed
        }
    }
}
```

### Transactions

Batch multiple operations for better performance and single observer notifications:

```java
try (YDoc doc = new YDoc()) {
    try (YText text = doc.getText("article")) {
        // Batch multiple operations in a single transaction
        try (YTransaction txn = doc.beginTransaction()) {
            text.insert(txn, 0, "Title\n\n");
            text.insert(txn, 7, "This is the introduction. ");
            text.insert(txn, 33, "This is the conclusion.");
            // All changes committed together when transaction closes
            // Observers receive a single notification with combined changes
        }
    }

    // Alternative: callback-based transaction
    doc.transaction(txn -> {
        try (YMap map = doc.getMap("metadata")) {
            map.setString(txn, "author", "Alice");
            map.setString(txn, "title", "My Article");
            map.setDouble(txn, "timestamp", System.currentTimeMillis());
        }
    });
}
```

**Benefits:**
- Better performance (fewer JNI calls)
- Single observer notification per transaction
- More efficient update encoding for synchronization
- Clearer semantic grouping of related changes

**Note:** Nested transactions are not supported. Only one transaction can be active per document at a time.

### Advanced Synchronization

```java
try (YDoc doc1 = new YDoc(); YDoc doc2 = new YDoc()) {
    try (YText text1 = doc1.getText("shared")) {
        text1.push("Hello");
    }

    // Get state vector from doc2
    byte[] stateVector = doc2.encodeStateVector();

    // Generate differential update from doc1
    byte[] diff = doc1.encodeDiff(stateVector);

    // Apply only the missing changes to doc2
    doc2.applyUpdate(diff);

    try (YText text2 = doc2.getText("shared")) {
        System.out.println(text2.toString()); // "Hello"
    }
}
```

## API Documentation

See [IMPLEMENTATION.md](IMPLEMENTATION.md) for technical details and architecture.

Full Javadoc: https://carcdr.net/y-crdt-jni/

## Testing

```bash
# Run all tests (Rust + Java)
./gradlew :ycrdt:test

# Run only Rust tests
cd ycrdt && cargo test

# Run with checkstyle
./gradlew :ycrdt:check
```

## Test Coverage

- 36 Rust unit tests (100% passing)
- 323 Java integration tests (100% passing)
  - 214 functional tests
  - 25 memory stress tests
  - 16 subdocument tests
  - 51 observer tests
  - 17 transaction tests
  - 16 advanced sync tests

## Development

See [.claude/CLAUDE.md](../.claude/CLAUDE.md) for development guidelines.

```bash
# Format Rust code
cd ycrdt && cargo fmt

# Run Rust linter
cd ycrdt && cargo clippy

# Run checkstyle
./gradlew :ycrdt:checkstyleMain :ycrdt:checkstyleTest
```

## License

Apache License 2.0
