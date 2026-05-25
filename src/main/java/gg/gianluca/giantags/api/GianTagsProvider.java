package gg.gianluca.giantags.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static accessor for the {@link GianTagsAPI}.
 *
 * <p>The API is available from plugin {@code onEnable} until {@code onDisable}.
 * Calling {@link #get()} outside that window throws {@link IllegalStateException}.
 */
public final class GianTagsProvider {

    @Nullable
    private static volatile GianTagsAPI instance;

    private GianTagsProvider() {}

    /**
     * Returns the active {@link GianTagsAPI} instance.
     *
     * @throws IllegalStateException if GianTags is not currently enabled
     */
    @NotNull
    public static GianTagsAPI get() {
        GianTagsAPI api = instance;
        if (api == null) {
            throw new IllegalStateException("GianTagsAPI is not available — plugin is not enabled.");
        }
        return api;
    }

    /**
     * Returns the API instance, or {@code null} if the plugin is not enabled.
     */
    @Nullable
    public static GianTagsAPI getOrNull() {
        return instance;
    }

    /**
     * Returns {@code true} if the API is currently available.
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    public static void initialize(@NotNull GianTagsAPI api) {
        if (instance != null) {
            throw new IllegalStateException("GianTagsAPI is already initialized.");
        }
        instance = api;
    }

    public static void uninitialize() {
        instance = null;
    }
}
