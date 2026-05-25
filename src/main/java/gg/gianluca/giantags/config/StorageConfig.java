package gg.gianluca.giantags.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Typed wrapper for {@code storage.yml}.
 */
public final class StorageConfig {

    public enum StorageType { FLATFILE, MYSQL }

    private StorageType type;
    private boolean autoSaveEnabled;
    private int autoSaveInterval;

    // MySQL
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private String tablePrefix;
    private int maxPoolSize;
    private int minIdle;
    private long maxLifetime;
    private long connectionTimeout;
    private long keepaliveTime;

    public void load(@NotNull FileConfiguration config) {
        ConfigurationSection storage = config.getConfigurationSection("storage");
        if (storage == null) {
            loadDefaults();
            return;
        }

        String typeStr = storage.getString("type", "FLATFILE").toUpperCase();
        try {
            type = StorageType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = StorageType.FLATFILE;
        }

        ConfigurationSection autoSave = storage.getConfigurationSection("auto-save");
        autoSaveEnabled = autoSave == null || autoSave.getBoolean("enabled", true);
        autoSaveInterval = autoSave != null ? autoSave.getInt("interval", 300) : 300;

        ConfigurationSection mysql = storage.getConfigurationSection("mysql");
        if (mysql != null) {
            mysqlHost = mysql.getString("host", "localhost");
            mysqlPort = mysql.getInt("port", 3306);
            mysqlDatabase = mysql.getString("database", "giantags");
            mysqlUsername = mysql.getString("username", "root");
            mysqlPassword = mysql.getString("password", "");
            tablePrefix = mysql.getString("table-prefix", "gt_");

            ConfigurationSection pool = mysql.getConfigurationSection("pool");
            if (pool != null) {
                maxPoolSize = pool.getInt("maximum-pool-size", 10);
                minIdle = pool.getInt("minimum-idle", 5);
                maxLifetime = pool.getLong("maximum-lifetime", 1800000L);
                connectionTimeout = pool.getLong("connection-timeout", 5000L);
                keepaliveTime = pool.getLong("keepalive-time", 0L);
            } else {
                setDefaultPool();
            }
        } else {
            setDefaultMysql();
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @NotNull public StorageType getType() { return type; }
    public boolean isAutoSaveEnabled() { return autoSaveEnabled; }
    public int getAutoSaveInterval() { return autoSaveInterval; }

    @NotNull public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    @NotNull public String getMysqlDatabase() { return mysqlDatabase; }
    @NotNull public String getMysqlUsername() { return mysqlUsername; }
    @NotNull public String getMysqlPassword() { return mysqlPassword; }
    @NotNull public String getTablePrefix() { return tablePrefix; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getMinIdle() { return minIdle; }
    public long getMaxLifetime() { return maxLifetime; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public long getKeepaliveTime() { return keepaliveTime; }

    // ── Defaults ──────────────────────────────────────────────────────────────

    private void loadDefaults() {
        type = StorageType.FLATFILE;
        autoSaveEnabled = true;
        autoSaveInterval = 300;
        setDefaultMysql();
    }

    private void setDefaultMysql() {
        mysqlHost = "localhost";
        mysqlPort = 3306;
        mysqlDatabase = "giantags";
        mysqlUsername = "root";
        mysqlPassword = "";
        tablePrefix = "gt_";
        setDefaultPool();
    }

    private void setDefaultPool() {
        maxPoolSize = 10;
        minIdle = 5;
        maxLifetime = 1800000L;
        connectionTimeout = 5000L;
        keepaliveTime = 0L;
    }
}
