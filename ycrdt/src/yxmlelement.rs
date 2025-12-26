use crate::{
    free_if_valid, from_java_ptr, get_mut_or_throw, get_ref_or_throw, throw_exception, to_java_ptr,
    to_jstring, DocPtr, DocWrapper, TxnPtr, XmlElementPtr,
};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jlong, jstring};
use jni::{Executor, JNIEnv};
use std::sync::Arc;
use yrs::types::xml::XmlEvent;
use yrs::types::Change;
use yrs::{
    GetString, Observable, Transact, TransactionMut, Xml, XmlElementPrelim, XmlElementRef,
    XmlFragment,
};

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
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);

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

    let fragment = wrapper.doc.get_or_insert_xml_fragment(name_str.as_str());

    // Ensure the fragment has an element child at index 0
    {
        let txn = wrapper.doc.transact();
        if fragment.len(&txn) == 0 {
            drop(txn);
            let mut txn = wrapper.doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlElementPrelim::empty(name_str.as_str()));
        }
    }

    // Return a pointer to the element at index 0, not the fragment
    let txn = wrapper.doc.transact();
    if let Some(child) = fragment.get(&txn, 0) {
        if let Some(element) = child.into_xml_element() {
            return to_java_ptr(element);
        }
    }
    0
}

/// Destroys a YXmlElement instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YXmlElement instance (can be XmlFragmentRef or XmlElementRef)
///
/// # Safety
/// The pointer must be valid and point to either a YXmlElement instance
/// Note: We try to free as XmlElementRef first (new pattern), then XmlFragmentRef (old pattern)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    free_if_valid!(XmlElementPtr::from_raw(ptr), XmlElementRef);
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
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetTagWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
) -> jstring {
    let _doc = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        std::ptr::null_mut()
    );
    let _txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let tag = element.tag();
    to_jstring(&mut env, tag.as_ref())
}

/// Gets an attribute value by name using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
/// - `name`: The attribute name
///
/// # Returns
/// The attribute value as a Java string, or null if not found
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetAttributeWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
    name: JString,
) -> jstring {
    let _doc = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

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

    match element.get_attribute(txn, &name_str) {
        Some(yrs::Out::Any(yrs::Any::String(s))) => to_jstring(&mut env, s.as_ref()),
        Some(_) => std::ptr::null_mut(), // Non-string attribute value
        None => std::ptr::null_mut(),
    }
}

/// Sets an attribute value using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
/// - `name`: The attribute name
/// - `value`: The attribute value
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeSetAttributeWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
    name: JString,
    value: JString,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement"
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

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

    element.insert_attribute(txn, name_str, value_str);
}

/// Removes an attribute using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
/// - `name`: The attribute name to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeRemoveAttributeWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
    name: JString,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement"
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert name to Rust string
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get name string");
            return;
        }
    };

    element.remove_attribute(txn, &name_str);
}

/// Gets all attribute names using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A Java String[] array containing all attribute names
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetAttributeNamesWithTxn<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
) -> JObject<'a> {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", JObject::null());
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        JObject::null()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        JObject::null()
    );

    let names: Vec<String> = element
        .attributes(txn)
        .map(|(k, _)| k.to_string())
        .collect();

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

/// Returns the XML string representation of the element using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A Java string containing the XML representation
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeToStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
) -> jstring {
    let _doc = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let xml_string = element.get_string(txn);
    to_jstring(&mut env, &xml_string)
}

/// Gets the number of child nodes in this element using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// The number of child nodes
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeChildCountWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
) -> jni::sys::jint {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        0
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    element.len(txn) as jni::sys::jint
}

/// Inserts an XML element child at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index at which to insert the child
/// - `tag`: The tag name for the new element
///
/// # Returns
/// A pointer to the new YXmlElement child
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeInsertElementWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
    index: jni::sys::jint,
    tag: JString,
) -> jlong {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        0
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    if index < 0 {
        throw_exception(&mut env, "Index cannot be negative");
        return 0;
    }

    // Convert tag to Rust string
    let tag_str: String = match env.get_string(&tag) {
        Ok(s) => match s.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                throw_exception(&mut env, "Invalid UTF-8 in tag");
                return 0;
            }
        },
        Err(_) => {
            throw_exception(&mut env, "Failed to get tag string");
            return 0;
        }
    };

    let new_element = element.insert(txn, index as u32, XmlElementPrelim::empty(tag_str.as_str()));
    to_java_ptr(new_element)
}

/// Inserts an XML text child at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index at which to insert the child
///
/// # Returns
/// A pointer to the new YXmlText child
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeInsertTextWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
    index: jni::sys::jint,
) -> jlong {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        0
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    if index < 0 {
        throw_exception(&mut env, "Index cannot be negative");
        return 0;
    }

    use yrs::XmlTextPrelim;
    let new_text = element.insert(txn, index as u32, XmlTextPrelim::new(""));
    to_java_ptr(new_text)
}

/// Gets the child node at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index of the child to retrieve
///
/// # Returns
/// A Java Object array [type, pointer] where type is 0 for Element, 1 for Text, or null if not found
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetChildWithTxn<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
    index: jni::sys::jint,
) -> JObject<'a> {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", JObject::null());
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        JObject::null()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        JObject::null()
    );

    if index < 0 {
        throw_exception(&mut env, "Index cannot be negative");
        return JObject::null();
    }

    match element.get(txn, index as u32) {
        Some(child) => {
            use yrs::XmlOut;

            // Create Object array [type, pointer]
            let object_class = match env.find_class("java/lang/Object") {
                Ok(cls) => cls,
                Err(_) => {
                    throw_exception(&mut env, "Failed to find Object class");
                    return JObject::null();
                }
            };

            let array = match env.new_object_array(2, object_class, JObject::null()) {
                Ok(arr) => arr,
                Err(_) => {
                    throw_exception(&mut env, "Failed to create Object array");
                    return JObject::null();
                }
            };

            let (type_val, ptr) = match child {
                XmlOut::Element(elem) => (0i32, to_java_ptr(elem)),
                XmlOut::Text(text) => (1i32, to_java_ptr(text)),
                XmlOut::Fragment(_) => {
                    throw_exception(&mut env, "Unexpected XmlFragment as child");
                    return JObject::null();
                }
            };

            // Set type as Integer
            let integer_class = match env.find_class("java/lang/Integer") {
                Ok(cls) => cls,
                Err(_) => {
                    throw_exception(&mut env, "Failed to find Integer class");
                    return JObject::null();
                }
            };

            let type_obj = match env.new_object(
                integer_class,
                "(I)V",
                &[jni::objects::JValue::Int(type_val)],
            ) {
                Ok(obj) => obj,
                Err(_) => {
                    throw_exception(&mut env, "Failed to create Integer object");
                    return JObject::null();
                }
            };

            if env.set_object_array_element(&array, 0, &type_obj).is_err() {
                throw_exception(&mut env, "Failed to set type in array");
                return JObject::null();
            }

            // Set pointer as Long
            let long_class = match env.find_class("java/lang/Long") {
                Ok(cls) => cls,
                Err(_) => {
                    throw_exception(&mut env, "Failed to find Long class");
                    return JObject::null();
                }
            };

            let ptr_obj =
                match env.new_object(long_class, "(J)V", &[jni::objects::JValue::Long(ptr)]) {
                    Ok(obj) => obj,
                    Err(_) => {
                        throw_exception(&mut env, "Failed to create Long object");
                        return JObject::null();
                    }
                };

            if env.set_object_array_element(&array, 1, &ptr_obj).is_err() {
                throw_exception(&mut env, "Failed to set pointer in array");
                return JObject::null();
            }

            JObject::from(array)
        }
        None => JObject::null(),
    }
}

/// Removes the child node at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index of the child to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeRemoveChildWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
    index: jni::sys::jint,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement"
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    if index < 0 {
        throw_exception(&mut env, "Index cannot be negative");
        return;
    }

    element.remove(txn, index as u32);
}

/// Gets the parent node of this element using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A Java Object array [type, pointer] where type is 0 for Element, 1 for Fragment, or null if no parent
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetParentWithTxn<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
) -> JObject<'a> {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", JObject::null());
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        JObject::null()
    );
    let _txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        JObject::null()
    );

    match element.parent() {
        Some(parent) => {
            use yrs::XmlOut;

            // Create Object array [type, pointer]
            let object_class = match env.find_class("java/lang/Object") {
                Ok(cls) => cls,
                Err(_) => {
                    throw_exception(&mut env, "Failed to find Object class");
                    return JObject::null();
                }
            };

            let array = match env.new_object_array(2, object_class, JObject::null()) {
                Ok(arr) => arr,
                Err(_) => {
                    throw_exception(&mut env, "Failed to create Object array");
                    return JObject::null();
                }
            };

            let (type_val, ptr) = match parent {
                XmlOut::Element(elem) => (0i32, to_java_ptr(elem)),
                XmlOut::Fragment(frag) => (1i32, to_java_ptr(frag)),
                XmlOut::Text(_) => {
                    throw_exception(&mut env, "Unexpected XmlText as parent");
                    return JObject::null();
                }
            };

            // Set type as Integer
            let integer_class = match env.find_class("java/lang/Integer") {
                Ok(cls) => cls,
                Err(_) => {
                    throw_exception(&mut env, "Failed to find Integer class");
                    return JObject::null();
                }
            };

            let type_obj = match env.new_object(
                integer_class,
                "(I)V",
                &[jni::objects::JValue::Int(type_val)],
            ) {
                Ok(obj) => obj,
                Err(_) => {
                    throw_exception(&mut env, "Failed to create Integer object");
                    return JObject::null();
                }
            };

            if env.set_object_array_element(&array, 0, &type_obj).is_err() {
                throw_exception(&mut env, "Failed to set type in array");
                return JObject::null();
            }

            // Set pointer as Long
            let long_class = match env.find_class("java/lang/Long") {
                Ok(cls) => cls,
                Err(_) => {
                    throw_exception(&mut env, "Failed to find Long class");
                    return JObject::null();
                }
            };

            let ptr_obj =
                match env.new_object(long_class, "(J)V", &[jni::objects::JValue::Long(ptr)]) {
                    Ok(obj) => obj,
                    Err(_) => {
                        throw_exception(&mut env, "Failed to create Long object");
                        return JObject::null();
                    }
                };

            if env.set_object_array_element(&array, 1, &ptr_obj).is_err() {
                throw_exception(&mut env, "Failed to set pointer in array");
                return JObject::null();
            }

            JObject::from(array)
        }
        None => JObject::null(),
    }
}

/// Gets the index of this element within its parent's children using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// The index within parent, or -1 if no parent or not found
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeGetIndexInParentWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    txn_ptr: jlong,
) -> jni::sys::jint {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", -1);
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement",
        -1
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", -1);

    // Get parent and iterate through children to find index
    match element.parent() {
        Some(parent) => {
            use yrs::XmlOut;

            use yrs::branch::Branch;
            let my_id = <XmlElementRef as AsRef<Branch>>::as_ref(element).id();

            // Match on parent type and iterate children directly
            match parent {
                XmlOut::Element(elem) => {
                    // Iterate through parent's children to find our index
                    for index in 0..elem.len(txn) {
                        if let Some(child) = elem.get(txn, index) {
                            let child_id = child.as_ptr().id();
                            if child_id == my_id {
                                return index as jni::sys::jint;
                            }
                        }
                    }
                    -1
                }
                XmlOut::Fragment(frag) => {
                    // Iterate through parent's children to find our index
                    for index in 0..frag.len(txn) {
                        if let Some(child) = frag.get(txn, index) {
                            let child_id = child.as_ptr().id();
                            if child_id == my_id {
                                return index as jni::sys::jint;
                            }
                        }
                    }
                    -1
                }
                XmlOut::Text(_) => -1, // Text can't be a parent
            }
        }
        None => -1, // No parent
    }
}

/// Registers an observer for the YXmlElement
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance
/// - `subscription_id`: The subscription ID from Java
/// - `yxmlelement_obj`: The Java YXmlElement object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeObserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_element_ptr: jlong,
    subscription_id: jlong,
    yxmlelement_obj: JObject,
) {
    let wrapper = get_mut_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let element = get_ref_or_throw!(
        &mut env,
        XmlElementPtr::from_raw(xml_element_ptr),
        "YXmlElement"
    );

    // Get JavaVM and create Executor for callback handling
    let executor = match env.get_java_vm() {
        Ok(vm) => Executor::new(Arc::new(vm)),
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to get JavaVM: {:?}", e));
            return;
        }
    };

    // Create a global reference to the Java YXmlElement object
    let global_ref = match env.new_global_ref(yxmlelement_obj) {
        Ok(r) => r,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create global ref: {:?}", e));
            return;
        }
    };

    // Create observer closure
    let subscription = element.observe(move |txn, event| {
        // Use Executor for thread attachment with automatic local frame management
        let _ = executor.with_attached(|env| {
            dispatch_xmlelement_event(env, doc_ptr, subscription_id, txn, event)
        });
    });

    // Store subscription and GlobalRef in the DocWrapper
    wrapper.add_subscription(subscription_id, subscription, global_ref);
}

/// Unregisters an observer for the YXmlElement
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_element_ptr`: Pointer to the YXmlElement instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlElement_nativeUnobserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    _xml_element_ptr: jlong,
    subscription_id: jlong,
) {
    let wrapper = get_mut_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");

    // Remove subscription and GlobalRef from DocWrapper
    // Both the Subscription and GlobalRef are dropped here
    wrapper.remove_subscription(subscription_id);
}

/// Helper function to dispatch an XML element event to Java
fn dispatch_xmlelement_event(
    env: &mut JNIEnv,
    doc_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &XmlEvent,
) -> Result<(), jni::errors::Error> {
    // Get the Java YXmlElement object from DocWrapper
    let yxmlelement_ref = unsafe {
        let wrapper = from_java_ptr::<DocWrapper>(doc_ptr);
        match wrapper.get_java_ref(subscription_id) {
            Some(r) => r,
            None => {
                eprintln!("No Java object found for subscription {}", subscription_id);
                return Ok(());
            }
        }
    };

    let yxmlelement_obj = yxmlelement_ref.as_obj();

    // Create a Java ArrayList for changes
    let changes_list = env.new_object("java/util/ArrayList", "()V", &[])?;

    // Process child changes (using Change enum like YArray)
    let delta = event.delta(txn);
    for change in delta {
        let change_obj = match change {
            Change::Added(items) => {
                // Create YArrayChange for INSERT (children are like array items)
                let items_list = env.new_object("java/util/ArrayList", "()V", &[])?;
                for item in items {
                    let item_obj = out_to_jobject(env, item)?;
                    env.call_method(
                        &items_list,
                        "add",
                        "(Ljava/lang/Object;)Z",
                        &[JValue::Object(&item_obj)],
                    )?;
                }

                let change_class = env.find_class("net/carcdr/ycrdt/YArrayChange")?;
                env.new_object(
                    change_class,
                    "(Ljava/util/List;)V",
                    &[JValue::Object(&items_list)],
                )?
            }
            Change::Removed(len) => {
                // Create YArrayChange for DELETE
                let change_class = env.find_class("net/carcdr/ycrdt/YArrayChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let delete_type =
                    env.get_static_field(type_class, "DELETE", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;I)V",
                    &[JValue::Object(&delete_type.l()?), JValue::Int(*len as i32)],
                )?
            }
            Change::Retain(len) => {
                // Create YArrayChange for RETAIN
                let change_class = env.find_class("net/carcdr/ycrdt/YArrayChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let retain_type =
                    env.get_static_field(type_class, "RETAIN", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;I)V",
                    &[JValue::Object(&retain_type.l()?), JValue::Int(*len as i32)],
                )?
            }
        };

        // Add to changes list
        env.call_method(
            &changes_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&change_obj)],
        )?;
    }

    // Process attribute changes
    let keys = event.keys(txn);
    for (attr_name, change) in keys.iter() {
        use yrs::types::EntryChange;

        let attr_change_obj = match change {
            EntryChange::Inserted(new_val) => {
                let new_str = new_val.to_string();
                let attr_name_jstr = env.new_string(attr_name)?;
                let new_val_jstr = env.new_string(&new_str)?;

                let change_class = env.find_class("net/carcdr/ycrdt/YXmlElementChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let insert_type =
                    env.get_static_field(type_class, "INSERT", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    &[
                        JValue::Object(&insert_type.l()?),
                        JValue::Object(&attr_name_jstr),
                        JValue::Object(&new_val_jstr),
                        JValue::Object(&JObject::null()),
                    ],
                )?
            }
            EntryChange::Updated(old_val, new_val) => {
                let old_str = old_val.to_string();
                let new_str = new_val.to_string();
                let attr_name_jstr = env.new_string(attr_name)?;
                let old_val_jstr = env.new_string(&old_str)?;
                let new_val_jstr = env.new_string(&new_str)?;

                let change_class = env.find_class("net/carcdr/ycrdt/YXmlElementChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let attribute_type = env.get_static_field(
                    type_class,
                    "ATTRIBUTE",
                    "Lnet/carcdr/ycrdt/YChange$Type;",
                )?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    &[
                        JValue::Object(&attribute_type.l()?),
                        JValue::Object(&attr_name_jstr),
                        JValue::Object(&new_val_jstr),
                        JValue::Object(&old_val_jstr),
                    ],
                )?
            }
            EntryChange::Removed(old_val) => {
                let old_str = old_val.to_string();
                let attr_name_jstr = env.new_string(attr_name)?;
                let old_val_jstr = env.new_string(&old_str)?;

                let change_class = env.find_class("net/carcdr/ycrdt/YXmlElementChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let delete_type =
                    env.get_static_field(type_class, "DELETE", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    &[
                        JValue::Object(&delete_type.l()?),
                        JValue::Object(&attr_name_jstr),
                        JValue::Object(&JObject::null()),
                        JValue::Object(&old_val_jstr),
                    ],
                )?
            }
        };

        // Add to changes list
        env.call_method(
            &changes_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&attr_change_obj)],
        )?;
    }

    // Create YEvent
    let event_class = env.find_class("net/carcdr/ycrdt/YEvent")?;
    let target = yxmlelement_obj; // Use the YXmlElement object as the target
    let origin_jstr = env.new_string("")?; // Empty origin for now

    let event_obj = env.new_object(
        event_class,
        "(Ljava/lang/Object;Ljava/util/List;Ljava/lang/String;)V",
        &[
            JValue::Object(target),
            JValue::Object(&changes_list),
            JValue::Object(&origin_jstr),
        ],
    )?;

    // Call YXmlElement.dispatchEvent(subscriptionId, event)
    env.call_method(
        yxmlelement_obj,
        "dispatchEvent",
        "(JLnet/carcdr/ycrdt/YEvent;)V",
        &[JValue::Long(subscription_id), JValue::Object(&event_obj)],
    )?;

    Ok(())
}

/// Helper function to convert yrs Out to JObject
fn out_to_jobject<'local>(
    env: &mut JNIEnv<'local>,
    value: &yrs::Out,
) -> Result<JObject<'local>, jni::errors::Error> {
    match value {
        yrs::Out::Any(any) => any_to_jobject(env, any),
        yrs::Out::YText(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        yrs::Out::YArray(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        yrs::Out::YMap(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        yrs::Out::YXmlElement(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        yrs::Out::YXmlText(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        yrs::Out::YDoc(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        _ => {
            // For other types, convert to string
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
    }
}

/// Helper function to convert yrs Any to JObject
fn any_to_jobject<'local>(
    env: &mut JNIEnv<'local>,
    value: &yrs::Any,
) -> Result<JObject<'local>, jni::errors::Error> {
    use yrs::Any;

    match value {
        Any::String(s) => {
            let jstr = env.new_string(s.as_ref())?;
            Ok(jstr.into())
        }
        Any::Bool(b) => {
            let boolean_class = env.find_class("java/lang/Boolean")?;
            let obj = env.new_object(
                boolean_class,
                "(Z)V",
                &[JValue::Bool(if *b { 1 } else { 0 })],
            )?;
            Ok(obj)
        }
        Any::Number(n) => {
            let double_class = env.find_class("java/lang/Double")?;
            let obj = env.new_object(double_class, "(D)V", &[JValue::Double(*n)])?;
            Ok(obj)
        }
        Any::BigInt(i) => {
            let long_class = env.find_class("java/lang/Long")?;
            let obj = env.new_object(long_class, "(J)V", &[JValue::Long(*i)])?;
            Ok(obj)
        }
        _ => {
            // For other types, convert to string
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::free_java_ptr;
    use yrs::{Doc, Transact, XmlFragment, XmlFragmentRef};

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
        let tag = element.tag();
        assert_eq!(tag.as_ref(), "test");
        drop(txn);
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
            Some(yrs::Out::Any(yrs::Any::String("container".into())))
        );
        assert_eq!(
            element.get_attribute(&txn, "id"),
            Some(yrs::Out::Any(yrs::Any::String("main".into())))
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
            Some(yrs::Out::Any(yrs::Any::String("main".into())))
        );
    }
}
