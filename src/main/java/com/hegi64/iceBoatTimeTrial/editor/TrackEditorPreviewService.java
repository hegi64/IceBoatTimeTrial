package com.hegi64.iceBoatTimeTrial.editor;

import com.hegi64.iceBoatTimeTrial.model.RegionBox;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Optional;

public class TrackEditorPreviewService {
    private final JavaPlugin plugin;
    private final TrackEditorSessionService sessions;
    private final TrackService trackService;
    private int taskId = -1;

    private long previewIntervalTicks;
    private double edgeStep;
    private float particleSize;

    public TrackEditorPreviewService(JavaPlugin plugin, TrackEditorSessionService sessions, TrackService trackService) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.trackService = trackService;
        // Defaults, will be set by setConfig
        this.previewIntervalTicks = 2L;
        this.edgeStep = 0.2;
        this.particleSize = 0.75f;
    }

    public void setConfig(com.hegi64.iceBoatTimeTrial.config.PluginConfig config) {
        this.previewIntervalTicks = config.editorPreviewIntervalTicks();
        this.edgeStep = config.editorPreviewEdgeStep();
        this.particleSize = config.editorPreviewParticleSize();
        // If running, restart with new interval
        if (taskId != -1) {
            start();
        }
    }

    public void start() {
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, previewIntervalTicks, previewIntervalTicks);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Optional<TrackEditorSession> sessionOpt = sessions.getByPlayer(player.getUniqueId());
            if (sessionOpt.isEmpty()) {
                continue;
            }
            TrackEditorSession session = sessionOpt.get();
            Track track = trackService.allTracks().stream()
                    .filter(t -> t.getUuid().equals(session.trackUuid()))
                    .findFirst()
                    .orElse(null);
            if (track == null) {
                continue;
            }
            drawTrackPreview(player, track);
        }
    }

    private void drawTrackPreview(Player player, Track track) {
        drawRegion(player, track.getRegion(RegionType.START_FINISH), Color.fromRGB(255, 80, 80));
        drawRegion(player, track.getRegion(RegionType.CHECKPOINT_1), Color.fromRGB(80, 255, 80));
        drawRegion(player, track.getRegion(RegionType.CHECKPOINT_2), Color.fromRGB(80, 160, 255));

        drawDirection(player, track.getRegion(RegionType.START_FINISH), Color.fromRGB(255, 120, 120));
        drawDirection(player, track.getRegion(RegionType.CHECKPOINT_1), Color.fromRGB(120, 255, 120));
        drawDirection(player, track.getRegion(RegionType.CHECKPOINT_2), Color.fromRGB(120, 180, 255));
    }

    private void drawRegion(Player player, RegionBox box, Color color) {
        if (box == null) {
            return;
        }
        World world = player.getWorld();
        if (!world.getName().equals(box.world())) {
            return;
        }

        Particle.DustOptions dust = new Particle.DustOptions(color, particleSize);

        double minX = box.minX();
        double minY = box.minY();
        double minZ = box.minZ();
        double maxX = box.maxX() + 1.0;
        double maxY = box.maxY() + 1.0;
        double maxZ = box.maxZ() + 1.0;

        // 12 box edges: 4 along X, 4 along Y, 4 along Z.
        drawLine(player, minX, minY, minZ, maxX, minY, minZ, dust);
        drawLine(player, minX, maxY, minZ, maxX, maxY, minZ, dust);
        drawLine(player, minX, minY, maxZ, maxX, minY, maxZ, dust);
        drawLine(player, minX, maxY, maxZ, maxX, maxY, maxZ, dust);

        drawLine(player, minX, minY, minZ, minX, maxY, minZ, dust);
        drawLine(player, maxX, minY, minZ, maxX, maxY, minZ, dust);
        drawLine(player, minX, minY, maxZ, minX, maxY, maxZ, dust);
        drawLine(player, maxX, minY, maxZ, maxX, maxY, maxZ, dust);

        drawLine(player, minX, minY, minZ, minX, minY, maxZ, dust);
        drawLine(player, maxX, minY, minZ, maxX, minY, maxZ, dust);
        drawLine(player, minX, maxY, minZ, minX, maxY, maxZ, dust);
        drawLine(player, maxX, maxY, minZ, maxX, maxY, maxZ, dust);
    }

    private void drawLine(Player player,
                          double x1,
                          double y1,
                          double z1,
                          double x2,
                          double y2,
                          double z2,
                          Particle.DustOptions dust) {
        Vector delta = new Vector(x2 - x1, y2 - y1, z2 - z1);
        double length = delta.length();
        if (length < 1.0e-6) {
            spawn(player, x1, y1, z1, dust);
            return;
        }
        int points = Math.max(2, (int) Math.ceil(length / edgeStep));
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            spawn(player,
                    x1 + (delta.getX() * t),
                    y1 + (delta.getY() * t),
                    z1 + (delta.getZ() * t),
                    dust);
        }
    }

    private void drawDirection(Player player, RegionBox box, Color color) {
        if (box == null || box.dirX() == null || box.dirY() == null || box.dirZ() == null) {
            return;
        }
        World world = player.getWorld();
        if (!world.getName().equals(box.world())) {
            return;
        }

        double cx = (box.minX() + box.maxX() + 1) / 2.0;
        double cy = (box.minY() + box.maxY() + 1) / 2.0;
        double cz = (box.minZ() + box.maxZ() + 1) / 2.0;

        Vector dir = new Vector(box.dirX(), box.dirY(), box.dirZ());
        if (dir.lengthSquared() < 1.0e-6) {
            return;
        }
        dir.normalize().multiply(2.0);
        Particle.DustOptions dust = new Particle.DustOptions(color, particleSize);
        for (double step = 0.0; step <= 1.0; step += 0.1) {
            Vector point = dir.clone().multiply(step);
            spawn(player, cx + point.getX(), cy + point.getY(), cz + point.getZ(), dust);
        }
    }

    private void spawn(Player player, double x, double y, double z, Particle.DustOptions dust) {
        player.spawnParticle(Particle.DUST, new Location(player.getWorld(), x, y, z), 1, 0.0, 0.0, 0.0, 0.0, dust);
    }
}
