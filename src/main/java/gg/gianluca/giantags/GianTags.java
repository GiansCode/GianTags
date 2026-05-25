package gg.gianluca.giantags;

import dev.triumphteam.gui.TriumphGui;
import gg.gianluca.giantags.api.GianTagsProvider;
import gg.gianluca.giantags.api.event.TagReloadEvent;
import gg.gianluca.giantags.command.AliasCommand;
import gg.gianluca.giantags.config.ConfigManager;
import gg.gianluca.giantags.gui.GuiManager;
import gg.gianluca.giantags.placeholder.GianTagsExpansion;
import gg.gianluca.giantags.storage.StorageManager;
import gg.gianluca.giantags.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main plugin class for GianTags.
 *
 * <h3>Startup sequence</h3>
 * <ol>
 *   <li>{@link GianTagsBootstrapper} registers Brigadier commands (bootstrap phase).</li>
 *   <li>{@link #onEnable()} loads configs, storage, GUI manager, events, PAPI, aliases.</li>
 * </ol>
 */
public final class GianTags extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private StorageManager storageManager;
    private GuiManager guiManager;

    private final List<String> registeredAliases = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        // Config + managers
        configManager = new ConfigManager(this);
        configManager.loadAll();

        storageManager = new StorageManager(this);
        storageManager.initialize(configManager.getStorageConfig());

        // TriumphGUI is loaded by Paper's library classloader, not the plugin
        // classloader, so its auto-detection via JavaPlugin.getProvidingPlugin()
        // fails. Explicitly registering the plugin instance here fixes that.
        TriumphGui.init(this);
        guiManager = new GuiManager(this);

        // Publish the API
        GianTagsProvider.initialize(new GianTagsImpl(this));

        // Register player data listeners
        getServer().getPluginManager().registerEvents(this, this);

        // Register dynamic alias commands from config
        registerAliasCommands();

        // PlaceholderAPI hook
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new GianTagsExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        // Pre-load data for already-online players (e.g. plugin hot-reload)
        for (Player player : getServer().getOnlinePlayers()) {
            storageManager.loadPlayerAsync(player.getUniqueId(), null);
        }

        if (SchedulerAdapter.IS_FOLIA) {
            getLogger().info("Folia detected — using region-aware schedulers.");
        }

        getLogger().info("GianTags v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // Close all open GUIs gracefully
        if (guiManager != null) {
            guiManager.closeAll();
        }

        // Flush all dirty player data synchronously before shutdown
        if (storageManager != null) {
            storageManager.saveAllSync();
            storageManager.close();
        }

        GianTagsProvider.uninitialize();

        getLogger().info("GianTags disabled. All data saved.");
    }

    // ── Plugin reload (called by ReloadSubCommand via GianTagsImpl) ───────────

    /**
     * Reloads all configs and refreshes in-memory state.
     */
    public void performReload() {
        // Close open GUIs before clearing config state
        guiManager.closeAll();

        // Re-load all config files
        configManager.reloadAll();

        // Re-register alias commands (config may have changed)
        unregisterAliasCommands();
        registerAliasCommands();

        // Fire the public API reload event
        TagReloadEvent event = new TagReloadEvent();
        getServer().getPluginManager().callEvent(event);

        getLogger().info("GianTags reloaded.");
    }

    // ── Player Data Listeners ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        storageManager.loadPlayerAsync(event.getPlayer().getUniqueId(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        storageManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    // ── Alias Command Registration ────────────────────────────────────────────

    private void registerAliasCommands() {
        CommandMap commandMap = getServer().getCommandMap();
        List<String> aliases = configManager.getMainConfig().getCommandAliases();

        for (String alias : aliases) {
            AliasCommand cmd = new AliasCommand(alias, this);
            if (commandMap.register("giantags", cmd)) {
                registeredAliases.add(alias);
            } else {
                getLogger().warning("Could not register alias '/" + alias + "' — may conflict with another plugin.");
            }
        }

        if (!registeredAliases.isEmpty()) {
            getLogger().info("Registered aliases: " + String.join(", ", registeredAliases.stream().map(a -> "/" + a).toList()));
        }
    }

    @SuppressWarnings("unchecked")
    private void unregisterAliasCommands() {
        try {
            CommandMap commandMap = getServer().getCommandMap();
            // Access known commands map to unregister
            var knownCommands = commandMap.getKnownCommands();
            for (String alias : registeredAliases) {
                var cmd = knownCommands.get(alias);
                if (cmd instanceof AliasCommand ac) {
                    ac.unregister(commandMap);
                    knownCommands.remove(alias);
                    knownCommands.remove("giantags:" + alias);
                }
            }
        } catch (Exception e) {
            getLogger().warning("Failed to unregister alias commands: " + e.getMessage());
        }
        registeredAliases.clear();
    }

    // ── Manager Accessors ─────────────────────────────────────────────────────

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @NotNull
    public StorageManager getStorageManager() {
        return storageManager;
    }

    @NotNull
    public GuiManager getGuiManager() {
        return guiManager;
    }
}
