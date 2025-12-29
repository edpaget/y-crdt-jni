# Panama FFM Struct Return Conventions

This document describes the challenges and solutions for handling C structs returned by value when using Project Panama FFM (Foreign Function & Memory) API on different architectures.

## The Problem

When calling native C functions that return structs by value, the calling convention varies by:
1. The architecture (x86_64, ARM64/AArch64)
2. The size and composition of the struct
3. Whether the struct is a "homogeneous floating-point aggregate" (HFA)

### ARM64 (AArch64) Calling Convention

On ARM64 (Apple Silicon, etc.), the rules for struct returns are:

| Struct Size | Return Method |
|-------------|---------------|
| <= 16 bytes | Returned in registers (x0/x1 for integers/pointers, v0-v3 for HFAs) |
| > 16 bytes  | Returned via hidden pointer (sret - x8 register contains return buffer address) |

### x86_64 Calling Convention

On x86_64, the rules are similar but with different thresholds:

| Struct Size | Return Method |
|-------------|---------------|
| <= 16 bytes | Returned in registers (rax/rdx) |
| > 16 bytes  | Returned via hidden pointer (sret - rdi contains return buffer address) |

## The Issue We Encountered

The `yffi` library's `yinput_*` functions return a `YInput` struct by value:

```c
struct YInput yinput_string(const char *str);
struct YInput yinput_float(double num);
struct YInput yinput_long(int64_t integer);
```

The `YInput` struct layout:
```c
typedef struct YInput {
    int8_t tag;              // 1 byte
    // 3 bytes padding
    uint32_t len;            // 4 bytes
    union YInputContent value; // 16 bytes (largest member is YMapInputData: 2 pointers)
} YInput;
```

**Total size: 24 bytes** (verified with sizeof in both C and Rust)

Since 24 > 16 bytes, ARM64 uses **sret** (hidden return pointer in x8 register).

### What Went Wrong

When we specified a 24-byte layout to Panama:

```java
public static final StructLayout YINPUT_LAYOUT = MemoryLayout.structLayout(
    ValueLayout.JAVA_BYTE.withName("tag"),
    MemoryLayout.paddingLayout(3),
    ValueLayout.JAVA_INT.withName("len"),
    ValueLayout.ADDRESS.withName("value_ptr1"),
    ValueLayout.ADDRESS.withName("value_ptr2")
);  // 24 bytes total
```

Panama rejected it with:
```
IllegalArgumentException: Layout '[b1(tag)x3i4(len)...]' has unexpected size: 24 != 16
```

When we tried a 16-byte layout, Panama accepted it but the native call crashed:
```
SIGBUS at yinput_string+0x4
```

Looking at the disassembly:
```asm
_yinput_string:
    mov  w9, #0xfb           ; tag = Y_JSON_STR
    strb w9, [x8]            ; CRASH - x8 not set by caller!
    mov  w9, #0x1
    str  w9, [x8, #0x4]
    str  x0, [x8, #0x8]
    ret
```

The function expects x8 to contain a valid return buffer address (sret convention), but Panama wasn't setting up x8 because it thought the struct was only 16 bytes.

## The Solution

Instead of calling the native `yinput_*` functions directly, we **construct the YInput struct manually in Java**:

```java
// Tag constants from libyrs.h
private static final byte Y_JSON_NUM = -7;  // double
private static final byte Y_JSON_INT = -6;  // int64
private static final byte Y_JSON_STR = -5;  // string

// 16-byte layout (sufficient for simple values)
public static final StructLayout YINPUT_LAYOUT = MemoryLayout.structLayout(
    ValueLayout.JAVA_BYTE.withName("tag"),
    MemoryLayout.paddingLayout(3),
    ValueLayout.JAVA_INT.withName("len"),
    ValueLayout.ADDRESS.withName("value")
);

// VarHandles for field access
private static final VarHandle YINPUT_TAG =
    YINPUT_LAYOUT.varHandle(PathElement.groupElement("tag"));
private static final VarHandle YINPUT_LEN =
    YINPUT_LAYOUT.varHandle(PathElement.groupElement("len"));
private static final VarHandle YINPUT_VALUE_PTR =
    YINPUT_LAYOUT.varHandle(PathElement.groupElement("value"));

public static MemorySegment yinputString(Arena arena, MemorySegment str) {
    MemorySegment yinput = arena.allocate(YINPUT_LAYOUT);
    YINPUT_TAG.set(yinput, 0L, Y_JSON_STR);
    YINPUT_LEN.set(yinput, 0L, 1);
    YINPUT_VALUE_PTR.set(yinput, 0L, str);
    return yinput;
}
```

This approach:
1. Avoids the sret ABI complexity entirely
2. Uses only the first 16 bytes of the union (sufficient for pointer/double/long values)
3. Works correctly on all architectures

## Guidelines for Panama FFM Development

### When Binding Native Functions

1. **Check struct sizes carefully**: Use `sizeof()` in C/Rust to verify actual sizes
2. **Be aware of union sizes**: A union's size is the size of its largest member
3. **Consider manual struct construction**: For complex structs or those > 16 bytes, it may be simpler to construct them in Java

### Struct Size Thresholds

| Architecture | Register Return Threshold |
|--------------|---------------------------|
| ARM64        | <= 16 bytes               |
| x86_64       | <= 16 bytes               |

### Diagnostic Steps When Crashes Occur

1. **Get the crash address**: Look for the problematic frame in the error log
2. **Disassemble the function**: Use `objdump -d library.dylib` to see the actual assembly
3. **Check for sret usage**: Look for stores to x8 (ARM64) or writes to [rdi] (x86_64) at the function start
4. **Verify struct sizes**: Compare what Panama expects vs. what the native code expects

### Workarounds for Large Struct Returns

1. **Manual struct construction**: Build the struct in Java if you know its layout
2. **Use output parameters**: If the API supports it, pass a buffer for the result
3. **Wrapper functions**: Create native wrapper functions that use output parameters instead of returns

## References

- [ARM64 ABI Documentation](https://developer.arm.com/documentation/ihi0055/latest)
- [System V AMD64 ABI](https://gitlab.com/x86-psABIs/x86-64-ABI)
- [JEP 454: Foreign Function & Memory API](https://openjdk.org/jeps/454)
- [Panama FFM Javadoc](https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/package-summary.html)
