package gg.gianluca.giantags.api.event;

import gg.gianluca.giantags.api.model.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player clears (deselects) their active tag via the GUI.
 * Cancelling this event prevents the tag from being removed.
 */
public final class TagClearEvent extends AbstractTagEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Tag clearedTag;
    private boolean cancelled = false;

    public TagClearEvent(@NotNull Player player, @NotNull Tag clearedTag) {
        this.player = player;
        this.clearedTag = clearedTag;
    }

    /** The player clearing their tag. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** The tag that is being removed. */
    @NotNull
    public Tag getClearedTag() {
        return clearedTag;
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
