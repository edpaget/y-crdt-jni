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
 * YDoc operation benchmarks comparing JNI and Panama implementations.
 *
 * <p>Measures performance of document lifecycle operations including
 * creation, state encoding, and update application.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class YDocBenchmark {

    @Param({"jni", "panama"})
    private String implementation;

    private YBinding binding;

    @Setup(Level.Trial)
    public void setup() {
        binding = "jni".equals(implementation)
            ? YBindingFactory.jni()
            : YBindingFactory.panama();
    }

    // ===== Document Lifecycle =====

    /**
     * Benchmark: Create and close an empty document.
     */
    @Benchmark
    public void createAndCloseEmptyDoc() {
        try (YDoc doc = binding.createDoc()) {
            // Just create and close
        }
    }

    /**
     * Benchmark: Create document and get a text type.
     */
    @Benchmark
    public void createDocWithText() {
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("content")) {
            // Create doc with one shared type
        }
    }

    // ===== State Encoding Benchmarks =====

    /**
     * State for document with content.
     */
    @State(Scope.Thread)
    public static class DocumentWithContent {
        @Param({"jni", "panama"})
        private String implementation;

        YDoc doc;
        YText text;

        @Setup(Level.Trial)
        public void setup() {
            YBinding binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            text = doc.getText("content");
            // Add some content
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("Hello World! ");
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
     * Benchmark: Encode state as update.
     */
    @Benchmark
    public byte[] encodeStateAsUpdate(DocumentWithContent state) {
        return state.doc.encodeStateAsUpdate();
    }

    /**
     * Benchmark: Encode state vector.
     */
    @Benchmark
    public byte[] encodeStateVector(DocumentWithContent state) {
        return state.doc.encodeStateVector();
    }

    // ===== Update Application Benchmarks =====

    /**
     * State with pre-encoded update.
     */
    @State(Scope.Thread)
    public static class PreEncodedUpdate {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        byte[] update;

        @Setup(Level.Trial)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();

            // Create source document with content
            try (YDoc sourceDoc = binding.createDoc();
                 YText text = sourceDoc.getText("content")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 1000; i++) {
                    sb.append("Hello World! ");
                }
                text.push(sb.toString());
                update = sourceDoc.encodeStateAsUpdate();
            }
        }
    }

    /**
     * Benchmark: Apply update to empty document.
     */
    @Benchmark
    public void applyUpdateToEmptyDoc(PreEncodedUpdate state) {
        try (YDoc doc = state.binding.createDoc()) {
            doc.applyUpdate(state.update);
        }
    }

    // ===== Differential Sync Benchmarks =====

    /**
     * State for differential sync testing.
     */
    @State(Scope.Thread)
    public static class DiffSyncState {
        @Param({"jni", "panama"})
        private String implementation;

        YBinding binding;
        YDoc sourceDoc;
        YText sourceText;
        byte[] baseStateVector;

        @Setup(Level.Trial)
        public void setup() {
            binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();

            sourceDoc = binding.createDoc();
            sourceText = sourceDoc.getText("content");

            // Add initial content
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                sb.append("Initial content. ");
            }
            sourceText.push(sb.toString());

            // Capture base state
            baseStateVector = sourceDoc.encodeStateVector();

            // Add more content (the diff)
            sourceText.push(" Additional changes that need to be synced.");
        }

        @TearDown(Level.Trial)
        public void teardown() {
            sourceText.close();
            sourceDoc.close();
        }
    }

    /**
     * Benchmark: Encode differential update.
     */
    @Benchmark
    public byte[] encodeDiff(DiffSyncState state) {
        return state.sourceDoc.encodeDiff(state.baseStateVector);
    }
}
