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
        String claimName = String.join(" ", args);

        // Get all chunks for this claim name
        List<ChunkData> chunks = chunkManager.getChunksByName(playerId, claimName);
        if (chunks.isEmpty()) {
            player.sendMessage(messages.getFor(playerId, "tp-not-found", "name", claimName));
            return true;
        }

        // Teleport to a safe position near the center of the first chunk in the group
        ChunkData firstChunk = chunks.get(0);
        World world = Bukkit.getWorld(firstChunk.getWorld());
        if (world == null) {
            player.sendMessage(messages.getFor(playerId, "tp-world-not-found", "world", firstChunk.getWorld()));
            return true;
        }

        int centerX = (firstChunk.getX() * 16) + 8;
        int centerZ = (firstChunk.getZ() * 16) + 8;

        Location destination = findSafeLocationInChunk(world, firstChunk.getX() * 16, firstChunk.getZ() * 16, centerX, centerZ);
        if (destination == null) {
            player.sendMessage(org.bukkit.ChatColor.RED + "No safe teleport location found inside claim '" + claimName + "'.");
            return true;
        }

        destination.setPitch(player.getLocation().getPitch());
        destination.setYaw(player.getLocation().getYaw());

        player.teleport(destination);
        player.sendMessage(messages.getFor(playerId, "tp-success", "name", claimName));

        return true;
    }

    private Location findSafeLocationInChunk(World world, int minX, int minZ, int centerX, int centerZ) {
        Location centerSafe = findSafeYAtColumn(world, centerX, centerZ);
        if (centerSafe != null) return centerSafe;

        // Fallback: probe grid points within the same chunk if center column is obstructed/lava
        for (int dx = 0; dx <= 15; dx += 2) {
            for (int dz = 0; dz <= 15; dz += 2) {
                Location candidate = findSafeYAtColumn(world, minX + dx, minZ + dz);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private Location findSafeYAtColumn(World world, int x, int z) {
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
        int minY = world.getMinHeight() + 1;
        int topY = isNether
                ? Math.min(122, world.getMaxHeight() - 3)
                : Math.min(world.getHighestBlockYAt(x, z), world.getMaxHeight() - 3);

        // First try surface (non-Nether) if safe
        if (!isNether && isSafeSpot(world, x, topY + 1, z)) {
            return new Location(world, x + 0.5, topY + 1, z + 0.5);
        }

        // Scan vertically downward from safe ceiling/surface to find 2 air/passable blocks above safe solid floor
        for (int y = topY; y >= minY; y--) {
            if (isSafeSpot(world, x, y, z)) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    private boolean isSafeSpot(World world, int x, int feetY, int z) {
        if (feetY - 1 < world.getMinHeight() || feetY + 1 >= world.getMaxHeight()) return false;
        org.bukkit.block.Block floor = world.getBlockAt(x, feetY - 1, z);
        org.bukkit.block.Block feet = world.getBlockAt(x, feetY, z);
        org.bukkit.block.Block head = world.getBlockAt(x, feetY + 1, z);
        return isSafeFloor(floor) && isPassableSafe(feet) && isPassableSafe(head);
    }

    private boolean isSafeFloor(org.bukkit.block.Block block) {
        org.bukkit.Material type = block.getType();
        if (!type.isSolid()) return false;
        String name = type.name();
        return !name.contains("LAVA")
                && !name.contains("FIRE")
                && !name.equals("MAGMA_BLOCK")
                && !name.equals("CACTUS")
                && !name.contains("CAMPFIRE")
                && !name.equals("SWEET_BERRY_BUSH")
                && !name.equals("POWDER_SNOW")
                && !name.equals("POINTED_DRIPSTONE");
    }

    private boolean isPassableSafe(org.bukkit.block.Block block) {
        if (block.isLiquid()) return false;
        org.bukkit.Material type = block.getType();
        if (type.isSolid()) return false;
        String name = type.name();
        return !name.contains("FIRE") && !name.equals("POWDER_SNOW") && !name.equals("SWEET_BERRY_BUSH");
    }
}
