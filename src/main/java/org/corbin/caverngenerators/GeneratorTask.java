package org.corbin.caverngenerators;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class GeneratorTask extends BukkitRunnable {
    private final CavernGenerators plugin;
    private final Block beaconBlock;
    private final String blockLocation;
    private final FileConfiguration config;

    public GeneratorTask(Block beaconBlock, CavernGenerators plugin) {
        this.beaconBlock = beaconBlock;
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.blockLocation = beaconBlock.getLocation().toString();
    }

    @Override
    public void run() {
        if (!(beaconBlock.getState() instanceof TileState)) {
            cancel();
            return;
        }

        TileState tileState = (TileState) beaconBlock.getState();
        if (!tileState.getPersistentDataContainer().has(CavernGenerators.generatorKey, PersistentDataType.BYTE)) {
            cancel();
            return;
        }

        int currentFuel = plugin.getGeneratorFuel(blockLocation);
        if (currentFuel > 0) {
            plugin.setGeneratorFuel(blockLocation, currentFuel - 1);
            int moneyEarned = plugin.getGeneratorMoney(blockLocation) + config.getInt("generator.money_per_minute");
            plugin.setGeneratorMoney(blockLocation, moneyEarned);

            updateAllOpenInventories();
        } else {
            cancel();
        }
    }

    private void updateAllOpenInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory != null && player.getOpenInventory().getTitle().equals(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("generator.gui.title")))) {
                int currentFuel = plugin.getGeneratorFuel(blockLocation);
                int currentMoney = plugin.getGeneratorMoney(blockLocation);
                plugin.updateFuelItem(inventory, plugin.getConfig(), currentFuel);
                plugin.updateCollectItem(inventory, plugin.getConfig(), currentMoney);
            }
        }
    }
}
