package gg.gianluca.giantags.api.event;

import gg.gianluca.giantags.api.model.Tag;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when an admin (or console) forcibly sets a player's tag via command.
 * Cancelling this event prevents the tag from being applied.
 */
public final class TagSetEvent extends AbstractTagEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CommandSender executor;
    private final Player target;
    private final Tag tag;
    private final @Nullable Tag previousTag;
    private final boolean permissionBypassed;
    private boolean cancelled = false;

    public TagSetEvent(
            @NotNull CommandSender executor,
            @NotNull Player target,
            @NotNull Tag tag,
            @Nullable Tag previousTag,
            boolean permissionBypassed
    ) {
        this.executor = executor;
        this.target = target;
        this.tag = tag;
        this.previousTag = previousTag;
        this.permissionBypassed = permissionBypassed;
    }

    /** The command sender who issued the set command. */
    @NotNull
    public CommandSender getExecutor() {
        return executor;
    }

    /** The player whose tag is being set. */
    @NotNull
    public Player getTarget() {
        return target;
    }

    /** The tag being assigned. */
    @NotNull
    public Tag getTag() {
        return tag;
    }

    /** The player's previously active tag, or {@code null}. */
    @Nullable
    public Tag getPreviousTag() {
        return previousTag;
    }

    /** Whether the {@code --ignorePermission} flag was used. */
    public boolean isPermissionBypassed() {
        return permissionBypassed;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
