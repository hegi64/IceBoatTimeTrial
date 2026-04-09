package com.hegi64.iceBoatTimeTrial.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface GuiAction {
    void execute(Player player, InventoryClickEvent event, GuiContext context);
}

