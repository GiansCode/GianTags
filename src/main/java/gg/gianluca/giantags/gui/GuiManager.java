package gg.gianluca.giantags.gui;

import dev.triumphteam.gui.guis.PaginatedGui;
import gg.gianluca.giantags.config.GuiConfig;
import gg.gianluca.giantags.util.SchedulerAdapter;
import gg.gianluca.giantags.util.SchedulerAdapter.CancellableTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages open {@link PaginatedGui} instances and their auto-update tasks.
 *
 * <h3>Paper &amp; Folia compatibility</h3>
 * GUI operations (opening, updating inventories) must run in the player's
 * owning region on Folia. All scheduled work therefore uses
 * {@link SchedulerAdapter#runForEntity} and
 * {@link SchedulerAdapter#runTimerForEntity} so the correct region thread is
 * used on Folia, while still executing on the main thread on Paper.
 *
 * <h3>Lifecycle per player</h3>
 * <ol>
 *   <li>{@link #openTagsGui(Player)} — builds GUI, schedules nav-row updater.</li>
 *   <li>Periodic entity-bound task refreshes the nav row every N ticks.</li>
 *   <li>On close (via {@code setCloseGuiAction}) → {@link #onGuiClose(UUID)} cleans up.</li>
 *   <li>{@link #refreshGui(Player)} — closes then reopens on the next entity tick.</li>
 * </ol>
 */
public final class GuiManager {

    private final JavaPlugin plugin;

    /** UUID → open PaginatedGui */
    private final Map<UUID, PaginatedGui> openGuis = new ConcurrentHashMap<>();

    /** UUID → periodic update task handle */
    private final Map<UUID, CancellableTask> updateTasks = new ConcurrentHashMap<>();

    /**
     * Players currently undergoing a refresh (close + reopen).
     * Suppresses the close-action cleanup so the newly opened GUI is not immediately torn down.
     */
    private final Set<UUID> refreshing = ConcurrentHashMap.newKeySet();

    public GuiManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Opens the tags GUI for the given player.
     * Must be called from the player's region thread (or main thread on Paper).
     */
    public void openTagsGui(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        cancelUpdateTask(uuid);

        PaginatedGui gui = TagsGui.create(plugin, player, this);
        openGuis.put(uuid, gui);

        gui.setCloseGuiAction(event -> {
            if (!refreshing.contains(uuid)) {
                onGuiClose(uuid);
            }
        });

        gui.open(player);

        // Schedule the nav-row update in the player's region (entity scheduler).
        // The retired runnable fires on Folia if the player logs out mid-task.
        int interval = getGuiConfig().getUpdateInterval();
        CancellableTask task = SchedulerAdapter.runTimerForEntity(
                plugin,
                player,
                () -> {
                    PaginatedGui current = openGuis.get(uuid);
                    if (current == null || current.getInventory().getViewers().isEmpty()) {
                        cancelUpdateTask(uuid);
                        return;
                    }
                    if (player.isOnline()) {
                        TagsGui.updateNavItems(plugin, player, current);
                    }
                },
                // Retired: player went offline — clean up silently
                () -> onGuiClose(uuid),
                interval,
                interval
        );
        updateTasks.put(uuid, task);
    }

    /**
     * Closes the current GUI and reopens a fresh one on the next entity tick.
     * Called after tag-state changes to reflect the new state immediately.
     */
    public void refreshGui(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (!openGuis.containsKey(uuid)) return;

        refreshing.add(uuid);
        cancelUpdateTask(uuid);
        openGuis.remove(uuid);

        // Close now (suppress the close handler), then reopen on the next tick
        // via the entity scheduler so we land on the correct region thread.
        player.closeInventory();
        SchedulerAdapter.runForEntity(
                plugin,
                player,
                () -> {
                    refreshing.remove(uuid);
                    if (player.isOnline()) {
                        openTagsGui(player);
                    }
                },
                // Retired (Folia): player is gone — just clean up the flag
                () -> refreshing.remove(uuid)
        );
    }

    /**
     * Called when the GUI close event fires for a non-refresh close.
     */
    public void onGuiClose(@NotNull UUID uuid) {
        cancelUpdateTask(uuid);
        openGuis.remove(uuid);
    }

    /** Returns {@code true} if the player currently has the tags GUI open. */
    public boolean hasGuiOpen(@NotNull UUID uuid) {
        return openGuis.containsKey(uuid);
    }

    /** Returns the open {@link PaginatedGui} for the player, or {@code null}. */
    @Nullable
    public PaginatedGui getOpenGui(@NotNull UUID uuid) {
        return openGuis.get(uuid);
    }

    /**
     * Closes all open GUIs and cancels all update tasks.
     * Called on plugin disable and reload.
     */
    public void closeAll() {
        for (UUID uuid : Set.copyOf(openGuis.keySet())) {
            cancelUpdateTask(uuid);
            PaginatedGui gui = openGuis.remove(uuid);
            if (gui != null) {
                gui.getInventory().getViewers().forEach(v -> v.closeInventory());
            }
        }
        updateTasks.values().forEach(CancellableTask::cancel);
        updateTasks.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cancelUpdateTask(@NotNull UUID uuid) {
        CancellableTask task = updateTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private GuiConfig getGuiConfig() {
        return ((gg.gianluca.giantags.GianTags) plugin).getConfigManager().getGuiConfig();
    }
}
