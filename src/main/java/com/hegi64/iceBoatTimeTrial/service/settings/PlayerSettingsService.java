package com.hegi64.iceBoatTimeTrial.service.settings;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSettingsService {
    private final PlayerSettingsStore store;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();

    public PlayerSettingsService(PlayerSettingsStore store) {
        this.store = store;
    }

    public PlayerSettings get(UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, this::loadOrDefault);
    }

    public void set(UUID playerUuid, PlayerSettings settings) {
        cache.put(playerUuid, settings);
        try {
            store.upsert(playerUuid, settings.bossbarEnabled(), settings.verbosity().name());
        } catch (SQLException ignored) {
            // Keep in-memory state even if persistence fails.
        }
    }

    public void setBossbarEnabled(UUID playerUuid, boolean enabled) {
        PlayerSettings current = get(playerUuid);
        set(playerUuid, new PlayerSettings(enabled, current.verbosity()));
    }

    public void cycleVerbosity(UUID playerUuid) {
        PlayerSettings current = get(playerUuid);
        MessageVerbosity next = switch (current.verbosity()) {
            case DETAILED -> MessageVerbosity.NORMAL;
            case NORMAL -> MessageVerbosity.MINIMAL;
            case MINIMAL -> MessageVerbosity.DETAILED;
        };
        set(playerUuid, new PlayerSettings(current.bossbarEnabled(), next));
    }

    public void clearCache(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    private PlayerSettings loadOrDefault(UUID playerUuid) {
        try {
            return store.load(playerUuid)
                    .map(row -> new PlayerSettings(row.bossbarEnabled(), MessageVerbosity.from(row.verbosity())))
                    .orElseGet(PlayerSettings::defaults);
        } catch (SQLException exception) {
            return PlayerSettings.defaults();
        }
    }
}
