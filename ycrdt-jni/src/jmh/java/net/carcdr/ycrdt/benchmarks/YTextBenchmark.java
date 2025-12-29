package net.carcdr.ycrdt.benchmarks;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.jni.JniYDoc;
import net.carcdr.ycrdt.YText;
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

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * YText operation benchmarks.
 *
 * <p>Measures performance of text operations including basic operations,
 * workload patterns, and scale tests with varying document sizes.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class YTextBenchmark {

    private YDoc doc;
    private YText text;

    @Setup(Level.Iteration)
    public void setup() {
        doc = new JniYDoc();
        text = doc.getText("content");
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        text.close();
        doc.close();
    }

    // ===== Basic Operations =====

    /**
     * Benchmark: Insert text at the end (push operation).
     */
    @Benchmark
    public void insertAtEnd() {
        text.push("x");
    }

    /**
     * Benchmark: Insert text at the beginning.
     */
    @Benchmark
    public void insertAtBeginning() {
        text.insert(0, "x");
    }

    /**
     * Benchmark: Insert text at the middle position.
     */
    @Benchmark
    public void insertAtMiddle() {
        int len = text.length();
        text.insert(len / 2, "x");
    }

    /**
     * Benchmark: Delete text from the end.
     */
    @Benchmark
    public void deleteFromEnd() {
        int len = text.length();
        if (len > 0) {
            text.delete(len - 1, 1);
        }
    }

    /**
     * Benchmark: Get text length.
     */
    @Benchmark
    public int readLength() {
        return text.length();
    }

    /**
     * Benchmark: Convert to string.
     */
    @Benchmark
    public String readToString() {
        return text.toString();
    }

    // ===== Workload Patterns =====

    /**
     * Benchmark: Sequential inserts (simulates typing).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void sequentialInserts() {
        for (int i = 0; i < 1000; i++) {
            text.push("x");
        }
    }

    /**
     * Benchmark: Random inserts throughout document.
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void randomInserts() {
        Random rand = new Random(42);
        for (int i = 0; i < 1000; i++) {
            int pos = rand.nextInt(text.length() + 1);
            text.insert(pos, "x");
        }
    }

    /**
     * Benchmark: Transactional inserts (batched).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void transactionalInserts() {
        try (YTransaction txn = doc.beginTransaction()) {
            for (int i = 0; i < 1000; i++) {
                text.push(txn, "x");
            }
        }
    }

    /**
     * Benchmark: Mixed operations (insert, delete, read).
     */
    @Benchmark
    @OperationsPerInvocation(300)
    public void mixedOperations() {
        for (int i = 0; i < 100; i++) {
            text.push("test");
            text.length();
            if (text.length() > 50) {
                text.delete(0, 10);
            }
        }
    }

    // ===== Scale Tests =====

    /**
     * State for small document benchmarks (100 characters).
     */
    @State(Scope.Thread)
    public static class SmallDocument {
        YDoc doc;
        YText text;

        @Setup(Level.Trial)
        public void setup() {
            doc = new JniYDoc();
            text = doc.getText("content");
            // 100 characters
            for (int i = 0; i < 100; i++) {
                text.push("x");
            }
        }

        @TearDown(Level.Trial)
        public void teardown() {
            text.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Insert into small document.
     */
    @Benchmark
    public void insertIntoSmallDocument(SmallDocument state) {
        state.text.insert(50, "test");
    }

    /**
     * Benchmark: Read from small document.
     */
    @Benchmark
    public String readFromSmallDocument(SmallDocument state) {
        return state.text.toString();
    }

    /**
     * State for medium document benchmarks (10KB).
     */
    @State(Scope.Thread)
    public static class MediumDocument {
        YDoc doc;
        YText text;

        @Setup(Level.Trial)
        public void setup() {
            doc = new JniYDoc();
            text = doc.getText("content");
            // 10KB document
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10 * 1024; i++) {
                sb.append("x");
            }
            text.push(sb.toString());
        }

        @TearDown(Level.Trial)
        public void teardown() {
            text.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Insert into medium document.
     */
    @Benchmark
    public void insertIntoMediumDocument(MediumDocument state) {
        state.text.insert(5000, "test");
    }

    /**
     * Benchmark: Read from medium document.
     */
    @Benchmark
    public String readFromMediumDocument(MediumDocument state) {
        return state.text.toString();
    }

    /**
     * State for large document benchmarks (1MB).
     */
    @State(Scope.Thread)
    public static class LargeDocument {
        YDoc doc;
        YText text;

        @Setup(Level.Trial)
        public void setup() {
            doc = new JniYDoc();
            text = doc.getText("content");
            // 1MB document
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1024 * 1024; i++) {
                sb.append("x");
            }
            text.push(sb.toString());
        }

        @TearDown(Level.Trial)
        public void teardown() {
            text.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Insert into large document.
     */
    @Benchmark
    public void insertIntoLargeDocument(LargeDocument state) {
        state.text.insert(500000, "test");
    }

    /**
     * Benchmark: Read from large document.
     */
    @Benchmark
    public String readFromLargeDocument(LargeDocument state) {
        return state.text.toString();
    }
}
