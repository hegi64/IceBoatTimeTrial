package com.hegi64.iceBoatTimeTrial.gui.menu;

import com.hegi64.iceBoatTimeTrial.gui.GuiButton;
import com.hegi64.iceBoatTimeTrial.gui.GuiContext;
import com.hegi64.iceBoatTimeTrial.gui.GuiItems;
import com.hegi64.iceBoatTimeTrial.gui.GuiMenu;
import com.hegi64.iceBoatTimeTrial.gui.GuiSessionService;
import com.hegi64.iceBoatTimeTrial.gui.Pagination;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackBrowserMenu extends GuiMenu {
    private static final int PAGE_SIZE = 45;

    private final TrackService trackService;
    private final TrackDetailsMenu detailsMenu;
    private final GuiSessionService sessions;

    public TrackBrowserMenu(TrackService trackService, TrackDetailsMenu detailsMenu, GuiSessionService sessions) {
        super("track_browser", "Track Browser", 54);
        this.trackService = trackService;
        this.detailsMenu = detailsMenu;
        this.sessions = sessions;
    }

    @Override
    protected void build(Player player, GuiContext context, Map<Integer, GuiButton> buttons) {
        List<Track> tracks = new ArrayList<>(trackService.allTracks());
        tracks.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Integer storedPage = context.get("page", Integer.class);
        int page = storedPage == null ? 0 : Math.max(0, storedPage);
        int lastPage = Pagination.lastPageIndex(tracks.size(), PAGE_SIZE);
        page = Math.min(page, lastPage);
        final int currentPage = page;

        List<Track> pageTracks = Pagination.page(tracks, page, PAGE_SIZE);
        for (int i = 0; i < pageTracks.size(); i++) {
            Track track = pageTracks.get(i);
            int slot = i;
            buttons.put(slot, GuiButton.of(
                    GuiItems.item(
                            Material.ICE,
                            "&b" + track.getName(),
                            List.of("&7World: &f" + track.getWorld(), "&7Enabled: &f" + track.isEnabled(), "&7Click for details")
                    ),
                    (p, event, ctx) -> {
                        GuiContext detailsCtx = new GuiContext()
                                .put("trackUuid", track.getUuid())
                                .put("page", currentPage)
                                .put("browser", this);
                        detailsMenu.open(p, sessions, detailsCtx);
                    }
            ));
        }

        if (page > 0) {
            int prevPage = page - 1;
            buttons.put(45, GuiButton.of(
                    GuiItems.item(Material.ARROW, "&ePrevious page", List.of("&7Go to page " + (prevPage + 1))),
                    (p, event, ctx) -> open(p, sessions, new GuiContext().put("page", prevPage))
            ));
        }

        if (page < lastPage) {
            int nextPage = page + 1;
            buttons.put(53, GuiButton.of(
                    GuiItems.item(Material.ARROW, "&eNext page", List.of("&7Go to page " + (nextPage + 1))),
                    (p, event, ctx) -> open(p, sessions, new GuiContext().put("page", nextPage))
            ));
        }

        buttons.put(49, GuiButton.of(
                GuiItems.item(Material.BARRIER, "&cClose", List.of()),
                (p, event, ctx) -> p.closeInventory()
        ));
    }
}
