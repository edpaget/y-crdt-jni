# yprosemirror - ProseMirror Y-CRDT Integration

Bidirectional synchronization between ProseMirror documents (via [prosemirror-kotlin](https://github.com/atlassian-labs/prosemirror-kotlin)) and Y-CRDT, for building collaborative editors on the JVM.

## Installation

```groovy
implementation 'net.carcdr:yprosemirror:0.1.0-SNAPSHOT'
```

Build from source: `./gradlew :yprosemirror:build`

## Usage

### Binding (Real-time Sync)

```java
import net.carcdr.ycrdt.*;
import net.carcdr.yprosemirror.*;
import com.atlassian.prosemirror.model.*;

YBinding binding = YBindingFactory.jni();

try (YDoc ydoc = binding.createDoc()) {
    try (YXmlFragment yFragment = ydoc.getXmlFragment("prosemirror")) {
        Schema schema = createMySchema();

        try (YProseMirrorBinding pmBinding = new YProseMirrorBinding(
            yFragment, schema,
            newDoc -> {
                // Called when Y-CRDT receives remote changes
                updateEditor(newDoc);
            }
        )) {
            // Push local ProseMirror changes to Y-CRDT
            pmBinding.updateFromProseMirror(localDoc);

            // Read current state
            Node current = pmBinding.getCurrentDocument();
        }
    }
}
```

### One-off Conversion

```java
// ProseMirror -> Y-CRDT
ProseMirrorConverter.nodeToYXml(proseMirrorNode, yFragment, schema);

// Y-CRDT -> ProseMirror
Node doc = YCrdtConverter.yXmlToNode(yFragment, schema);
```

## How It Works

- **Document-level sync**: On each change, the entire Y-CRDT content is replaced. Simpler than transaction-level sync, works around prosemirror-kotlin API differences from JavaScript ProseMirror.
- **Change loop prevention**: An `AtomicBoolean` flag prevents infinite loops when local changes trigger the Y-CRDT observer.
- **Callback-based updates**: Uses `Consumer<Node>` rather than direct ProseMirror plugin integration, keeping the binding decoupled from any specific editor setup.

## Known Limitations

1. Document-level sync only (no incremental updates)
2. No position mapping between ProseMirror and Y-CRDT coordinate systems
3. Only simple key-value marks supported
4. Schema construction from Java is complex (Kotlin interop)

## Documentation

- [Technical Details](IMPLEMENTATION.md)
- [Development Plan](PLAN.md)
- [y-prosemirror](https://github.com/yjs/y-prosemirror) -- reference TypeScript implementation

## License

Apache License 2.0
