package com.hegi64.iceBoatTimeTrial.service;

import com.hegi64.iceBoatTimeTrial.model.OverlapPolicy;
import com.hegi64.iceBoatTimeTrial.model.RegionBox;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackService {
    private final Database database;
    private final Map<String, Track> tracksByLowerName = new ConcurrentHashMap<>();

    public TrackService(Database database) {
        this.database = database;
    }

    public void load() throws SQLException {
        tracksByLowerName.clear();
        for (Track track : database.loadAllTracks()) {
            tracksByLowerName.put(track.getName().toLowerCase(Locale.ROOT), track);
        }
    }

    public List<Track> allTracks() {
        return new ArrayList<>(tracksByLowerName.values());
    }

    public Optional<Track> findByName(String name) {
        return Optional.ofNullable(tracksByLowerName.get(name.toLowerCase(Locale.ROOT)));
    }

    public Track createTrack(String name, String world) throws SQLException {
        Track track = database.createTrack(name, world);
        tracksByLowerName.put(name.toLowerCase(Locale.ROOT), track);
        return track;
    }

    public boolean renameTrack(Track track, String newName) throws SQLException {
        String oldLower = track.getName().toLowerCase(Locale.ROOT);
        String newLower = newName.toLowerCase(Locale.ROOT);
        if (!oldLower.equals(newLower) && tracksByLowerName.containsKey(newLower)) {
            return false;
        }
        if (!database.renameTrack(track.getUuid(), newName)) {
            return false;
        }
        tracksByLowerName.remove(oldLower);
        track.setName(newName);
        tracksByLowerName.put(newLower, track);
        return true;
    }

    public boolean deleteTrack(String name) throws SQLException {
        Track track = tracksByLowerName.remove(name.toLowerCase(Locale.ROOT));
        if (track == null) {
            return false;
        }
        database.deleteTrack(track.getUuid());
        return true;
    }

    public void setEnabled(Track track, boolean enabled) throws SQLException {
        track.setEnabled(enabled);
        database.setTrackEnabled(track.getUuid(), enabled);
    }

    public void setRegion(Track track, RegionType type, RegionBox regionBox) throws SQLException {
        track.setRegion(type, regionBox);
        database.upsertRegion(track.getUuid(), type, regionBox);
    }

    public void setSpawn(Track track, Location location) throws SQLException {
        track.setSpawn(location);
        database.updateSpawn(track.getUuid(), location);
    }

    public void setDirection(Track track, RegionType type, double x, double y, double z, Double threshold) throws SQLException {
        RegionBox box = track.getRegion(type);
        if (box == null) {
            throw new IllegalStateException("Region not set: " + type);
        }
        RegionBox updated = new RegionBox(
                box.world(), box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(),
                x, y, z, threshold
        );
        track.setRegion(type, updated);
        database.updateRegionDirection(track.getUuid(), type, x, y, z, threshold);
    }

    public Optional<Track> resolveStartTrack(Location location, OverlapPolicy overlapPolicy) {
        List<Track> matching = allTracks().stream()
                .filter(Track::isEnabled)
                .filter(Track::isComplete)
                .filter(track -> track.getRegion(RegionType.START_FINISH).contains(location))
                .toList();
        if (matching.isEmpty()) {
            return Optional.empty();
        }
        if (matching.size() == 1 || overlapPolicy == OverlapPolicy.FIRST_CREATED) {
            return Optional.of(matching.get(0));
        }
        return matching.stream()
                .min(Comparator.comparingDouble(track -> track.getRegion(RegionType.START_FINISH).centerDistanceSquared(location)));
    }
}
