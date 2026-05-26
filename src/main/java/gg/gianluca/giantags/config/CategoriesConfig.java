package gg.gianluca.giantags.config;

import gg.gianluca.giantags.config.model.ConfiguredItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

/**
 * Typed wrapper for {@code categories.yml}.
 *
 * <p>Holds the ordered list of categories and all GUI settings for both
 * the categories selection screen and the per-category tags view.
 */
public final class CategoriesConfig {

    private final Logger logger;

    // Category registry
    private List<Category> categories = List.of();

    // Categories GUI settings
    private String title;
    private int rows;
    private int pageSize;
    private int updateInterval;
    private boolean fillerEnabled;
    private ConfiguredItem fillerItem;
    private GuiConfig.NavItem prevPage;
    private GuiConfig.NavItem nextPage;
    private GuiConfig.NavItem close;
    private GuiConfig.CurrentTagItem currentTag;
    private boolean navFillerEnabled;
    private List<Integer> navFillerSlots;
    private ConfiguredItem navFillerItem;

    // Tags-GUI overrides for per-category browsing
    private String tagsTitle;
    private GuiConfig.NavItem backButton;

    public CategoriesConfig(@NotNull Logger logger) {
        this.logger = logger;
        loadDefaults();
    }

    public void load(@NotNull FileConfiguration config) {
        // ── Categories ────────────────────────────────────────────────────────
        List<Category> loaded = new ArrayList<>();
        ConfigurationSection catSection = config.getConfigurationSection("categories");
        if (catSection != null) {
            for (String id : catSection.getKeys(false)) {
                ConfigurationSection sec = catSection.getConfigurationSection(id);
                if (sec == null) continue;
                try {
                    String name = sec.getString("name", id);
                    int position = sec.getInt("position", Integer.MAX_VALUE);
                    ConfiguredItem item = parseItem(
                            sec.getConfigurationSection("item"),
                            Material.CHEST,
                            "<white>" + name + "</white>"
                    );
                    loaded.add(new Category(id, name, position, item));
                } catch (Exception e) {
                    logger.warning("Failed to load category '" + id + "': " + e.getMessage());
                }
            }
        }
        loaded.sort(Comparator.comparingInt(Category::position));
        this.categories = Collections.unmodifiableList(loaded);
        logger.info("Loaded " + categories.size() + " category(ies).");

        // ── GUI Settings ──────────────────────────────────────────────────────
        ConfigurationSection gui = config.getConfigurationSection("gui");
        if (gui == null) {
            loadDefaults();
            return;
        }

        title = gui.getString("title", "<dark_gray>✦ <gold>Categories</gold> ✦</dark_gray>");
        rows = Math.max(1, Math.min(6, gui.getInt("rows", 3)));
        pageSize = Math.max(1, gui.getInt("page-size", 18));
        updateInterval = Math.max(1, gui.getInt("update-interval", 10));

        ConfigurationSection fillerSec = gui.getConfigurationSection("filler");
        fillerEnabled = fillerSec != null && fillerSec.getBoolean("enabled", true);
        fillerItem = parseItem(
                fillerSec != null ? fillerSec.getConfigurationSection("item") : null,
                Material.BLACK_STAINED_GLASS_PANE, " "
        );

        ConfigurationSection nav = gui.getConfigurationSection("navigation");
        prevPage = parseNavItem(nav, "previous-page", 21, Material.ARROW, "<yellow>◀ Previous Page</yellow>");
        nextPage = parseNavItem(nav, "next-page", 23, Material.ARROW, "<yellow>Next Page ▶</yellow>");
        close = parseNavItem(nav, "close", 22, Material.BARRIER, "<red>✖ Close</red>");
        currentTag = parseCurrentTagItem(nav);

        ConfigurationSection navFillerSec = nav != null ? nav.getConfigurationSection("nav-filler") : null;
        navFillerEnabled = navFillerSec != null && navFillerSec.getBoolean("enabled", true);
        navFillerSlots = navFillerSec != null
                ? navFillerSec.getIntegerList("slots")
                : List.of(19, 20, 24, 25, 26);
        navFillerItem = parseItem(
                navFillerSec != null ? navFillerSec.getConfigurationSection("item") : null,
                Material.GRAY_STAINED_GLASS_PANE, " "
        );

        tagsTitle = gui.getString("tags-title",
                "<dark_gray>✦ <gold>{category_name}</gold> Tags ✦</dark_gray>");
        backButton = parseNavItem(gui, "back-button", 47, Material.ARROW,
                "<yellow>◀ Back to Categories</yellow>");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @NotNull public List<Category> getCategories() { return categories; }
    @NotNull public String getTitle() { return title; }
    public int getRows() { return rows; }
    public int getPageSize() { return pageSize; }
    public int getUpdateInterval() { return updateInterval; }
    public boolean isFillerEnabled() { return fillerEnabled; }
    @NotNull public ConfiguredItem getFillerItem() { return fillerItem; }
    @NotNull public GuiConfig.NavItem getPrevPage() { return prevPage; }
    @NotNull public GuiConfig.NavItem getNextPage() { return nextPage; }
    @NotNull public GuiConfig.NavItem getClose() { return close; }
    @NotNull public GuiConfig.CurrentTagItem getCurrentTag() { return currentTag; }
    public boolean isNavFillerEnabled() { return navFillerEnabled; }
    @NotNull public List<Integer> getNavFillerSlots() { return navFillerSlots; }
    @NotNull public ConfiguredItem getNavFillerItem() { return navFillerItem; }
    @NotNull public String getTagsTitle() { return tagsTitle; }
    @NotNull public GuiConfig.NavItem getBackButton() { return backButton; }

    // ── Nested types ─────────────────────────────────────────────────────────

    public record Category(@NotNull String id, @NotNull String name, int position, @NotNull ConfiguredItem item) {}

    // ── Parsing helpers ──────────────────────────────────────────────────────

    @NotNull
    private GuiConfig.NavItem parseNavItem(
            @Nullable ConfigurationSection sec,
            @NotNull String key,
            int defaultSlot,
            @NotNull Material defaultMaterial,
            @NotNull String defaultName
    ) {
        if (sec == null) return new GuiConfig.NavItem(defaultSlot, simpleItem(defaultMaterial, defaultName));
        ConfigurationSection keySec = sec.getConfigurationSection(key);
        int slot = keySec != null ? keySec.getInt("slot", defaultSlot) : defaultSlot;
        ConfiguredItem item = parseItem(
                keySec != null ? keySec.getConfigurationSection("item") : null,
                defaultMaterial, defaultName
        );
        return new GuiConfig.NavItem(slot, item);
    }

    @NotNull
    private GuiConfig.CurrentTagItem parseCurrentTagItem(@Nullable ConfigurationSection nav) {
        int defaultSlot = 18;
        if (nav == null) {
            return new GuiConfig.CurrentTagItem(defaultSlot,
                    simpleItem(Material.NAME_TAG, "<yellow>Current Tag</yellow>"),
                    simpleItem(Material.PAPER, "<yellow>Current Tag</yellow>"));
        }
        ConfigurationSection sec = nav.getConfigurationSection("current-tag");
        int slot = sec != null ? sec.getInt("slot", defaultSlot) : defaultSlot;
        ConfiguredItem hasTag = parseItem(
                sec != null ? sec.getConfigurationSection("has-tag") : null,
                Material.NAME_TAG, "<yellow>Current Tag</yellow>"
        );
        ConfiguredItem noTag = parseItem(
                sec != null ? sec.getConfigurationSection("no-tag") : null,
                Material.PAPER, "<yellow>Current Tag</yellow>"
        );
        return new GuiConfig.CurrentTagItem(slot, hasTag, noTag);
    }

    @NotNull
    private ConfiguredItem parseItem(@Nullable ConfigurationSection sec,
                                     @NotNull Material fallback,
                                     @NotNull String fallbackName) {
        if (sec == null) return simpleItem(fallback, fallbackName);

        Material material = fallback;
        String matName = sec.getString("type");
        if (matName != null) {
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown material '" + matName + "' in categories.yml, using " + fallback);
            }
        }

        int amount = Math.max(1, Math.min(64, sec.getInt("amount", 1)));
        String name = sec.getString("name", fallbackName);
        List<String> lore = sec.getStringList("lore");
        List<String> enchantments = sec.getStringList("enchantments");
        List<ItemFlag> flags = new ArrayList<>();
        for (String f : sec.getStringList("flags")) {
            try {
                flags.add(ItemFlag.valueOf(f.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                logger.warning("Unknown ItemFlag '" + f + "' in categories.yml");
            }
        }
        int cmd = sec.getInt("custom-model-data", 0);
        boolean glow = sec.getBoolean("glow", false);
        String skull = sec.getString("skull-texture");
        return new ConfiguredItem(material, amount, name, lore, null, enchantments, flags, cmd, glow, skull);
    }

    @NotNull
    private static ConfiguredItem simpleItem(@NotNull Material material, @NotNull String name) {
        return new ConfiguredItem(material, 1, name, List.of(), null, List.of(), List.of(), 0, false, null);
    }

    private void loadDefaults() {
        title = "<dark_gray>✦ <gold>Categories</gold> ✦</dark_gray>";
        rows = 3;
        pageSize = 18;
        updateInterval = 10;
        fillerEnabled = true;
        fillerItem = simpleItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        prevPage = new GuiConfig.NavItem(21, simpleItem(Material.ARROW, "<yellow>◀ Previous Page</yellow>"));
        nextPage = new GuiConfig.NavItem(23, simpleItem(Material.ARROW, "<yellow>Next Page ▶</yellow>"));
        close = new GuiConfig.NavItem(22, simpleItem(Material.BARRIER, "<red>✖ Close</red>"));
        currentTag = new GuiConfig.CurrentTagItem(18,
                simpleItem(Material.NAME_TAG, "<yellow>Current Tag</yellow>"),
                simpleItem(Material.PAPER, "<yellow>Current Tag</yellow>"));
        navFillerEnabled = true;
        navFillerSlots = List.of(19, 20, 24, 25, 26);
        navFillerItem = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        tagsTitle = "<dark_gray>✦ <gold>{category_name}</gold> Tags ✦</dark_gray>";
        backButton = new GuiConfig.NavItem(47, simpleItem(Material.ARROW, "<yellow>◀ Back to Categories</yellow>"));
    }
}
