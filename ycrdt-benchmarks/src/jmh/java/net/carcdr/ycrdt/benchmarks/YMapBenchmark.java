package net.carcdr.ycrdt.benchmarks;

import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YMap;
import net.carcdr.ycrdt.YTransaction;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.TimeUnit;

/**
 * YMap operation benchmarks comparing JNI and Panama implementations.
 *
 * <p>Measures performance of map operations including set, get, delete,
 * and bulk operations.</p>
 *
 * <p>Note: getString and getDouble benchmarks require JNI implementation only
 * due to known issues in Panama implementation.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class YMapBenchmark {

    @Param({"jni", "panama"})
    private String implementation;

    private YBinding binding;
    private YDoc doc;
    private YMap map;

    @Setup(Level.Iteration)
    public void setup() {
        binding = "jni".equals(implementation)
            ? YBindingFactory.jni()
            : YBindingFactory.panama();
        doc = binding.createDoc();
        map = doc.getMap("data");
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        map.close();
        doc.close();
    }

    // ===== Basic Operations - String =====

    /**
     * Benchmark: Set string value.
     */
    @Benchmark
    public void setString() {
        map.setString("key", "value");
    }

    // ===== Basic Operations - Double =====

    /**
     * Benchmark: Set double value.
     */
    @Benchmark
    public void setDouble() {
        map.setDouble("key", 42.0);
    }

    // ===== Key Operations =====

    /**
     * Benchmark: Check if key exists.
     */
    @Benchmark
    public boolean containsKey() {
        return map.containsKey("key");
    }

    /**
     * Benchmark: Remove key.
     */
    @Benchmark
    public void remove() {
        map.remove("key");
    }

    /**
     * Benchmark: Get all keys.
     */
    @Benchmark
    public String[] keys() {
        return map.keys();
    }

    /**
     * Benchmark: Clear all entries.
     */
    @Benchmark
    public void clear() {
        map.clear();
    }

    // ===== Read Operations =====

    /**
     * Benchmark: Get map size.
     */
    @Benchmark
    public int size() {
        return map.size();
    }

    /**
     * Benchmark: Convert map to JSON.
     */
    @Benchmark
    public String toJson() {
        return map.toJson();
    }

    // ===== Bulk Operations =====

    /**
     * Benchmark: Bulk set strings (implicit transactions).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void bulkSetStrings() {
        for (int i = 0; i < 1000; i++) {
            map.setString("key" + i, "value" + i);
        }
    }

    /**
     * Benchmark: Bulk set strings (explicit transaction).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void transactionalBulkSetStrings() {
        try (YTransaction txn = doc.beginTransaction()) {
            for (int i = 0; i < 1000; i++) {
                map.setString(txn, "key" + i, "value" + i);
            }
        }
    }

    /**
     * Benchmark: Bulk set doubles (implicit transactions).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void bulkSetDoubles() {
        for (int i = 0; i < 1000; i++) {
            map.setDouble("key" + i, (double) i);
        }
    }

    /**
     * Benchmark: Bulk set doubles (explicit transaction).
     */
    @Benchmark
    @OperationsPerInvocation(1000)
    public void transactionalBulkSetDoubles() {
        try (YTransaction txn = doc.beginTransaction()) {
            for (int i = 0; i < 1000; i++) {
                map.setDouble(txn, "key" + i, (double) i);
            }
        }
    }

    // ===== Scale Tests =====

    /**
     * State for pre-populated map benchmarks (10k entries).
     */
    @State(Scope.Thread)
    public static class PrePopulatedMap {
        @Param({"jni", "panama"})
        private String implementation;

        YDoc doc;
        YMap map;

        @Setup(Level.Trial)
        public void setup() {
            YBinding binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            map = doc.getMap("data");
            // Pre-populate with 10k entries
            for (int i = 0; i < 10000; i++) {
                map.setString("key" + i, "value" + i);
            }
        }

        @TearDown(Level.Trial)
        public void teardown() {
            map.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Check key existence in large map.
     */
    @Benchmark
    public boolean containsKeyInLargeMap(PrePopulatedMap state) {
        return state.map.containsKey("key5000");
    }

    /**
     * Benchmark: Insert into large map.
     */
    @Benchmark
    public void insertIntoLargeMap(PrePopulatedMap state) {
        state.map.setString("new-key", "new-value");
    }

    /**
     * Benchmark: Get keys from large map.
     */
    @Benchmark
    public String[] keysFromLargeMap(PrePopulatedMap state) {
        return state.map.keys();
    }

    /**
     * Benchmark: Convert large map to JSON.
     */
    @Benchmark
    public String largeMapToJson(PrePopulatedMap state) {
        return state.map.toJson();
    }
}
