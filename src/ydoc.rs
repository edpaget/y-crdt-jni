use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr};
use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jlong, jstring};
use jni::JNIEnv;
use yrs::updates::decoder::Decode;
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
        let mut options = yrs::Options::default();
        options.client_id = 12345;
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
