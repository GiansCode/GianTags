package gg.gianluca.giantags.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import gg.gianluca.giantags.api.event.TagClearEvent;
import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.config.CategoriesConfig;
import gg.gianluca.giantags.config.GuiConfig;
import gg.gianluca.giantags.config.MessagesConfig;
import gg.gianluca.giantags.config.TagsConfig;
import gg.gianluca.giantags.storage.StorageManager;
import gg.gianluca.giantags.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Factory + update utility for the categories selection {@link PaginatedGui}.
 *
 * <p>Opened when a player runs {@code /tags} and categories are enabled.
 * Each item represents a category; clicking it opens the per-category tags GUI.
 * The navigation row always shows the player's current-tag display.
 */
public final class CategoriesGui {

    private CategoriesGui() {}

    /**
     * Creates, populates, and opens the categories GUI for the given player.
     */
    @NotNull
    public static PaginatedGui create(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull GuiManager guiManager
    ) {
        CategoriesConfig cfg = getCategoriesConfig(plugin);

        Component title = TextUtil.parse(cfg.getTitle());
        PaginatedGui gui = Gui.paginated()
                .title(title)
                .rows(cfg.getRows())
                .pageSize(cfg.getPageSize())
                .disableAllInteractions()
                .create();

        populateCategoryItems(plugin, player, gui, guiManager);
        populateNavItems(plugin, player, gui, guiManager);

        return gui;
    }

    /**
     * Refreshes only the navigation row items (current-tag display, page indicators).
     * Called by the periodic update task.
     */
    public static void updateNavItems(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull PaginatedGui gui
    ) {
        CategoriesConfig cfg = getCategoriesConfig(plugin);
        StorageManager storage = getStorageManager(plugin);

        String currentTagId = storage.getPlayerData(player.getUniqueId())
                .flatMap(d -> Optional.ofNullable(d.getTagId()))
                .orElse(null);
        String playerTag = currentTagId != null ? getTagRaw(plugin, currentTagId) : "None";
        String page = String.valueOf(gui.getCurrentPageNum());
        String pages = String.valueOf(gui.getPagesNum());

        // Current-tag display
        GuiConfig.CurrentTagItem ctCfg = cfg.getCurrentTag();
        ItemStack ctItem = buildCurrentTagItem(ctCfg, player, currentTagId, playerTag, page, pages);
        gui.updateItem(ctCfg.slot(), ctItem);

        // Prev / Next page indicators
        gui.updateItem(cfg.getPrevPage().slot(),
                cfg.getPrevPage().item().buildPlain(
                        "player", player.getName(), "player_tag", playerTag,
                        "player_tag_id", currentTagId != null ? currentTagId : "none",
                        "page", page, "pages", pages));
        gui.updateItem(cfg.getNextPage().slot(),
                cfg.getNextPage().item().buildPlain(
                        "player", player.getName(), "player_tag", playerTag,
                        "player_tag_id", currentTagId != null ? currentTagId : "none",
                        "page", page, "pages", pages));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void populateCategoryItems(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull PaginatedGui gui,
            @NotNull GuiManager guiManager
    ) {
        CategoriesConfig cfg = getCategoriesConfig(plugin);
        TagsConfig tagsConfig = getTagsConfig(plugin);
        StorageManager storage = getStorageManager(plugin);

        String currentTagId = storage.getPlayerData(player.getUniqueId())
                .flatMap(d -> Optional.ofNullable(d.getTagId()))
                .orElse(null);
        String playerTag = currentTagId != null ? getTagRaw(plugin, currentTagId) : "None";
        String playerTagId = currentTagId != null ? currentTagId : "none";

        for (CategoriesConfig.Category category : cfg.getCategories()) {
            long tagCount = tagsConfig.getTags().values().stream()
                    .filter(t -> category.id().equals(t.getCategory()))
                    .filter(t -> player.hasPermission(t.getPermission()))
                    .count();

            ItemStack item = category.item().buildPlain(
                    "category_name", category.name(),
                    "tag_count", String.valueOf(tagCount),
                    "player", player.getName(),
                    "player_tag", playerTag,
                    "player_tag_id", playerTagId
            );

            final String categoryId = category.id();
            gui.addItem(new GuiItem(item, event -> {
                if (!(event.getWhoClicked() instanceof Player clicker)) return;
                guiManager.openTagsGuiForCategory(clicker, categoryId);
            }));
        }

        // Fill remaining empty slots on the last page
        if (cfg.isFillerEnabled()) {
            int catCount = cfg.getCategories().size();
            int ps = cfg.getPageSize();
            int empty = catCount == 0
                    ? ps
                    : (catCount % ps == 0 ? 0 : ps - (catCount % ps));
            if (empty > 0) {
                ItemStack filler = cfg.getFillerItem().buildPlain();
                GuiItem fillerItem = new GuiItem(filler);
                for (int i = 0; i < empty; i++) gui.addItem(fillerItem);
            }
        }
    }

    private static void populateNavItems(
            @NotNull JavaPlugin plugin,
            @NotNull Player player,
            @NotNull PaginatedGui gui,
            @NotNull GuiManager guiManager
    ) {
        CategoriesConfig cfg = getCategoriesConfig(plugin);
        StorageManager storage = getStorageManager(plugin);

        String currentTagId = storage.getPlayerData(player.getUniqueId())
                .flatMap(d -> Optional.ofNullable(d.getTagId()))
                .orElse(null);
        String playerTag = currentTagId != null ? getTagRaw(plugin, currentTagId) : "None";
        String playerTagId = currentTagId != null ? currentTagId : "none";
        String page = String.valueOf(gui.getCurrentPageNum());
        String pages = String.valueOf(gui.getPagesNum());

        // Previous page
        ItemStack prevItem = cfg.getPrevPage().item().buildPlain(
                "player", player.getName(), "player_tag", playerTag,
                "player_tag_id", playerTagId, "page", page, "pages", pages);
        gui.setItem(cfg.getPrevPage().slot(), new GuiItem(prevItem, e -> {
            gui.previous();
            updateNavItems(plugin, player, gui);
        }));

        // Next page
        ItemStack nextItem = cfg.getNextPage().item().buildPlain(
                "player", player.getName(), "player_tag", playerTag,
                "player_tag_id", playerTagId, "page", page, "pages", pages);
        gui.setItem(cfg.getNextPage().slot(), new GuiItem(nextItem, e -> {
            gui.next();
            updateNavItems(plugin, player, gui);
        }));

        // Close
        ItemStack closeItem = cfg.getClose().item().buildPlain(
                "player", player.getName(), "player_tag", playerTag);
        gui.setItem(cfg.getClose().slot(), new GuiItem(closeItem, e -> player.closeInventory()));

        // Current-tag display — clicking clears the active tag in-place
        GuiConfig.CurrentTagItem ctCfg = cfg.getCurrentTag();
        ItemStack ctItem = buildCurrentTagItem(ctCfg, player, currentTagId, playerTag, page, pages);
        gui.setItem(ctCfg.slot(), new GuiItem(ctItem, e -> {
            if (!(e.getWhoClicked() instanceof Player clicker)) return;
            String activeId = getStorageManager(plugin)
                    .getPlayerData(clicker.getUniqueId())
                    .flatMap(d -> Optional.ofNullable(d.getTagId()))
                    .orElse(null);
            if (activeId == null) return;
            Tag tag = getTagsConfig(plugin).getTags().get(activeId);
            if (tag == null) return;

            TagClearEvent clearEvent = new TagClearEvent(clicker, tag);
            plugin.getServer().getPluginManager().callEvent(clearEvent);
            if (clearEvent.isCancelled()) return;

            getStorageManager(plugin).clearTag(clicker.getUniqueId());
            clicker.sendMessage(getMessagesConfig(plugin).get("tag-cleared"));
            updateNavItems(plugin, clicker, gui);
        }));

        // Nav filler
        if (cfg.isNavFillerEnabled()) {
            ItemStack filler = cfg.getNavFillerItem().buildPlain();
            GuiItem fillerItem = new GuiItem(filler);
            for (int slot : cfg.getNavFillerSlots()) {
                gui.setItem(slot, fillerItem);
            }
        }
    }

    @NotNull
    private static ItemStack buildCurrentTagItem(
            @NotNull GuiConfig.CurrentTagItem ctCfg,
            @NotNull Player player,
            @Nullable String currentTagId,
            @NotNull String playerTag,
            @NotNull String page,
            @NotNull String pages
    ) {
        ItemStack item;
        if (currentTagId != null) {
            item = ctCfg.hasTagItem().buildPlain(
                    "player", player.getName(),
                    "player_tag", playerTag,
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

    private static CategoriesConfig getCategoriesConfig(@NotNull JavaPlugin plugin) {
        return ((gg.gianluca.giantags.GianTags) plugin).getConfigManager().getCategoriesConfig();
    }

    private static StorageManager getStorageManager(@NotNull JavaPlugin plugin) {
        return ((gg.gianluca.giantags.GianTags) plugin).getStorageManager();
    }

    private static TagsConfig getTagsConfig(@NotNull JavaPlugin plugin) {
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
