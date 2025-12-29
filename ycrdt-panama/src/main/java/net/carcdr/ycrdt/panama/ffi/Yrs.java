package net.carcdr.ycrdt.panama.ffi;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
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

    /**
     * Size of the YInput struct.
     * tag (1) + padding (3) + len (4) + union value (8 on 64-bit) = 16 bytes
     */
    public static final long YINPUT_SIZE = 16;

    // struct YInput yinput_string(const char *str)
    private static final MethodHandle YINPUT_STRING = LINKER.downcallHandle(
        LOOKUP.find("yinput_string").orElseThrow(),
        FunctionDescriptor.of(
            MemoryLayout.structLayout(
                ValueLayout.JAVA_BYTE.withName("tag"),
                MemoryLayout.paddingLayout(3),
                ValueLayout.JAVA_INT.withName("len"),
                ValueLayout.ADDRESS.withName("value")
            ),
            ValueLayout.ADDRESS
        )
    );

    /**
     * Creates a YInput containing a string value.
     *
     * @param arena the arena to allocate the result in
     * @param str pointer to the string
     * @return the YInput struct
     */
    public static MemorySegment yinputString(Arena arena, MemorySegment str) {
        try {
            return (MemorySegment) YINPUT_STRING.invokeExact(
                (SegmentAllocator) arena, str);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yinput_string", t);
        }
    }

    // struct YInput yinput_float(double num)
    private static final MethodHandle YINPUT_FLOAT = LINKER.downcallHandle(
        LOOKUP.find("yinput_float").orElseThrow(),
        FunctionDescriptor.of(
            MemoryLayout.structLayout(
                ValueLayout.JAVA_BYTE.withName("tag"),
                MemoryLayout.paddingLayout(3),
                ValueLayout.JAVA_INT.withName("len"),
                ValueLayout.JAVA_DOUBLE.withName("value")
            ),
            ValueLayout.JAVA_DOUBLE
        )
    );

    /**
     * Creates a YInput containing a float value.
     *
     * @param arena the arena to allocate the result in
     * @param num the double value
     * @return the YInput struct
     */
    public static MemorySegment yinputFloat(Arena arena, double num) {
        try {
            return (MemorySegment) YINPUT_FLOAT.invokeExact(
                (SegmentAllocator) arena, num);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yinput_float", t);
        }
    }

    // struct YInput yinput_long(int64_t integer)
    private static final MethodHandle YINPUT_LONG = LINKER.downcallHandle(
        LOOKUP.find("yinput_long").orElseThrow(),
        FunctionDescriptor.of(
            MemoryLayout.structLayout(
                ValueLayout.JAVA_BYTE.withName("tag"),
                MemoryLayout.paddingLayout(3),
                ValueLayout.JAVA_INT.withName("len"),
                ValueLayout.JAVA_LONG.withName("value")
            ),
            ValueLayout.JAVA_LONG
        )
    );

    /**
     * Creates a YInput containing a long value.
     *
     * @param arena the arena to allocate the result in
     * @param num the long value
     * @return the YInput struct
     */
    public static MemorySegment yinputLong(Arena arena, long num) {
        try {
            return (MemorySegment) YINPUT_LONG.invokeExact(
                (SegmentAllocator) arena, num);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to call yinput_long", t);
        }
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
}
