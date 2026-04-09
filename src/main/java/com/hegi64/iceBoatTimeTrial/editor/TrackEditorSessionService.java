package com.hegi64.iceBoatTimeTrial.editor;

import com.hegi64.iceBoatTimeTrial.model.Track;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackEditorSessionService {
    private final Map<UUID, TrackEditorSession> byPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> trackLocks = new ConcurrentHashMap<>();

    public Optional<TrackEditorSession> getByPlayer(UUID playerUuid) {
        return Optional.ofNullable(byPlayer.get(playerUuid));
    }

    public Optional<UUID> getEditorForTrack(UUID trackUuid) {
        return Optional.ofNullable(trackLocks.get(trackUuid));
    }

    public synchronized boolean startSession(UUID playerUuid, Track track) {
        TrackEditorSession existing = byPlayer.get(playerUuid);
        if (existing != null && !existing.trackUuid().equals(track.getUuid())) {
            return false;
        }
        UUID locker = trackLocks.get(track.getUuid());
        if (locker != null && !locker.equals(playerUuid)) {
            return false;
        }
        TrackEditorSession session = new TrackEditorSession(playerUuid, track.getUuid());
        byPlayer.put(playerUuid, session);
        trackLocks.put(track.getUuid(), playerUuid);
        return true;
    }

    public synchronized Optional<TrackEditorSession> stopSession(UUID playerUuid) {
        TrackEditorSession removed = byPlayer.remove(playerUuid);
        if (removed != null) {
            trackLocks.remove(removed.trackUuid(), playerUuid);
        }
        return Optional.ofNullable(removed);
    }

    public synchronized Optional<UUID> forceStopTrack(UUID trackUuid) {
        UUID playerUuid = trackLocks.remove(trackUuid);
        if (playerUuid != null) {
            byPlayer.remove(playerUuid);
        }
        return Optional.ofNullable(playerUuid);
    }

    public synchronized void clearAll() {
        byPlayer.clear();
        trackLocks.clear();
    }
}

