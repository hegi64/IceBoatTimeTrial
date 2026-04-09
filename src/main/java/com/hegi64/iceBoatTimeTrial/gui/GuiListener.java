package com.hegi64.iceBoatTimeTrial.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {
    private final GuiRegistry registry;
    private final GuiSessionService sessions;

    public GuiListener(GuiRegistry registry, GuiSessionService sessions) {
        this.registry = registry;
        this.sessions = sessions;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        GuiSession session = sessions.get(player).orElse(null);
        if (session == null) {
            return;
        }
        if (event.getView().getTopInventory() != session.inventory()) {
            return;
        }

        event.setCancelled(true);
        registry.get(session.menu().id()).ifPresent(menu -> menu.handleClick(player, event, session));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        GuiSession session = sessions.get(player).orElse(null);
        if (session == null) {
            return;
        }
        if (event.getView().getTopInventory() == session.inventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        GuiSession session = sessions.get(player).orElse(null);
        if (session == null) {
            return;
        }
        if (event.getInventory() != session.inventory()) {
            return;
        }

        sessions.close(player);
        registry.get(session.menu().id()).ifPresent(menu -> menu.onClose(player, session.context()));
    }
}

