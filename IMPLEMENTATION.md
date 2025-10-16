# Implementation Details

This document describes the technical implementation of the y-crdt JNI bindings, providing an overview of the architecture, components, and design decisions.

## Project Structure

This is a multi-module Gradle project with the following modules:

- **ycrdt** (`ycrdt/`) - Core Y-CRDT JNI bindings
  - Contains: Rust native library (Cargo project) + Java API
  - Artifact: `net.carcdr:ycrdt`
  - Location: `ycrdt/src/` for Rust, `ycrdt/src/main/java/` for Java

- **yprosemirror** (`yprosemirror/`) - ProseMirror integration (future work)
  - Contains: Pure Java library that depends on ycrdt
  - Artifact: `net.carcdr:yprosemirror`
  - Location: `yprosemirror/src/main/java/`

Both modules can be built independently or together using Gradle's multi-module capabilities. See the [Build System](#build-system) section for details on building individual modules.

## Architecture Overview

The project bridges Rust's Y-CRDT implementation (yrs) to the JVM through JNI (Java Native Interface), providing Java classes that wrap native Rust functionality. The architecture consists of three main layers:

1. **Rust JNI Layer** - Native methods that interface between Rust and Java (in ycrdt module)
2. **Java API Layer** - Idiomatic Java classes with proper resource management (in ycrdt module)
3. **Build System** - Multi-module Gradle + Cargo integration for seamless builds

## Rust Implementation

### Configuration (`Cargo.toml`)

The project is configured as a `cdylib` (C-compatible dynamic library) to support JNI:

```toml
[lib]
crate-type = ["cdylib"]
name = "ycrdt_jni"
```

**Dependencies:**
- `jni` (v0.21.1) - JNI bindings for Rust
- `yrs` (v0.21.3) - Y-CRDT implementation

**Optimizations:**
- Link-time optimization (LTO) enabled
- Optimization level 3 for release builds
- Code generation units optimized for single-threaded compilation

### Core Utilities (`src/lib.rs`)

Provides foundational JNI helper functions used across all bindings:

**String Conversion:**
- `to_jstring(env, str) -> jstring` - Converts Rust strings to Java strings with proper error handling

**Exception Handling:**
- `throw_exception(env, message)` - Throws Java `RuntimeException` from Rust code

**Pointer Management:**
- `to_java_ptr<T>(obj) -> jlong` - Converts Rust objects to Java pointers using `Box::into_raw()`
- `from_java_ptr<T>(ptr) -> &T` - Converts Java pointers back to Rust references
- `free_java_ptr<T>(ptr)` - Frees Rust objects using `Box::from_raw()`

These functions ensure safe memory management across the JNI boundary.

### YDoc Bindings (`src/ydoc.rs`)

Implements the core document type with 7 native methods:

1. **`nativeCreate()`** - Creates a new YDoc with random client ID
2. **`nativeCreateWithClientId(clientId)`** - Creates YDoc with specific client ID
3. **`nativeDestroy(ptr)`** - Frees YDoc memory
4. **`nativeGetClientId(ptr)`** - Returns the document's client ID
5. **`nativeGetGuid(ptr)`** - Returns the document's GUID as a string
6. **`nativeEncodeStateAsUpdate(ptr)`** - Encodes full document state as byte array
7. **`nativeApplyUpdate(ptr, update)`** - Applies binary update to document

**Key Implementation Details:**
- All methods validate pointer arguments before dereferencing
- String conversions handle UTF-8 encoding properly
- State encoding uses empty state vector to export full document state
- Proper error handling with Java exceptions

**Testing:**
- 3 Rust unit tests covering creation, client ID, and state encoding

### YText Bindings (`src/ytext.rs`)

Implements collaborative text editing with 7 native methods:

1. **`nativeGetText(docPtr, name)`** - Gets or creates a YText instance
2. **`nativeDestroy(ptr)`** - Frees YText memory
3. **`nativeLength(docPtr, textPtr)`** - Returns text length in UTF-8 bytes
4. **`nativeToString(docPtr, textPtr)`** - Returns text content as string
5. **`nativeInsert(docPtr, textPtr, index, chunk)`** - Inserts text at index
6. **`nativePush(docPtr, textPtr, chunk)`** - Appends text to end
7. **`nativeDelete(docPtr, textPtr, index, length)`** - Deletes text range

**Key Implementation Details:**
- Uses `TextRef` for reference-counted access to shared text
- All operations require both document and text pointers
- Transactions created automatically for each operation
- Modified UTF-8 handling for Java string compatibility
- Unicode and emoji support verified

**Testing:**
- 4 Rust unit tests covering creation, insert/read, push, and delete

### YArray Bindings (`src/yarray.rs`)

Implements collaborative arrays with 11 native methods:

1. **`nativeGetArray(docPtr, name)`** - Gets or creates a YArray instance
2. **`nativeDestroy(ptr)`** - Frees YArray memory
3. **`nativeLength(docPtr, arrayPtr)`** - Returns array length
4. **`nativeGetString(docPtr, arrayPtr, index)`** - Gets string at index
5. **`nativeGetDouble(docPtr, arrayPtr, index)`** - Gets double at index
6. **`nativeInsertString(docPtr, arrayPtr, index, value)`** - Inserts string at index
7. **`nativeInsertDouble(docPtr, arrayPtr, index, value)`** - Inserts double at index
8. **`nativePushString(docPtr, arrayPtr, value)`** - Appends string to end
9. **`nativePushDouble(docPtr, arrayPtr, value)`** - Appends double to end
10. **`nativeRemove(docPtr, arrayPtr, index, length)`** - Removes range of elements
11. **`nativeToJson(docPtr, arrayPtr)`** - Serializes array to JSON string

**Key Implementation Details:**
- Uses `ArrayRef` for reference-counted access to shared array
- Supports mixed types (strings and doubles)
- Type-specific getters return appropriate defaults on type mismatch
- JSON serialization uses `ToJson` trait from yrs
- Result error handling for type conversions

**Testing:**
- 4 Rust unit tests covering creation, push/read, insert, and remove

### YMap Bindings (`src/ymap.rs`)

Implements collaborative maps with 12 native methods:

1. **`nativeGetMap(docPtr, name)`** - Gets or creates a YMap instance
2. **`nativeDestroy(ptr)`** - Frees YMap memory
3. **`nativeSize(docPtr, mapPtr)`** - Returns map size
4. **`nativeGetString(docPtr, mapPtr, key)`** - Gets string value by key
5. **`nativeGetDouble(docPtr, mapPtr, key)`** - Gets double value by key
6. **`nativeSetString(docPtr, mapPtr, key, value)`** - Sets string value
7. **`nativeSetDouble(docPtr, mapPtr, key, value)`** - Sets double value
8. **`nativeRemove(docPtr, mapPtr, key)`** - Removes key from map
9. **`nativeContainsKey(docPtr, mapPtr, key)`** - Checks if key exists
10. **`nativeKeys(docPtr, mapPtr)`** - Returns all keys as String array
11. **`nativeClear(docPtr, mapPtr)`** - Clears all entries
12. **`nativeToJson(docPtr, mapPtr)`** - Serializes map to JSON string

**Key Implementation Details:**
- Uses `MapRef` for reference-counted access to shared map
- Supports mixed types (strings and doubles)
- Key-based access pattern typical of maps
- Type-specific getters return appropriate defaults on type mismatch or missing keys
- Keys returned as Java String array with proper JNI object creation
- JSON serialization uses `ToJson` trait from yrs
- Lifetime parameters required for JObject return types in JNI

**Testing:**
- 4 Rust unit tests covering creation, set/get, remove, and clear

### YXmlText Bindings (`src/yxmltext.rs`)

Implements collaborative XML text editing with rich text formatting and ancestor lookup support, 11 native methods:

1. **`nativeGetXmlText(docPtr, name)`** - Gets or creates a YXmlText instance
2. **`nativeDestroy(ptr)`** - Frees YXmlText memory
3. **`nativeLength(docPtr, xmlTextPtr)`** - Returns XML text length
4. **`nativeToString(docPtr, xmlTextPtr)`** - Returns XML text content as string
5. **`nativeInsert(docPtr, xmlTextPtr, index, chunk)`** - Inserts text at index
6. **`nativePush(docPtr, xmlTextPtr, chunk)`** - Appends text to end
7. **`nativeDelete(docPtr, xmlTextPtr, index, length)`** - Deletes text range
8. **`nativeInsertWithAttributes(docPtr, xmlTextPtr, index, chunk, attributes)`** - Inserts text with formatting
9. **`nativeFormat(docPtr, xmlTextPtr, index, length, attributes)`** - Applies formatting to text range
10. **`nativeGetParent(docPtr, xmlTextPtr)`** - Returns parent node [type, pointer] array
11. **`nativeGetIndexInParent(docPtr, xmlTextPtr)`** - Returns index within parent's children

**Key Implementation Details:**
- Uses `XmlFragmentRef` with lazy `XmlTextPrelim` child creation
- XmlFragmentRef is the root type (implements `RootRef` in yrs)
- XmlTextRef is accessed as child at index 0 within the fragment
- All operations require both document and XML text pointers
- Transactions created automatically for each operation
- Unicode and emoji support verified
- **Rich Text Formatting:**
  - `convert_java_map_to_attrs()` helper converts Java `Map<String, Object>` to Rust `HashMap<Arc<str>, Any>`
  - Supports Boolean, Integer, Long, Double, Float, and String attribute values
  - Type detection via JNI reflection (`getClass().getName()`)
  - Null values in map translate to `Any::Null` for format removal
  - Formatting synchronized across documents via CRDT update mechanism
- **Ancestor Lookup:**
  - `parent()` method returns `Option<XmlOut>` (Element or Fragment)
  - Parent nodes returned as BranchPtr (reference-counted, safe to clone)
  - Index calculation iterates parent's children, comparing BranchIDs via `AsRef<Branch>::as_ref().id()`
  - Match on concrete parent types (Element, Fragment) to access `len()` and `get()` methods
  - Returns -1 if no parent or node not found in parent's children

**Testing:**
- 7 Rust unit tests covering creation, insert/read, push, delete, and formatting (format, insertWithAttributes, removeFormat)

### YXmlElement Bindings (`src/yxmlelement.rs`)

Implements collaborative XML elements with hierarchical support and ancestor lookup, 15 native methods:

1. **`nativeGetXmlElement(docPtr, name)`** - Gets or creates a YXmlElement instance
2. **`nativeDestroy(ptr)`** - Frees YXmlElement memory
3. **`nativeGetTag(docPtr, xmlElementPtr)`** - Returns element tag name
4. **`nativeGetAttribute(docPtr, xmlElementPtr, name)`** - Gets attribute value by name
5. **`nativeSetAttribute(docPtr, xmlElementPtr, name, value)`** - Sets attribute value
6. **`nativeRemoveAttribute(docPtr, xmlElementPtr, name)`** - Removes attribute by name
7. **`nativeGetAttributeNames(docPtr, xmlElementPtr)`** - Returns all attribute names as String array
8. **`nativeToString(docPtr, xmlElementPtr)`** - Returns XML string representation
9. **`nativeChildCount(docPtr, xmlElementPtr)`** - Returns number of child nodes
10. **`nativeInsertElement(docPtr, xmlElementPtr, index, tag)`** - Inserts element child at index
11. **`nativeInsertText(docPtr, xmlElementPtr, index)`** - Inserts text child at index
12. **`nativeGetChild(docPtr, xmlElementPtr, index)`** - Returns child node [type, pointer] array
13. **`nativeRemoveChild(docPtr, xmlElementPtr, index)`** - Removes child at index
14. **`nativeGetParent(docPtr, xmlElementPtr)`** - Returns parent node [type, pointer] array
15. **`nativeGetIndexInParent(docPtr, xmlElementPtr)`** - Returns index within parent's children

**Key Implementation Details:**
- Uses `XmlFragmentRef` with lazy `XmlElementPrelim` child creation
- XmlFragmentRef is the root type (implements `RootRef` in yrs)
- XmlElementRef is accessed as child at index 0 within the fragment
- Attribute operations use `insert_attribute()`, `get_attribute()`, `remove_attribute()`
- Tag name retrieved without transaction parameter (immutable)
- Returns JObject for String array in `getAttributeNames()`
- **Hierarchical Support:**
  - `XmlElementRef` implements `XmlFragment` trait, providing child management methods
  - Child insertion uses `insert()` method with `XmlElementPrelim` or `XmlTextPrelim`
  - Child retrieval returns `XmlOut` enum (Element, Text, or Fragment)
  - Polymorphic child handling via `Object[]` with type indicator (0=Element, 1=Text)
  - Child pointers returned as independent closeable Java objects
  - Supports deeply nested XML structures (elements within elements)
- **Ancestor Lookup:**
  - `parent()` method returns `Option<XmlOut>` (Element or Fragment)
  - Parent nodes returned as BranchPtr (reference-counted, safe to clone)
  - Index calculation iterates parent's children, comparing BranchIDs via `AsRef<Branch>::as_ref().id()`
  - Match on concrete parent types (Element, Fragment) to access `len()` and `get()` methods
  - Returns -1 if no parent or node not found in parent's children
- Lifetime parameters required for JNI object returns

**Testing:**
- 4 Rust unit tests covering creation, tag retrieval, attributes, and attribute removal

### YXmlFragment Bindings (`src/yxmlfragment.rs`)

Implements collaborative XML fragment containers with 9 native methods:

1. **`nativeGetFragment(docPtr, name)`** - Gets or creates a YXmlFragment instance
2. **`nativeDestroy(ptr)`** - Frees YXmlFragment memory
3. **`nativeLength(docPtr, fragmentPtr)`** - Returns number of child nodes
4. **`nativeInsertElement(docPtr, fragmentPtr, index, tag)`** - Inserts element child at index
5. **`nativeInsertText(docPtr, fragmentPtr, index, content)`** - Inserts text child at index
6. **`nativeRemove(docPtr, fragmentPtr, index, length)`** - Removes range of children
7. **`nativeGetNodeType(docPtr, fragmentPtr, index)`** - Returns node type (ELEMENT=0, TEXT=1)
8. **`nativeGetElement(docPtr, fragmentPtr, index)`** - Returns XmlElementRef pointer at index
9. **`nativeGetText(docPtr, fragmentPtr, index)`** - Returns XmlTextRef pointer at index

**Key Implementation Details:**
- Uses `XmlFragmentRef` directly (no wrapper)
- Child retrieval returns direct `XmlElementRef` and `XmlTextRef` pointers
- Node type detection using `into_xml_element()` and `into_xml_text()` conversions
- Enables hierarchical XML tree navigation
- BranchPtr is reference-counted, safe to clone for multiple Java objects

**Architecture Change (2025-10-16):**
- Updated `nativeGetXmlElement` and `nativeGetXmlText` (in yxmlelement.rs and yxmltext.rs) to return direct element/text pointers instead of fragment wrappers
- Both old pattern (`doc.getXmlElement()`) and new pattern (`fragment.getElement()`) now use direct pointer architecture
- Eliminates confusion between XmlFragmentRef wrapper and XmlElementRef/XmlTextRef direct references

**Testing:**
- 7 Rust unit tests covering creation, insertion, removal, node type detection, and child retrieval

## Java Implementation

### YDoc (`src/main/java/net/carcdr/ycrdt/YDoc.java`)

The main document class providing a Java-friendly API:

**Constructors:**
- `YDoc()` - Creates document with random client ID
- `YDoc(long clientId)` - Creates document with specific client ID (validates non-negative)

**Public Methods:**
- `long getClientId()` - Returns the client ID
- `String getGuid()` - Returns the document GUID
- `byte[] encodeStateAsUpdate()` - Exports document state for synchronization
- `void applyUpdate(byte[] update)` - Imports state from another document
- `YText getText(String name)` - Gets or creates a YText instance
- `YArray getArray(String name)` - Gets or creates a YArray instance
- `YMap getMap(String name)` - Gets or creates a YMap instance
- `YXmlText getXmlText(String name)` - Gets or creates a YXmlText instance
- `YXmlElement getXmlElement(String name)` - Gets or creates a YXmlElement instance
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if document is closed

**Design Features:**
- Implements `Closeable` for try-with-resources pattern
- Thread-safe close operation using synchronized block
- `ensureNotClosed()` validation on all operations
- Comprehensive JavaDoc with usage examples
- Finalizer as safety net for missed close() calls
- Proper null checking with meaningful exceptions

**Testing:**
- 13 comprehensive tests covering all functionality

### YText (`src/main/java/net/carcdr/ycrdt/YText.java`)

Collaborative text editing class:

**Public Methods:**
- `int length()` - Returns text length
- `String toString()` - Returns text content
- `void insert(int index, String chunk)` - Inserts text at index
- `void push(String chunk)` - Appends text to end
- `void delete(int index, int length)` - Deletes text range
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getText()`)
- Input validation with meaningful exceptions
- Index bounds checking
- Null checking for all string parameters
- Thread-safe close operation
- Comprehensive JavaDoc

**Testing:**
- 23 comprehensive tests including unicode, synchronization, and edge cases

### YArray (`src/main/java/net/carcdr/ycrdt/YArray.java`)

Collaborative array class supporting mixed types:

**Public Methods:**
- `int length()` - Returns array length
- `String getString(int index)` - Gets string at index (returns null if out of bounds)
- `double getDouble(int index)` - Gets double at index (returns 0.0 if out of bounds)
- `void insertString(int index, String value)` - Inserts string at index
- `void insertDouble(int index, double value)` - Inserts double at index
- `void pushString(String value)` - Appends string to end
- `void pushDouble(double value)` - Appends double to end
- `void remove(int index, int length)` - Removes range of elements
- `String toJson()` - Serializes array to JSON
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getArray()`)
- Type-specific methods for strings and doubles
- Comprehensive bounds checking
- Null checking for string parameters
- Thread-safe close operation
- JSON serialization for inspection/debugging
- Comprehensive JavaDoc

**Testing:**
- 27 comprehensive tests covering all operations, mixed types, and synchronization

### YMap (`src/main/java/net/carcdr/ycrdt/YMap.java`)

Collaborative map class supporting mixed types:

**Public Methods:**
- `int size()` - Returns number of entries in map
- `boolean isEmpty()` - Checks if map is empty
- `String getString(String key)` - Gets string value by key (returns null if not found)
- `double getDouble(String key)` - Gets double value by key (returns 0.0 if not found)
- `void setString(String key, String value)` - Sets string value
- `void setDouble(String key, double value)` - Sets double value
- `void remove(String key)` - Removes key from map
- `boolean containsKey(String key)` - Checks if key exists
- `String[] keys()` - Returns all keys as array
- `void clear()` - Removes all entries
- `String toJson()` - Serializes map to JSON
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getMap()`)
- Type-specific methods for strings and doubles
- Comprehensive null checking for keys and values
- Thread-safe close operation
- JSON serialization for inspection/debugging
- Comprehensive JavaDoc

**Testing:**
- 30 comprehensive tests covering all operations, mixed types, and synchronization

### YXmlText (`src/main/java/net/carcdr/ycrdt/YXmlText.java`)

Collaborative XML text editing class with rich text formatting and ancestor lookup support:

**Public Methods:**
- `int length()` - Returns XML text length
- `String toString()` - Returns XML text content with formatting as XML tags
- `void insert(int index, String chunk)` - Inserts text at index
- `void push(String chunk)` - Appends text to end
- `void delete(int index, int length)` - Deletes text range
- `void insertWithAttributes(int index, String chunk, Map<String, Object> attributes)` - Inserts text with formatting
- `void format(int index, int length, Map<String, Object> attributes)` - Applies formatting to existing text
- `Object getParent()` - Returns parent node (YXmlElement or YXmlFragment), or null if no parent
- `int getIndexInParent()` - Returns 0-based index within parent's children, or -1 if no parent
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getXmlText()`)
- Input validation with meaningful exceptions
- Index bounds checking
- Null checking for all string and map parameters
- Thread-safe close operation
- Comprehensive JavaDoc with formatting examples
- Unicode and emoji support
- **Rich Text Formatting:**
  - Arbitrary formatting attributes supported (bold, italic, color, font, custom)
  - Map values can be Boolean, Integer, Long, Double, String
  - Null attribute values remove formatting
  - Formatting rendered as XML tags in `toString()` output
  - Formatting synchronized across documents
- **Ancestor Lookup:**
  - Navigate upward through XML tree hierarchy with `getParent()`
  - Determine position within parent using `getIndexInParent()`
  - Parent can be YXmlElement or YXmlFragment (returned polymorphically as Object)
  - Parent references are independent closeable instances
  - Synchronized parent references across documents via CRDT

**Testing:**
- 41 comprehensive tests including unicode, synchronization, formatting, ancestor lookup, and edge cases
- 14 tests specifically for rich text formatting features
- 7 tests specifically for ancestor lookup (parent retrieval, index calculation, mixed children, nested structures, synchronization)

### YXmlElement (`src/main/java/net/carcdr/ycrdt/YXmlElement.java`)

Collaborative XML element class with attribute management, hierarchical support, and ancestor lookup:

**Public Methods:**
- `String getTag()` - Returns element tag name
- `String getAttribute(String name)` - Gets attribute value by name (returns null if not found)
- `void setAttribute(String name, String value)` - Sets attribute value
- `void removeAttribute(String name)` - Removes attribute by name
- `String[] getAttributeNames()` - Returns all attribute names as array
- `String toString()` - Returns XML string representation
- `int childCount()` - Returns number of child nodes
- `YXmlElement insertElement(int index, String tag)` - Inserts element child at index
- `YXmlText insertText(int index)` - Inserts text child at index
- `Object getChild(int index)` - Returns child (YXmlElement or YXmlText) at index
- `void removeChild(int index)` - Removes child at index
- `Object getParent()` - Returns parent node (YXmlElement or YXmlFragment), or null if no parent
- `int getIndexInParent()` - Returns 0-based index within parent's children, or -1 if no parent
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getXmlElement()` or returned from `insertElement()`)
- Attribute key-value pair management
- **Hierarchical XML Support:**
  - Insert element and text children at any index
  - Retrieve children polymorphically (returns Object, cast to YXmlElement or YXmlText)
  - Remove children by index
  - Supports deeply nested XML structures
  - Child objects are independent closeable instances
- **Ancestor Lookup:**
  - Navigate upward through XML tree hierarchy with `getParent()`
  - Determine position within parent using `getIndexInParent()`
  - Parent can be YXmlElement or YXmlFragment (returned polymorphically as Object)
  - Parent references are independent closeable instances
  - Synchronized parent references across documents via CRDT
- Comprehensive null checking for attribute names, values, and tags
- Index bounds checking for child operations
- Thread-safe close operation
- XML string serialization for inspection
- Comprehensive JavaDoc

**Testing:**
- 55 comprehensive tests (25 original + 18 nested element tests + 12 ancestor lookup tests) covering:
  - Attribute operations, tag retrieval, and synchronization
  - Child count and insertion (elements and text)
  - Child retrieval and removal
  - Nested element structures
  - Mixed children (elements and text)
  - Deeply nested structures (5+ levels)
  - Cross-document synchronization with nested content
  - **Ancestor lookup tests:**
    - Parent retrieval for root elements (fragment parents)
    - Parent retrieval for nested elements (element parents)
    - Index calculation within parent
    - Navigation through deeply nested hierarchies (4+ levels)
    - Parent synchronization across documents
  - Error handling (null tags, negative indices, out of bounds)

### YXmlFragment (`src/main/java/net/carcdr/ycrdt/YXmlFragment.java`)

Collaborative XML fragment container class for hierarchical XML structures:

**Public Methods:**
- `int length()` - Returns number of child nodes
- `void insertElement(int index, String tag)` - Inserts element child at index
- `void insertText(int index, String content)` - Inserts text child at index
- `void remove(int index, int length)` - Removes range of children
- `YXmlNode.NodeType getNodeType(int index)` - Returns node type (ELEMENT or TEXT)
- `YXmlElement getElement(int index)` - Retrieves element child (returns null if not an element)
- `YXmlText getText(int index)` - Retrieves text child (returns null if not text)
- `String toXmlString()` - Returns XML string representation
- `void close()` - Frees native resources
- `boolean isClosed()` - Checks if closed

**Design Features:**
- Implements `Closeable` for resource management
- Package-private constructor (created via `YDoc.getXmlFragment()`)
- Child node retrieval creates independent YXmlElement/YXmlText instances
- Comprehensive bounds checking
- Null checking for content parameters
- Thread-safe close operation
- Supports hierarchical XML tree construction
- Comprehensive JavaDoc with examples

**Child Node Retrieval (Added 2025-10-16):**
- `getElement(index)` and `getText(index)` return independent closeable instances
- Child objects must be closed separately using try-with-resources
- Returns null if index is out of bounds or wrong node type
- Enables tree navigation and manipulation

**Ancestor Lookup Support (Added 2025-10-16):**
- Added package-private constructor accepting `long nativeHandle` directly
- Enables YXmlFragment to be returned from `getParent()` calls in YXmlElement/YXmlText
- Fragment pointer is reference-counted BranchPtr, safe to return as independent Java object

**Testing:**
- 9 comprehensive tests (2 basic + 7 for child retrieval) covering:
  - Fragment creation and lifecycle
  - Child insertion (elements and text)
  - Node type detection
  - Child retrieval and manipulation
  - Modification persistence
  - Cross-document synchronization
  - Error handling (null values, out of bounds)

### NativeLoader (`src/main/java/net/carcdr/ycrdt/NativeLoader.java`)

Utility class for loading platform-specific native libraries:

**Loading Strategy:**
1. Attempt to load from system library path (`System.loadLibrary()`)
2. If that fails, extract from JAR resources to temporary file
3. Load from temporary file location

**Platform Support:**
- **Operating Systems:** Linux, macOS (Darwin), Windows
- **Architectures:** x86_64 (amd64), aarch64 (arm64)
- **Naming Conventions:**
  - Linux: `libycrdt_jni.so`
  - macOS: `libycrdt_jni.dylib`
  - Windows: `ycrdt_jni.dll`

**Error Handling:**
- Detailed error messages for debugging
- Fallback mechanisms for different loading strategies
- Platform/architecture detection logging

### Example Program (`src/main/java/net/carcdr/ycrdt/Example.java`)

Comprehensive demonstration program with 14 examples:

1. **Creating a YDoc** - Basic document creation
2. **Creating YDoc with client ID** - Custom client ID
3. **Synchronizing documents** - Document state synchronization
4. **Working with YText** - Text editing operations
5. **Synchronizing YText** - Collaborative text editing between documents
6. **Working with YArray** - Array operations with mixed types
7. **Synchronizing YArray** - Collaborative array editing between documents
8. **Working with YMap** - Map operations with mixed types
9. **Synchronizing YMap** - Collaborative map editing between documents
10. **Working with YXmlText** - XML text editing operations
11. **Synchronizing YXmlText** - Collaborative XML text editing between documents
12. **Working with YXmlElement** - XML element with attribute management
13. **Synchronizing YXmlElement** - Collaborative XML element editing between documents
14. **Proper cleanup** - Resource management demonstration

**Code Organization:**
- Main method delegates to helper methods
- Each example is self-contained in a private static method
- Consistent output formatting
- Proper resource management with try-with-resources

## Build System

### Multi-Module Gradle Configuration

The project uses a multi-module Gradle structure with independent and shared build configurations:

**Root Project (`build.gradle`):**
- Defines shared configuration for all subprojects
- Sets group: `net.carcdr` and version: `0.1.0-SNAPSHOT`
- Applies common plugins (java-library, checkstyle)
- Configures Java 11 compatibility and Checkstyle for all modules

**ycrdt Module (`ycrdt/build.gradle`):**
Integrates Rust and Java builds seamlessly:

**Java Configuration:**
- Java 11 source/target compatibility
- JUnit 4.13.2 for testing
- Checkstyle for code quality
- Maven publishing with artifactId `ycrdt`

**Custom Gradle Tasks:**

1. **`buildRustLibrary`** - Executes `cargo build --release`
   - Compiles Rust code to native library
   - Uses release profile for optimization
   - Working directory: `ycrdt/` module directory

2. **`copyNativeLibrary`** - Copies native library to resources
   - Depends on `buildRustLibrary`
   - Copies to `ycrdt/build/resources/main/native/`
   - Handles platform-specific naming

3. **`cleanRust`** - Executes `cargo clean`
   - Cleans Rust build artifacts

4. **`testRust`** - Executes `cargo test`
   - Runs Rust unit tests
   - Integrated into Java test task

**yprosemirror Module (`yprosemirror/build.gradle`):**
- Java-only module (no Rust)
- Depends on ycrdt module: `api project(':ycrdt')`
- Maven publishing with artifactId `yprosemirror`

**Build Commands:**
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :ycrdt:build
./gradlew :yprosemirror:build

# Run tests for specific module
./gradlew :ycrdt:test

# List all modules
./gradlew projects
```

**Build Pipeline:**
- Java compilation in ycrdt depends on Rust library build
- yprosemirror module depends on ycrdt module
- Tests run both Rust and Java test suites
- Clean task cleans both Rust and Java artifacts

### CI/CD (`github/workflows/`)

Automated workflows for quality assurance:

**Quick Check Workflow:**
- Rust formatting check (`cargo fmt --check`)
- Rust linting (`cargo clippy`)
- Fast feedback for pull requests

**CI Workflow:**
- Multi-platform builds (Ubuntu, macOS, Windows)
- Rust and Java test execution
- Checkstyle validation
- Runs on push and pull requests

**Release Workflow:**
- Triggered by version tags
- Builds native libraries for all platforms
- Creates GitHub releases with artifacts

**Javadoc Workflow:**
- Generates Javadoc HTML
- Publishes to GitHub Pages
- Automatic deployment on main branch updates

## Memory Management

### Rust Side

**Allocation:**
- Objects wrapped in `Box` for heap allocation
- `Box::into_raw()` converts to raw pointer for Java
- Pointer cast to `jlong` for safe transmission

**Deallocation:**
- Java calls native `destroy` method
- `Box::from_raw()` reconstitutes Box
- Box drops, freeing memory automatically

**Safety Guarantees:**
- All pointers validated before dereferencing
- Zero pointer checks prevent null dereference
- Proper exception handling on errors

### Java Side

**Resource Management:**
- All types implement `Closeable` interface
- `close()` method calls native `destroy`
- `closed` flag prevents use-after-free
- Synchronized close prevents double-free

**Safety Mechanisms:**
- `ensureNotClosed()` / `checkClosed()` on all operations
- IllegalStateException thrown on closed objects
- Finalizer as safety net (deprecated but functional)
- Try-with-resources pattern recommended

**Lifecycle:**
```
Create → Use → Close → Disposed
         ↑      ↓
         └──────┘ (multiple operations allowed)
```

## Type System Mapping

### Primitive Types

| Rust Type | JNI Type | Java Type |
|-----------|----------|-----------|
| `i64` | `jlong` | `long` |
| `i32` | `jint` | `int` |
| `f64` | `jdouble` | `double` |
| `bool` | `jboolean` | `boolean` |

### Complex Types

| Rust Type | Java Type | Conversion |
|-----------|-----------|------------|
| `String` | `String` | `to_jstring()` + `get_string()` |
| `Vec<u8>` | `byte[]` | JNI byte array functions |
| `*mut T` | `long` | Pointer as 64-bit integer |

### Y-CRDT Types

| yrs Type | Rust Binding | Java Class |
|----------|--------------|------------|
| `Doc` | Boxed ownership | `YDoc` |
| `TextRef` | Shared reference | `YText` |
| `ArrayRef` | Shared reference | `YArray` |
| `MapRef` | Shared reference | `YMap` |
| `XmlFragmentRef` | Shared reference (with XmlTextPrelim child) | `YXmlText` |
| `XmlFragmentRef` | Shared reference (with XmlElementPrelim child) | `YXmlElement` |

## Error Handling

### Rust Error Strategy

**Pointer Validation:**
```rust
if ptr == 0 {
    throw_exception(&mut env, "Invalid pointer");
    return default_value;
}
```

**String Conversion:**
```rust
let string = match env.get_string(&input) {
    Ok(s) => s,
    Err(_) => {
        throw_exception(&mut env, "Failed to get string");
        return;
    }
};
```

**Result Handling:**
- Type conversions return `Result<T, E>`
- Errors converted to null/default values or exceptions
- Never panics across JNI boundary

### Java Error Strategy

**Exception Types:**
- `IllegalStateException` - Closed object accessed
- `IllegalArgumentException` - Invalid parameters (null, negative)
- `IndexOutOfBoundsException` - Array/text index out of range
- `RuntimeException` - Native errors bubbled from Rust

**Validation Order:**
1. Closed state check
2. Null parameter check
3. Bounds/range check
4. Native call (may throw RuntimeException)

## Testing Strategy

### Rust Tests (33 total)

**Unit Tests:**
- `lib.rs` - Pointer conversion (1 test)
- `ydoc.rs` - Document operations (3 tests)
- `ytext.rs` - Text operations (4 tests)
- `yarray.rs` - Array operations (4 tests)
- `ymap.rs` - Map operations (4 tests)
- `yxmltext.rs` - XML text operations (7 tests, including 3 formatting tests)
- `yxmlelement.rs` - XML element operations (4 tests)
- `yxmlfragment.rs` - XML fragment operations (7 tests)

**Coverage:**
- Creation and destruction
- Basic operations
- Type conversions
- Memory safety

### Java Tests (198 total)

**YDocTest (13 tests):**
- Creation and lifecycle
- Client ID management
- State encoding/decoding
- Synchronization
- Error handling

**YTextTest (23 tests):**
- Text operations (insert, push, delete)
- Unicode and emoji support
- Synchronization
- Complex editing sequences
- Error handling and edge cases

**YArrayTest (27 tests):**
- Array operations (push, insert, remove)
- Mixed type handling
- JSON serialization
- Synchronization (unidirectional and bidirectional)
- Error handling and boundary conditions

**YMapTest (30 tests):**
- Map operations (set, get, remove, containsKey, keys, clear)
- Mixed type handling
- JSON serialization
- Synchronization (unidirectional and bidirectional)
- Error handling (null keys, null values)
- Complex operation sequences
- Multiple maps in same document

**YXmlTextTest (41 tests):**
- XML text operations (insert, push, delete)
- Unicode and emoji support
- Synchronization
- Complex editing sequences
- Error handling and edge cases
- Bidirectional synchronization
- **Rich text formatting (14 tests):**
  - Format with bold and italic
  - Insert text with attributes
  - Multiple formatting attributes
  - Format removal
  - Complex formatting sequences
  - Formatting synchronization
  - Mixed content and formatting
  - Various attribute types (Boolean, Integer, String, Double)
  - Error handling (null attributes, negative indices)
- **Ancestor lookup (7 tests):**
  - Parent retrieval for text nodes
  - Index calculation within parent
  - Mixed children with elements
  - Nested structures
  - Parent synchronization across documents

**YXmlElementTest (55 tests):**
- Element creation and tag retrieval
- Attribute operations (get, set, remove)
- Attribute name enumeration
- Synchronization (unidirectional and bidirectional)
- Error handling (null names, null values)
- Multiple attributes and complex sequences
- Multiple elements in same document
- **Nested element tests (18 tests):**
  - Child count tracking
  - Element and text child insertion
  - Multiple children in correct order
  - Child retrieval by index with type checking
  - Child removal operations
  - Nested element hierarchies (3+ levels)
  - Mixed children (elements and text together)
  - Cross-document synchronization with nested structures
  - Complex nested structures (5+ levels deep)
  - XML string representation with nesting
  - Error handling (null tags, negative indices, out of bounds)
- **Ancestor lookup (12 tests):**
  - Parent retrieval for root elements (fragment parents)
  - Parent retrieval for nested elements (element parents)
  - Index calculation within parent
  - Navigation through deeply nested hierarchies (4+ levels)
  - Parent synchronization across documents

**YXmlFragmentTest (9 tests):**
- Fragment creation and lifecycle
- Child insertion (elements and text)
- Node removal operations
- Node type detection
- Child retrieval (getElement, getText)
- Child modification and persistence
- Cross-document synchronization with child retrieval
- Error handling (null values, out of bounds, wrong types)

**Test Quality:**
- 100% pass rate
- Integration tests verify Rust-Java communication
- Synchronization tests verify CRDT properties
- Resource management tests prevent leaks
- Error tests ensure proper exception handling

## Code Quality

### Rust Quality Tools

**Rustfmt:**
- Standard Rust formatting
- Enforced in CI/CD
- Run with `cargo fmt`

**Clippy:**
- Rust linter
- Zero warnings policy
- Run with `cargo clippy -- -D warnings`

### Java Quality Tools

**Checkstyle:**
- Google Java Style Guide
- Configured in `build.gradle`
- Enforced in CI/CD

**Quality Rules:**
- Maximum method length: 150 lines
- Proper JavaDoc on all public APIs
- Consistent naming conventions

## File Structure

```
y-crdt-jni/                                   # Root project
├── build.gradle                              # Root Gradle configuration
├── settings.gradle                           # Module declarations
├── gradlew / gradlew.bat                     # Gradle wrapper scripts
├── .gitignore                                # Git ignore rules
├── README.md                                 # User documentation
├── PLAN.md                                   # Project roadmap
├── CHANGELOG.md                              # Version history
├── IMPLEMENTATION.md                         # This file
├── .github/
│   └── workflows/
│       ├── quickcheck.yml                    # Quick check workflow
│       ├── ci.yml                            # Main CI workflow
│       ├── release.yml                       # Release workflow
│       └── javadoc.yml                       # Javadoc publishing
├── ycrdt/                                    # Core Y-CRDT module
│   ├── build.gradle                          # ycrdt module configuration
│   ├── Cargo.toml                            # Rust configuration
│   ├── Cargo.lock                            # Rust dependencies lock
│   ├── src/
│   │   ├── lib.rs                            # Rust library entry point
│   │   ├── ydoc.rs                           # YDoc JNI bindings
│   │   ├── ytext.rs                          # YText JNI bindings
│   │   ├── yarray.rs                         # YArray JNI bindings
│   │   ├── ymap.rs                           # YMap JNI bindings
│   │   ├── yxmltext.rs                       # YXmlText JNI bindings
│   │   ├── yxmlelement.rs                    # YXmlElement JNI bindings
│   │   ├── yxmlfragment.rs                   # YXmlFragment JNI bindings
│   │   ├── main/java/net/carcdr/ycrdt/
│   │   │   ├── YDoc.java                     # YDoc Java wrapper
│   │   │   ├── YText.java                    # YText Java wrapper
│   │   │   ├── YArray.java                   # YArray Java wrapper
│   │   │   ├── YMap.java                     # YMap Java wrapper
│   │   │   ├── YXmlText.java                 # YXmlText Java wrapper
│   │   │   ├── YXmlElement.java              # YXmlElement Java wrapper
│   │   │   ├── YXmlFragment.java             # YXmlFragment Java wrapper
│   │   │   ├── YXmlNode.java                 # YXmlNode interface
│   │   │   ├── NativeLoader.java            # Native library loader
│   │   │   └── Example.java                 # Example usage program
│   │   └── test/java/net/carcdr/ycrdt/
│   │       ├── YDocTest.java                 # YDoc test suite
│   │       ├── YTextTest.java                # YText test suite
│   │       ├── YArrayTest.java               # YArray test suite
│   │       ├── YMapTest.java                 # YMap test suite
│   │       ├── YXmlTextTest.java             # YXmlText test suite
│   │       ├── YXmlElementTest.java          # YXmlElement test suite
│   │       ├── YXmlFragmentTest.java         # YXmlFragment test suite
│   │       ├── StressTest.java               # Memory stress tests
│   │       └── SubdocumentTest.java          # Subdocument tests
│   ├── target/                               # Rust build artifacts (gitignored)
│   │   ├── debug/
│   │   │   └── libycrdt_jni.{so,dylib,dll}  # Debug native library
│   │   └── release/
│   │       └── libycrdt_jni.{so,dylib,dll}  # Optimized native library
│   └── build/                                # Gradle build artifacts (gitignored)
│       └── libs/
│           ├── ycrdt.jar                     # Main JAR
│           ├── ycrdt-sources.jar             # Sources JAR
│           └── ycrdt-javadoc.jar             # Javadoc JAR
└── yprosemirror/                             # ProseMirror module (future)
    ├── build.gradle                          # yprosemirror module configuration
    ├── src/
    │   ├── main/java/net/carcdr/yprosemirror/
    │   │   └── (future implementation)
    │   └── test/java/net/carcdr/yprosemirror/
    │       └── (future tests)
    └── build/                                # Gradle build artifacts (gitignored)
        └── libs/
            ├── yprosemirror.jar              # Main JAR
            ├── yprosemirror-sources.jar      # Sources JAR
            └── yprosemirror-javadoc.jar      # Javadoc JAR
```

## Performance Considerations

### JNI Overhead

**Minimization Strategies:**
- Batch operations where possible
- Avoid excessive string conversions
- Use transactions for multiple operations
- Cache native pointers in Java objects

**Cost Breakdown:**
- JNI call overhead: ~10-50ns per call
- String conversion: ~100-500ns depending on length
- Object allocation: ~50-100ns
- Transaction overhead: ~1-10µs

### Memory Efficiency

**Rust Side:**
- Zero-copy where possible
- Efficient state encoding (binary format)
- Shared references for Y types (Rc/Arc)

**Java Side:**
- Minimal wrapper overhead (single pointer field)
- No data duplication (native holds data)
- Lazy object creation (on-demand)

## Security Considerations

### Memory Safety

**Rust Guarantees:**
- No buffer overflows (bounds checked)
- No use-after-free (Box ownership)
- No null pointer dereferences (validation)
- Thread-safe by default

**JNI Safety:**
- Pointer validation on every native call
- No raw pointer exposure to Java
- Exception-based error propagation
- No panics across JNI boundary

### Input Validation

**Rust Layer:**
- All pointers checked for zero
- String conversions error-handled
- Array bounds validated

**Java Layer:**
- Null parameter checking
- State validation (closed check)
- Range validation (index bounds)
- Type validation where applicable

## Platform Support

### Tested Platforms

- **macOS** (Darwin)
  - x86_64 (Intel)
  - aarch64 (Apple Silicon)

### Supported Platforms (via CI)

- **Linux** (Ubuntu)
  - x86_64

- **macOS**
  - x86_64
  - aarch64

- **Windows**
  - x86_64

### Platform-Specific Concerns

**macOS:**
- Uses `.dylib` extension
- System Integrity Protection may require code signing

**Linux:**
- Uses `.so` extension
- glibc version compatibility

**Windows:**
- Uses `.dll` extension
- Microsoft Visual C++ Runtime required

## Future Extensibility

### Adding New Types

To add a new Y-CRDT type (e.g., YXmlText):

1. Create `src/yxmltext.rs` with native methods
2. Add `mod yxmltext;` and `pub use yxmltext::*;` to `lib.rs`
3. Create `YXmlText.java` with Closeable pattern
4. Add `getXmlText(String)` method to `YDoc.java`
5. Create `YXmlTextTest.java` with comprehensive tests
6. Add examples to `Example.java`

### Adding Features

**Observers/Callbacks:**
- Requires JNI global references
- Callback registration system
- Event dispatch mechanism

**Transactions:**
- Transaction handle management
- Begin/commit/rollback API
- Nested transaction support

**Additional Types:**
- Boolean, integer support in arrays
- Nested type support (arrays of arrays)
- Custom type serialization

## Known Limitations

1. **Single-threaded Access:** Core operations require external synchronization (observer callbacks are thread-safe)
2. **Limited Type Support for Arrays/Maps:** Only strings, doubles, and subdocuments supported (arrays and maps)
3. **No Transaction API:** Direct manipulation only
4. **Platform Builds:** Cross-compilation scripts not yet automated
5. **No Maven Publishing:** Library not in public repositories
6. **No State Vector API:** Only full state synchronization available (inefficient for large documents)
