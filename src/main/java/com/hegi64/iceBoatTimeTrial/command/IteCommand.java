package com.hegi64.iceBoatTimeTrial.command;

import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.editor.TrackEditorSession;
import com.hegi64.iceBoatTimeTrial.editor.TrackEditorSessionService;
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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class IteCommand implements CommandExecutor, TabCompleter {
    private final PluginConfig config;
    private final TrackService trackService;
    private final TrackEditorSessionService sessions;
    private final WorldEditPlugin worldEdit;

    public IteCommand(PluginConfig config, TrackService trackService, TrackEditorSessionService sessions, WorldEditPlugin worldEdit) {
        this.config = config;
        this.trackService = trackService;
        this.sessions = sessions;
        this.worldEdit = worldEdit;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission(Permissions.EDITOR_USE)) {
            send(sender, "&cMissing permission: " + Permissions.EDITOR_USE);
            return true;
        }
        if (args.length == 0) {
            help(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            return switch (sub) {
                case "create" -> create(player, args);
                case "edit" -> edit(player, args);
                case "forceedit" -> forceEdit(player, args);
                case "forcestop" -> forceStop(player, args);
                case "stopedit" -> stopEdit(player, args);
                case "setstart" -> setRegionInSession(player, RegionType.START_FINISH);
                case "setcp1" -> setRegionInSession(player, RegionType.CHECKPOINT_1);
                case "setcp2" -> setRegionInSession(player, RegionType.CHECKPOINT_2);
                case "setdir" -> setDirectionInSession(player, args);
                case "rename" -> renameInSession(player, args);
                case "status", "summary" -> summary(player);
                case "validate" -> validate(player);
                case "help" -> {
                    help(player);
                    yield true;
                }
                default -> {
                    send(player, "&cUnknown subcommand.");
                    help(player);
                    yield true;
                }
            };
        } catch (SQLException exception) {
            send(player, "&cDatabase error: " + exception.getMessage());
            return true;
        } catch (IllegalStateException exception) {
            send(player, "&c" + exception.getMessage());
            return true;
        }
    }

    private boolean create(Player player, String[] args) throws SQLException {
        if (!player.hasPermission(Permissions.EDITOR_MODIFY)) {
            send(player, "&cMissing permission: " + Permissions.EDITOR_MODIFY);
            return true;
        }
        if (args.length < 2) {
            send(player, "&eUsage: /ite create <name>");
            return true;
        }
        String name = args[1];
        if (trackService.findByName(name).isPresent()) {
            send(player, "&cTrack already exists.");
            return true;
        }
        Track track = trackService.createTrack(name, player.getWorld().getName());
        send(player, "&aCreated track &f" + track.getName() + "&a in world &f" + track.getWorld());
        return true;
    }

    private boolean edit(Player player, String[] args) {
        if (args.length < 2) {
            send(player, "&eUsage: /ite edit <trackname>");
            return true;
        }
        Optional<Track> trackOpt = trackService.findByName(args[1]);
        if (trackOpt.isEmpty()) {
            send(player, "&cTrack not found.");
            return true;
        }
        Track track = trackOpt.get();

        Optional<TrackEditorSession> current = sessions.getByPlayer(player.getUniqueId());
        if (current.isPresent() && !current.get().trackUuid().equals(track.getUuid())) {
            send(player, "&cYou are already editing another track. Use /ite stopedit first.");
            return true;
        }

        Optional<UUID> locker = sessions.getEditorForTrack(track.getUuid());
        if (locker.isPresent() && !locker.get().equals(player.getUniqueId())) {
            Player editing = Bukkit.getPlayer(locker.get());
            String name = editing != null ? editing.getName() : locker.get().toString();
            send(player, "&cTrack is currently edited by &f" + name);
            return true;
        }

        if (!sessions.startSession(player.getUniqueId(), track)) {
            send(player, "&cCould not enter editor mode.");
            return true;
        }
        send(player, "&aEditor mode enabled for track &f" + track.getName());
        summary(player);
        return true;
    }

    private boolean forceEdit(Player player, String[] args) {
        if (!player.hasPermission(Permissions.EDITOR_ADMIN)) {
            send(player, "&cMissing permission: " + Permissions.EDITOR_ADMIN);
            return true;
        }
        if (args.length < 2) {
            send(player, "&eUsage: /ite forceedit <trackname>");
            return true;
        }
        Optional<Track> trackOpt = trackService.findByName(args[1]);
        if (trackOpt.isEmpty()) {
            send(player, "&cTrack not found.");
            return true;
        }
        Track track = trackOpt.get();

        sessions.forceStopTrack(track.getUuid());
        sessions.stopSession(player.getUniqueId());
        sessions.startSession(player.getUniqueId(), track);
        send(player, "&aForce-enabled editor mode for &f" + track.getName());
        summary(player);
        return true;
    }

    private boolean forceStop(Player player, String[] args) {
        if (!player.hasPermission(Permissions.EDITOR_ADMIN)) {
            send(player, "&cMissing permission: " + Permissions.EDITOR_ADMIN);
            return true;
        }
        if (args.length < 2) {
            send(player, "&eUsage: /ite forcestop <trackname>");
            return true;
        }
        Optional<Track> trackOpt = trackService.findByName(args[1]);
        if (trackOpt.isEmpty()) {
            send(player, "&cTrack not found.");
            return true;
        }
        Track track = trackOpt.get();
        Optional<UUID> stopped = sessions.forceStopTrack(track.getUuid());
        if (stopped.isEmpty()) {
            send(player, "&eNo active editor for this track.");
            return true;
        }
        send(player, "&aForce-stopped editor session for &f" + track.getName());
        return true;
    }

    private boolean stopEdit(Player player, String[] args) {
        Optional<TrackEditorSession> sessionOpt = sessions.getByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            send(player, "&cYou are not in editor mode.");
            return true;
        }
        TrackEditorSession session = sessionOpt.get();
        if (args.length >= 2) {
            Optional<Track> byName = trackService.findByName(args[1]);
            if (byName.isEmpty() || !byName.get().getUuid().equals(session.trackUuid())) {
                send(player, "&cYou are not editing that track.");
                return true;
            }
        }
        sessions.stopSession(player.getUniqueId());
        send(player, "&eEditor mode disabled.");
        return true;
    }

    private boolean setRegionInSession(Player player, RegionType type) throws SQLException {
        if (!player.hasPermission(Permissions.EDITOR_MODIFY)) {
            send(player, "&cMissing permission: " + Permissions.EDITOR_MODIFY);
            return true;
        }
        Track track = requireEditedTrack(player);
        if (!track.getWorld().equals(player.getWorld().getName())) {
            send(player, "&cTrack world mismatch. Track world is &f" + track.getWorld());
            return true;
        }
        RegionBox box = regionFromSelection(player, track.getWorld());
        trackService.setRegion(track, type, box);
        send(player, "&aUpdated &f" + type.name() + " &afor &f" + track.getName());
        summary(player);
        return true;
    }

    private boolean setDirectionInSession(Player player, String[] args) throws SQLException {
        if (!player.hasPermission(Permissions.EDITOR_MODIFY)) {
            send(player, "&cMissing permission: " + Permissions.EDITOR_MODIFY);
            return true;
        }
        if (args.length < 5) {
            send(player, "&eUsage: /ite setdir <start|cp1|cp2> <x> <y> <z> [threshold]");
            return true;
        }
        Track track = requireEditedTrack(player);
        RegionType type = parseRegionType(args[1]);
        if (type == null) {
            send(player, "&cUnknown region type. Use start|cp1|cp2");
            return true;
        }

        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args[2]);
            y = Double.parseDouble(args[3]);
            z = Double.parseDouble(args[4]);
        } catch (NumberFormatException exception) {
            send(player, "&cDirection must be numeric.");
            return true;
        }
        Double threshold = null;
        if (args.length >= 6) {
            try {
                threshold = Double.parseDouble(args[5]);
            } catch (NumberFormatException exception) {
                send(player, "&cThreshold must be numeric.");
                return true;
            }
        }

        trackService.setDirection(track, type, x, y, z, threshold);
        send(player, "&aDirection updated for &f" + type.name());
        summary(player);
        return true;
    }

    private boolean renameInSession(Player player, String[] args) throws SQLException {
        if (!player.hasPermission(Permissions.EDITOR_MODIFY)) {
            send(player, "&cMissing permission: " + Permissions.EDITOR_MODIFY);
            return true;
        }
        if (args.length < 2) {
            send(player, "&eUsage: /ite rename <newname>");
            return true;
        }
        Track track = requireEditedTrack(player);
        String old = track.getName();
        String next = args[1];
        if (!trackService.renameTrack(track, next)) {
            send(player, "&cCould not rename. Name may already exist.");
            return true;
        }
        send(player, "&aRenamed track: &f" + old + " &7-> &f" + next);
        summary(player);
        return true;
    }

    private boolean validate(Player player) {
        Track track = requireEditedTrack(player);
        boolean start = track.getRegion(RegionType.START_FINISH) != null;
        boolean cp1 = track.getRegion(RegionType.CHECKPOINT_1) != null;
        boolean cp2 = track.getRegion(RegionType.CHECKPOINT_2) != null;

        boolean dStart = hasDirection(track.getRegion(RegionType.START_FINISH));
        boolean dCp1 = hasDirection(track.getRegion(RegionType.CHECKPOINT_1));
        boolean dCp2 = hasDirection(track.getRegion(RegionType.CHECKPOINT_2));

        send(player, "&bValidation for &f" + track.getName());
        send(player, "&7Start/Finish: " + boolColor(start));
        send(player, "&7Checkpoint 1: " + boolColor(cp1));
        send(player, "&7Checkpoint 2: " + boolColor(cp2));

        if (config.enforceDirection()) {
            send(player, "&7Direction Start: " + boolColor(dStart));
            send(player, "&7Direction CP1: " + boolColor(dCp1));
            send(player, "&7Direction CP2: " + boolColor(dCp2));
        } else {
            send(player, "&eDirection enforcement is disabled globally.");
        }

        boolean valid = start && cp1 && cp2 && (!config.enforceDirection() || (dStart && dCp1 && dCp2));
        send(player, valid ? "&aTrack is valid for racing." : "&cTrack is not fully configured yet.");
        return true;
    }

    private boolean summary(Player player) {
        Track track = requireEditedTrack(player);
        boolean start = track.getRegion(RegionType.START_FINISH) != null;
        boolean cp1 = track.getRegion(RegionType.CHECKPOINT_1) != null;
        boolean cp2 = track.getRegion(RegionType.CHECKPOINT_2) != null;
        boolean dStart = hasDirection(track.getRegion(RegionType.START_FINISH));
        boolean dCp1 = hasDirection(track.getRegion(RegionType.CHECKPOINT_1));
        boolean dCp2 = hasDirection(track.getRegion(RegionType.CHECKPOINT_2));

        send(player, "&bEditing: &f" + track.getName() + " &7(world: " + track.getWorld() + ")");
        send(player, "&7Start/Finish: " + boolColor(start));
        send(player, "&7Checkpoint 1: " + boolColor(cp1));
        send(player, "&7Checkpoint 2: " + boolColor(cp2));

        if (config.enforceDirection()) {
            send(player, "&7Direction Start: " + boolColor(dStart));
            send(player, "&7Direction CP1: " + boolColor(dCp1));
            send(player, "&7Direction CP2: " + boolColor(dCp2));
        }

        if (!start) {
            send(player, "&eNext: set start with &f/ite setstart");
        } else if (!cp1) {
            send(player, "&eNext: set cp1 with &f/ite setcp1");
        } else if (!cp2) {
            send(player, "&eNext: set cp2 with &f/ite setcp2");
        } else if (config.enforceDirection() && !dStart) {
            send(player, "&eNext: set start direction with &f/ite setdir start <x> <y> <z> [threshold]");
        } else if (config.enforceDirection() && !dCp1) {
            send(player, "&eNext: set cp1 direction with &f/ite setdir cp1 <x> <y> <z> [threshold]");
        } else if (config.enforceDirection() && !dCp2) {
            send(player, "&eNext: set cp2 direction with &f/ite setdir cp2 <x> <y> <z> [threshold]");
        } else {
            send(player, "&aAll required editor fields are set. Use &f/ite validate &ato confirm.");
        }
        return true;
    }

    private void help(CommandSender sender) {
        send(sender, "&bTrack editor commands:");
        send(sender, "&e/ite create <name>");
        send(sender, "&e/ite edit <trackname>");
        send(sender, "&e/ite stopedit [trackname]");
        send(sender, "&e/ite setstart | setcp1 | setcp2");
        send(sender, "&e/ite setdir <start|cp1|cp2> <x> <y> <z> [threshold]");
        send(sender, "&e/ite rename <newname>");
        send(sender, "&e/ite status | /ite validate");
        if (sender.hasPermission(Permissions.EDITOR_ADMIN)) {
            send(sender, "&e/ite forceedit <trackname>");
            send(sender, "&e/ite forcestop <trackname>");
        }
    }

    private boolean hasDirection(RegionBox box) {
        return box != null && box.dirX() != null && box.dirY() != null && box.dirZ() != null;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("create", "edit", "stopedit", "setstart", "setcp1", "setcp2", "setdir", "rename", "status", "summary", "validate", "help"));
            if (sender.hasPermission(Permissions.EDITOR_ADMIN)) {
                base.add("forceedit");
                base.add("forcestop");
            }
            return partial(args[0], base);
        }
        if ((args.length == 2) && ("edit".equalsIgnoreCase(args[0])
                || "stopedit".equalsIgnoreCase(args[0])
                || "forceedit".equalsIgnoreCase(args[0])
                || "forcestop".equalsIgnoreCase(args[0]))) {
            return partial(args[1], trackService.allTracks().stream().map(Track::getName).toList());
        }
        if (args.length == 2 && "setdir".equalsIgnoreCase(args[0])) {
            return partial(args[1], Arrays.asList("start", "cp1", "cp2"));
        }
        return List.of();
    }

    private List<String> partial(String token, List<String> candidates) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(candidate);
            }
        }
        return out;
    }

    private Track requireEditedTrack(Player player) {
        Optional<TrackEditorSession> sessionOpt = sessions.getByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            throw new IllegalStateException("You are not in editor mode. Use /ite edit <trackname>");
        }
        UUID trackUuid = sessionOpt.get().trackUuid();
        return trackService.allTracks().stream()
                .filter(track -> track.getUuid().equals(trackUuid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Edited track no longer exists."));
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

    private String boolColor(boolean value) {
        return value ? "&aSET" : "&cMISSING";
    }
}
