package net.carcdr.ycrdt;

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
        try (YDoc doc = new YDoc()) {
            YTransaction txn = doc.beginTransaction();
            assertNotNull("Transaction should be created", txn);
            assertFalse("Transaction should not be closed", txn.isClosed());
            txn.close();
        }
    }

    @Test
    public void testTransactionAutoCommit() {
        try (YDoc doc = new YDoc()) {
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
        try (YDoc doc = new YDoc()) {
            YTransaction txn = doc.beginTransaction();
            assertFalse("Transaction should not be closed", txn.isClosed());

            txn.commit();
            assertTrue("Transaction should be closed after commit", txn.isClosed());

            // Committing again should be safe (idempotent)
            txn.commit();
            assertTrue("Transaction should still be closed", txn.isClosed());
        }
    }

    @Test
    public void testExplicitRollback() {
        try (YDoc doc = new YDoc()) {
            YTransaction txn = doc.beginTransaction();
            assertFalse("Transaction should not be closed", txn.isClosed());

            txn.rollback();
            assertTrue("Transaction should be closed after rollback", txn.isClosed());

            // Rolling back again should be safe (idempotent)
            txn.rollback();
            assertTrue("Transaction should still be closed", txn.isClosed());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetNativePtrAfterClose() {
        try (YDoc doc = new YDoc()) {
            YTransaction txn = doc.beginTransaction();
            txn.close();
            txn.getNativePtr(); // Should throw IllegalStateException
        }
    }

    @Test
    public void testTransactionCallback() {
        try (YDoc doc = new YDoc()) {
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
        try (YDoc doc = new YDoc()) {
            doc.transaction(null); // Should throw IllegalArgumentException
        }
    }

    @Test
    public void testMultipleTransactions() {
        try (YDoc doc = new YDoc()) {
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
        try (YDoc doc = new YDoc()) {
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
        YDoc doc = new YDoc();
        doc.close();
        doc.beginTransaction(); // Should throw IllegalStateException
    }

    @Test
    public void testTransactionGetDoc() {
        try (YDoc doc = new YDoc()) {
            try (YTransaction txn = doc.beginTransaction()) {
                YDoc retrievedDoc = txn.getDoc();
                assertNotNull("Retrieved doc should not be null", retrievedDoc);
                assertEquals("Retrieved doc should be the same instance", doc, retrievedDoc);
            }
        }
    }

    @Test
    public void testMultipleCommits() {
        try (YDoc doc = new YDoc()) {
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
        try (YDoc doc = new YDoc()) {
            YTransaction txn = doc.beginTransaction();

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
        try (YDoc doc = new YDoc()) {
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
        try (YDoc doc = new YDoc()) {
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
        try (YDoc doc = new YDoc()) {
            YTransaction txn = doc.beginTransaction();

            assertFalse("New transaction should not be closed", txn.isClosed());

            txn.commit();
            assertTrue("Committed transaction should be closed", txn.isClosed());
        }
    }

    @Test
    public void testRollbackState() {
        try (YDoc doc = new YDoc()) {
            YTransaction txn = doc.beginTransaction();

            assertFalse("New transaction should not be closed", txn.isClosed());

            txn.rollback();
            assertTrue("Rolled back transaction should be closed", txn.isClosed());
        }
    }
}
