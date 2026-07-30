package org.ashin.chunkClaimPlugin2.api.events;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ChunkClaimEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final Chunk chunk;
    private String claimName;

    public ChunkClaimEvent(@NotNull Player player, @NotNull Chunk chunk, @NotNull String claimName) {
        this.player = player;
        this.chunk = chunk;
        this.claimName = claimName;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Chunk getChunk() {
        return chunk;
    }

    public @NotNull String getClaimName() {
        return claimName;
    }

    public void setClaimName(@NotNull String claimName) {
        this.claimName = claimName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
