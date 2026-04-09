package com.hegi64.iceBoatTimeTrial.service.settings;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSettingsServiceTest {

    @Test
    void loadsDefaultsWhenNoStoredSettingsExist() {
        InMemoryStore store = new InMemoryStore();
        PlayerSettingsService service = new PlayerSettingsService(store);

        PlayerSettings settings = service.get(UUID.randomUUID());

        assertTrue(settings.bossbarEnabled());
        assertEquals(MessageVerbosity.NORMAL, settings.verbosity());
    }

    @Test
    void persistsAndCyclesSettings() {
        InMemoryStore store = new InMemoryStore();
        PlayerSettingsService service = new PlayerSettingsService(store);
        UUID player = UUID.randomUUID();

        service.setBossbarEnabled(player, false);
        service.cycleVerbosity(player); // NORMAL -> MINIMAL

        PlayerSettings current = service.get(player);
        assertFalse(current.bossbarEnabled());
        assertEquals(MessageVerbosity.MINIMAL, current.verbosity());

        Optional<PlayerSettingsStore.PlayerSettingsRecord> persisted = store.load(player);
        assertTrue(persisted.isPresent());
        assertFalse(persisted.get().bossbarEnabled());
        assertEquals("MINIMAL", persisted.get().verbosity());
    }

    private static class InMemoryStore implements PlayerSettingsStore {
        private final Map<UUID, PlayerSettingsRecord> rows = new HashMap<>();

        @Override
        public Optional<PlayerSettingsRecord> load(UUID playerUuid) {
            return Optional.ofNullable(rows.get(playerUuid));
        }

        @Override
        public void upsert(UUID playerUuid, boolean bossbarEnabled, String verbosity) throws SQLException {
            rows.put(playerUuid, new PlayerSettingsRecord(bossbarEnabled, verbosity));
        }
    }
}
