package net.carcdr.ycrdt;

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
 * @see YObserver
 * @see YChange
 */
public final class YEvent {

    private final Object target;
    private final List<YChange> changes;
    private final String origin;

    /**
     * Package-private constructor. Events are created by the native layer.
     *
     * @param target the Y type that changed
     * @param changes the list of changes
     * @param origin optional origin identifier (may be null)
     */
    YEvent(Object target, List<YChange> changes, String origin) {
        this.target = target;
        this.changes = Collections.unmodifiableList(changes);
        this.origin = origin;
    }

    /**
     * Gets the Y-CRDT object that changed.
     *
     * @return the target object (YText, YArray, YMap, YXmlFragment, etc.)
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Gets the list of changes that occurred.
     *
     * <p>For text-like types (YText, YXmlText), changes are ordered deltas
     * describing insertions, deletions, and retains.</p>
     *
     * <p>For container types (YArray, YMap, YXmlFragment), changes describe
     * added, removed, or modified items.</p>
     *
     * @return an immutable list of changes
     */
    public List<YChange> getChanges() {
        return changes;
    }

    /**
     * Gets the origin identifier for this change.
     *
     * <p>Origins are optional string identifiers that can be used to
     * distinguish between different sources of changes (e.g., "local",
     * "remote", "undo"). This is set by the transaction that created
     * the change.</p>
     *
     * @return the origin string, or null if not set
     */
    public String getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return "YEvent{target=" + target.getClass().getSimpleName()
             + ", changes=" + changes.size()
             + ", origin=" + origin + "}";
    }
}
