package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
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

public class SettingsGUIListener implements Listener {
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final SettingsGUI gui;

    public SettingsGUIListener(ChunkManager chunkManager, MessageManager messages) {
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.gui = new SettingsGUI(chunkManager, messages);
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
            default: break;
        }
    }

    private void handleHome(Player player, ItemStack clicked, String name) {
        String nClaims = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-claims"));
        String nVis = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-visualize"));
        String nDel = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-delete"));
        String nLang = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-language"));
        if (name.equals(nClaims)) gui.openClaims(player);
        else if (name.equals(nVis)) gui.openVisualize(player);
        else if (name.equals(nDel)) gui.openDelete(player);
        else if (name.equals(nLang)) gui.openLanguage(player);
    }

    private ChunkData parseFromName(String name) {
        // Format: <world> @ <x>, <z>
        try {
            String[] parts = name.split(" @ ");
            String world = parts[0];
            String[] coords = parts[1].split(", ");
            int x = Integer.parseInt(coords[0]);
            int z = Integer.parseInt(coords[1]);
            return new ChunkData(world, x, z);
        } catch (Exception e) {
            return null;
        }
    }

    private void handleClaims(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        ChunkData cd = parseFromName(name);
        if (cd == null) return;
        gui.openClaimDetails(player, cd);
    }

    private void handleVisualize(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        ChunkData cd = parseFromName(name);
        if (cd == null) return;
        var world = Bukkit.getWorld(cd.getWorld());
        if (world == null) return;
        // Call single-claim visualization
        player.performCommand("visualizechunk " + cd.getWorld() + " " + cd.getX() + " " + cd.getZ());
    }

    private void handleDelete(Player player, ItemStack clicked, String name) {
        if (clicked.getType() == Material.ARROW) { gui.openHome(player); return; }
        ChunkData cd = parseFromName(name);
        if (cd == null) return;
        gui.openDeleteConfirm(player, cd);
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
            if (name.equals(telep)) {
                if (top == null) return; // safety
                // Get the info item in slot 11 to parse coords back
                ItemStack info = top.getItem(11);
                if (info != null && info.hasItemMeta() && info.getItemMeta().hasDisplayName()) {
                    ChunkData cd = parseFromName(ChatColor.stripColor(info.getItemMeta().getDisplayName()));
                    if (cd != null) {
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
                // Parse coords from center info item (slot 13)
                ItemStack info = top.getItem(13);
                if (info != null && info.hasItemMeta() && info.getItemMeta().hasDisplayName()) {
                    ChunkData cd = parseFromName(ChatColor.stripColor(info.getItemMeta().getDisplayName()));
                    if (cd != null) {
                        boolean ok = chunkManager.unclaimChunk(player.getUniqueId(), cd.getWorld(), cd.getX(), cd.getZ());
                        if (ok) {
                            chunkManager.saveData();
                            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-success"));
                        } else {
                            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-fail"));
                        }
                        gui.openDelete(player);
                    }
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
