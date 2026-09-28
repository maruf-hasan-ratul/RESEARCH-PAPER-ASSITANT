package com.example.research_project.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * TaskUtil.java - Utility for running background tasks off the JavaFX thread.
 *
 * WHY THIS EXISTS:
 *   JavaFX has a single "UI thread" (the Application Thread).
 *   Any blocking work done on this thread (DB queries, file I/O, report generation, etc.)
 *   will freeze the entire interface until it completes.
 *
 *   This utility makes it trivially easy to:
 *     1. Run a Callable<T> on a background daemon thread.
 *     2. When done, deliver the result back on the JavaFX thread via onSuccess.
 *     3. On failure, call onError (also on the JavaFX thread).
 *     4. Optionally show a busy cursor on any Node while the task runs.
 *
 * USAGE:
 *   TaskUtil.run(
 *       () -> paperService.getAllPapers(),   // background work
 *       papers -> papersTable.setItems(…),  // on success (FX thread)
 *       err -> showAlert(err.getMessage())  // on error   (FX thread)
 *   );
 */
public final class TaskUtil {

    private TaskUtil() {}

    /**
     * Run {@code work} on a background thread.
     * {@code onSuccess} and {@code onError} are called on the JavaFX Application Thread.
     *
     * @param work      Callable that performs blocking work and returns a result.
     * @param onSuccess Consumer called with the result on the FX thread.
     * @param onError   Consumer called with the exception on the FX thread.
     * @param <T>       Return type of the work.
     * @return The Task (already started); useful for binding progress/message if needed.
     */
    public static <T> Task<T> run(Callable<T> work,
                                  Consumer<T> onSuccess,
                                  Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("[TaskUtil] Background task failed: " +
                    (ex != null ? ex.getMessage() : "unknown error"));
            if (ex != null) ex.printStackTrace();
            if (onError != null) onError.accept(ex);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);        // Won't prevent JVM shutdown
        t.setName("bg-task-" + System.nanoTime());
        t.start();
        return task;
    }

    /**
     * Convenience overload without an error handler (errors are logged only).
     */
    public static <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess) {
        return run(work, onSuccess, null);
    }

    /**
     * Run fire-and-forget background work (no result needed).
     */
    public static Task<Void> run(Runnable work,
                                 Runnable onSuccess,
                                 Consumer<Throwable> onError) {
        return run(() -> { work.run(); return null; },
                   ignored -> { if (onSuccess != null) onSuccess.run(); },
                   onError);
    }

    /**
     * Show a WAIT cursor on {@code node} while the task is running,
     * then restore the default cursor when it finishes.
     *
     * @param node Any JavaFX node (typically the root pane of the view).
     * @param task The task to attach cursor management to.
     */
    public static void withBusyCursor(Node node, Task<?> task) {
        if (node == null) return;
        Platform.runLater(() -> node.setCursor(Cursor.WAIT));
        task.setOnSucceeded(e -> Platform.runLater(() -> node.setCursor(Cursor.DEFAULT)));
        task.setOnFailed(e    -> Platform.runLater(() -> node.setCursor(Cursor.DEFAULT)));
        task.setOnCancelled(e -> Platform.runLater(() -> node.setCursor(Cursor.DEFAULT)));
    }
}
