package net.carcdr.ycrdt;

import java.util.List;

/**
 * Represents an event triggered by changes to a Y-CRDT data structure.
 */
public interface YEvent {

    /**
     * Returns the Y-CRDT type that was modified.
     *
     * @return the target object (YText, YArray, YMap, etc.)
     */
    Object getTarget();

    /**
     * Returns the list of changes that occurred.
     *
     * @return a list of YChange instances describing the modifications
     */
    List<? extends YChange> getChanges();

    /**
     * Returns the origin identifier for this change.
     *
     * @return the origin string, or null if not specified
     */
    String getOrigin();
}
