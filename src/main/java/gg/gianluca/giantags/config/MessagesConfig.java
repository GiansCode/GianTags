package gg.gianluca.giantags.config;

import gg.gianluca.giantags.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed wrapper for {@code messages.yml}.
 *
 * <p>All messages are pre-parsed into Components on load for maximum performance.
 * Use {@link #get(String, String...)} to retrieve with placeholder substitution.
 */
public final class MessagesConfig {

    private String rawPrefix = "<dark_gray>[<gold>GianTags</gold>]</dark_gray> ";
    private final Map<String, String> rawMessages = new HashMap<>();

    public void load(@NotNull FileConfiguration config) {
        rawPrefix = config.getString("messages.prefix", rawPrefix);
        rawMessages.clear();

        FileConfiguration root = config;
        var messagesSection = root.getConfigurationSection("messages");
        if (messagesSection == null) return;

        for (String key : messagesSection.getKeys(false)) {
            if (key.equals("prefix")) continue;
            String value = messagesSection.getString(key);
            if (value != null) {
                rawMessages.put(key, value);
            }
        }
    }

    /**
     * Returns the parsed Component for the given message key.
     * Replaces {@code {prefix}} with the configured prefix automatically.
     *
     * @param key          the message key (e.g. {@code "tag-selected"})
     * @param replacements optional placeholder pairs: key1, value1, key2, value2 …
     */
    @NotNull
    public Component get(@NotNull String key, String... replacements) {
        String raw = rawMessages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
        raw = raw.replace("{prefix}", rawPrefix);
        return TextUtil.parse(raw, replacements);
    }

    /**
     * Returns the prefix Component.
     */
    @NotNull
    public Component prefix() {
        return TextUtil.parse(rawPrefix);
    }

    /**
     * Returns the raw (unparsed) MiniMessage string for a given key, or empty string.
     */
    @NotNull
    public String getRaw(@NotNull String key) {
        String raw = rawMessages.getOrDefault(key, "");
        return raw.replace("{prefix}", rawPrefix);
    }
}
