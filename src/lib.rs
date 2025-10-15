use jni::sys::{jlong, jstring};
use jni::JNIEnv;

mod ydoc;

pub use ydoc::*;

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
