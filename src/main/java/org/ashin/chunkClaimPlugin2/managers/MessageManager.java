package org.ashin.chunkClaimPlugin2.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MessageManager {
    private final JavaPlugin plugin;
    private final String serverDefaultLocale;
    private final YamlConfiguration defaultLangConfig; // en_US from jar
    private final Map<String, YamlConfiguration> localeCache = new HashMap<>();

    // Player locales persistence
    private final File playerDataFile;
    private final YamlConfiguration playerData;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.serverDefaultLocale = config.getString("locale", "en_US");

        // Ensure lang folder exists
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // Save default language files if not present
        saveResourceIfNotExists("lang/messages_en_US.yml");
        // Provide extra example language files
        saveResourceIfNotExists("lang/messages_es_ES.yml");
        saveResourceIfNotExists("lang/messages_fr_FR.yml");

        // Load default English from jar (for fallback)
        this.defaultLangConfig = loadResourceYaml("lang/messages_en_US.yml");

        // Preload server default into cache if present
        getLocaleYaml(this.serverDefaultLocale);

        // Prepare player data storage
        this.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    private void saveResourceIfNotExists(String path) {
        File outFile = new File(plugin.getDataFolder(), path);
        if (!outFile.exists()) {
            try {
                plugin.saveResource(path, false);
            } catch (IllegalArgumentException ignored) {
                // Resource not packaged; ignore
            }
        }
    }

    private YamlConfiguration loadResourceYaml(String resourcePath) {
        try {
            InputStream in = plugin.getResource(resourcePath);
            if (in == null) return new YamlConfiguration();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new YamlConfiguration();
        }
    }

    private YamlConfiguration getLocaleYaml(String locale) {
        if (locale == null || locale.isEmpty()) locale = serverDefaultLocale;
        String key = locale;
        if (localeCache.containsKey(key)) return localeCache.get(key);

        // Try data folder first
        File langFile = new File(new File(plugin.getDataFolder(), "lang"), "messages_" + locale + ".yml");
        if (!langFile.exists()) {
            // Attempt to extract from jar
            saveResourceIfNotExists("lang/messages_" + locale + ".yml");
        }

        YamlConfiguration yaml;
        if (langFile.exists()) {
            yaml = YamlConfiguration.loadConfiguration(langFile);
        } else {
            // Try from jar as absolute fallback (e.g., en_US)
            yaml = loadResourceYaml("lang/messages_" + locale + ".yml");
        }

        if (yaml == null) yaml = new YamlConfiguration();
        localeCache.put(key, yaml);
        return yaml;
    }

    public boolean isLocaleAvailable(String locale) {
        if (locale == null || locale.isEmpty()) return false;
        File langFile = new File(new File(plugin.getDataFolder(), "lang"), "messages_" + locale + ".yml");
        if (langFile.exists()) return true;
        // Check if bundled in jar
        return plugin.getResource("lang/messages_" + locale + ".yml") != null;
    }

    public List<String> getAvailableLocales() {
        Set<String> set = new HashSet<>();
        // From data folder
        File langDir = new File(plugin.getDataFolder(), "lang");
        File[] files = langDir.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                set.add(name.substring("messages_".length(), name.length() - 4));
            }
        }
        // From jar (at least our known set)
        for (String loc : new String[]{"en_US", "es_ES", "fr_FR"}) {
            if (plugin.getResource("lang/messages_" + loc + ".yml") != null) {
                set.add(loc);
            }
        }
        return new ArrayList<>(set);
    }

    // Player locale handling
    public String getPlayerLocale(UUID uuid) {
        if (uuid == null) return serverDefaultLocale;
        return playerData.getString("locales." + uuid.toString(), serverDefaultLocale);
    }

    public void setPlayerLocale(UUID uuid, String locale) {
        if (uuid == null || locale == null || locale.isEmpty()) return;
        playerData.set("locales." + uuid.toString(), locale);
        try {
            playerData.save(playerDataFile);
        } catch (Exception ignored) {
        }
    }

    public boolean hasPlayerLocale(UUID uuid) {
        if (uuid == null) return false;
        return playerData.contains("locales." + uuid.toString());
    }

    public String getServerDefaultLocale() {
        return serverDefaultLocale;
    }

    // Server-default message (no player context)
    public String get(String key) {
        return getInternal(null, key, null);
    }

    public String get(String key, Map<String, String> placeholders) {
        return getInternal(null, key, placeholders);
    }

    public String get(String key, String... kvPairs) {
        Map<String, String> map = toMap(kvPairs);
        return getInternal(null, key, map);
    }

    // Player-specific messages
    public String getFor(UUID uuid, String key) {
        return getInternal(uuid, key, null);
    }

    public String getFor(UUID uuid, String key, Map<String, String> placeholders) {
        return getInternal(uuid, key, placeholders);
    }

    public String getFor(UUID uuid, String key, String... kvPairs) {
        Map<String, String> map = toMap(kvPairs);
        return getInternal(uuid, key, map);
    }

    private Map<String, String> toMap(String... kvPairs) {
        Map<String, String> map = new HashMap<>();
        if (kvPairs != null) {
            for (int i = 0; i + 1 < kvPairs.length; i += 2) {
                map.put(kvPairs[i], kvPairs[i + 1]);
            }
        }
        return map;
    }

    private String getInternal(UUID uuid, String key, Map<String, String> placeholders) {
        // 1) Config override takes precedence (server-wide)
        String cfg = plugin.getConfig().getString("messages." + key);
        String raw = null;
        if (cfg != null) {
            raw = cfg;
        } else {
            // 2) Player locale
            String loc = uuid != null ? getPlayerLocale(uuid) : serverDefaultLocale;
            YamlConfiguration playerYaml = getLocaleYaml(loc);
            raw = playerYaml.getString(key);
            if (raw == null) {
                // 3) Server default locale
                YamlConfiguration serverYaml = getLocaleYaml(serverDefaultLocale);
                raw = serverYaml.getString(key);
                if (raw == null) {
                    // 4) Default English bundle
                    raw = defaultLangConfig.getString(key);
                }
            }
        }

        if (raw == null) raw = key; // fallback to key
        String msg = applyPlaceholders(raw, placeholders);
        return colorize(msg);
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders) {
        if (input == null) return null;
        String out = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return out;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
