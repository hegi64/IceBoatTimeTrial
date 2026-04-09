package com.hegi64.iceBoatTimeTrial.gui;

import com.hegi64.iceBoatTimeTrial.util.Chat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiItems {
    private GuiItems() {
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(Chat.color(line));
                }
                meta.setLore(colored);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}

