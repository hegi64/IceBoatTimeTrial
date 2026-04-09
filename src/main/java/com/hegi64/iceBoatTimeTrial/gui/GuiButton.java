package com.hegi64.iceBoatTimeTrial.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiPredicate;

public class GuiButton {
    private final ItemStack item;
    private final GuiAction action;
    private final BiPredicate<Player, GuiContext> visibility;

    public GuiButton(ItemStack item, GuiAction action) {
        this(item, action, (player, context) -> true);
    }

    public GuiButton(ItemStack item, GuiAction action, BiPredicate<Player, GuiContext> visibility) {
        this.item = item;
        this.action = action;
        this.visibility = visibility;
    }

    public static GuiButton of(ItemStack item, GuiAction action) {
        return new GuiButton(item, action);
    }

    public ItemStack item() {
        return item;
    }

    public GuiAction action() {
        return action;
    }

    public boolean isVisible(Player player, GuiContext context) {
        return visibility.test(player, context);
    }
}
