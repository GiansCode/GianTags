package gg.gianluca.giantags.storage;

import gg.gianluca.giantags.api.model.PlayerData;
import gg.gianluca.giantags.config.StorageConfig;
import gg.gianluca.giantags.storage.impl.FlatfileStorage;
import gg.gianluca.giantags.storage.impl.SqlStorage;
import gg.gianluca.giantags.util.SchedulerAdapter;
import gg.gianluca.giantags.util.SchedulerAdapter.CancellableTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages the in-memory player data cache, persistence backend, and auto-save.
 *
 * <h3>Threading model (Paper &amp; Folia)</h3>
 * <ul>
 *   <li>Cache reads/writes: main / global-region thread only (fast ConcurrentHashMap ops).</li>
 *   <li>Disk / DB I/O: always on the async scheduler via {@link SchedulerAdapter#runAsync}.</li>
 *   <li>Post-load callback: dispatched back to the global region via
 *       {@link SchedulerAdapter#runGlobal} so cache mutations stay on one thread.</li>
 *   <li>Auto-save timer: global region timer — safe on both Paper and Folia.</li>
 * </ul>
 */
public final class StorageManager {

    private final JavaPlugin plugin;
    private Storage backend;

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirtySet = ConcurrentHashMap.newKeySet();

    private @Nullable CancellableTask autoSaveTask;

    public StorageManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialises the storage backend. Called from {@code onEnable} on the main / global thread.
     */
    public void initialize(@NotNull StorageConfig config) {
        if (config.getType() == StorageConfig.StorageType.MYSQL) {
            backend = new SqlStorage(config, plugin.getLogger());
        } else {
            File dataDir = new File(plugin.getDataFolder(), "data");
            backend = new FlatfileStorage(dataDir, plugin.getLogger());
        }

        // Backend init is potentially blocking — do it off-thread
        SchedulerAdapter.runAsync(plugin, () -> {
            try {
                backend.init();
                plugin.getLogger().info("Storage backend initialised (" + config.getType() + ").");
            } catch (Storage.StorageException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialise storage backend!", e);
            }
        });

        if (config.isAutoSaveEnabled()) {
            long interval = config.getAutoSaveInterval() * 20L; // seconds → ticks
            autoSaveTask = SchedulerAdapter.runTimerGlobal(plugin, this::saveDirty, interval, interval);
        }
    }

    /**
     * Loads a player's data asynchronously and inserts it into the cache on the
     * global region thread when done.
     */
    public void loadPlayerAsync(@NotNull UUID uuid, @Nullable Runnable onComplete) {
        SchedulerAdapter.runAsync(plugin, () -> {
            PlayerData data;
            try {
                data = backend.loadPlayer(uuid);
            } catch (Storage.StorageException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load data for " + uuid, e);
                data = null;
            }
            final PlayerData finalData = data != null ? data : PlayerData.empty(uuid);
            SchedulerAdapter.runGlobal(plugin, () -> {
                cache.put(uuid, finalData);
                if (onComplete != null) onComplete.run();
            });
        });
    }

    /**
     * Returns the in-memory {@link PlayerData} for a player, or empty if not loaded.
     * Always fast — no I/O.
     */
    @NotNull
    public Optional<PlayerData> getPlayerData(@NotNull UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    /**
     * Returns or creates a default {@link PlayerData} entry.
     */
    @NotNull
    public PlayerData getOrCreate(@NotNull UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::empty);
    }

    /**
     * Sets a player's active tag ID and marks them dirty.
     * Pass {@code null} to clear the tag.
     */
    public void setTagId(@NotNull UUID uuid, @Nullable String tagId) {
        getOrCreate(uuid).setTagId(tagId);
        dirtySet.add(uuid);
    }

    /**
     * Clears a player's active tag and marks them dirty.
     */
    public void clearTag(@NotNull UUID uuid) {
        setTagId(uuid, null);
    }

    /**
     * Saves a single player asynchronously if their data is dirty.
     */
    public void savePlayerAsync(@NotNull UUID uuid) {
        if (!dirtySet.remove(uuid)) return;
        PlayerData snapshot = cache.get(uuid);
        if (snapshot == null) return;

        SchedulerAdapter.runAsync(plugin, () -> {
            try {
                backend.savePlayer(uuid, snapshot);
            } catch (Storage.StorageException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save data for " + uuid, e);
                dirtySet.add(uuid); // re-mark dirty for next cycle
            }
        });
    }

    /**
     * Saves all dirty players asynchronously. Called by the auto-save timer.
     */
    public void saveDirty() {
        if (dirtySet.isEmpty()) return;
        Set<UUID> toSave = Set.copyOf(dirtySet);
        dirtySet.removeAll(toSave);

        SchedulerAdapter.runAsync(plugin, () -> {
            for (UUID uuid : toSave) {
                PlayerData data = cache.get(uuid);
                if (data == null) continue;
                try {
                    backend.savePlayer(uuid, data);
                } catch (Storage.StorageException e) {
                    plugin.getLogger().log(Level.WARNING, "Auto-save failed for " + uuid, e);
                    dirtySet.add(uuid);
                }
            }
        });
    }

    /**
     * Saves all dirty players <em>synchronously</em> (blocking) and clears the cache.
     * Called from {@code onDisable} — no scheduler available at that point.
     */
    public void saveAllSync() {
        for (UUID uuid : dirtySet) {
            PlayerData data = cache.get(uuid);
            if (data == null) continue;
            try {
                backend.savePlayer(uuid, data);
            } catch (Storage.StorageException e) {
                plugin.getLogger().log(Level.WARNING, "Shutdown save failed for " + uuid, e);
            }
        }
        dirtySet.clear();
        cache.clear();
    }

    /**
     * Saves the player's data asynchronously then removes them from the cache.
     */
    public void unloadPlayer(@NotNull UUID uuid) {
        if (!dirtySet.remove(uuid)) {
            cache.remove(uuid);
            return;
        }
        PlayerData snapshot = cache.remove(uuid);
        if (snapshot == null) return;

        SchedulerAdapter.runAsync(plugin, () -> {
            try {
                backend.savePlayer(uuid, snapshot);
            } catch (Storage.StorageException e) {
                plugin.getLogger().log(Level.WARNING, "Unload save failed for " + uuid, e);
            }
        });
    }

    /**
     * Cancels the auto-save task and closes the backend.
     */
    public void close() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (backend != null) {
            backend.close();
        }
    }
}
