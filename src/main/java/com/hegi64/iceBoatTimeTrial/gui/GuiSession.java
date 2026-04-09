package com.hegi64.iceBoatTimeTrial.gui;

import org.bukkit.inventory.Inventory;

public record GuiSession(GuiMenu menu, Inventory inventory, GuiContext context) {
}

