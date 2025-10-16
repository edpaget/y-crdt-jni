use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring};
use jni::{AttachGuard, JNIEnv, JavaVM};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use yrs::types::text::TextEvent;
use yrs::{Doc, GetString, Observable, Subscription, Text, TextRef, Transact, TransactionMut};

// Global storage for Java YText objects (needed for callbacks)
lazy_static::lazy_static! {
    static ref TEXT_JAVA_OBJECTS: Arc<Mutex<HashMap<jlong, GlobalRef>>> =
        Arc::new(Mutex::new(HashMap::new()));
}

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

/// Registers an observer for the YText
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `subscription_id`: The subscription ID from Java
/// - `ytext_obj`: The Java YText object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeObserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    text_ptr: jlong,
    subscription_id: jlong,
    ytext_obj: JObject,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if text_ptr == 0 {
        throw_exception(&mut env, "Invalid YText pointer");
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

    // Create a global reference to the Java YText object
    let global_ref = match env.new_global_ref(ytext_obj) {
        Ok(r) => r,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create global ref: {:?}", e));
            return;
        }
    };

    // Store the global reference
    {
        let mut java_objects = TEXT_JAVA_OBJECTS.lock().unwrap();
        java_objects.insert(subscription_id, global_ref);
    }

    unsafe {
        let _doc = from_java_ptr::<Doc>(doc_ptr);
        let text = from_java_ptr::<TextRef>(text_ptr);

        // Create observer closure
        let jvm_clone = Arc::clone(&jvm);
        let subscription = text.observe(move |txn, event| {
            // Attach to JVM for this thread
            let mut env = match jvm_clone.attach_current_thread() {
                Ok(env) => env,
                Err(_) => return, // Can't do much if we can't attach
            };

            // Dispatch event to Java
            if let Err(e) = dispatch_text_event(&mut env, text_ptr, subscription_id, txn, event) {
                eprintln!("Failed to dispatch text event: {:?}", e);
            }
        });

        // Leak the subscription to keep it alive - we'll clean up on unobserve
        // This is a simplified approach; in production we'd use a better mechanism
        Box::leak(Box::new(subscription));
    }
}

/// Unregisters an observer for the YText
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance (unused but kept for consistency)
/// - `text_ptr`: Pointer to the YText instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeUnobserve(
    _env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    _text_ptr: jlong,
    subscription_id: jlong,
) {
    // Remove the global reference to allow the Java object to be GC'd
    let mut java_objects = TEXT_JAVA_OBJECTS.lock().unwrap();
    java_objects.remove(&subscription_id);
    // The GlobalRef is dropped here, releasing the reference
    // Note: The Subscription is still leaked from observe()
    // This is acceptable for now but should be fixed in production
}

/// Helper function to dispatch a text event to Java
fn dispatch_text_event(
    env: &mut AttachGuard,
    text_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &TextEvent,
) -> Result<(), jni::errors::Error> {
    // Get the delta
    let delta = event.delta(txn);

    // Create a Java ArrayList for changes
    let changes_list = env.new_object("java/util/ArrayList", "()V", &[])?;

    // Convert each delta to a YTextChange
    for d in delta {
        let change_obj = match d {
            yrs::types::Delta::Inserted(value, attrs) => {
                // Convert value to string
                let content = value.to_string();
                let content_jstr = env.new_string(&content)?;

                // Convert attributes to HashMap (or null)
                let attrs_map = if let Some(attrs) = attrs {
                    create_java_hashmap(env, &**attrs)?
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
                let delete_type = env.get_static_field(
                    type_class,
                    "DELETE",
                    "Lnet/carcdr/ycrdt/YChange$Type;",
                )?;

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
                let retain_type = env.get_static_field(
                    type_class,
                    "RETAIN",
                    "Lnet/carcdr/ycrdt/YChange$Type;",
                )?;

                let attrs_map = if let Some(attrs) = attrs {
                    create_java_hashmap(env, &**attrs)?
                } else {
                    JObject::null()
                };

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;ILjava/util/Map;)V",
                    &[JValue::Object(&retain_type.l()?), JValue::Int(*len as i32), JValue::Object(&attrs_map)],
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

    // Get the Java YText object from our global storage
    let java_objects = TEXT_JAVA_OBJECTS.lock().unwrap();
    let ytext_ref = match java_objects.get(&subscription_id) {
        Some(r) => r,
        None => {
            eprintln!("No Java object found for subscription {}", subscription_id);
            return Ok(());
        }
    };

    let ytext_obj = ytext_ref.as_obj();

    // Create YEvent
    let event_class = env.find_class("net/carcdr/ycrdt/YEvent")?;
    let target = ytext_obj; // Use the YText object as the target
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

    // Call YText.dispatchEvent(subscriptionId, event)
    env.call_method(
        ytext_obj,
        "dispatchEvent",
        "(JLnet/carcdr/ycrdt/YEvent;)V",
        &[JValue::Long(subscription_id), JValue::Object(&event_obj)],
    )?;

    Ok(())
}

/// Helper function to create a Java HashMap from yrs Attrs
fn create_java_hashmap<'local>(
    env: &mut AttachGuard<'local>,
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

/// Helper function to convert yrs Any to JObject
fn any_to_jobject<'local>(env: &mut AttachGuard<'local>, value: &yrs::Any) -> Result<JObject<'local>, jni::errors::Error> {
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
