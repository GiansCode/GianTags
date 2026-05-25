package gg.gianluca.giantags.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unified scheduler facade that works on both <b>Paper</b> and <b>Folia</b>.
 *
 * <p>Paper 1.20.4+ ships the Folia scheduler API (
 * {@code GlobalRegionScheduler}, {@code AsyncScheduler}, {@code EntityScheduler})
 * as part of its own API surface. On Paper these schedulers simply delegate to the
 * Bukkit main-thread or async scheduler. On Folia they schedule work in the
 * correct region. Because the API is identical on both forks, <em>no runtime
 * branching is required</em> for task submission; we simply avoid the legacy
 * {@code BukkitScheduler} altogether.
 *
 * <h3>Threading contract</h3>
 * <ul>
 *   <li>{@link #runAsync} — off main thread, never in a region.</li>
 *   <li>{@link #runGlobal} — global region thread (main thread on Paper).</li>
 *   <li>{@link #runForEntity} / {@link #runTimerForEntity} — entity's owning
 *       region (main thread on Paper); the {@code retired} runnable fires on
 *       Folia when the entity becomes invalid.</li>
 * </ul>
 *
 * @see #IS_FOLIA
 */
@SuppressWarnings("UnstableApiUsage")
public final class SchedulerAdapter {

    /**
     * {@code true} when the runtime is Folia (regionised multithreading).
     * Useful for logging and for any behaviour that must genuinely differ
     * between the two platforms (e.g., warning about non-region-safe calls).
     */
    public static final boolean IS_FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    private SchedulerAdapter() {}

    // ── Functional task handle ────────────────────────────────────────────────

    /**
     * A minimal cancellable task handle, compatible with both
     * {@link ScheduledTask} (Folia / new Paper API) and legacy {@code BukkitTask}.
     */
    @FunctionalInterface
    public interface CancellableTask {
        void cancel();

        /** Returns a no-op handle (useful when a task could not be scheduled). */
        @NotNull
        static CancellableTask noop() {
            return () -> {};
        }
    }

    // ── One-shot async ────────────────────────────────────────────────────────

    /**
     * Runs {@code runnable} asynchronously (never on any region thread).
     */
    public static void runAsync(@NotNull JavaPlugin plugin, @NotNull Runnable runnable) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, $ -> runnable.run());
    }

    // ── One-shot global region ────────────────────────────────────────────────

    /**
     * Runs {@code runnable} on the global region (main thread on Paper).
     * Use for non-entity, non-world-location tasks.
     */
    public static void runGlobal(@NotNull JavaPlugin plugin, @NotNull Runnable runnable) {
        plugin.getServer().getGlobalRegionScheduler().run(plugin, $ -> runnable.run());
    }

    // ── One-shot entity-bound ─────────────────────────────────────────────────

    /**
     * Runs {@code runnable} in the owning region of {@code entity}.
     * On Paper this is the main thread. On Folia it is the entity's region thread.
     *
     * @param retired called on Folia if the entity is invalid when the task fires;
     *                ignored on Paper. May be {@code null}.
     */
    public static void runForEntity(
            @NotNull JavaPlugin plugin,
            @NotNull Entity entity,
            @NotNull Runnable runnable,
            @Nullable Runnable retired
    ) {
        entity.getScheduler().run(plugin, $ -> runnable.run(), retired);
    }

    // ── Repeating global region ───────────────────────────────────────────────

    /**
     * Schedules a repeating task on the global region.
     *
     * @param delay  initial delay in ticks
     * @param period period in ticks
     */
    @NotNull
    public static CancellableTask runTimerGlobal(
            @NotNull JavaPlugin plugin,
            @NotNull Runnable runnable,
            long delay,
            long period
    ) {
        ScheduledTask task = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, $ -> runnable.run(), delay, period);
        return task::cancel;
    }

    // ── Repeating entity-bound ────────────────────────────────────────────────

    /**
     * Schedules a repeating task in the entity's owning region.
     *
     * @param retired called on Folia when the entity is invalid; ignored on Paper.
     * @param delay   initial delay in ticks
     * @param period  period in ticks
     * @return a handle to cancel the task, or a no-op if scheduling failed
     */
    @NotNull
    public static CancellableTask runTimerForEntity(
            @NotNull JavaPlugin plugin,
            @NotNull Entity entity,
            @NotNull Runnable runnable,
            @Nullable Runnable retired,
            long delay,
            long period
    ) {
        ScheduledTask task = entity.getScheduler()
                .runAtFixedRate(plugin, $ -> runnable.run(), retired, delay, period);
        return task != null ? task::cancel : CancellableTask.noop();
    }
}
