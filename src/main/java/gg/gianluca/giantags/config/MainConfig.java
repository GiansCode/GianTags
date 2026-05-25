package gg.gianluca.giantags.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Typed wrapper for {@code config.yml}.
 */
public final class MainConfig {

    private List<String> commandAliases;

    public MainConfig(@NotNull FileConfiguration config) {
        load(config);
    }

    public void load(@NotNull FileConfiguration config) {
        commandAliases = config.getStringList("command-aliases");
        if (commandAliases.isEmpty()) {
            commandAliases = List.of("tags", "tag");
        }
    }

    /** Aliases registered as full server commands that open the tags GUI. */
    @NotNull
    public List<String> getCommandAliases() {
        return commandAliases;
    }
}
