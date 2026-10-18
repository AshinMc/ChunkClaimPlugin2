package org.ashin.chunkClaimPlugin2.config;

import org.ashin.chunkClaimPlugin2.ChunkClaimPlugin2;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles automatic migration and upgrade of config.yml from legacy installations.
 * Preserves all user customizations, maps deprecated options to modern equivalents,
 * creates a safety backup, and ensures the config file stays aligned with the latest schema.
 */
public final class ConfigMigrator {

    public static final int CURRENT_CONFIG_VERSION = 2;

    private ConfigMigrator() {}

    /**
     * Inspects the server's config.yml and performs a lossless upgrade if an older
     * or unversioned configuration is detected.
     */
    public static void migrate(ChunkClaimPlugin2 plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            // Fresh installation - generate latest default template
            plugin.saveDefaultConfig();
            return;
        }

        YamlConfiguration existing = YamlConfiguration.loadConfiguration(configFile);
        int detectedVersion = existing.getInt("config-version", 1);

        if (detectedVersion >= CURRENT_CONFIG_VERSION) {
            // Up-to-date layout; ensure any minor new keys are populated without overwriting
            plugin.getConfig().options().copyDefaults(true);
            plugin.saveConfig();
            return;
        }

        plugin.getLogger().info("Detected legacy config.yml (specification v" + detectedVersion + "). Migrating to v" + CURRENT_CONFIG_VERSION + "...");

        // 1. Create a timestamped/indexed backup of the old config
        File backupFile = getUniqueBackupFile(dataFolder);
        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Created safety backup of existing config: " + backupFile.getName());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create backup of config.yml: " + e.getMessage());
        }

        // 2. Read and preserve all user custom values across all previous versions
        Map<String, Object> preservedValues = new LinkedHashMap<>();

        // Core scalars
        copyIfPresent(existing, preservedValues, "locale");
        copyIfPresent(existing, preservedValues, "max-claims-per-player");
        copyIfPresent(existing, preservedValues, "claim-item");
        copyIfPresent(existing, preservedValues, "greeting-display");
        copyIfPresent(existing, preservedValues, "show-wilderness-greeting");

        // Structured sections
        copySectionIfPresent(existing, preservedValues, "title-duration");
        copySectionIfPresent(existing, preservedValues, "economy");
        copySectionIfPresent(existing, preservedValues, "visualization");
        copySectionIfPresent(existing, preservedValues, "message-toggles");
        copySectionIfPresent(existing, preservedValues, "claim-flags");
        copySectionIfPresent(existing, preservedValues, "event-commands");
        copySectionIfPresent(existing, preservedValues, "messages");

        // 3. Intelligent migration for deprecated 'protection:' block (v0.1 - v0.5 leftover)
        if (existing.contains("protection.prevent-interactions")) {
            boolean preventInteractions = existing.getBoolean("protection.prevent-interactions", true);
            // If the user explicitly disabled interactions in the legacy block,
            // and did not explicitly configure mob-protection:
            if (!preventInteractions && !existing.contains("claim-flags.mob-protection")) {
                preservedValues.put("claim-flags.mob-protection", false);
                plugin.getLogger().info("Migrated legacy 'protection.prevent-interactions: false' -> 'claim-flags.mob-protection: false'");
            }
        }

        // 4. Overwrite config.yml with clean template from JAR, then re-apply preserved values
        try {
            plugin.saveResource("config.yml", true);
            plugin.reloadConfig();

            FileConfiguration modernConfig = plugin.getConfig();
            modernConfig.set("config-version", CURRENT_CONFIG_VERSION);

            for (Map.Entry<String, Object> entry : preservedValues.entrySet()) {
                modernConfig.set(entry.getKey(), entry.getValue());
            }

            plugin.saveConfig();
            plugin.getLogger().info("Successfully upgraded config.yml to v" + CURRENT_CONFIG_VERSION + " with all custom settings preserved!");
        } catch (Exception e) {
            plugin.getLogger().severe("Unable to write migrated config.yml to disk (" + e.getMessage() + "). Running with migrated values in-memory.");
            // Apply preserved values in-memory so server functionality is unaffected
            FileConfiguration memConfig = plugin.getConfig();
            memConfig.set("config-version", CURRENT_CONFIG_VERSION);
            for (Map.Entry<String, Object> entry : preservedValues.entrySet()) {
                memConfig.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void copyIfPresent(YamlConfiguration source, Map<String, Object> target, String path) {
        if (source.contains(path)) {
            target.put(path, source.get(path));
        }
    }

    private static void copySectionIfPresent(YamlConfiguration source, Map<String, Object> target, String path) {
        ConfigurationSection sec = source.getConfigurationSection(path);
        if (sec != null) {
            for (String key : sec.getKeys(true)) {
                if (!sec.isConfigurationSection(key)) {
                    target.put(path + "." + key, sec.get(key));
                }
            }
        }
    }

    private static File getUniqueBackupFile(File dataFolder) {
        File backup = new File(dataFolder, "config.yml.old");
        if (!backup.exists()) {
            return backup;
        }
        int index = 1;
        while (true) {
            backup = new File(dataFolder, "config.yml.old." + index);
            if (!backup.exists()) {
                return backup;
            }
            index++;
        }
    }
}
