package gg.gianluca.giantags.config;

import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.config.model.ConfiguredItem;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Typed wrapper for {@code tags.yml}.
 * Tags are indexed by their identifier and preserved in insertion order.
 */
public final class TagsConfig {

    private final Logger logger;
    private Map<String, Tag> tags = new LinkedHashMap<>();

    public TagsConfig(@NotNull Logger logger) {
        this.logger = logger;
    }

    public void load(@NotNull FileConfiguration config) {
        Map<String, Tag> loaded = new LinkedHashMap<>();
        ConfigurationSection tagsSection = config.getConfigurationSection("tags");
        if (tagsSection == null) {
            logger.warning("tags.yml has no 'tags' section — no tags loaded.");
            this.tags = loaded;
            return;
        }

        for (String id : tagsSection.getKeys(false)) {
            ConfigurationSection section = tagsSection.getConfigurationSection(id);
            if (section == null) continue;

            try {
                Tag tag = parseTag(id, section);
                loaded.put(id, tag);
            } catch (Exception e) {
                logger.warning("Failed to load tag '" + id + "': " + e.getMessage());
            }
        }

        this.tags = Collections.unmodifiableMap(loaded);
        logger.info("Loaded " + tags.size() + " tag(s).");
    }

    /** Returns an unmodifiable, insertion-ordered map of all loaded tags. */
    @NotNull
    public Map<String, Tag> getTags() {
        return tags;
    }

    // ── Parsing ──────────────────────────────────────────────────────────────

    @NotNull
    private Tag parseTag(@NotNull String id, @NotNull ConfigurationSection section) {
        String rawTag = section.getString("tag", "<gray>[" + id + "]</gray>");
        String rawDescription = section.getString("description", "");
        int position = section.getInt("position", Integer.MAX_VALUE);
        String category = section.getString("category");
        ConfiguredItem item = parseItem(id, section.getConfigurationSection("item"));
        return new Tag(id, rawTag, rawDescription, position, category, item);
    }

    /**
     * Returns all tags belonging to the given category, sorted by position.
     * Tags with no category are not included.
     */
    @NotNull
    public List<gg.gianluca.giantags.api.model.Tag> getTagsByCategory(@NotNull String category) {
        return tags.values().stream()
                .filter(t -> category.equals(t.getCategory()))
                .sorted(java.util.Comparator.comparingInt(gg.gianluca.giantags.api.model.Tag::getPosition))
                .toList();
    }

    @NotNull
    private ConfiguredItem parseItem(@NotNull String tagId, @org.jetbrains.annotations.Nullable ConfigurationSection sec) {
        if (sec == null) {
            return defaultItem(tagId);
        }

        Material material = parseMaterial(sec.getString("type", "PAPER"), tagId);
        int amount = Math.max(1, Math.min(64, sec.getInt("amount", 1)));
        String name = sec.getString("name");
        List<String> lore = sec.getStringList("lore");
        List<String> selectedLore = sec.contains("selected-lore") ? sec.getStringList("selected-lore") : null;
        List<String> enchantments = sec.getStringList("enchantments");
        List<ItemFlag> flags = parseFlags(sec.getStringList("flags"), tagId);
        int customModelData = sec.getInt("custom-model-data", 0);
        boolean glow = sec.getBoolean("glow", false);
        String skullTexture = sec.getString("skull-texture");

        return new ConfiguredItem(material, amount, name, lore, selectedLore, enchantments, flags, customModelData, glow, skullTexture);
    }

    @NotNull
    private ConfiguredItem defaultItem(@NotNull String tagId) {
        return new ConfiguredItem(Material.PAPER, 1,
                "<gray>[" + tagId + "]</gray>",
                List.of(), null, List.of(), List.of(),
                0, false, null);
    }

    @NotNull
    private Material parseMaterial(@NotNull String name, @NotNull String tagId) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown material '" + name + "' in tag '" + tagId + "', defaulting to PAPER.");
            return Material.PAPER;
        }
    }

    @NotNull
    private List<ItemFlag> parseFlags(@NotNull List<String> raw, @NotNull String tagId) {
        List<ItemFlag> flags = new java.util.ArrayList<>();
        for (String s : raw) {
            try {
                flags.add(ItemFlag.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown ItemFlag '" + s + "' in tag '" + tagId + "'.");
            }
        }
        return flags;
    }
}
