package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.gui.SettingsGUI;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SettingsGUIListener implements Listener {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final SettingsGUI gui;

    public SettingsGUIListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.gui = new SettingsGUI(plugin, chunkManager, messages);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        var top = event.getView().getTopInventory();
        var holder = top != null ? top.getHolder() : null;
        if (!(holder instanceof SettingsGUI.SettingsHolder)) return;
        if (top == null) return;
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());
        if (affectsTop) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        var top = event.getView().getTopInventory();
        var holder = top != null ? top.getHolder() : null;
        if (!(holder instanceof SettingsGUI.SettingsHolder)) return;

        // Only handle clicks in the top inventory (our GUI), not the player's inventory
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            event.setCancelled(true); // prevent moving items between invs while GUI is open
            return;
        }
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        SettingsGUI.SettingsHolder sh = (SettingsGUI.SettingsHolder) holder;
        switch (sh.view) {
            case HOME: handleHome(player, clicked, name); break;
            case CLAIMS: handleClaims(player, clicked, name); break;
            case VISUALIZE: handleVisualize(player, clicked, name); break;
            case DELETE: handleDelete(player, clicked, name); break;
            case LANGUAGE: handleLanguage(player, clicked, name); break;
            case PARTICLE: handleParticle(player, clicked, name); break;
            case TRUST: handleTrust(player, clicked, name, sh); break;
            default: break;
        }
    }

    private void handleHome(Player player, ItemStack clicked, String name) {
        String nClaims = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-claims"));
        String nVis = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-visualize"));
        String nDel = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-delete"));
        String nLang = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-language"));
        String nPart = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-particle"));
        if (name.equals(nClaims)) gui.openClaims(player);
        else if (name.equals(nVis)) gui.openVisualize(player);
        else if (name.equals(nDel)) gui.openDelete(player);
        else if (name.equals(nLang)) gui.openLanguage(player);
        else if (name.equals(nPart)) gui.openParticle(player);
    }

    private void handleClaims(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        // name is the claim group name
        if (chunkManager.hasClaimName(player.getUniqueId(), name)) {
            gui.openClaimDetails(player, name);
        }
    }

    private void handleVisualize(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        // Visualize by claim name
        player.performCommand("visualizechunk " + name);
    }

    private void handleDelete(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        // name is the claim group name
        if (chunkManager.hasClaimName(player.getUniqueId(), name)) {
            gui.openDeleteConfirm(player, name);
        }
    }

    private void handleLanguage(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        // name is the locale like en_US
        String loc = name;
        if (messages.isLocaleAvailable(loc)) {
            messages.setPlayerLocale(player.getUniqueId(), loc);
            player.sendMessage(messages.getFor(player.getUniqueId(), "lang-set", "locale", loc));
            gui.openLanguage(player);
        }
    }

    private void handleParticle(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        // Reset to server default
        String resetLabel = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-particle-reset"));
        if (name.equals(resetLabel)) {
            messages.setPlayerParticle(player.getUniqueId(), null);
            player.sendMessage(messages.getFor(player.getUniqueId(), "particle-reset"));
            gui.openParticle(player);
            return;
        }
        // Validate as particle name
        try {
            org.bukkit.Particle.valueOf(name);
            messages.setPlayerParticle(player.getUniqueId(), name);
            player.sendMessage(messages.getFor(player.getUniqueId(), "particle-set", "particle", name));
            gui.openParticle(player);
        } catch (IllegalArgumentException ignored) {
            // not a valid particle, ignore click
        }
    }

    private void handleTrust(Player player, ItemStack clicked, String name, SettingsGUI.SettingsHolder sh) {
        if (clicked.getType() == Material.ARROW) {
            // Back to claim details
            if (sh.claimName != null) gui.openClaimDetails(player, sh.claimName);
            else gui.openHome(player);
            return;
        }
        if (clicked.getType() != Material.PLAYER_HEAD || sh.claimName == null) return;
        // Toggle trust for this player
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) return;
        boolean currentlyTrusted = chunkManager.isTrusted(player.getUniqueId(), sh.claimName, target.getUniqueId());
        chunkManager.setTrusted(player.getUniqueId(), sh.claimName, target.getUniqueId(), !currentlyTrusted);
        chunkManager.saveData();
        if (!currentlyTrusted) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "trust-added", "player", target.getName(), "name", sh.claimName));
        } else {
            player.sendMessage(messages.getFor(player.getUniqueId(), "trust-removed", "player", target.getName(), "name", sh.claimName));
        }
        // Refresh the trust GUI
        gui.openTrust(player, sh.claimName);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDetailsClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        var top = event.getView().getTopInventory();
        var holder = top != null ? top.getHolder() : null;
        if (!(holder instanceof SettingsGUI.SettingsHolder)) return;
        SettingsGUI.SettingsHolder sh = (SettingsGUI.SettingsHolder) holder;
        boolean isDetails = sh.view == SettingsGUI.View.CLAIM_DETAILS;
        boolean isDelConf = sh.view == SettingsGUI.View.DELETE_CONFIRM;
        if (!isDetails && !isDelConf) return;

        // Always cancel any click when in details/confirm menus (even empty slots)
        event.setCancelled(true);

        // If it's not our top inventory, no further action
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (isDetails) {
            // Back
            if (clicked.getType() == Material.ARROW) { gui.openClaims(player); return; }
            // Teleport
            String telep = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-teleport"));
            String trustLabel = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-trust"));
            if (name.equals(trustLabel) && clicked.getType() == Material.PLAYER_HEAD) {
                // Open trust GUI for this claim
                ItemStack info = top.getItem(11);
                if (info != null && info.hasItemMeta() && info.getItemMeta().hasDisplayName()) {
                    String claimName = ChatColor.stripColor(info.getItemMeta().getDisplayName());
                    gui.openTrust(player, claimName);
                }
                return;
            }
            if (name.equals(telep)) {
                // Require operator (or explicit permission) to use teleport
                if (!(player.isOp() || player.hasPermission("chunkclaim.teleport"))) {
                    player.sendMessage(messages.getFor(player.getUniqueId(), "teleport-no-permission"));
                    return;
                }
                if (top == null) return; // safety
                // Get the info item in slot 11 to extract claim name, then teleport to first chunk
                ItemStack info = top.getItem(11);
                if (info != null && info.hasItemMeta() && info.getItemMeta().hasDisplayName()) {
                    String claimName = ChatColor.stripColor(info.getItemMeta().getDisplayName());
                    java.util.List<org.ashin.chunkClaimPlugin2.data.ChunkData> chunks =
                            chunkManager.getChunksByName(player.getUniqueId(), claimName);
                    if (!chunks.isEmpty()) {
                        org.ashin.chunkClaimPlugin2.data.ChunkData cd = chunks.get(0);
                        var world = Bukkit.getWorld(cd.getWorld());
                        if (world != null) {
                            int bx = (cd.getX() << 4) + 8;
                            int bz = (cd.getZ() << 4) + 8;
                            int y = findSafeY(world, bx, bz);
                            player.teleport(new org.bukkit.Location(world, bx + 0.5, y + 0.1, bz + 0.5));
                            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.1f);} catch (Throwable ignored) {}
                        }
                    }
                }
            }
            return;
        }

        if (isDelConf) {
            String confirm = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-confirm"));
            String cancel = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-cancel"));
            if (name.equals(cancel)) { gui.openDelete(player); return; }

            if (name.equals(confirm)) {
                if (top == null) return; // safety
                // Parse claim name from center info item (slot 13)
                ItemStack info = top.getItem(13);
                if (info != null && info.hasItemMeta() && info.getItemMeta().hasDisplayName()) {
                    String claimName = ChatColor.stripColor(info.getItemMeta().getDisplayName());
                    int count = chunkManager.unclaimByName(player.getUniqueId(), claimName);
                    if (count > 0) {
                        chunkManager.saveData();
                        player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-name-success",
                                "name", claimName, "count", String.valueOf(count)));
                    } else {
                        player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-fail"));
                    }
                    gui.openDelete(player);
                }
            }
        }
    }

    private int findSafeY(org.bukkit.World world, int x, int z) {
        // Try to find highest non-air block and stand one block above it.
        int top = Math.min(world.getMaxHeight() - 1, world.getHighestBlockYAt(x, z));
        // Ensure we don't end inside leaves/liquids: step down until solid ground-ish
        for (int y = top; y >= world.getMinHeight(); y--) {
            var type = world.getBlockAt(x, y, z).getType();
            if (type.isSolid()) {
                return y + 1;
            }
        }
        // Fallback to world spawn height if nothing found
        return world.getHighestBlockYAt(x, z) + 1;
    }
}
