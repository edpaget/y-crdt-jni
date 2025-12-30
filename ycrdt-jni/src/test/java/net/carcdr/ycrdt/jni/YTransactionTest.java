package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YText;
import net.carcdr.ycrdt.YTransaction;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the YTransaction class.
 */
public class YTransactionTest {

    @Test
    public void testBeginTransaction() {
        try (YDoc doc = new JniYDoc()) {
            YTransaction txn = doc.beginTransaction();
            assertNotNull("Transaction should be created", txn);
            assertFalse("Transaction should not be closed", txn.isClosed());
            txn.close();
        }
    }

    @Test
    public void testTransactionAutoCommit() {
        try (YDoc doc = new JniYDoc()) {
            YTransaction txn;
            try (YTransaction t = doc.beginTransaction()) {
                txn = t;
                assertFalse("Transaction should not be closed inside try block", txn.isClosed());
            }
            assertTrue("Transaction should be closed after try block", txn.isClosed());
        }
    }

    @Test
    public void testExplicitCommit() {
        try (YDoc doc = new JniYDoc()) {
            YTransaction txn = doc.beginTransaction();
            assertFalse("Transaction should not be closed", txn.isClosed());

            txn.commit();
            assertTrue("Transaction should be closed after commit", txn.isClosed());

            // Committing again should be safe (idempotent)
            txn.commit();
            assertTrue("Transaction should still be closed", txn.isClosed());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetNativePtrAfterClose() {
        try (YDoc doc = new JniYDoc()) {
            JniYTransaction txn = (JniYTransaction) doc.beginTransaction();
            txn.close();
            txn.getNativePtr(); // Should throw IllegalStateException
        }
    }

    @Test
    public void testTransactionCallback() {
        try (YDoc doc = new JniYDoc()) {
            final boolean[] executed = {false};

            doc.transaction(txn -> {
                assertNotNull("Transaction should not be null", txn);
                assertFalse("Transaction should not be closed", txn.isClosed());
                executed[0] = true;
            });

            assertTrue("Callback should have been executed", executed[0]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransactionCallbackNull() {
        try (YDoc doc = new JniYDoc()) {
            doc.transaction(null); // Should throw IllegalArgumentException
        }
    }

    @Test
    public void testMultipleTransactions() {
        try (YDoc doc = new JniYDoc()) {
            // Create and close multiple transactions sequentially
            try (YTransaction txn1 = doc.beginTransaction()) {
                assertFalse("Transaction 1 should not be closed", txn1.isClosed());
            }

            try (YTransaction txn2 = doc.beginTransaction()) {
                assertFalse("Transaction 2 should not be closed", txn2.isClosed());
            }

            try (YTransaction txn3 = doc.beginTransaction()) {
                assertFalse("Transaction 3 should not be closed", txn3.isClosed());
            }
        }
    }

    @Test
    public void testSequentialTransactionCallbacks() {
        try (YDoc doc = new JniYDoc()) {
            final int[] count = {0};

            // First transaction
            doc.transaction(txn1 -> {
                assertNotNull("First transaction should not be null", txn1);
                count[0]++;
            });

            // Second transaction (sequential, not nested)
            doc.transaction(txn2 -> {
                assertNotNull("Second transaction should not be null", txn2);
                count[0]++;
            });

            assertEquals("Both callbacks should have executed", 2, count[0]);
        }
    }

    // Note: Nested transactions are NOT supported because TransactionMut
    // requires exclusive mutable access to the Doc. Attempting to create
    // a transaction while another is active will cause a deadlock.
    // This is a limitation of the underlying yrs library.

    @Test(expected = IllegalStateException.class)
    public void testTransactionAfterDocClose() {
        YDoc doc = new JniYDoc();
        doc.close();
        doc.beginTransaction(); // Should throw IllegalStateException
    }

    @Test
    public void testTransactionGetDoc() {
        try (YDoc doc = new JniYDoc()) {
            try (JniYTransaction txn = (JniYTransaction) doc.beginTransaction()) {
                YDoc retrievedDoc = txn.getDoc();
                assertNotNull("Retrieved doc should not be null", retrievedDoc);
                assertEquals("Retrieved doc should be the same instance", doc, retrievedDoc);
            }
        }
    }

    @Test
    public void testMultipleCommits() {
        try (YDoc doc = new JniYDoc()) {
            YTransaction txn = doc.beginTransaction();

            txn.commit();
            assertTrue("Transaction should be closed", txn.isClosed());

            // Multiple commits should be safe
            txn.commit();
            txn.commit();
            assertTrue("Transaction should still be closed", txn.isClosed());
        }
    }

    @Test
    public void testTransactionLifecycle() {
        try (YDoc doc = new JniYDoc()) {
            JniYTransaction txn = (JniYTransaction) doc.beginTransaction();

            // Transaction starts open
            assertFalse("Transaction should be open initially", txn.isClosed());

            // Transaction can provide native pointer when open
            long nativePtr = txn.getNativePtr();
            assertTrue("Native pointer should be non-zero", nativePtr != 0);

            // Close the transaction
            txn.close();
            assertTrue("Transaction should be closed", txn.isClosed());

            // Closing again is safe
            txn.close();
            assertTrue("Transaction should still be closed", txn.isClosed());
        }
    }

    @Test
    public void testCallbackExceptionDoesNotLeakTransaction() {
        try (YDoc doc = new JniYDoc()) {
            try {
                doc.transaction(txn -> {
                    assertFalse("Transaction should be open", txn.isClosed());
                    throw new RuntimeException("Test exception");
                });
            } catch (RuntimeException e) {
                assertEquals("Exception message should match", "Test exception", e.getMessage());
            }

            // Doc should still be usable after exception
            try (YTransaction txn = doc.beginTransaction()) {
                assertFalse("New transaction should work", txn.isClosed());
            }
        }
    }

    @Test
    public void testEmptyTransaction() {
        try (YDoc doc = new JniYDoc()) {
            // Create and immediately close an empty transaction
            try (YTransaction txn = doc.beginTransaction()) {
                // Do nothing
            }

            // Doc should still be usable
            assertFalse("Doc should not be closed", doc.isClosed());
        }
    }

    @Test
    public void testTransactionState() {
        try (YDoc doc = new JniYDoc()) {
            YTransaction txn = doc.beginTransaction();

            assertFalse("New transaction should not be closed", txn.isClosed());

            txn.commit();
            assertTrue("Committed transaction should be closed", txn.isClosed());
        }
    }

    /**
     * Documents known limitation: getText/getArray/getMap cannot be called
     * inside an explicit transaction.
     *
     * <p>The underlying yrs library's get_or_insert_* methods internally call
     * transact_mut() to create a new write transaction. Since yrs uses
     * async_lock::RwLock which does NOT support recursive write locking,
     * calling these methods while already holding a write lock (from an
     * explicit transaction) causes a deadlock.</p>
     *
     * <p>WORKAROUND: Get shared types BEFORE starting the transaction:</p>
     * <pre>{@code
     * try (YText text = doc.getText("test")) {
     *     doc.transaction(txn -> {
     *         text.push("Hello");  // Works!
     *     });
     * }
     * }</pre>
     */
    @Ignore("Known limitation: getText inside transaction deadlocks due to yrs RwLock")
    @Test
    public void testGetTextInsideTransactionDeadlocks() {
        try (YDoc doc = new JniYDoc()) {
            doc.transaction(txn -> {
                // DEADLOCK: getText creates internal transaction, conflicts with outer txn
                try (YText text = doc.getText("test")) {
                    text.push("Hello");
                    assertEquals("Hello", text.toString());
                }
            });
        }
    }
}
