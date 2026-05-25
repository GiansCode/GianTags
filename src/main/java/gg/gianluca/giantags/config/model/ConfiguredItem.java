package gg.gianluca.giantags.config.model;

import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.util.ItemBuilder;
import gg.gianluca.giantags.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents item meta loaded from a YAML configuration block.
 * Immutable — create a new instance on config reload.
 */
public final class ConfiguredItem {

    private final Material material;
    private final int amount;
    private final @Nullable String name;
    private final List<String> lore;
    private final @Nullable List<String> selectedLore;
    private final List<String> enchantments;
    private final List<ItemFlag> flags;
    private final int customModelData;
    private final boolean glow;
    private final @Nullable String skullTexture;

    public ConfiguredItem(
            @NotNull Material material,
            int amount,
            @Nullable String name,
            @NotNull List<String> lore,
            @Nullable List<String> selectedLore,
            @NotNull List<String> enchantments,
            @NotNull List<ItemFlag> flags,
            int customModelData,
            boolean glow,
            @Nullable String skullTexture
    ) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = List.copyOf(lore);
        this.selectedLore = selectedLore == null ? null : List.copyOf(selectedLore);
        this.enchantments = List.copyOf(enchantments);
        this.flags = List.copyOf(flags);
        this.customModelData = customModelData;
        this.glow = glow;
        this.skullTexture = skullTexture;
    }

    /**
     * Builds a GUI ItemStack for a tag item.
     * Supports {tag}, {tag_id}, {player}, {player_tag} placeholders.
     */
    @NotNull
    public ItemStack buildItem(@NotNull Player player, @NotNull Tag tag, boolean selected) {
        String playerTagRaw = resolvePlayerTag(player);
        String[] replacements = {
                "tag", tag.getRawTag(),
                "tag_id", tag.getId(),
                "player", player.getName(),
                "player_tag", playerTagRaw
        };

        List<String> loreSource = (selected && selectedLore != null) ? selectedLore : lore;

        ItemBuilder builder = ItemBuilder.of(material)
                .amount(amount)
                .glow(glow)
                .customModelData(customModelData)
                .flags(flags)
                .enchant(enchantments)
                .skullTexture(skullTexture);

        if (name != null) {
            builder.name(applyReplacements(name, replacements));
        }

        List<Component> loreComponents = new ArrayList<>(loreSource.size());
        for (String line : loreSource) {
            loreComponents.add(TextUtil.parse(applyReplacements(line, replacements)));
        }
        builder.lore(loreComponents);

        return builder.build();
    }

    /**
     * Builds a plain ItemStack with configurable placeholder replacements.
     * Pass placeholder pairs as varargs: key1, value1, key2, value2, ...
     */
    @NotNull
    public ItemStack buildPlain(String... replacements) {
        ItemBuilder builder = ItemBuilder.of(material)
                .amount(amount)
                .glow(glow)
                .customModelData(customModelData)
                .flags(flags)
                .enchant(enchantments)
                .skullTexture(skullTexture);

        if (name != null) {
            builder.name(applyReplacements(name, replacements));
        }

        List<Component> loreComponents = new ArrayList<>(lore.size());
        for (String line : lore) {
            loreComponents.add(TextUtil.parse(applyReplacements(line, replacements)));
        }
        builder.lore(loreComponents);

        return builder.build();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    @NotNull public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    @Nullable public String getName() { return name; }
    @NotNull public List<String> getLore() { return lore; }
    @Nullable public List<String> getSelectedLore() { return selectedLore; }
    @NotNull public List<String> getEnchantments() { return enchantments; }
    @NotNull public List<ItemFlag> getFlags() { return flags; }
    public int getCustomModelData() { return customModelData; }
    public boolean isGlow() { return glow; }
    @Nullable public String getSkullTexture() { return skullTexture; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @NotNull
    private static String applyReplacements(@NotNull String text, @NotNull String[] pairs) {
        String result = text;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result = result.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return result;
    }

    @NotNull
    private static String resolvePlayerTag(@NotNull Player player) {
        var api = gg.gianluca.giantags.api.GianTagsProvider.getOrNull();
        if (api == null) return "None";
        return api.getPlayerTag(player.getUniqueId())
                .map(Tag::getRawTag)
                .orElse("None");
    }
}
