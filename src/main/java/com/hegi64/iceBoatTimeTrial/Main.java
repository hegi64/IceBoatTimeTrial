package com.hegi64.iceBoatTimeTrial;

import com.hegi64.iceBoatTimeTrial.command.IbtCommand;
import com.hegi64.iceBoatTimeTrial.command.IteCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.editor.TrackEditorPreviewService;
import com.hegi64.iceBoatTimeTrial.editor.TrackEditorSessionService;
import com.hegi64.iceBoatTimeTrial.gui.GuiListener;
import com.hegi64.iceBoatTimeTrial.gui.GuiRegistry;
import com.hegi64.iceBoatTimeTrial.gui.GuiSessionService;
import com.hegi64.iceBoatTimeTrial.hologram.HologramService;
import com.hegi64.iceBoatTimeTrial.hologram.HologramUpdater;
import com.hegi64.iceBoatTimeTrial.listener.RunListener;
import com.hegi64.iceBoatTimeTrial.listener.TrackEditorListener;
import com.hegi64.iceBoatTimeTrial.presentation.RunMessagePresenter;
import com.hegi64.iceBoatTimeTrial.service.BossBarService;
import com.hegi64.iceBoatTimeTrial.service.RunPersistenceService;
import com.hegi64.iceBoatTimeTrial.service.RunService;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import com.hegi64.iceBoatTimeTrial.util.ConfigMigrator;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Objects;

public final class Main extends JavaPlugin {
    private static Main instance;
    private Database database;
    private TrackService trackService;
    private BossBarService bossBarService;
    private RunService runService;
    private HologramService hologramService;
    private HologramUpdater hologramUpdater;
    private PluginConfig pluginConfig;
    private TrackEditorSessionService editorSessions;
    private TrackEditorPreviewService editorPreviewService;
    private GuiRegistry guiRegistry;
    private GuiSessionService guiSessions;

    public void onLoad() {
        super.onLoad();
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigMigrator.migrate();

        this.pluginConfig = new PluginConfig(getConfig());

        Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (!(wePlugin instanceof WorldEditPlugin worldEditPlugin)) {
            getLogger().severe("WorldEdit is required but not available.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.database = new Database(this);
        this.trackService = new TrackService(database);
        this.bossBarService = new BossBarService(this, pluginConfig);
        RunPersistenceService persistenceService = new RunPersistenceService(this, database);
        RunMessagePresenter messagePresenter = new RunMessagePresenter(pluginConfig);
        this.runService = new RunService(trackService, database, bossBarService, persistenceService, messagePresenter, pluginConfig);
        this.hologramService = new HologramService(this);
        this.editorSessions = new TrackEditorSessionService();
        this.editorPreviewService = new TrackEditorPreviewService(this, editorSessions, trackService);
        this.editorPreviewService.setConfig(pluginConfig);
        this.guiRegistry = new GuiRegistry();
        this.guiSessions = new GuiSessionService();

        try {
            database.init();
            trackService.load();
        } catch (SQLException exception) {
            getLogger().severe("Failed to initialize SQLite: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.hologramUpdater = new HologramUpdater(hologramService, trackService, database, pluginConfig);
        this.runService.setHologramUpdater(hologramUpdater);
        hologramService.loadAll();
        hologramService.spawnAll();
        hologramUpdater.updateAll();

        IbtCommand ibtCommand = new IbtCommand(this, pluginConfig, trackService, database, bossBarService, runService, hologramService, hologramUpdater);
        PluginCommand ibt = Objects.requireNonNull(getCommand("ibt"), "ibt command missing in plugin.yml");
        ibt.setExecutor(ibtCommand);
        ibt.setTabCompleter(ibtCommand);

        IteCommand iteCommand = new IteCommand(pluginConfig, trackService, editorSessions, worldEditPlugin);
        PluginCommand ite = Objects.requireNonNull(getCommand("icetrackeditor"), "icetrackeditor command missing in plugin.yml");
        ite.setExecutor(iteCommand);
        ite.setTabCompleter(iteCommand);

        getServer().getPluginManager().registerEvents(new RunListener(runService, bossBarService, pluginConfig), this);
        getServer().getPluginManager().registerEvents(new TrackEditorListener(editorSessions), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiRegistry, guiSessions), this);

        bossBarService.start(runService.getActiveRuns());
        editorPreviewService.start();
    }

    @Override
    public void onDisable() {
        if (bossBarService != null) {
            bossBarService.stop();
        }
        if (hologramService != null) {
            hologramService.despawnAll();
        }
        if (editorPreviewService != null) {
            editorPreviewService.stop();
        }
        if (editorSessions != null) {
            editorSessions.clearAll();
        }
        if (guiSessions != null) {
            guiSessions.clearAll();
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig.update(getConfig());
        bossBarService.setConfig(pluginConfig);
        runService.setConfig(pluginConfig);
        editorPreviewService.setConfig(pluginConfig);
    }
}
