package net.carcdr.ycrdt.benchmarks;

import net.carcdr.ycrdt.YArray;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YTransaction;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.TimeUnit;

/**
 * YArray operation benchmarks.
 *
 * <p>Measures performance of array operations including basic operations,
 * bulk operations, and different data types.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class YArrayBenchmark {

    private YDoc doc;
    private YArray array;

    @Setup(Level.Iteration)
    public void setup() {
        doc = new YDoc();
        array = doc.getArray("list");
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        array.close();
        doc.close();
    }

    // ===== Basic Operations - String =====

    /**
     * Benchmark: Push string to end of array.
     */
    @Benchmark
    public void pushString() {
        array.pushString("test");
    }

    /**
     * Benchmark: Insert string at beginning.
     */
    @Benchmark
    public void insertStringAtBeginning() {
        array.insertString(0, "test");
    }

    /**
     * Benchmark: Insert string at middle.
     */
    @Benchmark
    public void insertStringAtMiddle() {
        int len = array.length();
        array.insertString(len / 2, "test");
    }

    /**
     * Benchmark: Get string from array.
     */
    @Benchmark
    public String getString() {
        int len = array.length();
        if (len > 0) {
            return array.getString(len - 1);
        }
        return null;
    }

    // ===== Basic Operations - Double =====

    /**
     * Benchmark: Push double to end of array.
     */
    @Benchmark
    public void pushDouble() {
        array.pushDouble(42.0);
    }

    /**
     * Benchmark: Insert double at beginning.
     */
    @Benchmark
    public void insertDoubleAtBeginning() {
        array.insertDouble(0, 42.0);
    }

    /**
     * Benchmark: Get double from array.
     */
    @Benchmark
    public Double getDouble() {
        int len = array.length();
        if (len > 0) {
            return array.getDouble(len - 1);
        }
        return null;
    }

    // ===== Delete Operations =====

    /**
     * Benchmark: Delete from end of array.
     */
    @Benchmark
    public void deleteFromEnd() {
        int len = array.length();
        if (len > 0) {
            array.remove(len - 1, 1);
        }
    }

    // ===== Read Operations =====

    /**
     * Benchmark: Get array length.
     */
    @Benchmark
    public int readLength() {
        return array.length();
    }

    /**
     * Benchmark: Convert array to JSON.
     */
    @Benchmark
    public String toJson() {
        return array.toJson();
    }

    // ===== Bulk Operations =====

    /**
     * Benchmark: Bulk insert strings (implicit transactions).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void bulkInsertStrings() {
        for (int i = 0; i < 1000; i++) {
            array.pushString("item" + i);
        }
    }

    /**
     * Benchmark: Bulk insert strings (explicit transaction).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void transactionalBulkInsertStrings() {
        try (YTransaction txn = doc.beginTransaction()) {
            for (int i = 0; i < 1000; i++) {
                array.pushString(txn, "item" + i);
            }
        }
    }

    /**
     * Benchmark: Bulk insert doubles (implicit transactions).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void bulkInsertDoubles() {
        for (int i = 0; i < 1000; i++) {
            array.pushDouble((double) i);
        }
    }

    /**
     * Benchmark: Bulk insert doubles (explicit transaction).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void transactionalBulkInsertDoubles() {
        try (YTransaction txn = doc.beginTransaction()) {
            for (int i = 0; i < 1000; i++) {
                array.pushDouble(txn, (double) i);
            }
        }
    }

    // ===== Scale Tests =====

    /**
     * State for large array benchmarks (10k elements).
     */
    @State(Scope.Thread)
    public static class LargeArray {
        YDoc doc;
        YArray array;

        @Setup(Level.Trial)
        public void setup() {
            doc = new YDoc();
            array = doc.getArray("list");
            // Pre-populate with 10k elements
            for (int i = 0; i < 10000; i++) {
                array.pushString("item" + i);
            }
        }

        @TearDown(Level.Trial)
        public void teardown() {
            array.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Access element in large array.
     */
    @Benchmark
    public String accessInLargeArray(LargeArray state) {
        return state.array.getString(5000);
    }

    /**
     * Benchmark: Insert into large array.
     */
    @Benchmark
    public void insertIntoLargeArray(LargeArray state) {
        state.array.insertString(5000, "new-item");
    }

    /**
     * Benchmark: Convert large array to JSON.
     */
    @Benchmark
    public String largeArrayToJson(LargeArray state) {
        return state.array.toJson();
    }
}
