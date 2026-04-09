package com.hegi64.iceBoatTimeTrial.gui.menu;

import com.hegi64.iceBoatTimeTrial.gui.GuiButton;
import com.hegi64.iceBoatTimeTrial.gui.GuiContext;
import com.hegi64.iceBoatTimeTrial.gui.GuiItems;
import com.hegi64.iceBoatTimeTrial.gui.GuiMenu;
import com.hegi64.iceBoatTimeTrial.gui.GuiSessionService;
import com.hegi64.iceBoatTimeTrial.service.BossBarService;
import com.hegi64.iceBoatTimeTrial.service.settings.PlayerSettings;
import com.hegi64.iceBoatTimeTrial.service.settings.PlayerSettingsService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class SettingsMenu extends GuiMenu {
    private final PlayerSettingsService settingsService;
    private final BossBarService bossBarService;
    private final GuiSessionService sessions;

    public SettingsMenu(PlayerSettingsService settingsService, BossBarService bossBarService, GuiSessionService sessions) {
        super("settings", "IBT Settings", 27);
        this.settingsService = settingsService;
        this.bossBarService = bossBarService;
        this.sessions = sessions;
    }

    @Override
    protected void build(Player player, GuiContext context, Map<Integer, GuiButton> buttons) {
        PlayerSettings settings = settingsService.get(player.getUniqueId());

        buttons.put(11, GuiButton.of(
                GuiItems.item(Material.CLOCK,
                        "&bBossbar: " + bool(settings.bossbarEnabled()),
                        List.of("&7Toggle your run bossbar visibility.")),
                (p, event, ctx) -> {
                    settingsService.setBossbarEnabled(p.getUniqueId(), !settingsService.get(p.getUniqueId()).bossbarEnabled());
                    if (!settingsService.get(p.getUniqueId()).bossbarEnabled()) {
                        bossBarService.hide(p);
                    }
                    open(p, sessions, ctx);
                }
        ));

        buttons.put(15, GuiButton.of(
                GuiItems.item(Material.BOOK,
                        "&bMessage Verbosity: &f" + settings.verbosity().name(),
                        List.of("&7Cycles: DETAILED -> NORMAL -> MINIMAL")),
                (p, event, ctx) -> {
                    settingsService.cycleVerbosity(p.getUniqueId());
                    open(p, sessions, ctx);
                }
        ));

        buttons.put(22, GuiButton.of(
                GuiItems.item(Material.BARRIER, "&cClose", List.of()),
                (p, event, ctx) -> p.closeInventory()
        ));
    }

    private String bool(boolean value) {
        return value ? "&aON" : "&cOFF";
    }
}
