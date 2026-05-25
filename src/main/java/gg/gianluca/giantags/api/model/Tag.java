package gg.gianluca.giantags.api.model;

import gg.gianluca.giantags.config.model.ConfiguredItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an immutable tag definition loaded from tags.yml.
 */
public final class Tag {

    private final String id;
    private final String rawTag;
    private final String rawDescription;
    private final int position;
    private final ConfiguredItem item;

    public Tag(
            @NotNull String id,
            @NotNull String rawTag,
            @NotNull String rawDescription,
            int position,
            @NotNull ConfiguredItem item
    ) {
        this.id = id;
        this.rawTag = rawTag;
        this.rawDescription = rawDescription;
        this.position = position;
        this.item = item;
    }

    /** The unique identifier used in config and permissions. */
    @NotNull
    public String getId() {
        return id;
    }

    /** The raw MiniMessage string representing this tag (e.g. {@code "<gold>[VIP]</gold>"}). */
    @NotNull
    public String getRawTag() {
        return rawTag;
    }

    /** The raw MiniMessage description string. */
    @NotNull
    public String getRawDescription() {
        return rawDescription;
    }

    /** Parsed tag component. */
    @NotNull
    public Component getTagComponent() {
        return gg.gianluca.giantags.util.TextUtil.parse(rawTag);
    }

    /** Parsed description component. */
    @NotNull
    public Component getDescriptionComponent() {
        return gg.gianluca.giantags.util.TextUtil.parse(rawDescription);
    }

    /** The display position / sort order in the GUI (lower = earlier). */
    public int getPosition() {
        return position;
    }

    /** The item configuration used when rendering this tag in the GUI. */
    @NotNull
    public ConfiguredItem getItemConfig() {
        return item;
    }

    /**
     * Builds the GUI ItemStack for this tag.
     *
     * @param player     the player viewing the GUI
     * @param selected   whether this tag is the player's currently active tag
     */
    @NotNull
    public ItemStack buildGuiItem(@NotNull Player player, boolean selected) {
        return item.buildItem(player, this, selected);
    }

    /** Required Bukkit permission node for this tag. */
    @NotNull
    public String getPermission() {
        return "giantags.tag." + id;
    }

    /** Whether the given player has permission to use this tag. */
    public boolean hasPermission(@NotNull Player player) {
        return player.hasPermission(getPermission());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Tag other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Tag{id='" + id + "', position=" + position + '}';
    }
}
