package gg.gianluca.giantags.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.gianluca.giantags.api.model.PlayerData;
import gg.gianluca.giantags.config.StorageConfig;
import gg.gianluca.giantags.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL / MariaDB storage backend using HikariCP.
 */
public final class SqlStorage implements Storage {

    private final StorageConfig config;
    private final Logger logger;
    private HikariDataSource dataSource;
    private String tablePrefix;

    public SqlStorage(@NotNull StorageConfig config, @NotNull Logger logger) {
        this.config = config;
        this.logger = logger;
        this.tablePrefix = config.getTablePrefix();
    }

    @Override
    public void init() throws StorageException {
        tablePrefix = config.getTablePrefix();

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(String.format("jdbc:mariadb://%s:%d/%s?useSSL=false&characterEncoding=utf8",
                config.getMysqlHost(), config.getMysqlPort(), config.getMysqlDatabase()));
        hikari.setUsername(config.getMysqlUsername());
        hikari.setPassword(config.getMysqlPassword());
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");

        hikari.setMaximumPoolSize(config.getMaxPoolSize());
        hikari.setMinimumIdle(config.getMinIdle());
        hikari.setMaxLifetime(config.getMaxLifetime());
        hikari.setConnectionTimeout(config.getConnectionTimeout());
        if (config.getKeepaliveTime() > 0) {
            hikari.setKeepaliveTime(config.getKeepaliveTime());
        }
        hikari.setPoolName("GianTags-Pool");

        // Optimise for small payloads
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "50");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "512");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            dataSource = new HikariDataSource(hikari);
        } catch (Exception e) {
            throw new StorageException("Failed to create HikariCP data source", e);
        }

        createTables();
    }

    @Override
    @Nullable
    public PlayerData loadPlayer(@NotNull UUID uuid) throws StorageException {
        String sql = "SELECT `tag_id` FROM `" + tablePrefix + "player_data` WHERE `uuid` = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                String tagId = rs.getString("tag_id");
                return new PlayerData(uuid, tagId == null || tagId.isBlank() ? null : tagId);
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to load player " + uuid, e);
        }
    }

    @Override
    public void savePlayer(@NotNull UUID uuid, @NotNull PlayerData data) throws StorageException {
        String sql = "INSERT INTO `" + tablePrefix + "player_data` (`uuid`, `tag_id`) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE `tag_id` = VALUES(`tag_id`)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, data.getTagId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save player " + uuid, e);
        }
    }

    @Override
    public void deletePlayer(@NotNull UUID uuid) throws StorageException {
        String sql = "DELETE FROM `" + tablePrefix + "player_data` WHERE `uuid` = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete player " + uuid, e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    private void createTables() throws StorageException {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "player_data` ("
                + "`uuid` VARCHAR(36) NOT NULL, "
                + "`tag_id` VARCHAR(64) DEFAULT NULL, "
                + "PRIMARY KEY (`uuid`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to create tables", e);
        }
    }
}
