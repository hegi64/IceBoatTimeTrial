package com.hegi64.iceBoatTimeTrial.command.sub.editor;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class EditorCommandContext {
    private final PluginConfig config;
    private final TrackService trackService;
    private final TrackEditorSessionService sessions;
    private final WorldEditPlugin worldEdit;

    public EditorCommandContext(PluginConfig config,
                                TrackService trackService,
                                TrackEditorSessionService sessions,
                                WorldEditPlugin worldEdit) {
        this.config = config;
        this.trackService = trackService;
        this.sessions = sessions;
        this.worldEdit = worldEdit;
    }

    public PluginConfig config() {
        return config;
    }

    public TrackService trackService() {
        return trackService;
    }

    public TrackEditorSessionService sessions() {
        return sessions;
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }

    public Track requireEditedTrack(Player player) {
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

    public RegionType parseRegionType(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "start", "start_finish", "startfinish" -> RegionType.START_FINISH;
            case "cp1", "checkpoint1", "checkpoint_1" -> RegionType.CHECKPOINT_1;
            case "cp2", "checkpoint2", "checkpoint_2" -> RegionType.CHECKPOINT_2;
            default -> null;
        };
    }

    public RegionBox regionFromSelection(Player player, String expectedWorld) {
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

    public List<String> partial(String token, List<String> candidates) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(candidate);
            }
        }
        return out;
    }

    public boolean summary(Player player) {
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

    public boolean validate(Player player) {
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

    public void help(CommandSender sender) {
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

    public Optional<Track> findTrackByName(String name) {
        return trackService.findByName(name);
    }

    public String editorName(UUID editorUuid) {
        Player editing = Bukkit.getPlayer(editorUuid);
        return editing != null ? editing.getName() : editorUuid.toString();
    }

    private boolean hasDirection(RegionBox box) {
        return box != null && box.dirX() != null && box.dirY() != null && box.dirZ() != null;
    }

    private String boolColor(boolean value) {
        return value ? "&aSET" : "&cMISSING";
    }
}

