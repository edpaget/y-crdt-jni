package net.carcdr.ycrdt.benchmarks;

import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
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
 * XML type operation benchmarks comparing JNI and Panama implementations.
 *
 * <p>Measures performance of YXmlFragment, YXmlElement, and YXmlText operations
 * including tree manipulation, attribute handling, and sync operations.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class YXmlBenchmark {

    @Param({"jni", "panama"})
    private String implementation;

    private YBinding binding;
    private YDoc doc;
    private YXmlFragment fragment;

    @Setup(Level.Iteration)
    public void setup() {
        binding = "jni".equals(implementation)
            ? YBindingFactory.jni()
            : YBindingFactory.panama();
        doc = binding.createDoc();
        fragment = doc.getXmlFragment("content");
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        fragment.close();
        doc.close();
    }

    // ===== YXmlFragment Operations =====

    /**
     * Benchmark: Insert element into fragment.
     */
    @Benchmark
    public void insertElement() {
        int len = fragment.length();
        fragment.insertElement(len, "div");
    }

    /**
     * Benchmark: Insert text node into fragment.
     */
    @Benchmark
    public void insertText() {
        int len = fragment.length();
        fragment.insertText(len, "content");
    }

    /**
     * Benchmark: Get child count.
     */
    @Benchmark
    public int getLength() {
        return fragment.length();
    }

    /**
     * Benchmark: Remove child from fragment.
     */
    @Benchmark
    public void removeChild() {
        if (fragment.length() > 0) {
            fragment.remove(0, 1);
        }
    }

    // ===== YXmlElement Operations =====

    /**
     * State with a pre-existing element for attribute operations.
     */
    @State(Scope.Thread)
    public static class ElementState {
        @Param({"jni", "panama"})
        private String implementation;

        YDoc doc;
        YXmlFragment fragment;
        YXmlElement element;

        @Setup(Level.Iteration)
        public void setup() {
            YBinding binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            fragment = doc.getXmlFragment("content");
            fragment.insertElement(0, "div");
            element = fragment.getElement(0);
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            element.close();
            fragment.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Set attribute on element.
     */
    @Benchmark
    public void setAttribute(ElementState state) {
        state.element.setAttribute("class", "container");
    }

    /**
     * Benchmark: Get attribute from element.
     */
    @Benchmark
    public String getAttribute(ElementState state) {
        return state.element.getAttribute("class");
    }

    /**
     * Benchmark: Remove attribute from element.
     */
    @Benchmark
    public void removeAttribute(ElementState state) {
        state.element.removeAttribute("class");
    }

    /**
     * Benchmark: Get all attribute names.
     */
    @Benchmark
    public String[] getAttributeNames(ElementState state) {
        return state.element.getAttributeNames();
    }

    /**
     * Benchmark: Get element tag name.
     */
    @Benchmark
    public String getTag(ElementState state) {
        return state.element.getTag();
    }

    // ===== Tree Navigation =====

    /**
     * State with nested element structure for navigation.
     */
    @State(Scope.Thread)
    public static class NestedState {
        @Param({"jni", "panama"})
        private String implementation;

        YDoc doc;
        YXmlFragment fragment;
        YXmlElement parent;
        YXmlElement child;

        @Setup(Level.Iteration)
        public void setup() {
            YBinding binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            fragment = doc.getXmlFragment("content");
            fragment.insertElement(0, "parent");
            parent = fragment.getElement(0);
            parent.insertElement(0, "child");
            child = (YXmlElement) parent.getChild(0);
        }

        @TearDown(Level.Iteration)
        public void teardown() {
            child.close();
            parent.close();
            fragment.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Get child element.
     */
    @Benchmark
    public Object getChild(NestedState state) {
        Object child = state.parent.getChild(0);
        if (child instanceof AutoCloseable) {
            try {
                ((AutoCloseable) child).close();
            } catch (Exception e) {
                // ignore
            }
        }
        return child;
    }

    /**
     * Benchmark: Get parent element.
     */
    @Benchmark
    public Object getParent(NestedState state) {
        return state.child.getParent();
    }

    /**
     * Benchmark: Get index in parent.
     */
    @Benchmark
    public int getIndexInParent(NestedState state) {
        return state.child.getIndexInParent();
    }

    // ===== Scale Tests =====

    /**
     * State with deeply nested structure.
     */
    @State(Scope.Thread)
    public static class DeepNestedState {
        @Param({"jni", "panama"})
        private String implementation;

        YDoc doc;
        YXmlFragment fragment;

        @Setup(Level.Trial)
        public void setup() {
            YBinding binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            fragment = doc.getXmlFragment("content");
            // Create 50 levels of nesting
            fragment.insertElement(0, "level0");
            YXmlElement current = fragment.getElement(0);
            for (int i = 1; i < 50; i++) {
                current.insertElement(0, "level" + i);
                YXmlElement next = (YXmlElement) current.getChild(0);
                current.close();
                current = next;
            }
            current.close();
        }

        @TearDown(Level.Trial)
        public void teardown() {
            fragment.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Sync deeply nested structure.
     */
    @Benchmark
    public void deepNestedSync(DeepNestedState state) {
        byte[] update = state.doc.encodeStateAsUpdate();
        try (YDoc doc2 = binding.createDoc()) {
            doc2.applyUpdate(update);
        }
    }

    /**
     * State with wide tree structure (many siblings).
     */
    @State(Scope.Thread)
    public static class WideTreeState {
        @Param({"jni", "panama"})
        private String implementation;

        YDoc doc;
        YXmlFragment fragment;

        @Setup(Level.Trial)
        public void setup() {
            YBinding binding = "jni".equals(implementation)
                ? YBindingFactory.jni()
                : YBindingFactory.panama();
            doc = binding.createDoc();
            fragment = doc.getXmlFragment("content");
            fragment.insertElement(0, "root");
            YXmlElement root = fragment.getElement(0);
            // Create 100 child elements
            for (int i = 0; i < 100; i++) {
                root.insertElement(i, "child" + i);
            }
            root.close();
        }

        @TearDown(Level.Trial)
        public void teardown() {
            fragment.close();
            doc.close();
        }
    }

    /**
     * Benchmark: Sync wide tree structure.
     */
    @Benchmark
    public void wideTreeSync(WideTreeState state) {
        byte[] update = state.doc.encodeStateAsUpdate();
        try (YDoc doc2 = binding.createDoc()) {
            doc2.applyUpdate(update);
        }
    }

    /**
     * Benchmark: Insert element into wide tree.
     */
    @Benchmark
    public void insertIntoWideTree(WideTreeState state) {
        try (YXmlElement root = state.fragment.getElement(0)) {
            int count = root.childCount();
            YXmlElement child = root.insertElement(count, "newchild");
            child.close();
        }
    }

    // ===== Bulk Operations =====

    /**
     * Benchmark: Bulk insert elements.
     */
    @Benchmark
    @OperationsPerInvocation(100)
    public void bulkInsertElements() {
        for (int i = 0; i < 100; i++) {
            fragment.insertElement(i, "item" + i);
        }
    }

    /**
     * Benchmark: Bulk set attributes.
     */
    @Benchmark
    @OperationsPerInvocation(100)
    public void bulkSetAttributes(ElementState state) {
        for (int i = 0; i < 100; i++) {
            state.element.setAttribute("attr" + i, "value" + i);
        }
    }
}
