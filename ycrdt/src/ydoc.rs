use crate::{
    free_if_valid, free_transaction, get_mut_or_throw, get_ref_or_throw, throw_exception,
    to_java_ptr, DocPtr, DocWrapper, JniEnvExt, JniResultExt, TxnPtr,
};
use jni::objects::{JByteArray, JClass, JObject, JValue};
use jni::sys::{jbyteArray, jlong, jstring};
use jni::{Executor, JNIEnv};
use std::sync::Arc;
use yrs::updates::decoder::Decode;
use yrs::updates::encoder::Encode;
use yrs::{ReadTxn, Transact};

/// Creates a new YDoc instance
///
/// # Returns
/// A pointer to the YDoc instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeCreate(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let doc = DocWrapper::new();
    to_java_ptr(doc)
}

/// Creates a new YDoc instance with a specific client ID
///
/// # Parameters
/// - `client_id`: The client ID to assign to this document
///
/// # Returns
/// A pointer to the YDoc instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeCreateWithClientId(
    _env: JNIEnv,
    _class: JClass,
    client_id: jlong,
) -> jlong {
    let options = yrs::Options {
        client_id: client_id as u64,
        ..Default::default()
    };
    let doc = DocWrapper::with_options(options);
    to_java_ptr(doc)
}

/// Destroys a YDoc instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
///
/// # Safety
/// The pointer must be valid and point to a YDoc instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    // When DocWrapper is dropped, all subscriptions and GlobalRefs are automatically cleaned up
    free_if_valid!(DocPtr::from_raw(ptr), DocWrapper);
}

/// Gets the client ID of a YDoc instance
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
///
/// # Returns
/// The client ID as jlong
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeGetClientId(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) -> jlong {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(ptr), "YDoc", 0);
    wrapper.doc.client_id() as jlong
}

/// Gets a unique identifier (GUID) for the YDoc instance
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
///
/// # Returns
/// A Java string containing the GUID
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeGetGuid(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) -> jstring {
    let wrapper = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let guid = wrapper.doc.guid().to_string();
    crate::to_jstring(&mut env, &guid)
}

/// Encodes the current state of the document as a byte array using an existing transaction
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `txn_ptr`: Pointer to the transaction instance
///
/// # Returns
/// A Java byte array containing the encoded state
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeEncodeStateAsUpdateWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    txn_ptr: jlong,
) -> jbyteArray {
    let _wrapper = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    // Encode against an empty state vector to get the full document state
    let empty_sv = yrs::StateVector::default();
    let update = txn.encode_state_as_update_v1(&empty_sv);

    env.create_byte_array(&update).unwrap_or_throw(&mut env)
}

/// Applies an update to the document from a byte array using an existing transaction
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `txn_ptr`: Pointer to the transaction instance
/// - `update`: Java byte array containing the update
///
/// # Safety
/// The `update` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeApplyUpdateWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    txn_ptr: jlong,
    update: jbyteArray,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(ptr), "YDoc");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert Java byte array to Rust Vec<u8>
    let update_array = JByteArray::from_raw(update);
    let update_bytes = match env.convert_byte_array(update_array) {
        Ok(bytes) => bytes,
        Err(_) => {
            throw_exception(&mut env, "Failed to convert byte array");
            return;
        }
    };

    match yrs::Update::decode_v1(&update_bytes) {
        Ok(update) => {
            if let Err(e) = txn.apply_update(update) {
                throw_exception(&mut env, &format!("Failed to apply update: {:?}", e));
            }
        }
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to decode update: {:?}", e));
        }
    }
}

/// Encodes the current state vector of the document using an existing transaction
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `txn_ptr`: Pointer to the transaction instance
///
/// # Returns
/// A Java byte array containing the encoded state vector
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeEncodeStateVectorWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    txn_ptr: jlong,
) -> jbyteArray {
    let _wrapper = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let state_vector = txn.state_vector();
    let encoded = state_vector.encode_v1();

    env.create_byte_array(&encoded).unwrap_or_throw(&mut env)
}

/// Encodes a differential update containing only changes not yet observed by the remote peer
/// using an existing transaction
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `txn_ptr`: Pointer to the transaction instance
/// - `state_vector`: Java byte array containing the remote peer's state vector
///
/// # Returns
/// A Java byte array containing the differential update
///
/// # Safety
/// The `state_vector` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeEncodeDiffWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    txn_ptr: jlong,
    state_vector: jbyteArray,
) -> jbyteArray {
    let _wrapper = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    // Convert Java byte array to Rust Vec<u8>
    let sv_array = JByteArray::from_raw(state_vector);
    let sv_bytes = match env.convert_byte_array(sv_array) {
        Ok(bytes) => bytes,
        Err(_) => {
            throw_exception(&mut env, "Failed to convert state vector byte array");
            return std::ptr::null_mut();
        }
    };

    // Decode the state vector
    let sv = match yrs::StateVector::decode_v1(&sv_bytes) {
        Ok(sv) => sv,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to decode state vector: {:?}", e));
            return std::ptr::null_mut();
        }
    };

    // Encode the differential update
    let diff = txn.encode_diff_v1(&sv);

    env.create_byte_array(&diff).unwrap_or_throw(&mut env)
}

/// Merges multiple updates into a single compact update
///
/// # Parameters
/// - `updates`: Java 2D byte array containing the updates to merge
///
/// # Returns
/// A Java byte array containing the merged update
///
/// # Safety
/// The `updates` parameter is a raw JNI object array pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeMergeUpdates(
    mut env: JNIEnv,
    _class: JClass,
    updates: jni::sys::jobjectArray,
) -> jbyteArray {
    use jni::objects::JObjectArray as JObjArray;

    // Convert Java 2D byte array to Vec<Vec<u8>>
    let updates_array = unsafe { JObjArray::from_raw(updates) };
    let len = match env.get_array_length(&updates_array) {
        Ok(l) => l,
        Err(_) => {
            throw_exception(&mut env, "Failed to get updates array length");
            return std::ptr::null_mut();
        }
    };

    let mut rust_updates: Vec<Vec<u8>> = Vec::with_capacity(len as usize);
    for i in 0..len {
        let update_obj = match env.get_object_array_element(&updates_array, i) {
            Ok(obj) => obj,
            Err(_) => {
                throw_exception(&mut env, &format!("Failed to get update at index {}", i));
                return std::ptr::null_mut();
            }
        };

        let update_array = JByteArray::from(update_obj);
        let update_bytes = match env.convert_byte_array(update_array) {
            Ok(bytes) => bytes,
            Err(_) => {
                throw_exception(
                    &mut env,
                    &format!("Failed to convert update at index {}", i),
                );
                return std::ptr::null_mut();
            }
        };

        rust_updates.push(update_bytes);
    }

    // Convert Vec<Vec<u8>> to Vec<&[u8]> for merge_updates_v1
    let update_refs: Vec<&[u8]> = rust_updates.iter().map(|v| v.as_slice()).collect();

    // Merge the updates
    let merged = match yrs::merge_updates_v1(&update_refs) {
        Ok(m) => m,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to merge updates: {:?}", e));
            return std::ptr::null_mut();
        }
    };

    env.create_byte_array(&merged).unwrap_or_throw(&mut env)
}

/// Extracts the state vector from an encoded update
///
/// # Parameters
/// - `update`: Java byte array containing the update
///
/// # Returns
/// A Java byte array containing the encoded state vector
///
/// # Safety
/// The `update` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeEncodeStateVectorFromUpdate(
    mut env: JNIEnv,
    _class: JClass,
    update: jbyteArray,
) -> jbyteArray {
    // Convert Java byte array to Rust Vec<u8>
    let update_array = JByteArray::from_raw(update);
    let update_bytes = match env.convert_byte_array(update_array) {
        Ok(bytes) => bytes,
        Err(_) => {
            throw_exception(&mut env, "Failed to convert update byte array");
            return std::ptr::null_mut();
        }
    };

    // Extract state vector from update
    let state_vector = match yrs::encode_state_vector_from_update_v1(&update_bytes) {
        Ok(sv) => sv,
        Err(e) => {
            throw_exception(
                &mut env,
                &format!("Failed to extract state vector from update: {:?}", e),
            );
            return std::ptr::null_mut();
        }
    };

    env.create_byte_array(&state_vector)
        .unwrap_or_throw(&mut env)
}

/// Begins a new transaction for batching operations
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
///
/// # Returns
/// A transaction ID (as jlong) that can be used to reference this transaction
///
/// # Safety
/// The doc pointer must be valid. The returned transaction ID must be committed
/// or rolled back to free the transaction resources.
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeBeginTransaction(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) -> jlong {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(ptr), "YDoc", 0);
    let txn = wrapper.doc.transact_mut();

    // Return raw transaction pointer
    Box::into_raw(Box::new(txn)) as jlong
}

/// Commits a transaction, applying all batched operations
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance (for validation)
/// - `txn_ptr`: Transaction ID returned from nativeBeginTransaction
///
/// # Safety
/// The transaction ID must be valid and not already committed/rolled back
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YTransaction_nativeCommit(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    txn_ptr: jlong,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let _txn = get_ref_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Free transaction - this will drop it and commit
    unsafe {
        free_transaction(txn_ptr);
    }
}

/// Rolls back a transaction, discarding all batched operations
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance (for validation)
/// - `txn_ptr`: Transaction ID returned from nativeBeginTransaction
///
/// # Safety
/// The transaction ID must be valid and not already committed/rolled back
///
/// # Note
/// The underlying yrs library may not support true rollback. Currently,
/// this behaves the same as commit.
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YTransaction_nativeRollback(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    txn_ptr: jlong,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let _txn = get_ref_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Free transaction
    // Note: yrs doesn't support true rollback - dropping the transaction commits it
    // In the future, we might need to track changes and implement manual rollback
    unsafe {
        free_transaction(txn_ptr);
    }
}

/// Registers an update observer for the YDoc
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `subscription_id`: The subscription ID from Java
/// - `ydoc_obj`: The Java YDoc object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeObserveUpdateV1(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    subscription_id: jlong,
    ydoc_obj: JObject,
) {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(ptr), "YDoc");

    // Get JavaVM and create Executor for callback handling
    let executor = match env.get_java_vm() {
        Ok(vm) => Executor::new(Arc::new(vm)),
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to get JavaVM: {:?}", e));
            return;
        }
    };

    // Create a global reference to the Java YDoc object
    let global_ref = match env.new_global_ref(ydoc_obj) {
        Ok(r) => r,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create global ref: {:?}", e));
            return;
        }
    };

    // Create observer closure
    let subscription = match wrapper.doc.observe_update_v1(move |_txn, event| {
        // Use Executor for thread attachment with automatic local frame management
        let _ = executor.with_attached(|env| {
            dispatch_update_event(env, ptr, subscription_id, event.update.as_ref())
        });
    }) {
        Ok(sub) => sub,
        Err(e) => {
            eprintln!("Failed to observe update: {:?}", e);
            return;
        }
    };

    // Store subscription and global ref in the DocWrapper
    wrapper.add_subscription(subscription_id, subscription, global_ref);
}

/// Unregisters an update observer for the YDoc
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeUnobserveUpdateV1(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    subscription_id: jlong,
) {
    let doc_ptr = DocPtr::from_raw(ptr);
    if doc_ptr.is_null() {
        return;
    }

    // Remove and drop subscription - this properly unregisters the observer
    if let Some(wrapper) = unsafe { doc_ptr.as_ref() } {
        wrapper.remove_subscription(subscription_id);
    }
}

/// Helper function to dispatch an update event to Java
fn dispatch_update_event(
    env: &mut JNIEnv,
    doc_ptr: jlong,
    subscription_id: jlong,
    update: &[u8],
) -> Result<(), jni::errors::Error> {
    // Convert update to Java byte array
    let update_array = env.byte_array_from_slice(update)?;

    // Get origin (if any) - yrs update events don't have origin, so we'll use null
    let origin_jstr = JObject::null();

    // Get the Java YDoc object from DocWrapper
    let ptr = DocPtr::from_raw(doc_ptr);
    let ydoc_ref = match unsafe { ptr.as_ref() } {
        Some(wrapper) => match wrapper.get_java_ref(subscription_id) {
            Some(r) => r,
            None => {
                eprintln!("No Java object found for subscription {}", subscription_id);
                return Ok(());
            }
        },
        None => {
            eprintln!("Invalid doc pointer in dispatch_update_event");
            return Ok(());
        }
    };

    let ydoc_obj = ydoc_ref.as_obj();

    // Call YDoc.onUpdateCallback(subscriptionId, update, origin)
    env.call_method(
        ydoc_obj,
        "onUpdateCallback",
        "(J[BLjava/lang/String;)V",
        &[
            JValue::Long(subscription_id),
            JValue::Object(&update_array),
            JValue::Object(&origin_jstr),
        ],
    )?;

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use yrs::{Text, Transact};

    #[test]
    fn test_doc_creation() {
        let wrapper = DocWrapper::new();
        let ptr = to_java_ptr(wrapper);
        assert_ne!(ptr, 0);

        free_if_valid!(DocPtr::from_raw(ptr), DocWrapper);
    }

    #[test]
    fn test_doc_with_client_id() {
        let options = yrs::Options {
            client_id: 12345,
            ..Default::default()
        };
        let wrapper = DocWrapper::with_options(options);
        assert_eq!(wrapper.doc.client_id(), 12345);
    }

    #[test]
    fn test_state_encoding() {
        let wrapper = DocWrapper::new();
        let text = wrapper.doc.get_or_insert_text("test");
        {
            let mut txn = wrapper.doc.transact_mut();
            text.push(&mut txn, "Hello, World!");
        }

        let txn = wrapper.doc.transact();
        let empty_sv = yrs::StateVector::default();
        let update = txn.encode_state_as_update_v1(&empty_sv);
        assert!(!update.is_empty());
    }
}
