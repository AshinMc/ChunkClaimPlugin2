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
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages chunk claims with named claim groups.
 * Each claim has: chunkKey -> { owner UUID, claim name }.
 * A "claim group" is all chunks sharing the same (owner, name).
 */
public class ChunkManager {
    private final JavaPlugin plugin;
    // chunkKey -> owner UUID
    private final Map<String, UUID> chunkOwners = new ConcurrentHashMap<>();
    // chunkKey -> claim name
    private final Map<String, String> chunkNames = new ConcurrentHashMap<>();
    // Trust: "ownerUUID:claimNameLowerCase" -> Set of trusted player UUIDs
    private final Map<String, Set<UUID>> trustedPlayers = new ConcurrentHashMap<>();
    // Claim flags: "ownerUUID:claimNameLowerCase" -> { flagName -> enabled }
    private final Map<String, Map<String, Boolean>> claimFlags = new ConcurrentHashMap<>();
    // Player individual limits: UUID -> limit
    private final Map<UUID, Integer> playerLimits = new ConcurrentHashMap<>();
    // Claim marketplace prices: "ownerUUID:claimNameLowerCase" -> price
    private final Map<String, Double> claimPrices = new ConcurrentHashMap<>();
    // Recorded original server purchase cost per chunk key ("world:x:z" -> cost; 0.0 for free/traded chunks)
    private final Map<String, Double> chunkPaidCosts = new ConcurrentHashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;
    public final WorldGuardBridge worldGuardHandler;
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CCP-Async-Persistence");
        t.setDaemon(true);
        return t;
    });

    public ChunkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "chunkclaims.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        boolean hasWG = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        this.worldGuardHandler = hasWG ? new WG7WorldGuardBridge() : new NoopWorldGuardBridge();
        loadData();
    }

    public static final int MAX_CLAIM_NAME_LENGTH = 24;
    private static final java.util.regex.Pattern CLAIM_NAME_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * Validates that a claim name is 1-24 characters matching [a-zA-Z0-9_-].
     */
    public static boolean isValidClaimName(String name) {
        return name != null && !name.isEmpty() && name.length() <= MAX_CLAIM_NAME_LENGTH
                && CLAIM_NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Generates a unique default claim name (e.g. Claim_1) not already used by the player.
     */
    public String generateDefaultClaimName(UUID playerId) {
        int next = getPlayerClaimNames(playerId).size() + 1;
        while (hasClaimName(playerId, "Claim_" + next)) {
            next++;
        }
        return "Claim_" + next;
    }

    // ── Basic chunk operations ──

    public boolean isWorldDisabled(org.bukkit.World world) {
        if (world == null || plugin == null) return false;
        String worldName = world.getName();
        for (String disabled : plugin.getConfig().getStringList("disabled-worlds")) {
            if (disabled != null && disabled.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean canClaimChunk(Chunk chunk, Player player) {
        if (isWorldDisabled(chunk.getWorld())) return false;
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

    /**
     * Check if a chunk is claimed.
     */
    public boolean isChunkClaimed(Chunk chunk) {
        return chunkOwners.containsKey(getChunkKey(chunk));
    }

    // ── Claim with name ──

    /**
     * Claim a chunk with a given name. Returns true on success.
     */
    public boolean claimChunk(Player player, Chunk chunk, String name) {
        if (!canClaimChunk(chunk, player)) return false;
        org.ashin.chunkClaimPlugin2.api.events.ChunkClaimEvent event = new org.ashin.chunkClaimPlugin2.api.events.ChunkClaimEvent(player, chunk, name);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        name = event.getClaimName();
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
        String claimName = chunkNames.getOrDefault(key, "world");
        org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent event = new org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent(owner, claimName, chunk);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        chunkOwners.remove(key);
        chunkNames.remove(key);
        chunkPaidCosts.remove(key);
        return true;
    }

    public boolean unclaimChunk(UUID playerId, String world, int x, int z) {
        String key = getChunkKey(world, x, z);
        UUID owner = chunkOwners.get(key);
        if (owner == null || !owner.equals(playerId)) return false;
        String claimName = chunkNames.getOrDefault(key, "world");
        org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent event = new org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent(owner, claimName, null);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        chunkOwners.remove(key);
        chunkNames.remove(key);
        chunkPaidCosts.remove(key);
        return true;
    }

    /**
     * Rename an entire named claim group.
     * @return true if successful, false if name already exists or not found
     */
    public boolean renameClaim(UUID playerId, String oldName, String newName) {
        if (hasClaimName(playerId, newName)) return false;
        org.ashin.chunkClaimPlugin2.api.events.ChunkRenameEvent event = new org.ashin.chunkClaimPlugin2.api.events.ChunkRenameEvent(playerId, oldName, newName);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        newName = event.getNewName();

        boolean renamed = false;
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                String n = chunkNames.getOrDefault(entry.getKey(), "world");
                if (n.equalsIgnoreCase(oldName)) {
                    chunkNames.put(entry.getKey(), newName);
                    renamed = true;
                }
            }
        }

        if (renamed) {
            String oldKey = trustKey(playerId, oldName);
            String newKey = trustKey(playerId, newName);

            if (trustedPlayers.containsKey(oldKey)) {
                trustedPlayers.put(newKey, trustedPlayers.remove(oldKey));
            }
            if (claimFlags.containsKey(oldKey)) {
                claimFlags.put(newKey, claimFlags.remove(oldKey));
            }
            if (claimPrices.containsKey(oldKey)) {
                claimPrices.put(newKey, claimPrices.remove(oldKey));
            }
        }
        return renamed;
    }

    /**
     * Transfer a named claim group from one owner to another.
     * @return number of chunks transferred
     */
    public int transferClaim(UUID oldOwner, UUID newOwner, String claimName) {
        if (oldOwner.equals(newOwner)) return 0;
        org.ashin.chunkClaimPlugin2.api.events.ChunkTransferEvent event = new org.ashin.chunkClaimPlugin2.api.events.ChunkTransferEvent(oldOwner, newOwner, claimName);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return 0;

        int transferred = 0;
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(oldOwner)) {
                String n = chunkNames.getOrDefault(entry.getKey(), "world");
                if (n.equalsIgnoreCase(claimName)) {
                    entry.setValue(newOwner);
                    // Traded/transferred claims carry zero server refund value to prevent refund arbitrage
                    chunkPaidCosts.put(entry.getKey(), 0.0);
                    transferred++;
                }
            }
        }

        if (transferred > 0) {
            String oldKey = trustKey(oldOwner, claimName);
            String newKey = trustKey(newOwner, claimName);

            // Clear seller's trusted players upon ownership transfer so friends do not retain access
            trustedPlayers.remove(oldKey);
            if (claimFlags.containsKey(oldKey)) {
                claimFlags.put(newKey, claimFlags.remove(oldKey));
            }
            claimPrices.remove(oldKey);
            claimPrices.remove(newKey);
        }
        return transferred;
    }

    /**
     * Unclaim an entire named claim group (all chunks with matching name for this player).
     * @return number of chunks unclaimed
     */
    public int unclaimByName(UUID playerId, String name) {
        org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent event = new org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent(playerId, name, null);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return 0;

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
            chunkPaidCosts.remove(key);
        }
        // Clean up trust, flag, and price data for this claim group
        if (!toRemove.isEmpty()) {
            String tk = trustKey(playerId, name);
            trustedPlayers.remove(tk);
            claimFlags.remove(tk);
            claimPrices.remove(tk);
        }
        return toRemove.size();
    }

    // ── Admin unclaim operations ──

    public static class AdminUnclaimResult {
        public final UUID owner;
        public final String claimName;
        public final boolean success;
        public final int remainingInGroup;

        public AdminUnclaimResult(UUID owner, String claimName, boolean success, int remainingInGroup) {
            this.owner = owner;
            this.claimName = claimName;
            this.success = success;
            this.remainingInGroup = remainingInGroup;
        }
    }

    /**
     * Forcibly unclaim a specific chunk by an admin regardless of ownership.
     */
    public AdminUnclaimResult adminUnclaimChunk(Chunk chunk) {
        String key = getChunkKey(chunk);
        UUID owner = chunkOwners.get(key);
        if (owner == null) return null;
        String claimName = chunkNames.getOrDefault(key, "world");

        org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent event =
                new org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent(owner, claimName, chunk);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return new AdminUnclaimResult(owner, claimName, false, -1);

        chunkOwners.remove(key);
        chunkNames.remove(key);
        chunkPaidCosts.remove(key);

        int remaining = 0;
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(owner)) {
                String n = chunkNames.getOrDefault(entry.getKey(), "world");
                if (n.equalsIgnoreCase(claimName)) remaining++;
            }
        }

        if (remaining == 0) {
            String tk = trustKey(owner, claimName);
            trustedPlayers.remove(tk);
            claimFlags.remove(tk);
            claimPrices.remove(tk);
        }
        return new AdminUnclaimResult(owner, claimName, true, remaining);
    }

    /**
     * Forcibly unclaim an entire claim group for a specific player by an admin.
     */
    public int adminUnclaimGroup(UUID targetOwner, String claimName) {
        return unclaimByName(targetOwner, claimName);
    }

    /**
     * Forcibly unclaim ALL chunks owned by a player across all their claim groups.
     */
    public int adminUnclaimAll(UUID targetOwner) {
        List<String> toRemove = new ArrayList<>();
        Set<String> affectedGroups = new HashSet<>();
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(targetOwner)) {
                toRemove.add(entry.getKey());
                affectedGroups.add(chunkNames.getOrDefault(entry.getKey(), "world"));
            }
        }
        for (String key : toRemove) {
            chunkOwners.remove(key);
            chunkNames.remove(key);
            chunkPaidCosts.remove(key);
        }
        for (String group : affectedGroups) {
            String tk = trustKey(targetOwner, group);
            trustedPlayers.remove(tk);
            claimFlags.remove(tk);
            claimPrices.remove(tk);
        }
        return toRemove.size();
    }

    /**
     * Records the original server purchase cost of a chunk (0.0 for free/transferred claims).
     */
    public void setChunkPaidCost(Chunk chunk, double cost) {
        chunkPaidCosts.put(getChunkKey(chunk), Math.max(0.0, cost));
    }

    /**
     * Returns the recorded paid cost for a chunk, or null if untracked (legacy chunk).
     */
    public Double getTrackedChunkPaidCost(Chunk chunk) {
        return chunkPaidCosts.get(getChunkKey(chunk));
    }

    /**
     * Returns recorded paid costs (nullable entries for legacy chunks) for all chunks in a claim group.
     */
    public List<Double> getGroupTrackedPaidCosts(UUID playerId, String claimName) {
        List<Double> costs = new ArrayList<>();
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                String n = chunkNames.getOrDefault(entry.getKey(), "world");
                if (n.equalsIgnoreCase(claimName)) {
                    costs.add(chunkPaidCosts.get(entry.getKey()));
                }
            }
        }
        return costs;
    }

    // ── Claim marketplace operations ──

    public void setClaimPrice(UUID owner, String claimName, Double price) {
        String key = trustKey(owner, claimName);
        if (price == null || price <= 0.0) {
            claimPrices.remove(key);
        } else {
            claimPrices.put(key, price);
        }
    }

    public Double getClaimPrice(UUID owner, String claimName) {
        return claimPrices.get(trustKey(owner, claimName));
    }

    public boolean isClaimForSale(UUID owner, String claimName) {
        Double p = getClaimPrice(owner, claimName);
        return p != null && p > 0.0;
    }

    public Map<String, Double> getAllClaimsForSale() {
        return new HashMap<>(claimPrices);
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

    /**
     * Thread-safe total count of claimed chunks without copying the map.
     */
    public int getTotalClaimedChunkCount() {
        return chunkOwners.size();
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
        Set<UUID> set = trustedPlayers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
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
    public static final String FLAG_MOB_PROTECTION = "mob-protection";
    public static final String FLAG_EXPLOSIONS   = "explosions";
    public static final String FLAG_PVP          = "pvp";
    public static final String FLAG_FIRE_SPREAD  = "fire-spread";
    public static final String FLAG_GREETING_TITLE = "greeting-title";
    
    public static final String FLAG_INTERACT_CHEST = "interact-chest";
    public static final String FLAG_INTERACT_FURNACE = "interact-furnace";
    public static final String FLAG_INTERACT_STONECUTTER = "interact-stonecutter";
    public static final String FLAG_INTERACT_DOOR = "interact-door";
    public static final String FLAG_INTERACT_REDSTONE = "interact-redstone";
    public static final String FLAG_INTERACT_VILLAGERS = "interact-villagers";

    /** Ordered list for GUI display. */
    public static final String[] ALL_FLAGS = {
        FLAG_MOB_GRIEFING, FLAG_MOB_SPAWNING, FLAG_MOB_ENTRY, FLAG_MOB_PROTECTION, FLAG_EXPLOSIONS, FLAG_PVP, FLAG_FIRE_SPREAD, FLAG_GREETING_TITLE,
        FLAG_INTERACT_CHEST, FLAG_INTERACT_FURNACE, FLAG_INTERACT_STONECUTTER, FLAG_INTERACT_DOOR, FLAG_INTERACT_REDSTONE, FLAG_INTERACT_VILLAGERS
    };

    /** Default values: true = protection enabled / blocked action, false = allowed action */
    public static final Map<String, Boolean> DEFAULT_FLAGS = Map.ofEntries(
        Map.entry(FLAG_MOB_GRIEFING, true),   // block mob griefing by default
        Map.entry(FLAG_MOB_SPAWNING, false),  // allow mob spawning by default
        Map.entry(FLAG_MOB_ENTRY,    false),  // allow all mob entry by default
        Map.entry(FLAG_MOB_PROTECTION, true), // protect non-hostile mobs by default
        Map.entry(FLAG_EXPLOSIONS,   true),   // block explosions by default
        Map.entry(FLAG_PVP,          true),   // block PvP combat by default (safe-zone default)
        Map.entry(FLAG_FIRE_SPREAD,  true),   // block fire spread by default
        Map.entry(FLAG_GREETING_TITLE, true), // show welcome title by default
        Map.entry(FLAG_INTERACT_CHEST, true),      // protect chests by default
        Map.entry(FLAG_INTERACT_FURNACE, true),    // protect furnaces by default
        Map.entry(FLAG_INTERACT_STONECUTTER, true),// protect stonecutters by default
        Map.entry(FLAG_INTERACT_DOOR, true),       // protect doors by default
        Map.entry(FLAG_INTERACT_REDSTONE, true),   // protect redstone (buttons/levers) by default
        Map.entry(FLAG_INTERACT_VILLAGERS, true)   // protect villager trading by default
    );

    /**
     * Purges a specific claim flag across all registered claim groups.
     * When removed, claims will fall back to the server default.
     * @param flag the flag identifier to remove
     * @return the number of claim groups where the flag was removed
     */
    public int removeFlagFromAllClaims(String flag) {
        int count = 0;
        for (Map<String, Boolean> flags : claimFlags.values()) {
            if (flags.remove(flag) != null) {
                count++;
            }
        }
        if (count > 0) {
            saveData();
        }
        return count;
    }

    /**
     * Overrides a specific claim flag value across all registered claim groups.
     * @param flag the flag identifier to set
     * @param value the new boolean state
     * @return the number of claim groups updated
     */
    public int setFlagOnAllClaims(String flag, boolean value) {
        int count = 0;
        Set<String> allKeys = new HashSet<>();
        for (Map.Entry<String, UUID> entry : chunkOwners.entrySet()) {
            String claimName = chunkNames.getOrDefault(entry.getKey(), "world");
            allKeys.add(trustKey(entry.getValue(), claimName));
        }
        for (String key : allKeys) {
            Map<String, Boolean> flags = claimFlags.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
            flags.put(flag, value);
            count++;
        }
        if (count > 0) {
            saveData();
        }
        return count;
    }

    /**
     * Get a claim flag value. Returns the default if not explicitly set.
     */
    public boolean getClaimFlag(UUID owner, String claimName, String flag) {
        Map<String, Boolean> flags = claimFlags.get(trustKey(owner, claimName));
        if (flags != null && flags.containsKey(flag)) return flags.get(flag);
        return getDefaultFlag(flag);
    }

    /**
     * Convenience: check a flag for a specific chunk (looks up owner + claim name).
     */
    public boolean isChunkFlagEnabled(Chunk chunk, String flag) {
        UUID owner = getChunkOwner(chunk);
        if (owner == null) return false;
        String name = getChunkClaimName(chunk);
        if (name == null) return getDefaultFlag(flag);
        return getClaimFlag(owner, name, flag);
    }

    /**
     * Set a claim flag value.
     */
    public void setClaimFlag(UUID owner, String claimName, String flag, boolean value) {
        String key = trustKey(owner, claimName);
        Map<String, Boolean> flags = claimFlags.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        flags.put(flag, value);
    }

    /**
     * Set an individual claim limit for a player. Set to null or a negative value to remove.
     */
    public void setPlayerLimit(UUID playerId, Integer limit) {
        if (limit == null || limit < 0) {
            playerLimits.remove(playerId);
        } else {
            playerLimits.put(playerId, limit);
        }
    }

    /**
     * Get the player's individual limit if it exists, otherwise return the default limit.
     */
    public int getPlayerLimit(UUID playerId, int defaultLimit) {
        return playerLimits.getOrDefault(playerId, defaultLimit);
    }

    /**
     * Get the default value for a flag.
     * Checks server config.yml under claim-flags.<flag>, falling back to hardcoded DEFAULT_FLAGS.
     */
    public boolean getDefaultFlag(String flag) {
        if (plugin != null && plugin.getConfig() != null) {
            if (FLAG_PVP.equals(flag)) {
                if (plugin.getConfig().contains("claim-flags.block-pvp")) {
                    return plugin.getConfig().getBoolean("claim-flags.block-pvp");
                }
                if (plugin.getConfig().contains("claim-flags.pvp")) {
                    return plugin.getConfig().getBoolean("claim-flags.pvp");
                }
            }
            if (plugin.getConfig().contains("claim-flags." + flag)) {
                return plugin.getConfig().getBoolean("claim-flags." + flag);
            }
        }
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
        chunkPaidCosts.clear();
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
                            if (section.contains(key + ".paid-cost")) {
                                chunkPaidCosts.put(key, Math.max(0.0, section.getDouble(key + ".paid-cost", 0.0)));
                            }
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
                    Set<UUID> set = ConcurrentHashMap.newKeySet();
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
                        Map<String, Boolean> flags = new ConcurrentHashMap<>();
                        for (String flag : inner.getKeys(false)) {
                            flags.put(flag, inner.getBoolean(flag));
                        }
                        if (!flags.isEmpty()) claimFlags.put(claimKey, flags);
                    }
                }
            }
        }

        // Load player limits
        playerLimits.clear();
        if (dataConfig.contains("player-limits")) {
            ConfigurationSection limitsSection = dataConfig.getConfigurationSection("player-limits");
            if (limitsSection != null) {
                for (String uuidStr : limitsSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        int limit = limitsSection.getInt(uuidStr);
                        playerLimits.put(uuid, limit);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in player-limits: " + uuidStr);
                    }
                }
            }
        }

        // Load claim marketplace prices
        claimPrices.clear();
        if (dataConfig.contains("claim-prices")) {
            ConfigurationSection priceSection = dataConfig.getConfigurationSection("claim-prices");
            if (priceSection != null) {
                for (String key : priceSection.getKeys(false)) {
                    double price = priceSection.getDouble(key, 0.0);
                    if (price > 0.0) {
                        claimPrices.put(key, price);
                    }
                }
            }
        }
    }

    public void saveData() {
        DataSnapshot snapshot = captureSnapshot();
        if (plugin != null && !plugin.isEnabled()) {
            writeSnapshotAtomically(snapshot);
            return;
        }
        saveExecutor.submit(() -> writeSnapshotAtomically(snapshot));
    }

    /**
     * Synchronously captures and atomically persists claim data to disk.
     */
    public void saveDataSync() {
        writeSnapshotAtomically(captureSnapshot());
    }

    /**
     * Flushes pending asynchronous saves and performs a final synchronous atomic save on disable.
     */
    public void shutdown() {
        saveExecutor.shutdown();
        try {
            saveExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        saveDataSync();
    }

    private DataSnapshot captureSnapshot() {
        Map<String, UUID> ownersCopy = new HashMap<>(chunkOwners);
        Map<String, String> namesCopy = new HashMap<>(chunkNames);
        Map<String, List<String>> trustsCopy = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : trustedPlayers.entrySet()) {
            List<String> uuids = new ArrayList<>(entry.getValue().size());
            for (UUID u : entry.getValue()) {
                uuids.add(u.toString());
            }
            trustsCopy.put(entry.getKey(), uuids);
        }
        Map<String, Map<String, Boolean>> flagsCopy = new HashMap<>();
        for (Map.Entry<String, Map<String, Boolean>> entry : claimFlags.entrySet()) {
            flagsCopy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        Map<UUID, Integer> limitsCopy = new HashMap<>(playerLimits);
        Map<String, Double> pricesCopy = new HashMap<>(claimPrices);
        Map<String, Double> paidCostsCopy = new HashMap<>(chunkPaidCosts);
        return new DataSnapshot(ownersCopy, namesCopy, trustsCopy, flagsCopy, limitsCopy, pricesCopy, paidCostsCopy);
    }

    private void writeSnapshotAtomically(DataSnapshot snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, UUID> entry : snapshot.owners.entrySet()) {
            String key = entry.getKey();
            yaml.set("claims-v2." + key + ".owner", entry.getValue().toString());
            yaml.set("claims-v2." + key + ".name", snapshot.names.getOrDefault(key, "world"));
            Double paidCost = snapshot.paidCosts.get(key);
            if (paidCost != null) {
                yaml.set("claims-v2." + key + ".paid-cost", paidCost);
            }
        }
        for (Map.Entry<String, List<String>> entry : snapshot.trusts.entrySet()) {
            yaml.set("trusts." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Map<String, Boolean>> entry : snapshot.flags.entrySet()) {
            for (Map.Entry<String, Boolean> flag : entry.getValue().entrySet()) {
                yaml.set("claim-flags." + entry.getKey() + "." + flag.getKey(), flag.getValue());
            }
        }
        for (Map.Entry<UUID, Integer> entry : snapshot.limits.entrySet()) {
            yaml.set("player-limits." + entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<String, Double> entry : snapshot.prices.entrySet()) {
            yaml.set("claim-prices." + entry.getKey(), entry.getValue());
        }

        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Path targetPath = dataFile.toPath();
            Path tempPath = targetPath.resolveSibling(dataFile.getName() + ".tmp");
            Files.writeString(tempPath, yaml.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().severe("Could not save chunk claim data: " + e.getMessage());
            }
        }
    }

    private record DataSnapshot(
            Map<String, UUID> owners,
            Map<String, String> names,
            Map<String, List<String>> trusts,
            Map<String, Map<String, Boolean>> flags,
            Map<UUID, Integer> limits,
            Map<String, Double> prices,
            Map<String, Double> paidCosts
    ) {}
}
