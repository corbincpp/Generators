package org.corbin.caverngenerators;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeneratorTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null; // Let Bukkit handle player name completion
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> amounts = new ArrayList<>();
            for (int i = 1; i <= 64; i++) {
                amounts.add(String.valueOf(i));
            }
            return amounts;
        }
        return new ArrayList<>();
    }
}
