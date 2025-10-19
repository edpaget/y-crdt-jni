package net.carcdr.yprosemirror;

import java.util.Objects;

/**
 * Represents a position in a Y-CRDT document structure.
 *
 * <p>This class provides position tracking for Y-CRDT XML structures used in ProseMirror
 * integration. A position consists of a path through the document tree, identifying
 * a specific location within nested elements.
 *
 * <p><strong>Position Model:</strong>
 * <ul>
 *   <li>Positions are represented as integer offsets within parent containers</li>
 *   <li>The root position has offset 0 in the root fragment</li>
 *   <li>Child positions specify offset within their parent element</li>
 *   <li>Text positions specify offset within a text node</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Position at offset 5 in root fragment
 * YPosition pos = YPosition.at(5);
 *
 * // Position at offset 3 in second child (index 1) of root
 * YPosition pos = YPosition.at(1, 3);
 *
 * // Check if positions are equal
 * boolean same = pos1.equals(pos2);
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class YPosition {

    private final int[] path;
    private final int offset;

    /**
     * Creates a position with the given path and offset.
     *
     * @param path the path through the document tree (indices of parent elements)
     * @param offset the offset within the final container
     * @throws IllegalArgumentException if path is null or offset is negative
     */
    private YPosition(int[] path, int offset) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }

        // Defensive copy
        this.path = path.clone();
        this.offset = offset;
    }

    /**
     * Creates a position at the given offset in the root fragment.
     *
     * @param offset the offset in the root fragment
     * @return a new position
     * @throws IllegalArgumentException if offset is negative
     */
    public static YPosition at(int offset) {
        return new YPosition(new int[0], offset);
    }

    /**
     * Creates a position at the given offset within a child element.
     *
     * @param childIndex the index of the child element in the parent
     * @param offset the offset within that child
     * @return a new position
     * @throws IllegalArgumentException if any parameter is negative
     */
    public static YPosition at(int childIndex, int offset) {
        if (childIndex < 0) {
            throw new IllegalArgumentException("Child index cannot be negative");
        }
        return new YPosition(new int[] {childIndex}, offset);
    }

    /**
     * Creates a position with a custom path.
     *
     * @param path the path through the document (array of child indices)
     * @param offset the offset at the final position
     * @return a new position
     * @throws IllegalArgumentException if path is null or offset is negative
     */
    public static YPosition at(int[] path, int offset) {
        return new YPosition(path, offset);
    }

    /**
     * Gets the path through the document tree.
     *
     * <p>The path is an array of child indices. For example:
     * <ul>
     *   <li>[] = root fragment</li>
     *   <li>[2] = third child of root</li>
     *   <li>[2, 1] = second child of third child of root</li>
     * </ul>
     *
     * @return a copy of the path array
     */
    public int[] getPath() {
        return path.clone();
    }

    /**
     * Gets the offset within the container at this position.
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the depth of this position in the document tree.
     *
     * <p>Depth is the length of the path:
     * <ul>
     *   <li>0 = root fragment</li>
     *   <li>1 = direct child of root</li>
     *   <li>2 = grandchild of root</li>
     * </ul>
     *
     * @return the depth (0 for root)
     */
    public int getDepth() {
        return path.length;
    }

    /**
     * Checks if this position is in the root fragment.
     *
     * @return true if this is a root position (depth 0)
     */
    public boolean isRoot() {
        return path.length == 0;
    }

    /**
     * Creates a new position by appending a child index to this position's path.
     *
     * <p>Example: position at [2] with child 3 becomes [2, 3].
     *
     * @param childIndex the child index to append
     * @param offset the offset in the child
     * @return a new position representing the child
     * @throws IllegalArgumentException if childIndex or offset is negative
     */
    public YPosition child(int childIndex, int offset) {
        if (childIndex < 0) {
            throw new IllegalArgumentException("Child index cannot be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }

        int[] newPath = new int[path.length + 1];
        System.arraycopy(path, 0, newPath, 0, path.length);
        newPath[path.length] = childIndex;

        return new YPosition(newPath, offset);
    }

    /**
     * Creates a new position by removing the last element from the path (moving to parent).
     *
     * <p>The parent position will have the last element of the current path as its offset,
     * representing the index of the child container within the parent.
     *
     * @return the parent position, or null if already at root
     */
    public YPosition parent() {
        if (path.length == 0) {
            return null; // Already at root
        }

        int[] newPath = new int[path.length - 1];
        System.arraycopy(path, 0, newPath, 0, path.length - 1);

        // Use the last path element (child index in parent) as the offset
        int parentOffset = path[path.length - 1];
        return new YPosition(newPath, parentOffset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        YPosition that = (YPosition) o;
        return offset == that.offset && java.util.Arrays.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(java.util.Arrays.hashCode(path), offset);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("YPosition{path=[");
        for (int i = 0; i < path.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(path[i]);
        }
        sb.append("], offset=").append(offset).append("}");
        return sb.toString();
    }
}
