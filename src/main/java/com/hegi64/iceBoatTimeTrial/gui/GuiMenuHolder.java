package com.hegi64.iceBoatTimeTrial.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record GuiMenuHolder(String menuId) implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null;
    }
}

