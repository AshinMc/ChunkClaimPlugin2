package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;

    public UnclaimChunkCommand(ChunkManager chunkManager) {
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

        if (chunkManager.unclaimChunk(player, chunk)) {
            player.sendMessage(Component.text("You have successfully unclaimed this chunk!").color(NamedTextColor.GREEN));
            chunkManager.saveData();
        } else {
            player.sendMessage(Component.text("You don't own this chunk or it's not claimed!").color(NamedTextColor.RED));
        }

        return true;
    }
}