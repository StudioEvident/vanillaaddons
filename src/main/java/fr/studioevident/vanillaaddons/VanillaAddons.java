package fr.studioevident.vanillaaddons;

import fr.studioevident.vanillaaddons.listeners.VanillaAddonsListeners;
import fr.studioevident.vanillaaddons.utils.Language;
import fr.studioevident.vanillaaddons.utils.VanillaAddonsStorage;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VanillaAddons extends JavaPlugin {

    private final NamespacedKey rottenLeatherRecipeNamespacedKey = new NamespacedKey(this, "rottenLeatherRecipe");

    private final VanillaAddonsStorage storage = new VanillaAddonsStorage(this);
    private final Map<World, Boolean> raining = new HashMap<>();

    public Language language;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try { this.storage.loadData(); } catch (IOException e) { e.printStackTrace(); }

        this.language = new Language(this);
        for (World world : Bukkit.getWorlds()) if (world.getEnvironment() == World.Environment.NORMAL) this.raining.put(world, !world.isClearWeather());

        getServer().getPluginManager().registerEvents(new VanillaAddonsListeners(this, storage), this);

        Bukkit.getServer().getScheduler().runTaskTimer(this, this.everySecond(), 0L, 20L);
        Bukkit.getServer().getScheduler().runTaskTimer(this, this.hopperTick(), 0L, 4L);

        // Creation of recipes
        ItemStack rabbitHide = new ItemStack(Material.RABBIT_HIDE);
        SmokingRecipe rottenLeatherRecipe = new SmokingRecipe(rottenLeatherRecipeNamespacedKey, rabbitHide, Material.ROTTEN_FLESH, (float)0.1, 200);
        Bukkit.addRecipe(rottenLeatherRecipe);
    }

    @Override
    public void onDisable() {
        try { this.storage.saveData(); } catch (IOException e) { e.printStackTrace(); }
    }

    private Runnable everySecond() {
        return () -> {
            for (World world : this.raining.keySet()) {
                boolean isRaining = this.raining.get(world);

                List<Location> coppers = this.storage.getUnderDripstoneCoppers(world);

                if (isRaining) coppers.addAll(this.storage.getUnderSkyCoppers(world));

                for (Location loc : coppers) {
                    if (Math.random() < 0.1) {
                        Block block = loc.getWorld().getBlockAt(loc);

                        switch (block.getType().toString()) {
                            case "COPPER_BLOCK": block.setType(Material.EXPOSED_COPPER); break;
                            case "CUT_COPPER": block.setType(Material.EXPOSED_CUT_COPPER); break;
                            case "CUT_COPPER_STAIRS": block.setType(Material.EXPOSED_CUT_COPPER_STAIRS); break;
                            case "CUT_COPPER_SLAB": block.setType(Material.EXPOSED_CUT_COPPER_SLAB); break;
                            case "EXPOSED_COPPER": block.setType(Material.WEATHERED_COPPER); break;
                            case "EXPOSED_CUT_COPPER": block.setType(Material.WEATHERED_CUT_COPPER); break;
                            case "EXPOSED_CUT_COPPER_STAIRS": block.setType(Material.WEATHERED_CUT_COPPER_STAIRS); break;
                            case "EXPOSED_CUT_COPPER_SLAB": block.setType(Material.WEATHERED_CUT_COPPER_SLAB); break;
                            case "WEATHERED_COPPER": block.setType(Material.OXIDIZED_COPPER); break;
                            case "WEATHERED_CUT_COPPER": block.setType(Material.OXIDIZED_CUT_COPPER); break;
                            case "WEATHERED_CUT_COPPER_STAIRS": block.setType(Material.OXIDIZED_CUT_COPPER_STAIRS); break;
                            case "WEATHERED_CUT_COPPER_SLAB": block.setType(Material.OXIDIZED_CUT_COPPER_SLAB); break;
                        }
                    }
                }
            }
        };
    }

    private Runnable hopperTick() {
        return () -> {
            for (World world : Bukkit.getWorlds()) {
                storage.getHoppersFacingTable(world);
            }
        };
    }

    public void sendMessage(Player player, String configPath, Object... parameters) {
        String message = getConfig().getString("messages." + configPath, "");
        for (int i = 0; i < parameters.length - 1; i += 2) {
            message = message.replace(parameters[i].toString(), parameters[i + 1].toString());
        }
        player.spigot().sendMessage(TextComponent.fromLegacyText(message));
    }
    public Entity getTargetEntity(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();

        RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(eyeLocation.add(direction), direction, 6);
        if (rayTraceResult == null) return null;

        return rayTraceResult.getHitEntity();
    }

    public void setRaining(World world, boolean isRaining) {
        this.raining.put(world, isRaining);
    }

    public boolean isNonWaxedCopper(Block block) {
        return block.getType() == Material.COPPER_BLOCK ||
                block.getType() == Material.CUT_COPPER ||
                block.getType() == Material.CUT_COPPER_STAIRS ||
                block.getType() == Material.CUT_COPPER_SLAB ||
                block.getType() == Material.EXPOSED_COPPER ||
                block.getType() == Material.EXPOSED_CUT_COPPER ||
                block.getType() == Material.EXPOSED_CUT_COPPER_STAIRS ||
                block.getType() == Material.EXPOSED_CUT_COPPER_SLAB ||
                block.getType() == Material.WEATHERED_COPPER ||
                block.getType() == Material.WEATHERED_CUT_COPPER ||
                block.getType() == Material.WEATHERED_CUT_COPPER_STAIRS ||
                block.getType() == Material.WEATHERED_CUT_COPPER_SLAB;
    }

    public boolean isUnderSky(Block block) {
        Location blockLoc = block.getLocation();
        World world = block.getWorld();

        for (int i = (int)blockLoc.getY() + 1; i < world.getMaxHeight(); i++) {
            Location loc = new Location(world, blockLoc.getX(), i, blockLoc.getZ());
            if (world.getBlockAt(loc).getType() != Material.AIR) return false;
        }

        return true;
    }

    public boolean canSeeSky(Block block) {
        Location blockLoc = block.getLocation();
        World world = block.getWorld();

        for (int i = (int)blockLoc.getY()+1; i < world.getMaxHeight(); i++) {
            Location loc = new Location(world, blockLoc.getX(), i, blockLoc.getZ());
            if (world.getBlockAt(loc).getType().isOccluding()) return false;
        }

        return true;
    }

    public boolean isUnderWaterDripstone(Block block) {
        return isUnderDripstone(block, Material.WATER, false);
    }

    public boolean isUnderLavaDripstone(Block block) {
        return isUnderDripstone(block, Material.LAVA, false);
    }

    public boolean isUnderWaterFullDripstone(Block block) {
        return isUnderDripstone(block, Material.WATER, true);
    }

    public boolean isUnderLavaFullDripstone(Block block) {
        return isUnderDripstone(block, Material.LAVA, true);
    }

    private boolean isUnderDripstone(Block block, Material material, boolean fullDripstone) {
        if (material != Material.LAVA && material != Material.WATER) return false;
        World world = block.getWorld();

        Location blockLoc = block.getLocation();
        int maxHeight = world.getMaxHeight();

        int i = (int)blockLoc.getY()+1;
        for (int y = i; y < maxHeight; y++) {
            Location loc = new Location(world, blockLoc.getX(), y, blockLoc.getZ());
            Material aboveBlock = world.getBlockAt(loc).getType();

            if (aboveBlock == Material.POINTED_DRIPSTONE) { i=y; break; }
            else if (aboveBlock != Material.AIR) return false;
        }

        for (int y = i+1; y < maxHeight; y++) {
            Location loc = new Location(world, blockLoc.getX(), y, blockLoc.getZ());
            Material aboveBlock = world.getBlockAt(loc).getType();

            if (aboveBlock != Material.POINTED_DRIPSTONE) {
                if (aboveBlock == Material.AIR || (fullDripstone && aboveBlock != Material.DRIPSTONE_BLOCK)) return false;

                loc = new Location(world, blockLoc.getX(), y+1, blockLoc.getZ());
                aboveBlock = world.getBlockAt(loc).getType();

                return aboveBlock == material;
            }
        }

        return false;
    }
}

