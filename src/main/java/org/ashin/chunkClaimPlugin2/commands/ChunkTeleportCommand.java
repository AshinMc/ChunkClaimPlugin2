package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ChunkTeleportCommand implements CommandExecutor {
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public ChunkTeleportCommand(ChunkManager chunkManager, MessageManager messages) {
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(messages.get("tp-usage"));
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        String claimName = args[0];

        // Get all chunks for this claim name
        List<ChunkData> chunks = chunkManager.getChunksByName(playerId, claimName);
        if (chunks.isEmpty()) {
            player.sendMessage(messages.getFor(playerId, "tp-not-found", "name", claimName));
            return true;
        }

        // Teleport to the center of the first chunk in the group
        ChunkData firstChunk = chunks.get(0);
        World world = Bukkit.getWorld(firstChunk.getWorld());
        if (world == null) {
            player.sendMessage(messages.getFor(playerId, "tp-world-not-found", "world", firstChunk.getWorld()));
            return true;
        }

        // Calculate center of chunk (chunk coords * 16 + 8 for center)
        int centerX = (firstChunk.getX() * 16) + 8;
        int centerZ = (firstChunk.getZ() * 16) + 8;
        
        // Get highest block at center location for Y coordinate
        int centerY = world.getHighestBlockYAt(centerX, centerZ) + 1;

        Location destination = new Location(world, centerX + 0.5, centerY, centerZ + 0.5);
        destination.setPitch(player.getLocation().getPitch());
        destination.setYaw(player.getLocation().getYaw());

        player.teleport(destination);
        player.sendMessage(messages.getFor(playerId, "tp-success", "name", claimName));

        return true;
    }
}
