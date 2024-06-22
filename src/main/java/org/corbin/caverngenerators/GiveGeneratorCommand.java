package org.corbin.caverngenerators;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.stream.Collectors;

public class GiveGeneratorCommand implements CommandExecutor {
    private final CavernGenerators plugin;

    public GiveGeneratorCommand(CavernGenerators plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /generator <reload|give> [player] [amount]");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("cavern.reload")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("Generators configuration reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("cavern.give")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            Player target;
            int amount = 1;

            if (args.length == 2) {
                if (sender instanceof Player) {
                    target = (Player) sender;
                    amount = Integer.parseInt(args[1]);
                } else {
                    sender.sendMessage("You must specify a player when using this command from the console.");
                    return true;
                }
            } else if (args.length == 3) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                amount = Integer.parseInt(args[2]);
            } else {
                sender.sendMessage("Usage: /generator give [player] [amount]");
                return true;
            }

            giveGenerators(target, amount);
            sender.sendMessage("Given " + amount + " generator(s) to " + target.getName() + ".");
            return true;
        }

        sender.sendMessage("Usage: /generator <reload|give> [player] [amount]");
        return true;
    }

    private void giveGenerators(Player player, int amount) {
        ItemStack generatorBeacon = new ItemStack(Material.BEACON, amount);
        ItemMeta meta = generatorBeacon.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("generator.item.display_name")));
        meta.setLore(plugin.getConfig().getStringList("generator.item.lore").stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList()));
        meta.getPersistentDataContainer().set(CavernGenerators.generatorKey, PersistentDataType.BYTE, (byte) 1);
        generatorBeacon.setItemMeta(meta);

        player.getInventory().addItem(generatorBeacon);
    }
}
