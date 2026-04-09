package com.hegi64.iceBoatTimeTrial.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiSessionService {
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    public void open(Player player, GuiSession session) {
        sessions.put(player.getUniqueId(), session);
    }

    public Optional<GuiSession> get(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public Optional<GuiSession> close(Player player) {
        return Optional.ofNullable(sessions.remove(player.getUniqueId()));
    }

    public void clearAll() {
        sessions.clear();
    }
}

