package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID; // Added missing import

public class InfoChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;

    public InfoChunkCommand(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        List<ChunkData> chunks = chunkManager.getPlayerChunks(playerId);

        if (chunks.isEmpty()) {
            player.sendMessage(Component.text("You don't have any claimed chunks.").color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Your claimed chunks:").color(NamedTextColor.GREEN));
        int count = 1;
        for (ChunkData chunk : chunks) {
            // Calculate block coordinates from chunk coordinates
            int blockX = chunk.getX() * 16;
            int blockZ = chunk.getZ() * 16;

        player.sendMessage(
            Component.text()
                .append(Component.text(count + ". ").color(NamedTextColor.GOLD))
                .append(Component.text("World: ").color(NamedTextColor.WHITE))
                .append(Component.text(chunk.getWorld()).color(NamedTextColor.WHITE))
                .append(Component.text(" Block coordinates: X: ").color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(blockX)).color(NamedTextColor.WHITE))
                .append(Component.text(", Z: ").color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(blockZ)).color(NamedTextColor.WHITE))
                .build()
        );
            count++;
        }

        return true;
    }
}