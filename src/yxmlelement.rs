use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use yrs::{Doc, GetString, Transact, Xml, XmlElementPrelim, XmlFragment, XmlFragmentRef};

/// Gets or creates a YXmlElement instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the XML element object in the document
///
/// # Returns
/// A pointer to the YXmlElement instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetXmlElement(
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

        // Ensure the fragment has an element child at index 0
        {
            let txn = doc.transact();
            if fragment.len(&txn) == 0 {
                drop(txn);
                let mut txn = doc.transact_mut();
                fragment.insert(&mut txn, 0, XmlElementPrelim::empty(name_str.as_str()));
            }
        }

        to_java_ptr(fragment)
    }
}

/// Destroys a YXmlElement instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YXmlElement instance
///
/// # Safety
/// The pointer must be valid and point to a YXmlElement instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeDestroy(
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

/// Gets the tag name of the XML element
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
///
/// # Returns
/// A Java string containing the tag name
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetTag(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if xml_element_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlElement pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(xml_element_ptr);
        let txn = doc.transact();

        // Get the first child as XmlElementRef
        if let Some(child) = fragment.get(&txn, 0) {
            if let Some(element) = child.into_xml_element() {
                let tag = element.tag();
                return to_jstring(&mut env, tag.as_ref());
            }
        }
        to_jstring(&mut env, "")
    }
}

/// Gets an attribute value by name
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `name`: The attribute name
///
/// # Returns
/// The attribute value as a Java string, or null if not found
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetAttribute(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    name: JString,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if xml_element_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlElement pointer");
        return std::ptr::null_mut();
    }

    // Convert name to Rust string
    let name_str: String = match env.get_string(&name) {
        Ok(s) => match s.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                throw_exception(&mut env, "Invalid UTF-8 in name");
                return std::ptr::null_mut();
            }
        },
        Err(_) => {
            throw_exception(&mut env, "Failed to get name string");
            return std::ptr::null_mut();
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(xml_element_ptr);
        let txn = doc.transact();

        // Get the first child as XmlElementRef
        if let Some(child) = fragment.get(&txn, 0) {
            if let Some(element) = child.into_xml_element() {
                match element.get_attribute(&txn, &name_str) {
                    Some(value) => return to_jstring(&mut env, &value),
                    None => return std::ptr::null_mut(),
                }
            }
        }
        std::ptr::null_mut()
    }
}

/// Sets an attribute value
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `name`: The attribute name
/// - `value`: The attribute value
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeSetAttribute(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    name: JString,
    value: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if xml_element_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlElement pointer");
        return;
    }

    // Convert name to Rust string
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get name string");
            return;
        }
    };

    // Convert value to Rust string
    let value_str: String = match env.get_string(&value) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get value string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(xml_element_ptr);
        let mut txn = doc.transact_mut();

        // Get the first child as XmlElementRef
        if let Some(child) = fragment.get(&txn, 0) {
            if let Some(element) = child.into_xml_element() {
                element.insert_attribute(&mut txn, name_str, value_str);
            }
        }
    }
}

/// Removes an attribute
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `name`: The attribute name to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeRemoveAttribute(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    name: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if xml_element_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlElement pointer");
        return;
    }

    // Convert name to Rust string
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get name string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(xml_element_ptr);
        let mut txn = doc.transact_mut();

        // Get the first child as XmlElementRef
        if let Some(child) = fragment.get(&txn, 0) {
            if let Some(element) = child.into_xml_element() {
                element.remove_attribute(&mut txn, &name_str);
            }
        }
    }
}

/// Gets all attribute names
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
///
/// # Returns
/// A Java String[] array containing all attribute names
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetAttributeNames<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
) -> JObject<'a> {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return JObject::null();
    }
    if xml_element_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlElement pointer");
        return JObject::null();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(xml_element_ptr);
        let txn = doc.transact();

        // Get the first child as XmlElementRef
        let names: Vec<String> = if let Some(child) = fragment.get(&txn, 0) {
            if let Some(element) = child.into_xml_element() {
                element
                    .attributes(&txn)
                    .map(|(k, _)| k.to_string())
                    .collect()
            } else {
                Vec::new()
            }
        } else {
            Vec::new()
        };

        // Create Java String array
        let string_class = match env.find_class("java/lang/String") {
            Ok(cls) => cls,
            Err(_) => {
                throw_exception(&mut env, "Failed to find String class");
                return JObject::null();
            }
        };

        let array = match env.new_object_array(names.len() as i32, string_class, JObject::null()) {
            Ok(arr) => arr,
            Err(_) => {
                throw_exception(&mut env, "Failed to create String array");
                return JObject::null();
            }
        };

        // Fill the array
        for (i, name) in names.iter().enumerate() {
            let jname = match env.new_string(name) {
                Ok(s) => s,
                Err(_) => {
                    throw_exception(&mut env, "Failed to create Java string");
                    return JObject::null();
                }
            };
            if env
                .set_object_array_element(&array, i as i32, &jname)
                .is_err()
            {
                throw_exception(&mut env, "Failed to set array element");
                return JObject::null();
            }
        }

        JObject::from(array)
    }
}

/// Returns the XML string representation of the element
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
///
/// # Returns
/// A Java string containing the XML representation
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeToString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if xml_element_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlElement pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let fragment = from_java_ptr::<XmlFragmentRef>(xml_element_ptr);
        let txn = doc.transact();

        // Get the first child as XmlElementRef
        if let Some(child) = fragment.get(&txn, 0) {
            if let Some(element) = child.into_xml_element() {
                let xml_string = element.get_string(&txn);
                return to_jstring(&mut env, &xml_string);
            }
        }
        to_jstring(&mut env, "")
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use yrs::{Doc, Transact, XmlFragment};

    #[test]
    fn test_xml_element_creation() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        // Initialize with element child
        let mut txn = doc.transact_mut();
        fragment.insert(&mut txn, 0, XmlElementPrelim::empty("test"));
        drop(txn);

        let ptr = to_java_ptr(fragment);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<XmlFragmentRef>(ptr);
        }
    }

    #[test]
    fn test_xml_element_tag() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        let mut txn = doc.transact_mut();
        let element = fragment.insert(&mut txn, 0, XmlElementPrelim::empty("test"));
        drop(txn);

        let txn = doc.transact();
        let tag = element.tag();
        assert_eq!(tag.as_ref(), "test");
    }

    #[test]
    fn test_xml_element_attributes() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("div");

        {
            let mut txn = doc.transact_mut();
            let element = fragment.insert(&mut txn, 0, XmlElementPrelim::empty("div"));
            element.insert_attribute(&mut txn, "class", "container");
            element.insert_attribute(&mut txn, "id", "main");
        }

        let txn = doc.transact();
        let element = fragment.get(&txn, 0).unwrap().into_xml_element().unwrap();
        assert_eq!(
            element.get_attribute(&txn, "class"),
            Some("container".to_string())
        );
        assert_eq!(
            element.get_attribute(&txn, "id"),
            Some("main".to_string())
        );
    }

    #[test]
    fn test_xml_element_remove_attribute() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("div");

        {
            let mut txn = doc.transact_mut();
            let element = fragment.insert(&mut txn, 0, XmlElementPrelim::empty("div"));
            element.insert_attribute(&mut txn, "class", "container");
            element.insert_attribute(&mut txn, "id", "main");
        }

        {
            let mut txn = doc.transact_mut();
            let element = fragment.get(&txn, 0).unwrap().into_xml_element().unwrap();
            element.remove_attribute(&mut txn, &"class".to_string());
        }

        let txn = doc.transact();
        let element = fragment.get(&txn, 0).unwrap().into_xml_element().unwrap();
        assert_eq!(element.get_attribute(&txn, "class"), None);
        assert_eq!(
            element.get_attribute(&txn, "id"),
            Some("main".to_string())
        );
    }
}
