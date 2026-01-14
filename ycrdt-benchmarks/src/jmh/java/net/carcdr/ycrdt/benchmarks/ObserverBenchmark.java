package net.carcdr.ycrdt.benchmarks;

import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YText;
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
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Observer performance benchmarks comparing JNI and Panama implementations.
 *
 * <p>Measures observer registration overhead, notification performance,
 * and behavior under high-frequency updates.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ObserverBenchmark {

    @Param({"jni", "panama"})
    private String implementation;

    private YBinding binding;
    private YDoc doc;
    private YText text;

    @Setup(Level.Iteration)
    public void setup() {
        binding = "jni".equals(implementation)
            ? YBindingFactory.jni()
            : YBindingFactory.panama();
        doc = binding.createDoc();
        text = doc.getText("content");
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        text.close();
        doc.close();
    }

    // ===== Registration Overhead =====

    /**
     * Benchmark: Subscribe and unsubscribe cycle.
     */
    @Benchmark
    public void subscribeUnsubscribeCycle() {
        YSubscription sub = doc.observeUpdateV1((update, origin) -> { });
        sub.close();
    }

    /**
     * Benchmark: Register multiple observers.
     */
    @Benchmark
    @OperationsPerInvocation(10)
    public void multipleObserverRegistration(Blackhole bh) {
        YSubscription[] subs = new YSubscription[10];
        for (int i = 0; i < 10; i++) {
            subs[i] = doc.observeUpdateV1((update, origin) -> { });
        }
        for (YSubscription sub : subs) {
            bh.consume(sub);
            sub.close();
        }
    }

    // ===== Notification Performance =====

    /**
     * State with active observer.
     */
    @State(Scope.Thread)
    public static class ObservedState {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc doc;
        YText text;
        YSubscription subscription;
        AtomicInteger callCount;

        @Setup(Level.Iteration)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            text = doc.getText("content");
            callCount = new AtomicInteger(0);
            subscription = doc.observeUpdateV1((update, origin) -> {
                callCount.incrementAndGet();
            });
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            subscription.close();
            text.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Observer callback overhead (single change).
     */
    @Benchmark
    public void observerCallbackOverhead(ObservedState state) {
        state.text.push("x");
    }

    /**
     * Benchmark: High frequency updates with observer.
     */
    @Benchmark
    @OperationsPerInvocation(100)
    public void highFrequencyUpdates(ObservedState state) {
        for (int i = 0; i < 100; i++) {
            state.text.push("x");
        }
    }

    // ===== Update Observer Performance =====

    /**
     * Benchmark: Update observer with payload capture.
     */
    @Benchmark
    public void updateObserverWithCapture(Blackhole bh) {
        final byte[][] captured = new byte[1][];
        YSubscription sub = doc.observeUpdateV1((update, origin) -> {
            captured[0] = update;
        });
        text.push("test content");
        bh.consume(captured[0]);
        sub.close();
    }

    /**
     * State with large document for observer testing.
     */
    @State(Scope.Thread)
    public static class LargeDocumentObservedState {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc doc;
        YText text;
        YSubscription subscription;
        byte[] lastUpdate;

        @Setup(Level.Iteration)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            text = doc.getText("content");
            // Pre-populate with 100KB of content
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100 * 1024; i++) {
                sb.append("x");
            }
            text.push(sb.toString());
            subscription = doc.observeUpdateV1((update, origin) -> {
                lastUpdate = update;
            });
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            subscription.close();
            text.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Update observer with large payload.
     */
    @Benchmark
    public void updateObserverWithLargePayload(LargeDocumentObservedState state, Blackhole bh) {
        state.text.push("additional content");
        bh.consume(state.lastUpdate);
    }

    // ===== Multiple Observers =====

    /**
     * State with multiple observers.
     */
    @State(Scope.Thread)
    public static class MultiObserverState {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc doc;
        YText text;
        YSubscription[] subscriptions;
        AtomicInteger[] counters;

        @Setup(Level.Iteration)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            text = doc.getText("content");
            subscriptions = new YSubscription[5];
            counters = new AtomicInteger[5];
            for (int i = 0; i < 5; i++) {
                counters[i] = new AtomicInteger(0);
                final int idx = i;
                subscriptions[i] = doc.observeUpdateV1((update, origin) -> {
                    counters[idx].incrementAndGet();
                });
            }
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            for (YSubscription sub : subscriptions) {
                sub.close();
            }
            text.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Change with multiple observers.
     */
    @Benchmark
    public void multipleObserversNotification(MultiObserverState state) {
        state.text.push("x");
    }

    /**
     * Benchmark: High frequency with multiple observers.
     */
    @Benchmark
    @OperationsPerInvocation(100)
    public void multipleObserversHighFrequency(MultiObserverState state) {
        for (int i = 0; i < 100; i++) {
            state.text.push("x");
        }
    }

    // ===== Comparison: With vs Without Observer =====

    /**
     * Benchmark: Text push without observer (baseline).
     */
    @Benchmark
    public void textPushWithoutObserver() {
        text.push("x");
    }

    /**
     * Benchmark: Text push with observer.
     */
    @Benchmark
    public void textPushWithObserver(ObservedState state) {
        state.text.push("x");
    }
}
