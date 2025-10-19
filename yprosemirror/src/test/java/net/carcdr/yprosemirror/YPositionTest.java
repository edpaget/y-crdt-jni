package net.carcdr.yprosemirror;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for YPosition class.
 */
public class YPositionTest {

    @Test
    public void testCreateRootPosition() {
        YPosition pos = YPosition.at(0);

        assertNotNull("Position should not be null", pos);
        assertEquals("Offset should be 0", 0, pos.getOffset());
        assertEquals("Depth should be 0", 0, pos.getDepth());
        assertTrue("Should be root position", pos.isRoot());
        assertArrayEquals("Path should be empty", new int[0], pos.getPath());
    }

    @Test
    public void testCreateChildPosition() {
        YPosition pos = YPosition.at(2, 5);

        assertNotNull("Position should not be null", pos);
        assertEquals("Offset should be 5", 5, pos.getOffset());
        assertEquals("Depth should be 1", 1, pos.getDepth());
        assertFalse("Should not be root position", pos.isRoot());
        assertArrayEquals("Path should be [2]", new int[] {2}, pos.getPath());
    }

    @Test
    public void testCreateCustomPathPosition() {
        YPosition pos = YPosition.at(new int[] {1, 3, 7}, 10);

        assertNotNull("Position should not be null", pos);
        assertEquals("Offset should be 10", 10, pos.getOffset());
        assertEquals("Depth should be 3", 3, pos.getDepth());
        assertFalse("Should not be root position", pos.isRoot());
        assertArrayEquals("Path should match", new int[] {1, 3, 7}, pos.getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeOffset() {
        YPosition.at(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeChildIndex() {
        YPosition.at(-1, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPath() {
        YPosition.at(null, 0);
    }

    @Test
    public void testGetPath() {
        YPosition pos = YPosition.at(new int[] {1, 2, 3}, 0);
        int[] path = pos.getPath();

        assertNotNull("Path should not be null", path);
        assertArrayEquals("Path should match", new int[] {1, 2, 3}, path);

        // Modify returned path - should not affect position (defensive copy)
        path[0] = 999;
        assertArrayEquals("Position path should not change",
            new int[] {1, 2, 3}, pos.getPath());
    }

    @Test
    public void testChild() {
        YPosition parent = YPosition.at(2);  // Root position with offset 2
        YPosition child = parent.child(3, 5);

        assertNotNull("Child position should not be null", child);
        assertEquals("Child offset should be 5", 5, child.getOffset());
        assertEquals("Child depth should be parent + 1", 1, child.getDepth());
        assertArrayEquals("Child path should extend parent",
            new int[] {3}, child.getPath());

        // Parent should be unchanged
        assertEquals("Parent depth should still be 0", 0, parent.getDepth());
        assertArrayEquals("Parent path should be unchanged",
            new int[] {}, parent.getPath());
    }

    @Test
    public void testParent() {
        YPosition child = YPosition.at(new int[] {1, 2}, 5);
        YPosition parent = child.parent();

        assertNotNull("Parent position should not be null", parent);
        assertEquals("Parent offset should be last path element", 2, parent.getOffset());
        assertEquals("Parent depth should be child - 1", 1, parent.getDepth());
        assertArrayEquals("Parent path should be shorter",
            new int[] {1}, parent.getPath());

        // Child should be unchanged
        assertEquals("Child depth should still be 2", 2, child.getDepth());
        assertArrayEquals("Child path should be unchanged",
            new int[] {1, 2}, child.getPath());
    }

    @Test
    public void testParentOfRoot() {
        YPosition root = YPosition.at(0);
        YPosition parent = root.parent();

        assertNull("Parent of root should be null", parent);
    }

    @Test
    public void testEquals() {
        YPosition pos1 = YPosition.at(new int[] {1, 2}, 5);
        YPosition pos2 = YPosition.at(new int[] {1, 2}, 5);
        YPosition pos3 = YPosition.at(new int[] {1, 2}, 6); // Different offset
        YPosition pos4 = YPosition.at(new int[] {1, 3}, 5); // Different path

        assertEquals("Same positions should be equal", pos1, pos2);
        assertEquals("Hash codes should match", pos1.hashCode(), pos2.hashCode());

        assertNotEquals("Different offsets should not be equal", pos1, pos3);
        assertNotEquals("Different paths should not be equal", pos1, pos4);

        // Self-equality
        assertEquals("Position should equal itself", pos1, pos1);

        // Null check
        assertNotEquals("Position should not equal null", pos1, null);

        // Different class
        assertNotEquals("Position should not equal different class", pos1, "string");
    }

    @Test
    public void testToString() {
        YPosition pos = YPosition.at(new int[] {1, 2, 3}, 7);
        String str = pos.toString();

        assertNotNull("toString should not return null", str);
        assertTrue("String should contain path info", str.contains("1"));
        assertTrue("String should contain path info", str.contains("2"));
        assertTrue("String should contain path info", str.contains("3"));
        assertTrue("String should contain offset", str.contains("7"));
    }

    @Test
    public void testNestedChildCreation() {
        YPosition root = YPosition.at(0);
        YPosition level1 = root.child(1, 0);
        YPosition level2 = level1.child(2, 0);
        YPosition level3 = level2.child(3, 0);

        assertEquals("Level 1 depth", 1, level1.getDepth());
        assertEquals("Level 2 depth", 2, level2.getDepth());
        assertEquals("Level 3 depth", 3, level3.getDepth());

        assertArrayEquals("Level 3 path", new int[] {1, 2, 3}, level3.getPath());
    }

    @Test
    public void testNestedParentNavigation() {
        YPosition level3 = YPosition.at(new int[] {1, 2, 3}, 7);
        YPosition level2 = level3.parent();
        YPosition level1 = level2.parent();
        YPosition root = level1.parent();

        assertNotNull("Level 2 should exist", level2);
        assertNotNull("Level 1 should exist", level1);
        assertNotNull("Root should exist", root);

        assertEquals("Level 2 depth", 2, level2.getDepth());
        assertEquals("Level 1 depth", 1, level1.getDepth());
        assertEquals("Root depth", 0, root.getDepth());
        assertEquals("Root offset should be last path element of level1", 1, root.getOffset());

        // Parent of root should be null
        assertNull("Parent of root should be null", root.parent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChildNegativeIndex() {
        YPosition pos = YPosition.at(0);
        pos.child(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChildNegativeOffset() {
        YPosition pos = YPosition.at(0);
        pos.child(0, -1);
    }

    @Test
    public void testRootPositionWithNonZeroOffset() {
        YPosition pos = YPosition.at(5);

        assertTrue("Should be root position", pos.isRoot());
        assertEquals("Offset should be 5", 5, pos.getOffset());
        assertEquals("Depth should be 0", 0, pos.getDepth());
    }

    @Test
    public void testImmutability() {
        int[] originalPath = {1, 2, 3};
        YPosition pos = YPosition.at(originalPath, 5);

        // Modify original array
        originalPath[0] = 999;

        // Position should not be affected
        assertArrayEquals("Position path should not change",
            new int[] {1, 2, 3}, pos.getPath());
    }
}
