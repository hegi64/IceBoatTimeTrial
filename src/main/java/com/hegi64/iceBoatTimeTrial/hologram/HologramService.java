package com.hegi64.iceBoatTimeTrial.hologram;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramService {
    private final JavaPlugin plugin;
    private final HologramManager hologramManager;
    private final File storageFile;
    private final Map<String, HologramMeta> holograms = new HashMap<>(); // key: track|type
    private final Map<String, Hologram> spawned = new HashMap<>();

    public HologramService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hologramManager = FancyHologramsPlugin.get().getHologramManager();
        this.storageFile = new File(plugin.getDataFolder(), "holograms.yml");
    }

    public void loadAll() {
        holograms.clear();
        if (!storageFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
        for (String key : yaml.getKeys(false)) {
            String[] parts = key.split("\\|");
            if (parts.length != 2) {
                continue;
            }
            String track = parts[0];
            String type = parts[1];
            String world = yaml.getString(key + ".world");
            double x = yaml.getDouble(key + ".x");
            double y = yaml.getDouble(key + ".y");
            double z = yaml.getDouble(key + ".z");
            if (world == null || world.isBlank()) {
                continue;
            }
            holograms.put(key, new HologramMeta(track, type, world, x, y, z));
        }
    }

    public void saveAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, HologramMeta> entry : holograms.entrySet()) {
            String key = entry.getKey();
            HologramMeta meta = entry.getValue();
            yaml.set(key + ".track", meta.track());
            yaml.set(key + ".type", meta.type());
            yaml.set(key + ".world", meta.world());
            yaml.set(key + ".x", meta.x());
            yaml.set(key + ".y", meta.y());
            yaml.set(key + ".z", meta.z());
        }
        try {
            yaml.save(storageFile);
        } catch (IOException ignored) {
        }
    }

    public void spawnAll() {
        for (HologramMeta meta : holograms.values()) {
            spawn(meta);
        }
    }

    public void despawnAll() {
        for (Hologram holo : spawned.values()) {
            hologramManager.removeHologram(holo);
        }
        spawned.clear();
    }

    public boolean hasHologram(String track, String type) {
        return holograms.containsKey(key(track, type));
    }

    public Collection<HologramMeta> getPlacedHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    public void placeHologram(String track, String type, Location loc) {
        String key = key(track, type);
        HologramMeta meta = new HologramMeta(track, type, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        holograms.put(key, meta);
        saveAll();
        spawn(meta);
    }

    public void removeHologram(String track, String type) {
        String key = key(track, type);
        holograms.remove(key);
        saveAll();
        Hologram holo = spawned.remove(key);
        if (holo != null) {
            hologramManager.removeHologram(holo);
        }
    }

    public void updateHologram(String track, String type, List<String> lines) {
        String key = key(track, type);
        Hologram holo = spawned.get(key);
        if (holo == null) {
            HologramMeta meta = holograms.get(key);
            if (meta == null) {
                return;
            }
            spawn(meta);
            holo = spawned.get(key);
            if (holo == null) {
                return;
            }
        }

        if (holo.getData() instanceof TextHologramData textData) {
            textData.setText(new ArrayList<>(lines));
            holo.forceUpdate();
        }
    }

    private void spawn(HologramMeta meta) {
        String key = key(meta.track(), meta.type());
        World world = Bukkit.getWorld(meta.world());
        if (world == null) {
            return;
        }

        Hologram previous = spawned.remove(key);
        if (previous != null) {
            hologramManager.removeHologram(previous);
        }

        String hologramName = "ibt_" + sanitize(meta.track()) + "_" + sanitize(meta.type());
        hologramManager.getHologram(hologramName).ifPresent(hologramManager::removeHologram);

        Location loc = new Location(world, meta.x(), meta.y(), meta.z());
        TextHologramData data = new TextHologramData(hologramName, loc);
        data.setPersistent(false);

        Hologram hologram = hologramManager.create(data);
        hologramManager.addHologram(hologram);
        spawned.put(key, hologram);
    }

    private String key(String track, String type) {
        return track + "|" + type;
    }

    private String sanitize(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }

    public record HologramMeta(String track, String type, String world, double x, double y, double z) {
    }
}
