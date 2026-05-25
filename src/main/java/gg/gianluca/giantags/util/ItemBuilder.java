package gg.gianluca.giantags.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Fluent builder for creating {@link ItemStack} instances.
 */
public final class ItemBuilder {

    private static final Logger LOGGER = Logger.getLogger("GianTags");

    private final Material material;
    private int amount = 1;
    private @Nullable Component name;
    private final List<Component> lore = new ArrayList<>();
    private boolean glow = false;
    private int customModelData = 0;
    private final List<ItemFlag> flags = new ArrayList<>();
    private final List<EnchantEntry> enchantments = new ArrayList<>();
    private @Nullable String skullTexture;

    private record EnchantEntry(Enchantment enchantment, int level) {}

    private ItemBuilder(@NotNull Material material) {
        this.material = material;
    }

    @NotNull
    public static ItemBuilder of(@NotNull Material material) {
        return new ItemBuilder(material);
    }

    @NotNull
    public static ItemBuilder fromString(@NotNull String materialName) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown material '" + materialName + "', defaulting to STONE.");
            material = Material.STONE;
        }
        return new ItemBuilder(material);
    }

    @NotNull
    public ItemBuilder amount(int amount) {
        this.amount = Math.max(1, Math.min(64, amount));
        return this;
    }

    @NotNull
    public ItemBuilder name(@Nullable Component name) {
        this.name = name;
        return this;
    }

    @NotNull
    public ItemBuilder name(@Nullable String miniMessageText) {
        this.name = miniMessageText == null ? null : TextUtil.parse(miniMessageText);
        return this;
    }

    @NotNull
    public ItemBuilder lore(@NotNull List<Component> lore) {
        this.lore.clear();
        this.lore.addAll(lore);
        return this;
    }

    @NotNull
    public ItemBuilder lore(@NotNull Component... lines) {
        this.lore.clear();
        this.lore.addAll(Arrays.asList(lines));
        return this;
    }

    @NotNull
    public ItemBuilder addLoreLine(@NotNull Component line) {
        this.lore.add(line);
        return this;
    }

    @NotNull
    public ItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    @NotNull
    public ItemBuilder customModelData(int data) {
        this.customModelData = data;
        return this;
    }

    @NotNull
    public ItemBuilder flags(@NotNull ItemFlag... flags) {
        this.flags.addAll(Arrays.asList(flags));
        return this;
    }

    @NotNull
    public ItemBuilder flags(@NotNull List<ItemFlag> flags) {
        this.flags.addAll(flags);
        return this;
    }

    /**
     * Adds an enchantment. Accepts formats: "sharpness:5", "minecraft:sharpness:5"
     */
    @NotNull
    public ItemBuilder enchant(@NotNull String enchantString) {
        String[] parts = enchantString.split(":");
        if (parts.length < 1) return this;

        String key;
        int level = 1;

        if (parts.length >= 3) {
            // minecraft:sharpness:5
            key = parts[0] + ":" + parts[1];
            try { level = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
        } else if (parts.length == 2) {
            // sharpness:5
            key = parts[0];
            try { level = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) { key = enchantString; }
        } else {
            key = parts[0];
        }

        Enchantment enchant = resolveEnchantment(key);
        if (enchant != null) {
            enchantments.add(new EnchantEntry(enchant, level));
        }
        return this;
    }

    @NotNull
    public ItemBuilder enchant(@NotNull List<String> enchantStrings) {
        enchantStrings.forEach(this::enchant);
        return this;
    }

    @NotNull
    public ItemBuilder skullTexture(@Nullable String base64Texture) {
        this.skullTexture = base64Texture;
        return this;
    }

    @NotNull
    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null) {
            meta.displayName(name);
        }

        if (!lore.isEmpty()) {
            meta.lore(lore);
        }

        if (customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }

        if (glow && enchantments.isEmpty()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        for (EnchantEntry entry : enchantments) {
            meta.addEnchant(entry.enchantment(), entry.level(), true);
        }

        if (!flags.isEmpty()) {
            meta.addItemFlags(flags.toArray(new ItemFlag[0]));
        }

        if (skullTexture != null && !skullTexture.isBlank() && meta instanceof SkullMeta skullMeta) {
            applySkullTexture(skullMeta, skullTexture);
        }

        item.setItemMeta(meta);
        return item;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Maps legacy Bukkit / pre-1.21 enchantment field names → modern minecraft: key.
     * Allows configs written for older servers to work without any changes.
     */
    private static final Map<String, String> LEGACY_ENCHANT_MAP = Map.ofEntries(
            // Sword
            Map.entry("damage_all",          "sharpness"),
            Map.entry("damage_undead",        "smite"),
            Map.entry("damage_arthropods",    "bane_of_arthropods"),
            Map.entry("fire_aspect",          "fire_aspect"),
            Map.entry("loot_bonus_mobs",      "looting"),
            Map.entry("knockback",            "knockback"),
            Map.entry("sweeping",             "sweeping_edge"),
            // Tool
            Map.entry("dig_speed",            "efficiency"),
            Map.entry("loot_bonus_blocks",    "fortune"),
            Map.entry("silk_touch",           "silk_touch"),
            // Universal
            Map.entry("durability",           "unbreaking"),
            Map.entry("protection_environmental", "protection"),
            Map.entry("protection_fire",      "fire_protection"),
            Map.entry("protection_fall",      "feather_falling"),
            Map.entry("protection_explosions","blast_protection"),
            Map.entry("protection_projectile","projectile_protection"),
            Map.entry("thorns",               "thorns"),
            // Bow
            Map.entry("arrow_damage",         "power"),
            Map.entry("arrow_knockback",      "punch"),
            Map.entry("arrow_fire",           "flame"),
            Map.entry("arrow_infinite",       "infinity"),
            // Fishing / Helmet
            Map.entry("luck",                 "luck_of_the_sea"),
            Map.entry("water_worker",         "aqua_affinity"),
            Map.entry("oxygen",               "respiration"),
            // Mending / Curses
            Map.entry("mending",              "mending"),
            Map.entry("binding_curse",        "binding_curse"),
            Map.entry("vanishing_curse",      "vanishing_curse")
    );

    @Nullable
    private static Enchantment resolveEnchantment(@NotNull String key) {
        String cleaned = key.toLowerCase().trim();

        // Remap legacy names transparently
        String remapped = LEGACY_ENCHANT_MAP.get(cleaned);
        if (remapped != null) {
            cleaned = remapped;
        }

        // Ensure namespaced form (e.g. "sharpness" → "minecraft:sharpness")
        String namespacedKey = cleaned.contains(":") ? cleaned : "minecraft:" + cleaned;
        NamespacedKey nk = NamespacedKey.fromString(namespacedKey);
        if (nk != null) {
            Enchantment found = Registry.ENCHANTMENT.get(nk);
            if (found != null) return found;
        }

        // Last resort: scan the whole registry by key name
        for (Enchantment e : Registry.ENCHANTMENT) {
            if (e.getKey().getKey().equalsIgnoreCase(cleaned)) return e;
        }

        LOGGER.warning("Unknown enchantment: '" + key + "'. Skipping.");
        return null;
    }

    private static void applySkullTexture(@NotNull SkullMeta meta, @NotNull String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            // Extract URL from: {"textures":{"SKIN":{"url":"..."}}}
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd = decoded.indexOf("\"", urlStart);
            if (urlStart < 7 || urlEnd < 0) return;

            String url = decoded.substring(urlStart, urlEnd);
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(url).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException | IllegalArgumentException e) {
            LOGGER.warning("Failed to apply skull texture: " + e.getMessage());
        }
    }
}
