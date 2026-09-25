package org.ashin.chunkClaimPlugin2.api;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChunkClaimAPI {
    private static ChunkClaimAPI instance;
    private final ChunkManager chunkManager;

    private ChunkClaimAPI(@NotNull ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    public static void init(@NotNull ChunkManager chunkManager) {
        instance = new ChunkClaimAPI(chunkManager);
    }

    public static @NotNull ChunkClaimAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ChunkClaimAPI is not initialized yet!");
        }
        return instance;
    }

    public @NotNull ChunkManager getChunkManager() {
        return chunkManager;
    }

    public boolean isChunkClaimed(@NotNull Chunk chunk) {
        return chunkManager.isChunkClaimed(chunk);
    }

    public @Nullable UUID getChunkOwner(@NotNull Chunk chunk) {
        return chunkManager.getChunkOwner(chunk);
    }

    public @Nullable String getChunkClaimName(@NotNull Chunk chunk) {
        return chunkManager.getChunkClaimName(chunk);
    }

    public boolean canClaimChunk(@NotNull Chunk chunk, @NotNull Player player) {
        return chunkManager.canClaimChunk(chunk, player);
    }

    public int getPlayerChunkCount(@NotNull UUID playerId) {
        return chunkManager.getPlayerChunkCount(playerId);
    }

    public @NotNull List<String> getPlayerClaimNames(@NotNull UUID playerId) {
        return chunkManager.getPlayerClaimNames(playerId);
    }

    public @NotNull List<ChunkData> getPlayerChunks(@NotNull UUID playerId) {
        return chunkManager.getPlayerChunks(playerId);
    }

    public boolean isTrusted(@NotNull UUID owner, @NotNull String claimName, @NotNull UUID playerId) {
        return chunkManager.isTrusted(owner, claimName, playerId);
    }

    public @NotNull Set<UUID> getTrustedPlayers(@NotNull UUID owner, @NotNull String claimName) {
        return chunkManager.getTrustedPlayers(owner, claimName);
    }

    public boolean getClaimFlag(@NotNull UUID owner, @NotNull String claimName, @NotNull String flag) {
        return chunkManager.getClaimFlag(owner, claimName, flag);
    }

    public boolean isClaimForSale(@NotNull UUID owner, @NotNull String claimName) {
        return chunkManager.isClaimForSale(owner, claimName);
    }

    public @Nullable Double getClaimPrice(@NotNull UUID owner, @NotNull String claimName) {
        return chunkManager.getClaimPrice(owner, claimName);
    }

    public ChunkManager.AdminUnclaimResult adminUnclaimChunk(@NotNull Chunk chunk) {
        return chunkManager.adminUnclaimChunk(chunk);
    }

    public int adminUnclaimGroup(@NotNull UUID owner, @NotNull String claimName) {
        return chunkManager.adminUnclaimGroup(owner, claimName);
    }

    public int adminUnclaimAll(@NotNull UUID owner) {
        return chunkManager.adminUnclaimAll(owner);
    }

    public int removeFlagFromAllClaims(@NotNull String flag) {
        return chunkManager.removeFlagFromAllClaims(flag);
    }

    public int setFlagOnAllClaims(@NotNull String flag, boolean value) {
        return chunkManager.setFlagOnAllClaims(flag, value);
    }
}
