package org.ashin.chunkClaimPlugin2;

import org.ashin.chunkClaimPlugin2.commands.*;
import org.ashin.chunkClaimPlugin2.listeners.ChunkProtectionListener;
import org.ashin.chunkClaimPlugin2.listeners.PlayerJoinLocaleListener;
import org.ashin.chunkClaimPlugin2.listeners.SettingsGUIListener;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChunkClaimPlugin2 extends JavaPlugin {

    private ChunkManager chunkManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        // Initialize config
        saveDefaultConfig();

        // Initialize chunk manager
        chunkManager = new ChunkManager(this);
    messageManager = new MessageManager(this);

        // Register commands
        registerCommands();

        getLogger().info("ChunkClaimPlugin2 has been enabled!");

        // Register listeners
    getServer().getPluginManager().registerEvents(new ChunkProtectionListener(chunkManager, messageManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinLocaleListener(messageManager), this);
    getServer().getPluginManager().registerEvents(new SettingsGUIListener(chunkManager, messageManager), this);
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
        getCommand("claimchunk").setExecutor(new ClaimChunkCommand(chunkManager, messageManager));
        getCommand("unclaimchunk").setExecutor(new UnclaimChunkCommand(chunkManager, messageManager));
        getCommand("checkchunk").setExecutor(new CheckChunkCommand(chunkManager, messageManager));
        getCommand("infochunk").setExecutor(new InfoChunkCommand(chunkManager, messageManager));
        getCommand("visualizechunk").setExecutor(new VisualizeChunkCommand(this, chunkManager, messageManager));
        // Language command for per-player locale
        ChunkLangCommand langCmd = new ChunkLangCommand(messageManager);
        if (getCommand("chunklang") != null) {
            getCommand("chunklang").setExecutor(langCmd);
            getCommand("chunklang").setTabCompleter(langCmd);
        }
        if (getCommand("chunksettings") != null) {
            getCommand("chunksettings").setExecutor(new ChunkSettingsCommand(chunkManager, messageManager));
        }
    }
}