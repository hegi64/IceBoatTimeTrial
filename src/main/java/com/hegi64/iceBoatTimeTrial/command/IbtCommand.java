package com.hegi64.iceBoatTimeTrial.command;

import com.hegi64.iceBoatTimeTrial.command.sub.BestSubCommand;
import com.hegi64.iceBoatTimeTrial.command.sub.HologramSubCommand;
import com.hegi64.iceBoatTimeTrial.command.sub.ReloadSubCommand;
import com.hegi64.iceBoatTimeTrial.command.sub.SettingsSubCommand;
import com.hegi64.iceBoatTimeTrial.command.sub.StatsSubCommand;
import com.hegi64.iceBoatTimeTrial.command.sub.TopSubCommand;
import com.hegi64.iceBoatTimeTrial.command.sub.TracksSubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.gui.GuiSessionService;
import com.hegi64.iceBoatTimeTrial.gui.menu.SettingsMenu;
import com.hegi64.iceBoatTimeTrial.gui.menu.TrackBrowserMenu;
import com.hegi64.iceBoatTimeTrial.hologram.HologramService;
import com.hegi64.iceBoatTimeTrial.hologram.HologramUpdater;
import com.hegi64.iceBoatTimeTrial.service.BossBarService;
import com.hegi64.iceBoatTimeTrial.service.RunService;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.service.analytics.LeaderboardAnalyticsService;
import com.hegi64.iceBoatTimeTrial.service.stats.PlayerStatsService;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IbtCommand implements CommandExecutor, TabCompleter {
    private final PluginConfig config;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public IbtCommand(JavaPlugin plugin,
                      PluginConfig config,
                      TrackService trackService,
                      Database database,
                      LeaderboardAnalyticsService analytics,
                      PlayerStatsService playerStatsService,
                      SettingsMenu settingsMenu,
                      TrackBrowserMenu trackBrowserMenu,
                      GuiSessionService guiSessions,
                      BossBarService bossBarService,
                      RunService runService,
                      HologramService hologramService,
                      HologramUpdater hologramUpdater) {
        this.config = config;

        register(new BestSubCommand(config, trackService, database));
        register(new StatsSubCommand(config, playerStatsService));
        register(new TopSubCommand(config, trackService, database, analytics));
        register(new SettingsSubCommand(config, settingsMenu, guiSessions));
        register(new TracksSubCommand(config, trackBrowserMenu, guiSessions));
        register(new ReloadSubCommand(plugin, config, trackService, runService, bossBarService, hologramService, hologramUpdater));
        register(new HologramSubCommand(config, trackService, hologramService, hologramUpdater));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        SubCommand subCommand = subCommands.get(name);
        if (subCommand == null) {
            send(sender, "&cUnknown subcommand.");
            sendHelp(sender);
            return true;
        }

        if (!subCommand.hasRequiredPermission(sender)) {
            send(sender, "&cMissing permission.");
            return true;
        }

        return subCommand.execute(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], new ArrayList<>(subCommands.keySet()));
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            return List.of();
        }
        if (!subCommand.hasRequiredPermission(sender)) {
            return List.of();
        }
        return subCommand.tabComplete(sender, command, alias, args);
    }

    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(Locale.ROOT), subCommand);
    }

    private void sendHelp(CommandSender sender) {
        send(sender, "&bIceBoatTimeTrial commands:");
        for (SubCommand subCommand : subCommands.values()) {
            if (!subCommand.hasRequiredPermission(sender)) {
                continue;
            }
            send(sender, "&e" + subCommand.getDescription());
        }
    }

    private List<String> partial(String token, List<String> candidates) {
        String lower = token.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}
