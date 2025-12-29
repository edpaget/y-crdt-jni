use crate::{
    free_if_valid, from_java_ptr, get_mut_or_throw, get_ref_or_throw, throw_exception, to_java_ptr,
    to_jstring, DocPtr, DocWrapper, JniEnvExt, TxnPtr, XmlFragmentPtr,
};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jlong, jstring};
use jni::{Executor, JNIEnv};
use std::sync::Arc;
use yrs::types::xml::XmlEvent;
use yrs::types::Change;
use yrs::{
    GetString, Observable, Out, TransactionMut, XmlElementPrelim, XmlFragment, XmlFragmentRef,
    XmlTextPrelim,
};

/// Gets or creates a YXmlFragment instance from a YDoc
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `name`: The name of the XML fragment in the document
///
/// # Returns
/// A pointer to the YXmlFragment instance (as jlong)
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeGetFragment(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    name: JString,
) -> jlong {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc", 0);

    // Convert Java string to Rust string
    let name_str = match env.get_rust_string(&name) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return 0;
        }
    };

    let fragment = wrapper.doc.get_or_insert_xml_fragment(name_str.as_str());
    to_java_ptr(fragment)
}

/// Destroys a YXmlFragment instance and frees its memory
///
/// # Parameters
/// - `ptr`: Pointer to the YXmlFragment instance
///
/// # Safety
/// The pointer must be valid and point to a YXmlFragment instance
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    free_if_valid!(XmlFragmentPtr::from_raw(ptr), XmlFragmentRef);
}

/// Gets the number of children in the fragment using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// The number of children as jint
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeLengthWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
) -> jint {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment",
        0
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    fragment.len(txn) as jint
}

/// Inserts an XML element as a child at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index at which to insert the element
/// - `tag`: The tag name for the element
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeInsertElementWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    tag: JString,
) {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment"
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert tag to Rust string
    let tag_str = match env.get_rust_string(&tag) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    fragment.insert(txn, index as u32, XmlElementPrelim::empty(tag_str.as_str()));
}

/// Inserts an XML text node as a child at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index at which to insert the text
/// - `content`: The text content
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeInsertTextWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    content: JString,
) {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment"
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    // Convert content to Rust string
    let content_str = match env.get_rust_string(&content) {
        Ok(s) => s,
        Err(e) => {
            throw_exception(&mut env, &e.to_string());
            return;
        }
    };

    fragment.insert(txn, index as u32, XmlTextPrelim::new(content_str.as_str()));
}

/// Removes children from the fragment using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The starting index
/// - `length`: The number of children to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeRemoveWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
    length: jint,
) {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment"
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction");

    fragment.remove_range(txn, index as u32, length as u32);
}

/// Gets the type of child node at the specified index using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index of the child
///
/// # Returns
/// 0 for ELEMENT, 1 for TEXT, -1 if no node at index
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeGetNodeTypeWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
) -> jint {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment",
        -1
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", -1);

    if let Some(child) = fragment.get(txn, index as u32) {
        // Check element first, then text
        if child.clone().into_xml_element().is_some() {
            return 0; // ELEMENT
        } else if child.into_xml_text().is_some() {
            return 1; // TEXT
        }
    }
    -1 // No node at index
}

/// Gets the XML element at the specified index (if it is an element) using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index of the child
///
/// # Returns
/// Pointer to the XmlElementRef, or 0 if not an element
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeGetElementWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
) -> jlong {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment",
        0
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    // Get child at index
    if let Some(child) = fragment.get(txn, index as u32) {
        // Extract element if it's an element type
        if let Some(element) = child.into_xml_element() {
            // element is XmlElementRef containing a BranchPtr
            // BranchPtr is reference-counted, so we can safely return a pointer to it
            return to_java_ptr(element);
        }
    }
    0
}

/// Gets the XML text at the specified index (if it is text) using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
/// - `index`: The index of the child
///
/// # Returns
/// Pointer to the XmlTextRef, or 0 if not text
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeGetTextWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
    index: jint,
) -> jlong {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment",
        0
    );
    let txn = get_mut_or_throw!(&mut env, TxnPtr::from_raw(txn_ptr), "YTransaction", 0);

    // Get child at index
    if let Some(child) = fragment.get(txn, index as u32) {
        // Extract text if it's a text type
        if let Some(text) = child.into_xml_text() {
            // text is XmlTextRef containing a BranchPtr
            // BranchPtr is reference-counted, so we can safely return a pointer to it
            return to_java_ptr(text);
        }
    }
    0
}

/// Returns the XML string representation of the fragment using an existing transaction
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `txn_ptr`: Pointer to the transaction
///
/// # Returns
/// A Java string containing the XML representation
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeToXmlStringWithTxn(
    mut env: JNIEnv,
    _class: JClass,
    _doc_ptr: jlong,
    fragment_ptr: jlong,
    txn_ptr: jlong,
) -> jstring {
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment",
        std::ptr::null_mut()
    );
    let txn = get_mut_or_throw!(
        &mut env,
        TxnPtr::from_raw(txn_ptr),
        "YTransaction",
        std::ptr::null_mut()
    );

    let xml_string = fragment.get_string(txn);
    to_jstring(&mut env, &xml_string)
}

/// Registers an observer for the YXmlFragment
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance
/// - `subscription_id`: The subscription ID from Java
/// - `fragment_obj`: The Java YXmlFragment object for callbacks
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeObserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    fragment_ptr: jlong,
    subscription_id: jlong,
    fragment_obj: JObject,
) {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");
    let fragment = get_ref_or_throw!(
        &mut env,
        XmlFragmentPtr::from_raw(fragment_ptr),
        "YXmlFragment"
    );

    // Get JavaVM and create Executor for callback handling
    let executor = match env.get_java_vm() {
        Ok(vm) => Executor::new(Arc::new(vm)),
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to get JavaVM: {:?}", e));
            return;
        }
    };

    // Create a global reference to the Java YXmlFragment object
    let global_ref = match env.new_global_ref(fragment_obj) {
        Ok(r) => r,
        Err(e) => {
            throw_exception(&mut env, &format!("Failed to create global ref: {:?}", e));
            return;
        }
    };

    // Create observer closure
    let subscription = fragment.observe(move |txn, event| {
        // Use Executor for thread attachment with automatic local frame management
        let _ = executor.with_attached(|env| {
            dispatch_xmlfragment_event(env, doc_ptr, subscription_id, txn, event)
        });
    });

    // Store subscription and GlobalRef in the DocWrapper
    wrapper.add_subscription(subscription_id, subscription, global_ref);
}

/// Unregisters an observer for the YXmlFragment
///
/// # Parameters
/// - `doc_ptr`: Pointer to the YDoc instance
/// - `fragment_ptr`: Pointer to the YXmlFragment instance (unused but kept for consistency)
/// - `subscription_id`: The subscription ID to remove
#[no_mangle]
pub extern "system" fn Java_net_carcdr_ycrdt_jni_JniYXmlFragment_nativeUnobserve(
    mut env: JNIEnv,
    _class: JClass,
    doc_ptr: jlong,
    _fragment_ptr: jlong,
    subscription_id: jlong,
) {
    let wrapper = get_ref_or_throw!(&mut env, DocPtr::from_raw(doc_ptr), "YDoc");

    // Remove subscription and GlobalRef from DocWrapper
    // Both the Subscription and GlobalRef are dropped here
    wrapper.remove_subscription(subscription_id);
}

/// Helper function to dispatch an XML fragment event to Java
fn dispatch_xmlfragment_event(
    env: &mut JNIEnv,
    doc_ptr: jlong,
    subscription_id: jlong,
    txn: &TransactionMut,
    event: &XmlEvent,
) -> Result<(), jni::errors::Error> {
    // Get the Java YXmlFragment object from DocWrapper
    let fragment_ref = unsafe {
        let wrapper = from_java_ptr::<DocWrapper>(doc_ptr);
        match wrapper.get_java_ref(subscription_id) {
            Some(r) => r,
            None => {
                eprintln!("No Java object found for subscription {}", subscription_id);
                return Ok(());
            }
        }
    };

    let fragment_obj = fragment_ref.as_obj();

    // Get the delta
    let delta = event.delta(txn);

    // Create a Java ArrayList for changes
    let changes_list = env.new_object("java/util/ArrayList", "()V", &[])?;

    // Convert each Change to a YArrayChange (XmlFragment uses the same structure as Array)
    for change in delta {
        let change_obj = match change {
            Change::Added(items) => {
                // Create YArrayChange for INSERT
                // Convert items to Java ArrayList
                let items_list = env.new_object("java/util/ArrayList", "()V", &[])?;
                for item in items {
                    let item_obj = out_to_jobject(env, item)?;
                    env.call_method(
                        &items_list,
                        "add",
                        "(Ljava/lang/Object;)Z",
                        &[JValue::Object(&item_obj)],
                    )?;
                }

                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYArrayChange")?;
                env.new_object(
                    change_class,
                    "(Ljava/util/List;)V",
                    &[JValue::Object(&items_list)],
                )?
            }
            Change::Removed(len) => {
                // Create YArrayChange for DELETE
                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYArrayChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let delete_type =
                    env.get_static_field(type_class, "DELETE", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;I)V",
                    &[JValue::Object(&delete_type.l()?), JValue::Int(*len as i32)],
                )?
            }
            Change::Retain(len) => {
                // Create YArrayChange for RETAIN
                let change_class = env.find_class("net/carcdr/ycrdt/jni/JniYArrayChange")?;
                let type_class = env.find_class("net/carcdr/ycrdt/YChange$Type")?;
                let retain_type =
                    env.get_static_field(type_class, "RETAIN", "Lnet/carcdr/ycrdt/YChange$Type;")?;

                env.new_object(
                    change_class,
                    "(Lnet/carcdr/ycrdt/YChange$Type;I)V",
                    &[JValue::Object(&retain_type.l()?), JValue::Int(*len as i32)],
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

    // Create YEvent
    let event_class = env.find_class("net/carcdr/ycrdt/jni/JniYEvent")?;
    let target = fragment_obj; // Use the YXmlFragment object as the target
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

    // Call YXmlFragment.dispatchEvent(subscriptionId, event)
    env.call_method(
        fragment_obj,
        "dispatchEvent",
        "(JLnet/carcdr/ycrdt/jni/JniYEvent;)V",
        &[JValue::Long(subscription_id), JValue::Object(&event_obj)],
    )?;

    Ok(())
}

/// Helper function to convert yrs Out to JObject
fn out_to_jobject<'local>(
    env: &mut JNIEnv<'local>,
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
    env: &mut JNIEnv<'local>,
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
    use crate::{free_java_ptr, from_java_ptr};
    use yrs::{Doc, Transact, XmlElementRef, XmlFragment, XmlFragmentRef, XmlTextRef};

    #[test]
    fn test_fragment_creation() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        let ptr = to_java_ptr(fragment);
        assert_ne!(ptr, 0);

        unsafe {
            free_java_ptr::<XmlFragmentRef>(ptr);
        }
    }

    #[test]
    fn test_fragment_insert_element() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlElementPrelim::empty("div"));
        }

        let txn = doc.transact();
        assert_eq!(fragment.len(&txn), 1);

        let child = fragment.get(&txn, 0).unwrap();
        assert!(child.into_xml_element().is_some());
    }

    #[test]
    fn test_fragment_insert_text() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlTextPrelim::new("Hello"));
        }

        let txn = doc.transact();
        assert_eq!(fragment.len(&txn), 1);

        let child = fragment.get(&txn, 0).unwrap();
        assert!(child.into_xml_text().is_some());
    }

    #[test]
    fn test_fragment_remove() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlElementPrelim::empty("div"));
            fragment.insert(&mut txn, 1, XmlTextPrelim::new("Hello"));
        }

        {
            let txn = doc.transact();
            assert_eq!(fragment.len(&txn), 2);
        }

        {
            let mut txn = doc.transact_mut();
            fragment.remove_range(&mut txn, 0, 1);
        }

        let txn = doc.transact();
        assert_eq!(fragment.len(&txn), 1);
    }

    #[test]
    fn test_fragment_get_element() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlElementPrelim::empty("div"));
            fragment.insert(&mut txn, 1, XmlTextPrelim::new("Hello"));
        }

        let txn = doc.transact();

        // Get element at index 0
        let child = fragment.get(&txn, 0).unwrap();
        let element = child.into_xml_element().unwrap();
        assert_eq!(element.tag().as_ref(), "div");

        // Convert to pointer and back
        let element_ptr = to_java_ptr(element);
        assert_ne!(element_ptr, 0);

        unsafe {
            let _element_ref = from_java_ptr::<XmlElementRef>(element_ptr);
            free_java_ptr::<XmlElementRef>(element_ptr);
        }
    }

    #[test]
    fn test_fragment_get_text() {
        let doc = Doc::new();
        let fragment = doc.get_or_insert_xml_fragment("test");

        {
            let mut txn = doc.transact_mut();
            fragment.insert(&mut txn, 0, XmlTextPrelim::new("Hello"));
        }

        let txn = doc.transact();

        // Get text at index 0
        let child = fragment.get(&txn, 0).unwrap();
        let text = child.into_xml_text().unwrap();

        // Convert to pointer and back
        let text_ptr = to_java_ptr(text);
        assert_ne!(text_ptr, 0);

        unsafe {
            let _text_ref = from_java_ptr::<XmlTextRef>(text_ptr);
            free_java_ptr::<XmlTextRef>(text_ptr);
        }
    }
}
