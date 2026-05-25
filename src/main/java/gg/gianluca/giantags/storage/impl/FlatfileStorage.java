package gg.gianluca.giantags.storage.impl;

import gg.gianluca.giantags.api.model.PlayerData;
import gg.gianluca.giantags.storage.Storage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * YAML flatfile storage backend.
 * Each player gets their own {@code /plugins/GianTags/data/<uuid>.yml} file.
 *
 * <p>Files are minimal — only the tag field is stored so reads/writes are
 * extremely fast even on spinning disks.
 */
public final class FlatfileStorage implements Storage {

    private final File dataFolder;
    private final Logger logger;

    public FlatfileStorage(@NotNull File dataFolder, @NotNull Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public void init() throws StorageException {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new StorageException("Could not create data directory: " + dataFolder.getAbsolutePath());
        }
    }

    @Override
    @Nullable
    public PlayerData loadPlayer(@NotNull UUID uuid) throws StorageException {
        File file = playerFile(uuid);
        if (!file.exists()) return null;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String tagId = config.getString("tag");
            return new PlayerData(uuid, tagId == null || tagId.isBlank() ? null : tagId);
        } catch (Exception e) {
            throw new StorageException("Failed to load data for " + uuid, e);
        }
    }

    @Override
    public void savePlayer(@NotNull UUID uuid, @NotNull PlayerData data) throws StorageException {
        File file = playerFile(uuid);
        YamlConfiguration config = new YamlConfiguration();

        if (data.hasTag()) {
            config.set("tag", data.getTagId());
        }
        // No tag? We write an empty file (or delete it to keep things clean)

        if (!data.hasTag()) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            throw new StorageException("Failed to save data for " + uuid, e);
        }
    }

    @Override
    public void deletePlayer(@NotNull UUID uuid) throws StorageException {
        File file = playerFile(uuid);
        if (file.exists() && !file.delete()) {
            throw new StorageException("Failed to delete data file for " + uuid);
        }
    }

    @Override
    public void close() {
        // Nothing to close for flat files
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @NotNull
    private File playerFile(@NotNull UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }
}
