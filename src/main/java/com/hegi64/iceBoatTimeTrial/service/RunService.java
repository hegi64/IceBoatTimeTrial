package com.hegi64.iceBoatTimeTrial.service;

import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.hologram.HologramUpdater;
import com.hegi64.iceBoatTimeTrial.model.ActiveRun;
import com.hegi64.iceBoatTimeTrial.model.OverlapPolicy;
import com.hegi64.iceBoatTimeTrial.model.RegionBox;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.RunResult;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.presentation.RunMessagePresenter;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RunService {
    private final TrackService trackService;
    private final Database database;
    private final BossBarService bossBarService;
    private final RunPersistenceService persistenceService;
    private final RunMessagePresenter messagePresenter;
    private PluginConfig config;
    private final Map<UUID, ActiveRun> activeRuns = new ConcurrentHashMap<>();
    private final Map<String, Long> triggerDebounce = new ConcurrentHashMap<>();
    private HologramUpdater hologramUpdater;

    public RunService(TrackService trackService,
                      Database database,
                      BossBarService bossBarService,
                      RunPersistenceService persistenceService,
                      RunMessagePresenter messagePresenter,
                      PluginConfig config) {
        this.trackService = trackService;
        this.database = database;
        this.bossBarService = bossBarService;
        this.persistenceService = persistenceService;
        this.messagePresenter = messagePresenter;
        this.config = config;
    }

    public Map<UUID, ActiveRun> getActiveRuns() {
        return activeRuns;
    }

    public void handleMove(Player player, Location from, Location to) {
        if (to == null || from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        if (!(player.getVehicle() instanceof Boat)) {
            return;
        }

        Vector movement = to.toVector().subtract(from.toVector());
        UUID playerId = player.getUniqueId();
        ActiveRun run = activeRuns.get(playerId);
        if (run == null) {
            tryStart(player, from, to, movement);
            return;
        }
        advanceRun(player, run, from, to, movement);
    }

    private void tryStart(Player player, Location from, Location to, Vector movement) {
        OverlapPolicy policy = OverlapPolicy.fromConfig(config.overlapPolicy());
        Optional<Track> trackOpt = trackService.resolveStartTrack(to, policy);
        if (trackOpt.isEmpty()) {
            return;
        }
        Track track = trackOpt.get();
        if (!isCrossing(track, RegionType.START_FINISH, player.getUniqueId(), from, to, movement)) {
            return;
        }
        ActiveRun run = createRunWithReferenceBests(player.getUniqueId(), track);
        activeRuns.put(player.getUniqueId(), run);
        bossBarService.show(player);
    }

    private void advanceRun(Player player, ActiveRun run, Location from, Location to, Vector movement) {
        UUID playerId = player.getUniqueId();
        Track track = run.getTrack();

        if (!run.isCheckpoint1Passed() && isCrossing(track, RegionType.CHECKPOINT_1, playerId, from, to, movement)) {
            long elapsed = System.currentTimeMillis() - run.getStartedAtMillis();
            long sector1 = elapsed;
            run.setCheckpoint1Passed(true);
            run.setSector1Millis(sector1);
            messagePresenter.sendSectorMessage(player, 1, sector1, run.getPersonalBestSector1Millis(), run.getGlobalBestSector1Millis());
            return;
        }

        if (run.isCheckpoint1Passed() && !run.isCheckpoint2Passed() && isCrossing(track, RegionType.CHECKPOINT_2, playerId, from, to, movement)) {
            long elapsed = System.currentTimeMillis() - run.getStartedAtMillis();
            long sector2 = elapsed - run.getSector1Millis();
            run.setCheckpoint2Passed(true);
            run.setSector2Millis(sector2);
            messagePresenter.sendSectorMessage(player, 2, sector2, run.getPersonalBestSector2Millis(), run.getGlobalBestSector2Millis());
            return;
        }

        if (isCrossing(track, RegionType.START_FINISH, playerId, from, to, movement)) {
            if (!run.isCheckpoint1Passed() || !run.isCheckpoint2Passed()) {
                abortRun(player, config.runAbortedMessage());
                return;
            }
            finishRun(player, run);
            ActiveRun newRun = createRunWithReferenceBests(player.getUniqueId(), track);
            activeRuns.put(player.getUniqueId(), newRun);
            bossBarService.show(player);
        }
    }

    private ActiveRun createRunWithReferenceBests(UUID playerId, Track track) {
        ActiveRun run = new ActiveRun(playerId, track, System.currentTimeMillis());
        try {
            Optional<long[]> personalBest = database.getBest(playerId, track.getUuid());
            personalBest.ifPresent(best -> {
                run.setPersonalBestTotalMillis(best[0]);
                run.setPersonalBestSector1Millis(best[1]);
                run.setPersonalBestSector2Millis(best[2]);
                run.setPersonalBestSector3Millis(best[3]);
            });

            Optional<long[]> globalBest = database.getGlobalBest(track.getUuid());
            globalBest.ifPresent(best -> {
                run.setGlobalBestTotalMillis(best[0]);
                run.setGlobalBestSector1Millis(best[1]);
                run.setGlobalBestSector2Millis(best[2]);
                run.setGlobalBestSector3Millis(best[3]);
            });
        } catch (SQLException ignored) {
            // Run tracking must continue even if best references cannot be loaded.
        }
        return run;
    }

    private void finishRun(Player player, ActiveRun run) {
        long total = System.currentTimeMillis() - run.getStartedAtMillis();
        long s1 = run.getSector1Millis() == null ? 0 : run.getSector1Millis();
        long s2 = run.getSector2Millis() == null ? 0 : run.getSector2Millis();
        long s3 = total - s1 - s2;
        RunResult result = new RunResult(total, s1, s2, s3);

        activeRuns.remove(player.getUniqueId());
        bossBarService.hide(player);

        messagePresenter.sendSectorMessage(player, 3, s3, run.getPersonalBestSector3Millis(), run.getGlobalBestSector3Millis());
        messagePresenter.sendFinishMessage(player, run, result);

        persistenceService.saveRunAsync(
                player.getUniqueId(),
                run.getTrack().getUuid(),
                total,
                s1,
                s2,
                s3,
                config.saveRunHistory(),
                () -> refreshTrackHolograms(run.getTrack().getName()),
                details -> messagePresenter.sendStorageError(player, details)
        );
    }

    private void refreshTrackHolograms(String trackName) {
        if (hologramUpdater == null) {
            return;
        }
        hologramUpdater.update(trackName, "top_times");
        hologramUpdater.update(trackName, "top_players");
        hologramUpdater.update(trackName, "top_times_s1");
        hologramUpdater.update(trackName, "top_times_s2");
        hologramUpdater.update(trackName, "top_times_s3");
        hologramUpdater.update(trackName, "top_players_s1");
        hologramUpdater.update(trackName, "top_players_s2");
        hologramUpdater.update(trackName, "top_players_s3");
    }

    public void abortRun(Player player, String reasonMessage) {
        if (activeRuns.remove(player.getUniqueId()) == null) {
            return;
        }
        bossBarService.hide(player);
        messagePresenter.sendAbort(player, reasonMessage);
    }

    public void abortRunSilent(UUID playerId) {
        activeRuns.remove(playerId);
    }

    private boolean isCrossing(Track track, RegionType type, UUID playerId, Location from, Location to, Vector movement) {
        RegionBox box = track.getRegion(type);
        if (box == null) {
            return false;
        }
        if (box.contains(from) || !box.contains(to)) {
            return false;
        }
        if (!box.directionMatches(movement, config.enforceDirection(), config.defaultDirectionThreshold())) {
            return false;
        }

        String key = playerId + ":" + track.getUuid() + ":" + type.name();
        long now = System.currentTimeMillis();
        long debounce = config.triggerDebounceMillis();
        Long last = triggerDebounce.get(key);
        if (last != null && now - last < debounce) {
            return false;
        }
        triggerDebounce.put(key, now);
        return true;
    }

    public void setConfig(PluginConfig config) {
        this.config = config;
    }

    public void setHologramUpdater(HologramUpdater updater) {
        this.hologramUpdater = updater;
    }
}
