package gg.gianluca.giantags.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import gg.gianluca.giantags.api.GianTagsAPI;
import gg.gianluca.giantags.api.GianTagsProvider;
import gg.gianluca.giantags.api.model.Tag;
import gg.gianluca.giantags.config.MessagesConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public final class GetSubCommand {

    @NotNull
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("get")
                .requires(src -> src.getSender().hasPermission("giantags.commands.get"))
                .executes(this::executeSelf)
                .then(Commands.argument("player", StringArgumentType.word())
                        .requires(src -> src.getSender().hasPermission("giantags.commands.get.others"))
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .executes(this::executeOther)
                );
    }

    private int executeSelf(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GianTagsProvider.get().getMessagesConfig().get("players-only"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        GianTagsAPI api = GianTagsProvider.get();
        MessagesConfig msg = api.getMessagesConfig();
        Optional<Tag> tag = api.getPlayerTag(player.getUniqueId());

        if (tag.isPresent()) {
            player.sendMessage(msg.get("current-tag-self", "tag", tag.get().getRawTag()));
        } else {
            player.sendMessage(msg.get("no-current-tag-self"));
        }
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int executeOther(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        GianTagsAPI api = GianTagsProvider.get();
        MessagesConfig msg = api.getMessagesConfig();

        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(msg.get("player-not-found", "player", name));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        Optional<Tag> tag = api.getPlayerTag(target.getUniqueId());
        if (tag.isPresent()) {
            sender.sendMessage(msg.get("current-tag-other",
                    "player", target.getName(),
                    "tag", tag.get().getRawTag()));
        } else {
            sender.sendMessage(msg.get("no-current-tag-other", "player", target.getName()));
        }
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
