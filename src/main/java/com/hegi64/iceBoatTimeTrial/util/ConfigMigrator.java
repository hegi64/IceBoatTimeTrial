package com.hegi64.iceBoatTimeTrial.util;

import com.hegi64.iceBoatTimeTrial.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles automatic config migration when the plugin's default config version
 * is newer than the server's existing config version.
 *
 * Strategy: collect the user's old values, back up the old config, replace it
 * with the fresh default from the JAR, then re-apply the user's old values via
 * Bukkit's config API and save.
 */
public final class ConfigMigrator {

    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final DateTimeFormatter BACKUP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private ConfigMigrator() {
    }

    /**
     * Checks whether the default config bundled in the JAR is newer than the
     * server's current config. If so, backs up the old config, writes the fresh
     * default, and re-applies the user's old values.
     */
    public static void migrate() {
        Main plugin = Main.getInstance();
        if (plugin == null) {
            return;
        }

        String prefix = "[" + plugin.getName() + "] ";

        // Load the default config shipped inside the JAR
        YamlConfiguration defaultConfig = loadDefaultConfig(plugin);
        if (defaultConfig == null) {
            plugin.getLogger().warning("Could not load default config.yml from JAR - skipping config migration.");
            return;
        }

        String currentVersion = plugin.getConfig().getString(CONFIG_VERSION_KEY, "0");
        String defaultVersion = defaultConfig.getString(CONFIG_VERSION_KEY, "0");

        if (!isNewer(defaultVersion, currentVersion)) {
            return; // already up-to-date
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +
                prefix + "Config version changed (" + currentVersion + " -> " + defaultVersion +
                "). Migrating config...");

        // 1. Collect all leaf values from the old config (flat key map)
        Map<String, Object> oldValues = new LinkedHashMap<>();
        collectLeafValues(plugin.getConfig(), "", oldValues);
        // Remove configVersion - we always want the new version
        oldValues.remove(CONFIG_VERSION_KEY);

        // 2. Backup current config
        backupConfig(plugin, currentVersion, prefix);

        // 3. Delete old config and write fresh default from JAR
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            try {
                Files.delete(configFile.toPath());
            } catch (IOException exception) {
                plugin.getLogger().severe("Failed to delete old config.yml: " + exception.getMessage());
                return;
            }
        }
        plugin.saveDefaultConfig();

        // 4. Reload so Bukkit picks up the fresh default
        plugin.reloadConfig();

        // 5. Collect default leaf values to know which keys exist in the new default
        Map<String, Object> defaultValues = new LinkedHashMap<>();
        collectLeafValues(defaultConfig, "", defaultValues);
        defaultValues.remove(CONFIG_VERSION_KEY);

        // 6. For each old value that also exists in the new default, apply it
        int restored = 0;
        for (Map.Entry<String, Object> entry : oldValues.entrySet()) {
            String key = entry.getKey();
            if (!defaultValues.containsKey(key)) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY +
                        prefix + "  - Old key no longer exists in default, skipped: " + key);
                continue;
            }

            Object oldVal = entry.getValue();
            Object defVal = defaultValues.get(key);

            if (valuesEqual(oldVal, defVal)) {
                continue; // same as default, nothing to restore
            }

            plugin.getConfig().set(key, oldVal);
            restored++;
            Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY +
                    prefix + "  ~ Restored user setting: " + key);
        }

        // 7. Save the merged config
        plugin.saveConfig();
        plugin.reloadConfig();

        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +
                prefix + "Config migration complete. " + restored + " user setting(s) restored.");
    }

    // -------------------------------------------------------------------------
    // Collecting values
    // -------------------------------------------------------------------------

    /**
     * Recursively collects all leaf (non-section) values into a flat map
     * with dotted paths as keys. Lists are treated as leaf values.
     */
    private static void collectLeafValues(ConfigurationSection section, String parentPath, Map<String, Object> map) {
        for (String key : section.getKeys(false)) {
            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            if (section.isConfigurationSection(key)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child != null) {
                    collectLeafValues(child, fullPath, map);
                }
            } else {
                map.put(fullPath, section.get(key));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Value comparison
    // -------------------------------------------------------------------------

    private static boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    // -------------------------------------------------------------------------
    // Config loading & backup
    // -------------------------------------------------------------------------

    /**
     * Loads the default config.yml from inside the plugin JAR as a
     * {@link YamlConfiguration}, without touching the file on disk.
     */
    private static YamlConfiguration loadDefaultConfig(Main plugin) {
        try (InputStream is = plugin.getResource("config.yml")) {
            if (is == null) return null;
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error reading default config.yml: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a timestamped backup copy of the current config.yml.
     */
    private static void backupConfig(Main plugin, String currentVersion, String prefix) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        String timestamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
        String backupName = "config-backup-" + currentVersion + "_" + timestamp + ".yml";
        File backupFile = new File(plugin.getDataFolder(), backupName);

        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + prefix + "Config backup saved as " + backupName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create config backup: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Version comparison
    // -------------------------------------------------------------------------

    /**
     * Compares two semantic version strings (e.g. "1.0.1" vs "1.1.0").
     *
     * @return {@code true} if {@code newVersion} is strictly greater than {@code oldVersion}
     */
    private static boolean isNewer(String newVersion, String oldVersion) {
        int[] newParts = parseVersion(newVersion);
        int[] oldParts = parseVersion(oldVersion);

        int length = Math.max(newParts.length, oldParts.length);
        for (int i = 0; i < length; i++) {
            int n = i < newParts.length ? newParts[i] : 0;
            int o = i < oldParts.length ? oldParts[i] : 0;
            if (n > o) return true;
            if (n < o) return false;
        }
        return false; // equal
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
}
