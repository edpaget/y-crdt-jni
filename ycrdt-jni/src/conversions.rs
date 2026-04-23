//! Shared conversion helpers for JNI operations.
//!
//! This module contains helper functions to convert between Rust y-crdt types
//! and Java objects via JNI. These are consolidated here to avoid duplication
//! across the various type modules.

use jni::objects::{JObject, JString, JValue};
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
        Any::Null | Any::Undefined => Ok(JObject::null()),
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
            // For other types (Buffer, Array, Map), convert to string as a fallback.
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

/// Failure modes for [`jobject_to_any`].
#[derive(Debug)]
pub enum AnyConversionError {
    /// The Java value's class is not one of the supported attribute types.
    Unsupported(String),
    /// A JNI call failed while inspecting or unboxing the value.
    Jni(jni::errors::Error),
}

impl From<jni::errors::Error> for AnyConversionError {
    fn from(e: jni::errors::Error) -> Self {
        AnyConversionError::Jni(e)
    }
}

/// Convert a Java `JObject` to a `yrs::Any`.
///
/// Supported Java classes: `String`, `Long`, `Integer`, `Double`, `Float`,
/// `Boolean`, and `null`. `Integer` widens to `Any::BigInt`; `Float` widens to
/// `Any::Number`. Any other class returns
/// `Err(AnyConversionError::Unsupported(class_name))`.
pub fn jobject_to_any(env: &mut JNIEnv, value: &JObject) -> Result<Any, AnyConversionError> {
    if value.is_null() {
        return Ok(Any::Null);
    }

    if env.is_instance_of(value, "java/lang/String")? {
        let jstr = JString::from(unsafe { JObject::from_raw(value.as_raw()) });
        let rust_str: String = env.get_string(&jstr)?.into();
        return Ok(Any::String(rust_str.into()));
    }

    if env.is_instance_of(value, "java/lang/Boolean")? {
        let b = env.call_method(value, "booleanValue", "()Z", &[])?.z()?;
        return Ok(Any::Bool(b));
    }

    if env.is_instance_of(value, "java/lang/Long")?
        || env.is_instance_of(value, "java/lang/Integer")?
    {
        let n = env.call_method(value, "longValue", "()J", &[])?.j()?;
        return Ok(Any::BigInt(n));
    }

    if env.is_instance_of(value, "java/lang/Double")?
        || env.is_instance_of(value, "java/lang/Float")?
    {
        let n = env.call_method(value, "doubleValue", "()D", &[])?.d()?;
        return Ok(Any::Number(n));
    }

    // Fetch the concrete class name for the error message.
    let class = env.get_object_class(value)?;
    let name_val = env.call_method(&class, "getName", "()Ljava/lang/String;", &[])?;
    let name_obj = name_val.l()?;
    let class_name: String = env.get_string(&JString::from(name_obj))?.into();
    Err(AnyConversionError::Unsupported(class_name))
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
