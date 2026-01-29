# ycrdt-core - Y-CRDT Interfaces

Pure Java interfaces for Y-CRDT types. This module defines the API contract that implementations (ycrdt-jni, ycrdt-panama) provide. No native dependencies.

## Installation

```groovy
implementation 'net.carcdr:ycrdt-core:0.1.0-SNAPSHOT'
```

You also need at least one implementation on the classpath:

```groovy
// Pick one (or both):
implementation 'net.carcdr:ycrdt-jni:0.1.0-SNAPSHOT'
implementation 'net.carcdr:ycrdt-panama:0.1.0-SNAPSHOT'
```

## Usage

### Selecting an Implementation

```java
import net.carcdr.ycrdt.*;

YBinding binding = YBindingFactory.auto();    // first available via ServiceLoader
YBinding jni     = YBindingFactory.jni();     // explicit JNI
YBinding panama  = YBindingFactory.panama();  // explicit Panama

try (YDoc doc = binding.createDoc()) {
    // ...
}
```

### Interfaces

| Interface | Description |
|-----------|-------------|
| `YBinding` | SPI for implementations. Creates documents, merges updates. |
| `YBindingFactory` | Selects implementations by name or auto-detects via ServiceLoader. |
| `YDoc` | Root document container. State encoding, sync, transactions. |
| `YText` | Collaborative text with insert/delete/push. |
| `YArray` | Collaborative array (strings, doubles, subdocuments). |
| `YMap` | Collaborative map (strings, doubles, subdocuments). |
| `YXmlElement` | XML element with attributes and child nodes. |
| `YXmlText` | XML text node with formatting attributes. |
| `YXmlFragment` | XML node container. |
| `YTransaction` | Batches operations atomically. |
| `YSubscription` | Handle to a registered observer. Close to unsubscribe. |
| `YObserver` | Callback for type-level change events. |
| `UpdateObserver` | Callback for document-level binary updates. |
| `ObserverErrorHandler` | Callback for exceptions thrown by observers. |

### Event Types

| Class | Used By |
|-------|---------|
| `YEvent` | All observers -- carries target and list of changes. |
| `YTextChange` | YText, YXmlText -- insert/delete/retain with content and attributes. |
| `YArrayChange` | YArray, YXmlFragment -- insert/delete with items. |
| `YMapChange` | YMap -- key, old value, new value. |
| `YXmlElementChange` | YXmlElement -- attribute name, old value, new value. |

## Testing

ycrdt-core provides abstract test base classes (`AbstractYDocTest`, `AbstractYTextTest`, `AbstractYArrayTest`, `AbstractYMapTest`) that implementation modules extend. No concrete tests run in this module directly.

## License

Apache License 2.0
