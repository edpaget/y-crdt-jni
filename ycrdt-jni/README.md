# ycrdt-jni - Y-CRDT JNI Bindings

Java bindings for [y-crdt](https://github.com/y-crdt/y-crdt) via JNI. Provides YDoc, YText, YArray, YMap, YXmlText, YXmlElement, YXmlFragment, subdocuments, observers, and transactions.

## Requirements

- Java 21+
- Rust 1.70+ (building from source)

## Installation

```groovy
implementation 'net.carcdr:ycrdt-core:0.1.0-SNAPSHOT'
implementation 'net.carcdr:ycrdt-jni:0.1.0-SNAPSHOT'
```

Build from source: `./gradlew :ycrdt-jni:build`

## Usage

### Basic Types

```java
import net.carcdr.ycrdt.*;

YBinding binding = YBindingFactory.jni();

try (YDoc doc = binding.createDoc()) {
    // Text
    try (YText text = doc.getText("article")) {
        text.push("The quick brown fox");
        text.insert(4, "very ");  // "The very quick brown fox"
        text.delete(14, 6);       // "The very quick fox"
    }

    // Array
    try (YArray array = doc.getArray("list")) {
        array.pushString("apple");
        array.pushDouble(42.0);
        array.insertString(1, "banana");
        System.out.println(array.toJson()); // ["apple","banana",42.0]
    }

    // Map
    try (YMap map = doc.getMap("user")) {
        map.setString("name", "Alice");
        map.setDouble("score", 95.5);
        System.out.println(map.toJson()); // {"name":"Alice","score":95.5}
    }
}
```

### XML with Formatting

```java
try (YDoc doc = binding.createDoc()) {
    try (YXmlElement div = doc.getXmlElement("document")) {
        div.setAttribute("class", "container");

        try (YXmlText text = div.insertText(0)) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("bold", true);
            text.insertWithAttributes(0, "Important", attrs);
        }
    }
}
```

### Subdocuments

```java
try (YDoc doc = binding.createDoc()) {
    try (YMap map = doc.getMap("pages")) {
        try (YDoc pageDoc = binding.createDoc()) {
            try (YText content = pageDoc.getText("content")) {
                content.push("Page content");
            }
            map.setDoc("page1", pageDoc);
        }
    }
}
```

### Observers

```java
try (YDoc doc = binding.createDoc()) {
    try (YText text = doc.getText("myText")) {
        try (YSubscription sub = text.observe(event -> {
            for (Object change : event.getChanges()) {
                YTextChange tc = (YTextChange) change;
                System.out.println(tc.getType() + ": " + tc.getInsert());
            }
        })) {
            text.push("Hello");
        }
    }
}
```

### Transactions

Batch operations for fewer JNI calls and single observer notifications:

```java
try (YDoc doc = binding.createDoc()) {
    try (YText text = doc.getText("article")) {
        try (YTransaction txn = doc.beginTransaction()) {
            text.insert(txn, 0, "Title\n\n");
            text.insert(txn, 7, "Body text.");
        }
    }
}
```

Nested transactions are not supported. One transaction per document at a time.

### Synchronization

```java
try (YDoc doc1 = binding.createDoc(); YDoc doc2 = binding.createDoc()) {
    try (YText text = doc1.getText("shared")) {
        text.push("Hello");
    }

    // Differential sync using state vectors
    byte[] stateVector = doc2.encodeStateVector();
    byte[] diff = doc1.encodeDiff(stateVector);
    doc2.applyUpdate(diff);
}
```

## Documentation

- [API Reference (Javadoc)](https://carcdr.net/y-crdt-jni/)
- [Technical Details](IMPLEMENTATION.md)

## License

Apache License 2.0
