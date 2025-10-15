use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{JClass, JString};
use jni::sys::{jdouble, jint, jlong, jstring};
use jni::JNIEnv;
use yrs::types::ToJson;
use yrs::{Array, ArrayRef, Doc, Transact};

/// Gets or creates a YArray instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the array object in the document
///
/// # Returns
/// A pointer to the YArray instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeGetArray(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    name: JString,
) -> jlong {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }

    // Convert Java string to Rust string
    let name_str = match env.get_string(&name) {
        Ok(s) => match s.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                throw_exception(&mut env, "Invalid UTF-8 in name");
                return 0;
            }
        },
        Err(_) => {
            throw_exception(&mut env, "Failed to get name string");
            return 0;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = doc.get_or_insert_array(name_str.as_str());
        to_java_ptr(array)
    }
}

/// Destroys a YArray instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YArray instance
///
/// # Safety
/// The pointer must be valid and point to a YArray instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    if ptr != 0 {
        unsafe {
            free_java_ptr::<ArrayRef>(ptr);
        }
    }
}

/// Gets the length of the array
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
///
/// # Returns
/// The length of the array as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeLength(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
) -> jint {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return 0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let txn = doc.transact();
        array.len(&txn) as jint
    }
}

/// Gets a string value from the array at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `index`: The index to get from
///
/// # Returns
/// A Java string, or null if index is out of bounds or value is not a string
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeGetString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    index: jint,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let txn = doc.transact();

        match array.get(&txn, index as u32) {
            Some(value) => {
                let s = value.to_string(&txn);
                to_jstring(&mut env, &s)
            }
            None => std::ptr::null_mut(),
        }
    }
}

/// Gets a double value from the array at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `index`: The index to get from
///
/// # Returns
/// The double value, or 0.0 if index is out of bounds or value is not a number
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeGetDouble(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    index: jint,
) -> jdouble {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0.0;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return 0.0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let txn = doc.transact();

        match array.get(&txn, index as u32) {
            Some(value) => value.cast::<f64>().unwrap_or(0.0),
            None => 0.0,
        }
    }
}

/// Inserts a string value at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `index`: The index at which to insert
/// - `value`: The string value to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeInsertString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    index: jint,
    value: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return;
    }

    // Convert Java string to Rust string
    let value_jstring = match env.get_string(&value) {
        Ok(s) => s,
        Err(_) => {
            throw_exception(&mut env, "Failed to get value string");
            return;
        }
    };
    let value_str: String = value_jstring.into();

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let mut txn = doc.transact_mut();
        array.insert(&mut txn, index as u32, value_str);
    }
}

/// Inserts a double value at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `index`: The index at which to insert
/// - `value`: The double value to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeInsertDouble(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    index: jint,
    value: jdouble,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let mut txn = doc.transact_mut();
        array.insert(&mut txn, index as u32, value);
    }
}

/// Pushes a string value to the end of the array
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `value`: The string value to push
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativePushString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    value: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return;
    }

    // Convert Java string to Rust string
    let value_jstring = match env.get_string(&value) {
        Ok(s) => s,
        Err(_) => {
            throw_exception(&mut env, "Failed to get value string");
            return;
        }
    };
    let value_str: String = value_jstring.into();

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let mut txn = doc.transact_mut();
        array.push_back(&mut txn, value_str);
    }
}

/// Pushes a double value to the end of the array
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `value`: The double value to push
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativePushDouble(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    value: jdouble,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let mut txn = doc.transact_mut();
        array.push_back(&mut txn, value);
    }
}

/// Removes a range of elements from the array
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `index`: The starting index
/// - `length`: The number of elements to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeRemove(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    index: jint,
    length: jint,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let mut txn = doc.transact_mut();
        array.remove_range(&mut txn, index as u32, length as u32);
    }
}

/// Converts the array to a JSON string representation
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
///
/// # Returns
/// A JSON string representation of the array
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeToJson(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let txn = doc.transact();
        let json = array.to_json(&txn).to_string();
        to_jstring(&mut env, &json)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use yrs::{Doc, Transact};

    #[test]
    fn test_array_creation() {
        let doc = Doc::new();
        let array = doc.get_or_insert_array("test");
        let ptr = to_java_ptr(array);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<ArrayRef>(ptr);
        }
    }

    #[test]
    fn test_array_push_and_read() {
        let doc = Doc::new();
        let array = doc.get_or_insert_array("test");

        {
            let mut txn = doc.transact_mut();
            array.push_back(&mut txn, "Hello");
            array.push_back(&mut txn, 42.0);
            array.push_back(&mut txn, "World");
        }

        let txn = doc.transact();
        assert_eq!(array.len(&txn), 3);
        assert_eq!(array.get(&txn, 0).unwrap().to_string(&txn), "Hello");
        assert_eq!(array.get(&txn, 1).unwrap().cast::<f64>().unwrap(), 42.0);
        assert_eq!(array.get(&txn, 2).unwrap().to_string(&txn), "World");
    }

    #[test]
    fn test_array_insert() {
        let doc = Doc::new();
        let array = doc.get_or_insert_array("test");

        {
            let mut txn = doc.transact_mut();
            array.push_back(&mut txn, "Hello");
            array.push_back(&mut txn, "World");
            array.insert(&mut txn, 1, "Beautiful");
        }

        let txn = doc.transact();
        assert_eq!(array.len(&txn), 3);
        assert_eq!(array.get(&txn, 0).unwrap().to_string(&txn), "Hello");
        assert_eq!(array.get(&txn, 1).unwrap().to_string(&txn), "Beautiful");
        assert_eq!(array.get(&txn, 2).unwrap().to_string(&txn), "World");
    }

    #[test]
    fn test_array_remove() {
        let doc = Doc::new();
        let array = doc.get_or_insert_array("test");

        {
            let mut txn = doc.transact_mut();
            array.push_back(&mut txn, "One");
            array.push_back(&mut txn, "Two");
            array.push_back(&mut txn, "Three");
        }

        {
            let mut txn = doc.transact_mut();
            array.remove_range(&mut txn, 1, 1);
        }

        let txn = doc.transact();
        assert_eq!(array.len(&txn), 2);
        assert_eq!(array.get(&txn, 0).unwrap().to_string(&txn), "One");
        assert_eq!(array.get(&txn, 1).unwrap().to_string(&txn), "Three");
    }
}
