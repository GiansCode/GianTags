package gg.gianluca.giantags.storage;

import gg.gianluca.giantags.api.model.PlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Storage backend interface.
 *
 * <p>All methods may be called off the main thread.
 * Implementations are responsible for their own thread safety.
 */
public interface Storage {

    /**
     * Initialises the storage backend (create tables, connect pool, etc.).
     *
     * @throws StorageException if initialisation fails
     */
    void init() throws StorageException;

    /**
     * Loads data for the given player. Returns {@code null} if no record exists.
     *
     * @throws StorageException on I/O failure
     */
    @Nullable
    PlayerData loadPlayer(@NotNull UUID uuid) throws StorageException;

    /**
     * Persists data for the given player (insert or update).
     *
     * @throws StorageException on I/O failure
     */
    void savePlayer(@NotNull UUID uuid, @NotNull PlayerData data) throws StorageException;

    /**
     * Deletes the stored record for the given player, if any.
     *
     * @throws StorageException on I/O failure
     */
    void deletePlayer(@NotNull UUID uuid) throws StorageException;

    /**
     * Closes all resources held by this backend (connections, threads, etc.).
     */
    void close();

    /**
     * Checked exception thrown by storage operations.
     */
    final class StorageException extends Exception {
        public StorageException(@NotNull String message) { super(message); }
        public StorageException(@NotNull String message, @NotNull Throwable cause) { super(message, cause); }
    }
}
