use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;
use yrs::{Doc, GetString, Text, Transact, XmlFragment, XmlTextPrelim, XmlTextRef};

/// Gets or creates a YXmlText instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the XML text object in the document
///
/// # Returns
/// A pointer to the YXmlText instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeGetXmlText(
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
        let fragment = doc.get_or_insert_xml_fragment(name_str.as_str());

        // Ensure the fragment has a text child at index 0
        {
            let txn = doc.transact();
            if fragment.len(&txn) == 0 {
                drop(txn);
                let mut txn = doc.transact_mut();
                fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
            }
        }

        // Return a pointer to the text at index 0, not the fragment
        let txn = doc.transact();
        if let Some(child) = fragment.get(&txn, 0) {
            if let Some(text) = child.into_xml_text() {
                return to_java_ptr(text);
            }
        }
        0
    }
}

/// Destroys a YXmlText instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YXmlText instance
///
/// # Safety
/// The pointer must be valid and point to a YXmlText instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    if ptr != 0 {
        unsafe {
            free_java_ptr::<XmlTextRef>(ptr);
        }
    }
}

/// Gets the length of the XML text (number of characters)
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
///
/// # Returns
/// The length of the text as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeLength(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
) -> jint {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return 0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let txn = doc.transact();

        text.len(&txn) as jint
    }
}

/// Returns the string representation of the XML text
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
///
/// # Returns
/// A Java string containing the text content
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeToString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let txn = doc.transact();

        let string = text.get_string(&txn);
        to_jstring(&mut env, &string)
    }
}

/// Inserts text at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `index`: The index at which to insert the text
/// - `chunk`: The text to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeInsert(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    index: jint,
    chunk: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return;
    }

    // Convert chunk to Rust string
    let chunk_str: String = match env.get_string(&chunk) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get chunk string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let mut txn = doc.transact_mut();

        text.insert(&mut txn, index as u32, &chunk_str);
    }
}

/// Appends text to the end
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `chunk`: The text to append
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativePush(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    chunk: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return;
    }

    // Convert chunk to Rust string
    let chunk_str: String = match env.get_string(&chunk) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get chunk string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let mut txn = doc.transact_mut();

        text.push(&mut txn, &chunk_str);
    }
}

/// Deletes a range of text
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `index`: The starting index of the deletion
/// - `length`: The number of characters to delete
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeDelete(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    index: jint,
    length: jint,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let mut txn = doc.transact_mut();

        text.remove_range(&mut txn, index as u32, length as u32);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use yrs::{Doc, Transact, XmlFragment, XmlFragmentRef};

    #[test]
    fn test_xml_text_creation() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        // Initialize with text child
        let mut txn = doc.transact_mut();
        fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
        drop(txn);

        let ptr = to_java_ptr(fragment);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<XmlFragmentRef>(ptr);
        }
    }

    #[test]
    fn test_xml_text_insert_and_read() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            let text = fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
            text.insert(&mut txn, 0, "Hello");
        }

        let txn = doc.transact();
        let text = fragment.get(&txn, 0).unwrap().into_xml_text().unwrap();
        assert_eq!(text.len(&txn), 5);
        assert_eq!(text.get_string(&txn), "Hello");
    }

    #[test]
    fn test_xml_text_push() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            let text = fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
            text.push(&mut txn, "Hello");
            text.push(&mut txn, " World");
        }

        let txn = doc.transact();
        let text = fragment.get(&txn, 0).unwrap().into_xml_text().unwrap();
        assert_eq!(text.get_string(&txn), "Hello World");
    }

    #[test]
    fn test_xml_text_delete() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            let text = fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
            text.push(&mut txn, "Hello World");
            text.remove_range(&mut txn, 5, 6); // Remove " World"
        }

        let txn = doc.transact();
        let text = fragment.get(&txn, 0).unwrap().into_xml_text().unwrap();
        assert_eq!(text.get_string(&txn), "Hello");
    }
}
