package gg.gianluca.giantags.command;

import gg.gianluca.giantags.api.GianTagsProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A dynamic alias command that opens the tags GUI.
 * Instances are registered programmatically via Bukkit's {@link org.bukkit.command.CommandMap}
 * at startup and on reload.
 */
public final class AliasCommand extends Command {

    private final JavaPlugin plugin;

    public AliasCommand(@NotNull String name, @NotNull JavaPlugin plugin) {
        super(name);
        this.plugin = plugin;
        setDescription("Open the GianTags tag selection GUI.");
        setPermission("giantags.gui");
        setPermissionMessage(null); // handled in execute
    }

    @Override
    public boolean execute(
            @NotNull CommandSender sender,
            @NotNull String commandLabel,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GianTagsProvider.get().getMessagesConfig().get("players-only"));
            return true;
        }

        if (!player.hasPermission("giantags.gui")) {
            player.sendMessage(GianTagsProvider.get().getMessagesConfig().get("no-permission"));
            return true;
        }

        GianTagsProvider.get().openTagsGui(player);
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(
            @NotNull CommandSender sender,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        return List.of();
    }
}
