package com.hegi64.iceBoatTimeTrial.service;

import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.ActiveRun;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarService {
    private final JavaPlugin plugin;
    private PluginConfig config;
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private int taskId = -1;

    public BossBarService(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start(Map<UUID, ActiveRun> activeRuns) {
        // Bossbar updates as frequently as possible (every tick, 50ms). Bukkit does not support sub-tick intervals.
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> updateBars(activeRuns), 1L, 1L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }

    public void show(Player player) {
        bars.computeIfAbsent(player.getUniqueId(), ignored -> {
            BossBar bar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
            bar.setVisible(true);
            return bar;
        });
    }

    public void hide(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
            bar.removeAll();
        }
    }

    private void updateBars(Map<UUID, ActiveRun> activeRuns) {
        String template = config.bossbarTitleTemplate();
        for (Map.Entry<UUID, ActiveRun> entry : activeRuns.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            BossBar bar = bars.computeIfAbsent(player.getUniqueId(), ignored -> {
                BossBar created = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
                created.addPlayer(player);
                created.setVisible(true);
                return created;
            });
            ActiveRun run = entry.getValue();
            int sector = !run.isCheckpoint1Passed() ? 1 : (!run.isCheckpoint2Passed() ? 2 : 3);
            long elapsed = System.currentTimeMillis() - run.getStartedAtMillis();
            long sectorElapsed = currentSectorElapsed(run, elapsed, sector);
            Long personalBest = sectorPersonalBest(run, sector);
            String delta = formatBossbarDelta(sectorElapsed, personalBest);
            String text = template
                    .replace("%track%", run.getTrack().getName())
                    .replace("%time%", TimeFormat.formatMillisOneDecimal(elapsed))
                    .replace("%sector%", Integer.toString(sector))
                    .replace("%delta%", delta);
            bar.setTitle(Chat.color(text));
            bar.setProgress(1.0);
        }
    }

    private long currentSectorElapsed(ActiveRun run, long elapsed, int sector) {
        if (sector == 1) {
            return elapsed;
        }
        if (sector == 2) {
            return elapsed - (run.getSector1Millis() == null ? 0L : run.getSector1Millis());
        }
        long s1 = run.getSector1Millis() == null ? 0L : run.getSector1Millis();
        long s2 = run.getSector2Millis() == null ? 0L : run.getSector2Millis();
        return elapsed - s1 - s2;
    }

    private Long sectorPersonalBest(ActiveRun run, int sector) {
        return switch (sector) {
            case 1 -> run.getPersonalBestSector1Millis();
            case 2 -> run.getPersonalBestSector2Millis();
            default -> run.getPersonalBestSector3Millis();
        };
    }

    private String formatBossbarDelta(long sectorElapsed, Long personalBest) {
        if (personalBest == null) {
            return "&7--";
        }
        long delta = sectorElapsed - personalBest;
        String color = delta <= 0 ? "&a" : "&e";
        return color + TimeFormat.formatSignedMillisOneDecimal(delta);
    }

    public void setConfig(PluginConfig config) {
        this.config = config;
    }
}
