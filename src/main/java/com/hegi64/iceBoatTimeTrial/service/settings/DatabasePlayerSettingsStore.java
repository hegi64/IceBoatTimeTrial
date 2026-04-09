package com.hegi64.iceBoatTimeTrial.service.settings;

import com.hegi64.iceBoatTimeTrial.storage.Database;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class DatabasePlayerSettingsStore implements PlayerSettingsStore {
    private final Database database;

    public DatabasePlayerSettingsStore(Database database) {
        this.database = database;
    }

    @Override
    public Optional<PlayerSettingsRecord> load(UUID playerUuid) throws SQLException {
        return database.loadPlayerSettings(playerUuid)
                .map(row -> new PlayerSettingsRecord(row.bossbarEnabled(), row.verbosity()));
    }

    @Override
    public void upsert(UUID playerUuid, boolean bossbarEnabled, String verbosity) throws SQLException {
        database.upsertPlayerSettings(playerUuid, bossbarEnabled, verbosity);
    }
}
