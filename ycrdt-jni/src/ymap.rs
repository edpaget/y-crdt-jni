use crate::{
    free_if_valid, from_java_ptr, get_mut_or_throw, get_ref_or_throw, throw_exception, to_java_ptr,
    to_jstring, DocPtr, DocWrapper, JniEnvExt, MapPtr, TxnPtr,
};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jdouble, jlong, jstring};
use jni::{Executor, JNIEnv};
use std::sync::Arc;
use yrs::types::map::MapEvent;
use yrs::types::{EntryChange, ToJson};
use yrs::{Doc, Map, MapRef, Observable, Out, TransactionMut};

/// Gets or creates a YMap instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the map object in the document
///
/// # Returns
/// A pointer to the YMap instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeGetMap(
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

    let map = wrapper.doc.get_or_insert_map(name_str.as_str());
    to_java_ptr(map)
}

/// Destroys a YMap instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YMap instance
///
/// # Safety
/// The pointer must be valid and point to a YMap instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    free_if_valid!(MapPtr::from_raw(ptr), MapRef);
}

/// Gets the size of the map (number of entries) with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// The size of the map as jlong
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeSizeWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
) -> jlong {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap", 0);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    map.len(txn) as jlong
}

/// Gets a string value from the map by key with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to the transaction
/// - `key`: The key to look up
///
/// # Returns
/// A Java string, or null if key not found or value is not a string
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeGetStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
) -> jstring {
    let _wrapper = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let map = get_ref_or_throw!(
        &mut env,
        MapPtr::from_raw(map_ptr),
        "YMap",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return std::ptr::null_mut();
        }
    };

    match map.get(txn, &key_str) {
        Some(value) => {
            let s = value.to_string(txn);
            to_jstring(&mut env, &s)
        }
        None => std::ptr::null_mut(),
    }
}

/// Gets a double value from the map by key with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to the transaction
/// - `key`: The key to look up
///
/// # Returns
/// The double value, or 0.0 if key not found or value is not a number
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeGetDoubleWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
) -> jdouble {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0.0);
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap", 0.0);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0.0);

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return 0.0;
        }
    };

    match map.get(txn, &key_str) {
        Some(value) => value.cast::<f64>().unwrap_or(0.0),
        None => 0.0,
    }
}

/// Sets a string value in the map with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction
/// - `key`: The key to set
/// - `value`: The string value to set
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeSetStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
    value: JString,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    // Convert value to Rust string
    let value_str = match env.get_rust_string(&value) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    map.insert(txn, key_str, value_str);
}

/// Sets a double value in the map with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction
/// - `key`: The key to set
/// - `value`: The double value to set
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeSetDoubleWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
    value: jdouble,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    map.insert(txn, key_str, value);
}

/// Removes a key from the map with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction
/// - `key`: The key to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeRemoveWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    map.remove(txn, &key_str);
}

/// Checks if a key exists in the map with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to the transaction
/// - `key`: The key to check
///
/// # Returns
/// true if the key exists, false otherwise
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeContainsKeyWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
) -> bool {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", false);
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap", false);
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", false);

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return false;
        }
    };

    map.contains_key(txn, &key_str)
}

/// Gets all keys from the map as a Java array with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A Java String[] array containing all keys
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeKeysWithTxn<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
) -> JObject<'a> {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", JObject::null());
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap", JObject::null());
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        JObject::null()
    );

    // Collect all keys
    let keys: Vec<String> = map.keys(txn).map(|k| k.to_string()).collect();

    // Create Java String array
    let string_class = match env.find_class("java/lang/String") {
        Ok(cls) => cls,
        Err(_) => {
            throw_exception(&mut env, "Failed to find String class");
            return JObject::null();
        }
    };

    let array = match env.new_object_array(keys.len() as i32, string_class, JObject::null()) {
        Ok(arr) => arr,
        Err(_) => {
            throw_exception(&mut env, "Failed to create String array");
            return JObject::null();
        }
    };

    // Fill the array
    for (i, key) in keys.iter().enumerate() {
        let jkey = match env.new_string(key) {
            Ok(s) => s,
            Err(_) => {
                throw_exception(&mut env, "Failed to create Java string");
                return JObject::null();
            }
        };
        if env
            .set_object_array_element(&array, i as i32, &jkey)
            .is_err()
        {
            throw_exception(&mut env, "Failed to set array element");
            return JObject::null();
        }
    }

    JObject::from(array)
}

/// Clears all entries from the map with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeClearWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    map.clear(txn);
}

/// Converts the map to a JSON string representation with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A JSON string representation of the map
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeToJsonWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
) -> jstring {
    let _wrapper = get_ref_or_throw!(
        &mut env,
        DocPtr::from_raw(doc_ptr),
        "YDoc",
        std::ptr::null_mut()
    );
    let map = get_ref_or_throw!(
        &mut env,
        MapPtr::from_raw(map_ptr),
        "YMap",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let json = map.to_json(txn).to_string();
    to_jstring(&mut env, &json)
}

/// Sets a YDoc subdocument value in the map with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction
/// - `key`: The key to set
/// - `subdoc_ptr`: Pointer to the YDoc subdocument to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeSetDocWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
    subdoc_ptr: jlong,
) {
    let _wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let map = get_ref_or_throw!(&mut env, MapPtr::from_raw(map_ptr), "YMap");
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");
    let subdoc_wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(subdoc_ptr), "subdocument");

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    // Clone the inner doc for insertion (Doc implements Prelim)
    let subdoc_clone = subdoc_wrapper.doc.clone();

    map.insert(txn, key_str, subdoc_clone);
}

/// Gets a YDoc subdocument value from the map by key with transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to the transaction
/// - `key`: The key to look up
///
/// # Returns
/// A pointer to the YDoc subdocument, or 0 if key not found or value is not a Doc
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeGetDocWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
) -> jlong {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return 0;
    }
    if txn_ptr == 0 {
        throw_exception(&mut env, "Invalid transaction pointer");
        return 0;
    }

    // Convert key to Rust string
    let key_str = match env.get_rust_string(&key) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return 0;
        }
    };

    unsafe {
        let map = from_java_ptr::<MapRef>(map_ptr);
        match crate::get_transaction_mut(txn_ptr) {
            Some(txn) => match map.get(txn, &key_str) {
                Some(value) => {
                    // Try to cast to Doc
                    match value.cast::<Doc>() {
                        // Wrap in DocWrapper so nativeDestroy can properly free it
                        Ok(subdoc) => to_java_ptr(DocWrapper::from_doc(subdoc.clone())),
                        Err(_) => 0,
                    }
                }
                None => 0,
            },
            None => {
                throw_exception(&mut env, "Transaction not found");
                0
            }
        }
    }
}

/// Registers an observer for the YMap
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `subscription_id`: The subscription ID from Java
/// - `ymap_obj`: The Java YMap object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeObserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    subscription_id: jlong,
    ymap_obj: JObject,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return;
    }

    // Get JavaVM and create Executor for callback handling
    let executor = match env.get_java_vm() {
        Ok(vm) => Executor::new(Arc::new(vm)),
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to get JavaVM: {:?}", e));
            return;
        }
    };

    // Create a global reference to the Java YMap object
    let global_ref = match env.new_global_ref(ymap_obj) {
        Ok(r) => r,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create global ref: {:?}", e));
            return;
        }
    };

    unsafe {
        let wrapper = from_java_ptr::<DocWrapper>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);

        // Create observer closure
        let subscription = map.observe(move |txn, event| {
            // Use Executor for thread attachment with automatic local frame management
            let _ = executor
                .with_attached(|env| dispatch_map_event(env, doc_ptr, subscription_id, txn, event));
        });

        // Store subscription and GlobalRef in the DocWrapper
        wrapper.add_subscription(subscription_id, subscription, global_ref);
    }
}

/// Unregisters an observer for the YMap
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYMap_nativeUnobserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    _map_ptr: jlong,
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

/// Helper function to dispatch a map event to Java
fn dispatch_map_event(
    env: &mut JNIEnv,
    doc_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &MapEvent,
) -> Result<(), jni::errors::Error> {
    // Get the Java YMap object from DocWrapper
    let ymap_ref = unsafe {
        let wrapper = from_java_ptr::<DocWrapper>(doc_ptr);
        match wrapper.get_java_ref(subscription_id) {
            Some(r) => r,
            None => {
                eprintln!("No Java object found for subscription {}", subscription_id);
                return Ok(());
            }
        }
    };

    let ymap_obj = ymap_ref.as_obj();

    // Get the keys that changed
    let keys = event.keys(txn);

    // Create a Java ArrayList for changes
    let changes_list = env.new_object("java/util/ArrayList", "()V", &[])?;

    // Convert each EntryChange to a YMapChange
    for (key, change) in keys {
        let key_str = key.to_string();
        let change_obj = match change {
            EntryChange::Inserted(new_value) => {
                // Create YMapChange for INSERT
                let new_value_obj = out_to_jobject(env, new_value)?;

                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYMapChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let insert_type =
                    env.get_static_field(type_class, "INSERT", "Lnet/carcdr/ycrdt/YChange$Type;")?;
                let key_jstr = env.new_string(&key_str)?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                    &[
                        JValue::Object(&insert_type.l()?),
                        JValue::Object(&key_jstr),
                        JValue::Object(&new_value_obj),
                        JValue::Object(&JObject::null()),
                    ],
                )?
            }
            EntryChange::Updated(old_value, new_value) => {
                // Create YMapChange for ATTRIBUTE (update)
                let old_value_obj = out_to_jobject(env, old_value)?;
                let new_value_obj = out_to_jobject(env, new_value)?;

                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYMapChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let attribute_type = env.get_static_field(
                    type_class,
                    "ATTRIBUTE",
                    "Lnet/carcdr/ycrdt/YChange$Type;",
                )?;
                let key_jstr = env.new_string(&key_str)?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                    &[
                        JValue::Object(&attribute_type.l()?),
                        JValue::Object(&key_jstr),
                        JValue::Object(&new_value_obj),
                        JValue::Object(&old_value_obj),
                    ],
                )?
            }
            EntryChange::Removed(old_value) => {
                // Create YMapChange for DELETE
                let old_value_obj = out_to_jobject(env, old_value)?;

                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYMapChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let delete_type =
                    env.get_static_field(type_class, "DELETE", "Lnet/carcdr/ycrdt/YChange$Type;")?;
                let key_jstr = env.new_string(&key_str)?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                    &[
                        JValue::Object(&delete_type.l()?),
                        JValue::Object(&key_jstr),
                        JValue::Object(&JObject::null()),
                        JValue::Object(&old_value_obj),
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
    let event_class = env.find_class("net/carcdr/ycrdt/jni/JniYEvent")?;
    let target = ymap_obj; // Use the YMap object as the target
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

    // Call YMap.dispatchEvent(subscriptionId, event)
    env.call_method(
        ymap_obj,
        "dispatchEvent",
        "(JLnet/carcdr/ycrdt/jni/JniYEvent;)V",
        &[JValue::Long(subscription_id), JValue::Object(&event_obj)],
    )?;

    Ok(())
}

/// Helper function to convert yrs Out to JObject
fn out_to_jobject<'local>(
    env: &mut JNIEnv<'local>,
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
    fn test_map_creation() {
        let doc = Doc::new();
        let map = doc.get_or_insert_map("test");
        let ptr = to_java_ptr(map);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<MapRef>(ptr);
        }
    }

    #[test]
    fn test_map_set_and_get() {
        let doc = Doc::new();
        let map = doc.get_or_insert_map("test");

        {
            let mut txn = doc.transact_mut();
            map.insert(&mut txn, "name", "Alice");
            map.insert(&mut txn, "age", 30.0);
        }

        let txn = doc.transact();
        assert_eq!(map.len(&txn), 2);
        assert_eq!(map.get(&txn, "name").unwrap().to_string(&txn), "Alice");
        assert_eq!(map.get(&txn, "age").unwrap().cast::<f64>().unwrap(), 30.0);
    }

    #[test]
    fn test_map_remove() {
        let doc = Doc::new();
        let map = doc.get_or_insert_map("test");

        {
            let mut txn = doc.transact_mut();
            map.insert(&mut txn, "key1", "value1");
            map.insert(&mut txn, "key2", "value2");
        }

        {
            let mut txn = doc.transact_mut();
            map.remove(&mut txn, "key1");
        }

        let txn = doc.transact();
        assert_eq!(map.len(&txn), 1);
        assert!(map.get(&txn, "key1").is_none());
        assert_eq!(map.get(&txn, "key2").unwrap().to_string(&txn), "value2");
    }

    #[test]
    fn test_map_clear() {
        let doc = Doc::new();
        let map = doc.get_or_insert_map("test");

        {
            let mut txn = doc.transact_mut();
            map.insert(&mut txn, "key1", "value1");
            map.insert(&mut txn, "key2", "value2");
            map.insert(&mut txn, "key3", "value3");
        }

        {
            let mut txn = doc.transact_mut();
            map.clear(&mut txn);
        }

        let txn = doc.transact();
        assert_eq!(map.len(&txn), 0);
    }

    #[test]
    fn test_map_subdocument() {
        let doc = Doc::new();
        let map = doc.get_or_insert_map("test");
        let subdoc = Doc::new();

        // Insert subdocument
        {
            let mut txn = doc.transact_mut();
            map.insert(&mut txn, "nested", subdoc.clone());
        }

        // Retrieve subdocument
        let txn = doc.transact();
        let retrieved = map.get(&txn, "nested");
        assert!(retrieved.is_some());

        let retrieved_doc = retrieved.unwrap().cast::<Doc>();
        assert!(retrieved_doc.is_ok());
    }
}
