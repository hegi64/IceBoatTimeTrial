package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.hologram.HologramService;
import com.hegi64.iceBoatTimeTrial.hologram.HologramUpdater;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

public class HologramSubCommand implements SubCommand {
    private final PluginConfig config;
    private final TrackService trackService;
    private final HologramService hologramService;
    private final HologramUpdater hologramUpdater;

    public HologramSubCommand(PluginConfig config, TrackService trackService, HologramService hologramService, HologramUpdater hologramUpdater) {
        this.config = config;
        this.trackService = trackService;
        this.hologramService = hologramService;
        this.hologramUpdater = hologramUpdater;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 4) {
            send(sender, "&eUsage: /ibt hologram <place|remove> <track> <type>");
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        String track = args[2];
        String type = args[3].toLowerCase(Locale.ROOT);

        if (!HologramUpdater.supportedTypes().contains(type)) {
            send(sender, "&cUnknown hologram type: " + type);
            return true;
        }
        if (trackService.findByName(track).isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }

        return switch (action) {
            case "place" -> place(sender, track, type);
            case "remove" -> remove(sender, track, type);
            default -> {
                send(sender, "&cUnknown hologram action.");
                yield true;
            }
        };
    }

    @Override
    public String getName() {
        return "hologram";
    }

    @Override
    public String getDescription() {
        return "/ibt hologram <place|remove> <track> <type>";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.ADMIN);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return partial(args[1], List.of("place", "remove"));
        }
        if (args.length == 3) {
            return partial(args[2], trackService.allTracks().stream().map(Track::getName).toList());
        }
        if (args.length == 4) {
            return partial(args[3], HologramUpdater.supportedTypes());
        }
        return List.of();
    }

    private boolean place(CommandSender sender, String track, String type) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can place holograms.");
            return true;
        }
        if (hologramService.hasHologram(track, type)) {
            send(sender, "&cHologram already exists for this track and type.");
            return true;
        }

        hologramService.placeHologram(track, type, player.getLocation());
        hologramUpdater.update(track, type);
        send(sender, "&aHologram placed for track &f" + track + " &7type &f" + type);
        return true;
    }

    private boolean remove(CommandSender sender, String track, String type) {
        if (!hologramService.hasHologram(track, type)) {
            send(sender, "&cNo hologram exists for this track and type.");
            return true;
        }

        hologramService.removeHologram(track, type);
        send(sender, "&aHologram removed for track &f" + track + " &7type &f" + type);
        return true;
    }

    private List<String> partial(String token, List<String> candidates) {
        String lower = token.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}
