package org.corbin.caverngenerators;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.stream.Collectors;

public class GeneratorListener implements Listener {
    private final HashSet<String> generatorChunks = new HashSet<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        ItemStack itemInHand = event.getItemInHand();

        if (block.getType() == Material.BEACON && itemInHand.hasItemMeta() && itemInHand.getItemMeta().getPersistentDataContainer().has(CavernGenerators.generatorKey, PersistentDataType.BYTE)) {
            Player player = event.getPlayer();
            Chunk chunk = block.getChunk();
            String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
            FileConfiguration config = CavernGenerators.getInstance().getConfig();

            if (generatorChunks.contains(chunkKey)) {
                sendMessageWithSound(player, config.getString("generator.messages.single_generator_per_chunk"), config.getString("generator.messages.single_generator_per_chunk_sound"));
                event.setCancelled(true);
            } else {
                generatorChunks.add(chunkKey);
                if (block.getState() instanceof TileState) {
                    TileState tileState = (TileState) block.getState();
                    tileState.getPersistentDataContainer().set(CavernGenerators.generatorKey, PersistentDataType.BYTE, (byte) 1);
                    tileState.update();
                }
                sendMessageWithSound(player, config.getString("generator.messages.generator_placed_successfully"), config.getString("generator.messages.generator_placed_successfully_sound"));
                CavernGenerators.getInstance().resetGeneratorData(block);
                new GeneratorTask(block, CavernGenerators.getInstance()).runTaskTimer(CavernGenerators.getInstance(), 1200L, 1200L);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.BEACON && block.getState() instanceof TileState) {
            TileState tileState = (TileState) block.getState();
            if (tileState.getPersistentDataContainer().has(CavernGenerators.generatorKey, PersistentDataType.BYTE)) {
                Chunk chunk = block.getChunk();
                String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
                generatorChunks.remove(chunkKey);
                dropGeneratorItem(block);
                CavernGenerators.getInstance().resetGeneratorData(block);
            }
        }
    }

    private void dropGeneratorItem(Block block) {
        FileConfiguration config = CavernGenerators.getInstance().getConfig();
        ItemStack generatorItem = new ItemStack(Material.valueOf(config.getString("generator.item.material")));
        ItemMeta meta = generatorItem.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("generator.item.display_name")));
        meta.setLore(config.getStringList("generator.item.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList()));
        meta.getPersistentDataContainer().set(CavernGenerators.generatorKey, PersistentDataType.BYTE, (byte) 1);
        generatorItem.setItemMeta(meta);
        block.getWorld().dropItemNaturally(block.getLocation(), generatorItem);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.BEACON && block.getState() instanceof TileState) {
                TileState tileState = (TileState) block.getState();
                if (tileState.getPersistentDataContainer().has(CavernGenerators.generatorKey, PersistentDataType.BYTE)) {
                    Player player = event.getPlayer();
                    openGeneratorGUI(player, block);
                    event.setCancelled(true); // Cancel the default beacon GUI
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = CavernGenerators.getInstance().getConfig();

        if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', config.getString("generator.gui.title")))) {
            event.setCancelled(true); // Prevent item removal

            if (event.getClick() == ClickType.LEFT) {
                int slot = event.getSlot();
                Block block = player.getTargetBlockExact(5); // Assuming the player is looking at the block they interacted with
                if (block != null && block.getType() == Material.BEACON && block.getState() instanceof TileState) {
                    String blockLocation = block.getLocation().toString();
                    if (slot == config.getInt("generator.gui.slots.fuel_slot")) {
                        int maxFuel = config.getInt("generator.max_fuel");
                        int currentFuel = CavernGenerators.getInstance().getGeneratorFuel(blockLocation);

                        if (currentFuel < maxFuel) {
                            int fuelToAdd = Math.min(maxFuel - currentFuel, getCoalAmount(player));
                            if (fuelToAdd > 0) {
                                CavernGenerators.getInstance().setGeneratorFuel(blockLocation, currentFuel + fuelToAdd);
                                removeCoalFromPlayer(player, fuelToAdd);
                                CavernGenerators.getInstance().updateFuelItem(inventory, config, currentFuel + fuelToAdd);
                                sendMessageWithSound(player, config.getString("generator.messages.fuel_added").replace("{amount}", String.valueOf(fuelToAdd)), config.getString("generator.messages.fuel_added_sound"));
                                updateAllOpenInventories(block);
                            } else {
                                sendMessageWithSound(player, config.getString("generator.messages.no_coal_in_inventory"), config.getString("generator.messages.no_coal_in_inventory_sound"));
                            }
                        } else {
                            sendMessageWithSound(player, config.getString("generator.messages.generator_full"), config.getString("generator.messages.generator_full_sound"));
                        }
                    } else if (slot == config.getInt("generator.gui.slots.collect_button")) {
                        int moneyEarned = CavernGenerators.getInstance().getGeneratorMoney(blockLocation);
                        if (moneyEarned > 0) {
                            CavernGenerators.getInstance().setGeneratorMoney(blockLocation, 0); // Reset money
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + moneyEarned);
                            sendMessageWithSound(player, null, config.getString("generator.messages.collected_money_sound"));
                            CavernGenerators.getInstance().updateCollectItem(inventory, config, 0);
                            updateAllOpenInventories(block);
                        } else {
                            sendMessageWithSound(player, config.getString("generator.messages.no_money_to_collect"), config.getString("generator.messages.no_money_to_collect_sound"));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        FileConfiguration config = CavernGenerators.getInstance().getConfig();

        if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', config.getString("generator.gui.title")))) {
            Block block = event.getPlayer().getTargetBlockExact(5); // Assuming the player is looking at the block they interacted with
            if (block != null && block.getType() == Material.BEACON) {
                int fuelAmount = CavernGenerators.getInstance().getGeneratorFuel(block.getLocation().toString());
                CavernGenerators.getInstance().updateFuelItem(inventory, config, fuelAmount);
            }
        }
    }

    private void openGeneratorGUI(Player player, Block beaconBlock) {
        FileConfiguration config = CavernGenerators.getInstance().getConfig();
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("generator.gui.title"));
        int size = config.getInt("generator.gui.size");

        Inventory gui = Bukkit.createInventory(null, size, title);

        String blockLocation = beaconBlock.getLocation().toString();
        int currentFuel = CavernGenerators.getInstance().getGeneratorFuel(blockLocation);
        int currentMoney = CavernGenerators.getInstance().getGeneratorMoney(blockLocation);

        ItemStack fuelItem = CavernGenerators.getInstance().createGuiItem(config, "generator.gui.items.fuel_item", currentFuel, config.getInt("generator.max_fuel"), currentMoney);
        ItemStack collectItem = CavernGenerators.getInstance().createGuiItem(config, "generator.gui.items.collect_item", currentFuel, config.getInt("generator.max_fuel"), currentMoney);

        gui.setItem(config.getInt("generator.gui.slots.fuel_slot"), fuelItem);
        gui.setItem(config.getInt("generator.gui.slots.collect_button"), collectItem);

        player.openInventory(gui);
    }

    private int getCoalAmount(Player player) {
        ItemStack[] items = player.getInventory().getContents();
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() == Material.valueOf(CavernGenerators.getInstance().getConfig().getString("generator.gui.items.fuel_item.material"))) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeCoalFromPlayer(Player player, int amount) {
        ItemStack[] items = player.getInventory().getContents();
        for (ItemStack item : items) {
            if (item != null && item.getType() == Material.valueOf(CavernGenerators.getInstance().getConfig().getString("generator.gui.items.fuel_item.material"))) {
                int itemAmount = item.getAmount();
                if (itemAmount > amount) {
                    item.setAmount(itemAmount - amount);
                    return;
                } else {
                    player.getInventory().removeItem(item);
                    amount -= itemAmount;
                }
            }
        }
    }

    private void sendMessageWithSound(Player player, String message, String sound) {
        if (message != null && !message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        if (sound != null && !sound.isEmpty()) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(sound), 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid sound: " + sound);
            }
        }
    }

    private void updateAllOpenInventories(Block block) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory != null && player.getOpenInventory().getTitle().equals(ChatColor.translateAlternateColorCodes('&', CavernGenerators.getInstance().getConfig().getString("generator.gui.title")))) {
                int currentFuel = CavernGenerators.getInstance().getGeneratorFuel(block.getLocation().toString());
                int currentMoney = CavernGenerators.getInstance().getGeneratorMoney(block.getLocation().toString());
                CavernGenerators.getInstance().updateFuelItem(inventory, CavernGenerators.getInstance().getConfig(), currentFuel);
                CavernGenerators.getInstance().updateCollectItem(inventory, CavernGenerators.getInstance().getConfig(), currentMoney);
            }
        }
    }
}
