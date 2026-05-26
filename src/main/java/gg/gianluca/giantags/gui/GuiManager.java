package gg.gianluca.giantags.gui;

import dev.triumphteam.gui.guis.PaginatedGui;
import gg.gianluca.giantags.config.CategoriesConfig;
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
 * Manages all open GUI instances and their auto-update tasks.
 *
 * <h3>GUI contexts</h3>
 * <ul>
 *   <li>{@link GuiContext#CATEGORIES} — the category selection screen</li>
 *   <li>{@link GuiContext#TAGS_ALL} — paginated list of all tags</li>
 *   <li>{@link GuiContext#TAGS_CATEGORY} — paginated list for one category</li>
 * </ul>
 *
 * <h3>Paper &amp; Folia compatibility</h3>
 * All GUI operations use {@link SchedulerAdapter#runForEntity} /
 * {@link SchedulerAdapter#runTimerForEntity} to stay on the correct region thread.
 */
public final class GuiManager {

    /** What type of GUI a player currently has open. */
    public enum GuiContext { CATEGORIES, TAGS_ALL, TAGS_CATEGORY }

    /** Immutable snapshot of an open GUI's state. */
    private record OpenGuiState(
            @NotNull PaginatedGui gui,
            @NotNull GuiContext context,
            @Nullable String categoryId  // non-null only for TAGS_CATEGORY
    ) {}

    private final JavaPlugin plugin;

    private final Map<UUID, OpenGuiState> openGuis = new ConcurrentHashMap<>();
    private final Map<UUID, CancellableTask> updateTasks = new ConcurrentHashMap<>();

    /**
     * Players currently mid-transition between GUIs.
     * Suppresses the old GUI's close-action so it cannot clobber the new state.
     */
    private final Set<UUID> refreshing = ConcurrentHashMap.newKeySet();

    public GuiManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Smart entry point: opens the categories GUI if categories are enabled,
     * otherwise opens the all-tags GUI.
     */
    public void openTagsGui(@NotNull Player player) {
        if (isCategoriesEnabled()) {
            openCategoriesGui(player);
        } else {
            openTagsGuiAll(player);
        }
    }

    /**
     * Always opens the full paginated tags GUI regardless of the categories setting.
     * Accessible via {@code /tags all}.
     */
    public void openTagsGuiAll(@NotNull Player player) {
        PaginatedGui gui = TagsGui.create(plugin, player, this, null);
        openGuiInternal(player, gui, GuiContext.TAGS_ALL, null);
    }

    /**
     * Opens the categories selection GUI.
     */
    public void openCategoriesGui(@NotNull Player player) {
        PaginatedGui gui = CategoriesGui.create(plugin, player, this);
        openGuiInternal(player, gui, GuiContext.CATEGORIES, null);
    }

    /**
     * Opens the tags GUI filtered to a specific category.
     */
    public void openTagsGuiForCategory(@NotNull Player player, @NotNull String categoryId) {
        PaginatedGui gui = TagsGui.create(plugin, player, this, categoryId);
        openGuiInternal(player, gui, GuiContext.TAGS_CATEGORY, categoryId);
    }

    /**
     * Closes and reopens the same GUI context the player currently has open.
     * Used after tag-state changes so the view reflects the new state.
     */
    public void refreshGui(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        OpenGuiState state = openGuis.get(uuid);
        if (state == null) return;

        refreshing.add(uuid);
        cancelUpdateTask(uuid);
        openGuis.remove(uuid);

        player.closeInventory();

        OpenGuiState capturedState = state;
        SchedulerAdapter.runForEntity(plugin, player, () -> {
            refreshing.remove(uuid);
            if (!player.isOnline()) return;
            switch (capturedState.context()) {
                case CATEGORIES -> openCategoriesGui(player);
                case TAGS_ALL -> openTagsGuiAll(player);
                case TAGS_CATEGORY -> openTagsGuiForCategory(player, capturedState.categoryId());
            }
        }, () -> refreshing.remove(uuid));
    }

    /**
     * Called when a GUI close event fires for a non-refresh close.
     */
    public void onGuiClose(@NotNull UUID uuid) {
        cancelUpdateTask(uuid);
        openGuis.remove(uuid);
    }

    /** Returns {@code true} if the player has any GUI open. */
    public boolean hasGuiOpen(@NotNull UUID uuid) {
        return openGuis.containsKey(uuid);
    }

    /** Returns the open {@link PaginatedGui} for the player, or {@code null}. */
    @Nullable
    public PaginatedGui getOpenGui(@NotNull UUID uuid) {
        OpenGuiState state = openGuis.get(uuid);
        return state != null ? state.gui() : null;
    }

    /**
     * Returns the category ID of the currently open per-category tags GUI,
     * or {@code null} if the player has a different GUI context open.
     */
    @Nullable
    public String getOpenGuiCategoryId(@NotNull UUID uuid) {
        OpenGuiState state = openGuis.get(uuid);
        if (state == null || state.context() != GuiContext.TAGS_CATEGORY) return null;
        return state.categoryId();
    }

    /**
     * Closes all open GUIs and cancels all update tasks.
     * Called on plugin disable and reload.
     */
    public void closeAll() {
        for (UUID uuid : Set.copyOf(openGuis.keySet())) {
            cancelUpdateTask(uuid);
            OpenGuiState state = openGuis.remove(uuid);
            if (state != null) {
                state.gui().getInventory().getViewers().forEach(v -> v.closeInventory());
            }
        }
        updateTasks.values().forEach(CancellableTask::cancel);
        updateTasks.clear();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Common path for all GUI open operations.
     *
     * <ol>
     *   <li>Adds the player to {@link #refreshing} so any close event fired by
     *       {@link PaginatedGui#open} (for the previous inventory) is suppressed.</li>
     *   <li>Registers the new GUI state.</li>
     *   <li>Opens the inventory (triggers the old close event, harmlessly suppressed).</li>
     *   <li>Removes from {@link #refreshing} and schedules the nav-row update task.</li>
     * </ol>
     */
    private void openGuiInternal(
            @NotNull Player player,
            @NotNull PaginatedGui gui,
            @NotNull GuiContext context,
            @Nullable String categoryId
    ) {
        UUID uuid = player.getUniqueId();

        refreshing.add(uuid);
        cancelUpdateTask(uuid);
        openGuis.remove(uuid);

        openGuis.put(uuid, new OpenGuiState(gui, context, categoryId));

        gui.setCloseGuiAction(event -> {
            if (!refreshing.contains(uuid)) {
                onGuiClose(uuid);
            }
        });

        gui.open(player);
        refreshing.remove(uuid);

        int interval = getUpdateInterval(context);
        CancellableTask task = SchedulerAdapter.runTimerForEntity(
                plugin,
                player,
                () -> {
                    OpenGuiState current = openGuis.get(uuid);
                    if (current == null || current.gui().getInventory().getViewers().isEmpty()) {
                        cancelUpdateTask(uuid);
                        return;
                    }
                    if (player.isOnline()) {
                        if (current.context() == GuiContext.CATEGORIES) {
                            CategoriesGui.updateNavItems(plugin, player, current.gui());
                        } else {
                            TagsGui.updateNavItems(plugin, player, current.gui());
                        }
                    }
                },
                () -> onGuiClose(uuid),
                interval,
                interval
        );
        updateTasks.put(uuid, task);
    }

    private void cancelUpdateTask(@NotNull UUID uuid) {
        CancellableTask task = updateTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private int getUpdateInterval(@NotNull GuiContext context) {
        if (context == GuiContext.CATEGORIES) {
            return ((gg.gianluca.giantags.GianTags) plugin)
                    .getConfigManager().getCategoriesConfig().getUpdateInterval();
        }
        return ((gg.gianluca.giantags.GianTags) plugin)
                .getConfigManager().getGuiConfig().getUpdateInterval();
    }

    private boolean isCategoriesEnabled() {
        return ((gg.gianluca.giantags.GianTags) plugin)
                .getConfigManager().getMainConfig().isCategoriesEnabled();
    }
}
