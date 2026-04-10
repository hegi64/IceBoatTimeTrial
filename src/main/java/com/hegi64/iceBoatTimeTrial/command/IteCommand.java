package com.hegi64.iceBoatTimeTrial.command;

import com.hegi64.iceBoatTimeTrial.command.sub.editor.*;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.editor.TrackEditorSessionService;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IteCommand implements CommandExecutor, TabCompleter {
    private final PluginConfig config;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final List<SubCommand> helpCommands = new ArrayList<>();

    public IteCommand(PluginConfig config, TrackService trackService, TrackEditorSessionService sessions, WorldEditPlugin worldEdit) {
        this.config = config;
        EditorCommandContext context = new EditorCommandContext(config, trackService, sessions, worldEdit);

        register(new CreateEditorSubCommand(context));
        register(new EditEditorSubCommand(context));
        register(new StopEditEditorSubCommand(context));
        register(new SetRegionEditorSubCommand(context, "setstart", "/ite setstart", RegionType.START_FINISH));
        register(new SetRegionEditorSubCommand(context, "setcp1", "/ite setcp1", RegionType.CHECKPOINT_1));
        register(new SetRegionEditorSubCommand(context, "setcp2", "/ite setcp2", RegionType.CHECKPOINT_2));
        register(new SetDirectionEditorSubCommand(context));
        register(new SetSpawnEditorSubCommand(context));
        register(new RenameEditorSubCommand(context));
        SubCommand statusCommand = new StatusEditorSubCommand(context);
        register(statusCommand);
        registerAlias("summary", statusCommand);
        register(new ValidateEditorSubCommand(context));
        register(new HelpEditorSubCommand(context));
        register(new ForceEditEditorSubCommand(context));
        register(new ForceStopEditorSubCommand(context));
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
            List<String> base = new ArrayList<>();
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                if (entry.getValue().hasRequiredPermission(sender)) {
                    base.add(entry.getKey());
                }
            }
            return partial(args[0], base);
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null || !subCommand.hasRequiredPermission(sender)) {
            return List.of();
        }
        return subCommand.tabComplete(sender, command, alias, args);
    }

    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(Locale.ROOT), subCommand);
        helpCommands.add(subCommand);
    }

    private void registerAlias(String alias, SubCommand subCommand) {
        subCommands.put(alias.toLowerCase(Locale.ROOT), subCommand);
    }

    private void sendHelp(CommandSender sender) {
        send(sender, "&bTrack editor commands:");
        for (SubCommand subCommand : helpCommands) {
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
