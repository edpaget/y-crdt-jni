use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;
use yrs::{Doc, GetString, Text, TextRef, Transact};

/// Gets or creates a YText instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the text object in the document
///
/// # Returns
/// A pointer to the YText instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeGetText(
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
        let text = doc.get_or_insert_text(name_str.as_str());
        to_java_ptr(text)
    }
}

/// Destroys a YText instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YText instance
///
/// # Safety
/// The pointer must be valid and point to a YText instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    if ptr != 0 {
        unsafe {
            free_java_ptr::<TextRef>(ptr);
        }
    }
}

/// Gets the length of the text
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
///
/// # Returns
/// The length of the text as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeLength(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    text_ptr: jlong,
) -> jint {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if text_ptr == 0 {
        throw_exception(&mut env, "Invalid YText pointer");
        return 0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<TextRef>(text_ptr);
        let txn = doc.transact();
        text.len(&txn) as jint
    }
}

/// Gets the string content of the text
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
///
/// # Returns
/// A Java string containing the text content
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeToString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    text_ptr: jlong,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if text_ptr == 0 {
        throw_exception(&mut env, "Invalid YText pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<TextRef>(text_ptr);
        let txn = doc.transact();
        let content = text.get_string(&txn);
        to_jstring(&mut env, &content)
    }
}

/// Inserts text at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `index`: The index at which to insert the text
/// - `chunk`: The text to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeInsert(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    text_ptr: jlong,
    index: jint,
    chunk: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if text_ptr == 0 {
        throw_exception(&mut env, "Invalid YText pointer");
        return;
    }

    // Convert Java string to Rust string
    // Use get_string which handles Modified UTF-8 (Java's internal format)
    let chunk_jstring = match env.get_string(&chunk) {
        Ok(s) => s,
        Err(_) => {
            throw_exception(&mut env, "Failed to get chunk string");
            return;
        }
    };
    let chunk_str: String = chunk_jstring.into();

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<TextRef>(text_ptr);
        let mut txn = doc.transact_mut();
        text.insert(&mut txn, index as u32, &chunk_str);
    }
}

/// Appends text to the end
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `chunk`: The text to append
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativePush(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    text_ptr: jlong,
    chunk: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if text_ptr == 0 {
        throw_exception(&mut env, "Invalid YText pointer");
        return;
    }

    // Convert Java string to Rust string
    // Use get_string which handles Modified UTF-8 (Java's internal format)
    let chunk_jstring = match env.get_string(&chunk) {
        Ok(s) => s,
        Err(_) => {
            throw_exception(&mut env, "Failed to get chunk string");
            return;
        }
    };
    let chunk_str: String = chunk_jstring.into();

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<TextRef>(text_ptr);
        let mut txn = doc.transact_mut();
        text.push(&mut txn, &chunk_str);
    }
}

/// Deletes a range of text
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `index`: The starting index
/// - `length`: The number of characters to delete
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeDelete(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    text_ptr: jlong,
    index: jint,
    length: jint,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if text_ptr == 0 {
        throw_exception(&mut env, "Invalid YText pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<TextRef>(text_ptr);
        let mut txn = doc.transact_mut();
        text.remove_range(&mut txn, index as u32, length as u32);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use yrs::{Doc, Transact};

    #[test]
    fn test_text_creation() {
        let doc = Doc::new();
        let text = doc.get_or_insert_text("test");
        let ptr = to_java_ptr(text);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<TextRef>(ptr);
        }
    }

    #[test]
    fn test_text_insert_and_read() {
        let doc = Doc::new();
        let text = doc.get_or_insert_text("test");

        {
            let mut txn = doc.transact_mut();
            text.insert(&mut txn, 0, "Hello");
            text.insert(&mut txn, 5, " World");
        }

        let txn = doc.transact();
        let content = text.get_string(&txn);
        assert_eq!(content, "Hello World");
        assert_eq!(text.len(&txn), 11);
    }

    #[test]
    fn test_text_push() {
        let doc = Doc::new();
        let text = doc.get_or_insert_text("test");

        {
            let mut txn = doc.transact_mut();
            text.push(&mut txn, "Hello");
            text.push(&mut txn, " ");
            text.push(&mut txn, "World");
        }

        let txn = doc.transact();
        let content = text.get_string(&txn);
        assert_eq!(content, "Hello World");
    }

    #[test]
    fn test_text_delete() {
        let doc = Doc::new();
        let text = doc.get_or_insert_text("test");

        {
            let mut txn = doc.transact_mut();
            text.push(&mut txn, "Hello World");
        }

        {
            let mut txn = doc.transact_mut();
            text.remove_range(&mut txn, 5, 6); // Remove " World"
        }

        let txn = doc.transact();
        let content = text.get_string(&txn);
        assert_eq!(content, "Hello");
        assert_eq!(text.len(&txn), 5);
    }
}
