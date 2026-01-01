package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(PanamaYSubscription.class.getName());
    private static final HexFormat HEX = HexFormat.of();
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

            LOG.info("[Panama] Subscription #" + subscriptionId + " created for doc "
                + target.getGuid() + " on thread " + Thread.currentThread().getName());
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
        String threadName = Thread.currentThread().getName();
        LOG.info("[Panama] onUpdate called: sub=#" + subscriptionId
            + " len=" + len
            + " data=" + (data.equals(MemorySegment.NULL) ? "NULL" : "0x" + Long.toHexString(data.address()))
            + " thread=" + threadName
            + " closed=" + closed);

        if (closed) {
            LOG.warning("[Panama] onUpdate ignored - subscription #" + subscriptionId + " is closed");
            return;
        }

        try {
            // Read the update data into a byte array
            byte[] update;
            if (len > 0 && !data.equals(MemorySegment.NULL)) {
                MemorySegment reinterpreted = data.reinterpret(len);
                update = reinterpreted.toArray(ValueLayout.JAVA_BYTE);
                LOG.info("[Panama] sub=#" + subscriptionId + " update bytes ("
                    + update.length + "): " + HEX.formatHex(update));
            } else {
                update = new byte[0];
                LOG.warning("[Panama] sub=#" + subscriptionId + " empty update (len="
                    + len + ", data null=" + data.equals(MemorySegment.NULL) + ")");
            }

            // Invoke the Java observer
            LOG.info("[Panama] sub=#" + subscriptionId + " invoking observer...");
            // Note: yffi's v1 observer doesn't provide origin information
            observer.onUpdate(update, null);
            LOG.info("[Panama] sub=#" + subscriptionId + " observer completed");
        } catch (Exception e) {
            LOG.severe("[Panama] sub=#" + subscriptionId + " observer threw exception: " + e);
            // Use configured error handler - observers should not break each other
            target.getObserverErrorHandler().handleError(e, target);
        } catch (Throwable t) {
            LOG.severe("[Panama] sub=#" + subscriptionId + " observer threw throwable: " + t);
            // For Errors and other Throwables, wrap and use error handler
            target.getObserverErrorHandler().handleError(
                new RuntimeException("Observer threw " + t.getClass().getSimpleName(), t), target);
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
                    LOG.info("[Panama] Closing subscription #" + subscriptionId
                        + " on thread " + Thread.currentThread().getName());
                    closed = true;

                    // First unsubscribe from native library
                    if (!nativeSubscription.equals(MemorySegment.NULL)) {
                        Yrs.yunobserve(nativeSubscription);
                    }

                    // Then close the arena (which invalidates the upcall stub)
                    subscriptionArena.close();
                    LOG.info("[Panama] Subscription #" + subscriptionId + " closed");
                }
            }
        }
    }
}
