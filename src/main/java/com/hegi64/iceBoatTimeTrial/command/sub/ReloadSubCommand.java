package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.hologram.HologramService;
import com.hegi64.iceBoatTimeTrial.hologram.HologramUpdater;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.BossBarService;
import com.hegi64.iceBoatTimeTrial.service.RunService;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class ReloadSubCommand implements SubCommand {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final TrackService trackService;
    private final RunService runService;
    private final BossBarService bossBarService;
    private final HologramService hologramService;
    private final HologramUpdater hologramUpdater;

    public ReloadSubCommand(JavaPlugin plugin,
                            PluginConfig config,
                            TrackService trackService,
                            RunService runService,
                            BossBarService bossBarService,
                            HologramService hologramService,
                            HologramUpdater hologramUpdater) {
        this.plugin = plugin;
        this.config = config;
        this.trackService = trackService;
        this.runService = runService;
        this.bossBarService = bossBarService;
        this.hologramService = hologramService;
        this.hologramUpdater = hologramUpdater;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        try {
            plugin.reloadConfig();
            config.update(plugin.getConfig());
            runService.setConfig(config);
            hologramUpdater.setConfig(config);

            bossBarService.stop();
            bossBarService.start(runService.getActiveRuns());

            trackService.load();
            hologramService.loadAll();
            hologramService.spawnAll();
            hologramUpdater.updateAll();

            send(sender, "&aConfig and track cache reloaded.");
        } catch (SQLException exception) {
            send(sender, "&cReload failed: " + exception.getMessage());
        }
        return true;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "/ibt reload";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.ADMIN);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}
