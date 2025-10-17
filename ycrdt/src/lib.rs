use jni::sys::{jlong, jstring};
use jni::JNIEnv;
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
