use jni::objects::GlobalRef;
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use std::cell::RefCell;
use std::collections::HashMap;
use std::marker::PhantomData;
use yrs::{ArrayRef, Doc, MapRef, Subscription, TextRef, TransactionMut};
use yrs::{XmlElementRef, XmlFragmentRef, XmlTextRef};

mod yarray;
mod ydoc;
mod ymap;
mod ytext;
mod yxmlelement;
mod yxmlfragment;
mod yxmltext;

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
    /// Using RefCell since JNI calls are serialized per-document
    subscriptions: RefCell<HashMap<jlong, Subscription>>,
    /// Java GlobalRefs for callback objects, keyed by subscription ID
    java_refs: RefCell<HashMap<jlong, GlobalRef>>,
}

impl DocWrapper {
    /// Create a new DocWrapper with a new document
    pub fn new() -> Self {
        Self {
            doc: Doc::new(),
            subscriptions: RefCell::new(HashMap::new()),
            java_refs: RefCell::new(HashMap::new()),
        }
    }

    /// Create a new DocWrapper with a document using the given options
    pub fn with_options(options: yrs::Options) -> Self {
        Self {
            doc: Doc::with_options(options),
            subscriptions: RefCell::new(HashMap::new()),
            java_refs: RefCell::new(HashMap::new()),
        }
    }

    /// Create a DocWrapper from an existing Doc (e.g., for subdocuments)
    pub fn from_doc(doc: Doc) -> Self {
        Self {
            doc,
            subscriptions: RefCell::new(HashMap::new()),
            java_refs: RefCell::new(HashMap::new()),
        }
    }

    /// Store a subscription and its associated Java GlobalRef
    pub fn add_subscription(&self, id: jlong, subscription: Subscription, java_ref: GlobalRef) {
        self.subscriptions.borrow_mut().insert(id, subscription);
        self.java_refs.borrow_mut().insert(id, java_ref);
    }

    /// Remove a subscription and its associated Java GlobalRef
    /// Returns the removed subscription (if any) so it can be dropped outside any locks
    pub fn remove_subscription(&self, id: jlong) -> Option<Subscription> {
        self.java_refs.borrow_mut().remove(&id);
        self.subscriptions.borrow_mut().remove(&id)
    }

    /// Get a reference to a Java GlobalRef by subscription ID
    pub fn get_java_ref(&self, id: jlong) -> Option<GlobalRef> {
        self.java_refs.borrow().get(&id).cloned()
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
        assert!(doc_ref.subscriptions.borrow().is_empty());

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
