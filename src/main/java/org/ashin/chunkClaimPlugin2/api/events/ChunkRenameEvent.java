package org.ashin.chunkClaimPlugin2.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ChunkRenameEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final UUID ownerId;
    private final String oldName;
    private String newName;

    public ChunkRenameEvent(@NotNull UUID ownerId, @NotNull String oldName, @NotNull String newName) {
        this.ownerId = ownerId;
        this.oldName = oldName;
        this.newName = newName;
    }

    public @NotNull UUID getOwnerId() {
        return ownerId;
    }

    public @NotNull String getOldName() {
        return oldName;
    }

    public @NotNull String getNewName() {
        return newName;
    }

    public void setNewName(@NotNull String newName) {
        this.newName = newName;
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
