package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.gui.AdminSettingsGUI;
import org.ashin.chunkClaimPlugin2.gui.SettingsGUI;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Handles /chunksettings command.
 * If executed without arguments, opens the Settings GUI.
 * Also provides full command-line subcommands for custom menus (DeluxeMenus, CommandPanels, etc.):
 *  - /chunksettings flag <claim> <flag> [true|false|toggle]
 *  - /chunksettings trust <claim> <player>
 *  - /chunksettings untrust <claim> <player>
 *  - /chunksettings particle <type|reset>
 *  - /chunksettings rename <oldName> <newName>
 *  - /chunksettings transfer <claim> <player>
 *  - /chunksettings tp <claim>
 *  - /chunksettings visualize [claim]
 *  - /chunksettings unclaim <claim>
 *  - /chunksettings lang <locale>
 */
public class ChunkSettingsCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final EconomyManager economyManager;
    private final SettingsGUI gui;

    public ChunkSettingsCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.economyManager = economyManager;
        this.gui = new SettingsGUI(plugin, chunkManager, messages, economyManager);
    }

    public ChunkSettingsCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this(plugin, chunkManager, messages, null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        if (args.length == 0) {
            gui.openHome(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "flag" -> handleFlag(player, label, args);
            case "trust" -> handleTrust(player, label, args, true);
            case "untrust" -> handleTrust(player, label, args, false);
            case "particle" -> handleParticle(player, label, args);
            case "rename" -> handleRename(player, label, args);
            case "transfer" -> handleTransfer(player, label, args);
            case "tp", "teleport" -> handleTeleport(player, label, args);
            case "visualize", "vis" -> handleVisualize(player, label, args);
            case "unclaim", "delete" -> handleUnclaim(player, label, args);
            case "lang", "language" -> handleLanguage(player, label, args);
            case "gui", "menu" -> {
                gui.openHome(player);
            }
            case "help" -> sendHelp(player, label);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand '" + args[0] + "'. Type /" + label + " help for subcommands or /" + label + " to open the menu.");
            }
        }

        return true;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "=== Chunk Settings Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open the interactive settings GUI");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " flag <claim> <flag> [true|false|toggle] " + ChatColor.GRAY + "- Toggle or set claim flag");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " trust <claim> <player> " + ChatColor.GRAY + "- Trust a player in your claim");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " untrust <claim> <player> " + ChatColor.GRAY + "- Remove trust from a player");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " particle <type|reset> " + ChatColor.GRAY + "- Set your claim border particle");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " rename <oldName> <newName> " + ChatColor.GRAY + "- Rename your claim group");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " transfer <claim> <player> " + ChatColor.GRAY + "- Transfer claim ownership");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " tp <claim> " + ChatColor.GRAY + "- Teleport to one of your claims");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " visualize [claim] " + ChatColor.GRAY + "- Visualize your claim borders");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " unclaim <claim> " + ChatColor.GRAY + "- Unclaim an entire claim group");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " lang <locale> " + ChatColor.GRAY + "- Switch your language/locale");
    }

    private void handleFlag(Player player, String label, String[] args) {
        String claimName;
        String flagKey;
        String stateArg = "toggle";

        if (args.length >= 3) {
            claimName = args[1];
            flagKey = args[2].toLowerCase();
            if (args.length >= 4) {
                stateArg = args[3].toLowerCase();
            }
        } else if (args.length == 2) {
            // Check if standing in own claim and args[1] is a flag key
            Chunk chunk = player.getLocation().getChunk();
            UUID owner = chunkManager.getChunkOwner(chunk);
            if (owner != null && owner.equals(player.getUniqueId()) && ChunkManager.DEFAULT_FLAGS.containsKey(args[1].toLowerCase())) {
                claimName = chunkManager.getChunkClaimName(chunk);
                flagKey = args[1].toLowerCase();
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /" + label + " flag <claimName> <flagKey> [true|false|toggle]");
                return;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " flag <claimName> <flagKey> [true|false|toggle]");
            return;
        }

        if (!chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", claimName));
            return;
        }

        if (!ChunkManager.DEFAULT_FLAGS.containsKey(flagKey)) {
            player.sendMessage(ChatColor.RED + "Invalid flag '" + flagKey + "'. Valid flags: " +
                    String.join(", ", ChunkManager.DEFAULT_FLAGS.keySet()));
            return;
        }

        // Check flag permission
        if (!player.hasPermission("ccp.flag.*") && !player.hasPermission("ccp.flag." + flagKey)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "flag-no-permission"));
            return;
        }

        boolean current = chunkManager.getClaimFlag(player.getUniqueId(), claimName, flagKey);
        boolean newState;
        if (stateArg.equals("true") || stateArg.equals("enable") || stateArg.equals("on") || stateArg.equals("yes") || stateArg.equals("1")) {
            newState = true;
        } else if (stateArg.equals("false") || stateArg.equals("disable") || stateArg.equals("off") || stateArg.equals("no") || stateArg.equals("0")) {
            newState = false;
        } else {
            newState = !current;
        }

        chunkManager.setClaimFlag(player.getUniqueId(), claimName, flagKey, newState);
        chunkManager.saveData();

        String flagLabel = ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-flag-" + flagKey));
        if (flagLabel == null || flagLabel.equals("gui-flag-" + flagKey)) flagLabel = flagKey;
        String stateStr = newState
                ? ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-flag-enabled"))
                : ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-flag-disabled"));

        player.sendMessage(messages.getFor(player.getUniqueId(), "flag-toggled",
                "flag", flagLabel, "state", stateStr, "name", claimName));
    }

    private void handleTrust(Player player, String label, String[] args, boolean isTrust) {
        String claimName;
        String targetPlayerName;

        if (args.length >= 3) {
            claimName = args[1];
            targetPlayerName = args[2];
        } else if (args.length == 2) {
            // Check if standing in own claim
            Chunk chunk = player.getLocation().getChunk();
            UUID owner = chunkManager.getChunkOwner(chunk);
            if (owner != null && owner.equals(player.getUniqueId())) {
                claimName = chunkManager.getChunkClaimName(chunk);
                targetPlayerName = args[1];
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /" + label + " " + (isTrust ? "trust" : "untrust") + " <claimName> <playerName>");
                return;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " " + (isTrust ? "trust" : "untrust") + " <claimName> <playerName>");
            return;
        }

        if (!chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", claimName));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetPlayerName);
        UUID targetUUID;
        String targetName;
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetPlayerName);
            if (off.hasPlayedBefore() || off.isOnline()) {
                targetUUID = off.getUniqueId();
                targetName = off.getName() != null ? off.getName() : targetPlayerName;
            } else {
                player.sendMessage(ChatColor.RED + "Player '" + targetPlayerName + "' was not found.");
                return;
            }
        }

        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot trust or untrust yourself.");
            return;
        }

        chunkManager.setTrusted(player.getUniqueId(), claimName, targetUUID, isTrust);
        chunkManager.saveData();

        if (isTrust) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "trust-added", "player", targetName, "name", claimName));
        } else {
            player.sendMessage(messages.getFor(player.getUniqueId(), "trust-removed", "player", targetName, "name", claimName));
        }
    }

    private void handleParticle(Player player, String label, String[] args) {
        if (args.length < 2) {
            String current = messages.getPlayerParticle(player.getUniqueId());
            if (current == null || current.isEmpty()) {
                current = plugin.getConfig().getString("visualization.particle-type", "FLAME");
            }
            player.sendMessage(ChatColor.YELLOW + "Current border particle: " + ChatColor.AQUA + current +
                    ChatColor.YELLOW + ". Usage: /" + label + " particle <type|reset>");
            return;
        }

        String partArg = args[1].toUpperCase();
        if (partArg.equalsIgnoreCase("RESET") || partArg.equalsIgnoreCase("DEFAULT")) {
            messages.setPlayerParticle(player.getUniqueId(), null);
            player.sendMessage(messages.getFor(player.getUniqueId(), "particle-reset"));
            return;
        }

        boolean valid = false;
        for (String[] entry : AdminSettingsGUI.getAvailableParticles()) {
            if (entry[0].equalsIgnoreCase(partArg)) {
                valid = true;
                partArg = entry[0];
                break;
            }
        }

        if (!valid) {
            try {
                org.bukkit.Particle.valueOf(partArg);
                valid = true;
            } catch (IllegalArgumentException ignored) {}
        }

        if (!valid) {
            List<String> validNames = Arrays.stream(AdminSettingsGUI.getAvailableParticles()).map(e -> e[0]).toList();
            player.sendMessage(ChatColor.RED + "Invalid particle name. Available: " + String.join(", ", validNames));
            return;
        }

        messages.setPlayerParticle(player.getUniqueId(), partArg);
        player.sendMessage(messages.getFor(player.getUniqueId(), "particle-set", "particle", partArg));
    }

    private void handleRename(Player player, String label, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " rename <oldName> <newName>");
            return;
        }

        String oldName = args[1];
        String newName = args[2];

        if (!chunkManager.hasClaimName(player.getUniqueId(), oldName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", oldName));
            return;
        }

        if (chunkManager.hasClaimName(player.getUniqueId(), newName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-name-exists", "name", newName));
            return;
        }

        if (chunkManager.renameClaim(player.getUniqueId(), oldName, newName)) {
            chunkManager.saveData();
            player.sendMessage(messages.getFor(player.getUniqueId(), "rename-success", "name", newName));
        } else {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-name-exists", "name", newName));
        }
    }

    private void handleTransfer(Player player, String label, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " transfer <claimName> <playerName>");
            return;
        }

        String claimName = args[1];
        String targetName = args[2];

        if (!chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", claimName));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetName + "' must be online to transfer ownership.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot transfer a claim to yourself.");
            return;
        }

        int groupChunks = chunkManager.getChunksByName(player.getUniqueId(), claimName).size();
        int defaultMax = plugin.getConfig().getInt("max-claims-per-player", 10);
        int maxClaims = chunkManager.getPlayerLimit(target.getUniqueId(), defaultMax);
        if (maxClaims > 0) {
            int currentCount = chunkManager.getPlayerChunkCount(target.getUniqueId());
            if (currentCount + groupChunks > maxClaims) {
                player.sendMessage(ChatColor.RED + target.getName() + " cannot accept this claim because they would exceed their chunk limit (" + maxClaims + ").");
                return;
            }
        }

        chunkManager.transferClaim(player.getUniqueId(), target.getUniqueId(), claimName);
        chunkManager.setClaimPrice(player.getUniqueId(), claimName, null);
        chunkManager.saveData();

        player.sendMessage(messages.getFor(player.getUniqueId(), "transfer-success", "name", claimName, "player", target.getName()));
        target.sendMessage(ChatColor.GREEN + "You have received ownership of claim group '" + claimName + "' (" + groupChunks + " chunks) from " + player.getName() + "!");
    }

    private void handleTeleport(Player player, String label, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " tp <claimName>");
            return;
        }
        player.performCommand("chunktp " + args[1]);
    }

    private void handleVisualize(Player player, String label, String[] args) {
        if (args.length >= 2) {
            player.performCommand("visualizechunk " + args[1]);
        } else {
            player.performCommand("visualizechunk");
        }
    }

    private void handleUnclaim(Player player, String label, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " unclaim <claimName>");
            return;
        }
        player.performCommand("unclaimchunk " + args[1]);
    }

    private void handleLanguage(Player player, String label, String[] args) {
        if (args.length < 2) {
            player.performCommand("chunklang list");
        } else {
            player.performCommand("chunklang set " + args[1]);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = List.of("flag", "trust", "untrust", "particle", "rename", "transfer", "tp", "visualize", "unclaim", "lang", "menu", "help");
            List<String> res = new ArrayList<>();
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) res.add(s);
            }
            return res;
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("flag") || sub.equals("trust") || sub.equals("untrust") || sub.equals("rename")
                    || sub.equals("transfer") || sub.equals("tp") || sub.equals("teleport")
                    || sub.equals("visualize") || sub.equals("unclaim") || sub.equals("delete")) {
                List<String> claims = chunkManager.getPlayerClaimNames(player.getUniqueId());
                List<String> res = new ArrayList<>();
                for (String c : claims) {
                    if (c.toLowerCase().startsWith(args[1].toLowerCase())) res.add(c);
                }
                return res;
            }
            if (sub.equals("particle")) {
                List<String> particles = new ArrayList<>();
                for (String[] p : AdminSettingsGUI.getAvailableParticles()) particles.add(p[0]);
                particles.add("reset");
                List<String> res = new ArrayList<>();
                for (String p : particles) {
                    if (p.toLowerCase().startsWith(args[1].toLowerCase())) res.add(p);
                }
                return res;
            }
            if (sub.equals("lang") || sub.equals("language")) {
                List<String> locales = messages.getAvailableLocales();
                List<String> res = new ArrayList<>();
                for (String l : locales) {
                    if (l.toLowerCase().startsWith(args[1].toLowerCase())) res.add(l);
                }
                return res;
            }
        }

        if (args.length == 3) {
            if (sub.equals("flag")) {
                List<String> flags = new ArrayList<>(ChunkManager.DEFAULT_FLAGS.keySet());
                List<String> res = new ArrayList<>();
                for (String f : flags) {
                    if (f.toLowerCase().startsWith(args[2].toLowerCase())) res.add(f);
                }
                return res;
            }
            if (sub.equals("trust") || sub.equals("untrust") || sub.equals("transfer")) {
                List<String> res = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(player.getUniqueId()) && online.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        res.add(online.getName());
                    }
                }
                return res;
            }
        }

        if (args.length == 4 && sub.equals("flag")) {
            return List.of("true", "false", "toggle");
        }

        return Collections.emptyList();
    }
}
