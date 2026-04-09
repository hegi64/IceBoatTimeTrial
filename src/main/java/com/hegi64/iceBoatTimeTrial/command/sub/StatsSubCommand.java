package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.analytics.PeriodFilter;
import com.hegi64.iceBoatTimeTrial.service.stats.PlayerStatsService;
import com.hegi64.iceBoatTimeTrial.service.stats.PlayerStatsSnapshot;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class StatsSubCommand implements SubCommand {
    private final PluginConfig config;
    private final PlayerStatsService statsService;

    public StatsSubCommand(PluginConfig config, PlayerStatsService statsService) {
        this.config = config;
        this.statsService = statsService;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                send(sender, "&eUsage: /ibt stats <player> [day|week|month|all]");
                return true;
            }
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            OfflinePlayer offline = resolvePlayer(args[1]);
            if (offline == null) {
                send(sender, "&cPlayer not found: &f" + args[1]);
                return true;
            }
            targetUuid = offline.getUniqueId();
            targetName = offline.getName() != null ? offline.getName() : offline.getUniqueId().toString();
        }

        PeriodFilter period = parsePeriod(args, 2);
        Optional<PlayerStatsSnapshot> statsOpt = statsService.getStats(targetUuid, period);
        if (statsOpt.isEmpty()) {
            send(sender, "&eNo stats available for &f" + targetName + "&e in this period.");
            return true;
        }

        PlayerStatsSnapshot stats = statsOpt.get();
        send(sender, "&bStats for &f" + targetName + " &7(" + period.name().toLowerCase(Locale.ROOT) + ")");
        send(sender, "&7Runs: &f" + stats.totalRuns() + " &7| Completions: &f" + stats.totalCompletions());
        send(sender, "&7Best overall: &f" + formatMillisOrNa(stats.bestOverallMs()));
        send(sender, "&7Recent average: &f" + formatMillisOrNa(stats.recentAverageMs()));
        send(sender, "&7Improvement: &a" + TimeFormat.formatMillis(stats.improvementMs()));
        return true;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "/ibt stats [player] [day|week|month|all]";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.PLAYER);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return partial(args[1], names);
        }
        if (args.length == 3) {
            return partial(args[2], List.of("day", "week", "month", "all"));
        }
        return List.of();
    }

    private PeriodFilter parsePeriod(String[] args, int index) {
        if (args.length <= index) {
            return PeriodFilter.ALL;
        }
        return switch (args[index].toLowerCase(Locale.ROOT)) {
            case "day", "d" -> PeriodFilter.DAY;
            case "week", "w" -> PeriodFilter.WEEK;
            case "month", "m" -> PeriodFilter.MONTH;
            case "all", "a" -> PeriodFilter.ALL;
            default -> PeriodFilter.ALL;
        };
    }

    private String formatMillisOrNa(long millis) {
        return millis < 0 ? "n/a" : TimeFormat.formatMillis(millis);
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

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }

    private OfflinePlayer resolvePlayer(String token) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(token)) {
                return online;
            }
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String name = offline.getName();
            if (name != null && name.equalsIgnoreCase(token)) {
                return offline;
            }
        }
        return null;
    }
}
