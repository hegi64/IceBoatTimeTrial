package com.hegi64.iceBoatTimeTrial.model;

import org.bukkit.Location;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class Track {
    private final UUID uuid;
    private String name;
    private final String world;
    private boolean enabled;
    private final Map<RegionType, RegionBox> regions;
    private Location spawn;

    public Track(UUID uuid, String name, String world, boolean enabled) {
        this.uuid = uuid;
        this.name = name;
        this.world = world;
        this.enabled = enabled;
        this.regions = new EnumMap<>(RegionType.class);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorld() {
        return world;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RegionBox getRegion(RegionType type) {
        return regions.get(type);
    }

    public void setRegion(RegionType type, RegionBox box) {
        regions.put(type, box);
    }

    public Location getSpawn() { return spawn; }

    public void setSpawn(Location spawn) { this.spawn = spawn; }

    public boolean hasSpawn() {
        return spawn != null;
    }

    public boolean isComplete() {
        return regions.containsKey(RegionType.START_FINISH)
                && regions.containsKey(RegionType.CHECKPOINT_1)
                && regions.containsKey(RegionType.CHECKPOINT_2);
    }
}
