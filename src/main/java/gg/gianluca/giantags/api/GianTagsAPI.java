package gg.gianluca.giantags.api;

import gg.gianluca.giantags.api.model.PlayerData;
import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.config.MessagesConfig;
import gg.gianluca.giantags.config.TagsConfig;
import gg.gianluca.giantags.gui.GuiManager;
import gg.gianluca.giantags.storage.StorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for GianTags.
 *
 * <p>Obtain an instance via {@link GianTagsProvider#get()}.
 *
 * <p>All methods that modify state are <strong>thread-safe</strong>:
 * data mutations update the in-memory cache synchronously and schedule
 * async persistence. Methods that read GUI state must be called on the
 * main server thread.
 */
public interface GianTagsAPI {

    // ── Tag Registry ────────────────────────────────────────────────────────

    /**
     * Returns the tag with the given identifier, or empty if not found.
     */
    @NotNull
    Optional<Tag> getTag(@NotNull String id);

    /**
     * Returns all registered tags, sorted by their {@link Tag#getPosition()}.
     */
    @NotNull
    Collection<Tag> getAllTags();

    /**
     * Returns all tags the given player has permission to use,
     * sorted by position.
     */
    @NotNull
    Collection<Tag> getTagsForPlayer(@NotNull Player player);

    // ── Player Tag State ────────────────────────────────────────────────────

    /**
     * Returns the {@link PlayerData} for the given UUID if loaded, or empty.
     * Data is loaded when a player joins and unloaded when they leave.
     */
    @NotNull
    Optional<PlayerData> getPlayerData(@NotNull UUID uuid);

    /**
     * Returns the active {@link Tag} for the given player, or empty.
     */
    @NotNull
    Optional<Tag> getPlayerTag(@NotNull UUID uuid);

    /**
     * Returns the active tag's identifier string for the given player, or empty.
     */
    @NotNull
    Optional<String> getPlayerTagId(@NotNull UUID uuid);

    /**
     * Sets a player's active tag.
     *
     * @param uuid              the target player's UUID
     * @param tagId             the tag identifier to set
     * @param ignorePermission  if {@code true}, skips the permission check
     * @return {@code true} if the tag was applied; {@code false} if refused
     *         (e.g. player lacks permission and {@code ignorePermission} is false,
     *         or the tag does not exist, or a {@link gg.gianluca.giantags.api.event.TagSetEvent} was cancelled)
     */
    boolean setPlayerTag(
            @NotNull CommandSender executor,
            @NotNull UUID uuid,
            @NotNull String tagId,
            boolean ignorePermission
    );

    /**
     * Clears the active tag for the given player.
     * Fires a {@link gg.gianluca.giantags.api.event.TagClearEvent} if the player is online.
     *
     * @return {@code true} if a tag was actually removed
     */
    boolean clearPlayerTag(@NotNull UUID uuid);

    // ── GUI ──────────────────────────────────────────────────────────────────

    /**
     * Opens the paginated tags GUI for the given player.
     * Must be called on the main thread.
     */
    void openTagsGui(@NotNull Player player);

    // ── Managers ──────────────────────────────────────────────────────────────

    @NotNull
    GuiManager getGuiManager();

    @NotNull
    StorageManager getStorageManager();

    @NotNull
    TagsConfig getTagsConfig();

    @NotNull
    MessagesConfig getMessagesConfig();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Reloads all plugin configuration and refreshes in-memory state.
     * Closes all open GUIs and fires {@link gg.gianluca.giantags.api.event.TagReloadEvent}.
     */
    void reload();
}
