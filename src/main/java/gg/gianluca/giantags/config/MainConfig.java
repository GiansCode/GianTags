package gg.gianluca.giantags.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Typed wrapper for {@code config.yml}.
 */
public final class MainConfig {

    private List<String> commandAliases;
    private boolean categoriesEnabled;

    public MainConfig(@NotNull FileConfiguration config) {
        load(config);
    }

    public void load(@NotNull FileConfiguration config) {
        commandAliases = config.getStringList("command-aliases");
        if (commandAliases.isEmpty()) {
            commandAliases = List.of("tags", "tag");
        }
        categoriesEnabled = config.getBoolean("categories.enabled", false);
    }

    /** Aliases registered as full server commands that open the tags GUI. */
    @NotNull
    public List<String> getCommandAliases() {
        return commandAliases;
    }

    /**
     * When {@code true}, {@code /tags} opens the category selection menu instead
     * of showing all tags directly. Players can use {@code /tags all} to bypass.
     */
    public boolean isCategoriesEnabled() {
        return categoriesEnabled;
    }
}
