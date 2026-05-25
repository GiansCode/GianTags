package gg.gianluca.giantags.api.event;

import gg.gianluca.giantags.api.model.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player selects a tag via the GUI.
 * Cancelling this event prevents the tag from being applied.
 */
public final class TagSelectEvent extends AbstractTagEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Tag tag;
    private final @Nullable Tag previousTag;
    private boolean cancelled = false;

    public TagSelectEvent(
            @NotNull Player player,
            @NotNull Tag tag,
            @Nullable Tag previousTag
    ) {
        this.player = player;
        this.tag = tag;
        this.previousTag = previousTag;
    }

    /** The player selecting the tag. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** The tag being selected. */
    @NotNull
    public Tag getTag() {
        return tag;
    }

    /** The player's previously active tag, or {@code null} if they had none. */
    @Nullable
    public Tag getPreviousTag() {
        return previousTag;
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
