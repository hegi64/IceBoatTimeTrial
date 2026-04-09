package com.hegi64.iceBoatTimeTrial.presentation;

import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.ActiveRun;
import com.hegi64.iceBoatTimeTrial.model.RunResult;
import com.hegi64.iceBoatTimeTrial.service.settings.MessageVerbosity;
import com.hegi64.iceBoatTimeTrial.service.settings.PlayerSettingsService;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.entity.Player;

public class RunMessagePresenter {
    private static final String COLOR_OVERALL_BEST = "&d";
    private static final String COLOR_PERSONAL_BEST = "&a";
    private static final String COLOR_WORSE = "&e";

    private final PluginConfig config;
    private final PlayerSettingsService playerSettingsService;

    public RunMessagePresenter(PluginConfig config, PlayerSettingsService playerSettingsService) {
        this.config = config;
        this.playerSettingsService = playerSettingsService;
    }

    public void sendAbort(Player player, String reasonMessage) {
        send(player, reasonMessage);
    }

    public void sendAbort(Player player) {
        sendAbort(player, config.runAbortedMessage());
    }

    public void sendStorageError(Player player, String details) {
        send(player, "&cCould not save run: " + details);
    }

    public void sendSectorMessage(Player player, int sector, long value, Long personalBest, Long globalBest) {
        if (playerSettingsService.get(player.getUniqueId()).verbosity() != MessageVerbosity.DETAILED) {
            return;
        }
        String color = performanceColor(value, personalBest, globalBest);
        String delta = personalBest == null
                ? "&7n/a&8"
                : deltaColor(value - personalBest) + TimeFormat.formatSignedMillis(value - personalBest) + "&8";
        String message = config.sectorFinishedTemplate()
                .replace("%sector%", Integer.toString(sector))
                .replace("%color%", color)
                .replace("%time%", TimeFormat.formatMillis(value))
                .replace("%delta%", delta);
        send(player, message);
    }

    public void sendFinishMessage(Player player, ActiveRun run, RunResult result) {
        // MINIMAL still receives finish message, but condensed.
        if (playerSettingsService.get(player.getUniqueId()).verbosity() == MessageVerbosity.MINIMAL) {
            send(player, "&aFinished: &f" + TimeFormat.formatMillis(result.totalMillis()));
            return;
        }
        String totalColor = performanceColor(result.totalMillis(), run.getPersonalBestTotalMillis(), run.getGlobalBestTotalMillis());
        String delta = run.getPersonalBestTotalMillis() == null
                ? "&7n/a&8"
                : deltaColor(result.totalMillis() - run.getPersonalBestTotalMillis())
                + TimeFormat.formatSignedMillis(result.totalMillis() - run.getPersonalBestTotalMillis())
                + "&8";

        String s1 = performanceColor(result.sector1Millis(), run.getPersonalBestSector1Millis(), run.getGlobalBestSector1Millis())
                + TimeFormat.formatMillis(result.sector1Millis()) + "&7";
        String s2 = performanceColor(result.sector2Millis(), run.getPersonalBestSector2Millis(), run.getGlobalBestSector2Millis())
                + TimeFormat.formatMillis(result.sector2Millis()) + "&7";
        String s3 = performanceColor(result.sector3Millis(), run.getPersonalBestSector3Millis(), run.getGlobalBestSector3Millis())
                + TimeFormat.formatMillis(result.sector3Millis()) + "&7";

        String message = config.runFinishedTemplate()
                .replace("%color%", totalColor)
                .replace("%total%", TimeFormat.formatMillis(result.totalMillis()))
                .replace("%delta%", delta)
                .replace("%s1%", s1)
                .replace("%s2%", s2)
                .replace("%s3%", s3);
        send(player, message);
    }

    private String performanceColor(long value, Long personalBest, Long globalBest) {
        if (globalBest == null || value < globalBest) {
            return COLOR_OVERALL_BEST;
        }
        if (personalBest == null || value < personalBest) {
            return COLOR_PERSONAL_BEST;
        }
        return COLOR_WORSE;
    }

    private String deltaColor(long deltaMillis) {
        return deltaMillis <= 0 ? "&a" : "&e";
    }

    private void send(Player player, String message) {
        player.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}
