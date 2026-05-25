package gg.gianluca.giantags;

import gg.gianluca.giantags.command.GianTagsCommand;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs before the plugin is enabled to register Brigadier commands
 * with aliases sourced from the plugin's {@code config.yml}.
 *
 * <p>Using paper-plugin.yml + PluginBootstrap gives us first-class
 * Brigadier registration with proper client-side tab completion.
 */
@SuppressWarnings("UnstableApiUsage")
public final class GianTagsBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        // Read command aliases from config.yml before registration
        List<String> aliases = loadAliases(context.getDataDirectory());

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(
                    GianTagsCommand.buildNode(),
                    "GianTags — manage and select player tags",
                    aliases
            );
        });
    }

    /**
     * Reads command aliases from config.yml if it exists.
     * Falls back to sensible defaults so the server can boot without a config present.
     */
    @NotNull
    private List<String> loadAliases(@NotNull Path dataDir) {
        Path configPath = dataDir.resolve("config.yml");
        if (!Files.exists(configPath)) {
            return List.of("tags", "tag", "mytag");
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configPath.toFile());
            List<String> aliases = config.getStringList("command-aliases");
            return aliases.isEmpty() ? List.of("tags", "tag", "mytag") : aliases;
        } catch (Exception e) {
            return List.of("tags", "tag", "mytag");
        }
    }
}
