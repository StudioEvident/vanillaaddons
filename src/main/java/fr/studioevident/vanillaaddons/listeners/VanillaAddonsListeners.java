package fr.studioevident.vanillaaddons.listeners;

import fr.studioevident.vanillaaddons.VanillaAddons;
import fr.studioevident.vanillaaddons.utils.VanillaAddonsStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.EnchantingTable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class VanillaAddonsListeners implements Listener {

    private final VanillaAddons plugin;
    private final VanillaAddonsStorage storage;

    public VanillaAddonsListeners(VanillaAddons plugin, VanillaAddonsStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    // Players Events

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;

        EnchantingTable enchantingTable = (EnchantingTable)block.getState();

    }

    // Inventory Events

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.ENCHANTING) return;

        EnchantingInventory eInventory = (EnchantingInventory)inventory;
        Location location = event.getInventory().getLocation();
        Block block = location.getBlock();

        eInventory.setSecondary(new ItemStack(Material.LAPIS_LAZULI, this.storage.getLapisCount(block)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.ENCHANTING) return;

        EnchantingInventory eInventory = (EnchantingInventory)inventory;
        Block block = inventory.getLocation().getBlock();
        ItemStack lapisSlot = eInventory.getSecondary();

        int lapisCount = lapisSlot == null ? 0 : lapisSlot.getAmount();
        if (lapisSlot != null) lapisSlot.setAmount(0);

        this.storage.setLapisCount(block, lapisCount);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventory(InventoryEvent event) {
        System.out.println(event.getEventName());
    }

    // Blocks Events

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlaced(BlockPlaceEvent event) {
        Block block = event.getBlock();
        World world = block.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        if (block.getType() == Material.ENCHANTING_TABLE) {
            this.storage.addEnchantingTable(block.getLocation());
            this.storage.setLapisCount(block, 0);
        }
        if (block.getType() == Material.BEACON) this.storage.addBeacon(block.getLocation());
        if (this.plugin.isNonWaxedCopper(block)) this.storage.addCopper(block.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        World world = block.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        if (block.getType() == Material.BEACON) this.storage.removeBeacon(block.getLocation());
        if (block.getType() == Material.ENCHANTING_TABLE) this.storage.addEnchantingTable(block.getLocation());
        if (this.plugin.isNonWaxedCopper(block)) this.storage.removeCopper(block.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        preventExplosion(event.blockList());
    }

    // Entity Events

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        preventExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;

        Location location = event.getLocation();
        World world = location.getWorld();
        for (Location loc : this.storage.getActivatedBeacons(world)) {
            Block block = world.getBlockAt(loc);
            Beacon beacon = (Beacon)block.getState();
            int beaconLvl = beacon.getTier();
            int beaconRadius = (beaconLvl+1)*10;
            System.out.println(beaconLvl);

            // Limit the radius under the beacon but not above it
            Location entityLoc = location.getY() > loc.getY() ? new Location(world, location.getX(), loc.getY(), location.getZ()) : location;
            if (entityLoc.distance(loc) <= beaconRadius) event.setCancelled(true); return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Villager)) return;

        Villager villager = (Villager)entity;
        if (villager.getLastDamageCause().getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        Entity damagingEntity = ((EntityDamageByEntityEvent)villager.getLastDamageCause()).getDamager();
        if (!(damagingEntity instanceof Zombie)) return;

        villager.zombify();
    }

    // World Events

    @EventHandler(ignoreCancelled = true)
    public void onWeatherChanged(WeatherChangeEvent event) {
        World world = event.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        this.plugin.setRaining(world, event.toWeatherState());
    }

    // Multiple times used functions

    private void preventExplosion(List<Block> explodedBlocks) {
        for (Block block : explodedBlocks)  {
            if (this.plugin.isNonWaxedCopper(block)) this.storage.removeCopper(block.getLocation());
        }
    }
}
