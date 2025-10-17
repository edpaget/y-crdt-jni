use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::sys::{jdouble, jlong, jstring};
use jni::{AttachGuard, JNIEnv};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use yrs::types::map::MapEvent;
use yrs::types::{EntryChange, ToJson};
use yrs::{Doc, Map, MapRef, Observable, Out, Transact, TransactionMut};

// Global storage for Java YMap objects (needed for callbacks)
lazy_static::lazy_static! {
    static ref MAP_JAVA_OBJECTS: Arc<Mutex<HashMap<jlong, GlobalRef>>> =
        Arc::new(Mutex::new(HashMap::new()));
}

/// Gets or creates a YMap instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the map object in the document
///
/// # Returns
/// A pointer to the YMap instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeGetMap(
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
        let map = doc.get_or_insert_map(name_str.as_str());
        to_java_ptr(map)
    }
}

/// Destroys a YMap instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YMap instance
///
/// # Safety
/// The pointer must be valid and point to a YMap instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    if ptr != 0 {
        unsafe {
            free_java_ptr::<MapRef>(ptr);
        }
    }
}

/// Gets the size of the map (number of entries)
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
///
/// # Returns
/// The size of the map as jlong
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeSize(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
) -> jlong {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return 0;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let txn = doc.transact();
        map.len(&txn) as jlong
    }
}

/// Gets a string value from the map by key
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `key`: The key to look up
///
/// # Returns
/// A Java string, or null if key not found or value is not a string
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeGetString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    key: JString,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return std::ptr::null_mut();
    }

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => match s.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                throw_exception(&mut env, "Invalid UTF-8 in key");
                return std::ptr::null_mut();
            }
        },
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return std::ptr::null_mut();
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let txn = doc.transact();

        match map.get(&txn, &key_str) {
            Some(value) => {
                let s = value.to_string(&txn);
                to_jstring(&mut env, &s)
            }
            None => std::ptr::null_mut(),
        }
    }
}

/// Gets a double value from the map by key
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `key`: The key to look up
///
/// # Returns
/// The double value, or 0.0 if key not found or value is not a number
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeGetDouble(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    key: JString,
) -> jdouble {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return 0.0;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return 0.0;
    }

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => match s.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                throw_exception(&mut env, "Invalid UTF-8 in key");
                return 0.0;
            }
        },
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return 0.0;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let txn = doc.transact();

        match map.get(&txn, &key_str) {
            Some(value) => value.cast::<f64>().unwrap_or(0.0),
            None => 0.0,
        }
    }
}

/// Sets a string value in the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction (0 = create implicit transaction)
/// - `key`: The key to set
/// - `value`: The string value to set
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeSetString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
    value: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return;
    }

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return;
        }
    };

    // Convert value to Rust string
    let value_str: String = match env.get_string(&value) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get value string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);

        if txn_ptr == 0 {
            // Legacy behavior: create implicit transaction
            let mut txn = doc.transact_mut();
            map.insert(&mut txn, key_str, value_str);
        } else {
            // Use existing transaction
            if let Some(txn) = crate::get_transaction_mut(txn_ptr) {
                map.insert(txn, key_str, value_str);
            } else {
                throw_exception(&mut env, "Invalid transaction pointer");
            }
        }
    }
}

/// Sets a double value in the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction (0 = create implicit transaction)
/// - `key`: The key to set
/// - `value`: The double value to set
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeSetDouble(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
    value: jdouble,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return;
    }

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);

        if txn_ptr == 0 {
            // Legacy behavior: create implicit transaction
            let mut txn = doc.transact_mut();
            map.insert(&mut txn, key_str, value);
        } else {
            // Use existing transaction
            if let Some(txn) = crate::get_transaction_mut(txn_ptr) {
                map.insert(txn, key_str, value);
            } else {
                throw_exception(&mut env, "Invalid transaction pointer");
            }
        }
    }
}

/// Removes a key from the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction (0 = create implicit transaction)
/// - `key`: The key to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeRemove(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return;
    }

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);

        if txn_ptr == 0 {
            // Legacy behavior: create implicit transaction
            let mut txn = doc.transact_mut();
            map.remove(&mut txn, &key_str);
        } else {
            // Use existing transaction
            if let Some(txn) = crate::get_transaction_mut(txn_ptr) {
                map.remove(txn, &key_str);
            } else {
                throw_exception(&mut env, "Invalid transaction pointer");
            }
        }
    }
}

/// Checks if a key exists in the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `key`: The key to check
///
/// # Returns
/// true if the key exists, false otherwise
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeContainsKey(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    key: JString,
) -> bool {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return false;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return false;
    }

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => match s.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                throw_exception(&mut env, "Invalid UTF-8 in key");
                return false;
            }
        },
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return false;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let txn = doc.transact();
        map.contains_key(&txn, &key_str)
    }
}

/// Gets all keys from the map as a Java array
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
///
/// # Returns
/// A Java String[] array containing all keys
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeKeys<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    doc_ptr: jlong,
    map_ptr: jlong,
) -> JObject<'a> {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return JObject::null();
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return JObject::null();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let txn = doc.transact();

        // Collect all keys
        let keys: Vec<String> = map.keys(&txn).map(|k| k.to_string()).collect();

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
}

/// Clears all entries from the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction (0 = create implicit transaction)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeClear(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return;
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);

        if txn_ptr == 0 {
            // Legacy behavior: create implicit transaction
            let mut txn = doc.transact_mut();
            map.clear(&mut txn);
        } else {
            // Use existing transaction
            if let Some(txn) = crate::get_transaction_mut(txn_ptr) {
                map.clear(txn);
            } else {
                throw_exception(&mut env, "Invalid transaction pointer");
            }
        }
    }
}

/// Converts the map to a JSON string representation
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
///
/// # Returns
/// A JSON string representation of the map
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeToJson(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
) -> jstring {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return std::ptr::null_mut();
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return std::ptr::null_mut();
    }

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let txn = doc.transact();
        let json = map.to_json(&txn).to_string();
        to_jstring(&mut env, &json)
    }
}

/// Sets a YDoc subdocument value in the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `txn_ptr`: Pointer to transaction (0 = create implicit transaction)
/// - `key`: The key to set
/// - `subdoc_ptr`: Pointer to the YDoc subdocument to insert
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeSetDoc(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
    txn_ptr: jlong,
    key: JString,
    subdoc_ptr: jlong,
) {
    if doc_ptr == 0 {
        throw_exception(&mut env, "Invalid YDoc pointer");
        return;
    }
    if map_ptr == 0 {
        throw_exception(&mut env, "Invalid YMap pointer");
        return;
    }
    if subdoc_ptr == 0 {
        throw_exception(&mut env, "Invalid subdocument pointer");
        return;
    }

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let subdoc = from_java_ptr::<Doc>(subdoc_ptr);

        // Clone the subdoc for insertion (Doc implements Prelim)
        let subdoc_clone = subdoc.clone();

        if txn_ptr == 0 {
            // Legacy behavior: create implicit transaction
            let mut txn = doc.transact_mut();
            map.insert(&mut txn, key_str, subdoc_clone);
        } else {
            // Use existing transaction
            if let Some(txn) = crate::get_transaction_mut(txn_ptr) {
                map.insert(txn, key_str, subdoc_clone);
            } else {
                throw_exception(&mut env, "Invalid transaction pointer");
            }
        }
    }
}

/// Gets a YDoc subdocument value from the map by key
///
/// # Parameters
/// - `doc_ptr`: Pointer to the parent YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `key`: The key to look up
///
/// # Returns
/// A pointer to the YDoc subdocument, or 0 if key not found or value is not a Doc
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeGetDoc(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
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

    // Convert key to Rust string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => match s.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                throw_exception(&mut env, "Invalid UTF-8 in key");
                return 0;
            }
        },
        Err(_) => {
            throw_exception(&mut env, "Failed to get key string");
            return 0;
        }
    };

    unsafe {
        let doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);
        let txn = doc.transact();

        match map.get(&txn, &key_str) {
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

/// Registers an observer for the YMap
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `subscription_id`: The subscription ID from Java
/// - `ymap_obj`: The Java YMap object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeObserve(
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

    // Get JavaVM for later callback
    let jvm = match env.get_java_vm() {
        Ok(vm) => Arc::new(vm),
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

    // Store the global reference
    {
        let mut java_objects = MAP_JAVA_OBJECTS.lock().unwrap();
        java_objects.insert(subscription_id, global_ref);
    }

    unsafe {
        let _doc = from_java_ptr::<Doc>(doc_ptr);
        let map = from_java_ptr::<MapRef>(map_ptr);

        // Create observer closure
        let jvm_clone = Arc::clone(&jvm);
        let subscription = map.observe(move |txn, event| {
            // Attach to JVM for this thread
            let mut env = match jvm_clone.attach_current_thread() {
                Ok(env) => env,
                Err(_) => return, // Can't do much if we can't attach
            };

            // Dispatch event to Java
            if let Err(e) = dispatch_map_event(&mut env, map_ptr, subscription_id, txn, event) {
                eprintln!("Failed to dispatch map event: {:?}", e);
            }
        });

        // Leak the subscription to keep it alive - we'll clean up on unobserve
        // This is a simplified approach; in production we'd use a better mechanism
        Box::leak(Box::new(subscription));
    }
}

/// Unregisters an observer for the YMap
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance (unused but kept for consistency)
/// - `map_ptr`: Pointer to the YMap instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeUnobserve(
    _env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    _map_ptr: jlong,
    subscription_id: jlong,
) {
    // Remove the global reference to allow the Java object to be GC'd
    let mut java_objects = MAP_JAVA_OBJECTS.lock().unwrap();
    java_objects.remove(&subscription_id);
    // The GlobalRef is dropped here, releasing the reference
    // Note: The Subscription is still leaked from observe()
    // This is acceptable for now but should be fixed in production
}

/// Helper function to dispatch a map event to Java
fn dispatch_map_event(
    env: &mut AttachGuard,
    _map_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &MapEvent,
) -> Result<(), jni::errors::Error> {
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

                let change_class = env.find_class("net/carcdr/ycrdt/YMapChange")?;
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

                let change_class = env.find_class("net/carcdr/ycrdt/YMapChange")?;
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

                let change_class = env.find_class("net/carcdr/ycrdt/YMapChange")?;
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

    // Get the Java YMap object from our global storage
    let java_objects = MAP_JAVA_OBJECTS.lock().unwrap();
    let ymap_ref = match java_objects.get(&subscription_id) {
        Some(r) => r,
        None => {
            eprintln!("No Java object found for subscription {}", subscription_id);
            return Ok(());
        }
    };

    let ymap_obj = ymap_ref.as_obj();

    // Create YEvent
    let event_class = env.find_class("net/carcdr/ycrdt/YEvent")?;
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
