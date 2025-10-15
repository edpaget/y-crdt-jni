use crate::{free_java_ptr, from_java_ptr, throw_exception, to_java_ptr, to_jstring};
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jdouble, jlong, jstring};
use jni::JNIEnv;
use yrs::types::ToJson;
use yrs::{Doc, Map, MapRef, Transact};

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
/// - `key`: The key to set
/// - `value`: The string value to set
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeSetString(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
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
        let mut txn = doc.transact_mut();
        map.insert(&mut txn, key_str, value_str);
    }
}

/// Sets a double value in the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `key`: The key to set
/// - `value`: The double value to set
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeSetDouble(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
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
        let mut txn = doc.transact_mut();
        map.insert(&mut txn, key_str, value);
    }
}

/// Removes a key from the map
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `map_ptr`: Pointer to the YMap instance
/// - `key`: The key to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeRemove(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
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
        let mut txn = doc.transact_mut();
        map.remove(&mut txn, &key_str);
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
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_YMap_nativeClear(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    map_ptr: jlong,
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
        let mut txn = doc.transact_mut();
        map.clear(&mut txn);
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
}
