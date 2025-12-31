//! Shared conversion helpers for JNI operations.
//!
//! This module contains helper functions to convert between Rust y-crdt types
//! and Java objects via JNI. These are consolidated here to avoid duplication
//! across the various type modules.

use jni::objects::{JObject, JValue};
use jni::JNIEnv;
use yrs::types::Attrs;
use yrs::{Any, Out};

/// Convert a yrs::Any value to a Java JObject.
///
/// Handles the following types:
/// - `Any::String` -> Java String
/// - `Any::Bool` -> Java Boolean
/// - `Any::Number` -> Java Double
/// - `Any::BigInt` -> Java Long
/// - Other types -> Java String (via to_string())
pub fn any_to_jobject<'local>(
    env: &mut JNIEnv<'local>,
    value: &Any,
) -> Result<JObject<'local>, jni::errors::Error> {
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

/// Convert a yrs::Out value to a Java JObject.
///
/// For `Out::Any`, delegates to `any_to_jobject`.
/// For complex types (YText, YArray, YMap, etc.), returns their string representation.
pub fn out_to_jobject<'local>(
    env: &mut JNIEnv<'local>,
    value: &Out,
) -> Result<JObject<'local>, jni::errors::Error> {
    match value {
        Out::Any(any) => any_to_jobject(env, any),
        Out::YText(_)
        | Out::YArray(_)
        | Out::YMap(_)
        | Out::YXmlElement(_)
        | Out::YXmlText(_)
        | Out::YDoc(_) => {
            // For complex types, return string representation
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

/// Create a Java HashMap from yrs Attrs.
///
/// Each attribute key becomes a String key in the HashMap,
/// and each value is converted using `any_to_jobject`.
pub fn attrs_to_java_hashmap<'local>(
    env: &mut JNIEnv<'local>,
    attrs: &Attrs,
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
