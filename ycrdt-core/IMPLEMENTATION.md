# Implementation Details - ycrdt-core

## Architecture

ycrdt-core is a pure interface module with no native code. It defines the contract that ycrdt-jni and ycrdt-panama implement.

```
ycrdt-core (interfaces)
    |
    +-- YBinding / YBindingFactory (SPI + factory)
    |
    +-- CRDT types: YDoc, YText, YArray, YMap
    |
    +-- XML types: YXmlElement, YXmlText, YXmlFragment, YXmlNode
    |
    +-- Transactions: YTransaction
    |
    +-- Observers: YObserver, UpdateObserver, YSubscription, ObserverErrorHandler
    |
    +-- Events: YEvent, YTextChange, YArrayChange, YMapChange, YXmlElementChange
    |
    +-- Formatting: FormattingChunk
```

## Implementation Discovery

`YBindingFactory` uses Java's `ServiceLoader` to find implementations:

```java
// auto() loads the first available
ServiceLoader.load(YBinding.class).findFirst()

// jni() / panama() filter by package name
ServiceLoader.load(YBinding.class).stream()
    .filter(p -> p.type().getName().contains(".jni."))
```

Implementations register via `META-INF/services/net.carcdr.ycrdt.YBinding`.

## Design Decisions

### All Types Are AutoCloseable

Every CRDT type interface extends `AutoCloseable`. This ensures native resources (pointers in JNI, memory segments in Panama) are released deterministically rather than relying on GC finalization.

### Transaction-Optional Operations

All mutation methods have two overloads: one without a transaction (auto-transactional) and one accepting a `YTransaction`. This lets callers choose between convenience and explicit batching.

```java
// Auto-transactional
text.insert(0, "hello");

// Explicit transaction
try (YTransaction txn = doc.beginTransaction()) {
    text.insert(txn, 0, "hello");
    text.insert(txn, 5, " world");
}
```

### Observer Error Handling

Observers run in the calling thread. If an observer throws, the default `ObserverErrorHandler` prints to stderr. Users can set a custom handler per document via `doc.setObserverErrorHandler()`.

### Value Types in Collections

YArray and YMap support three value types: `String`, `double`, and `YDoc` (subdocuments). Each has dedicated getter/setter methods rather than a generic `Object` API. This avoids boxing ambiguity and makes the native boundary explicit.

## Abstract Test Classes

The module provides abstract test base classes that define the test contract:

- `AbstractYDocTest` -- document creation, state encoding, sync
- `AbstractYTextTest` -- text operations, Unicode, observers
- `AbstractYArrayTest` -- array operations, type-specific access
- `AbstractYMapTest` -- map operations, containsKey, keys, clear

Concrete test classes in ycrdt-jni and ycrdt-panama extend these to verify each implementation against the same test suite.

## Dependencies

Runtime: none (pure Java interfaces).
Test: JUnit 4, ycrdt-jni (to run abstract tests against a concrete implementation).
