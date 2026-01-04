package net.carcdr.ycrdt.panama.ffi;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Panama FFM bindings for the yffi (Y-CRDT C FFI) library.
 *
 * <p>This class provides low-level access to the native yffi functions using
 * Java's Foreign Function and Memory API (Project Panama).</p>
 */
public final class Yrs {

    private Yrs() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = NativeLoader.getSymbolLookup();

    // =========================================================================
    // YDoc Functions
    // =========================================================================

    // YDoc *ydoc_new(void)
    private static final MethodHandle YDOC_NEW = LINKER.downcallHandle(
        LOOKUP.find("ydoc_new").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS)
    );

    /**
     * Creates a new Y-CRDT document.
     *
     * @return pointer to the new document
     */
    public static MemorySegment ydocNew() {
        try {
            return (MemorySegment) YDOC_NEW.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ydoc_new", t);
        }
    }

    // void ydoc_destroy(YDoc *value)
    private static final MethodHandle YDOC_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("ydoc_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Destroys a Y-CRDT document.
     *
     * @param doc pointer to the document
     */
    public static void ydocDestroy(MemorySegment doc) {
        try {
            YDOC_DESTROY.invokeExact(doc);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ydoc_destroy", t);
        }
    }

    // uint64_t ydoc_id(YDoc *doc)
    private static final MethodHandle YDOC_ID = LINKER.downcallHandle(
        LOOKUP.find("ydoc_id").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    );

    /**
     * Gets the client ID of a document.
     *
     * @param doc pointer to the document
     * @return the client ID
     */
    public static long ydocId(MemorySegment doc) {
        try {
            return (long) YDOC_ID.invokeExact(doc);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ydoc_id", t);
        }
    }

    // char *ydoc_guid(YDoc *doc)
    private static final MethodHandle YDOC_GUID = LINKER.downcallHandle(
        LOOKUP.find("ydoc_guid").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the GUID of a document.
     *
     * @param doc pointer to the document
     * @return pointer to the GUID string (must be freed with ystringDestroy)
     */
    public static MemorySegment ydocGuid(MemorySegment doc) {
        try {
            return (MemorySegment) YDOC_GUID.invokeExact(doc);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ydoc_guid", t);
        }
    }

    // =========================================================================
    // Transaction Functions
    // =========================================================================

    // YTransaction *ydoc_read_transaction(YDoc *doc)
    private static final MethodHandle YDOC_READ_TRANSACTION = LINKER.downcallHandle(
        LOOKUP.find("ydoc_read_transaction").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Creates a read transaction on a document.
     *
     * @param doc pointer to the document
     * @return pointer to the transaction
     */
    public static MemorySegment ydocReadTransaction(MemorySegment doc) {
        try {
            return (MemorySegment) YDOC_READ_TRANSACTION.invokeExact(doc);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ydoc_read_transaction", t);
        }
    }

    // YTransaction *ydoc_write_transaction(YDoc *doc, uint32_t origin_len, const char *origin)
    private static final MethodHandle YDOC_WRITE_TRANSACTION = LINKER.downcallHandle(
        LOOKUP.find("ydoc_write_transaction").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Creates a write transaction on a document.
     *
     * @param doc pointer to the document
     * @param originLen length of the origin string
     * @param origin pointer to the origin string (can be NULL)
     * @return pointer to the transaction
     */
    public static MemorySegment ydocWriteTransaction(
            MemorySegment doc, int originLen, MemorySegment origin) {
        try {
            return (MemorySegment) YDOC_WRITE_TRANSACTION.invokeExact(doc, originLen, origin);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ydoc_write_transaction", t);
        }
    }

    // void ytransaction_commit(YTransaction *txn)
    private static final MethodHandle YTRANSACTION_COMMIT = LINKER.downcallHandle(
        LOOKUP.find("ytransaction_commit").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Commits and frees a transaction.
     *
     * @param txn pointer to the transaction
     */
    public static void ytransactionCommit(MemorySegment txn) {
        try {
            YTRANSACTION_COMMIT.invokeExact(txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytransaction_commit", t);
        }
    }

    // char *ytransaction_state_vector_v1(const YTransaction *txn, uint32_t *len)
    private static final MethodHandle YTRANSACTION_STATE_VECTOR_V1 = LINKER.downcallHandle(
        LOOKUP.find("ytransaction_state_vector_v1").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the state vector from a transaction.
     *
     * @param txn pointer to the transaction
     * @param len pointer to store the length
     * @return pointer to the state vector (must be freed with ybinaryDestroy)
     */
    public static MemorySegment ytransactionStateVectorV1(MemorySegment txn, MemorySegment len) {
        try {
            return (MemorySegment) YTRANSACTION_STATE_VECTOR_V1.invokeExact(txn, len);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytransaction_state_vector_v1", t);
        }
    }

    // char *ytransaction_state_diff_v1(const YTransaction *txn, const char *sv, uint32_t sv_len,
    //                                  uint32_t *len)
    private static final MethodHandle YTRANSACTION_STATE_DIFF_V1 = LINKER.downcallHandle(
        LOOKUP.find("ytransaction_state_diff_v1").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Gets the state diff from a transaction.
     *
     * @param txn pointer to the transaction
     * @param sv pointer to the state vector
     * @param svLen length of the state vector
     * @param len pointer to store the result length
     * @return pointer to the diff (must be freed with ybinaryDestroy)
     */
    public static MemorySegment ytransactionStateDiffV1(
            MemorySegment txn, MemorySegment sv, int svLen, MemorySegment len) {
        try {
            return (MemorySegment) YTRANSACTION_STATE_DIFF_V1.invokeExact(txn, sv, svLen, len);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytransaction_state_diff_v1", t);
        }
    }

    // uint8_t ytransaction_apply(YTransaction *txn, const char *diff, uint32_t diff_len)
    private static final MethodHandle YTRANSACTION_APPLY = LINKER.downcallHandle(
        LOOKUP.find("ytransaction_apply").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_BYTE,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Applies an update diff to a transaction's document.
     *
     * @param txn pointer to the transaction
     * @param diff pointer to the update diff
     * @param diffLen length of the diff
     * @return 0 on success, error code on failure
     */
    public static byte ytransactionApply(MemorySegment txn, MemorySegment diff, int diffLen) {
        try {
            return (byte) YTRANSACTION_APPLY.invokeExact(txn, diff, diffLen);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytransaction_apply", t);
        }
    }

    // =========================================================================
    // YText Functions
    // =========================================================================

    // Branch *ytext(YDoc *doc, const char *name)
    private static final MethodHandle YTEXT = LINKER.downcallHandle(
        LOOKUP.find("ytext").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets or creates a YText with the given name.
     *
     * @param doc pointer to the document
     * @param name pointer to the name string
     * @return pointer to the text branch
     */
    public static MemorySegment ytext(MemorySegment doc, MemorySegment name) {
        try {
            return (MemorySegment) YTEXT.invokeExact(doc, name);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytext", t);
        }
    }

    // uint32_t ytext_len(const Branch *txt, const YTransaction *txn)
    private static final MethodHandle YTEXT_LEN = LINKER.downcallHandle(
        LOOKUP.find("ytext_len").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the length of a YText.
     *
     * @param txt pointer to the text branch
     * @param txn pointer to the transaction
     * @return the length
     */
    public static int ytextLen(MemorySegment txt, MemorySegment txn) {
        try {
            return (int) YTEXT_LEN.invokeExact(txt, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytext_len", t);
        }
    }

    // char *ytext_string(const Branch *txt, const YTransaction *txn)
    private static final MethodHandle YTEXT_STRING = LINKER.downcallHandle(
        LOOKUP.find("ytext_string").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the string content of a YText.
     *
     * @param txt pointer to the text branch
     * @param txn pointer to the transaction
     * @return pointer to the string (must be freed with ystringDestroy)
     */
    public static MemorySegment ytextString(MemorySegment txt, MemorySegment txn) {
        try {
            return (MemorySegment) YTEXT_STRING.invokeExact(txt, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytext_string", t);
        }
    }

    // void ytext_insert(const Branch *txt, YTransaction *txn, uint32_t index,
    //                   const char *value, const struct YInput *attrs)
    private static final MethodHandle YTEXT_INSERT = LINKER.downcallHandle(
        LOOKUP.find("ytext_insert").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Inserts text at the given index.
     *
     * @param txt pointer to the text branch
     * @param txn pointer to the transaction
     * @param index the index to insert at
     * @param value pointer to the value string
     * @param attrs pointer to the attributes (can be NULL)
     */
    public static void ytextInsert(
            MemorySegment txt, MemorySegment txn, int index,
            MemorySegment value, MemorySegment attrs) {
        try {
            YTEXT_INSERT.invokeExact(txt, txn, index, value, attrs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytext_insert", t);
        }
    }

    // void ytext_remove_range(const Branch *txt, YTransaction *txn, uint32_t index, uint32_t len)
    private static final MethodHandle YTEXT_REMOVE_RANGE = LINKER.downcallHandle(
        LOOKUP.find("ytext_remove_range").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Removes text from the given range.
     *
     * @param txt pointer to the text branch
     * @param txn pointer to the transaction
     * @param index the start index
     * @param len the length to remove
     */
    public static void ytextRemoveRange(
            MemorySegment txt, MemorySegment txn, int index, int len) {
        try {
            YTEXT_REMOVE_RANGE.invokeExact(txt, txn, index, len);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytext_remove_range", t);
        }
    }

    // =========================================================================
    // YArray Functions
    // =========================================================================

    // Branch *yarray(YDoc *doc, const char *name)
    private static final MethodHandle YARRAY = LINKER.downcallHandle(
        LOOKUP.find("yarray").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets or creates a YArray with the given name.
     *
     * @param doc pointer to the document
     * @param name pointer to the name string
     * @return pointer to the array branch
     */
    public static MemorySegment yarray(MemorySegment doc, MemorySegment name) {
        try {
            return (MemorySegment) YARRAY.invokeExact(doc, name);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yarray", t);
        }
    }

    // uint32_t yarray_len(const Branch *array)
    private static final MethodHandle YARRAY_LEN = LINKER.downcallHandle(
        LOOKUP.find("yarray_len").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    );

    /**
     * Gets the length of a YArray.
     *
     * @param array pointer to the array branch
     * @return the length
     */
    public static int yarrayLen(MemorySegment array) {
        try {
            return (int) YARRAY_LEN.invokeExact(array);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yarray_len", t);
        }
    }

    // void yarray_insert_range(const Branch *array, YTransaction *txn, uint32_t index,
    //                          const struct YInput *values, uint32_t values_len)
    private static final MethodHandle YARRAY_INSERT_RANGE = LINKER.downcallHandle(
        LOOKUP.find("yarray_insert_range").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Inserts values into an array at the given index.
     *
     * @param array pointer to the array branch
     * @param txn pointer to the transaction
     * @param index the index to insert at
     * @param values pointer to the values array
     * @param valuesLen the number of values
     */
    public static void yarrayInsertRange(
            MemorySegment array, MemorySegment txn, int index,
            MemorySegment values, int valuesLen) {
        try {
            YARRAY_INSERT_RANGE.invokeExact(array, txn, index, values, valuesLen);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yarray_insert_range", t);
        }
    }

    // void yarray_remove_range(const Branch *array, YTransaction *txn, uint32_t index, uint32_t len)
    private static final MethodHandle YARRAY_REMOVE_RANGE = LINKER.downcallHandle(
        LOOKUP.find("yarray_remove_range").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Removes values from an array at the given range.
     *
     * @param array pointer to the array branch
     * @param txn pointer to the transaction
     * @param index the start index
     * @param len the number of values to remove
     */
    public static void yarrayRemoveRange(
            MemorySegment array, MemorySegment txn, int index, int len) {
        try {
            YARRAY_REMOVE_RANGE.invokeExact(array, txn, index, len);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yarray_remove_range", t);
        }
    }

    // =========================================================================
    // YMap Functions
    // =========================================================================

    // Branch *ymap(YDoc *doc, const char *name)
    private static final MethodHandle YMAP = LINKER.downcallHandle(
        LOOKUP.find("ymap").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets or creates a YMap with the given name.
     *
     * @param doc pointer to the document
     * @param name pointer to the name string
     * @return pointer to the map branch
     */
    public static MemorySegment ymap(MemorySegment doc, MemorySegment name) {
        try {
            return (MemorySegment) YMAP.invokeExact(doc, name);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap", t);
        }
    }

    // uint32_t ymap_len(const Branch *map, const YTransaction *txn)
    private static final MethodHandle YMAP_LEN = LINKER.downcallHandle(
        LOOKUP.find("ymap_len").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the number of entries in a YMap.
     *
     * @param map pointer to the map branch
     * @param txn pointer to the transaction
     * @return the number of entries
     */
    public static int ymapLen(MemorySegment map, MemorySegment txn) {
        try {
            return (int) YMAP_LEN.invokeExact(map, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_len", t);
        }
    }

    // void ymap_insert(const Branch *map, YTransaction *txn, const char *key,
    //                  const struct YInput *value)
    private static final MethodHandle YMAP_INSERT = LINKER.downcallHandle(
        LOOKUP.find("ymap_insert").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Inserts a value into a YMap.
     *
     * @param map pointer to the map branch
     * @param txn pointer to the transaction
     * @param key pointer to the key string
     * @param value pointer to the YInput value
     */
    public static void ymapInsert(
            MemorySegment map, MemorySegment txn, MemorySegment key, MemorySegment value) {
        try {
            YMAP_INSERT.invokeExact(map, txn, key, value);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_insert", t);
        }
    }

    // void ymap_remove(const Branch *map, YTransaction *txn, const char *key)
    private static final MethodHandle YMAP_REMOVE = LINKER.downcallHandle(
        LOOKUP.find("ymap_remove").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Removes a value from a YMap.
     *
     * @param map pointer to the map branch
     * @param txn pointer to the transaction
     * @param key pointer to the key string
     */
    public static void ymapRemove(MemorySegment map, MemorySegment txn, MemorySegment key) {
        try {
            YMAP_REMOVE.invokeExact(map, txn, key);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_remove", t);
        }
    }

    // =========================================================================
    // YXmlFragment Functions
    // =========================================================================

    // Branch *yxmlfragment(YDoc *doc, const char *name)
    private static final MethodHandle YXMLFRAGMENT = LINKER.downcallHandle(
        LOOKUP.find("yxmlfragment").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets or creates a YXmlFragment with the given name.
     *
     * @param doc pointer to the document
     * @param name pointer to the name string
     * @return pointer to the xml fragment branch
     */
    public static MemorySegment yxmlfragment(MemorySegment doc, MemorySegment name) {
        try {
            return (MemorySegment) YXMLFRAGMENT.invokeExact(doc, name);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlfragment", t);
        }
    }

    // =========================================================================
    // Memory Management Functions
    // =========================================================================

    // void ystring_destroy(char *str)
    private static final MethodHandle YSTRING_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("ystring_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Frees a string allocated by yffi.
     *
     * @param str pointer to the string
     */
    public static void ystringDestroy(MemorySegment str) {
        try {
            YSTRING_DESTROY.invokeExact(str);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ystring_destroy", t);
        }
    }

    // void ybinary_destroy(char *ptr, uint32_t len)
    private static final MethodHandle YBINARY_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("ybinary_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    );

    /**
     * Frees a binary buffer allocated by yffi.
     *
     * @param ptr pointer to the buffer
     * @param len length of the buffer
     */
    public static void ybinaryDestroy(MemorySegment ptr, int len) {
        try {
            YBINARY_DESTROY.invokeExact(ptr, len);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ybinary_destroy", t);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Creates a native string from a Java string in the given arena.
     *
     * @param arena the arena to allocate in
     * @param str the Java string
     * @return pointer to the native string
     */
    public static MemorySegment createString(Arena arena, String str) {
        return arena.allocateFrom(str);
    }

    /**
     * Reads a native string and frees it.
     *
     * @param ptr pointer to the native string
     * @return the Java string
     */
    public static String readAndFreeString(MemorySegment ptr) {
        if (ptr.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment reinterpreted = ptr.reinterpret(Long.MAX_VALUE);
        String result = reinterpreted.getString(0);
        ystringDestroy(ptr);
        return result;
    }

    /**
     * Reads a native binary buffer and frees it.
     *
     * @param ptr pointer to the buffer
     * @param len length of the buffer
     * @return the byte array
     */
    public static byte[] readAndFreeBinary(MemorySegment ptr, int len) {
        if (ptr.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment reinterpreted = ptr.reinterpret(len);
        byte[] result = reinterpreted.toArray(ValueLayout.JAVA_BYTE);
        ybinaryDestroy(ptr, len);
        return result;
    }

    /**
     * Copies a byte array to native memory in the given arena.
     *
     * @param arena the arena to allocate in
     * @param data the byte array
     * @return pointer to the native buffer
     */
    public static MemorySegment createBinary(Arena arena, byte[] data) {
        MemorySegment segment = arena.allocate(data.length);
        segment.copyFrom(MemorySegment.ofArray(data));
        return segment;
    }

    // =========================================================================
    // YInput Functions (for creating values to insert into arrays/maps)
    // =========================================================================

    // YInput tag constants from libyrs.h
    private static final byte Y_JSON_NUM = -7;  // double
    private static final byte Y_JSON_INT = -6;  // int64
    private static final byte Y_JSON_STR = -5;  // string
    private static final byte Y_DOC = 7;        // subdocument

    /**
     * Layout for the YInput struct.
     * The struct is: tag (1) + padding (3) + len (4) + union value (8 for ptr/double/long).
     *
     * <p>Note: The actual C struct is 24 bytes (union is 16 bytes for YMapInputData),
     * but for simple values (string, double, long) only the first 8 bytes of the union
     * are used. We construct these structs manually to avoid ARM64 ABI issues with
     * struct-by-value returns > 16 bytes.</p>
     */
    public static final StructLayout YINPUT_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("tag"),
        MemoryLayout.paddingLayout(3),
        ValueLayout.JAVA_INT.withName("len"),
        ValueLayout.ADDRESS.withName("value")
    );

    /**
     * Layout for YInput with double value in the union.
     */
    private static final StructLayout YINPUT_FLOAT_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("tag"),
        MemoryLayout.paddingLayout(3),
        ValueLayout.JAVA_INT.withName("len"),
        ValueLayout.JAVA_DOUBLE.withName("value")
    );

    /**
     * Layout for YInput with long value in the union.
     */
    private static final StructLayout YINPUT_LONG_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("tag"),
        MemoryLayout.paddingLayout(3),
        ValueLayout.JAVA_INT.withName("len"),
        ValueLayout.JAVA_LONG.withName("value")
    );

    // VarHandles for accessing YInput struct fields
    private static final java.lang.invoke.VarHandle YINPUT_TAG =
        YINPUT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tag"));
    private static final java.lang.invoke.VarHandle YINPUT_LEN =
        YINPUT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("len"));
    private static final java.lang.invoke.VarHandle YINPUT_VALUE_PTR =
        YINPUT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("value"));
    private static final java.lang.invoke.VarHandle YINPUT_VALUE_DOUBLE =
        YINPUT_FLOAT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("value"));
    private static final java.lang.invoke.VarHandle YINPUT_VALUE_LONG =
        YINPUT_LONG_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("value"));

    /**
     * Creates a YInput containing a string value.
     *
     * <p>This constructs the YInput struct manually in Java to avoid ARM64 ABI issues
     * with the native yinput_string function, which returns a 24-byte struct via sret.</p>
     *
     * @param arena the arena to allocate the struct in
     * @param str pointer to the null-terminated string
     * @return the YInput struct as a MemorySegment
     */
    public static MemorySegment yinputString(Arena arena, MemorySegment str) {
        MemorySegment yinput = arena.allocate(YINPUT_LAYOUT);
        YINPUT_TAG.set(yinput, 0L, Y_JSON_STR);
        YINPUT_LEN.set(yinput, 0L, 1);
        YINPUT_VALUE_PTR.set(yinput, 0L, str);
        return yinput;
    }

    /**
     * Creates a YInput containing a double value.
     *
     * <p>This constructs the YInput struct manually in Java to avoid ARM64 ABI issues
     * with the native yinput_float function.</p>
     *
     * @param arena the arena to allocate the struct in
     * @param num the double value
     * @return the YInput struct as a MemorySegment
     */
    public static MemorySegment yinputFloat(Arena arena, double num) {
        MemorySegment yinput = arena.allocate(YINPUT_FLOAT_LAYOUT);
        YINPUT_TAG.set(yinput, 0L, Y_JSON_NUM);
        YINPUT_LEN.set(yinput, 0L, 1);
        YINPUT_VALUE_DOUBLE.set(yinput, 0L, num);
        return yinput;
    }

    /**
     * Creates a YInput containing a long value.
     *
     * <p>This constructs the YInput struct manually in Java to avoid ARM64 ABI issues
     * with the native yinput_long function.</p>
     *
     * @param arena the arena to allocate the struct in
     * @param num the long value
     * @return the YInput struct as a MemorySegment
     */
    public static MemorySegment yinputLong(Arena arena, long num) {
        MemorySegment yinput = arena.allocate(YINPUT_LONG_LAYOUT);
        YINPUT_TAG.set(yinput, 0L, Y_JSON_INT);
        YINPUT_LEN.set(yinput, 0L, 1);
        YINPUT_VALUE_LONG.set(yinput, 0L, num);
        return yinput;
    }

    /**
     * Creates a YInput containing a subdocument.
     *
     * <p>This constructs the YInput struct manually in Java to avoid ARM64 ABI issues
     * with the native yinput_ydoc function.</p>
     *
     * @param arena the arena to allocate the struct in
     * @param docPtr pointer to the YDoc
     * @return the YInput struct as a MemorySegment
     */
    public static MemorySegment yinputYdoc(Arena arena, MemorySegment docPtr) {
        MemorySegment yinput = arena.allocate(YINPUT_LAYOUT);
        YINPUT_TAG.set(yinput, 0L, Y_DOC);
        YINPUT_LEN.set(yinput, 0L, 1);
        YINPUT_VALUE_PTR.set(yinput, 0L, docPtr);
        return yinput;
    }

    // =========================================================================
    // YOutput Functions (for reading values from arrays/maps)
    // =========================================================================

    // struct YOutput *yarray_get(const Branch *array, const YTransaction *txn, uint32_t index)
    private static final MethodHandle YARRAY_GET = LINKER.downcallHandle(
        LOOKUP.find("yarray_get").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Gets a value from an array at the given index.
     *
     * @param array pointer to the array branch
     * @param txn pointer to the transaction
     * @param index the index
     * @return pointer to the YOutput (must be freed with youtputDestroy)
     */
    public static MemorySegment yarrayGet(MemorySegment array, MemorySegment txn, int index) {
        try {
            return (MemorySegment) YARRAY_GET.invokeExact(array, txn, index);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yarray_get", t);
        }
    }

    // void youtput_destroy(struct YOutput *val)
    private static final MethodHandle YOUTPUT_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("youtput_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Frees a YOutput value.
     *
     * @param val pointer to the YOutput
     */
    public static void youtputDestroy(MemorySegment val) {
        try {
            YOUTPUT_DESTROY.invokeExact(val);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call youtput_destroy", t);
        }
    }

    // char *youtput_read_string(const struct YOutput *val)
    private static final MethodHandle YOUTPUT_READ_STRING = LINKER.downcallHandle(
        LOOKUP.find("youtput_read_string").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Reads a string from a YOutput value.
     *
     * @param val pointer to the YOutput
     * @return pointer to the string (must be freed with ystringDestroy), or null if not a string
     */
    public static MemorySegment youtputReadString(MemorySegment val) {
        try {
            return (MemorySegment) YOUTPUT_READ_STRING.invokeExact(val);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call youtput_read_string", t);
        }
    }

    // const double *youtput_read_float(const struct YOutput *val)
    private static final MethodHandle YOUTPUT_READ_FLOAT = LINKER.downcallHandle(
        LOOKUP.find("youtput_read_float").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Reads a double from a YOutput value.
     *
     * @param val pointer to the YOutput
     * @return pointer to the double, or null if not a double
     */
    public static MemorySegment youtputReadFloat(MemorySegment val) {
        try {
            return (MemorySegment) YOUTPUT_READ_FLOAT.invokeExact(val);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call youtput_read_float", t);
        }
    }

    // const int64_t *youtput_read_long(const struct YOutput *val)
    private static final MethodHandle YOUTPUT_READ_LONG = LINKER.downcallHandle(
        LOOKUP.find("youtput_read_long").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Reads a long from a YOutput value.
     *
     * @param val pointer to the YOutput
     * @return pointer to the long, or null if not a long
     */
    public static MemorySegment youtputReadLong(MemorySegment val) {
        try {
            return (MemorySegment) YOUTPUT_READ_LONG.invokeExact(val);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call youtput_read_long", t);
        }
    }

    // YDoc *youtput_read_ydoc(const struct YOutput *val)
    private static final MethodHandle YOUTPUT_READ_YDOC = LINKER.downcallHandle(
        LOOKUP.find("youtput_read_ydoc").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Reads a subdocument from a YOutput value.
     *
     * @param val pointer to the YOutput
     * @return pointer to the YDoc, or null if not a subdocument
     */
    public static MemorySegment youtputReadYdoc(MemorySegment val) {
        try {
            return (MemorySegment) YOUTPUT_READ_YDOC.invokeExact(val);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call youtput_read_ydoc", t);
        }
    }

    // =========================================================================
    // YMap Iterator Functions
    // =========================================================================

    // YMapIter *ymap_iter(const Branch *map, const YTransaction *txn)
    private static final MethodHandle YMAP_ITER = LINKER.downcallHandle(
        LOOKUP.find("ymap_iter").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Creates an iterator over map entries.
     *
     * @param map pointer to the map branch
     * @param txn pointer to the transaction
     * @return pointer to the iterator
     */
    public static MemorySegment ymapIter(MemorySegment map, MemorySegment txn) {
        try {
            return (MemorySegment) YMAP_ITER.invokeExact(map, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_iter", t);
        }
    }

    // void ymap_iter_destroy(YMapIter *iter)
    private static final MethodHandle YMAP_ITER_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("ymap_iter_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Destroys a map iterator.
     *
     * @param iter pointer to the iterator
     */
    public static void ymapIterDestroy(MemorySegment iter) {
        try {
            YMAP_ITER_DESTROY.invokeExact(iter);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_iter_destroy", t);
        }
    }

    // struct YMapEntry *ymap_iter_next(YMapIter *iter)
    private static final MethodHandle YMAP_ITER_NEXT = LINKER.downcallHandle(
        LOOKUP.find("ymap_iter_next").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the next entry from the iterator.
     *
     * @param iter pointer to the iterator
     * @return pointer to the entry, or null if no more entries
     */
    public static MemorySegment ymapIterNext(MemorySegment iter) {
        try {
            return (MemorySegment) YMAP_ITER_NEXT.invokeExact(iter);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_iter_next", t);
        }
    }

    // void ymap_entry_destroy(struct YMapEntry *value)
    private static final MethodHandle YMAP_ENTRY_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("ymap_entry_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Destroys a map entry.
     *
     * @param entry pointer to the entry
     */
    public static void ymapEntryDestroy(MemorySegment entry) {
        try {
            YMAP_ENTRY_DESTROY.invokeExact(entry);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_entry_destroy", t);
        }
    }

    /**
     * Layout for YMapEntry struct.
     * The struct is: const char *key (8 bytes) + const struct YOutput *value (8 bytes)
     */
    public static final StructLayout YMAP_ENTRY_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("key"),
        ValueLayout.ADDRESS.withName("value")
    );

    private static final java.lang.invoke.VarHandle YMAP_ENTRY_KEY =
        YMAP_ENTRY_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("key"));

    /**
     * Reads the key from a YMapEntry.
     *
     * @param entry pointer to the entry
     * @return the key string
     */
    public static String ymapEntryReadKey(MemorySegment entry) {
        MemorySegment reinterpreted = entry.reinterpret(YMAP_ENTRY_LAYOUT.byteSize());
        MemorySegment keyPtr = (MemorySegment) YMAP_ENTRY_KEY.get(reinterpreted, 0L);
        if (keyPtr.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment keyReinterpreted = keyPtr.reinterpret(Long.MAX_VALUE);
        return keyReinterpreted.getString(0);
    }

    // struct YOutput *ymap_get(const Branch *map, const YTransaction *txn, const char *key)
    private static final MethodHandle YMAP_GET = LINKER.downcallHandle(
        LOOKUP.find("ymap_get").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Gets a value from a map by key.
     *
     * @param map pointer to the map branch
     * @param txn pointer to the transaction
     * @param key pointer to the key string
     * @return pointer to the YOutput (must be freed with youtputDestroy), or null if not found
     */
    public static MemorySegment ymapGet(MemorySegment map, MemorySegment txn, MemorySegment key) {
        try {
            return (MemorySegment) YMAP_GET.invokeExact(map, txn, key);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ymap_get", t);
        }
    }

    // =========================================================================
    // Branch Functions
    // =========================================================================

    // Type kind constants from libyrs.h
    /** YOutput tag for YXmlElement. */
    public static final int Y_XML_ELEM = 4;
    /** YOutput tag for YXmlText. */
    public static final int Y_XML_TEXT = 5;
    /** YOutput tag for YXmlFragment. */
    public static final int Y_XML_FRAG = 6;

    // int8_t ytype_kind(const Branch *branch)
    private static final MethodHandle YTYPE_KIND = LINKER.downcallHandle(
        LOOKUP.find("ytype_kind").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.ADDRESS)
    );

    /**
     * Gets the type kind of a branch.
     *
     * @param branch pointer to the branch
     * @return the type kind (Y_XML_ELEM, Y_XML_TEXT, Y_XML_FRAG, etc.)
     */
    public static int ytypeKind(MemorySegment branch) {
        try {
            return (byte) YTYPE_KIND.invokeExact(branch);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ytype_kind", t);
        }
    }

    // Branch *yxmlelem_parent(const Branch *xml)
    private static final MethodHandle YXMLELEM_PARENT = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_parent").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the parent of an XML element or text node.
     *
     * @param xml pointer to the xml branch
     * @return pointer to the parent branch, or null if root-level
     */
    public static MemorySegment yxmlelemParent(MemorySegment xml) {
        try {
            return (MemorySegment) YXMLELEM_PARENT.invokeExact(xml);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_parent", t);
        }
    }

    // char *ybranch_json(Branch *branch, YTransaction *txn)
    private static final MethodHandle YBRANCH_JSON = LINKER.downcallHandle(
        LOOKUP.find("ybranch_json").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the JSON representation of a branch.
     *
     * @param branch pointer to the branch
     * @param txn pointer to the transaction
     * @return pointer to the JSON string (must be freed with ystringDestroy)
     */
    public static MemorySegment ybranchJson(MemorySegment branch, MemorySegment txn) {
        try {
            return (MemorySegment) YBRANCH_JSON.invokeExact(branch, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ybranch_json", t);
        }
    }

    // =========================================================================
    // Observer Functions
    // =========================================================================

    /**
     * FunctionDescriptor for the update observer callback.
     * Signature: void callback(void* state, uint32_t len, const char* data)
     */
    public static final FunctionDescriptor UPDATE_CALLBACK_DESCRIPTOR = FunctionDescriptor.ofVoid(
        ValueLayout.ADDRESS,      // void* state
        ValueLayout.JAVA_INT,     // uint32_t len
        ValueLayout.ADDRESS       // const char* data
    );

    // YSubscription *ydoc_observe_updates_v1(YDoc *doc, void *state,
    //     void (*cb)(void*, uint32_t, const char*))
    private static final MethodHandle YDOC_OBSERVE_UPDATES_V1 = LINKER.downcallHandle(
        LOOKUP.find("ydoc_observe_updates_v1").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,    // YSubscription* return
            ValueLayout.ADDRESS,    // YDoc* doc
            ValueLayout.ADDRESS,    // void* state
            ValueLayout.ADDRESS     // callback function pointer
        )
    );

    /**
     * Registers an observer for document updates (v1 encoding).
     *
     * @param doc pointer to the document
     * @param state opaque state pointer passed to callback
     * @param callback function pointer for the callback (created via Linker.upcallStub)
     * @return pointer to the subscription (must be freed with yunobserve)
     */
    public static MemorySegment ydocObserveUpdatesV1(
            MemorySegment doc, MemorySegment state, MemorySegment callback) {
        try {
            return (MemorySegment) YDOC_OBSERVE_UPDATES_V1.invokeExact(doc, state, callback);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call ydoc_observe_updates_v1", t);
        }
    }

    // void yunobserve(YSubscription *subscription)
    private static final MethodHandle YUNOBSERVE = LINKER.downcallHandle(
        LOOKUP.find("yunobserve").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Unsubscribes an observer.
     *
     * @param subscription pointer to the subscription
     */
    public static void yunobserve(MemorySegment subscription) {
        try {
            YUNOBSERVE.invokeExact(subscription);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yunobserve", t);
        }
    }

    // =========================================================================
    // XmlFragment Functions
    // =========================================================================

    // uint32_t yxmlelem_child_len(const Branch *xml, const YTransaction *txn)
    private static final MethodHandle YXMLELEM_CHILD_LEN = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_child_len").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the number of children in an XML element or fragment.
     *
     * @param xml pointer to the xml branch
     * @param txn pointer to the transaction
     * @return the number of children
     */
    public static int yxmlelemChildLen(MemorySegment xml, MemorySegment txn) {
        try {
            return (int) YXMLELEM_CHILD_LEN.invokeExact(xml, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_child_len", t);
        }
    }

    // =========================================================================
    // YXmlElement Functions
    // =========================================================================

    // char *yxmlelem_tag(const Branch *xml)
    private static final MethodHandle YXMLELEM_TAG = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_tag").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the tag name of an XML element.
     *
     * @param xml pointer to the xml element branch
     * @return pointer to the tag string (must be freed with ystringDestroy)
     */
    public static MemorySegment yxmlelemTag(MemorySegment xml) {
        try {
            return (MemorySegment) YXMLELEM_TAG.invokeExact(xml);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_tag", t);
        }
    }

    // Branch *yxmlelem_insert_elem(const Branch *xml, YTransaction *txn,
    //                               uint32_t index, const char *name)
    private static final MethodHandle YXMLELEM_INSERT_ELEM = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_insert_elem").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Inserts a new XML element as a child at the given index.
     *
     * @param xml pointer to the xml element/fragment branch
     * @param txn pointer to the transaction
     * @param index the index to insert at
     * @param name pointer to the tag name string
     * @return pointer to the new element branch
     */
    public static MemorySegment yxmlelemInsertElem(
            MemorySegment xml, MemorySegment txn, int index, MemorySegment name) {
        try {
            return (MemorySegment) YXMLELEM_INSERT_ELEM.invokeExact(xml, txn, index, name);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_insert_elem", t);
        }
    }

    // Branch *yxmlelem_insert_text(const Branch *xml, YTransaction *txn, uint32_t index)
    private static final MethodHandle YXMLELEM_INSERT_TEXT = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_insert_text").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Inserts a new XML text node as a child at the given index.
     *
     * @param xml pointer to the xml element/fragment branch
     * @param txn pointer to the transaction
     * @param index the index to insert at
     * @return pointer to the new text branch
     */
    public static MemorySegment yxmlelemInsertText(
            MemorySegment xml, MemorySegment txn, int index) {
        try {
            return (MemorySegment) YXMLELEM_INSERT_TEXT.invokeExact(xml, txn, index);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_insert_text", t);
        }
    }

    // void yxmlelem_remove_range(const Branch *xml, YTransaction *txn,
    //                             uint32_t index, uint32_t len)
    private static final MethodHandle YXMLELEM_REMOVE_RANGE = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_remove_range").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Removes children from an XML element/fragment in a range.
     *
     * @param xml pointer to the xml branch
     * @param txn pointer to the transaction
     * @param index the start index
     * @param len the number of children to remove
     */
    public static void yxmlelemRemoveRange(
            MemorySegment xml, MemorySegment txn, int index, int len) {
        try {
            YXMLELEM_REMOVE_RANGE.invokeExact(xml, txn, index, len);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_remove_range", t);
        }
    }

    // struct YOutput *yxmlelem_get(const Branch *xml, const YTransaction *txn, uint32_t index)
    private static final MethodHandle YXMLELEM_GET = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_get").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Gets a child from an XML element/fragment at the given index.
     *
     * @param xml pointer to the xml branch
     * @param txn pointer to the transaction
     * @param index the index
     * @return pointer to YOutput (must be freed with youtputDestroy)
     */
    public static MemorySegment yxmlelemGet(MemorySegment xml, MemorySegment txn, int index) {
        try {
            return (MemorySegment) YXMLELEM_GET.invokeExact(xml, txn, index);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_get", t);
        }
    }

    // char *yxmlelem_string(const Branch *xml, const YTransaction *txn)
    private static final MethodHandle YXMLELEM_STRING = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_string").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the XML string representation of an element/fragment.
     *
     * @param xml pointer to the xml branch
     * @param txn pointer to the transaction
     * @return pointer to the XML string (must be freed with ystringDestroy)
     */
    public static MemorySegment yxmlelemString(MemorySegment xml, MemorySegment txn) {
        try {
            return (MemorySegment) YXMLELEM_STRING.invokeExact(xml, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_string", t);
        }
    }

    // =========================================================================
    // YXmlElement Attribute Functions
    // =========================================================================

    // void yxmlelem_insert_attr(const Branch *xml, YTransaction *txn,
    //                           const char *name, const char *value)
    private static final MethodHandle YXMLELEM_INSERT_ATTR = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_insert_attr").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Sets an attribute on an XML element.
     *
     * @param xml pointer to the xml element branch
     * @param txn pointer to the transaction
     * @param name pointer to the attribute name string
     * @param value pointer to the attribute value string
     */
    public static void yxmlelemInsertAttr(
            MemorySegment xml, MemorySegment txn, MemorySegment name, MemorySegment value) {
        try {
            YXMLELEM_INSERT_ATTR.invokeExact(xml, txn, name, value);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_insert_attr", t);
        }
    }

    // void yxmlelem_remove_attr(const Branch *xml, YTransaction *txn, const char *name)
    private static final MethodHandle YXMLELEM_REMOVE_ATTR = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_remove_attr").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Removes an attribute from an XML element.
     *
     * @param xml pointer to the xml element branch
     * @param txn pointer to the transaction
     * @param name pointer to the attribute name string
     */
    public static void yxmlelemRemoveAttr(
            MemorySegment xml, MemorySegment txn, MemorySegment name) {
        try {
            YXMLELEM_REMOVE_ATTR.invokeExact(xml, txn, name);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_remove_attr", t);
        }
    }

    // char *yxmlelem_get_attr(const Branch *xml, const YTransaction *txn, const char *name)
    private static final MethodHandle YXMLELEM_GET_ATTR = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_get_attr").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Gets an attribute value from an XML element.
     *
     * @param xml pointer to the xml element branch
     * @param txn pointer to the transaction
     * @param name pointer to the attribute name string
     * @return pointer to the value string (must be freed with ystringDestroy), or null
     */
    public static MemorySegment yxmlelemGetAttr(
            MemorySegment xml, MemorySegment txn, MemorySegment name) {
        try {
            return (MemorySegment) YXMLELEM_GET_ATTR.invokeExact(xml, txn, name);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_get_attr", t);
        }
    }

    // YXmlAttrIter *yxmlelem_attr_iter(const Branch *xml, const YTransaction *txn)
    private static final MethodHandle YXMLELEM_ATTR_ITER = LINKER.downcallHandle(
        LOOKUP.find("yxmlelem_attr_iter").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Creates an iterator over XML element attributes.
     *
     * @param xml pointer to the xml element branch
     * @param txn pointer to the transaction
     * @return pointer to the iterator
     */
    public static MemorySegment yxmlelemAttrIter(MemorySegment xml, MemorySegment txn) {
        try {
            return (MemorySegment) YXMLELEM_ATTR_ITER.invokeExact(xml, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlelem_attr_iter", t);
        }
    }

    // struct YXmlAttr *yxmlattr_iter_next(YXmlAttrIter *iterator)
    private static final MethodHandle YXMLATTR_ITER_NEXT = LINKER.downcallHandle(
        LOOKUP.find("yxmlattr_iter_next").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the next attribute from the iterator.
     *
     * @param iter pointer to the iterator
     * @return pointer to the attribute, or null if no more attributes
     */
    public static MemorySegment yxmlattrIterNext(MemorySegment iter) {
        try {
            return (MemorySegment) YXMLATTR_ITER_NEXT.invokeExact(iter);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlattr_iter_next", t);
        }
    }

    // void yxmlattr_iter_destroy(YXmlAttrIter *iterator)
    private static final MethodHandle YXMLATTR_ITER_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("yxmlattr_iter_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Destroys an attribute iterator.
     *
     * @param iter pointer to the iterator
     */
    public static void yxmlattrIterDestroy(MemorySegment iter) {
        try {
            YXMLATTR_ITER_DESTROY.invokeExact(iter);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlattr_iter_destroy", t);
        }
    }

    // void yxmlattr_destroy(struct YXmlAttr *attr)
    private static final MethodHandle YXMLATTR_DESTROY = LINKER.downcallHandle(
        LOOKUP.find("yxmlattr_destroy").orElseThrow(),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Destroys an attribute.
     *
     * @param attr pointer to the attribute
     */
    public static void yxmlattrDestroy(MemorySegment attr) {
        try {
            YXMLATTR_DESTROY.invokeExact(attr);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmlattr_destroy", t);
        }
    }

    /**
     * Layout for YXmlAttr struct.
     * The struct is: const char *name (8 bytes) + const char *value (8 bytes)
     */
    public static final StructLayout YXMLATTR_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("name"),
        ValueLayout.ADDRESS.withName("value")
    );

    private static final java.lang.invoke.VarHandle YXMLATTR_NAME =
        YXMLATTR_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("name"));
    private static final java.lang.invoke.VarHandle YXMLATTR_VALUE =
        YXMLATTR_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("value"));

    /**
     * Reads the name from a YXmlAttr.
     *
     * @param attr pointer to the attribute
     * @return the name string
     */
    public static String yxmlattrReadName(MemorySegment attr) {
        MemorySegment reinterpreted = attr.reinterpret(YXMLATTR_LAYOUT.byteSize());
        MemorySegment namePtr = (MemorySegment) YXMLATTR_NAME.get(reinterpreted, 0L);
        if (namePtr.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment nameReinterpreted = namePtr.reinterpret(Long.MAX_VALUE);
        return nameReinterpreted.getString(0);
    }

    /**
     * Reads the value from a YXmlAttr.
     *
     * @param attr pointer to the attribute
     * @return the value string
     */
    public static String yxmlattrReadValue(MemorySegment attr) {
        MemorySegment reinterpreted = attr.reinterpret(YXMLATTR_LAYOUT.byteSize());
        MemorySegment valuePtr = (MemorySegment) YXMLATTR_VALUE.get(reinterpreted, 0L);
        if (valuePtr.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment valueReinterpreted = valuePtr.reinterpret(Long.MAX_VALUE);
        return valueReinterpreted.getString(0);
    }

    // =========================================================================
    // YXmlText Functions
    // =========================================================================

    // uint32_t yxmltext_len(const Branch *txt, const YTransaction *txn)
    private static final MethodHandle YXMLTEXT_LEN = LINKER.downcallHandle(
        LOOKUP.find("yxmltext_len").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the length of an XML text node.
     *
     * @param txt pointer to the xml text branch
     * @param txn pointer to the transaction
     * @return the length
     */
    public static int yxmltextLen(MemorySegment txt, MemorySegment txn) {
        try {
            return (int) YXMLTEXT_LEN.invokeExact(txt, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmltext_len", t);
        }
    }

    // char *yxmltext_string(const Branch *txt, const YTransaction *txn)
    private static final MethodHandle YXMLTEXT_STRING = LINKER.downcallHandle(
        LOOKUP.find("yxmltext_string").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Gets the string content of an XML text node.
     *
     * @param txt pointer to the xml text branch
     * @param txn pointer to the transaction
     * @return pointer to the string (must be freed with ystringDestroy)
     */
    public static MemorySegment yxmltextString(MemorySegment txt, MemorySegment txn) {
        try {
            return (MemorySegment) YXMLTEXT_STRING.invokeExact(txt, txn);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmltext_string", t);
        }
    }

    // void yxmltext_insert(const Branch *txt, YTransaction *txn, uint32_t index,
    //                      const char *str, const struct YInput *attrs)
    private static final MethodHandle YXMLTEXT_INSERT = LINKER.downcallHandle(
        LOOKUP.find("yxmltext_insert").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Inserts text into an XML text node at the given index.
     *
     * @param txt pointer to the xml text branch
     * @param txn pointer to the transaction
     * @param index the index to insert at
     * @param str pointer to the string to insert
     * @param attrs pointer to attributes (can be NULL)
     */
    public static void yxmltextInsert(
            MemorySegment txt, MemorySegment txn, int index,
            MemorySegment str, MemorySegment attrs) {
        try {
            YXMLTEXT_INSERT.invokeExact(txt, txn, index, str, attrs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmltext_insert", t);
        }
    }

    // void yxmltext_remove_range(const Branch *txt, YTransaction *txn, uint32_t idx, uint32_t len)
    private static final MethodHandle YXMLTEXT_REMOVE_RANGE = LINKER.downcallHandle(
        LOOKUP.find("yxmltext_remove_range").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT
        )
    );

    /**
     * Removes text from an XML text node at the given range.
     *
     * @param txt pointer to the xml text branch
     * @param txn pointer to the transaction
     * @param index the start index
     * @param len the length to remove
     */
    public static void yxmltextRemoveRange(
            MemorySegment txt, MemorySegment txn, int index, int len) {
        try {
            YXMLTEXT_REMOVE_RANGE.invokeExact(txt, txn, index, len);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmltext_remove_range", t);
        }
    }

    // void yxmltext_format(const Branch *txt, YTransaction *txn,
    //                      uint32_t index, uint32_t len, const struct YInput *attrs)
    private static final MethodHandle YXMLTEXT_FORMAT = LINKER.downcallHandle(
        LOOKUP.find("yxmltext_format").orElseThrow(),
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS
        )
    );

    /**
     * Applies formatting to a range of text in an XML text node.
     *
     * @param txt pointer to the xml text branch
     * @param txn pointer to the transaction
     * @param index the start index
     * @param len the length to format
     * @param attrs pointer to the formatting attributes
     */
    public static void yxmltextFormat(
            MemorySegment txt, MemorySegment txn, int index, int len, MemorySegment attrs) {
        try {
            YXMLTEXT_FORMAT.invokeExact(txt, txn, index, len, attrs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yxmltext_format", t);
        }
    }

    // =========================================================================
    // YOutput XML Reader Functions
    // =========================================================================

    // Branch *youtput_read_yxmlelem(const struct YOutput *val)
    private static final MethodHandle YOUTPUT_READ_YXMLELEM = LINKER.downcallHandle(
        LOOKUP.find("youtput_read_yxmlelem").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Reads an XML element from a YOutput value.
     *
     * @param val pointer to the YOutput
     * @return pointer to the element branch, or null if not an element
     */
    public static MemorySegment youtputReadYxmlelem(MemorySegment val) {
        try {
            return (MemorySegment) YOUTPUT_READ_YXMLELEM.invokeExact(val);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call youtput_read_yxmlelem", t);
        }
    }

    // Branch *youtput_read_yxmltext(const struct YOutput *val)
    private static final MethodHandle YOUTPUT_READ_YXMLTEXT = LINKER.downcallHandle(
        LOOKUP.find("youtput_read_yxmltext").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    );

    /**
     * Reads an XML text node from a YOutput value.
     *
     * @param val pointer to the YOutput
     * @return pointer to the text branch, or null if not a text node
     */
    public static MemorySegment youtputReadYxmltext(MemorySegment val) {
        try {
            return (MemorySegment) YOUTPUT_READ_YXMLTEXT.invokeExact(val);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call youtput_read_yxmltext", t);
        }
    }
}
