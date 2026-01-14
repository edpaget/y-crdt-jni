use jni::objects::GlobalRef;
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use std::collections::HashMap;
use std::marker::PhantomData;
use std::sync::RwLock;
use yrs::{ArrayRef, Doc, MapRef, Subscription, TextRef, TransactionMut};
use yrs::{XmlElementRef, XmlFragmentRef, XmlTextRef};

mod conversions;
mod yarray;
mod ydoc;
mod ymap;
mod ytext;
mod yxmlelement;
mod yxmlfragment;
mod yxmltext;

pub use conversions::*;
pub use yarray::*;
pub use ydoc::*;
pub use ymap::*;
pub use ytext::*;
pub use yxmlelement::*;
pub use yxmlfragment::*;
pub use yxmltext::*;

/// Wrapper around yrs::Doc that owns subscriptions and Java GlobalRefs.
/// This ensures subscriptions are properly cleaned up when the document is destroyed,
/// avoiding the need for global static storage and eliminating potential deadlocks.
pub struct DocWrapper {
    /// The underlying yrs document
    pub doc: Doc,
    /// Subscriptions keyed by subscription ID
    /// Using RwLock for thread-safe access from multiple threads (e.g., observer callbacks)
    subscriptions: RwLock<HashMap<jlong, Subscription>>,
    /// Java GlobalRefs for callback objects, keyed by subscription ID
    java_refs: RwLock<HashMap<jlong, GlobalRef>>,
}

impl DocWrapper {
    /// Create a new DocWrapper with a new document
    pub fn new() -> Self {
        Self {
            doc: Doc::new(),
            subscriptions: RwLock::new(HashMap::new()),
            java_refs: RwLock::new(HashMap::new()),
        }
    }

    /// Create a new DocWrapper with a document using the given options
    pub fn with_options(options: yrs::Options) -> Self {
        Self {
            doc: Doc::with_options(options),
            subscriptions: RwLock::new(HashMap::new()),
            java_refs: RwLock::new(HashMap::new()),
        }
    }

    /// Create a DocWrapper from an existing Doc (e.g., for subdocuments)
    pub fn from_doc(doc: Doc) -> Self {
        Self {
            doc,
            subscriptions: RwLock::new(HashMap::new()),
            java_refs: RwLock::new(HashMap::new()),
        }
    }

    /// Store a subscription and its associated Java GlobalRef
    pub fn add_subscription(&self, id: jlong, subscription: Subscription, java_ref: GlobalRef) {
        // Use write locks for exclusive access
        if let Ok(mut subs) = self.subscriptions.write() {
            subs.insert(id, subscription);
        }
        if let Ok(mut refs) = self.java_refs.write() {
            refs.insert(id, java_ref);
        }
    }

    /// Remove a subscription and its associated Java GlobalRef
    /// Returns the removed subscription (if any) so it can be dropped outside any locks
    pub fn remove_subscription(&self, id: jlong) -> Option<Subscription> {
        if let Ok(mut refs) = self.java_refs.write() {
            refs.remove(&id);
        }
        if let Ok(mut subs) = self.subscriptions.write() {
            return subs.remove(&id);
        }
        None
    }

    /// Get a reference to a Java GlobalRef by subscription ID
    pub fn get_java_ref(&self, id: jlong) -> Option<GlobalRef> {
        self.java_refs.read().ok()?.get(&id).cloned()
    }
}

impl Default for DocWrapper {
    fn default() -> Self {
        Self::new()
    }
}

/// A typed wrapper around a Java pointer (jlong) for type safety.
///
/// This provides compile-time type safety for pointer operations and
/// enables the use of typed validation macros.
#[derive(Debug)]
pub struct JavaPtr<T> {
    ptr: jlong,
    _marker: PhantomData<*mut T>,
}

impl<T> JavaPtr<T> {
    /// Create a JavaPtr from a raw jlong pointer
    pub fn from_raw(ptr: jlong) -> Self {
        Self {
            ptr,
            _marker: PhantomData,
        }
    }

    /// Get the raw pointer value
    pub fn raw(&self) -> jlong {
        self.ptr
    }

    /// Check if the pointer is null (zero)
    pub fn is_null(&self) -> bool {
        self.ptr == 0
    }

    /// Get an immutable reference to the pointed value
    ///
    /// # Safety
    /// The pointer must be valid and point to a properly initialized value of type T.
    /// The returned reference has 'static lifetime because the pointed value is
    /// heap-allocated and will outlive this JavaPtr wrapper.
    pub unsafe fn as_ref(&self) -> Option<&'static T> {
        if self.ptr == 0 {
            None
        } else {
            Some(&*(self.ptr as *const T))
        }
    }

    /// Get a mutable reference to the pointed value
    ///
    /// # Safety
    /// The pointer must be valid and point to a properly initialized value of type T.
    /// The returned reference has 'static lifetime because the pointed value is
    /// heap-allocated and will outlive this JavaPtr wrapper.
    pub unsafe fn as_mut(&self) -> Option<&'static mut T> {
        if self.ptr == 0 {
            None
        } else {
            Some(&mut *(self.ptr as *mut T))
        }
    }
}

// Type aliases for common pointer types
pub type DocPtr = JavaPtr<DocWrapper>;
pub type TextPtr = JavaPtr<TextRef>;
pub type ArrayPtr = JavaPtr<ArrayRef>;
pub type MapPtr = JavaPtr<MapRef>;
pub type XmlElementPtr = JavaPtr<XmlElementRef>;
pub type XmlFragmentPtr = JavaPtr<XmlFragmentRef>;
pub type XmlTextPtr = JavaPtr<XmlTextRef>;
pub type TxnPtr<'a> = JavaPtr<TransactionMut<'a>>;

/// Validate a pointer and get an immutable reference, or throw an exception and return.
///
/// # Arguments
/// * `$env` - Mutable reference to JNIEnv
/// * `$ptr` - The JavaPtr to validate
/// * `$name` - Name of the pointer type for error message (e.g., "YDoc")
/// * `$ret` - Value to return if validation fails (omit for unit-returning functions)
#[macro_export]
macro_rules! get_ref_or_throw {
    ($env:expr, $ptr:expr, $name:expr) => {{
        let ptr = $ptr;
        match unsafe { ptr.as_ref() } {
            Some(r) => r,
            None => {
                $crate::throw_exception($env, concat!("Invalid ", $name, " pointer"));
                return;
            }
        }
    }};
    ($env:expr, $ptr:expr, $name:expr, $ret:expr) => {{
        let ptr = $ptr;
        match unsafe { ptr.as_ref() } {
            Some(r) => r,
            None => {
                $crate::throw_exception($env, concat!("Invalid ", $name, " pointer"));
                return $ret;
            }
        }
    }};
}

/// Validate a pointer and get a mutable reference, or throw an exception and return.
///
/// # Arguments
/// * `$env` - Mutable reference to JNIEnv
/// * `$ptr` - The JavaPtr to validate
/// * `$name` - Name of the pointer type for error message (e.g., "YTransaction")
/// * `$ret` - Value to return if validation fails (omit for unit-returning functions)
#[macro_export]
macro_rules! get_mut_or_throw {
    ($env:expr, $ptr:expr, $name:expr) => {{
        let ptr = $ptr;
        match unsafe { ptr.as_mut() } {
            Some(r) => r,
            None => {
                $crate::throw_exception($env, concat!("Invalid ", $name, " pointer"));
                return;
            }
        }
    }};
    ($env:expr, $ptr:expr, $name:expr, $ret:expr) => {{
        let ptr = $ptr;
        match unsafe { ptr.as_mut() } {
            Some(r) => r,
            None => {
                $crate::throw_exception($env, concat!("Invalid ", $name, " pointer"));
                return $ret;
            }
        }
    }};
}

/// Free a pointer if it is non-null (for destroy functions).
///
/// # Arguments
/// * `$ptr` - The JavaPtr to free
/// * `$type` - The underlying type (e.g., TextRef)
#[macro_export]
macro_rules! free_if_valid {
    ($ptr:expr, $type:ty) => {
        if !$ptr.is_null() {
            unsafe { $crate::free_java_ptr::<$type>($ptr.raw()) }
        }
    };
}

/// Convert a JString to a Rust String, or throw an exception and return.
///
/// # Arguments
/// * `$env` - Mutable reference to JNIEnv
/// * `$jstring` - The JString to convert
/// * `$ret` - Value to return if conversion fails (omit for unit-returning functions)
#[macro_export]
macro_rules! get_string_or_throw {
    ($env:expr, $jstring:expr) => {{
        match $env.get_rust_string(&$jstring) {
            Ok(s) => s,
            Err(e) => {
                $crate::throw_exception($env, &e.to_string());
                return;
            }
        }
    }};
    ($env:expr, $jstring:expr, $ret:expr) => {{
        match $env.get_rust_string(&$jstring) {
            Ok(s) => s,
            Err(e) => {
                $crate::throw_exception($env, &e.to_string());
                return $ret;
            }
        }
    }};
}

//=============================================================================
// Result-based Error Handling
//=============================================================================

use jni::objects::JString;
use jni::sys::{jbyteArray, jdouble, jint};
use std::fmt;

/// Error type for JNI operations
#[derive(Debug)]
pub enum JniError {
    /// JNI operation failed
    Jni(jni::errors::Error),
    /// Invalid pointer provided from Java
    InvalidPointer(&'static str),
    /// String conversion failed
    StringConversion(&'static str),
    /// UTF-8 encoding error
    Utf8Error,
    /// Y-CRDT operation failed
    Yrs(String),
    /// Generic error with message
    Other(String),
}

impl fmt::Display for JniError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            JniError::Jni(e) => write!(f, "JNI error: {:?}", e),
            JniError::InvalidPointer(name) => write!(f, "Invalid {} pointer", name),
            JniError::StringConversion(ctx) => write!(f, "Failed to get {} string", ctx),
            JniError::Utf8Error => write!(f, "Invalid UTF-8 in string"),
            JniError::Yrs(msg) => write!(f, "Y-CRDT error: {}", msg),
            JniError::Other(msg) => write!(f, "{}", msg),
        }
    }
}

impl std::error::Error for JniError {}

impl From<jni::errors::Error> for JniError {
    fn from(e: jni::errors::Error) -> Self {
        JniError::Jni(e)
    }
}

impl From<std::str::Utf8Error> for JniError {
    fn from(_: std::str::Utf8Error) -> Self {
        JniError::Utf8Error
    }
}

/// Result type for JNI operations
pub type JniResult<T> = Result<T, JniError>;

/// Trait for types that have a default "error" value for JNI returns
pub trait JniDefault {
    /// The default value to return when an error occurs
    fn jni_default() -> Self;
}

impl JniDefault for () {
    fn jni_default() -> Self {}
}

impl JniDefault for jlong {
    fn jni_default() -> Self {
        0
    }
}

impl JniDefault for jint {
    fn jni_default() -> Self {
        0
    }
}

impl JniDefault for jdouble {
    fn jni_default() -> Self {
        0.0
    }
}

// Note: jstring and jbyteArray are both *mut _jobject, so one impl covers both
impl JniDefault for jstring {
    fn jni_default() -> Self {
        std::ptr::null_mut()
    }
}

impl JniDefault for bool {
    fn jni_default() -> Self {
        false
    }
}

impl<'a> JniDefault for jni::objects::JObject<'a> {
    fn jni_default() -> Self {
        jni::objects::JObject::null()
    }
}

/// Extension trait for JniResult to handle throwing exceptions
pub trait JniResultExt<T> {
    /// Unwrap the result or throw an exception and return the default value
    fn unwrap_or_throw(self, env: &mut JNIEnv) -> T
    where
        T: JniDefault;
}

impl<T> JniResultExt<T> for JniResult<T> {
    fn unwrap_or_throw(self, env: &mut JNIEnv) -> T
    where
        T: JniDefault,
    {
        match self {
            Ok(v) => v,
            Err(e) => {
                throw_exception(env, &e.to_string());
                T::jni_default()
            }
        }
    }
}

//=============================================================================
// JNI Helper Extension Traits
//=============================================================================

/// Extension trait for JNIEnv to simplify common operations
pub trait JniEnvExt<'local> {
    /// Get a Rust String from a JString, with proper error handling
    fn get_rust_string(&mut self, s: &JString) -> JniResult<String>;

    /// Create a jstring from a Rust str
    fn create_jstring(&mut self, s: &str) -> JniResult<jstring>;

    /// Create a byte array from a slice
    fn create_byte_array(&mut self, data: &[u8]) -> JniResult<jbyteArray>;
}

impl<'local> JniEnvExt<'local> for JNIEnv<'local> {
    fn get_rust_string(&mut self, s: &JString) -> JniResult<String> {
        let jstr = self
            .get_string(s)
            .map_err(|_| JniError::StringConversion("java string"))?;
        // Use Into<String> which properly handles Modified UTF-8 (CESU-8) to UTF-8 conversion
        Ok(jstr.into())
    }

    fn create_jstring(&mut self, s: &str) -> JniResult<jstring> {
        let jstr = self.new_string(s)?;
        Ok(jstr.into_raw())
    }

    fn create_byte_array(&mut self, data: &[u8]) -> JniResult<jbyteArray> {
        let arr = self.byte_array_from_slice(data)?;
        Ok(arr.into_raw())
    }
}

/// Retrieve a mutable reference to a transaction from a raw pointer
///
/// # Safety
/// The caller must ensure the pointer is valid and points to a TransactionMut
pub unsafe fn get_transaction_mut<'a>(txn_ptr: jlong) -> Option<&'a mut TransactionMut<'a>> {
    if txn_ptr == 0 {
        return None;
    }
    let ptr = txn_ptr as *mut TransactionMut<'a>;
    Some(&mut *ptr)
}

/// Free a transaction pointer
///
/// # Safety
/// The caller must ensure the pointer is valid and has not been freed
pub unsafe fn free_transaction(txn_ptr: jlong) {
    if txn_ptr != 0 {
        // Reconstruct the Box and drop it to free memory and commit the transaction
        let _ = Box::from_raw(txn_ptr as *mut TransactionMut);
    }
}

/// Helper function to convert a Rust string to a Java string
pub fn to_jstring(env: &mut JNIEnv, s: &str) -> jstring {
    match env.new_string(s) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Helper function to throw a Java exception
pub fn throw_exception(env: &mut JNIEnv, message: &str) {
    let _ = env.throw_new("java/lang/RuntimeException", message);
}

/// Helper function to convert a Java pointer (long) to a Rust reference
///
/// # Safety
/// The pointer must be valid and point to the expected type
pub unsafe fn from_java_ptr<T>(ptr: jlong) -> &'static mut T {
    &mut *(ptr as *mut T)
}

/// Helper function to convert a Rust reference to a Java pointer (long)
pub fn to_java_ptr<T>(obj: T) -> jlong {
    Box::into_raw(Box::new(obj)) as jlong
}

/// Helper function to free a Rust object from a Java pointer
///
/// # Safety
/// The pointer must be valid and point to the expected type
pub unsafe fn free_java_ptr<T>(ptr: jlong) {
    if ptr != 0 {
        let _ = Box::from_raw(ptr as *mut T);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_pointer_conversion() {
        let doc = DocWrapper::new();
        let ptr = to_java_ptr(doc);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<DocWrapper>(ptr);
        }
    }

    #[test]
    fn test_java_ptr_null() {
        let ptr: JavaPtr<DocWrapper> = JavaPtr::from_raw(0);
        assert!(ptr.is_null());
        assert!(unsafe { ptr.as_ref() }.is_none());
        assert!(unsafe { ptr.as_mut() }.is_none());
    }

    #[test]
    fn test_java_ptr_valid() {
        let doc = DocWrapper::new();
        let raw = to_java_ptr(doc);
        let ptr: DocPtr = DocPtr::from_raw(raw);

        assert!(!ptr.is_null());
        assert_eq!(ptr.raw(), raw);

        let doc_ref = unsafe { ptr.as_ref() }.unwrap();
        assert!(doc_ref.subscriptions.read().unwrap().is_empty());

        unsafe {
            free_java_ptr::<DocWrapper>(raw);
        }
    }

    #[test]
    fn test_type_aliases() {
        // Test that type aliases work correctly
        let _doc_ptr: DocPtr = DocPtr::from_raw(0);
        let _text_ptr: TextPtr = TextPtr::from_raw(0);
        let _array_ptr: ArrayPtr = ArrayPtr::from_raw(0);
        let _map_ptr: MapPtr = MapPtr::from_raw(0);
        let _xml_element_ptr: XmlElementPtr = XmlElementPtr::from_raw(0);
        let _xml_fragment_ptr: XmlFragmentPtr = XmlFragmentPtr::from_raw(0);
        let _xml_text_ptr: XmlTextPtr = XmlTextPtr::from_raw(0);
    }
}
