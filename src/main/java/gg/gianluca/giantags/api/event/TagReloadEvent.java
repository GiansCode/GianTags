package gg.gianluca.giantags.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after GianTags has fully reloaded its configuration and data.
 * This event is not cancellable — reload has already completed when it fires.
 */
public final class TagReloadEvent extends AbstractTagEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public TagReloadEvent() {}

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
