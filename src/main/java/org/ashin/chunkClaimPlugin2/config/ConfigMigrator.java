package org.ashin.chunkClaimPlugin2.config;

import org.ashin.chunkClaimPlugin2.ChunkClaimPlugin2;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            // Up-to-date layout; Bukkit reads JAR defaults in-memory so skip rewriting disk file on startup
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
        copyIfPresent(existing, preservedValues, "disabled-worlds");
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

        // 3. Intelligent migration for deprecated keys
        if (preservedValues.containsKey("claim-flags.pvp") && !preservedValues.containsKey("claim-flags.block-pvp")) {
            preservedValues.put("claim-flags.block-pvp", preservedValues.remove("claim-flags.pvp"));
        } else {
            preservedValues.remove("claim-flags.pvp");
        }
        if (existing.contains("protection.prevent-interactions")) {
            boolean preventInteractions = existing.getBoolean("protection.prevent-interactions", true);
            // If the user explicitly disabled interactions in the legacy block,
            // and did not explicitly configure mob-protection:
            if (!preventInteractions && !existing.contains("claim-flags.mob-protection")) {
                preservedValues.put("claim-flags.mob-protection", false);
                plugin.getLogger().info("Migrated legacy 'protection.prevent-interactions: false' -> 'claim-flags.mob-protection: false'");
            }
        }

        // 4. Overwrite config.yml with clean commented template from JAR, then re-apply preserved values cleanly
        try {
            plugin.saveResource("config.yml", true);

            Map<String, Object> remainingForYaml = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : preservedValues.entrySet()) {
                Object val = entry.getValue();
                if (!(val instanceof List) && !(val instanceof Map) && !(val instanceof ConfigurationSection)) {
                    if (!updateScalarInFilePreservingComments(configFile, entry.getKey(), val)) {
                        remainingForYaml.put(entry.getKey(), val);
                    }
                } else {
                    remainingForYaml.put(entry.getKey(), val);
                }
            }

            if (!remainingForYaml.isEmpty()) {
                YamlConfiguration modernYaml = new YamlConfiguration();
                enableParseComments(modernYaml);
                modernYaml.load(configFile);
                modernYaml.set("config-version", CURRENT_CONFIG_VERSION);
                for (Map.Entry<String, Object> entry : remainingForYaml.entrySet()) {
                    modernYaml.set(entry.getKey(), entry.getValue());
                }
                modernYaml.save(configFile);
            }

            plugin.reloadConfig();
            plugin.getLogger().info("Successfully upgraded config.yml to v" + CURRENT_CONFIG_VERSION + " with comments and custom settings preserved!");
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

    /**
     * Safely reads a string from config, falling back when the key is missing, null, or blank.
     */
    public static String getNonBlankString(FileConfiguration config, String path, String fallback) {
        if (config == null) return fallback;
        String val = config.getString(path);
        return (val != null && !val.isBlank()) ? val.trim() : fallback;
    }

    /**
     * Updates a configuration key in memory and writes it to config.yml without stripping comments or section banners.
     */
    public static void setConfigValuePreservingComments(org.bukkit.plugin.java.JavaPlugin plugin, String path, Object value) {
        plugin.getConfig().set(path, value);
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (configFile.exists() && !(value instanceof List) && !(value instanceof Map)
                    && updateScalarInFilePreservingComments(configFile, path, value)) {
                return;
            }
            YamlConfiguration yaml = new YamlConfiguration();
            enableParseComments(yaml);
            if (configFile.exists()) {
                yaml.load(configFile);
            }
            yaml.set(path, value);
            yaml.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save config.yml preserving comments (" + e.getMessage() + "); falling back to default save.");
            plugin.saveConfig();
        }
    }

    private static void enableParseComments(YamlConfiguration yaml) {
        try {
            yaml.options().parseComments(true);
        } catch (Throwable ignored) {
            // Older Spigot builds (<1.18.1) lack YamlConfigurationOptions#parseComments
        }
    }

    /**
     * Updates a scalar key line-by-line in config.yml while keeping all headers, blank lines, and inline comments intact.
     * Returns true if the key was found and updated in-place.
     */
    private static boolean updateScalarInFilePreservingComments(File configFile, String dotPath, Object value) throws IOException {
        List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
        String[] segments = dotPath.split("\\.");
        int[] sectionIndent = new int[segments.length];
        java.util.Arrays.fill(sectionIndent, -1);
        int matchedDepth = 0;

        Pattern keyPattern = Pattern.compile("^(\\s*)([^#\\s][^:]*?):(.*)$");
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            Matcher matcher = keyPattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            int indent = matcher.group(1).length();
            String key = matcher.group(2).trim();
            if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'"))) {
                key = key.substring(1, key.length() - 1);
            }

            // Pop any parent sections whose indentation is >= current line indentation
            while (matchedDepth > 0 && indent <= sectionIndent[matchedDepth - 1]) {
                matchedDepth--;
            }

            if (matchedDepth < segments.length && key.equals(segments[matchedDepth])) {
                if (matchedDepth == segments.length - 1) {
                    // Found exact target scalar line
                    String rest = matcher.group(3);
                    String inlineComment = extractInlineComment(rest);
                    String formattedValue = formatYamlScalar(value);
                    String newLine = matcher.group(1) + matcher.group(2) + ": " + formattedValue
                            + (inlineComment.isEmpty() ? "" : " " + inlineComment);
                    lines.set(i, newLine);
                    Files.write(configFile.toPath(), lines, StandardCharsets.UTF_8);
                    return true;
                } else {
                    sectionIndent[matchedDepth] = indent;
                    matchedDepth++;
                }
            }
        }
        return false;
    }

    private static String extractInlineComment(String rest) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == '#' && !inSingle && !inDouble) {
                return rest.substring(i).trim();
            }
        }
        return "";
    }

    private static String formatYamlScalar(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        String s = String.valueOf(value);
        if (s.isEmpty() || s.contains(":") || s.contains("#") || s.contains("\"")) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return s;
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
