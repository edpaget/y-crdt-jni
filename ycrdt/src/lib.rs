use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use yrs::TransactionMut;

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

// Wrapper for transaction pointer that implements Send
// This is safe because we ensure transactions are only accessed from
// the thread that uses them via JNI calls
struct TransactionPtr(*mut ());

unsafe impl Send for TransactionPtr {}

// Transaction storage for batching operations
// Maps transaction ID to the transaction pointer
// Note: We store raw pointers to handle lifetime issues with TransactionMut
lazy_static::lazy_static! {
    static ref ACTIVE_TRANSACTIONS: Arc<Mutex<HashMap<jlong, TransactionPtr>>> =
        Arc::new(Mutex::new(HashMap::new()));

    static ref TRANSACTION_COUNTER: Arc<Mutex<jlong>> = Arc::new(Mutex::new(1));
}

/// Generate unique transaction ID
pub fn next_transaction_id() -> jlong {
    let mut counter = TRANSACTION_COUNTER.lock().unwrap();
    let id = *counter;
    *counter += 1;
    id
}

/// Store a transaction with the given ID
///
/// # Safety
/// The caller must ensure the transaction pointer remains valid
/// until it is removed from storage
pub unsafe fn store_transaction<'a>(id: jlong, txn: TransactionMut<'a>) {
    let txn_ptr = Box::into_raw(Box::new(txn)) as *mut ();
    let mut transactions = ACTIVE_TRANSACTIONS.lock().unwrap();
    transactions.insert(id, TransactionPtr(txn_ptr));
}

/// Retrieve a transaction by ID
///
/// # Safety
/// The caller must ensure the transaction ID is valid and the
/// transaction has not been freed
pub unsafe fn get_transaction_mut<'a>(id: jlong) -> Option<&'a mut TransactionMut<'a>> {
    // Get the pointer without holding the lock to avoid deadlock
    let ptr = {
        let transactions = ACTIVE_TRANSACTIONS.lock().unwrap();
        transactions.get(&id).map(|wrapper| wrapper.0)
    };

    ptr.map(|p| {
        let txn_ptr = p as *mut TransactionMut<'a>;
        &mut *txn_ptr
    })
}

/// Remove and drop a transaction by ID
///
/// # Safety
/// The caller must ensure the transaction ID is valid and the
/// transaction has not been freed
pub unsafe fn remove_transaction(id: jlong) {
    let mut transactions = ACTIVE_TRANSACTIONS.lock().unwrap();
    if let Some(wrapper) = transactions.remove(&id) {
        // Reconstruct the Box and drop it to free memory and commit the transaction
        let txn_ptr = wrapper.0 as *mut TransactionMut;
        let _ = Box::from_raw(txn_ptr);
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
    use yrs::Doc;

    #[test]
    fn test_pointer_conversion() {
        let doc = Doc::new();
        let ptr = to_java_ptr(doc);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<Doc>(ptr);
        }
    }
}
