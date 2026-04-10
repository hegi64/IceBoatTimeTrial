package com.hegi64.iceBoatTimeTrial.gui.menu;

import com.hegi64.iceBoatTimeTrial.gui.GuiButton;
import com.hegi64.iceBoatTimeTrial.gui.GuiContext;
import com.hegi64.iceBoatTimeTrial.gui.GuiItems;
import com.hegi64.iceBoatTimeTrial.gui.GuiMenu;
import com.hegi64.iceBoatTimeTrial.gui.GuiSessionService;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TrackDetailsMenu extends GuiMenu {
    private final TrackService trackService;
    private final GuiSessionService sessions;

    public TrackDetailsMenu(TrackService trackService, GuiSessionService sessions) {
        super("track_details", "Track Details", 27);
        this.trackService = trackService;
        this.sessions = sessions;
    }

    @Override
    protected void build(Player player, GuiContext context, Map<Integer, GuiButton> buttons) {
        UUID trackUuid = context.get("trackUuid", UUID.class);
        if (trackUuid == null) {
            return;
        }
        Optional<Track> trackOpt = trackService.allTracks().stream().filter(t -> t.getUuid().equals(trackUuid)).findFirst();
        if (trackOpt.isEmpty()) {
            return;
        }
        Track track = trackOpt.get();

        buttons.put(11, GuiButton.of(
                GuiItems.item(Material.NAME_TAG, "&b" + track.getName(), List.of("&7World: &f" + track.getWorld(), "&7Enabled: &f" + track.isEnabled())),
                (p, event, ctx) -> {
                }
        ));

        if (track.hasSpawn()) {
            buttons.put(13, GuiButton.of(
                    GuiItems.item(Material.ENDER_PEARL,
                            ChatColor.DARK_PURPLE + "Teleport to " + track.getName(), List.of(ChatColor.GRAY + "Click to teleport to track spawn.")),
                    (p, event, ctx) -> {
                        p.teleport(track.getSpawn());
                        p.closeInventory();
                    }
            ));
        } else {
            buttons.put(13, GuiButton.of(
                    GuiItems.item(Material.STRUCTURE_VOID,
                            ChatColor.RED + "No spawn set", List.of(ChatColor.GRAY + "Could not find a spawn location for this track.")),
                    (p, event, ctx) -> {
                    }
            ));
        }


        buttons.put(15, GuiButton.of(
                GuiItems.item(Material.COMPASS,
                        "&bTrack Validation",
                        List.of(
                                "&7Start/Finish: " + flag(track.getRegion(RegionType.START_FINISH) != null),
                                "&7Checkpoint 1: " + flag(track.getRegion(RegionType.CHECKPOINT_1) != null),
                                "&7Checkpoint 2: " + flag(track.getRegion(RegionType.CHECKPOINT_2) != null)
                        )),
                (p, event, ctx) -> {
                }
        ));

        buttons.put(22, GuiButton.of(
                GuiItems.item(Material.ARROW, "&eBack", List.of("&7Return to track browser.")),
                (p, event, ctx) -> {
                    GuiContext back = new GuiContext().put("page", context.get("page", Integer.class));
                    TrackBrowserMenu browser = context.get("browser", TrackBrowserMenu.class);
                    if (browser != null) {
                        browser.open(p, sessions, back);
                    }
                }
        ));
    }

    private String flag(boolean ok) {
        return ok ? "&aSET" : "&cMISSING";
    }
}

