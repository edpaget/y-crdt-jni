use crate::{
    free_if_valid, from_java_ptr, get_mut_or_throw, get_ref_or_throw, get_string_or_throw,
    out_to_jobject, throw_exception, to_java_ptr, to_jstring, ArrayPtr, DocPtr, DocWrapper,
    JniEnvExt, TxnPtr,
};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jdouble, jint, jlong, jstring};
use jni::{Executor, JNIEnv};
use std::sync::Arc;
use yrs::types::array::ArrayEvent;
use yrs::types::{Change, ToJson};
use yrs::{Array, ArrayRef, Doc, Observable, TransactionMut};

/// Gets or creates a YArray instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the array object in the document
///
/// # Returns
/// A pointer to the YArray instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeGetArray(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    name: JString,
) -> jlong {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let name_str = get_string_or_throw!(&mut env, name, 0);

    let array = wrapper.doc.get_or_insert_array(name_str.as_str());
    to_java_ptr(array)
}

/// Destroys a YArray instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YArray instance
///
/// # Safety
/// The pointer must be valid and point to a YArray instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    free_if_valid!(ArrayPtr::from_raw(ptr), ArrayRef);
}

/// Gets the length of the array using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// The length of the array as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeLengthWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
) -> jint {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray", 0);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    array.len(txn) as jint
}

/// Gets a string value from the array at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index to get from
///
/// # Returns
/// A Java string, or null if index is out of bounds or value is not a string
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeGetStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
) -> jstring {
    let _doc = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let array = get_ref_or_throw!(
        &mut env,
        ArrayPtr::from_raw(array_ptr),
        "YArray",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    match array.get(txn, index as u32) {
        Some(value) => {
            let s = value.to_string(txn);
            to_jstring(&mut env, &s)
        }
        None => std::ptr::null_mut(),
    }
}

/// Gets a double value from the array at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index to get from
///
/// # Returns
/// The double value, or 0.0 if index is out of bounds or value is not a number
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeGetDoubleWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
) -> jdouble {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0.0);
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray", 0.0);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0.0);

    match array.get(txn, index as u32) {
        Some(value) => value.cast::<f64>().unwrap_or(0.0),
        None => 0.0,
    }
}

/// Inserts a string value at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `index`: The index at which to insert
/// - `value`: The string value to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeInsertStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    value: JString,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");
    let value_str = get_string_or_throw!(&mut env, value);

    array.insert(txn, index as u32, value_str);
}

/// Inserts a double value at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `index`: The index at which to insert
/// - `value`: The double value to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeInsertDoubleWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    value: jdouble,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    array.insert(txn, index as u32, value);
}

/// Pushes a string value to the end of the array using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `value`: The string value to push
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativePushStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    value: JString,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");
    let value_str = get_string_or_throw!(&mut env, value);

    array.push_back(txn, value_str);
}

/// Pushes a double value to the end of the array using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `value`: The double value to push
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativePushDoubleWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    value: jdouble,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    array.push_back(txn, value);
}

/// Removes a range of elements from the array using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `index`: The starting index
/// - `length`: The number of elements to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeRemoveWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    length: jint,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    array.remove_range(txn, index as u32, length as u32);
}

/// Converts the array to a JSON string representation using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A JSON string representation of the array
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeToJsonWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
) -> jstring {
    let _doc = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let array = get_ref_or_throw!(
        &mut env,
        ArrayPtr::from_raw(array_ptr),
        "YArray",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let json = array.to_json(txn).to_string();
    to_jstring(&mut env, &json)
}

/// Inserts a YDoc subdocument at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `index`: The index at which to insert
/// - `subdoc_ptr`: Pointer to the YDoc subdocument to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeInsertDocWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    subdoc_ptr: jlong,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");
    // subdoc_ptr comes from Java YDoc which stores DocWrapper, not raw Doc
    let subdoc_wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(subdoc_ptr), "subdocument");

    // Clone the inner doc for insertion (Doc implements Prelim)
    let subdoc_clone = subdoc_wrapper.doc.clone();
    array.insert(txn, index as u32, subdoc_clone);
}

/// Pushes a YDoc subdocument to the end of the array using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction ID
/// - `subdoc_ptr`: Pointer to the YDoc subdocument to push
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativePushDocWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    subdoc_ptr: jlong,
) {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");
    // subdoc_ptr comes from Java YDoc which stores DocWrapper, not raw Doc
    let subdoc_wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(subdoc_ptr), "subdocument");

    // Clone the inner doc for insertion (Doc implements Prelim)
    let subdoc_clone = subdoc_wrapper.doc.clone();
    array.push_back(txn, subdoc_clone);
}

/// Gets a YDoc subdocument from the array at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `array_ptr`: Pointer to the YArray instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index to get from
///
/// # Returns
/// A pointer to the YDoc subdocument, or 0 if index is out of bounds or value is not a Doc
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeGetDocWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
) -> jlong {
    let _doc = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray", 0);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    match array.get(txn, index as u32) {
        Some(value) => {
            // Try to cast to Doc
            match value.cast::<Doc>() {
                // Wrap in DocWrapper so nativeDestroy can properly free it
                Ok(subdoc) => to_java_ptr(DocWrapper::from_doc(subdoc.clone())),
                Err(_) => 0,
            }
        }
        None => 0,
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
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeObserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    array_ptr: jlong,
    subscription_id: jlong,
    yarray_obj: JObject,
) {
    let wrapper = get_mut_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let array = get_ref_or_throw!(&mut env, ArrayPtr::from_raw(array_ptr), "YArray");

    // Get JavaVM and create Executor for callback handling
    let executor = match env.get_java_vm() {
        Ok(vm) => Executor::new(Arc::new(vm)),
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

    // Create observer closure
    let subscription = array.observe(move |txn, event| {
        // Use Executor for thread attachment with automatic local frame management
        let _ = executor
            .with_attached(|env| dispatch_array_event(env, doc_ptr, subscription_id, txn, event));
    });

    // Store subscription and GlobalRef in the DocWrapper
    wrapper.add_subscription(subscription_id, subscription, global_ref);
}

/// Unregisters an observer for the YArray
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `array_ptr`: Pointer to the YArray instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYArray_nativeUnobserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    _array_ptr: jlong,
    subscription_id: jlong,
) {
    let wrapper = get_mut_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");

    // Remove subscription and GlobalRef from DocWrapper
    // Both the Subscription and GlobalRef are dropped here
    wrapper.remove_subscription(subscription_id);
}

/// Helper function to dispatch an array event to Java
fn dispatch_array_event(
    env: &mut JNIEnv,
    doc_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &ArrayEvent,
) -> Result<(), jni::errors::Error> {
    // Get the Java YArray object from DocWrapper
    let yarray_ref = unsafe {
        let wrapper = from_java_ptr::<DocWrapper>(doc_ptr);
        match wrapper.get_java_ref(subscription_id) {
            Some(r) => r,
            None => {
                eprintln!("No Java object found for subscription {}", subscription_id);
                return Ok(());
            }
        }
    };

    let yarray_obj = yarray_ref.as_obj();

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

                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYArrayChange")?;
                env.new_object(
                    change_class,
                    "(Ljava/util/List;)V",
                    &[JValue::Object(&items_list)],
                )?
            }
            Change::Removed(len) => {
                // Create YArrayChange for DELETE
                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYArrayChange")?;
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
                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYArrayChange")?;
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

    // Create YEvent
    let event_class = env.find_class("net/carcdr/ycrdt/jni/JniYEvent")?;
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
        "(JLnet/carcdr/ycrdt/jni/JniYEvent;)V",
        &[JValue::Long(subscription_id), JValue::Object(&event_obj)],
    )?;

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::free_java_ptr;
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
