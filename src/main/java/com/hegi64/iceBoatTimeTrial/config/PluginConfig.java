package com.hegi64.iceBoatTimeTrial.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    private FileConfiguration config;

    public PluginConfig(FileConfiguration config) {
        this.config = config;
    }

    public void update(FileConfiguration config) {
        this.config = config;
    }

    public String messagePrefix() {
        return config.getString("messages.prefix", "&7[&bIBT&7] ");
    }

    public String runAbortedMessage() {
        return config.getString("messages.run-aborted", "&cRun aborted.");
    }

    public String runFinishedTemplate() {
        return config.getString("messages.run-finished", "&aFinished! Total: %color%%total% &8(%delta%) &7(S1 %s1% | S2 %s2% | S3 %s3%)");
    }

    public String sectorFinishedTemplate() {
        return config.getString("messages.sector-finished", "&7Sector %sector%: %color%%time% &8(%delta%)");
    }

    public String bossbarTitleTemplate() {
        return config.getString("bossbar.title", "&bTrack: &f%track% &7| &e%time% &7| &aS%sector% &7| %delta%");
    }

    public long triggerDebounceMillis() {
        return Math.max(0L, config.getLong("timing.trigger-debounce-ms", 250L));
    }

    public boolean abortOnTeleport() {
        return config.getBoolean("timing.abort-on-teleport", true);
    }

    public boolean abortOnWorldChange() {
        return config.getBoolean("timing.abort-on-world-change", true);
    }

    public boolean abortOnVehicleExit() {
        return config.getBoolean("timing.abort-on-vehicle-exit", true);
    }

    public boolean saveRunHistory() {
        return config.getBoolean("timing.save-run-history", true);
    }

    public boolean enforceDirection() {
        return config.getBoolean("detection.enforce-direction", true);
    }

    public double defaultDirectionThreshold() {
        return config.getDouble("detection.default-direction-threshold", 0.25);
    }

    public String overlapPolicy() {
        return config.getString("detection.overlap-policy", "FIRST_CREATED");
    }

    public int leaderboardDefaultLimit() {
        return config.getInt("leaderboard.default-limit", 10);
    }

    public int hologramLimit() {
        return config.getInt("hologram.limit", 10);
    }

    public String hologramTopTimesTitle() {
        return config.getString("hologram.top_times_title", "§bTop Times for §f%track%");
    }

    public String hologramTopPlayersTitle() {
        return config.getString("hologram.top_players_title", "§bTop Players for §f%track%");
    }

    public String hologramTopTimesSectorTitle() {
        return config.getString("hologram.top_times_sector_title", "§bTop S%sector% Times for §f%track%");
    }

    public String hologramTopPlayersSectorTitle() {
        return config.getString("hologram.top_players_sector_title", "§bTop S%sector% Players for §f%track%");
    }

    public String hologramEntryTemplate() {
        return config.getString("hologram.entry", "§e#%rank% §f%player% §7- §a%time%");
    }

    public String hologramEntryPlayersTemplate() {
        return config.getString("hologram.entry_players", "§e#%rank% §f%player% §7- §a%time%");
    }

    public String hologramEmptyLine() {
        return config.getString("hologram.empty", "§7No entries yet.");
    }

    public long editorPreviewIntervalTicks() {
        int val = config.getInt("editor-preview.interval-ticks", 2);
        return val < 1 ? 2 : val;
    }

    public double editorPreviewEdgeStep() {
        double val = config.getDouble("editor-preview.edge-step", 0.2);
        if (val < 0.05 || val > 1.0) return 0.2;
        return val;
    }

    public float editorPreviewParticleSize() {
        double val = config.getDouble("editor-preview.particle-size", 0.75);
        if (val < 0.1 || val > 2.0) return 0.75f;
        return (float) val;
    }
}
