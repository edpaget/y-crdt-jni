use crate::{
    free_if_valid, from_java_ptr, get_mut_or_throw, get_ref_or_throw, throw_exception, to_java_ptr,
    to_jstring, DocPtr, DocWrapper, TxnPtr, XmlTextPtr,
};
use jni::objects::{JClass, JMap, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring};
use jni::{Executor, JNIEnv};
use std::collections::HashMap;
use std::sync::Arc;
use yrs::types::xml::XmlTextEvent;
use yrs::{
    Any, GetString, Observable, Text, Transact, TransactionMut, Xml, XmlFragment, XmlTextPrelim,
    XmlTextRef,
};

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

    // Ensure the fragment has a text child at index 0
    {
        let txn = wrapper.doc.transact();
        if fragment.len(&txn) == 0 {
            drop(txn);
            let mut txn = wrapper.doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
        }
    }

    // Return a pointer to the text at index 0, not the fragment
    let txn = wrapper.doc.transact();
    if let Some(child) = fragment.get(&txn, 0) {
        if let Some(text) = child.into_xml_text() {
            return to_java_ptr(text);
        }
    }
    0
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
    free_if_valid!(XmlTextPtr::from_raw(ptr), XmlTextRef);
}

/// Gets the length of the XML text (number of characters) using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// The length of the text as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeLengthWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
) -> jint {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let text = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xml_text_ptr), "YXmlText", 0);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    text.len(txn) as jint
}

/// Returns the string representation of the XML text using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A Java string containing the text content
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeToStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
) -> jstring {
    let _doc = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let text = get_ref_or_throw!(
        &mut env,
        XmlTextPtr::from_raw(xml_text_ptr),
        "YXmlText",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let string = text.get_string(txn);
    to_jstring(&mut env, &string)
}

/// Inserts text at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index at which to insert the text
/// - `chunk`: The text to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeInsertWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    chunk: JString,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let text = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xml_text_ptr), "YXmlText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert chunk to Rust string
    let chunk_str: String = match env.get_string(&chunk) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get chunk string");
            return;
        }
    };

    text.insert(txn, index as u32, &chunk_str);
}

/// Appends text to the end using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
/// - `chunk`: The text to append
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativePushWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
    chunk: JString,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let text = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xml_text_ptr), "YXmlText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert chunk to Rust string
    let chunk_str: String = match env.get_string(&chunk) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get chunk string");
            return;
        }
    };

    text.push(txn, &chunk_str);
}

/// Deletes a range of text using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The starting index of the deletion
/// - `length`: The number of characters to delete
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeDeleteWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    length: jint,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let text = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xml_text_ptr), "YXmlText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    text.remove_range(txn, index as u32, length as u32);
}

/// Inserts text with formatting attributes at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index at which to insert the text
/// - `chunk`: The text to insert
/// - `attributes`: A Java Map<String, Object> of formatting attributes
///
/// # Safety
/// The `attributes` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeInsertWithAttributesWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    chunk: JString,
    attributes: JObject,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let text = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xml_text_ptr), "YXmlText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert chunk to Rust string
    let chunk_str: String = match env.get_string(&chunk) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get chunk string");
            return;
        }
    };

    // Convert Java Map to Rust HashMap<Arc<str>, Any>
    let attrs = match convert_java_map_to_attrs(&mut env, &attributes) {
        Ok(attrs) => attrs,
        Err(e) => {
            throw_exception(&mut env, &e);
            return;
        }
    };

    text.insert_with_attributes(txn, index as u32, &chunk_str, attrs);
}

/// Formats a range of text with the specified attributes using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The starting index of the range to format
/// - `length`: The length of the range to format
/// - `attributes`: A Java Map<String, Object> of formatting attributes.
///   Use null value to remove formatting
///
/// # Safety
/// The `attributes` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeFormatWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    length: jint,
    attributes: JObject,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let text = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xml_text_ptr), "YXmlText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert Java Map to Rust HashMap<Arc<str>, Any>
    let attrs = match convert_java_map_to_attrs(&mut env, &attributes) {
        Ok(attrs) => attrs,
        Err(e) => {
            throw_exception(&mut env, &e);
            return;
        }
    };

    text.format(txn, index as u32, length as u32, attrs);
}

/// Helper function to convert a Java Map<String, Object> to Rust HashMap<Arc<str>, Any>
fn convert_java_map_to_attrs(
    env: &mut JNIEnv,
    java_map: &JObject,
) -> Result<HashMap<Arc<str>, Any>, String> {
    let mut attrs = HashMap::new();

    // Get the Map interface
    let map = JMap::from_env(env, java_map).map_err(|e| format!("Failed to get map: {:?}", e))?;

    // Iterate through the map entries
    let mut iter = map
        .iter(env)
        .map_err(|e| format!("Failed to iterate map: {:?}", e))?;

    while let Some((key, value)) = iter
        .next(env)
        .map_err(|e| format!("Failed to get next entry: {:?}", e))?
    {
        // Get the key as String
        let key_jstring = JString::from(key);
        let key_str: String = env
            .get_string(&key_jstring)
            .map_err(|e| format!("Failed to get key string: {:?}", e))?
            .into();

        // Convert the value to yrs::Any
        let any_value = if value.is_null() {
            Any::Null
        } else {
            // Check the type of the value
            let value_class = env
                .get_object_class(&value)
                .map_err(|e| format!("Failed to get value class: {:?}", e))?;

            let class_name = env
                .call_method(&value_class, "getName", "()Ljava/lang/String;", &[])
                .map_err(|e| format!("Failed to get class name: {:?}", e))?;

            let class_name_obj = class_name
                .l()
                .map_err(|e| format!("Failed to get class name object: {:?}", e))?;
            let class_name_str: String = env
                .get_string(&JString::from(class_name_obj))
                .map_err(|e| format!("Failed to convert class name: {:?}", e))?
                .into();

            match class_name_str.as_str() {
                "java.lang.Boolean" => {
                    let bool_val = env
                        .call_method(&value, "booleanValue", "()Z", &[])
                        .map_err(|e| format!("Failed to get boolean value: {:?}", e))?;
                    Any::Bool(
                        bool_val
                            .z()
                            .map_err(|e| format!("Failed to convert to bool: {:?}", e))?,
                    )
                }
                "java.lang.Integer" | "java.lang.Long" => {
                    let long_val = env
                        .call_method(&value, "longValue", "()J", &[])
                        .map_err(|e| format!("Failed to get long value: {:?}", e))?;
                    Any::BigInt(
                        long_val
                            .j()
                            .map_err(|e| format!("Failed to convert to long: {:?}", e))?,
                    )
                }
                "java.lang.Double" | "java.lang.Float" => {
                    let double_val = env
                        .call_method(&value, "doubleValue", "()D", &[])
                        .map_err(|e| format!("Failed to get double value: {:?}", e))?;
                    Any::Number(
                        double_val
                            .d()
                            .map_err(|e| format!("Failed to convert to double: {:?}", e))?,
                    )
                }
                "java.lang.String" => {
                    let string_val = JString::from(value);
                    let rust_str: String = env
                        .get_string(&string_val)
                        .map_err(|e| format!("Failed to get string value: {:?}", e))?
                        .into();
                    Any::String(rust_str.into())
                }
                _ => {
                    // Try to convert to string as fallback
                    let string_val = env
                        .call_method(&value, "toString", "()Ljava/lang/String;", &[])
                        .map_err(|e| format!("Failed to call toString: {:?}", e))?;
                    let string_obj = string_val
                        .l()
                        .map_err(|e| format!("Failed to get string object: {:?}", e))?;
                    let rust_str: String = env
                        .get_string(&JString::from(string_obj))
                        .map_err(|e| format!("Failed to convert to string: {:?}", e))?
                        .into();
                    Any::String(rust_str.into())
                }
            }
        };

        attrs.insert(Arc::from(key_str.as_str()), any_value);
    }

    Ok(attrs)
}

/// Gets the parent of this XML text node using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
///
/// An Object array [type, pointer] where:
/// - type: 0 = XmlElement, 1 = XmlFragment
/// - pointer: Java pointer to the parent object
///
/// Returns null if this node has no parent
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeGetParentWithTxn<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    _txn_ptr: jlong,
) -> JObject<'a> {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", JObject::null());
    let text = get_ref_or_throw!(
        &mut env,
        XmlTextPtr::from_raw(xml_text_ptr),
        "YXmlText",
        JObject::null()
    );

    match text.parent() {
        Some(parent) => {
            use yrs::XmlOut;

            // Create Object array [type, pointer]
            // type: 0=Element, 1=Fragment
            let (type_val, ptr) = match parent {
                XmlOut::Element(elem) => (0i32, to_java_ptr(elem)),
                XmlOut::Fragment(frag) => (1i32, to_java_ptr(frag)),
                XmlOut::Text(_) => {
                    throw_exception(&mut env, "Unexpected XmlText as parent");
                    return JObject::null();
                }
            };

            // Create Object array
            let array = match env.new_object_array(2, "java/lang/Object", JObject::null()) {
                Ok(arr) => arr,
                Err(e) => {
                    throw_exception(&mut env, &format!("Failed to create array: {:?}", e));
                    return JObject::null();
                }
            };

            // Set type (Integer)
            let type_obj = match env.new_object(
                "java/lang/Integer",
                "(I)V",
                &[jni::objects::JValueGen::Int(type_val)],
            ) {
                Ok(obj) => obj,
                Err(e) => {
                    throw_exception(&mut env, &format!("Failed to create Integer: {:?}", e));
                    return JObject::null();
                }
            };

            if let Err(e) = env.set_object_array_element(&array, 0, type_obj) {
                throw_exception(&mut env, &format!("Failed to set type: {:?}", e));
                return JObject::null();
            }

            // Set pointer (Long)
            let ptr_obj = match env.new_object(
                "java/lang/Long",
                "(J)V",
                &[jni::objects::JValueGen::Long(ptr)],
            ) {
                Ok(obj) => obj,
                Err(e) => {
                    throw_exception(&mut env, &format!("Failed to create Long: {:?}", e));
                    return JObject::null();
                }
            };

            if let Err(e) = env.set_object_array_element(&array, 1, ptr_obj) {
                throw_exception(&mut env, &format!("Failed to set pointer: {:?}", e));
                return JObject::null();
            }

            JObject::from(array)
        }
        None => JObject::null(),
    }
}

/// Gets the index of this XML text node within its parent using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// The 0-based index of this node within its parent's children,
/// or -1 if this node has no parent or the index could not be determined
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeGetIndexInParentWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
) -> jni::sys::jint {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", -1);
    let text = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xml_text_ptr), "YXmlText", -1);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", -1);

    match text.parent() {
        Some(parent) => {
            use yrs::XmlOut;

            use yrs::branch::Branch;
            let my_id = <XmlTextRef as AsRef<Branch>>::as_ref(text).id();

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

/// Registers an observer for the YXmlText
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xmltext_ptr`: Pointer to the YXmlText instance
/// - `subscription_id`: The subscription ID from Java
/// - `yxmltext_obj`: The Java YXmlText object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeObserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xmltext_ptr: jlong,
    subscription_id: jlong,
    yxmltext_obj: JObject,
) {
    let wrapper = get_mut_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let xmltext = get_ref_or_throw!(&mut env, XmlTextPtr::from_raw(xmltext_ptr), "YXmlText");

    // Get JavaVM and create Executor for callback handling
    let executor = match env.get_java_vm() {
        Ok(vm) => Executor::new(Arc::new(vm)),
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to get JavaVM: {:?}", e));
            return;
        }
    };

    // Create a global reference to the Java YXmlText object
    let global_ref = match env.new_global_ref(yxmltext_obj) {
        Ok(r) => r,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create global ref: {:?}", e));
            return;
        }
    };

    // Create observer closure
    let subscription = xmltext.observe(move |txn, event| {
        // Use Executor for thread attachment with automatic local frame management
        let _ = executor
            .with_attached(|env| dispatch_xmltext_event(env, doc_ptr, subscription_id, txn, event));
    });

    // Store subscription and GlobalRef in the DocWrapper
    wrapper.add_subscription(subscription_id, subscription, global_ref);
}

/// Unregisters an observer for the YXmlText
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xmltext_ptr`: Pointer to the YXmlText instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeUnobserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    _xmltext_ptr: jlong,
    subscription_id: jlong,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }

    unsafe {
        let wrapper = from_java_ptr::<DocWrapper>(doc_ptr);
        // Remove subscription and GlobalRef from DocWrapper
        // Both the Subscription and GlobalRef are dropped here
        wrapper.remove_subscription(subscription_id);
    }
}

/// Helper function to dispatch an xmltext event to Java
fn dispatch_xmltext_event(
    env: &mut JNIEnv,
    doc_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &XmlTextEvent,
) -> Result<(), jni::errors::Error> {
    // Get the Java YXmlText object from DocWrapper
    let yxmltext_ref = unsafe {
        let wrapper = from_java_ptr::<DocWrapper>(doc_ptr);
        match wrapper.get_java_ref(subscription_id) {
            Some(r) => r,
            None => {
                eprintln!("No Java object found for subscription {}", subscription_id);
                return Ok(());
            }
        }
    };

    let yxmltext_obj = yxmltext_ref.as_obj();

    // Get the delta (XmlTextEvent uses Delta enum, same as Text)
    let delta = event.delta(txn);

    // Create a Java ArrayList for changes
    let changes_list = env.new_object("java/util/ArrayList", "()V", &[])?;

    // Convert each delta to a YTextChange (XmlText uses same delta as Text)
    for d in delta {
        let change_obj = match d {
            yrs::types::Delta::Inserted(value, attrs) => {
                // Convert value to string
                let content = value.to_string();
                let content_jstr = env.new_string(&content)?;

                // Convert attributes to HashMap (or null)
                let attrs_map = if let Some(attrs) = attrs {
                    create_java_hashmap_from_attrs(env, attrs)?
                } else {
                    JObject::null()
                };

                // Create YTextChange for INSERT
                let change_class = env.find_class("net/carcdr/ycrdt/YTextChange")?;
                env.new_object(
                    change_class,
                    "(Ljava/lang/String;Ljava/util/Map;)V",
                    &[JValue::Object(&content_jstr), JValue::Object(&attrs_map)],
                )?
            }
            yrs::types::Delta::Deleted(len) => {
                // Create YTextChange for DELETE
                let change_class = env.find_class("net/carcdr/ycrdt/YTextChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let delete_type =
                    env.get_static_field(type_class, "DELETE", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;I)V",
                    &[JValue::Object(&delete_type.l()?), JValue::Int(*len as i32)],
                )?
            }
            yrs::types::Delta::Retain(len, attrs) => {
                // Create YTextChange for RETAIN
                let change_class = env.find_class("net/carcdr/ycrdt/YTextChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let retain_type =
                    env.get_static_field(type_class, "RETAIN", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                let attrs_map = if let Some(attrs) = attrs {
                    create_java_hashmap_from_attrs(env, attrs)?
                } else {
                    JObject::null()
                };

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;ILjava/util/Map;)V",
                    &[
                        JValue::Object(&retain_type.l()?),
                        JValue::Int(*len as i32),
                        JValue::Object(&attrs_map),
                    ],
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

    // Create YEvent
    let event_class = env.find_class("net/carcdr/ycrdt/YEvent")?;
    let target = yxmltext_obj; // Use the YXmlText object as the target
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

    // Call YXmlText.dispatchEvent(subscriptionId, event)
    env.call_method(
        yxmltext_obj,
        "dispatchEvent",
        "(JLnet/carcdr/ycrdt/YEvent;)V",
        &[JValue::Long(subscription_id), JValue::Object(&event_obj)],
    )?;

    Ok(())
}

/// Helper function to create a Java HashMap from yrs Attrs
fn create_java_hashmap_from_attrs<'local>(
    env: &mut JNIEnv<'local>,
    attrs: &yrs::types::Attrs,
) -> Result<JObject<'local>, jni::errors::Error> {
    let hashmap = env.new_object("java/util/HashMap", "()V", &[])?;

    for (key, value) in attrs.iter() {
        let key_jstr = env.new_string(key)?;
        let value_obj = any_to_jobject_xmltext(env, value)?;

        env.call_method(
            &hashmap,
            "put",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            &[JValue::Object(&key_jstr), JValue::Object(&value_obj)],
        )?;
    }

    Ok(hashmap)
}

/// Helper function to convert yrs Any to JObject (for XmlText)
fn any_to_jobject_xmltext<'local>(
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

/// Gets the formatting chunks (delta) of the XML text using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A Java List<FormattingChunk> containing the text chunks with their formatting attributes
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeGetFormattingChunksWithTxn<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    txn_ptr: jlong,
) -> JObject<'local> {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", JObject::null());
    let text = get_ref_or_throw!(
        &mut env,
        XmlTextPtr::from_raw(xml_text_ptr),
        "YXmlText",
        JObject::null()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        JObject::null()
    );

    // Get the diff (chunks of text with formatting)
    let diff = text.diff(txn, yrs::types::text::YChange::identity);

    // Create a Java ArrayList to hold FormattingChunk objects
    let chunks_list = match env.new_object("java/util/ArrayList", "()V", &[]) {
        Ok(list) => list,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create ArrayList: {:?}", e));
            return JObject::null();
        }
    };

    // Convert each diff chunk to a FormattingChunk
    for d in diff {
        // Get the text content from insert field
        let text_str = d.insert.to_string(txn);
        let text_jstr = match env.new_string(&text_str) {
            Ok(s) => s,
            Err(e) => {
                throw_exception(&mut env, &format!("Failed to create text string: {:?}", e));
                return JObject::null();
            }
        };

        // Convert attributes to HashMap (or null if no attributes)
        let attrs_map = if let Some(attrs) = d.attributes {
            match convert_attrs_to_java_hashmap(&mut env, &attrs) {
                Ok(map) => map,
                Err(e) => {
                    throw_exception(&mut env, &format!("Failed to convert attributes: {:?}", e));
                    return JObject::null();
                }
            }
        } else {
            JObject::null()
        };

        // Create FormattingChunk(text, attributes)
        let chunk_class = match env.find_class("net/carcdr/ycrdt/FormattingChunk") {
            Ok(cls) => cls,
            Err(e) => {
                throw_exception(
                    &mut env,
                    &format!("Failed to find FormattingChunk class: {:?}", e),
                );
                return JObject::null();
            }
        };

        let chunk_obj = match env.new_object(
            chunk_class,
            "(Ljava/lang/String;Ljava/util/Map;)V",
            &[JValue::Object(&text_jstr), JValue::Object(&attrs_map)],
        ) {
            Ok(obj) => obj,
            Err(e) => {
                throw_exception(
                    &mut env,
                    &format!("Failed to create FormattingChunk: {:?}", e),
                );
                return JObject::null();
            }
        };

        // Add to list
        if let Err(e) = env.call_method(
            &chunks_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&chunk_obj)],
        ) {
            throw_exception(&mut env, &format!("Failed to add chunk to list: {:?}", e));
            return JObject::null();
        }
    }

    chunks_list
}

/// Helper function to convert yrs Attrs to Java HashMap for FormattingChunk
fn convert_attrs_to_java_hashmap<'local>(
    env: &mut JNIEnv<'local>,
    attrs: &yrs::types::Attrs,
) -> Result<JObject<'local>, jni::errors::Error> {
    let hashmap = env.new_object("java/util/HashMap", "()V", &[])?;

    for (key, value) in attrs.iter() {
        let key_jstr = env.new_string(key)?;
        let value_obj = any_to_jobject(env, value)?;

        env.call_method(
            &hashmap,
            "put",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            &[JValue::Object(&key_jstr), JValue::Object(&value_obj)],
        )?;
    }

    Ok(hashmap)
}

/// Helper function to convert yrs Any to JObject (for standard JNIEnv)
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

    #[test]
    fn test_xml_text_format() {
        use yrs::types::Attrs;

        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            let text = fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
            text.insert(&mut txn, 0, "hello ");
            text.insert(&mut txn, 6, "world");

            // Format "hello" with bold
            let bold = Attrs::from([("b".into(), true.into())]);
            text.format(&mut txn, 0, 5, bold);
        }

        let txn = doc.transact();
        let text = fragment.get(&txn, 0).unwrap().into_xml_text().unwrap();
        assert_eq!(text.get_string(&txn), "<b>hello</b> world");
    }

    #[test]
    fn test_xml_text_insert_with_attributes() {
        use yrs::types::Attrs;

        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            let text = fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));
            text.insert(&mut txn, 0, "hello ");

            // Insert "world" with italic formatting
            let italic = Attrs::from([("i".into(), true.into())]);
            text.insert_with_attributes(&mut txn, 6, "world", italic);
        }

        let txn = doc.transact();
        let text = fragment.get(&txn, 0).unwrap().into_xml_text().unwrap();
        assert_eq!(text.get_string(&txn), "hello <i>world</i>");
    }

    #[test]
    fn test_xml_text_remove_format() {
        use yrs::types::Attrs;

        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            let text = fragment.insert(&mut txn, 0, XmlTextPrelim::new(""));

            // Insert with italic
            let italic = Attrs::from([("i".into(), true.into())]);
            text.insert_with_attributes(&mut txn, 0, "world", italic);

            // Remove italic formatting
            let remove_italic = Attrs::from([("i".into(), Any::Null)]);
            text.format(&mut txn, 0, 5, remove_italic);
        }

        let txn = doc.transact();
        let text = fragment.get(&txn, 0).unwrap().into_xml_text().unwrap();
        assert_eq!(text.get_string(&txn), "world");
    }
}
