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
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public final class ListSubCommand {

    @NotNull
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("list")
                .requires(src -> src.getSender().hasPermission("giantags.commands.list"))
                .executes(this::executeSelf)
                .then(Commands.argument("player", StringArgumentType.word())
                        .requires(src -> src.getSender().hasPermission("giantags.commands.list.others"))
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .executes(this::executeOther));
    }

    private int executeSelf(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        GianTagsAPI api = GianTagsProvider.get();
        MessagesConfig msg = api.getMessagesConfig();

        if (sender instanceof Player player) {
            Collection<Tag> tags = api.getTagsForPlayer(player);
            Optional<Tag> activeTag = api.getPlayerTag(player.getUniqueId());
            sendList(sender, msg, tags, activeTag.orElse(null));
        } else {
            // Console: show all tags
            Collection<Tag> tags = api.getAllTags();
            sendList(sender, msg, tags, null);
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

        Collection<Tag> tags = api.getTagsForPlayer(target);
        Optional<Tag> activeTag = api.getPlayerTag(target.getUniqueId());

        sender.sendMessage(msg.get("list-header-player",
                "player", target.getName(),
                "count", String.valueOf(tags.size())));

        if (tags.isEmpty()) {
            sender.sendMessage(msg.get("list-empty"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        for (Tag tag : tags) {
            boolean active = activeTag.map(t -> t.getId().equals(tag.getId())).orElse(false);
            if (active) {
                sender.sendMessage(msg.get("list-entry-active",
                        "tag_id", tag.getId(),
                        "tag", tag.getRawTag()));
            } else {
                sender.sendMessage(msg.get("list-entry",
                        "tag_id", tag.getId(),
                        "tag", tag.getRawTag()));
            }
        }

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private void sendList(
            @NotNull CommandSender sender,
            @NotNull MessagesConfig msg,
            @NotNull Collection<Tag> tags,
            @org.jetbrains.annotations.Nullable Tag activeTag
    ) {
        sender.sendMessage(msg.get("list-header", "count", String.valueOf(tags.size())));

        if (tags.isEmpty()) {
            sender.sendMessage(msg.get("list-empty"));
            return;
        }

        for (Tag tag : tags) {
            boolean active = activeTag != null && activeTag.getId().equals(tag.getId());
            if (active) {
                sender.sendMessage(msg.get("list-entry-active",
                        "tag_id", tag.getId(),
                        "tag", tag.getRawTag()));
            } else {
                sender.sendMessage(msg.get("list-entry",
                        "tag_id", tag.getId(),
                        "tag", tag.getRawTag()));
            }
        }
    }
}
