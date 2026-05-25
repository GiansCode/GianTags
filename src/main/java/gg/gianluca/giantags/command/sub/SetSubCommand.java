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
public final class SetSubCommand {

    @NotNull
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("set")
                .requires(src -> src.getSender().hasPermission("giantags.commands.set"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    var api = GianTagsProvider.getOrNull();
                                    if (api != null) api.getAllTags().forEach(t -> builder.suggest(t.getId()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeSet(ctx, false))
                                .then(Commands.literal("--ignorePermission")
                                        .requires(src -> src.getSender().hasPermission("giantags.commands.set.bypass"))
                                        .executes(ctx -> executeSet(ctx, true))
                                )
                        )
                );
    }

    private int executeSet(@NotNull CommandContext<CommandSourceStack> ctx, boolean ignorePermission) {
        CommandSender sender = ctx.getSource().getSender();
        GianTagsAPI api = GianTagsProvider.get();
        MessagesConfig msg = api.getMessagesConfig();

        String playerName = StringArgumentType.getString(ctx, "player");
        String tagId = StringArgumentType.getString(ctx, "tag");

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(msg.get("player-not-found", "player", playerName));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        if (api.getTag(tagId).isEmpty()) {
            sender.sendMessage(msg.get("tag-not-found", "tag", tagId));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        boolean success = api.setPlayerTag(sender, target.getUniqueId(), tagId, ignorePermission);

        if (success) {
            String rawTag = api.getTag(tagId).map(t -> t.getRawTag()).orElse(tagId);
            sender.sendMessage(msg.get("tag-set", "player", target.getName(), "tag", rawTag));
        } else {
            sender.sendMessage(msg.get("no-permission-tag", "player", target.getName(), "tag", tagId));
        }

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
