package gg.gianluca.giantags.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import gg.gianluca.giantags.api.GianTagsAPI;
import gg.gianluca.giantags.api.GianTagsProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class ReloadSubCommand {

    @NotNull
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reload")
                .requires(src -> src.getSender().hasPermission("giantags.commands.reload"))
                .executes(this::execute);
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        GianTagsAPI api = GianTagsProvider.get();

        try {
            api.reload();
            sender.sendMessage(api.getMessagesConfig().get("reload-success"));
        } catch (Exception e) {
            sender.sendMessage(api.getMessagesConfig().get("reload-failed"));
            throw e;
        }

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
