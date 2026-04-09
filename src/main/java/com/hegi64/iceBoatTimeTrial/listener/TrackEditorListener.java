package com.hegi64.iceBoatTimeTrial.listener;

import com.hegi64.iceBoatTimeTrial.editor.TrackEditorSessionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TrackEditorListener implements Listener {
    private final TrackEditorSessionService sessions;

    public TrackEditorListener(TrackEditorSessionService sessions) {
        this.sessions = sessions;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sessions.stopSession(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        sessions.stopSession(event.getPlayer().getUniqueId());
    }
}

