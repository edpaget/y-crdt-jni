package net.carcdr.yhocuspocus.util;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Debounced document persistence scheduler.
 *
 * <p>Implements the debouncing logic from Hocuspocus:
 * <ul>
 *   <li>Wait for quiet period (debounce) before saving</li>
 *   <li>Force save after max debounce time</li>
 *   <li>Cancel pending save if new changes arrive</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
 * DebouncedDocumentSaver saver = new DebouncedDocumentSaver(
 *     scheduler,
 *     Duration.ofSeconds(2),  // Wait 2 seconds of quiet
 *     Duration.ofSeconds(10)  // Force save after 10 seconds max
 * );
 *
 * // Schedule saves (will be debounced)
 * saver.scheduleSave("doc1", () -> saveToDatabase("doc1"));
 * }</pre>
 *
 * @since 1.0.0
 */
public class DebouncedDocumentSaver {

    private final ScheduledExecutorService scheduler;
    private final Duration debounce;
    private final Duration maxDebounce;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledSaves;
    private final ConcurrentHashMap<String, Long> saveStartTimes;

    /**
     * Creates a new debounced document saver.
     *
     * @param scheduler executor service for scheduling saves
     * @param debounce minimum quiet period before saving
     * @param maxDebounce maximum time to wait before forcing a save
     */
    public DebouncedDocumentSaver(
        ScheduledExecutorService scheduler,
        Duration debounce,
        Duration maxDebounce
    ) {
        this.scheduler = scheduler;
        this.debounce = debounce;
        this.maxDebounce = maxDebounce;
        this.scheduledSaves = new ConcurrentHashMap<>();
        this.saveStartTimes = new ConcurrentHashMap<>();
    }

    /**
     * Schedules a save with debouncing.
     *
     * <p>If this is the first change, waits for {@code debounce} duration.
     * If changes continue, cancels previous save and reschedules.
     * If {@code maxDebounce} time has elapsed since first change, saves immediately.</p>
     *
     * @param documentName document name
     * @param saveTask task to execute when saving
     */
    public void scheduleSave(String documentName, Runnable saveTask) {
        long now = System.currentTimeMillis();
        Long startTime = saveStartTimes.computeIfAbsent(documentName, k -> now);

        // Cancel existing save if any
        ScheduledFuture<?> existing = scheduledSaves.get(documentName);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }

        // Check if max debounce exceeded
        long elapsed = now - startTime;
        long delay = (elapsed >= maxDebounce.toMillis())
            ? 0  // Save immediately
            : debounce.toMillis();

        // Schedule new save
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                saveTask.run();
            } finally {
                scheduledSaves.remove(documentName);
                saveStartTimes.remove(documentName);
            }
        }, delay, TimeUnit.MILLISECONDS);

        scheduledSaves.put(documentName, future);
    }

    /**
     * Forces immediate save, bypassing debounce logic.
     *
     * <p>Cancels any pending save and executes immediately.</p>
     *
     * @param documentName document name
     * @param saveTask task to execute
     */
    public void saveImmediately(String documentName, Runnable saveTask) {
        // Cancel pending save
        ScheduledFuture<?> existing = scheduledSaves.remove(documentName);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }
        saveStartTimes.remove(documentName);

        // Execute immediately
        saveTask.run();
    }

    /**
     * Cancels any pending save for a document.
     *
     * @param documentName document name
     * @return true if a save was cancelled
     */
    public boolean cancelSave(String documentName) {
        ScheduledFuture<?> future = scheduledSaves.remove(documentName);
        saveStartTimes.remove(documentName);

        if (future != null && !future.isDone()) {
            return future.cancel(false);
        }
        return false;
    }

    /**
     * Checks if a save is pending for a document.
     *
     * @param documentName document name
     * @return true if a save is scheduled
     */
    public boolean hasPendingSave(String documentName) {
        ScheduledFuture<?> future = scheduledSaves.get(documentName);
        return future != null && !future.isDone();
    }

    /**
     * Gets the number of pending saves.
     *
     * @return count of documents with pending saves
     */
    public int getPendingSaveCount() {
        // Clean up completed futures
        scheduledSaves.entrySet().removeIf(entry -> entry.getValue().isDone());
        return scheduledSaves.size();
    }
}
