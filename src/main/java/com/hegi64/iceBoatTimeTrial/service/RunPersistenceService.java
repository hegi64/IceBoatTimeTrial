package com.hegi64.iceBoatTimeTrial.service;

import com.hegi64.iceBoatTimeTrial.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.UUID;

public class RunPersistenceService {
    private final JavaPlugin plugin;
    private final Database database;

    public RunPersistenceService(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void saveRunAsync(UUID playerId,
                             UUID trackUuid,
                             long total,
                             long s1,
                             long s2,
                             long s3,
                             boolean saveHistory,
                             Runnable onSuccess,
                             java.util.function.Consumer<String> onFailure) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                database.saveRun(playerId, trackUuid, total, s1, s2, s3, saveHistory);
                Bukkit.getScheduler().runTask(plugin, onSuccess);
            } catch (SQLException exception) {
                Bukkit.getScheduler().runTask(plugin, () -> onFailure.accept(exception.getMessage()));
            }
        });
    }
}
