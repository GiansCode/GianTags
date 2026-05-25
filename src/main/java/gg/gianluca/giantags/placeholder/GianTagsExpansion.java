package gg.gianluca.giantags.placeholder;

import gg.gianluca.giantags.GianTags;
import gg.gianluca.giantags.api.GianTagsAPI;
import gg.gianluca.giantags.api.GianTagsProvider;
import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.util.TextUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for GianTags.
 *
 * <pre>
 * %giantags_tag%              → Formatted tag string (legacy colours for compatibility)
 * %giantags_tag_id%           → Tag identifier string
 * %giantags_tag_raw%          → Raw MiniMessage tag string
 * %giantags_tag_plain%        → Plain text tag (no formatting codes)
 * %giantags_has_tag%          → true / false
 * %giantags_has_tag_<id>%     → true / false — whether the player has a specific tag active
 * %giantags_can_use_<id>%     → true / false — whether the player has permission for the tag
 * %giantags_tag_count%        → Number of tags available to the player
 * %giantags_total_tags%       → Total number of registered tags
 * </pre>
 */
public final class GianTagsExpansion extends PlaceholderExpansion {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final GianTags plugin;

    public GianTagsExpansion(@NotNull GianTags plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "giantags";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "GianLuca";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        GianTagsAPI api = GianTagsProvider.getOrNull();
        if (api == null) return "";

        String lower = params.toLowerCase();

        // ── has_tag_<id> ──────────────────────────────────────────────────────
        if (lower.startsWith("has_tag_")) {
            String id = params.substring(8);
            return api.getPlayerTagId(offlinePlayer.getUniqueId())
                    .map(tid -> String.valueOf(tid.equalsIgnoreCase(id)))
                    .orElse("false");
        }

        // ── can_use_<id> (online only) ────────────────────────────────────────
        if (lower.startsWith("can_use_")) {
            String id = params.substring(8);
            Tag tag = api.getTag(id).orElse(null);
            if (tag == null) return "false";
            if (offlinePlayer instanceof Player player) {
                return String.valueOf(player.hasPermission(tag.getPermission()));
            }
            return "false";
        }

        return switch (lower) {
            case "tag" -> {
                yield api.getPlayerTag(offlinePlayer.getUniqueId())
                        .map(tag -> LEGACY.serialize(TextUtil.parse(tag.getRawTag())))
                        .orElse("");
            }
            case "tag_id" -> api.getPlayerTagId(offlinePlayer.getUniqueId()).orElse("");
            case "tag_raw" -> api.getPlayerTag(offlinePlayer.getUniqueId())
                    .map(Tag::getRawTag)
                    .orElse("");
            case "tag_plain" -> api.getPlayerTag(offlinePlayer.getUniqueId())
                    .map(tag -> TextUtil.toPlain(TextUtil.parse(tag.getRawTag())))
                    .orElse("");
            case "has_tag" -> String.valueOf(api.getPlayerTagId(offlinePlayer.getUniqueId()).isPresent());
            case "tag_count" -> {
                if (offlinePlayer instanceof Player player) {
                    yield String.valueOf(api.getTagsForPlayer(player).size());
                }
                yield "0";
            }
            case "total_tags" -> String.valueOf(api.getAllTags().size());
            default -> null;
        };
    }
}
