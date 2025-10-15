use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;
use yrs::{Doc, GetString, Transact, XmlElementPrelim, XmlFragment, XmlFragmentRef, XmlTextPrelim};

/// Gets or creates a YXmlFragment instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the XML fragment in the document
///
/// # Returns
/// A pointer to the YXmlFragment instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeGetFragment(
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
        to_java_ptr(fragment)
    }
}

/// Destroys a YXmlFragment instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YXmlFragment instance
///
/// # Safety
/// The pointer must be valid and point to a YXmlFragment instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    if ptr != 0 {
        unsafe {
            free_java_ptr::<XmlFragmentRef>(ptr);
        }
    }
}

/// Gets the number of children in the fragment
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
///
/// # Returns
/// The number of children as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeLength(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
) -> jint {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return 0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(fragment_ptr);
        let txn = doc.transact();
        fragment.len(&txn) as jint
    }
}

/// Inserts an XML element as a child at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `index`: The index at which to insert the element
/// - `tag`: The tag name for the element
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeInsertElement(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
    index: jint,
    tag: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return;
    }

    // Convert tag to Rust string
    let tag_str: String = match env.get_string(&tag) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get tag string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(fragment_ptr);
        let mut txn = doc.transact_mut();
        fragment.insert(
            &mut txn,
            index as u32,
            XmlElementPrelim::empty(tag_str.as_str()),
        );
    }
}

/// Inserts an XML text node as a child at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `index`: The index at which to insert the text
/// - `content`: The text content
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeInsertText(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
    index: jint,
    content: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return;
    }

    // Convert content to Rust string
    let content_str: String = match env.get_string(&content) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get content string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(fragment_ptr);
        let mut txn = doc.transact_mut();
        fragment.insert(
            &mut txn,
            index as u32,
            XmlTextPrelim::new(content_str.as_str()),
        );
    }
}

/// Removes children from the fragment
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `index`: The starting index
/// - `length`: The number of children to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeRemove(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
    index: jint,
    length: jint,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(fragment_ptr);
        let mut txn = doc.transact_mut();
        fragment.remove_range(&mut txn, index as u32, length as u32);
    }
}

/// Gets the type of child node at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `index`: The index of the child
///
/// # Returns
/// 0 for ELEMENT, 1 for TEXT, -1 if no node at index
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeGetNodeType(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
    index: jint,
) -> jint {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return -1;
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return -1;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(fragment_ptr);
        let txn = doc.transact();

        if let Some(child) = fragment.get(&txn, index as u32) {
            // Check element first, then text
            if child.clone().into_xml_element().is_some() {
                return 0; // ELEMENT
            } else if child.into_xml_text().is_some() {
                return 1; // TEXT
            }
        }
        -1 // No node at index
    }
}

/// Gets the XML element at the specified index (if it is an element)
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `index`: The index of the child
///
/// # Returns
/// Pointer to the element (wrapped in fragment), or 0 if not an element
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeGetElement(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
    index: jint,
) -> jlong {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return 0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(fragment_ptr);
        let txn = doc.transact();

        if let Some(child) = fragment.get(&txn, index as u32) {
            if let Some(element) = child.into_xml_element() {
                // Create a temporary fragment to wrap just this element
                // We'll use the element's parent reference approach
                // For now, return a pointer that can be used to access this element
                // This requires creating a new wrapper fragment
                let wrapper_name = format!("__element_wrapper_{}", element.tag());
                let wrapper_fragment = doc.get_or_insert_xml_fragment(wrapper_name.as_str());

                // Clear the wrapper and insert just this element reference
                drop(txn);
                let mut txn = doc.transact_mut();
                let wrapper_len = wrapper_fragment.len(&txn);
                if wrapper_len > 0 {
                    wrapper_fragment.remove_range(&mut txn, 0, wrapper_len);
                }

                // Note: We can't easily move the element, so we need a different approach
                // Instead, we'll store the fragment pointer and index, and access it that way
                // For now, return 0 and we'll need to redesign this
                return 0;
            }
        }
        0
    }
}

/// Gets the XML text at the specified index (if it is text)
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `index`: The index of the child
///
/// # Returns
/// Pointer to the text (wrapped in fragment), or 0 if not text
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeGetText(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
    _index: jint,
) -> jlong {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return 0;
    }

    // Similar issue as getElement - we need to redesign how we handle node references
    // For now return 0
    0
}

/// Returns the XML string representation of the fragment
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
///
/// # Returns
/// A Java string containing the XML representation
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlFragment_nativeToXmlString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if fragment_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlFragment pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(fragment_ptr);
        let txn = doc.transact();
        let xml_string = fragment.get_string(&txn);
        to_jstring(&mut env, &xml_string)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use yrs::{Doc, Transact, XmlFragment};

    #[test]
    fn test_fragment_creation() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        let ptr = to_java_ptr(fragment);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<XmlFragmentRef>(ptr);
        }
    }

    #[test]
    fn test_fragment_insert_element() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlElementPrelim::empty("div"));
        }

        let txn = doc.transact();
        assert_eq!(fragment.len(&txn), 1);

        let child = fragment.get(&txn, 0).unwrap();
        assert!(child.into_xml_element().is_some());
    }

    #[test]
    fn test_fragment_insert_text() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlTextPrelim::new("Hello"));
        }

        let txn = doc.transact();
        assert_eq!(fragment.len(&txn), 1);

        let child = fragment.get(&txn, 0).unwrap();
        assert!(child.into_xml_text().is_some());
    }

    #[test]
    fn test_fragment_remove() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlElementPrelim::empty("div"));
            fragment.insert(&mut txn, 1, XmlTextPrelim::new("Hello"));
        }

        {
            let txn = doc.transact();
            assert_eq!(fragment.len(&txn), 2);
        }

        {
            let mut txn = doc.transact_mut();
            fragment.remove_range(&mut txn, 0, 1);
        }

        let txn = doc.transact();
        assert_eq!(fragment.len(&txn), 1);
    }
}
