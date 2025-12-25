use jni::objects::GlobalRef;
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use std::cell::RefCell;
use std::collections::HashMap;
use yrs::{Doc, Subscription, TransactionMut};

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
}
