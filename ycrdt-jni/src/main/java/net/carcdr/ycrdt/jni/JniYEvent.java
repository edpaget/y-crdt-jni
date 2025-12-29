package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YChange;
import net.carcdr.ycrdt.YEvent;

import java.util.Collections;
import java.util.List;

/**
 * Represents a change event from a Y-CRDT data structure.
 *
 * <p>Events contain:
 * <ul>
 *   <li>The target object that changed</li>
 *   <li>A list of changes (deltas) describing the modifications</li>
 *   <li>Metadata about the change origin</li>
 * </ul>
 *
 * <p>Events are immutable and thread-safe.</p>
 *
 * @see YChange
 */
public final class JniYEvent implements YEvent {

    private final Object target;
    private final List<? extends YChange> changes;
    private final String origin;

    /**
     * Package-private constructor. Events are created by the native layer.
     *
     * @param target the Y type that changed
     * @param changes the list of changes
     * @param origin optional origin identifier (may be null)
     */
    JniYEvent(Object target, List<? extends YChange> changes, String origin) {
        this.target = target;
        this.changes = Collections.unmodifiableList(changes);
        this.origin = origin;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public List<? extends YChange> getChanges() {
        return changes;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return "JniYEvent{target=" + target.getClass().getSimpleName()
             + ", changes=" + changes.size()
             + ", origin=" + origin + "}";
    }
}
