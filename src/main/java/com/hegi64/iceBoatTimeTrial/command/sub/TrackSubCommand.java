package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.RegionBox;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TrackSubCommand implements SubCommand {
    private final PluginConfig config;
    private final TrackService trackService;
    private final WorldEditPlugin worldEdit;

    public TrackSubCommand(PluginConfig config, TrackService trackService, WorldEditPlugin worldEdit) {
        this.config = config;
        this.trackService = trackService;
        this.worldEdit = worldEdit;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            send(sender, "&cUsage: /ibt track <create|delete|enable|disable|setstart|setcp1|setcp2|setdir|info> <name>");
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        String trackName = args[2];

        try {
            return switch (action) {
                case "create" -> createTrack(sender, trackName);
                case "delete" -> deleteTrack(sender, trackName);
                case "enable" -> toggleTrack(sender, trackName, true);
                case "disable" -> toggleTrack(sender, trackName, false);
                case "setstart" -> setRegion(sender, trackName, RegionType.START_FINISH);
                case "setcp1" -> setRegion(sender, trackName, RegionType.CHECKPOINT_1);
                case "setcp2" -> setRegion(sender, trackName, RegionType.CHECKPOINT_2);
                case "setdir" -> setDirection(sender, args);
                case "info" -> info(sender, trackName);
                default -> {
                    send(sender, "&cUnknown track action.");
                    yield true;
                }
            };
        } catch (IllegalStateException exception) {
            send(sender, "&c" + exception.getMessage());
            return true;
        } catch (SQLException exception) {
            send(sender, "&cDatabase error: " + exception.getMessage());
            return true;
        }
    }

    @Override
    public String getName() {
        return "track";
    }

    @Override
    public String getDescription() {
        return "/ibt track <create|delete|enable|disable|setstart|setcp1|setcp2|setdir|info> <name>";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.ADMIN);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return partial(args[1], List.of("create", "delete", "enable", "disable", "setstart", "setcp1", "setcp2", "setdir", "info"));
        }
        if (args.length == 3 && !"create".equalsIgnoreCase(args[1])) {
            return partial(args[2], trackService.allTracks().stream().map(Track::getName).toList());
        }
        if (args.length == 4 && "setdir".equalsIgnoreCase(args[1])) {
            return partial(args[3], Arrays.asList("start", "cp1", "cp2"));
        }
        return List.of();
    }

    private boolean createTrack(CommandSender sender, String name) throws SQLException {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can create tracks.");
            return true;
        }
        if (trackService.findByName(name).isPresent()) {
            send(sender, "&cTrack already exists.");
            return true;
        }
        Track created = trackService.createTrack(name, player.getWorld().getName());
        send(sender, "&aTrack created: &f" + created.getName());
        return true;
    }

    private boolean deleteTrack(CommandSender sender, String name) throws SQLException {
        if (!trackService.deleteTrack(name)) {
            send(sender, "&cTrack not found.");
            return true;
        }
        send(sender, "&aTrack deleted.");
        return true;
    }

    private boolean toggleTrack(CommandSender sender, String name, boolean enabled) throws SQLException {
        Optional<Track> trackOpt = trackService.findByName(name);
        if (trackOpt.isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }
        trackService.setEnabled(trackOpt.get(), enabled);
        send(sender, enabled ? "&aTrack enabled." : "&eTrack disabled.");
        return true;
    }

    private boolean setRegion(CommandSender sender, String trackName, RegionType type) throws SQLException {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can set regions.");
            return true;
        }
        Optional<Track> trackOpt = trackService.findByName(trackName);
        if (trackOpt.isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }
        Track track = trackOpt.get();
        if (!track.getWorld().equals(player.getWorld().getName())) {
            send(sender, "&cTrack world mismatch. Track world: " + track.getWorld());
            return true;
        }

        RegionBox box = regionFromSelection(player, track.getWorld());
        trackService.setRegion(track, type, box);
        send(sender, "&aUpdated " + type.name() + " region for track &f" + track.getName());
        return true;
    }

    private boolean setDirection(CommandSender sender, String[] args) throws SQLException {
        if (args.length < 7) {
            send(sender, "&cUsage: /ibt track setdir <name> <start|cp1|cp2> <x> <y> <z> [threshold]");
            return true;
        }
        String trackName = args[2];
        Optional<Track> trackOpt = trackService.findByName(trackName);
        if (trackOpt.isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }
        RegionType type = parseRegionType(args[3]);
        if (type == null) {
            send(sender, "&cUnknown region type. Use start|cp1|cp2");
            return true;
        }

        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args[4]);
            y = Double.parseDouble(args[5]);
            z = Double.parseDouble(args[6]);
        } catch (NumberFormatException exception) {
            send(sender, "&cDirection must be numeric.");
            return true;
        }

        Double threshold = null;
        if (args.length >= 8) {
            try {
                threshold = Double.parseDouble(args[7]);
            } catch (NumberFormatException exception) {
                send(sender, "&cThreshold must be numeric.");
                return true;
            }
        }

        trackService.setDirection(trackOpt.get(), type, x, y, z, threshold);
        send(sender, "&aDirection updated.");
        return true;
    }

    private boolean info(CommandSender sender, String trackName) {
        Optional<Track> trackOpt = trackService.findByName(trackName);
        if (trackOpt.isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }

        Track track = trackOpt.get();
        send(sender, "&bTrack: &f" + track.getName());
        send(sender, "&7World: &f" + track.getWorld());
        send(sender, "&7Enabled: &f" + track.isEnabled());
        send(sender, "&7Complete: &f" + track.isComplete());
        send(sender, "&7Start: &f" + (track.getRegion(RegionType.START_FINISH) != null));
        send(sender, "&7CP1: &f" + (track.getRegion(RegionType.CHECKPOINT_1) != null));
        send(sender, "&7CP2: &f" + (track.getRegion(RegionType.CHECKPOINT_2) != null));
        return true;
    }

    private RegionType parseRegionType(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "start", "start_finish", "startfinish" -> RegionType.START_FINISH;
            case "cp1", "checkpoint1", "checkpoint_1" -> RegionType.CHECKPOINT_1;
            case "cp2", "checkpoint2", "checkpoint_2" -> RegionType.CHECKPOINT_2;
            default -> null;
        };
    }

    private RegionBox regionFromSelection(Player player, String expectedWorld) {
        LocalSession session = worldEdit.getSession(player);
        Region region;
        try {
            region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException exception) {
            throw new IllegalStateException("WorldEdit selection is incomplete.");
        }

        if (!player.getWorld().getName().equals(expectedWorld)) {
            throw new IllegalStateException("Selection world mismatch.");
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new RegionBox(
                expectedWorld,
                min.x(), min.y(), min.z(),
                max.x(), max.y(), max.z(),
                null, null, null, null
        );
    }

    private List<String> partial(String token, List<String> candidates) {
        String lower = token.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}
