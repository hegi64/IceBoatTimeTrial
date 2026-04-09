package com.hegi64.iceBoatTimeTrial.listener;

import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.service.BossBarService;
import com.hegi64.iceBoatTimeTrial.service.RunService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class RunListener implements Listener {
    private final RunService runService;
    private final BossBarService bossBarService;
    private final PluginConfig config;

    public RunListener(RunService runService, BossBarService bossBarService, PluginConfig config) {
        this.runService = runService;
        this.bossBarService = bossBarService;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        runService.handleMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) {
            return;
        }
        if (!config.abortOnVehicleExit()) {
            return;
        }
        runService.abortRun(player, config.runAbortedMessage());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!config.abortOnTeleport()) {
            return;
        }
        runService.abortRun(event.getPlayer(), config.runAbortedMessage());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!config.abortOnWorldChange()) {
            return;
        }
        runService.abortRun(event.getPlayer(), config.runAbortedMessage());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        runService.abortRunSilent(event.getPlayer().getUniqueId());
        bossBarService.hide(event.getPlayer());
    }
}
