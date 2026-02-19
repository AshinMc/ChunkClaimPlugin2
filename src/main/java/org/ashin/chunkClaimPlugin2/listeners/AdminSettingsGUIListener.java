package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.gui.AdminSettingsGUI;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
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

public class AdminSettingsGUIListener implements Listener {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final AdminSettingsGUI gui;

    public AdminSettingsGUIListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.gui = new AdminSettingsGUI(plugin, chunkManager, messages);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        var top = event.getView().getTopInventory();
        var holder = top != null ? top.getHolder() : null;
        if (!(holder instanceof AdminSettingsGUI.AdminHolder)) return;
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
        if (!(holder instanceof AdminSettingsGUI.AdminHolder)) return;

        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        AdminSettingsGUI.AdminHolder ah = (AdminSettingsGUI.AdminHolder) holder;

        switch (ah.view) {
            case ADMIN_HOME: handleHome(player, clicked, name); break;
            case ADMIN_MAX_CLAIMS: handleMaxClaims(player, clicked, name); break;
            case ADMIN_LANGUAGE: handleLanguage(player, clicked, name); break;
            case ADMIN_PARTICLE: handleParticle(player, clicked, name); break;
        }
    }

    private void handleHome(Player player, ItemStack clicked, String name) {
        String nMax = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-max-claims"));
        String nLang = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-language"));
        String nPart = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-particle"));
        String nReload = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-reload"));

        if (name.equals(nMax)) {
            gui.openMaxClaims(player);
        } else if (name.equals(nLang)) {
            gui.openLanguage(player);
        } else if (name.equals(nPart)) {
            gui.openParticle(player);
        } else if (name.equals(nReload)) {
            plugin.reloadConfig();
            messages.reloadLocales();
            player.sendMessage(messages.getFor(player.getUniqueId(), "admin-config-reloaded"));
            gui.openHome(player);
        }
    }

    private void handleMaxClaims(Player player, ItemStack clicked, String name) {
        String back = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"));
        if (name.equals(back)) { gui.openHome(player); return; }

        int current = plugin.getConfig().getInt("max-claims-per-player", 10);
        int delta = 0;

        if (name.equals("-10")) delta = -10;
        else if (name.equals("-5")) delta = -5;
        else if (name.equals("-1")) delta = -1;
        else if (name.equals("+1")) delta = 1;
        else if (name.equals("+5")) delta = 5;
        else if (name.equals("+10")) delta = 10;
        else if (name.contains("Unlimited")) {
            plugin.getConfig().set("max-claims-per-player", 0);
            saveConfig();
            player.sendMessage(messages.getFor(player.getUniqueId(), "admin-max-claims-set", "max", "Unlimited"));
            gui.openMaxClaims(player);
            return;
        }

        if (delta != 0) {
            int newVal = Math.max(0, current + delta);
            plugin.getConfig().set("max-claims-per-player", newVal);
            saveConfig();
            String display = newVal == 0 ? "Unlimited" : String.valueOf(newVal);
            player.sendMessage(messages.getFor(player.getUniqueId(), "admin-max-claims-set", "max", display));
            gui.openMaxClaims(player);
        }
    }

    private void handleLanguage(Player player, ItemStack clicked, String name) {
        String back = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"));
        if (name.equals(back)) { gui.openHome(player); return; }

        // Name is the locale string
        if (messages.isLocaleAvailable(name)) {
            plugin.getConfig().set("locale", name);
            saveConfig();
            player.sendMessage(messages.getFor(player.getUniqueId(), "admin-language-set", "locale", name));
            gui.openLanguage(player);
        }
    }

    private void handleParticle(Player player, ItemStack clicked, String name) {
        String back = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"));
        if (name.equals(back)) { gui.openHome(player); return; }

        // name is the particle type name
        try {
            org.bukkit.Particle.valueOf(name);
            plugin.getConfig().set("visualization.particle-type", name);
            saveConfig();
            player.sendMessage(messages.getFor(player.getUniqueId(), "admin-particle-set", "particle", name));
            gui.openParticle(player);
        } catch (IllegalArgumentException ignored) {
            // Not a valid particle name, ignore
        }
    }

    private void saveConfig() {
        plugin.saveConfig();
    }
}
