package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicLong;

import net.carcdr.ycrdt.UpdateObserver;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YSubscription for document update observers.
 *
 * <p>This class manages the lifecycle of Panama upcall stubs, which are native
 * function pointers that invoke Java code. The Arena holding the upcall stub
 * must remain open for the entire lifetime of the subscription.</p>
 */
public class PanamaYSubscription implements YSubscription {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final long subscriptionId;
    private final PanamaYDoc target;
    private final UpdateObserver observer;
    private final Arena subscriptionArena;
    private final MemorySegment nativeSubscription;
    private volatile boolean closed = false;

    /**
     * Creates a new subscription for document updates.
     *
     * @param target the document being observed
     * @param observer the observer callback
     */
    PanamaYSubscription(PanamaYDoc target, UpdateObserver observer) {
        this.subscriptionId = ID_GENERATOR.incrementAndGet();
        this.target = target;
        this.observer = observer;

        // Create a shared arena that will live as long as the subscription
        this.subscriptionArena = Arena.ofShared();

        try {
            // Create the upcall stub for the native callback
            MethodHandle callbackHandle = MethodHandles.lookup()
                .findVirtual(PanamaYSubscription.class, "onUpdate",
                    MethodType.methodType(void.class,
                        MemorySegment.class, int.class, MemorySegment.class))
                .bindTo(this);

            MemorySegment callbackStub = Linker.nativeLinker()
                .upcallStub(callbackHandle, Yrs.UPDATE_CALLBACK_DESCRIPTOR, subscriptionArena);

            // Register the observer with the native library
            // We pass NULL for state since we bound the callback to this instance
            this.nativeSubscription = Yrs.ydocObserveUpdatesV1(
                target.getDocPtr(), MemorySegment.NULL, callbackStub);

            if (this.nativeSubscription.equals(MemorySegment.NULL)) {
                subscriptionArena.close();
                throw new RuntimeException("Failed to register update observer");
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            subscriptionArena.close();
            throw new RuntimeException("Failed to create upcall stub", e);
        }
    }

    /**
     * Native callback method invoked when the document is updated.
     *
     * <p>This method is called from native code via the upcall stub.</p>
     *
     * @param state the state pointer (unused, we bound to this instance)
     * @param len the length of the update data
     * @param data pointer to the update data
     */
    @SuppressWarnings("unused") // Called from native via upcall stub
    private void onUpdate(MemorySegment state, int len, MemorySegment data) {
        if (closed) {
            return;
        }

        try {
            // Read the update data into a byte array
            byte[] update;
            if (len > 0 && !data.equals(MemorySegment.NULL)) {
                MemorySegment reinterpreted = data.reinterpret(len);
                update = reinterpreted.toArray(ValueLayout.JAVA_BYTE);
            } else {
                update = new byte[0];
            }

            // Invoke the Java observer
            // Note: yffi's v1 observer doesn't provide origin information
            observer.onUpdate(update, null);
        } catch (Throwable e) {
            // Log but don't propagate exceptions from callbacks
            System.err.println("Exception in update observer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public long getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public YObserver getObserver() {
        // UpdateObserver is not a YObserver, return null per interface contract
        return null;
    }

    /**
     * Gets the UpdateObserver for this subscription.
     *
     * @return the update observer
     */
    public UpdateObserver getUpdateObserver() {
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
                    closed = true;

                    // First unsubscribe from native library
                    if (!nativeSubscription.equals(MemorySegment.NULL)) {
                        Yrs.yunobserve(nativeSubscription);
                    }

                    // Then close the arena (which invalidates the upcall stub)
                    subscriptionArena.close();
                }
            }
        }
    }
}
