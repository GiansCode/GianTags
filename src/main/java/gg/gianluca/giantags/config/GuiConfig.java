package gg.gianluca.giantags.config;

import gg.gianluca.giantags.config.model.ConfiguredItem;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

/**
 * Typed wrapper for {@code gui.yml}.
 */
public final class GuiConfig {

    private final Logger logger;

    private String title;
    private int rows;
    private int pageSize;
    private int updateInterval;

    private boolean fillerEnabled;
    private ConfiguredItem fillerItem;

    private NavItem prevPage;
    private NavItem nextPage;
    private NavItem close;
    private CurrentTagItem currentTag;

    private boolean navFillerEnabled;
    private List<Integer> navFillerSlots;
    private ConfiguredItem navFillerItem;

    public GuiConfig(@NotNull Logger logger) {
        this.logger = logger;
    }

    public void load(@NotNull FileConfiguration config) {
        ConfigurationSection gui = config.getConfigurationSection("gui");
        if (gui == null) {
            loadDefaults();
            return;
        }

        title = gui.getString("title", "<dark_gray>✦ <gold>Tags</gold> ✦</dark_gray>");
        rows = Math.max(1, Math.min(6, gui.getInt("rows", 6)));
        pageSize = Math.max(1, gui.getInt("page-size", 45));
        updateInterval = Math.max(1, gui.getInt("update-interval", 10));

        // Filler
        ConfigurationSection fillerSec = gui.getConfigurationSection("filler");
        fillerEnabled = fillerSec != null && fillerSec.getBoolean("enabled", true);
        fillerItem = fillerSec != null ? parseItem(fillerSec.getConfigurationSection("item"), Material.BLACK_STAINED_GLASS_PANE, " ") : simpleItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Navigation
        ConfigurationSection nav = gui.getConfigurationSection("navigation");
        prevPage = parseNavItem(nav, "previous-page", 48, Material.ARROW, "<yellow>◀ Previous Page</yellow>");
        nextPage = parseNavItem(nav, "next-page", 50, Material.ARROW, "<yellow>Next Page ▶</yellow>");
        close = parseNavItem(nav, "close", 49, Material.BARRIER, "<red>✖ Close</red>");
        currentTag = parseCurrentTagItem(nav);

        // Nav filler
        ConfigurationSection navFillerSec = nav != null ? nav.getConfigurationSection("nav-filler") : null;
        navFillerEnabled = navFillerSec != null && navFillerSec.getBoolean("enabled", true);
        navFillerSlots = navFillerSec != null ? navFillerSec.getIntegerList("slots") : List.of(46, 47, 51, 52, 53);
        navFillerItem = navFillerSec != null ? parseItem(navFillerSec.getConfigurationSection("item"), Material.GRAY_STAINED_GLASS_PANE, " ") : simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @NotNull public String getTitle() { return title; }
    public int getRows() { return rows; }
    public int getPageSize() { return pageSize; }
    public int getUpdateInterval() { return updateInterval; }
    public boolean isFillerEnabled() { return fillerEnabled; }
    @NotNull public ConfiguredItem getFillerItem() { return fillerItem; }
    @NotNull public NavItem getPrevPage() { return prevPage; }
    @NotNull public NavItem getNextPage() { return nextPage; }
    @NotNull public NavItem getClose() { return close; }
    @NotNull public CurrentTagItem getCurrentTag() { return currentTag; }
    public boolean isNavFillerEnabled() { return navFillerEnabled; }
    @NotNull public List<Integer> getNavFillerSlots() { return navFillerSlots; }
    @NotNull public ConfiguredItem getNavFillerItem() { return navFillerItem; }

    // ── Nested types ─────────────────────────────────────────────────────────

    public record NavItem(int slot, @NotNull ConfiguredItem item) {}

    public record CurrentTagItem(int slot, @NotNull ConfiguredItem hasTagItem, @NotNull ConfiguredItem noTagItem) {}

    // ── Parsing helpers ──────────────────────────────────────────────────────

    @NotNull
    private NavItem parseNavItem(
            @Nullable ConfigurationSection nav,
            @NotNull String key,
            int defaultSlot,
            @NotNull Material defaultMaterial,
            @NotNull String defaultName
    ) {
        if (nav == null) {
            return new NavItem(defaultSlot, simpleItem(defaultMaterial, defaultName));
        }
        ConfigurationSection sec = nav.getConfigurationSection(key);
        int slot = sec != null ? sec.getInt("slot", defaultSlot) : defaultSlot;
        ConfiguredItem item = sec != null ? parseItem(sec.getConfigurationSection("item"), defaultMaterial, defaultName) : simpleItem(defaultMaterial, defaultName);
        return new NavItem(slot, item);
    }

    @NotNull
    private CurrentTagItem parseCurrentTagItem(@Nullable ConfigurationSection nav) {
        int defaultSlot = 45;
        if (nav == null) {
            return new CurrentTagItem(defaultSlot,
                    simpleItem(Material.NAME_TAG, "<yellow>Current Tag</yellow>"),
                    simpleItem(Material.PAPER, "<yellow>Current Tag</yellow>"));
        }
        ConfigurationSection sec = nav.getConfigurationSection("current-tag");
        int slot = sec != null ? sec.getInt("slot", defaultSlot) : defaultSlot;
        ConfiguredItem hasTag = sec != null ? parseItem(sec.getConfigurationSection("has-tag"), Material.NAME_TAG, "<yellow>Current Tag</yellow>") : simpleItem(Material.NAME_TAG, "<yellow>Current Tag</yellow>");
        ConfiguredItem noTag = sec != null ? parseItem(sec.getConfigurationSection("no-tag"), Material.PAPER, "<yellow>Current Tag</yellow>") : simpleItem(Material.PAPER, "<yellow>Current Tag</yellow>");
        return new CurrentTagItem(slot, hasTag, noTag);
    }

    @NotNull
    private ConfiguredItem parseItem(@Nullable ConfigurationSection sec, @NotNull Material fallbackMaterial, @NotNull String fallbackName) {
        if (sec == null) return simpleItem(fallbackMaterial, fallbackName);

        Material material = fallbackMaterial;
        String matName = sec.getString("type");
        if (matName != null) {
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown material '" + matName + "' in gui.yml, using " + fallbackMaterial);
            }
        }

        int amount = Math.max(1, Math.min(64, sec.getInt("amount", 1)));
        String name = sec.getString("name", fallbackName);
        List<String> lore = sec.getStringList("lore");
        List<String> enchantments = sec.getStringList("enchantments");
        List<ItemFlag> flags = new java.util.ArrayList<>();
        for (String f : sec.getStringList("flags")) {
            try { flags.add(ItemFlag.valueOf(f.toUpperCase())); }
            catch (IllegalArgumentException ex) { logger.warning("Unknown ItemFlag: " + f); }
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
        title = "<dark_gray>✦ <gold>Tags</gold> ✦</dark_gray>";
        rows = 6;
        pageSize = 45;
        updateInterval = 10;
        fillerEnabled = true;
        fillerItem = simpleItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        prevPage = new NavItem(48, simpleItem(Material.ARROW, "<yellow>◀ Previous Page</yellow>"));
        nextPage = new NavItem(50, simpleItem(Material.ARROW, "<yellow>Next Page ▶</yellow>"));
        close = new NavItem(49, simpleItem(Material.BARRIER, "<red>✖ Close</red>"));
        currentTag = new CurrentTagItem(45,
                simpleItem(Material.NAME_TAG, "<yellow>Current Tag</yellow>"),
                simpleItem(Material.PAPER, "<yellow>Current Tag</yellow>"));
        navFillerEnabled = true;
        navFillerSlots = List.of(46, 47, 51, 52, 53);
        navFillerItem = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
}
