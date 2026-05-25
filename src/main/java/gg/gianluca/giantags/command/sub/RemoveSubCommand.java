package gg.gianluca.giantags.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import gg.gianluca.giantags.api.GianTagsAPI;
import gg.gianluca.giantags.api.GianTagsProvider;
import gg.gianluca.giantags.config.MessagesConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class RemoveSubCommand {

    /** Builds a node for the given literal name (supports both "remove" and "reset"). */
    @NotNull
    public LiteralArgumentBuilder<CommandSourceStack> build(@NotNull String literal) {
        return Commands.literal(literal)
                .requires(src -> src.getSender().hasPermission("giantags.commands.remove"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                );
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        GianTagsAPI api = GianTagsProvider.get();
        MessagesConfig msg = api.getMessagesConfig();

        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(msg.get("player-not-found", "player", playerName));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        api.clearPlayerTag(target.getUniqueId());
        sender.sendMessage(msg.get("tag-removed", "player", target.getName()));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
