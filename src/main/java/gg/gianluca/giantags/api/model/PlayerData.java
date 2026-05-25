package gg.gianluca.giantags.api.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Mutable data container for a player's GianTags state.
 * Always accessed and modified on the main thread (or under synchronisation).
 */
public final class PlayerData {

    private final UUID uuid;
    private @Nullable String tagId;

    public PlayerData(@NotNull UUID uuid, @Nullable String tagId) {
        this.uuid = uuid;
        this.tagId = tagId;
    }

    /** Creates a fresh PlayerData with no tag selected. */
    @NotNull
    public static PlayerData empty(@NotNull UUID uuid) {
        return new PlayerData(uuid, null);
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /** Returns the active tag identifier, or {@code null} if no tag is selected. */
    @Nullable
    public String getTagId() {
        return tagId;
    }

    /** Sets the active tag identifier. Pass {@code null} to clear. */
    public void setTagId(@Nullable String tagId) {
        this.tagId = tagId;
    }

    /** Returns {@code true} if the player has an active tag. */
    public boolean hasTag() {
        return tagId != null && !tagId.isBlank();
    }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid + ", tagId='" + tagId + "'}";
    }
}
