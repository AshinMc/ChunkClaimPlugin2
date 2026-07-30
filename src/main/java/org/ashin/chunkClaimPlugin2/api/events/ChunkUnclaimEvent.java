package org.ashin.chunkClaimPlugin2.api.events;

import org.bukkit.Chunk;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ChunkUnclaimEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final UUID ownerId;
    private final String claimName;
    private final Chunk chunk;

    public ChunkUnclaimEvent(@NotNull UUID ownerId, @NotNull String claimName, @Nullable Chunk chunk) {
        this.ownerId = ownerId;
        this.claimName = claimName;
        this.chunk = chunk;
    }

    public @NotNull UUID getOwnerId() {
        return ownerId;
    }

    public @NotNull String getClaimName() {
        return claimName;
    }

    public @Nullable Chunk getChunk() {
        return chunk;
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
