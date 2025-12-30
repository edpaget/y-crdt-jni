package net.carcdr.ycrdt.benchmarks;

import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YText;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.TimeUnit;

/**
 * Synchronization benchmarks comparing JNI and Panama implementations.
 *
 * <p>Measures end-to-end synchronization performance including
 * full sync and differential sync operations.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SyncBenchmark {

    @Param({"jni", "panama"})
    private String implementation;

    private YBinding binding;

    @Setup(Level.Trial)
    public void setup() {
        binding = "jni".equals(implementation)
            ? YBindingFactory.jni()
            : YBindingFactory.panama();
    }

    // ===== Full Sync Benchmarks =====

    /**
     * State with source document containing content.
     */
    @State(Scope.Thread)
    public static class SourceDocument {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc sourceDoc;
        YText sourceText;

        @Setup(Level.Iteration)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();

            sourceDoc = binding.createDoc();
            sourceText = sourceDoc.getText("content");
            sourceText.push("Hello, this is some initial content for sync testing.");
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            sourceText.close();
            sourceDoc.close();
        }
    }

    /**
     * Benchmark: Full sync - encode update and apply to new document.
     */
    @Benchmark
    public void fullSync(SourceDocument state) {
        byte[] update = state.sourceDoc.encodeStateAsUpdate();
        try (YDoc targetDoc = state.binding.createDoc()) {
            targetDoc.applyUpdate(update);
        }
    }

    // ===== Differential Sync Benchmarks =====

    /**
     * State for differential sync with base and incremental changes.
     */
    @State(Scope.Thread)
    public static class DiffSyncScenario {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc sourceDoc;
        YText sourceText;
        YDoc targetDoc;
        byte[] targetStateVector;

        @Setup(Level.Iteration)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();

            // Create source with initial content
            sourceDoc = binding.createDoc();
            sourceText = sourceDoc.getText("content");
            sourceText.push("Initial shared content that both documents have.");

            // Sync to target
            targetDoc = binding.createDoc();
            byte[] initialUpdate = sourceDoc.encodeStateAsUpdate();
            targetDoc.applyUpdate(initialUpdate);

            // Capture target's state vector
            targetStateVector = targetDoc.encodeStateVector();

            // Add more content to source (this is what we'll diff)
            sourceText.push(" Additional content that needs to be synced.");
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            sourceText.close();
            sourceDoc.close();
            targetDoc.close();
        }
    }

    /**
     * Benchmark: Differential sync - encode diff and apply.
     */
    @Benchmark
    public void differentialSync(DiffSyncScenario state) {
        byte[] diff = state.sourceDoc.encodeDiff(state.targetStateVector);
        state.targetDoc.applyUpdate(diff);
    }

    // ===== Large Document Sync Benchmarks =====

    /**
     * State with large source document.
     */
    @State(Scope.Thread)
    public static class LargeSourceDocument {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc sourceDoc;
        byte[] fullUpdate;

        @Setup(Level.Trial)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();

            sourceDoc = binding.createDoc();
            try (YText text = sourceDoc.getText("content")) {
                // Create 100KB of content
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 100 * 1024; i++) {
                    sb.append("x");
                }
                text.push(sb.toString());
            }
            fullUpdate = sourceDoc.encodeStateAsUpdate();
        }

        @TearDown(Level.Trial)
        public void teardown() {
            sourceDoc.close();
        }
    }

    /**
     * Benchmark: Full sync of large document.
     */
    @Benchmark
    public void fullSyncLargeDocument(LargeSourceDocument state) {
        try (YDoc targetDoc = state.binding.createDoc()) {
            targetDoc.applyUpdate(state.fullUpdate);
        }
    }

    // ===== Bidirectional Sync Benchmarks =====

    /**
     * State for bidirectional sync testing.
     */
    @State(Scope.Thread)
    public static class BidirectionalSyncScenario {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc doc1;
        YText text1;
        YDoc doc2;
        YText text2;

        @Setup(Level.Iteration)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();

            doc1 = binding.createDoc();
            text1 = doc1.getText("content");

            doc2 = binding.createDoc();
            text2 = doc2.getText("content");
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            text1.close();
            doc1.close();
            text2.close();
            doc2.close();
        }
    }

    /**
     * Benchmark: Bidirectional sync with concurrent edits.
     */
    @Benchmark
    public void bidirectionalSync(BidirectionalSyncScenario state) {
        // Both make changes
        state.text1.push("Changes from doc1. ");
        state.text2.push("Changes from doc2. ");

        // Sync doc1 -> doc2
        byte[] update1 = state.doc1.encodeStateAsUpdate();
        state.doc2.applyUpdate(update1);

        // Sync doc2 -> doc1
        byte[] update2 = state.doc2.encodeStateAsUpdate();
        state.doc1.applyUpdate(update2);
    }
}
