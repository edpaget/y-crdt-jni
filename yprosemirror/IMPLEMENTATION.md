# Implementation Details - yprosemirror

Technical implementation details for the yprosemirror ProseMirror integration module.

## Architecture

The yprosemirror module provides bidirectional synchronization between ProseMirror documents and Y-CRDT:

1. **ProseMirrorConverter** - Converts ProseMirror to Y-CRDT
2. **YCrdtConverter** - Converts Y-CRDT to ProseMirror
3. **YProseMirrorBinding** - Real-time bidirectional synchronization

## Dependencies

- **ycrdt module** - Core Y-CRDT JNI bindings
- **prosemirror-kotlin** (v1.1.13) - ProseMirror JVM implementation
- **kotlin-stdlib** (v2.1.0) - Kotlin interoperability

## ProseMirrorConverter

### Purpose

Converts ProseMirror document structures to Y-CRDT XML structures.

### Implementation

Main method: `nodeToYXml(Node node, YXmlFragment fragment, Schema schema)`

Process:
1. **Clear fragment** - Remove existing children
2. **Iterate node content** - Process each child node
3. **Handle node types**:
   - Text nodes → YXmlText with formatting marks as attributes
   - Element nodes → YXmlElement with attributes and recursive children
4. **Convert marks** - ProseMirror marks become Y-CRDT formatting attributes

### Mark Conversion

ProseMirror marks map to formatting attributes:
```java
Mark → Map<String, Object>
mark.type().name() → attribute key
mark.attrs() → attribute values (if any)
```

Example: `bold` mark → `{"bold": true}`

### Limitations

- Currently supports text and element nodes only
- Marks must be serializable to key-value pairs
- Node content must be iterable

## YCrdtConverter

### Purpose

Converts Y-CRDT XML structures to ProseMirror documents.

### Implementation

Main method: `yXmlToNode(YXmlFragment fragment, Schema schema)`

Process:
1. **Iterate Y-CRDT children** - Get all child nodes from fragment
2. **Convert each child**:
   - YXmlText → ProseMirror text node with marks
   - YXmlElement → ProseMirror element node with attributes
3. **Build node tree** - Create ProseMirror nodes with proper hierarchy

### Attribute Conversion

Y-CRDT formatting attributes map to ProseMirror marks:
```java
Map<String, Object> → List<Mark>
attribute key → mark type name
attribute values → mark attrs (if any)
```

Example: `{"bold": true}` → `bold` mark

### Type Mapping

- YXmlElement tag → ProseMirror node type
- YXmlText → text node
- Attributes → node attrs or marks (depending on context)

## YProseMirrorBinding

### Purpose

Provides real-time bidirectional synchronization between ProseMirror and Y-CRDT.

### Architecture

```
ProseMirror Document
       ↓ updateFromProseMirror()
YProseMirrorBinding
       ↓ (writes to)
YXmlFragment (Y-CRDT)
       ↓ (observes changes)
YProseMirrorBinding
       ↓ onUpdate callback
ProseMirror Editor
```

### Key Components

1. **YXmlFragment** - The shared Y-CRDT structure
2. **Schema** - ProseMirror schema for document structure
3. **Consumer<Node>** - Callback for Y-CRDT changes
4. **YSubscription** - Observer subscription handle
5. **AtomicBoolean** - Change loop prevention flag

### Change Flow

#### Local Changes (ProseMirror → Y-CRDT)

1. Editor emits ProseMirror document
2. `updateFromProseMirror(Node)` called
3. Set `updatingFromProseMirror` flag
4. Convert ProseMirror → Y-CRDT (ProseMirrorConverter)
5. Clear `updatingFromProseMirror` flag
6. Y-CRDT change triggers observer (but ignored due to flag)

#### Remote Changes (Y-CRDT → ProseMirror)

1. Remote update applied to Y-CRDT
2. Observer callback triggered
3. Check `updatingFromProseMirror` flag (skip if true)
4. Convert Y-CRDT → ProseMirror (YCrdtConverter)
5. Invoke `onUpdate` callback with new document
6. Editor updates to reflect changes

### Change Loop Prevention

The binding uses an `AtomicBoolean` flag to prevent infinite loops:

```java
private final AtomicBoolean updatingFromProseMirror = new AtomicBoolean(false);
```

When `updateFromProseMirror()` is called:
1. Set flag to `true`
2. Update Y-CRDT
3. Set flag to `false`

When Y-CRDT observer fires:
1. Check if flag is `true`
2. If true, skip update (change originated locally)
3. If false, convert and trigger callback

This prevents the loop:
```
ProseMirror → Y-CRDT → Observer → ProseMirror → Y-CRDT → ...
```

### Resource Management

YProseMirrorBinding implements `Closeable`:
- Closes Y-CRDT observer subscription
- Closes YXmlFragment
- Sets `closed` flag
- Prevents further operations

## Design Decisions

### Document-Level Synchronization

**Decision:** Use document-level sync (replace entire content)

**Rationale:**
- prosemirror-kotlin API differs from JavaScript ProseMirror
- Transaction API is complex when called from Java
- Document-level sync is simpler and more robust
- Sufficient for most use cases

**Trade-offs:**
- Less efficient for large documents
- More data transfer on each change
- Simpler implementation

### Callback-Based Updates

**Decision:** Use `Consumer<Node>` callback instead of direct plugin integration

**Rationale:**
- Maximum flexibility for different integration approaches
- No need to manage ProseMirror state internally
- Easier to test with mock callbacks
- Cleaner separation of concerns

**Trade-offs:**
- Caller must manually update editor
- No automatic plugin registration

### No Transaction Support

**Decision:** Don't expose ProseMirror transactions in the binding

**Rationale:**
- prosemirror-kotlin transaction API is complex
- Document-level sync doesn't need transactions
- Simplifies API surface

**Trade-offs:**
- Can't track fine-grained changes
- No undo/redo integration (yet)

## Testing Strategy

### Conversion Tests

ProseMirrorConverter and YCrdtConverter have comprehensive tests:
- Simple structures (paragraph + text)
- Nested elements (div > heading > text)
- Attributes (heading level, id)
- Multiple siblings
- Text formatting (bold, italic marks)
- Complex documents
- Synchronization across documents

### Binding Tests

Limited due to Schema construction challenges:
- Creating valid Schema instances from Java is complex
- Requires proper node/mark definitions
- Full testing requires actual ProseMirror environment

## Kotlin Interoperability

The module consumes prosemirror-kotlin (Kotlin library) from Java:

**Working features:**
- Creating Node and Mark instances
- Accessing node content and attributes
- Schema usage (when pre-constructed)

**Challenges:**
- Schema construction from Java
- Kotlin nullable types
- Extension functions
- Kotlin DSL builders

## Performance Considerations

### Document-Level Sync Overhead

Each change replaces entire document content:
- Clear Y-CRDT fragment
- Rebuild entire structure
- More overhead than incremental updates

Mitigation strategies:
- Use for documents < 10MB
- Consider debouncing rapid changes
- Future: transaction-level sync

### Memory Usage

Objects created per update:
- New YXmlElement per node
- New YXmlText per text segment
- Map instances for attributes
- Temporary collections

All properly closed via try-with-resources.

## Known Limitations

1. **No Transaction Support** - Cannot integrate with ProseMirror's transaction system
2. **Document-Level Only** - No incremental updates
3. **Limited Mark Support** - Only simple key-value marks
4. **No Position Mapping** - Phase 3 feature (not yet implemented)
5. **Schema Constraints** - Requires compatible node/mark definitions

## Future Enhancements

### Transaction-Level Sync

- Map ProseMirror steps to Y-CRDT operations
- More efficient for large documents
- Enables undo/redo integration

### Position Mapping

- Convert positions between ProseMirror and Y-CRDT
- Essential for cursor tracking
- Required for collaborative features

### Incremental Updates

- Only sync changed portions
- Reduce data transfer
- Better performance

### Cursor Synchronization

- Track remote cursors
- Display in editor
- Requires position mapping

## References

- [y-prosemirror](https://github.com/yjs/y-prosemirror) - Reference TypeScript implementation
- [prosemirror-kotlin](https://github.com/atlassian-labs/prosemirror-kotlin) - ProseMirror JVM
- [Y-CRDT](https://github.com/y-crdt/y-crdt) - Rust CRDT implementation
