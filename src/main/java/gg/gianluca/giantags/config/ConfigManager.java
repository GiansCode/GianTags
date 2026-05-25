package gg.gianluca.giantags.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Manages all plugin configuration files and provides typed config objects.
 * Call {@link #loadAll()} on startup and reload.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;

    private final MainConfig mainConfig;
    private final TagsConfig tagsConfig;
    private final GuiConfig guiConfig;
    private final StorageConfig storageConfig;
    private final MessagesConfig messagesConfig;

    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.mainConfig = new MainConfig(plugin.getConfig());
        this.tagsConfig = new TagsConfig(plugin.getLogger());
        this.guiConfig = new GuiConfig(plugin.getLogger());
        this.storageConfig = new StorageConfig();
        this.messagesConfig = new MessagesConfig();
    }

    /**
     * Saves all default config files (if absent) and reloads everything into memory.
     */
    public void loadAll() {
        saveDefaults();
        reloadAll();
    }

    /**
     * Reloads all configs from disk into memory. Does NOT re-save defaults.
     */
    public void reloadAll() {
        // Main config
        plugin.reloadConfig();
        mainConfig.load(plugin.getConfig());

        // Tags
        FileConfiguration tagsFile = loadYaml("tags.yml");
        tagsConfig.load(tagsFile);

        // GUI
        FileConfiguration guiFile = loadYaml("gui.yml");
        guiConfig.load(guiFile);

        // Storage
        FileConfiguration storageFile = loadYaml("storage.yml");
        storageConfig.load(storageFile);

        // Messages
        FileConfiguration messagesFile = loadYaml("messages.yml");
        messagesConfig.load(messagesFile);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @NotNull public MainConfig getMainConfig() { return mainConfig; }
    @NotNull public TagsConfig getTagsConfig() { return tagsConfig; }
    @NotNull public GuiConfig getGuiConfig() { return guiConfig; }
    @NotNull public StorageConfig getStorageConfig() { return storageConfig; }
    @NotNull public MessagesConfig getMessagesConfig() { return messagesConfig; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void saveDefaults() {
        plugin.saveDefaultConfig();
        saveDefaultResource("tags.yml");
        saveDefaultResource("gui.yml");
        saveDefaultResource("storage.yml");
        saveDefaultResource("messages.yml");
    }

    private void saveDefaultResource(@NotNull String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    /**
     * Loads a YAML file from the plugin data folder, merging with bundled defaults.
     */
    @NotNull
    private FileConfiguration loadYaml(@NotNull String name) {
        File file = new File(plugin.getDataFolder(), name);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Merge with bundled defaults so new keys appear automatically
        InputStream defaults = plugin.getResource(name);
        if (defaults != null) {
            try (InputStreamReader reader = new InputStreamReader(defaults, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                config.setDefaults(defaultConfig);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not load defaults for " + name, e);
            }
        }

        return config;
    }
}
