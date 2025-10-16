use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr};
use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jlong, jstring};
use jni::JNIEnv;
use yrs::updates::decoder::Decode;
use yrs::updates::encoder::Encode;
use yrs::{Doc, ReadTxn, Transact};

/// Creates a new YDoc instance
///
/// # Returns
/// A pointer to the YDoc instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeCreate(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let doc = Doc::new();
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
    let doc = Doc::with_options(options);
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
    if ptr != 0 {
        unsafe {
            free_java_ptr::<Doc>(ptr);
        }
    }
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
    if ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(ptr);
        doc.client_id() as jlong
    }
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
    if ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(ptr);
        let guid = doc.guid().to_string();
        crate::to_jstring(&mut env, &guid)
    }
}

/// Encodes the current state of the document as a byte array
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
///
/// # Returns
/// A Java byte array containing the encoded state
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeEncodeStateAsUpdate(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) -> jbyteArray {
    if ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(ptr);
        let txn = doc.transact();
        // Encode against an empty state vector to get the full document state
        let empty_sv = yrs::StateVector::default();
        let update = txn.encode_state_as_update_v1(&empty_sv);

        match env.byte_array_from_slice(&update) {
            Ok(arr) => arr.into_raw(),
            Err(_) => {
                throw_exception(&mut env, "Failed to create byte array");
                std::ptr::null_mut()
            }
        }
    }
}

/// Applies an update to the document from a byte array
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `update`: Java byte array containing the update
///
/// # Safety
/// The `update` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeApplyUpdate(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    update: jbyteArray,
) {
    if ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }

    // Convert Java byte array to Rust Vec<u8>
    let update_array = JByteArray::from_raw(update);
    let update_bytes = match env.convert_byte_array(update_array) {
        Ok(bytes) => bytes,
        Err(_) => {
            throw_exception(&mut env, "Failed to convert byte array");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(ptr);

        // Apply the update
        let mut txn = doc.transact_mut();
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
}

/// Encodes the current state vector of the document
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
///
/// # Returns
/// A Java byte array containing the encoded state vector
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeEncodeStateVector(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) -> jbyteArray {
    if ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(ptr);
        let txn = doc.transact();
        let state_vector = txn.state_vector();
        let encoded = state_vector.encode_v1();

        match env.byte_array_from_slice(&encoded) {
            Ok(arr) => arr.into_raw(),
            Err(_) => {
                throw_exception(&mut env, "Failed to create byte array");
                std::ptr::null_mut()
            }
        }
    }
}

/// Encodes a differential update containing only changes not yet observed by the remote peer
///
/// # Parameters
/// - `ptr`: Pointer to the YDoc instance
/// - `state_vector`: Java byte array containing the remote peer's state vector
///
/// # Returns
/// A Java byte array containing the differential update
///
/// # Safety
/// The `state_vector` parameter is a raw JNI pointer that must be valid
#[no_mangle]
pub unsafe extern "system" fn Java_net_carcdr_ycrdt_YDoc_nativeEncodeDiff(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    state_vector: jbyteArray,
) -> jbyteArray {
    if ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }

    // Convert Java byte array to Rust Vec<u8>
    let sv_array = JByteArray::from_raw(state_vector);
    let sv_bytes = match env.convert_byte_array(sv_array) {
        Ok(bytes) => bytes,
        Err(_) => {
            throw_exception(&mut env, "Failed to convert state vector byte array");
            return std::ptr::null_mut();
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(ptr);

        // Decode the state vector
        let sv = match yrs::StateVector::decode_v1(&sv_bytes) {
            Ok(sv) => sv,
            Err(e) => {
                throw_exception(&mut env, &format!("Failed to decode state vector: {:?}", e));
                return std::ptr::null_mut();
            }
        };

        // Encode the differential update
        let txn = doc.transact();
        let diff = txn.encode_diff_v1(&sv);

        match env.byte_array_from_slice(&diff) {
            Ok(arr) => arr.into_raw(),
            Err(_) => {
                throw_exception(&mut env, "Failed to create byte array");
                std::ptr::null_mut()
            }
        }
    }
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

    match env.byte_array_from_slice(&merged) {
        Ok(arr) => arr.into_raw(),
        Err(_) => {
            throw_exception(&mut env, "Failed to create byte array");
            std::ptr::null_mut()
        }
    }
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

    match env.byte_array_from_slice(&state_vector) {
        Ok(arr) => arr.into_raw(),
        Err(_) => {
            throw_exception(&mut env, "Failed to create byte array");
            std::ptr::null_mut()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use yrs::{Text, Transact};

    #[test]
    fn test_doc_creation() {
        let doc = Doc::new();
        let ptr = to_java_ptr(doc);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<Doc>(ptr);
        }
    }

    #[test]
    fn test_doc_with_client_id() {
        let options = yrs::Options {
            client_id: 12345,
            ..Default::default()
        };
        let doc = Doc::with_options(options);
        assert_eq!(doc.client_id(), 12345);
    }

    #[test]
    fn test_state_encoding() {
        let doc = Doc::new();
        let text = doc.get_or_insert_text("test");
        {
            let mut txn = doc.transact_mut();
            text.push(&mut txn, "Hello, World!");
        }

        let txn = doc.transact();
        let empty_sv = yrs::StateVector::default();
        let update = txn.encode_state_as_update_v1(&empty_sv);
        assert!(!update.is_empty());
    }
}
