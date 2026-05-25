package gg.gianluca.giantags.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all GianTags events.
 */
public abstract class AbstractTagEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    protected AbstractTagEvent() {
        super(false);
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
