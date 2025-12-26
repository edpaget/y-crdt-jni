use crate::{
    free_if_valid, get_mut_or_throw, get_ref_or_throw, throw_exception, to_java_ptr, to_jstring,
    DocPtr, JniEnvExt, TextPtr, TxnPtr,
};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring};
use jni::{Executor, JNIEnv};
use std::sync::Arc;
use yrs::types::text::TextEvent;
use yrs::{GetString, Observable, Text, TextRef, TransactionMut};

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
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);

    // Convert Java string to Rust string
    let name_str = match env.get_rust_string(&name) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return 0;
        }
    };

    let text = wrapper.doc.get_or_insert_text(name_str.as_str());
    to_java_ptr(text)
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
    free_if_valid!(TextPtr::from_raw(ptr), TextRef);
}

/// Gets the length of the text with an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `txn_ptr`: Pointer to the transaction instance
///
/// # Returns
/// The length of the text as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeLengthWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    text_ptr: jlong,
    txn_ptr: jlong,
) -> jint {
    let text = get_ref_or_throw!(&mut env, TextPtr::from_raw(text_ptr), "YText", 0);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    text.len(txn) as jint
}

/// Gets the string content of the text using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `txn_ptr`: Pointer to the transaction instance
///
/// # Returns
/// A Java string containing the text content
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeToStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    text_ptr: jlong,
    txn_ptr: jlong,
) -> jstring {
    let text = get_ref_or_throw!(
        &mut env,
        TextPtr::from_raw(text_ptr),
        "YText",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let content = text.get_string(txn);
    to_jstring(&mut env, &content)
}

/// Inserts text at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `index`: The index at which to insert the text
/// - `chunk`: The text to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeInsertWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    text_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    chunk: JString,
) {
    let text = get_ref_or_throw!(&mut env, TextPtr::from_raw(text_ptr), "YText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert Java string to Rust string
    let chunk_str = match env.get_rust_string(&chunk) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    text.insert(txn, index as u32, &chunk_str);
}

/// Appends text to the end using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `chunk`: The text to append
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativePushWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    text_ptr: jlong,
    txn_ptr: jlong,
    chunk: JString,
) {
    let text = get_ref_or_throw!(&mut env, TextPtr::from_raw(text_ptr), "YText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert Java string to Rust string
    let chunk_str = match env.get_rust_string(&chunk) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    text.push(txn, &chunk_str);
}

/// Deletes a range of text using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `index`: The starting index
/// - `length`: The number of characters to delete
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeDeleteWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    text_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    length: jint,
) {
    let text = get_ref_or_throw!(&mut env, TextPtr::from_raw(text_ptr), "YText");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    text.remove_range(txn, index as u32, length as u32);
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
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let text = get_ref_or_throw!(&mut env, TextPtr::from_raw(text_ptr), "YText");

    // Get JavaVM and create Executor for callback handling
    let executor = match env.get_java_vm() {
        Ok(vm) => Executor::new(Arc::new(vm)),
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

    // Create observer closure
    let subscription = text.observe(move |txn, event| {
        // Use Executor for thread attachment with automatic local frame management
        let _ = executor
            .with_attached(|env| dispatch_text_event(env, doc_ptr, subscription_id, txn, event));
    });

    // Store subscription and GlobalRef in the DocWrapper
    wrapper.add_subscription(subscription_id, subscription, global_ref);
}

/// Unregisters an observer for the YText
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `text_ptr`: Pointer to the YText instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YText_nativeUnobserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    _text_ptr: jlong,
    subscription_id: jlong,
) {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");

    // Remove subscription and GlobalRef from DocWrapper
    // Both the Subscription and GlobalRef are dropped here
    wrapper.remove_subscription(subscription_id);
}

/// Helper function to dispatch a text event to Java
fn dispatch_text_event(
    env: &mut JNIEnv,
    doc_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &TextEvent,
) -> Result<(), jni::errors::Error> {
    // Get the Java YText object from DocWrapper
    let wrapper = match unsafe { DocPtr::from_raw(doc_ptr).as_ref() } {
        Some(w) => w,
        None => {
            eprintln!("Invalid YDoc pointer in dispatch_text_event");
            return Ok(());
        }
    };
    let ytext_ref = match wrapper.get_java_ref(subscription_id) {
        Some(r) => r,
        None => {
            eprintln!("No Java object found for subscription {}", subscription_id);
            return Ok(());
        }
    };

    let ytext_obj = ytext_ref.as_obj();

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
                    create_java_hashmap(env, attrs)?
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
                    create_java_hashmap(env, attrs)?
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
