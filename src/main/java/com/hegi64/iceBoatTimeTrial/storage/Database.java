package com.hegi64.iceBoatTimeTrial.storage;

import com.hegi64.iceBoatTimeTrial.model.RegionBox;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.Track;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Database {
    private final JavaPlugin plugin;
    private final String jdbcUrl;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
        File dbFile = new File(plugin.getDataFolder(), "iceboat.db");
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public void init() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new SQLException("Could not create plugin data folder");
        }
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureUuidSchema(connection);
                createIndexes(connection);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void ensureUuidSchema(Connection connection) throws SQLException {
        if (!tableExists(connection, "tracks")) {
            createUuidSchema(connection);
            return;
        }
        if (!columnExists(connection, "tracks", "track_uuid")) {
            migrateLegacyNumericSchema(connection);
        }
    }

    private void createUuidSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tracks (
                        track_uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        world TEXT NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS track_regions (
                        track_uuid TEXT NOT NULL,
                        region_type TEXT NOT NULL,
                        min_x INTEGER NOT NULL,
                        min_y INTEGER NOT NULL,
                        min_z INTEGER NOT NULL,
                        max_x INTEGER NOT NULL,
                        max_y INTEGER NOT NULL,
                        max_z INTEGER NOT NULL,
                        dir_x REAL,
                        dir_y REAL,
                        dir_z REAL,
                        dir_threshold REAL,
                        PRIMARY KEY (track_uuid, region_type),
                        FOREIGN KEY (track_uuid) REFERENCES tracks(track_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_bests (
                        player_uuid TEXT NOT NULL,
                        track_uuid TEXT NOT NULL,
                        best_total_ms INTEGER NOT NULL,
                        best_s1_ms INTEGER NOT NULL,
                        best_s2_ms INTEGER NOT NULL,
                        best_s3_ms INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, track_uuid),
                        FOREIGN KEY (track_uuid) REFERENCES tracks(track_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS run_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        track_uuid TEXT NOT NULL,
                        total_ms INTEGER NOT NULL,
                        s1_ms INTEGER NOT NULL,
                        s2_ms INTEGER NOT NULL,
                        s3_ms INTEGER NOT NULL,
                        valid INTEGER NOT NULL,
                        finished_at INTEGER NOT NULL,
                        FOREIGN KEY (track_uuid) REFERENCES tracks(track_uuid) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private void migrateLegacyNumericSchema(Connection connection) throws SQLException {
        Map<Long, UUID> idMapping = new HashMap<>();

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE tracks_new (
                        track_uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        world TEXT NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE track_regions_new (
                        track_uuid TEXT NOT NULL,
                        region_type TEXT NOT NULL,
                        min_x INTEGER NOT NULL,
                        min_y INTEGER NOT NULL,
                        min_z INTEGER NOT NULL,
                        max_x INTEGER NOT NULL,
                        max_y INTEGER NOT NULL,
                        max_z INTEGER NOT NULL,
                        dir_x REAL,
                        dir_y REAL,
                        dir_z REAL,
                        dir_threshold REAL,
                        PRIMARY KEY (track_uuid, region_type),
                        FOREIGN KEY (track_uuid) REFERENCES tracks_new(track_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE player_bests_new (
                        player_uuid TEXT NOT NULL,
                        track_uuid TEXT NOT NULL,
                        best_total_ms INTEGER NOT NULL,
                        best_s1_ms INTEGER NOT NULL,
                        best_s2_ms INTEGER NOT NULL,
                        best_s3_ms INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, track_uuid),
                        FOREIGN KEY (track_uuid) REFERENCES tracks_new(track_uuid) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE run_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        track_uuid TEXT NOT NULL,
                        total_ms INTEGER NOT NULL,
                        s1_ms INTEGER NOT NULL,
                        s2_ms INTEGER NOT NULL,
                        s3_ms INTEGER NOT NULL,
                        valid INTEGER NOT NULL,
                        finished_at INTEGER NOT NULL,
                        FOREIGN KEY (track_uuid) REFERENCES tracks_new(track_uuid) ON DELETE CASCADE
                    )
                    """);
        }

        try (PreparedStatement select = connection.prepareStatement("SELECT id, name, world, enabled, created_at FROM tracks");
             ResultSet rs = select.executeQuery();
             PreparedStatement insert = connection.prepareStatement("INSERT INTO tracks_new(track_uuid, name, world, enabled, created_at) VALUES(?, ?, ?, ?, ?)")) {
            while (rs.next()) {
                long oldId = rs.getLong("id");
                UUID newId = UUID.randomUUID();
                idMapping.put(oldId, newId);
                insert.setString(1, newId.toString());
                insert.setString(2, rs.getString("name"));
                insert.setString(3, rs.getString("world"));
                insert.setInt(4, rs.getInt("enabled"));
                insert.setLong(5, rs.getLong("created_at"));
                insert.addBatch();
            }
            insert.executeBatch();
        }

        migrateTrackRegions(connection, idMapping);
        migratePlayerBests(connection, idMapping);
        migrateRunHistory(connection, idMapping);

        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE track_regions");
            statement.execute("DROP TABLE player_bests");
            statement.execute("DROP TABLE run_history");
            statement.execute("DROP TABLE tracks");

            statement.execute("ALTER TABLE tracks_new RENAME TO tracks");
            statement.execute("ALTER TABLE track_regions_new RENAME TO track_regions");
            statement.execute("ALTER TABLE player_bests_new RENAME TO player_bests");
            statement.execute("ALTER TABLE run_history_new RENAME TO run_history");
        }
    }

    private void migrateTrackRegions(Connection connection, Map<Long, UUID> idMapping) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT * FROM track_regions");
             ResultSet rs = select.executeQuery();
             PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO track_regions_new(track_uuid, region_type, min_x, min_y, min_z, max_x, max_y, max_z, dir_x, dir_y, dir_z, dir_threshold)
                     VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            while (rs.next()) {
                UUID newId = idMapping.get(rs.getLong("track_id"));
                if (newId == null) {
                    continue;
                }
                insert.setString(1, newId.toString());
                insert.setString(2, rs.getString("region_type"));
                insert.setInt(3, rs.getInt("min_x"));
                insert.setInt(4, rs.getInt("min_y"));
                insert.setInt(5, rs.getInt("min_z"));
                insert.setInt(6, rs.getInt("max_x"));
                insert.setInt(7, rs.getInt("max_y"));
                insert.setInt(8, rs.getInt("max_z"));
                setNullableDouble(insert, 9, asNullableDouble(rs, "dir_x"));
                setNullableDouble(insert, 10, asNullableDouble(rs, "dir_y"));
                setNullableDouble(insert, 11, asNullableDouble(rs, "dir_z"));
                setNullableDouble(insert, 12, asNullableDouble(rs, "dir_threshold"));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void migratePlayerBests(Connection connection, Map<Long, UUID> idMapping) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT * FROM player_bests");
             ResultSet rs = select.executeQuery();
             PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO player_bests_new(player_uuid, track_uuid, best_total_ms, best_s1_ms, best_s2_ms, best_s3_ms, updated_at)
                     VALUES(?, ?, ?, ?, ?, ?, ?)
                     """)) {
            while (rs.next()) {
                UUID newId = idMapping.get(rs.getLong("track_id"));
                if (newId == null) {
                    continue;
                }
                insert.setString(1, rs.getString("player_uuid"));
                insert.setString(2, newId.toString());
                insert.setLong(3, rs.getLong("best_total_ms"));
                insert.setLong(4, rs.getLong("best_s1_ms"));
                insert.setLong(5, rs.getLong("best_s2_ms"));
                insert.setLong(6, rs.getLong("best_s3_ms"));
                insert.setLong(7, rs.getLong("updated_at"));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void migrateRunHistory(Connection connection, Map<Long, UUID> idMapping) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT * FROM run_history");
             ResultSet rs = select.executeQuery();
             PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO run_history_new(player_uuid, track_uuid, total_ms, s1_ms, s2_ms, s3_ms, valid, finished_at)
                     VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            while (rs.next()) {
                UUID newId = idMapping.get(rs.getLong("track_id"));
                if (newId == null) {
                    continue;
                }
                insert.setString(1, rs.getString("player_uuid"));
                insert.setString(2, newId.toString());
                insert.setLong(3, rs.getLong("total_ms"));
                insert.setLong(4, rs.getLong("s1_ms"));
                insert.setLong(5, rs.getLong("s2_ms"));
                insert.setLong(6, rs.getLong("s3_ms"));
                insert.setInt(7, rs.getInt("valid"));
                insert.setLong(8, rs.getLong("finished_at"));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void createIndexes(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_bests_track_total ON player_bests(track_uuid, best_total_ms)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_history_track_total ON run_history(track_uuid, total_ms)");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1, tableName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + tableName + ")");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public Track createTrack(String name, String world) throws SQLException {
        UUID trackUuid = UUID.randomUUID();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tracks(track_uuid, name, world, enabled, created_at) VALUES(?, ?, ?, 1, ?)") ) {
            statement.setString(1, trackUuid.toString());
            statement.setString(2, name);
            statement.setString(3, world);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
            return new Track(trackUuid, name, world, true);
        }
    }

    public boolean renameTrack(UUID trackUuid, String newName) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE tracks SET name = ? WHERE track_uuid = ?")) {
            statement.setString(1, newName);
            statement.setString(2, trackUuid.toString());
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<Track> findTrackByName(String name) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT track_uuid, name, world, enabled FROM tracks WHERE lower(name) = lower(?)")) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Track track = new Track(
                        UUID.fromString(rs.getString("track_uuid")),
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getInt("enabled") == 1
                );
                loadTrackRegions(connection, track);
                return Optional.of(track);
            }
        }
    }

    public List<Track> loadAllTracks() throws SQLException {
        List<Track> tracks = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT track_uuid, name, world, enabled FROM tracks ORDER BY created_at ASC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Track track = new Track(
                        UUID.fromString(rs.getString("track_uuid")),
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getInt("enabled") == 1
                );
                loadTrackRegions(connection, track);
                tracks.add(track);
            }
        }
        return tracks;
    }

    private void loadTrackRegions(Connection connection, Track track) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM track_regions WHERE track_uuid = ?")) {
            statement.setString(1, track.getUuid().toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    RegionType type = RegionType.valueOf(rs.getString("region_type"));
                    RegionBox box = new RegionBox(
                            track.getWorld(),
                            rs.getInt("min_x"),
                            rs.getInt("min_y"),
                            rs.getInt("min_z"),
                            rs.getInt("max_x"),
                            rs.getInt("max_y"),
                            rs.getInt("max_z"),
                            asNullableDouble(rs, "dir_x"),
                            asNullableDouble(rs, "dir_y"),
                            asNullableDouble(rs, "dir_z"),
                            asNullableDouble(rs, "dir_threshold")
                    );
                    track.setRegion(type, box);
                }
            }
        }
    }

    private Double asNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    public void setTrackEnabled(UUID trackUuid, boolean enabled) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE tracks SET enabled = ? WHERE track_uuid = ?")) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, trackUuid.toString());
            statement.executeUpdate();
        }
    }

    public void deleteTrack(UUID trackUuid) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM tracks WHERE track_uuid = ?")) {
            statement.setString(1, trackUuid.toString());
            statement.executeUpdate();
        }
    }

    public void upsertRegion(UUID trackUuid, RegionType type, RegionBox box) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO track_regions(track_uuid, region_type, min_x, min_y, min_z, max_x, max_y, max_z, dir_x, dir_y, dir_z, dir_threshold)
                     VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     ON CONFLICT(track_uuid, region_type) DO UPDATE SET
                         min_x = excluded.min_x,
                         min_y = excluded.min_y,
                         min_z = excluded.min_z,
                         max_x = excluded.max_x,
                         max_y = excluded.max_y,
                         max_z = excluded.max_z,
                         dir_x = excluded.dir_x,
                         dir_y = excluded.dir_y,
                         dir_z = excluded.dir_z,
                         dir_threshold = excluded.dir_threshold
                     """)) {
            statement.setString(1, trackUuid.toString());
            statement.setString(2, type.name());
            statement.setInt(3, box.minX());
            statement.setInt(4, box.minY());
            statement.setInt(5, box.minZ());
            statement.setInt(6, box.maxX());
            statement.setInt(7, box.maxY());
            statement.setInt(8, box.maxZ());
            setNullableDouble(statement, 9, box.dirX());
            setNullableDouble(statement, 10, box.dirY());
            setNullableDouble(statement, 11, box.dirZ());
            setNullableDouble(statement, 12, box.directionThreshold());
            statement.executeUpdate();
        }
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DOUBLE);
        } else {
            statement.setDouble(index, value);
        }
    }

    public void updateRegionDirection(UUID trackUuid, RegionType type, double x, double y, double z, Double threshold) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE track_regions SET dir_x = ?, dir_y = ?, dir_z = ?, dir_threshold = ? WHERE track_uuid = ? AND region_type = ?")) {
            statement.setDouble(1, x);
            statement.setDouble(2, y);
            statement.setDouble(3, z);
            setNullableDouble(statement, 4, threshold);
            statement.setString(5, trackUuid.toString());
            statement.setString(6, type.name());
            statement.executeUpdate();
        }
    }

    public void saveRun(UUID playerId, UUID trackUuid, long total, long s1, long s2, long s3, boolean saveHistory) throws SQLException {
        long now = System.currentTimeMillis();
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                upsertBest(connection, playerId, trackUuid, total, s1, s2, s3, now);
                if (saveHistory) {
                    try (PreparedStatement history = connection.prepareStatement(
                            "INSERT INTO run_history(player_uuid, track_uuid, total_ms, s1_ms, s2_ms, s3_ms, valid, finished_at) VALUES(?, ?, ?, ?, ?, ?, 1, ?)") ) {
                        history.setString(1, playerId.toString());
                        history.setString(2, trackUuid.toString());
                        history.setLong(3, total);
                        history.setLong(4, s1);
                        history.setLong(5, s2);
                        history.setLong(6, s3);
                        history.setLong(7, now);
                        history.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void upsertBest(Connection connection, UUID playerId, UUID trackUuid, long total, long s1, long s2, long s3, long now) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT best_total_ms, best_s1_ms, best_s2_ms, best_s3_ms FROM player_bests WHERE player_uuid = ? AND track_uuid = ?")) {
            select.setString(1, playerId.toString());
            select.setString(2, trackUuid.toString());
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO player_bests(player_uuid, track_uuid, best_total_ms, best_s1_ms, best_s2_ms, best_s3_ms, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?)") ) {
                        insert.setString(1, playerId.toString());
                        insert.setString(2, trackUuid.toString());
                        insert.setLong(3, total);
                        insert.setLong(4, s1);
                        insert.setLong(5, s2);
                        insert.setLong(6, s3);
                        insert.setLong(7, now);
                        insert.executeUpdate();
                    }
                    return;
                }

                long bestTotal = Math.min(total, rs.getLong("best_total_ms"));
                long bestS1 = Math.min(s1, rs.getLong("best_s1_ms"));
                long bestS2 = Math.min(s2, rs.getLong("best_s2_ms"));
                long bestS3 = Math.min(s3, rs.getLong("best_s3_ms"));

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE player_bests SET best_total_ms = ?, best_s1_ms = ?, best_s2_ms = ?, best_s3_ms = ?, updated_at = ? WHERE player_uuid = ? AND track_uuid = ?")) {
                    update.setLong(1, bestTotal);
                    update.setLong(2, bestS1);
                    update.setLong(3, bestS2);
                    update.setLong(4, bestS3);
                    update.setLong(5, now);
                    update.setString(6, playerId.toString());
                    update.setString(7, trackUuid.toString());
                    update.executeUpdate();
                }
            }
        }
    }

    public List<String> getTopTimes(UUID trackUuid, int limit) throws SQLException {
        return getTopPlayers(trackUuid, limit, 0);
    }

    public List<String> getTopPlayers(UUID trackUuid, int limit, int sector) throws SQLException {
        String column = playerBestColumnForSector(sector);
        List<String> lines = new ArrayList<>();
        String sql = "SELECT player_uuid, " + column + " AS value_ms FROM player_bests WHERE track_uuid = ? ORDER BY " + column + " ASC LIMIT ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trackUuid.toString());
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    lines.add(rs.getString("player_uuid") + ":" + rs.getLong("value_ms"));
                }
            }
        }
        return lines;
    }

    public Optional<long[]> getBest(UUID playerId, UUID trackUuid) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT best_total_ms, best_s1_ms, best_s2_ms, best_s3_ms FROM player_bests WHERE player_uuid = ? AND track_uuid = ?")) {
            statement.setString(1, playerId.toString());
            statement.setString(2, trackUuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new long[]{
                        rs.getLong("best_total_ms"),
                        rs.getLong("best_s1_ms"),
                        rs.getLong("best_s2_ms"),
                        rs.getLong("best_s3_ms")
                });
            }
        }
    }

    public List<String> getTopTimesFromHistory(UUID trackUuid, int limit) throws SQLException {
        return getTopTimesFromHistory(trackUuid, limit, 0);
    }

    public List<String> getTopTimesFromHistory(UUID trackUuid, int limit, int sector) throws SQLException {
        String column = historyColumnForSector(sector);
        List<String> lines = new ArrayList<>();
        String sql = "SELECT player_uuid, " + column + " AS value_ms FROM run_history WHERE track_uuid = ? AND valid = 1 ORDER BY " + column + " ASC LIMIT ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trackUuid.toString());
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    lines.add(rs.getString("player_uuid") + ":" + rs.getLong("value_ms"));
                }
            }
        }
        return lines;
    }

    private String playerBestColumnForSector(int sector) {
        return switch (sector) {
            case 0 -> "best_total_ms";
            case 1 -> "best_s1_ms";
            case 2 -> "best_s2_ms";
            case 3 -> "best_s3_ms";
            default -> throw new IllegalArgumentException("Invalid sector: " + sector);
        };
    }

    private String historyColumnForSector(int sector) {
        return switch (sector) {
            case 0 -> "total_ms";
            case 1 -> "s1_ms";
            case 2 -> "s2_ms";
            case 3 -> "s3_ms";
            default -> throw new IllegalArgumentException("Invalid sector: " + sector);
        };
    }

    public Optional<long[]> getGlobalBest(UUID trackUuid) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT MIN(best_total_ms) AS best_total_ms, MIN(best_s1_ms) AS best_s1_ms, MIN(best_s2_ms) AS best_s2_ms, MIN(best_s3_ms) AS best_s3_ms FROM player_bests WHERE track_uuid = ?")) {
            statement.setString(1, trackUuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getObject("best_total_ms") == null) {
                    return Optional.empty();
                }
                return Optional.of(new long[]{
                        rs.getLong("best_total_ms"),
                        rs.getLong("best_s1_ms"),
                        rs.getLong("best_s2_ms"),
                        rs.getLong("best_s3_ms")
                });
            }
        }
    }
}
