use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::sys::{jdouble, jint, jlong, jstring};
use jni::{AttachGuard, JNIEnv};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use yrs::types::array::ArrayEvent;
use yrs::types::{Change, ToJson};
use yrs::{Array, ArrayRef, Doc, Observable, Out, Transact, TransactionMut};

// Global storage for Java YArray objects (needed for callbacks)
lazy_static::lazy_static! {
    static ref ARRAY_JAVA_OBJECTS: Arc<Mutex<HashMap<jlong, GlobalRef>>> =
        Arc::new(Mutex::new(HashMap::new()));
}

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

/// Inserts a YDoc subdocument at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `index`: The index at which to insert
/// - `subdoc_ptr`: Pointer to the YDoc subdocument to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeInsertDoc(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    index: jint,
    subdoc_ptr: jlong,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return;
    }
    if subdoc_ptr == 0 {
        throw_exception(&mut env, "Invalid subdocument pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let subdoc = from_java_ptr::<Doc>(subdoc_ptr);

        // Clone the subdoc for insertion (Doc implements Prelim)
        let subdoc_clone = subdoc.clone();

        let mut txn = doc.transact_mut();
        array.insert(&mut txn, index as u32, subdoc_clone);
    }
}

/// Pushes a YDoc subdocument to the end of the array
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `subdoc_ptr`: Pointer to the YDoc subdocument to push
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativePushDoc(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    subdoc_ptr: jlong,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
        return;
    }
    if subdoc_ptr == 0 {
        throw_exception(&mut env, "Invalid subdocument pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);
        let subdoc = from_java_ptr::<Doc>(subdoc_ptr);

        // Clone the subdoc for insertion (Doc implements Prelim)
        let subdoc_clone = subdoc.clone();

        let mut txn = doc.transact_mut();
        array.push_back(&mut txn, subdoc_clone);
    }
}

/// Gets a YDoc subdocument from the array at the specified index
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `index`: The index to get from
///
/// # Returns
/// A pointer to the YDoc subdocument, or 0 if index is out of bounds or value is not a Doc
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeGetDoc(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    index: jint,
) -> jlong {
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

        match array.get(&txn, index as u32) {
            Some(value) => {
                // Try to cast to Doc
                match value.cast::<Doc>() {
                    Ok(subdoc) => to_java_ptr(subdoc.clone()),
                    Err(_) => 0,
                }
            }
            None => 0,
        }
    }
}

/// Registers an observer for the YArray
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `subscription_id`: The subscription ID from Java
/// - `yarray_obj`: The Java YArray object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeObserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    subscription_id: jlong,
    yarray_obj: JObject,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if array_ptr == 0 {
        throw_exception(&mut env, "Invalid YArray pointer");
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

    // Create a global reference to the Java YArray object
    let global_ref = match env.new_global_ref(yarray_obj) {
        Ok(r) => r,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create global ref: {:?}", e));
            return;
        }
    };

    // Store the global reference
    {
        let mut java_objects = ARRAY_JAVA_OBJECTS.lock().unwrap();
        java_objects.insert(subscription_id, global_ref);
    }

    unsafe {
        let _doc = from_java_ptr::<Doc>(doc_ptr);
        let array = from_java_ptr::<ArrayRef>(array_ptr);

        // Create observer closure
        let jvm_clone = Arc::clone(&jvm);
        let subscription = array.observe(move |txn, event| {
            // Attach to JVM for this thread
            let mut env = match jvm_clone.attach_current_thread() {
                Ok(env) => env,
                Err(_) => return, // Can't do much if we can't attach
            };

            // Dispatch event to Java
            if let Err(e) = dispatch_array_event(&mut env, array_ptr, subscription_id, txn, event) {
                eprintln!("Failed to dispatch array event: {:?}", e);
            }
        });

        // Leak the subscription to keep it alive - we'll clean up on unobserve
        // This is a simplified approach; in production we'd use a better mechanism
        Box::leak(Box::new(subscription));
    }
}

/// Unregisters an observer for the YArray
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance (unused but kept for consistency)
/// - `array_ptr`: Pointer to the YArray instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YArray_nativeUnobserve(
    _env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    _array_ptr: jlong,
    subscription_id: jlong,
) {
    // Remove the global reference to allow the Java object to be GC'd
    let mut java_objects = ARRAY_JAVA_OBJECTS.lock().unwrap();
    java_objects.remove(&subscription_id);
    // The GlobalRef is dropped here, releasing the reference
    // Note: The Subscription is still leaked from observe()
    // This is acceptable for now but should be fixed in production
}

/// Helper function to dispatch an array event to Java
fn dispatch_array_event(
    env: &mut AttachGuard,
    array_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &ArrayEvent,
) -> Result<(), jni::errors::Error> {
    // Get the delta
    let delta = event.delta(txn);

    // Create a Java ArrayList for changes
    let changes_list = env.new_object("java/util/ArrayList", "()V", &[])?;

    // Convert each Change to a YArrayChange
    for change in delta {
        let change_obj = match change {
            Change::Added(items) => {
                // Create YArrayChange for INSERT
                // Convert items to Java ArrayList
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
            Change::Retain(len) => {
                // Create YArrayChange for RETAIN
                let change_class = env.find_class("net/carcdr/ycrdt/YArrayChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let retain_type = env.get_static_field(
                    type_class,
                    "RETAIN",
                    "Lnet/carcdr/ycrdt/YChange$Type;",
                )?;

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

    // Get the Java YArray object from our global storage
    let java_objects = ARRAY_JAVA_OBJECTS.lock().unwrap();
    let yarray_ref = match java_objects.get(&subscription_id) {
        Some(r) => r,
        None => {
            eprintln!("No Java object found for subscription {}", subscription_id);
            return Ok(());
        }
    };

    let yarray_obj = yarray_ref.as_obj();

    // Create YEvent
    let event_class = env.find_class("net/carcdr/ycrdt/YEvent")?;
    let target = yarray_obj; // Use the YArray object as the target
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

    // Call YArray.dispatchEvent(subscriptionId, event)
    env.call_method(
        yarray_obj,
        "dispatchEvent",
        "(JLnet/carcdr/ycrdt/YEvent;)V",
        &[JValue::Long(subscription_id), JValue::Object(&event_obj)],
    )?;

    Ok(())
}

/// Helper function to convert yrs Out to JObject
fn out_to_jobject<'local>(
    env: &mut AttachGuard<'local>,
    value: &Out,
) -> Result<JObject<'local>, jni::errors::Error> {
    match value {
        Out::Any(any) => any_to_jobject(env, any),
        Out::YText(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        Out::YArray(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        Out::YMap(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        Out::YXmlElement(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        Out::YXmlText(_) => {
            // For now, return string representation
            let s = value.to_string();
            let jstr = env.new_string(&s)?;
            Ok(jstr.into())
        }
        Out::YDoc(_) => {
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

    #[test]
    fn test_array_subdocument_push() {
        let doc = Doc::new();
        let array = doc.get_or_insert_array("test");
        let subdoc = Doc::new();

        // Push subdocument
        {
            let mut txn = doc.transact_mut();
            array.push_back(&mut txn, subdoc.clone());
        }

        // Retrieve subdocument
        let txn = doc.transact();
        assert_eq!(array.len(&txn), 1);
        let retrieved = array.get(&txn, 0);
        assert!(retrieved.is_some());

        let retrieved_doc = retrieved.unwrap().cast::<Doc>();
        assert!(retrieved_doc.is_ok());
    }

    #[test]
    fn test_array_subdocument_insert() {
        let doc = Doc::new();
        let array = doc.get_or_insert_array("test");
        let subdoc1 = Doc::new();
        let subdoc2 = Doc::new();

        // Push first subdocument, insert second at beginning
        {
            let mut txn = doc.transact_mut();
            array.push_back(&mut txn, subdoc1.clone());
            array.insert(&mut txn, 0, subdoc2.clone());
        }

        // Verify order
        let txn = doc.transact();
        assert_eq!(array.len(&txn), 2);

        let first = array.get(&txn, 0).unwrap().cast::<Doc>();
        assert!(first.is_ok());

        let second = array.get(&txn, 1).unwrap().cast::<Doc>();
        assert!(second.is_ok());
    }
}
