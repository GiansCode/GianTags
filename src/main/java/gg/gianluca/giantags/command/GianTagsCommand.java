package gg.gianluca.giantags.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import gg.gianluca.giantags.api.GianTagsAPI;
import gg.gianluca.giantags.api.GianTagsProvider;
import gg.gianluca.giantags.command.sub.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Brigadier command tree for {@code /giantags} (and its aliases).
 *
 * <p>All subcommand instances are stateless — they resolve plugin state
 * lazily via {@link GianTagsProvider#get()} at execution time, so the
 * tree can be built safely during the bootstrap phase before the plugin
 * has finished enabling.
 */
@SuppressWarnings("UnstableApiUsage")
public final class GianTagsCommand {

    private GianTagsCommand() {}

    /**
     * Builds and returns the root Brigadier command node.
     * Safe to call from the plugin bootstrapper.
     */
    @NotNull
    public static LiteralCommandNode<CommandSourceStack> buildNode() {
        RemoveSubCommand removeSubCommand = new RemoveSubCommand();

        return Commands.literal("giantags")
                // Running /giantags with no arguments opens the GUI for players
                .executes(GianTagsCommand::executeRoot)
                .then(Commands.literal("all").executes(GianTagsCommand::executeAll))
                // /giantags <category> — open tags GUI for a specific category
                .then(Commands.argument("category", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            GianTagsAPI api = GianTagsProvider.getOrNull();
                            if (api != null) {
                                String partial = builder.getRemainingLowerCase();
                                for (String cat : api.getCategoryIds()) {
                                    if (cat.startsWith(partial)) builder.suggest(cat);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(GianTagsCommand::executeCategory))
                .then(new ListSubCommand().build())
                .then(new SetSubCommand().build())
                .then(removeSubCommand.build("remove"))
                .then(removeSubCommand.build("reset"))
                .then(new GetSubCommand().build())
                .then(new ReloadSubCommand().build())
                .build();
    }

    // ── Root executor (opens GUI) ─────────────────────────────────────────────

    private static int executeCategory(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "Usage: /giantags <category>", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            return Command.SINGLE_SUCCESS;
        }

        if (!player.hasPermission("giantags.gui")) {
            player.sendMessage(GianTagsProvider.get().getMessagesConfig().get("no-permission"));
            return Command.SINGLE_SUCCESS;
        }

        String category = StringArgumentType.getString(ctx, "category");
        GianTagsProvider.get().openTagsGuiForCategory(player, category);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeAll(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "Usage: /giantags all", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            return Command.SINGLE_SUCCESS;
        }

        if (!player.hasPermission("giantags.gui")) {
            GianTagsAPI api = GianTagsProvider.get();
            player.sendMessage(api.getMessagesConfig().get("no-permission"));
            return Command.SINGLE_SUCCESS;
        }

        GianTagsProvider.get().openTagsGuiAll(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeRoot(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            // Console: print usage
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "Usage: /giantags <list|set|remove|reset|get|reload>",
                    net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            return Command.SINGLE_SUCCESS;
        }

        if (!player.hasPermission("giantags.gui")) {
            GianTagsAPI api = GianTagsProvider.get();
            player.sendMessage(api.getMessagesConfig().get("no-permission"));
            return Command.SINGLE_SUCCESS;
        }

        GianTagsProvider.get().openTagsGui(player);
        return Command.SINGLE_SUCCESS;
    }
}
