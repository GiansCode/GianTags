package gg.gianluca.giantags.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import gg.gianluca.giantags.api.event.TagClearEvent;
import gg.gianluca.giantags.api.event.TagSelectEvent;
import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.config.GuiConfig;
import gg.gianluca.giantags.config.MessagesConfig;
import gg.gianluca.giantags.storage.StorageManager;
import gg.gianluca.giantags.util.ItemBuilder;
import gg.gianluca.giantags.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Factory + update utility for the player's tags {@link PaginatedGui}.
 *
 * <p>Creates one {@link PaginatedGui} per player open session.
 * The GUI is entirely re-built on click (for immediate correctness)
 * while the auto-update task refreshes only the dynamic nav row.
 */
public final class TagsGui {

    private TagsGui() {}

    /**
     * Creates, populates, and opens the tags GUI for the given player.
     */
    @NotNull
    public static PaginatedGui create(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull GuiManager guiManager
    ) {
        GuiConfig cfg = getGuiConfig(plugin);
        StorageManager storage = getStorageManager(plugin);

        Component title = TextUtil.parse(cfg.getTitle());
        PaginatedGui gui = Gui.paginated()
                .title(title)
                .rows(cfg.getRows())
                .pageSize(cfg.getPageSize())
                .disableAllInteractions()
                .create();

        populateTagItems(plugin, player, gui, guiManager);
        populateNavItems(plugin, player, gui, guiManager);

        return gui;
    }

    /**
     * Refreshes only the navigation row items (current-tag display, page indicators).
     * Called by the periodic update task — cheap, avoids full reconstruction.
     */
    public static void updateNavItems(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull PaginatedGui gui
    ) {
        GuiConfig cfg = getGuiConfig(plugin);
        StorageManager storage = getStorageManager(plugin);

        String currentTagId = storage.getPlayerData(player.getUniqueId())
                .flatMap(d -> Optional.ofNullable(d.getTagId()))
                .orElse(null);

        // Resolve current-tag display item
        GuiConfig.CurrentTagItem ctCfg = cfg.getCurrentTag();
        ItemStack ctItem = buildCurrentTagItem(ctCfg, player, currentTagId,
                String.valueOf(gui.getCurrentPageNum()),
                String.valueOf(gui.getPagesNum()));
        gui.updateItem(ctCfg.slot(), ctItem);

        // Page indicators on prev/next buttons
        String page = String.valueOf(gui.getCurrentPageNum());
        String pages = String.valueOf(gui.getPagesNum());

        ItemStack prevItem = cfg.getPrevPage().item().buildPlain(
                "player", player.getName(),
                "player_tag", currentTagId != null ? getTagRaw(plugin, currentTagId) : "None",
                "page", page,
                "pages", pages
        );
        gui.updateItem(cfg.getPrevPage().slot(), prevItem);

        ItemStack nextItem = cfg.getNextPage().item().buildPlain(
                "player", player.getName(),
                "player_tag", currentTagId != null ? getTagRaw(plugin, currentTagId) : "None",
                "page", page,
                "pages", pages
        );
        gui.updateItem(cfg.getNextPage().slot(), nextItem);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void populateTagItems(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull PaginatedGui gui,
            @NotNull GuiManager guiManager
    ) {
        StorageManager storage = getStorageManager(plugin);
        String currentTagId = storage.getPlayerData(player.getUniqueId())
                .flatMap(d -> Optional.ofNullable(d.getTagId()))
                .orElse(null);

        List<Tag> tags = getTagsConfig(plugin).getTags().values().stream()
                .filter(tag -> player.hasPermission(tag.getPermission()))
                .sorted(java.util.Comparator.comparingInt(Tag::getPosition))
                .toList();

        for (Tag tag : tags) {
            boolean selected = tag.getId().equals(currentTagId);
            ItemStack item = tag.buildGuiItem(player, selected);

            gui.addItem(new GuiItem(item, event -> {
                if (!(event.getWhoClicked() instanceof Player clicker)) return;
                handleTagClick(plugin, clicker, tag, guiManager);
            }));
        }

        // Fill the remaining empty slots on the last page with the background item.
        // These are added as PAGINATED items (not static) — static items in paginated
        // slots would be detected by calculatePageSize() as "occupied", zeroing out the
        // available page slots and preventing ALL tag items from rendering.
        GuiConfig cfg = getGuiConfig(plugin);
        if (cfg.isFillerEnabled()) {
            int tagCount = tags.size();
            int pageSize = cfg.getPageSize();
            int emptySlots = tagCount == 0
                    ? pageSize
                    : (tagCount % pageSize == 0 ? 0 : pageSize - (tagCount % pageSize));
            if (emptySlots > 0) {
                ItemStack fillerStack = cfg.getFillerItem().buildPlain();
                GuiItem filler = new GuiItem(fillerStack);
                for (int i = 0; i < emptySlots; i++) {
                    gui.addItem(filler);
                }
            }
        }
    }

    private static void handleTagClick(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull Tag tag,
            @NotNull GuiManager guiManager
    ) {
        StorageManager storage = getStorageManager(plugin);
        MessagesConfig msg = getMessagesConfig(plugin);

        String currentId = storage.getPlayerData(player.getUniqueId())
                .flatMap(d -> Optional.ofNullable(d.getTagId()))
                .orElse(null);
        boolean isSelected = tag.getId().equals(currentId);

        if (isSelected) {
            TagClearEvent clearEvent = new TagClearEvent(player, tag);
            plugin.getServer().getPluginManager().callEvent(clearEvent);
            if (clearEvent.isCancelled()) return;

            storage.clearTag(player.getUniqueId());
            player.sendMessage(msg.get("tag-cleared"));
        } else {
            Tag previousTag = currentId != null ? getTagsConfig(plugin).getTags().get(currentId) : null;
            TagSelectEvent selectEvent = new TagSelectEvent(player, tag, previousTag);
            plugin.getServer().getPluginManager().callEvent(selectEvent);
            if (selectEvent.isCancelled()) return;

            storage.setTagId(player.getUniqueId(), tag.getId());
            player.sendMessage(msg.get("tag-selected", "tag", tag.getRawTag()));
        }

        // Refresh the GUI in-place so the player's current page is preserved.
        // clearPageItems(false) clears the paginated item list without resetting
        // the internal page counter; gui.update() re-renders the same page in the
        // already-open inventory — no close/reopen, no cursor position reset.
        PaginatedGui gui = guiManager.getOpenGui(player.getUniqueId());
        if (gui != null) {
            gui.clearPageItems(false);
            populateTagItems(plugin, player, gui, guiManager);
            gui.update();
            updateNavItems(plugin, player, gui);
        }
    }

    private static void populateNavItems(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull PaginatedGui gui,
            @NotNull GuiManager guiManager
    ) {
        GuiConfig cfg = getGuiConfig(plugin);
        StorageManager storage = getStorageManager(plugin);

        String currentTagId = storage.getPlayerData(player.getUniqueId())
                .flatMap(d -> Optional.ofNullable(d.getTagId()))
                .orElse(null);
        String playerTag = currentTagId != null ? getTagRaw(plugin, currentTagId) : "None";
        String page = String.valueOf(gui.getCurrentPageNum());
        String pages = String.valueOf(gui.getPagesNum());

        // Previous page
        ItemStack prevItem = cfg.getPrevPage().item().buildPlain(
                "player", player.getName(), "player_tag", playerTag,
                "player_tag_id", currentTagId != null ? currentTagId : "none",
                "page", page, "pages", pages);
        gui.setItem(cfg.getPrevPage().slot(), new GuiItem(prevItem, e -> {
            gui.previous();
            updateNavItems(plugin, player, gui);
        }));

        // Next page
        ItemStack nextItem = cfg.getNextPage().item().buildPlain(
                "player", player.getName(), "player_tag", playerTag,
                "player_tag_id", currentTagId != null ? currentTagId : "none",
                "page", page, "pages", pages);
        gui.setItem(cfg.getNextPage().slot(), new GuiItem(nextItem, e -> {
            gui.next();
            updateNavItems(plugin, player, gui);
        }));

        // Close
        ItemStack closeItem = cfg.getClose().item().buildPlain(
                "player", player.getName(), "player_tag", playerTag);
        gui.setItem(cfg.getClose().slot(), new GuiItem(closeItem, e -> player.closeInventory()));

        // Current tag display
        GuiConfig.CurrentTagItem ctCfg = cfg.getCurrentTag();
        ItemStack ctItem = buildCurrentTagItem(ctCfg, player, currentTagId, page, pages);
        // Clicking the current-tag display clears the active tag.
        // The tag id is resolved fresh at click time — NOT from the captured
        // 'currentTagId' variable, which is stale after in-place tag updates.
        gui.setItem(ctCfg.slot(), new GuiItem(ctItem, e -> {
            if (!(e.getWhoClicked() instanceof Player clicker)) return;
            String activeId = getStorageManager(plugin)
                    .getPlayerData(clicker.getUniqueId())
                    .flatMap(d -> Optional.ofNullable(d.getTagId()))
                    .orElse(null);
            if (activeId == null) return;
            Tag tag = getTagsConfig(plugin).getTags().get(activeId);
            if (tag == null) return;
            handleTagClick(plugin, clicker, tag, guiManager);
        }));

        // Navigation row filler
        if (cfg.isNavFillerEnabled()) {
            ItemStack fillerItem = cfg.getNavFillerItem().buildPlain();
            GuiItem filler = new GuiItem(fillerItem);
            for (int slot : cfg.getNavFillerSlots()) {
                gui.setItem(slot, filler);
            }
        }

    }

    /**
     * Builds the current-tag nav item and, if the configured type is
     * {@code PLAYER_HEAD}, stamps the viewer's own skin onto it so it never
     * resets to the Steve head — even when called from the periodic update task.
     */
    @NotNull
    private static ItemStack buildCurrentTagItem(
            @NotNull GuiConfig.CurrentTagItem ctCfg,
            @NotNull Player player,
            @Nullable String currentTagId,
            @NotNull String page,
            @NotNull String pages
    ) {
        ItemStack item;
        if (currentTagId != null) {
            item = ctCfg.hasTagItem().buildPlain(
                    "player", player.getName(),
                    "player_tag", currentTagId,
                    "player_tag_id", currentTagId,
                    "page", page, "pages", pages);
            if (item.getType() == org.bukkit.Material.PLAYER_HEAD) {
                org.bukkit.inventory.meta.SkullMeta skull =
                        (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
                if (skull != null) {
                    skull.setPlayerProfile(player.getPlayerProfile());
                    item.setItemMeta(skull);
                }
            }
        } else {
            item = ctCfg.noTagItem().buildPlain(
                    "player", player.getName(),
                    "player_tag", "None",
                    "player_tag_id", "none",
                    "page", page, "pages", pages);
        }
        return item;
    }

    // ── Plugin accessor shorthands ────────────────────────────────────────────

    private static GuiConfig getGuiConfig(@NotNull JavaPlugin plugin) {
        return ((gg.gianluca.giantags.GianTags) plugin).getConfigManager().getGuiConfig();
    }

    private static StorageManager getStorageManager(@NotNull JavaPlugin plugin) {
        return ((gg.gianluca.giantags.GianTags) plugin).getStorageManager();
    }

    private static gg.gianluca.giantags.config.TagsConfig getTagsConfig(@NotNull JavaPlugin plugin) {
        return ((gg.gianluca.giantags.GianTags) plugin).getConfigManager().getTagsConfig();
    }

    private static MessagesConfig getMessagesConfig(@NotNull JavaPlugin plugin) {
        return ((gg.gianluca.giantags.GianTags) plugin).getConfigManager().getMessagesConfig();
    }

    @NotNull
    private static String getTagRaw(@NotNull JavaPlugin plugin, @NotNull String tagId) {
        Tag t = getTagsConfig(plugin).getTags().get(tagId);
        return t != null ? t.getRawTag() : tagId;
    }
}
