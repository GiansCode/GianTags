package gg.gianluca.giantags.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for MiniMessage / Adventure text operations.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private TextUtil() {}

    /**
     * Parses a MiniMessage string into a Component.
     */
    @NotNull
    public static Component parse(@Nullable String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * Parses a MiniMessage string with additional tag resolvers.
     */
    @NotNull
    public static Component parse(@Nullable String text, TagResolver... resolvers) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(text, resolvers);
    }

    /**
     * Parses a MiniMessage string replacing {key} style placeholders.
     * Placeholders are passed as pairs: key1, value1, key2, value2, ...
     */
    @NotNull
    public static Component parse(@Nullable String text, String... replacements) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (replacements.length == 0) return MINI_MESSAGE.deserialize(text);

        String replaced = text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            replaced = replaced.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return MINI_MESSAGE.deserialize(replaced);
    }

    /**
     * Parses a list of MiniMessage strings into a list of Components.
     */
    @NotNull
    public static List<Component> parseList(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(parse(line));
        }
        return components;
    }

    /**
     * Parses a list of MiniMessage strings with {key} placeholders.
     * Placeholders are passed as pairs: key1, value1, key2, value2, ...
     */
    @NotNull
    public static List<Component> parseList(@Nullable List<String> lines, String... replacements) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(parse(line, replacements));
        }
        return components;
    }

    /**
     * Converts a Component to its plain text representation.
     */
    @NotNull
    public static String toPlain(@NotNull Component component) {
        return PLAIN.serialize(component);
    }

    /**
     * Serialises a Component back to a MiniMessage string.
     */
    @NotNull
    public static String serialize(@NotNull Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Returns the shared MiniMessage instance.
     */
    @NotNull
    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    /**
     * Creates a Placeholder TagResolver for use with MiniMessage.
     */
    @NotNull
    public static TagResolver placeholder(@NotNull String key, @NotNull Component value) {
        return Placeholder.component(key, value);
    }

    /**
     * Creates a Placeholder TagResolver (string value parsed as MiniMessage).
     */
    @NotNull
    public static TagResolver placeholder(@NotNull String key, @NotNull String value) {
        return Placeholder.parsed(key, value);
    }
}
