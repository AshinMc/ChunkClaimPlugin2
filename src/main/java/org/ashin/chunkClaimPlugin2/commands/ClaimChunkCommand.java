package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ClaimChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;

    public ClaimChunkCommand(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }

    Player player = (Player) sender;
    Chunk chunk = player.getLocation().getChunk();

        // Check if the chunk is already claimed by someone
        UUID existingOwner = chunkManager.getChunkOwner(chunk);
        if (existingOwner != null) {
            if (existingOwner.equals(player.getUniqueId())) {
                player.sendMessage(Component.text("You have already claimed this chunk.").color(NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("This chunk is already claimed by another player.").color(NamedTextColor.RED));
            }
            return true;
        }

        // Check WorldGuard compatibility separately
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(Component.text("You cannot claim this chunk because it overlaps with a WorldGuard region you don't own.").color(NamedTextColor.RED));
            return true;
        }

        // Actually claim the chunk
        if (chunkManager.claimChunk(player, chunk)) {
            chunkManager.saveData();
            player.sendMessage(Component.text("You have successfully claimed this chunk!").color(NamedTextColor.GREEN));
        } else {
            // This should rarely happen due to the checks above, but just in case
            player.sendMessage(Component.text("Failed to claim this chunk.").color(NamedTextColor.RED));
        }

        return true;
    }
}