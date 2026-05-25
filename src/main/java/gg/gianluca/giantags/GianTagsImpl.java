package gg.gianluca.giantags;

import gg.gianluca.giantags.api.GianTagsAPI;
import gg.gianluca.giantags.api.event.TagClearEvent;
import gg.gianluca.giantags.api.event.TagSetEvent;
import gg.gianluca.giantags.api.model.PlayerData;
import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.config.MessagesConfig;
import gg.gianluca.giantags.config.TagsConfig;
import gg.gianluca.giantags.gui.GuiManager;
import gg.gianluca.giantags.storage.StorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Concrete implementation of {@link GianTagsAPI}.
 * Delegates to the plugin's managers; all methods are safe to call from the main thread.
 */
public final class GianTagsImpl implements GianTagsAPI {

    private final GianTags plugin;

    GianTagsImpl(@NotNull GianTags plugin) {
        this.plugin = plugin;
    }

    // ── Tag Registry ──────────────────────────────────────────────────────────

    @Override
    @NotNull
    public Optional<Tag> getTag(@NotNull String id) {
        return Optional.ofNullable(plugin.getConfigManager().getTagsConfig().getTags().get(id));
    }

    @Override
    @NotNull
    public Collection<Tag> getAllTags() {
        return plugin.getConfigManager().getTagsConfig().getTags().values().stream()
                .sorted(Comparator.comparingInt(Tag::getPosition))
                .toList();
    }

    @Override
    @NotNull
    public Collection<Tag> getTagsForPlayer(@NotNull Player player) {
        return getAllTags().stream()
                .filter(tag -> player.hasPermission(tag.getPermission()))
                .toList();
    }

    // ── Player Tag State ──────────────────────────────────────────────────────

    @Override
    @NotNull
    public Optional<PlayerData> getPlayerData(@NotNull UUID uuid) {
        return plugin.getStorageManager().getPlayerData(uuid);
    }

    @Override
    @NotNull
    public Optional<Tag> getPlayerTag(@NotNull UUID uuid) {
        return getPlayerTagId(uuid).flatMap(this::getTag);
    }

    @Override
    @NotNull
    public Optional<String> getPlayerTagId(@NotNull UUID uuid) {
        return plugin.getStorageManager().getPlayerData(uuid)
                .flatMap(d -> Optional.ofNullable(d.getTagId()));
    }

    @Override
    public boolean setPlayerTag(
            @NotNull CommandSender executor,
            @NotNull UUID uuid,
            @NotNull String tagId,
            boolean ignorePermission
    ) {
        Tag tag = plugin.getConfigManager().getTagsConfig().getTags().get(tagId);
        if (tag == null) return false;

        // Permission check — only applies to online players
        if (!ignorePermission) {
            Player onlineTarget = plugin.getServer().getPlayer(uuid);
            if (onlineTarget != null && !onlineTarget.hasPermission(tag.getPermission())) {
                return false;
            }
        }

        Tag previousTag = getPlayerTag(uuid).orElse(null);

        // Fire TagSetEvent for admin-initiated changes
        Player onlineTarget = plugin.getServer().getPlayer(uuid);
        if (onlineTarget != null) {
            TagSetEvent event = new TagSetEvent(executor, onlineTarget, tag, previousTag, ignorePermission);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
        }

        plugin.getStorageManager().setTagId(uuid, tagId);

        // Refresh the player's GUI if open
        if (onlineTarget != null && plugin.getGuiManager().hasGuiOpen(uuid)) {
            plugin.getGuiManager().refreshGui(onlineTarget);
        }

        return true;
    }

    @Override
    public boolean clearPlayerTag(@NotNull UUID uuid) {
        if (getPlayerTagId(uuid).isEmpty()) return false;

        plugin.getStorageManager().clearTag(uuid);

        // Refresh GUI if open
        Player onlineTarget = plugin.getServer().getPlayer(uuid);
        if (onlineTarget != null && plugin.getGuiManager().hasGuiOpen(uuid)) {
            plugin.getGuiManager().refreshGui(onlineTarget);
        }

        return true;
    }

    // ── GUI ───────────────────────────────────────────────────────────────────

    @Override
    public void openTagsGui(@NotNull Player player) {
        plugin.getGuiManager().openTagsGui(player);
    }

    // ── Managers ──────────────────────────────────────────────────────────────

    @Override
    @NotNull
    public GuiManager getGuiManager() {
        return plugin.getGuiManager();
    }

    @Override
    @NotNull
    public StorageManager getStorageManager() {
        return plugin.getStorageManager();
    }

    @Override
    @NotNull
    public TagsConfig getTagsConfig() {
        return plugin.getConfigManager().getTagsConfig();
    }

    @Override
    @NotNull
    public MessagesConfig getMessagesConfig() {
        return plugin.getConfigManager().getMessagesConfig();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void reload() {
        plugin.performReload();
    }
}
