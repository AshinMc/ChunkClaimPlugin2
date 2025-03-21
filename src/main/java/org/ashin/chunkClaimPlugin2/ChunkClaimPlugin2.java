package org.ashin.chunkClaimPlugin2;

import org.ashin.chunkClaimPlugin2.commands.*;
import org.ashin.chunkClaimPlugin2.listeners.ChunkProtectionListener;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChunkClaimPlugin2 extends JavaPlugin {

    private ChunkManager chunkManager;

    @Override
    public void onEnable() {
        // Initialize config
        saveDefaultConfig();

        // Initialize chunk manager
        chunkManager = new ChunkManager(this);

        // Register commands
        registerCommands();

        getLogger().info("ChunkClaimPlugin2 has been enabled!");

        // In ChunkClaimPlugin2.java, add to the onEnable method:
        getServer().getPluginManager().registerEvents(new ChunkProtectionListener(chunkManager), this);
    }

    @Override
    public void onDisable() {
        // Save claimed chunks data
        if (chunkManager != null) {
            chunkManager.saveData();
        }

        getLogger().info("ChunkClaimPlugin2 has been disabled!");
    }

    private void registerCommands() {
        getCommand("claimchunk").setExecutor(new ClaimChunkCommand(this, chunkManager));
        getCommand("unclaimchunk").setExecutor(new UnclaimChunkCommand(this, chunkManager));
        getCommand("checkchunk").setExecutor(new CheckChunkCommand(this, chunkManager));
        getCommand("infochunk").setExecutor(new InfoChunkCommand(this, chunkManager));
        getCommand("visualizechunk").setExecutor(new VisualizeChunkCommand(this, chunkManager));
    }
}