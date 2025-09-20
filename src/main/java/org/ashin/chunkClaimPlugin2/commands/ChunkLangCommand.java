package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChunkLangCommand implements CommandExecutor, TabCompleter {
    private final MessageManager messages;

    public ChunkLangCommand(MessageManager messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            String current = messages.getPlayerLocale(uuid);
            player.sendMessage(messages.getFor(uuid, "lang-current", "locale", current));
            player.sendMessage(messages.getFor(uuid, "lang-usage", "label", label));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("get")) {
            String current = messages.getPlayerLocale(uuid);
            player.sendMessage(messages.getFor(uuid, "lang-current", "locale", current));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            List<String> avail = messages.getAvailableLocales();
            player.sendMessage(messages.getFor(uuid, "lang-available", "list", String.join(", ", avail)));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage(messages.getFor(uuid, "lang-specify", "label", label));
                return true;
            }
            String loc = args[1];
            if (!messages.isLocaleAvailable(loc)) {
                List<String> avail = messages.getAvailableLocales();
                player.sendMessage(messages.getFor(uuid, "lang-unknown", "locale", loc));
                player.sendMessage(messages.getFor(uuid, "lang-available", "list", String.join(", ", avail)));
                return true;
            }
            messages.setPlayerLocale(uuid, loc);
            player.sendMessage(messages.getFor(uuid, "lang-set", "locale", loc));
            return true;
        }

        player.sendMessage(messages.getFor(uuid, "lang-usage", "label", label));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String a0 = args[0].toLowerCase();
            if ("set".startsWith(a0)) out.add("set");
            if ("get".startsWith(a0)) out.add("get");
            if ("list".startsWith(a0)) out.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            for (String l : messages.getAvailableLocales()) {
                if (l.toLowerCase().startsWith(args[1].toLowerCase())) out.add(l);
            }
        }
        return out;
    }
}
