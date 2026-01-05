package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.YChange;
import net.carcdr.ycrdt.YEvent;

import java.util.Collections;
import java.util.List;

/**
 * Panama implementation of YEvent.
 *
 * <p>Represents an event triggered by changes to a Y-CRDT data structure.</p>
 */
public final class PanamaYEvent implements YEvent {

    private final Object target;
    private final List<YChange> changes;
    private final String origin;

    /**
     * Creates a new event.
     *
     * @param target the Y-CRDT type that was modified
     * @param changes the list of changes
     * @param origin the origin identifier (may be null)
     */
    PanamaYEvent(Object target, List<YChange> changes, String origin) {
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
        return "PanamaYEvent{target=" + target.getClass().getSimpleName()
               + ", changes=" + changes + ", origin=" + origin + "}";
    }
}
