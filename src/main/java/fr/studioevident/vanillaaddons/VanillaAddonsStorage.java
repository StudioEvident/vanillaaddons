package fr.studioevident.vanillaaddons.utils;

import com.google.gson.Gson;
import fr.studioevident.vanillaaddons.VanillaAddons;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataType;

import java.io.*;
import java.util.*;

public class VanillaAddonsStorage {

    private final VanillaAddons plugin;

    private final NamespacedKey lapisCountNamespacedKey;

    private final Map<World, List<Location>> coppers = new HashMap<>();
    private final Map<World, List<Location>> beacons = new HashMap<>();
    private final Map<World, List<Location>> enchantingTables = new HashMap<>();

    public VanillaAddonsStorage(VanillaAddons plugin) {
        this.plugin = plugin;

        this.lapisCountNamespacedKey = new NamespacedKey(plugin, "lapisCount");
    }

    public void loadData() throws IOException {
        Gson gson = new Gson();
        File file = new File(this.plugin.getDataFolder().getAbsolutePath() + "/data.json");
        file.getParentFile().mkdir();
        file.createNewFile();
        Reader reader = new FileReader(file);

        Map<String, Map<String, List<List<Double>>>> loaded = gson.fromJson(reader, Map.class);

        // Load coppers locations
        Map<String, List<List<Double>>> loadedCoppers = loaded.get("coppers");
        for (String worldName : loadedCoppers.keySet()) {
            List<List<Double>> wCop = loadedCoppers.get(worldName);
            World world = this.plugin.getServer().getWorld(worldName);
            for (List<Double> coos : wCop) {
                Location loc = new Location(world, coos.get(0), coos.get(1), coos.get(2));
                addCopper(loc);
            }
        }

        // Load beacons locations
        Map<String, List<List<Double>>> loadedBeacons = loaded.get("beacons");
        for (String worldName : loadedBeacons.keySet()) {
            List<List<Double>> wBeacons = loadedBeacons.get(worldName);
            World world = this.plugin.getServer().getWorld(worldName);
            for (List<Double> coos : wBeacons) {
                Location loc = new Location(world, coos.get(0), coos.get(1), coos.get(2));
                addBeacon(loc);
            }
        }
    }

    public void saveData() throws IOException {
        Gson gson = new Gson();
        File file = new File(this.plugin.getDataFolder().getAbsolutePath() + "/data.json");
        file.getParentFile().mkdir();
        file.createNewFile();
        Writer writer = new FileWriter(file, false);

        Map<String, Object> toSave = new HashMap<>();

        // Coppers locations saving
        Map<String, List<List<Double>>> coppers = new HashMap<>();
        for (World world : this.coppers.keySet()) {
            List<Location> wCop = getCoppers(world);
            List<List<Double>> saveWCop = new ArrayList<>();
            for (Location loc : wCop) {
                List<Double> coos = new ArrayList<>();
                coos.add(loc.getX());
                coos.add(loc.getY());
                coos.add(loc.getZ());
                saveWCop.add(coos);
            }
            coppers.put(world.getName(), saveWCop);
        }
        toSave.put("coppers", coppers);

        // Beacons locations saving
        Map<String, List<List<Double>>> beacons = new HashMap<>();
        for (World world : this.beacons.keySet()) {
            List<Location> wBeacons = getBeacons(world);
            List<List<Double>> saveWBeacons = new ArrayList<>();
            for (Location loc : wBeacons) {
                List<Double> coos = new ArrayList<>();
                coos.add(loc.getX());
                coos.add(loc.getY());
                coos.add(loc.getZ());
                saveWBeacons.add(coos);
            }
            beacons.put(world.getName(), saveWBeacons);
        }
        toSave.put("beacons", beacons);

        // Saving
        writer.write(gson.toJson(toSave));

        writer.flush();
        writer.close();
    }

    public List<Location> getCoppers(World world) {
        List<Location> worldCoppers = this.coppers.get(world);
        return worldCoppers == null ? new ArrayList<>() : worldCoppers;
    }

    public List<Location> getUnderSkyCoppers(World world) {
        List<Location> validCoppers = new ArrayList<>();

        for (Location loc : getCoppers(world)) {
            Block block = loc.getWorld().getBlockAt(loc);

            if (!this.plugin.isNonWaxedCopper(block)) removeCopper(loc);
            if (this.plugin.isUnderSky(block)) validCoppers.add(loc);
        }

        return validCoppers;
    }

    public List<Location> getUnderDripstoneCoppers(World world) {
        List<Location> validCoppers = new ArrayList<>();

        for (Location loc : getCoppers(world)) {
            Block block = world.getBlockAt(loc);

            if (!this.plugin.isNonWaxedCopper(block)) { removeCopper(loc); continue; }
            if (this.plugin.isUnderWaterDripstone(block)) validCoppers.add(loc);
        }

        return validCoppers;
    }

    public void addCopper(Location location) {
        World world = location.getWorld();
        List<Location> worldCoppers = getCoppers(world);
        worldCoppers.add(location);
        this.coppers.put(world, worldCoppers);
    }

    public void removeCopper(Location location) {
        World world = location.getWorld();
        List<Location> worldCoppers = getCoppers(world);

        if (!worldCoppers.contains(location)) return;

        worldCoppers.remove(worldCoppers.indexOf(location));
        this.coppers.put(world, worldCoppers);
    }

    public List<Location> getBeacons(World world) {
        List<Location> worldBeacons = this.beacons.get(world);
        return worldBeacons == null ? new ArrayList<>() : worldBeacons;
    }

    public List<Location> getActivatedBeacons(World world) {
        List<Location> activatedBeacons = new ArrayList<>();

        for (Location loc : getBeacons(world)) {
            Block block = world.getBlockAt(loc);
            if (block.getType() != Material.BEACON) removeBeacon(loc);

            Beacon beacon = (Beacon)block.getState();
            if (beacon.getTier() > 0 && this.plugin.canSeeSky(block)) activatedBeacons.add(loc);
        }

        return activatedBeacons;
    }

    public void addBeacon(Location location) {
        World world = location.getWorld();
        List<Location> worldBeacons = getBeacons(world);
        worldBeacons.add(location);
        this.beacons.put(world, worldBeacons);
    }

    public void removeBeacon(Location location) {
        World world = location.getWorld();
        List<Location> worldBeacons = getBeacons(world);
        worldBeacons.remove(location);
        this.beacons.put(world, worldBeacons);
    }

    public List<Location> getEnchantingTables(World world) {
        List<Location> worldTables = this.enchantingTables.get(world);
        return worldTables == null ? new ArrayList<>() : worldTables;
    }

    public HashMap<Location, List<Location>> getHoppersFacingTable(World world) {
        HashMap<Location, List<Location>> tables = new HashMap<>();

        for (Location loc : getEnchantingTables(world)) {
            List<Location> hoppers = new ArrayList<>();
            Block block = world.getBlockAt(loc);

            if (block.getType() != Material.ENCHANTING_TABLE) removeEnchantingTable(loc);

            for (int i = 0; i < 6; i++) {
                BlockFace face = BlockFace.values()[i];
                System.out.println(face);
            }
        }

        return tables;
    }

    public void addEnchantingTable(Location location) {
        World world = location.getWorld();
        List<Location> worldTables = getEnchantingTables(world);
        worldTables.add(location);
        this.enchantingTables.put(world, worldTables);
    }

    public void removeEnchantingTable(Location location) {
        World world = location.getWorld();
        List<Location> worldTables = getEnchantingTables(world);
        worldTables.remove(location);
        this.enchantingTables.put(world, worldTables);
    }

    public void setLapisCount(Block block, int count) {
        TileState container = (TileState)block.getState();
        container.getPersistentDataContainer().set(lapisCountNamespacedKey, PersistentDataType.INTEGER, count);
        container.update();
    }

    public int getLapisCount(Block block) {
        if (block.getType() != Material.ENCHANTING_TABLE) return -1;

        TileState container = (TileState) block.getState();
        int count = container.getPersistentDataContainer().get(lapisCountNamespacedKey, PersistentDataType.INTEGER);

        if (count < 0 || count > 64) {
            setLapisCount(block, 0);
            return 0;
        }

        return count;
    }
}
