package com.hegi64.iceBoatTimeTrial.service.settings;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSettingsStore {
    Optional<PlayerSettingsRecord> load(UUID playerUuid) throws SQLException;

    void upsert(UUID playerUuid, boolean bossbarEnabled, String verbosity) throws SQLException;

    record PlayerSettingsRecord(boolean bossbarEnabled, String verbosity) {
    }
}
