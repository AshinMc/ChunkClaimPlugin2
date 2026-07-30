package org.ashin.chunkClaimPlugin2.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ChunkTransferEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final UUID oldOwnerId;
    private final UUID newOwnerId;
    private final String claimName;

    public ChunkTransferEvent(@NotNull UUID oldOwnerId, @NotNull UUID newOwnerId, @NotNull String claimName) {
        this.oldOwnerId = oldOwnerId;
        this.newOwnerId = newOwnerId;
        this.claimName = claimName;
    }

    public @NotNull UUID getOldOwnerId() {
        return oldOwnerId;
    }

    public @NotNull UUID getNewOwnerId() {
        return newOwnerId;
    }

    public @NotNull String getClaimName() {
        return claimName;
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
