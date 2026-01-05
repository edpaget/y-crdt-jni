package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import net.carcdr.ycrdt.YChange;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YSubscription for XML observers.
 *
 * <p>Handles callbacks for YXmlElement, YXmlText, and YXmlFragment changes.</p>
 */
public class PanamaYXmlSubscription implements YSubscription {

    private static final Logger LOG = Logger.getLogger(PanamaYXmlSubscription.class.getName());
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    // VarHandles for struct field access
    private static final VarHandle EVENT_CHANGE_TAG = Yrs.YEVENT_CHANGE_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("tag"));
    private static final VarHandle EVENT_CHANGE_LEN = Yrs.YEVENT_CHANGE_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle EVENT_CHANGE_VALUES = Yrs.YEVENT_CHANGE_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("values"));

    private static final VarHandle EVENT_KEY_CHANGE_KEY = Yrs.YEVENT_KEY_CHANGE_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("key"));
    private static final VarHandle EVENT_KEY_CHANGE_TAG = Yrs.YEVENT_KEY_CHANGE_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("tag"));
    private static final VarHandle EVENT_KEY_CHANGE_OLD = Yrs.YEVENT_KEY_CHANGE_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("old_value"));
    private static final VarHandle EVENT_KEY_CHANGE_NEW = Yrs.YEVENT_KEY_CHANGE_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("new_value"));

    private static final VarHandle DELTA_OUT_TAG = Yrs.YDELTA_OUT_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("tag"));
    private static final VarHandle DELTA_OUT_LEN = Yrs.YDELTA_OUT_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle DELTA_OUT_INSERT = Yrs.YDELTA_OUT_LAYOUT
        .varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("insert"));

    /**
     * Type of XML observer.
     */
    public enum Type {
        /** Observer for YXmlElement. */
        ELEMENT,
        /** Observer for YXmlText. */
        TEXT,
        /** Observer for YXmlFragment (uses same callback as ELEMENT). */
        FRAGMENT
    }

    private final long subscriptionId;
    private final Object target;
    private final YObserver observer;
    private final Type type;
    private final Arena subscriptionArena;
    private final MemorySegment nativeSubscription;
    private volatile boolean closed = false;

    /**
     * Creates a new XML subscription.
     *
     * @param target the XML type being observed
     * @param observer the observer callback
     * @param type the type of XML observer
     */
    PanamaYXmlSubscription(Object target, YObserver observer, Type type) {
        this.subscriptionId = ID_GENERATOR.incrementAndGet();
        this.target = target;
        this.observer = observer;
        this.type = type;

        this.subscriptionArena = Arena.ofShared();

        try {
            MemorySegment callbackStub;
            MemorySegment branchPtr;

            if (type == Type.TEXT) {
                // YXmlText uses different callback signature
                MethodHandle callbackHandle = MethodHandles.lookup()
                    .findVirtual(PanamaYXmlSubscription.class, "onTextEvent",
                        MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class))
                    .bindTo(this);

                callbackStub = Linker.nativeLinker()
                    .upcallStub(callbackHandle, Yrs.YXMLTEXT_CALLBACK_DESCRIPTOR, subscriptionArena);

                branchPtr = ((PanamaYXmlText) target).getBranchPtr();
                this.nativeSubscription = Yrs.yxmltextObserve(
                    branchPtr, MemorySegment.NULL, callbackStub);
            } else {
                // Element and Fragment use the same callback signature
                MethodHandle callbackHandle = MethodHandles.lookup()
                    .findVirtual(PanamaYXmlSubscription.class, "onElementEvent",
                        MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class))
                    .bindTo(this);

                callbackStub = Linker.nativeLinker()
                    .upcallStub(callbackHandle, Yrs.YXML_CALLBACK_DESCRIPTOR, subscriptionArena);

                if (type == Type.ELEMENT) {
                    branchPtr = ((PanamaYXmlElement) target).getBranchPtr();
                } else {
                    branchPtr = ((PanamaYXmlFragment) target).getBranchPtr();
                }
                this.nativeSubscription = Yrs.yxmlelemObserve(
                    branchPtr, MemorySegment.NULL, callbackStub);
            }

            if (this.nativeSubscription.equals(MemorySegment.NULL)) {
                subscriptionArena.close();
                throw new RuntimeException("Failed to register XML observer");
            }

            LOG.fine("[Panama] XML Subscription #" + subscriptionId + " created for " + type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            subscriptionArena.close();
            throw new RuntimeException("Failed to create upcall stub", e);
        }
    }

    /**
     * Callback for element/fragment events.
     */
    @SuppressWarnings("unused")
    private void onElementEvent(MemorySegment state, MemorySegment eventPtr) {
        if (closed) {
            return;
        }

        try {
            List<YChange> changes = new ArrayList<>();

            // Parse child changes (delta)
            try (Arena tempArena = Arena.ofConfined()) {
                MemorySegment lenPtr = tempArena.allocate(ValueLayout.JAVA_INT);
                MemorySegment deltaPtr = Yrs.yxmlelemEventDelta(eventPtr, lenPtr);
                int deltaLen = lenPtr.get(ValueLayout.JAVA_INT, 0);

                if (!deltaPtr.equals(MemorySegment.NULL) && deltaLen > 0) {
                    parseElementDelta(deltaPtr, deltaLen, changes);
                    Yrs.yeventDeltaDestroy(deltaPtr, deltaLen);
                }

                // Parse attribute changes (keys)
                MemorySegment keysPtr = Yrs.yxmlelemEventKeys(eventPtr, lenPtr);
                int keysLen = lenPtr.get(ValueLayout.JAVA_INT, 0);

                if (!keysPtr.equals(MemorySegment.NULL) && keysLen > 0) {
                    parseKeyChanges(keysPtr, keysLen, changes);
                    Yrs.yeventKeysDestroy(keysPtr, keysLen);
                }
            }

            // Create and dispatch the event
            PanamaYEvent event = new PanamaYEvent(target, changes, null);
            observer.onChange(event);

        } catch (Exception e) {
            LOG.warning("[Panama] XML observer #" + subscriptionId + " threw exception: " + e);
            // Get error handler from the appropriate doc
            PanamaYDoc doc = getDoc();
            if (doc != null) {
                doc.getObserverErrorHandler().handleError(e, doc);
            }
        }
    }

    /**
     * Callback for text events.
     */
    @SuppressWarnings("unused")
    private void onTextEvent(MemorySegment state, MemorySegment eventPtr) {
        if (closed) {
            return;
        }

        try {
            List<YChange> changes = new ArrayList<>();

            // Parse text delta
            try (Arena tempArena = Arena.ofConfined()) {
                MemorySegment lenPtr = tempArena.allocate(ValueLayout.JAVA_INT);
                MemorySegment deltaPtr = Yrs.yxmltextEventDelta(eventPtr, lenPtr);
                int deltaLen = lenPtr.get(ValueLayout.JAVA_INT, 0);

                if (!deltaPtr.equals(MemorySegment.NULL) && deltaLen > 0) {
                    parseTextDelta(deltaPtr, deltaLen, changes);
                    Yrs.ytextDeltaDestroy(deltaPtr, deltaLen);
                }
            }

            // Create and dispatch the event
            PanamaYEvent event = new PanamaYEvent(target, changes, null);
            observer.onChange(event);

        } catch (Exception e) {
            LOG.warning("[Panama] XML text observer #" + subscriptionId + " threw exception: " + e);
            PanamaYDoc doc = getDoc();
            if (doc != null) {
                doc.getObserverErrorHandler().handleError(e, doc);
            }
        }
    }

    /**
     * Parses element delta (child changes) from native memory.
     */
    private void parseElementDelta(MemorySegment deltaPtr, int count, List<YChange> changes) {
        long structSize = Yrs.YEVENT_CHANGE_LAYOUT.byteSize();
        MemorySegment array = deltaPtr.reinterpret(structSize * count);

        for (int i = 0; i < count; i++) {
            MemorySegment entry = array.asSlice(i * structSize, structSize);

            byte tag = (byte) EVENT_CHANGE_TAG.get(entry, 0L);
            int len = (int) EVENT_CHANGE_LEN.get(entry, 0L);

            if (tag == Yrs.Y_EVENT_CHANGE_ADD) {
                // For ADD, we'd need to parse the values array
                // For now, create a simple INSERT change with empty list
                List<Object> items = new ArrayList<>();
                // TODO: Parse values array if needed for full implementation
                for (int j = 0; j < len; j++) {
                    items.add(null); // Placeholder
                }
                changes.add(new PanamaYArrayChange(items));
            } else if (tag == Yrs.Y_EVENT_CHANGE_DELETE) {
                changes.add(new PanamaYArrayChange(YChange.Type.DELETE, len));
            } else if (tag == Yrs.Y_EVENT_CHANGE_RETAIN) {
                changes.add(new PanamaYArrayChange(YChange.Type.RETAIN, len));
            }
        }
    }

    /**
     * Parses key changes (attribute changes) from native memory.
     */
    private void parseKeyChanges(MemorySegment keysPtr, int count, List<YChange> changes) {
        long structSize = Yrs.YEVENT_KEY_CHANGE_LAYOUT.byteSize();
        MemorySegment array = keysPtr.reinterpret(structSize * count);

        for (int i = 0; i < count; i++) {
            MemorySegment entry = array.asSlice(i * structSize, structSize);

            MemorySegment keyPtr = (MemorySegment) EVENT_KEY_CHANGE_KEY.get(entry, 0L);
            byte tag = (byte) EVENT_KEY_CHANGE_TAG.get(entry, 0L);

            String key = null;
            if (keyPtr.address() != 0) {
                key = keyPtr.reinterpret(Long.MAX_VALUE).getString(0);
            }

            String oldValue = null;
            String newValue = null;

            // Note: YOutput parsing is complex as it requires checking the output type tag first.
            // For now, we just record that a change occurred without extracting values.
            // TODO: Implement full YOutput parsing with type checking

            YChange.Type changeType;
            if (tag == Yrs.Y_EVENT_KEY_CHANGE_ADD) {
                changeType = YChange.Type.INSERT;
            } else if (tag == Yrs.Y_EVENT_KEY_CHANGE_DELETE) {
                changeType = YChange.Type.DELETE;
            } else {
                changeType = YChange.Type.ATTRIBUTE;
            }

            changes.add(new PanamaYXmlElementChange(changeType, key, newValue, oldValue));
        }
    }

    /**
     * Parses text delta from native memory.
     */
    private void parseTextDelta(MemorySegment deltaPtr, int count, List<YChange> changes) {
        long structSize = Yrs.YDELTA_OUT_LAYOUT.byteSize();
        MemorySegment array = deltaPtr.reinterpret(structSize * count);

        for (int i = 0; i < count; i++) {
            MemorySegment entry = array.asSlice(i * structSize, structSize);

            byte tag = (byte) DELTA_OUT_TAG.get(entry, 0L);
            int len = (int) DELTA_OUT_LEN.get(entry, 0L);

            if (tag == Yrs.Y_EVENT_CHANGE_ADD) {
                // For INSERT, read the inserted content
                MemorySegment insertPtr = (MemorySegment) DELTA_OUT_INSERT.get(entry, 0L);
                String content = "";
                if (insertPtr.address() != 0) {
                    MemorySegment strPtr = Yrs.youtputReadString(insertPtr);
                    if (strPtr.address() != 0) {
                        content = Yrs.readAndFreeString(strPtr);
                    }
                }
                // TODO: Parse attributes if needed
                changes.add(new PanamaYTextChange(content, null));
            } else if (tag == Yrs.Y_EVENT_CHANGE_DELETE) {
                changes.add(new PanamaYTextChange(YChange.Type.DELETE, len));
            } else if (tag == Yrs.Y_EVENT_CHANGE_RETAIN) {
                changes.add(new PanamaYTextChange(YChange.Type.RETAIN, len));
            }
        }
    }

    /**
     * Gets the document for error handling.
     */
    private PanamaYDoc getDoc() {
        if (target instanceof PanamaYXmlElement) {
            return ((PanamaYXmlElement) target).getDoc();
        } else if (target instanceof PanamaYXmlText) {
            return ((PanamaYXmlText) target).getDoc();
        } else if (target instanceof PanamaYXmlFragment) {
            return ((PanamaYXmlFragment) target).getDoc();
        }
        return null;
    }

    @Override
    public long getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public YObserver getObserver() {
        return observer;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    LOG.fine("[Panama] Closing XML subscription #" + subscriptionId);
                    closed = true;

                    if (!nativeSubscription.equals(MemorySegment.NULL)) {
                        Yrs.yunobserve(nativeSubscription);
                    }

                    subscriptionArena.close();
                }
            }
        }
    }
}
