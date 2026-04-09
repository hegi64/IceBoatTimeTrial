package com.hegi64.iceBoatTimeTrial.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class GuiMenu {
    private final String id;
    private final String title;
    private final int size;

    protected GuiMenu(String id, String title, int size) {
        this.id = id;
        this.title = title;
        this.size = size;
    }

    public String id() {
        return id;
    }

    public int size() {
        return size;
    }

    public String title() {
        return title;
    }

    public void open(Player player, GuiSessionService sessionService, GuiContext context) {
        Inventory inventory = Bukkit.createInventory(new GuiMenuHolder(id), size, title);
        Map<Integer, GuiButton> buttons = new LinkedHashMap<>();
        build(player, context, buttons);
        for (Map.Entry<Integer, GuiButton> entry : buttons.entrySet()) {
            int slot = entry.getKey();
            GuiButton button = entry.getValue();
            if (slot < 0 || slot >= size || !button.isVisible(player, context)) {
                continue;
            }
            inventory.setItem(slot, button.item());
        }
        sessionService.open(player, new GuiSession(this, inventory, context));
        player.openInventory(inventory);
    }

    public void handleClick(Player player, InventoryClickEvent event, GuiSession session) {
        if (event.getRawSlot() < 0 || event.getRawSlot() >= session.inventory().getSize()) {
            return;
        }
        Map<Integer, GuiButton> buttons = new LinkedHashMap<>();
        build(player, session.context(), buttons);
        GuiButton button = buttons.get(event.getRawSlot());
        if (button != null && button.isVisible(player, session.context())) {
            button.action().execute(player, event, session.context());
        }
    }

    protected abstract void build(Player player, GuiContext context, Map<Integer, GuiButton> buttons);

    public void onClose(Player player, GuiContext context) {
        // Optional hook for subclasses.
    }
}

