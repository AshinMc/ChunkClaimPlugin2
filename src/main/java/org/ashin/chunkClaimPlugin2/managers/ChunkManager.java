package org.ashin.chunkClaimPlugin2.managers;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.handlers.NoopWorldGuardBridge;
import org.ashin.chunkClaimPlugin2.handlers.WG7WorldGuardBridge;
import org.ashin.chunkClaimPlugin2.handlers.WorldGuardBridge;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages chunk claims with named claim groups.
 * Each claim has: chunkKey -> { owner UUID, claim name }.
 * A "claim group" is all chunks sharing the same (owner, name).
 */
public class ChunkManager {
    private final JavaPlugin plugin;
    // chunkKey -> owner UUID
    private final Map<String, UUID> chunkOwners = new HashMap<>();
    // chunkKey -> claim name
    private final Map<String, String> chunkNames = new HashMap<>();
    // Trust: "ownerUUID:claimNameLowerCase" -> Set of trusted player UUIDs
    private final Map<String, Set<UUID>> trustedPlayers = new HashMap<>();
    // Claim flags: "ownerUUID:claimNameLowerCase" -> { flagName -> enabled }
    private final Map<String, Map<String, Boolean>> claimFlags = new HashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;
    public final WorldGuardBridge worldGuardHandler;

    public ChunkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "chunkclaims.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        boolean hasWG = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        this.worldGuardHandler = hasWG ? new WG7WorldGuardBridge() : new NoopWorldGuardBridge();
        loadData();
    }

    // ── Basic chunk operations ──

    public boolean canClaimChunk(Chunk chunk, Player player) {
        String chunkKey = getChunkKey(chunk);
        if (chunkOwners.containsKey(chunkKey)) return false;
        return worldGuardHandler.canClaimChunk(chunk, player);
    }

    public UUID getChunkOwner(Chunk chunk) {
        return chunkOwners.get(getChunkKey(chunk));
    }

    public UUID getChunkOwner(String chunkKey) {
        return chunkOwners.get(chunkKey);
    }

    public String getChunkClaimName(Chunk chunk) {
        return chunkNames.get(getChunkKey(chunk));
    }

    public String getChunkClaimName(String chunkKey) {
        return chunkNames.get(chunkKey);
    }

    // ── Claim with name ──

    /**
     * Claim a chunk with a given name. Returns true on success.
     */
    public boolean claimChunk(Player player, Chunk chunk, String name) {
        if (!canClaimChunk(chunk, player)) return false;
        String key = getChunkKey(chunk);
        chunkOwners.put(key, player.getUniqueId());
        chunkNames.put(key, name);
        return true;
    }

    /** Legacy: claim with default name */
    public boolean claimChunk(Player player, Chunk chunk) {
        return claimChunk(player, chunk, "world");
    }

    // ── Unclaim operations ──

    public boolean unclaimChunk(Player player, Chunk chunk) {
        String key = getChunkKey(chunk);
        UUID owner = chunkOwners.get(key);
        if (owner == null || !owner.equals(player.getUniqueId())) return false;
        chunkOwners.remove(key);
        chunkNames.remove(key);
        return true;
    }

    public boolean unclaimChunk(UUID playerId, String world, int x, int z) {
        String key = getChunkKey(world, x, z);
        UUID owner = chunkOwners.get(key);
        if (owner == null || !owner.equals(playerId)) return false;
        chunkOwners.remove(key);
        chunkNames.remove(key);
        return true;
    }

    /**
     * Unclaim an entire named claim group (all chunks with matching name for this player).
     * @return number of chunks unclaimed
     */
    public int unclaimByName(UUID playerId, String name) {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                String n = chunkNames.getOrDefault(entry.getKey(), "world");
                if (n.equalsIgnoreCase(name)) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        for (String key : toRemove) {
            chunkOwners.remove(key);
            chunkNames.remove(key);
        }
        // Clean up trust data for this claim group
        if (!toRemove.isEmpty()) {
            String tk = trustKey(playerId, name);
            trustedPlayers.remove(tk);
            claimFlags.remove(tk);
        }
        return toRemove.size();
    }

    // ── Query operations ──

    /**
     * Get all individual chunks owned by a player (with claim names set).
     */
    public List<ChunkData> getPlayerChunks(UUID playerId) {
        List<ChunkData> result = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                ChunkData cd = parseChunkKey(entry.getKey());
                if (cd != null) {
                    cd.setClaimName(chunkNames.getOrDefault(entry.getKey(), "world"));
                    result.add(cd);
                }
            }
        }
        return result;
    }

    /**
     * Get the distinct claim group names for a player.
     */
    public List<String> getPlayerClaimNames(UUID playerId) {
        Set<String> names = new LinkedHashSet<>();
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                names.add(chunkNames.getOrDefault(entry.getKey(), "world"));
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * Get all chunks in a named claim group for a player.
     */
    public List<ChunkData> getChunksByName(UUID playerId, String name) {
        List<ChunkData> result = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                String n = chunkNames.getOrDefault(entry.getKey(), "world");
                if (n.equalsIgnoreCase(name)) {
                    ChunkData cd = parseChunkKey(entry.getKey());
                    if (cd != null) {
                        cd.setClaimName(n);
                        result.add(cd);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns total number of individual chunks a player owns (across all claim groups).
     */
    public int getPlayerChunkCount(UUID playerId) {
        int count = 0;
        for (UUID owner : chunkOwners.values()) {
            if (owner.equals(playerId)) count++;
        }
        return count;
    }

    /**
     * Check if a player already has a claim group with the given name.
     */
    public boolean hasClaimName(UUID playerId, String name) {
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                String n = chunkNames.getOrDefault(entry.getKey(), "world");
                if (n.equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }

    /**
     * Returns a copy of all claimed chunks (key -> owner).
     */
    public Map<String, UUID> getClaimedChunks() {
        return new HashMap<>(chunkOwners);
    }

    // ── Trust operations ──

    private String trustKey(UUID owner, String claimName) {
        return owner.toString() + ":" + claimName.toLowerCase();
    }

    /**
     * Check if a player is trusted on a specific claim group.
     */
    public boolean isTrusted(UUID owner, String claimName, UUID playerId) {
        Set<UUID> trusted = trustedPlayers.get(trustKey(owner, claimName));
        return trusted != null && trusted.contains(playerId);
    }

    /**
     * Add or remove a trusted player on a claim group.
     */
    public void setTrusted(UUID owner, String claimName, UUID playerId, boolean trust) {
        String key = trustKey(owner, claimName);
        Set<UUID> set = trustedPlayers.computeIfAbsent(key, k -> new HashSet<>());
        if (trust) set.add(playerId);
        else set.remove(playerId);
        if (set.isEmpty()) trustedPlayers.remove(key);
    }

    /**
     * Get all trusted player UUIDs for a claim group.
     */
    public Set<UUID> getTrustedPlayers(UUID owner, String claimName) {
        return trustedPlayers.getOrDefault(trustKey(owner, claimName), Collections.emptySet());
    }

    // ── Claim flag operations ──

    /** All known claim flags with their defaults. */
    public static final String FLAG_MOB_GRIEFING = "mob-griefing";
    public static final String FLAG_MOB_SPAWNING = "mob-spawning";
    public static final String FLAG_MOB_ENTRY    = "mob-entry";
    public static final String FLAG_EXPLOSIONS   = "explosions";
    public static final String FLAG_PVP          = "pvp";

    /** Ordered list for GUI display. */
    public static final String[] ALL_FLAGS = {
        FLAG_MOB_GRIEFING, FLAG_MOB_SPAWNING, FLAG_MOB_ENTRY, FLAG_EXPLOSIONS, FLAG_PVP
    };

    /** Default values: true = protection enabled. */
    private static final Map<String, Boolean> DEFAULT_FLAGS = Map.of(
        FLAG_MOB_GRIEFING, true,   // block mob griefing by default
        FLAG_MOB_SPAWNING, false,  // allow mob spawning by default
        FLAG_MOB_ENTRY,    false,  // allow mob entry by default
        FLAG_EXPLOSIONS,   true,   // block explosions by default
        FLAG_PVP,          false   // allow PvP by default
    );

    /**
     * Get a claim flag value. Returns the default if not explicitly set.
     */
    public boolean getClaimFlag(UUID owner, String claimName, String flag) {
        Map<String, Boolean> flags = claimFlags.get(trustKey(owner, claimName));
        if (flags != null && flags.containsKey(flag)) return flags.get(flag);
        return DEFAULT_FLAGS.getOrDefault(flag, false);
    }

    /**
     * Convenience: check a flag for a specific chunk (looks up owner + claim name).
     */
    public boolean isChunkFlagEnabled(Chunk chunk, String flag) {
        UUID owner = getChunkOwner(chunk);
        if (owner == null) return false;
        String name = getChunkClaimName(chunk);
        if (name == null) return DEFAULT_FLAGS.getOrDefault(flag, false);
        return getClaimFlag(owner, name, flag);
    }

    /**
     * Set a claim flag value.
     */
    public void setClaimFlag(UUID owner, String claimName, String flag, boolean value) {
        String key = trustKey(owner, claimName);
        Map<String, Boolean> flags = claimFlags.computeIfAbsent(key, k -> new HashMap<>());
        flags.put(flag, value);
    }

    /**
     * Get the default value for a flag.
     */
    public static boolean getDefaultFlag(String flag) {
        return DEFAULT_FLAGS.getOrDefault(flag, false);
    }

    // ── Key utilities ──

    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public String getChunkKey(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    private ChunkData parseChunkKey(String key) {
        String[] parts = key.split(":");
        if (parts.length < 3) {
            plugin.getLogger().warning("Invalid chunk key: " + key);
            return null;
        }
        try {
            return new ChunkData(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid chunk coords in key: " + key);
            return null;
        }
    }

    // ── Persistence ──

    public void loadData() {
        chunkOwners.clear();
        chunkNames.clear();
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // New format: claims-v2.<key>.owner / claims-v2.<key>.name
        if (dataConfig.contains("claims-v2")) {
            ConfigurationSection section = dataConfig.getConfigurationSection("claims-v2");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    String owner = section.getString(key + ".owner");
                    String name = section.getString(key + ".name", "world");
                    if (owner != null) {
                        try {
                            chunkOwners.put(key, UUID.fromString(owner));
                            chunkNames.put(key, name);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in claims-v2: " + key);
                        }
                    }
                }
            }
        }
        // Migrate old format if present and v2 was empty
        else if (dataConfig.contains("claims")) {
            ConfigurationSection section = dataConfig.getConfigurationSection("claims");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    String ownerStr = section.getString(key);
                    if (ownerStr != null) {
                        try {
                            chunkOwners.put(key, UUID.fromString(ownerStr));
                            chunkNames.put(key, "world");
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in legacy claims: " + key);
                        }
                    }
                }
                plugin.getLogger().info("Migrated " + chunkOwners.size() + " claims from legacy format.");
            }
        }

        // Load trust data
        trustedPlayers.clear();
        if (dataConfig.contains("trusts")) {
            ConfigurationSection trustSection = dataConfig.getConfigurationSection("trusts");
            if (trustSection != null) {
                for (String trustKey : trustSection.getKeys(false)) {
                    List<String> uuids = trustSection.getStringList(trustKey);
                    Set<UUID> set = new HashSet<>();
                    for (String u : uuids) {
                        try { set.add(UUID.fromString(u)); }
                        catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid trusted UUID in key " + trustKey + ": " + u);
                        }
                    }
                    if (!set.isEmpty()) trustedPlayers.put(trustKey, set);
                }
            }
        }

        // Load claim flags
        claimFlags.clear();
        if (dataConfig.contains("claim-flags")) {
            ConfigurationSection flagSection = dataConfig.getConfigurationSection("claim-flags");
            if (flagSection != null) {
                for (String claimKey : flagSection.getKeys(false)) {
                    ConfigurationSection inner = flagSection.getConfigurationSection(claimKey);
                    if (inner != null) {
                        Map<String, Boolean> flags = new HashMap<>();
                        for (String flag : inner.getKeys(false)) {
                            flags.put(flag, inner.getBoolean(flag));
                        }
                        if (!flags.isEmpty()) claimFlags.put(claimKey, flags);
                    }
                }
            }
        }
    }

    public void saveData() {
        // Clear both old and new sections
        dataConfig.set("claims", null);
        dataConfig.set("claims-v2", null);
        dataConfig.set("trusts", null);

        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            String key = entry.getKey();
            dataConfig.set("claims-v2." + key + ".owner", entry.getValue().toString());
            dataConfig.set("claims-v2." + key + ".name", chunkNames.getOrDefault(key, "world"));
        }

        // Save trust data
        for (Map.Entry<String, Set<UUID>> entry : trustedPlayers.entrySet()) {
            List<String> uuids = new ArrayList<>();
            for (UUID u : entry.getValue()) uuids.add(u.toString());
            dataConfig.set("trusts." + entry.getKey(), uuids);
        }

        // Save claim flags
        for (Map.Entry<String, Map<String, Boolean>> entry : claimFlags.entrySet()) {
            for (Map.Entry<String, Boolean> flag : entry.getValue().entrySet()) {
                dataConfig.set("claim-flags." + entry.getKey() + "." + flag.getKey(), flag.getValue());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chunk claim data: " + e.getMessage());
        }
    }
}