package org.corbin.caverngenerators;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class CavernGenerators extends JavaPlugin {
    public static NamespacedKey generatorKey;
    private static CavernGenerators instance;
    private File databaseFile;
    private FileConfiguration database;

    @Override
    public void onEnable() {
        instance = this;
        generatorKey = new NamespacedKey(this, "generator_key");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new GeneratorListener(), this);
        this.getCommand("generator").setExecutor(new GiveGeneratorCommand(this));
        this.getCommand("generator").setTabCompleter(new GeneratorTabCompleter());
        loadDatabase();
    }

    @Override
    public void onDisable() {
        saveDatabase();
    }

    public static CavernGenerators getInstance() {
        return instance;
    }

    private void loadDatabase() {
        databaseFile = new File(getDataFolder(), "database.yml");
        if (!databaseFile.exists()) {
            databaseFile.getParentFile().mkdirs();
            saveResource("database.yml", false);
        }
        database = YamlConfiguration.loadConfiguration(databaseFile);
    }

    public FileConfiguration getDatabase() {
        return this.database;
    }

    public void saveDatabase() {
        try {
            database.save(databaseFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Methods for fuel and money management
    public int getGeneratorFuel(String blockLocation) {
        return database.getInt(blockLocation + ".fuel", 0);
    }

    public void setGeneratorFuel(String blockLocation, int amount) {
        database.set(blockLocation + ".fuel", amount);
        saveDatabase();
    }

    public int getGeneratorMoney(String blockLocation) {
        return database.getInt(blockLocation + ".money", 0);
    }

    public void setGeneratorMoney(String blockLocation, int amount) {
        database.set(blockLocation + ".money", amount);
        saveDatabase();
    }

    public void resetGeneratorData(Block block) {
        database.set(block.getLocation().toString(), null);
        saveDatabase();
    }

    public ItemStack createGuiItem(FileConfiguration config, String path, int currentFuel, int maxFuel, int currentMoney) {
        ItemStack item = new ItemStack(Material.valueOf(config.getString(path + ".material")));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString(path + ".display_name")));
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&',
                config.getString(path + ".lore")
                        .replace("{amount}", String.valueOf(currentFuel))
                        .replace("{max_fuel}", String.valueOf(maxFuel))
                        .replace("{amount_made}", String.valueOf(currentMoney)))));
        item.setItemMeta(meta);
        return item;
    }

    public void updateFuelItem(Inventory inventory, FileConfiguration config, int amount) {
        ItemStack fuelItem = inventory.getItem(config.getInt("generator.gui.slots.fuel_slot"));
        if (fuelItem != null) {
            ItemMeta fuelMeta = fuelItem.getItemMeta();
            fuelMeta.setLore(Collections.singletonList(
                    ChatColor.translateAlternateColorCodes('&',
                            config.getString("generator.gui.items.fuel_item.lore")
                                    .replace("{amount}", String.valueOf(amount))
                                    .replace("{max_fuel}", String.valueOf(config.getInt("generator.max_fuel"))))
            ));
            fuelItem.setItemMeta(fuelMeta);
            inventory.setItem(config.getInt("generator.gui.slots.fuel_slot"), fuelItem);
        }
    }

    public void updateCollectItem(Inventory inventory, FileConfiguration config, int amountMade) {
        ItemStack collectItem = inventory.getItem(config.getInt("generator.gui.slots.collect_button"));
        if (collectItem != null) {
            ItemMeta collectMeta = collectItem.getItemMeta();
            collectMeta.setLore(Collections.singletonList(
                    ChatColor.translateAlternateColorCodes('&',
                            config.getString("generator.gui.items.collect_item.lore")
                                    .replace("{amount_made}", String.valueOf(amountMade)))
            ));
            collectItem.setItemMeta(collectMeta);
            inventory.setItem(config.getInt("generator.gui.slots.collect_button"), collectItem);
        }
    }
}
