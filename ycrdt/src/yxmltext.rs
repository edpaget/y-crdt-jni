use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{GlobalRef, JClass, JMap, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring};
use jni::{AttachGuard, JNIEnv};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use yrs::types::xml::XmlTextEvent;
use yrs::{
    Any, Doc, GetString, Observable, Text, Transact, TransactionMut, Xml, XmlFragment,
    XmlTextPrelim, XmlTextRef,
};

// Global storage for Java YXmlText objects (needed for callbacks)
lazy_static::lazy_static! {
    static ref XMLTEXT_JAVA_OBJECTS: Arc<Mutex<HashMap<jlong, GlobalRef>>> =
        Arc::new(Mutex::new(HashMap::new()));
}

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

/// Inserts text with formatting attributes at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `index`: The index at which to insert the text
/// - `chunk`: The text to insert
/// - `attributes`: A Java Map<String, Object> of formatting attributes
///
/// # Safety
/// The `attributes` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeInsertWithAttributes(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    index: jint,
    chunk: JString,
    attributes: JObject,
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

    // Convert Java Map to Rust HashMap<Arc<str>, Any>
    let attrs = match convert_java_map_to_attrs(&mut env, &attributes) {
        Ok(attrs) => attrs,
        Err(e) => {
            throw_exception(&mut env, &e);
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let mut txn = doc.transact_mut();

        text.insert_with_attributes(&mut txn, index as u32, &chunk_str, attrs);
    }
}

/// Formats a range of text with the specified attributes
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
/// - `index`: The starting index of the range to format
/// - `length`: The length of the range to format
/// - `attributes`: A Java Map<String, Object> of formatting attributes.
///   Use null value to remove formatting
///
/// # Safety
/// The `attributes` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeFormat(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
    index: jint,
    length: jint,
    attributes: JObject,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return;
    }

    // Convert Java Map to Rust HashMap<Arc<str>, Any>
    let attrs = match convert_java_map_to_attrs(&mut env, &attributes) {
        Ok(attrs) => attrs,
        Err(e) => {
            throw_exception(&mut env, &e);
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let mut txn = doc.transact_mut();

        text.format(&mut txn, index as u32, length as u32, attrs);
    }
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

/// Gets the parent of this XML text node
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
///
/// # Returns
///
/// An Object array [type, pointer] where:
/// - type: 0 = XmlElement, 1 = XmlFragment
/// - pointer: Java pointer to the parent object
///
/// Returns null if this node has no parent
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeGetParent<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
) -> JObject<'a> {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return JObject::null();
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return JObject::null();
    }

    unsafe {
        let _doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);

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
}

/// Gets the index of this XML text node within its parent
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `xml_text_ptr`: Pointer to the YXmlText instance
///
/// # Returns
/// The 0-based index of this node within its parent's children,
/// or -1 if this node has no parent or the index could not be determined
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeGetIndexInParent(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    xml_text_ptr: jlong,
) -> jni::sys::jint {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return -1;
    }
    if xml_text_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return -1;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<XmlTextRef>(xml_text_ptr);
        let txn = doc.transact();

        match text.parent() {
            Some(parent) => {
                use yrs::XmlOut;

                use yrs::branch::Branch;
                let my_id = <XmlTextRef as AsRef<Branch>>::as_ref(text).id();

                // Match on parent type and iterate children directly
                match parent {
                    XmlOut::Element(elem) => {
                        // Iterate through parent's children to find our index
                        for index in 0..elem.len(&txn) {
                            if let Some(child) = elem.get(&txn, index) {
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
                        for index in 0..frag.len(&txn) {
                            if let Some(child) = frag.get(&txn, index) {
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
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if xmltext_ptr == 0 {
        throw_exception(&mut env, "Invalid YXmlText pointer");
        return;
    }

    // Get JavaVM for later callback
    let jvm = match env.get_java_vm() {
        Ok(vm) => Arc::new(vm),
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

    // Store the global reference
    {
        let mut java_objects = XMLTEXT_JAVA_OBJECTS.lock().unwrap();
        java_objects.insert(subscription_id, global_ref);
    }

    unsafe {
        let _doc = from_java_ptr::<Doc>(doc_ptr);
        let xmltext = from_java_ptr::<XmlTextRef>(xmltext_ptr);

        // Create observer closure
        let jvm_clone = Arc::clone(&jvm);
        let subscription = xmltext.observe(move |txn, event| {
            // Attach to JVM for this thread
            let mut env = match jvm_clone.attach_current_thread() {
                Ok(env) => env,
                Err(_) => return, // Can't do much if we can't attach
            };

            // Dispatch event to Java
            if let Err(e) =
                dispatch_xmltext_event(&mut env, xmltext_ptr, subscription_id, txn, event)
            {
                eprintln!("Failed to dispatch xmltext event: {:?}", e);
            }
        });

        // Leak the subscription to keep it alive - we'll clean up on unobserve
        // This is a simplified approach; in production we'd use a better mechanism
        Box::leak(Box::new(subscription));
    }
}

/// Unregisters an observer for the YXmlText
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance (unused but kept for consistency)
/// - `xmltext_ptr`: Pointer to the YXmlText instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YXmlText_nativeUnobserve(
    _env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    _xmltext_ptr: jlong,
    subscription_id: jlong,
) {
    // Remove the global reference to allow the Java object to be GC'd
    let mut java_objects = XMLTEXT_JAVA_OBJECTS.lock().unwrap();
    java_objects.remove(&subscription_id);
    // The GlobalRef is dropped here, releasing the reference
    // Note: The Subscription is still leaked from observe()
    // This is acceptable for now but should be fixed in production
}

/// Helper function to dispatch an xmltext event to Java
fn dispatch_xmltext_event(
    env: &mut AttachGuard,
    _xmltext_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &XmlTextEvent,
) -> Result<(), jni::errors::Error> {
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
                    create_java_hashmap_from_attrs(env, &**attrs)?
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
                    create_java_hashmap_from_attrs(env, &**attrs)?
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

    // Get the Java YXmlText object from our global storage
    let java_objects = XMLTEXT_JAVA_OBJECTS.lock().unwrap();
    let yxmltext_ref = match java_objects.get(&subscription_id) {
        Some(r) => r,
        None => {
            eprintln!("No Java object found for subscription {}", subscription_id);
            return Ok(());
        }
    };

    let yxmltext_obj = yxmltext_ref.as_obj();

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
    env: &mut AttachGuard<'local>,
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
    env: &mut AttachGuard<'local>,
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
