package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.gui.AdminSettingsGUI;
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
import java.util.Collections;
import java.util.List;

public class ChunkAdminCommand implements CommandExecutor, TabCompleter {
    private final AdminSettingsGUI gui;
    private final MessageManager messages;
    private final ChunkManager chunkManager;

    public ChunkAdminCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.gui = new AdminSettingsGUI(plugin, chunkManager, messages);
        this.messages = messages;
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chunkclaim.admin")) {
            if (sender instanceof Player) {
                sender.sendMessage(messages.getFor(((Player) sender).getUniqueId(), "admin-no-permission"));
            } else {
                sender.sendMessage("No permission.");
            }
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();

            // ── /chunkadmin unclaim (current chunk) ──
            if (sub.equals("unclaim") || sub.equals("removechunk")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "You must be a player in-game to unclaim your current chunk. Use /chunkadmin unclaimplayer <player> from console.");
                    return true;
                }

                Chunk chunk = player.getLocation().getChunk();
                if (!chunkManager.isChunkClaimed(chunk)) {
                    player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unowned"));
                    return true;
                }

                ChunkManager.AdminUnclaimResult res = chunkManager.adminUnclaimChunk(chunk);
                if (res != null && res.success) {
                    chunkManager.saveData();
                    OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(res.owner);
                    String ownerName = ownerPlayer.getName() != null ? ownerPlayer.getName() : res.owner.toString();

                    player.sendMessage(messages.getFor(player.getUniqueId(), "admin-unclaim-chunk-success",
                            "x", String.valueOf(chunk.getX() * 16),
                            "z", String.valueOf(chunk.getZ() * 16),
                            "player", ownerName,
                            "claim", res.claimName));

                    if (ownerPlayer.isOnline() && ownerPlayer.getPlayer() != null && !ownerPlayer.getUniqueId().equals(player.getUniqueId())) {
                        ownerPlayer.getPlayer().sendMessage(messages.getFor(ownerPlayer.getUniqueId(), "admin-unclaim-chunk-notify",
                                "x", String.valueOf(chunk.getX() * 16),
                                "z", String.valueOf(chunk.getZ() * 16),
                                "claim", res.claimName));
                    }
                } else {
                    player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-fail"));
                }
                return true;
            }

            // ── /chunkadmin unclaimplayer <player> [claimName|--all] ──
            if (sub.equals("unclaimplayer") || sub.equals("removeclaim")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chunkadmin unclaimplayer <player> [claimName|--all]");
                    return true;
                }

                String targetName = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                    return true;
                }

                if (args.length >= 3 && !args[2].equalsIgnoreCase("--all")) {
                    // Unclaim a specific named group
                    String claimName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    if (!chunkManager.hasClaimName(target.getUniqueId(), claimName)) {
                        sender.sendMessage(ChatColor.RED + targetName + " does not have a claim named '" + claimName + "'.");
                        return true;
                    }

                    int count = chunkManager.adminUnclaimGroup(target.getUniqueId(), claimName);
                    chunkManager.saveData();
                    sender.sendMessage(ChatColor.GREEN + "Removed claim '" + claimName + "' (" + count + " chunks) owned by " + targetName + ".");

                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(messages.getFor(target.getUniqueId(), "admin-unclaim-group-notify",
                                "claim", claimName));
                    }
                } else {
                    // Unclaim all chunks
                    int total = chunkManager.adminUnclaimAll(target.getUniqueId());
                    chunkManager.saveData();
                    sender.sendMessage(ChatColor.GREEN + "Removed all " + total + " chunk claim(s) owned by " + targetName + ".");

                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(messages.getFor(target.getUniqueId(), "admin-unclaim-all-notify"));
                    }
                }
                return true;
            }

            // ── /chunkadmin setlimit <player> <amount> ──
            if (sub.equals("setlimit")) {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chunkadmin setlimit <player> <amount>");
                    return true;
                }
                String targetName = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                    return true;
                }
                try {
                    int limit = Integer.parseInt(args[2]);
                    chunkManager.setPlayerLimit(target.getUniqueId(), limit);
                    chunkManager.saveData();
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s chunk limit to " + (limit == 0 ? "Unlimited" : limit) + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number format for limit.");
                }
                return true;
            }

            // ── /chunkadmin removelimit <player> ──
            if (sub.equals("removelimit")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chunkadmin removelimit <player>");
                    return true;
                }
                String targetName = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                    return true;
                }
                chunkManager.setPlayerLimit(target.getUniqueId(), null);
                chunkManager.saveData();
                sender.sendMessage(ChatColor.GREEN + "Removed individual chunk limit for " + target.getName() + " (reverted to default).");
                return true;
            }

            // ── /chunkadmin resetflag <flag> [remove|true|false] ──
            if (sub.equals("resetflag") || sub.equals("removeflag") || sub.equals("setflag")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chunkadmin resetflag <flag> [remove|true|false]");
                    sender.sendMessage(ChatColor.GRAY + "Available flags: " + String.join(", ", ChunkManager.ALL_FLAGS));
                    return true;
                }
                String flagName = args[1].toLowerCase();
                boolean validFlag = false;
                for (String f : ChunkManager.ALL_FLAGS) {
                    if (f.equalsIgnoreCase(flagName)) {
                        flagName = f;
                        validFlag = true;
                        break;
                    }
                }
                if (!validFlag) {
                    sender.sendMessage(ChatColor.RED + "Unknown flag '" + flagName + "'. Available flags: " + String.join(", ", ChunkManager.ALL_FLAGS));
                    return true;
                }

                String action = args.length >= 3 ? args[2].toLowerCase() : (sub.equals("removeflag") ? "remove" : "remove");
                if (action.equals("remove") || action.equals("reset") || action.equals("default")) {
                    int count = chunkManager.removeFlagFromAllClaims(flagName);
                    sender.sendMessage(ChatColor.GREEN + "Removed flag '" + flagName + "' from " + count + " claim group(s). All claims now use server default (" + (chunkManager.getDefaultFlag(flagName) ? "enabled" : "disabled") + ").");
                } else if (action.equals("true") || action.equals("enable") || action.equals("on")) {
                    int count = chunkManager.setFlagOnAllClaims(flagName, true);
                    sender.sendMessage(ChatColor.GREEN + "Force-enabled flag '" + flagName + "' across all " + count + " claim group(s).");
                } else if (action.equals("false") || action.equals("disable") || action.equals("off")) {
                    int count = chunkManager.setFlagOnAllClaims(flagName, false);
                    sender.sendMessage(ChatColor.YELLOW + "Force-disabled flag '" + flagName + "' across all " + count + " claim group(s).");
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown action '" + action + "'. Valid options: remove, true, false");
                }
                return true;
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("You must be a player to open the GUI. Available console commands: unclaimplayer, setlimit, removelimit, resetflag.");
            return true;
        }

        gui.openHome(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("chunkclaim.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = List.of("unclaim", "unclaimplayer", "setlimit", "removelimit", "resetflag", "removeflag", "setflag");
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("unclaimplayer") || sub.equals("setlimit") || sub.equals("removelimit")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        names.add(p.getName());
                    }
                }
                return names;
            } else if (sub.equals("resetflag") || sub.equals("removeflag") || sub.equals("setflag")) {
                List<String> flags = new ArrayList<>();
                for (String f : ChunkManager.ALL_FLAGS) {
                    if (f.toLowerCase().startsWith(args[1].toLowerCase())) {
                        flags.add(f);
                    }
                }
                return flags;
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("unclaimplayer")) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target != null) {
                    List<String> claims = chunkManager.getPlayerClaimNames(target.getUniqueId());
                    List<String> result = new ArrayList<>();
                    result.add("--all");
                    for (String c : claims) {
                        if (c.toLowerCase().startsWith(args[2].toLowerCase())) {
                            result.add(c);
                        }
                    }
                    return result;
                }
            } else if (sub.equals("resetflag") || sub.equals("setflag")) {
                List<String> actions = List.of("remove", "false", "true");
                List<String> result = new ArrayList<>();
                for (String a : actions) {
                    if (a.startsWith(args[2].toLowerCase())) result.add(a);
                }
                return result;
            }
        }

        return Collections.emptyList();
    }
}
