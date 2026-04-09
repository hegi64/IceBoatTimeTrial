package com.hegi64.iceBoatTimeTrial.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginConfigTest {

    @Test
    void readsConfiguredValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("messages.prefix", "&7[TEST] ");
        yaml.set("timing.trigger-debounce-ms", 123L);
        yaml.set("timing.abort-on-teleport", false);
        yaml.set("timing.abort-on-world-change", false);
        yaml.set("timing.abort-on-vehicle-exit", false);
        yaml.set("timing.save-run-history", false);
        yaml.set("leaderboard.default-limit", 7);
        yaml.set("bossbar.title", "X");

        PluginConfig config = new PluginConfig(yaml);

        assertEquals("&7[TEST] ", config.messagePrefix());
        assertEquals(123L, config.triggerDebounceMillis());
        assertFalse(config.abortOnTeleport());
        assertFalse(config.abortOnWorldChange());
        assertFalse(config.abortOnVehicleExit());
        assertFalse(config.saveRunHistory());
        assertEquals(7, config.leaderboardDefaultLimit());
        assertEquals("X", config.bossbarTitleTemplate());
    }

    @Test
    void usesDefaultsWhenMissing() {
        PluginConfig config = new PluginConfig(new YamlConfiguration());

        assertEquals("&7[&bIBT&7] ", config.messagePrefix());
        assertEquals(250L, config.triggerDebounceMillis());
        assertTrue(config.abortOnTeleport());
        assertTrue(config.abortOnWorldChange());
        assertTrue(config.abortOnVehicleExit());
        assertTrue(config.saveRunHistory());
        assertEquals(10, config.leaderboardDefaultLimit());
    }
}

