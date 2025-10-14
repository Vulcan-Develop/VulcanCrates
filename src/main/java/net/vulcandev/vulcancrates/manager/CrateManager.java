package net.vulcandev.vulcancrates.manager;

import net.xantharddev.vulcanlib.Logger;
import net.xantharddev.vulcanlib.libs.SerializableLocation;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages loading, storing, and accessing crate definitions.
 * Handles crate configuration parsing, location management, and chest placement/removal.
 */
public class CrateManager {

    private final VulcanCrates plugin;
    private final Map<String, Crate> crates = new ConcurrentHashMap<>();

    public CrateManager(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    public void loadAllCrates() {
        crates.clear();
        
        File cratesFolder = new File(plugin.getDataFolder(), "crates");
        if (!cratesFolder.exists()) {
            Logger.log(plugin, "Crates folder does not exist, creating and copying defaults...");
            cratesFolder.mkdirs();
            // Copy default crates from resources
            copyDefaultCrates();
        }
        
        File[] crateFiles = cratesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (crateFiles == null) {
            Logger.log(plugin, "No crate files found in crates directory!");
            return;
        }
        
        Logger.log(plugin, "Loading " + crateFiles.length + " crate files...");
        for (File crateFile : crateFiles) {
            String crateName = crateFile.getName().replace(".yml", "");
            loadCrate(crateName);
        }
        
        Logger.log(plugin, "Successfully loaded " + crates.size() + " crates: " + crates.keySet());
    }

    private void copyDefaultCrates() {
        try {
            Logger.log(plugin, "Copying default crate files from resources...");
            plugin.saveResource("crates/common.yml", false);
            plugin.saveResource("crates/legendary.yml", false);
            Logger.log(plugin, "Default crate files copied successfully.");
        } catch (Exception e) {
            Logger.log(plugin, "Failed to copy default crate files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadCrate(String crateName) {
        try {
            Logger.log(plugin, "Loading crate: " + crateName);
            File crateFile = new File(plugin.getDataFolder(), "crates/" + crateName + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);

            Crate crate = createCrateFromConfig(crateName, config);
            crates.put(crateName, crate);
            Logger.log(plugin, "Successfully loaded crate '" + crateName + "' with " + crate.getPrizes().size() + " prizes");
        } catch (Exception e) {
            Logger.log(plugin, "Failed to load crate '" + crateName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Crate createCrateFromConfig(String crateName, FileConfiguration config) {
        Crate crate = new Crate(crateName);

        crate.setDisplayName(config.getString("display-name", "&f&l" + crateName + " Crate"));

        if (config.contains("hologram")) {
            crate.setHologramLines(config.getStringList("hologram.lines"));
            crate.setHologramYOffset(config.getDouble("hologram.y-offset", 1.0));
        } else {
            List<String> defaultLines = new ArrayList<>();
            defaultLines.add(crate.getDisplayName());
            defaultLines.add("&eRight-click to open");
            defaultLines.add("&eLeft-click to preview");
            crate.setHologramLines(defaultLines);
            crate.setHologramYOffset(1.0);
        }

        if (config.contains("prizes")) {
            Map<String, Prize> prizes = new LinkedHashMap<>();
            try {
                if (config.getConfigurationSection("prizes") != null) {
                    Set<String> prizeKeys = config.getConfigurationSection("prizes").getKeys(false);
                    for (String prizeKey : prizeKeys) {
                        Prize prize = loadPrizeFromConfig(config, "prizes." + prizeKey);
                        prizes.put(prizeKey, prize);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            crate.setPrizes(prizes);
        }

        return crate;
    }

    private Prize loadPrizeFromConfig(FileConfiguration config, String path) {
        Prize prize = new Prize();
        
        prize.setName(config.getString(path + ".name", "Unknown Prize"));
        prize.setCommands(config.getStringList(path + ".commands"));

        if (config.contains(path + ".chance")) {
            prize.setChance(config.getDouble(path + ".chance", 1.0));
        } else {
            prize.setChance(config.getDouble(path + ".weight", 1.0));
        }
        
        prize.setAnnounce(config.getBoolean(path + ".announce", false));
        prize.setUrl(config.getString(path + ".url", ""));

        if (config.contains(path + ".item")) {
            String materialStr = config.getString(path + ".item.type",
                                config.getString(path + ".item.material", "STONE"));
            prize.setMaterial(MaterialDb.get(materialStr));

            prize.setAmount(config.getInt(path + ".item.amount", 1));

            int itemData = config.getInt(path + ".item.damage",
                          config.getInt(path + ".item.data", 0));
            prize.setData((byte) itemData);

            if (config.contains(path + ".item.name")) {
                prize.setName(config.getString(path + ".item.name"));
            }

            prize.setLore(config.getStringList(path + ".item.lore"));

            boolean glowing = config.getBoolean(path + ".item.glowing",
                             config.getBoolean(path + ".item.glow", false));
            prize.setGlow(glowing);
        }

        return prize;
    }

    private void placeChestAtLocation(SerializableLocation sLocation) {
        if (sLocation == null) return;
        Location bukkitLoc = sLocation.getLocation();

        if (bukkitLoc == null || bukkitLoc.getWorld() == null) return;

        if (!plugin.conf().getBoolean("crate-block.auto-place-on-set")) {
            return;
        }

        Block block = bukkitLoc.getBlock();

        List<String> replaceableBlocks = plugin.conf().getStringList("crate-block.replaceable-blocks");
        if (replaceableBlocks == null || replaceableBlocks.isEmpty()) {
            replaceableBlocks = Arrays.asList("AIR", "GRASS", "TALL_GRASS", "WATER", "LAVA");
        }

        boolean canReplace = replaceableBlocks.contains(block.getType().name());

        if (canReplace) {
            String chestMaterial = plugin.conf().getString("crate-block.material");
            block.setType(MaterialDb.get(chestMaterial));
        }
    }

    public Crate getCrate(String name) {
        return crates.get(name);
    }

    public Crate getCrateIgnoreCase(String name) {
        if (name == null) return null;

        Crate crate = crates.get(name);
        if (crate != null) {
            return crate;
        }

        for (Map.Entry<String, Crate> entry : crates.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public Collection<Crate> getAllCrates() {
        return crates.values();
    }

    public Set<String> getCrateNames() {
        return crates.keySet();
    }

    public void removeCrateLocation(String crateName) {
        Crate crate = getCrate(crateName);
        if (crate != null) {
            SerializableLocation location = crate.getLocation();
            if (location != null) {
                removeChestAtLocation(location);
                plugin.removeCrateLocation(crateName);
            }
        }
    }

    private void removeChestAtLocation(SerializableLocation sLocation) {
        if (sLocation == null) return;
        Location bukkitLoc = sLocation.getLocation();

        if (bukkitLoc == null || bukkitLoc.getWorld() == null) return;

        Block block = bukkitLoc.getBlock();
        if (block.getType() == Material.CHEST) {
            block.setType(Material.AIR);
        }
    }

    public void loadAndRestoreChests() {
        if (!plugin.conf().getBoolean("crate-block.auto-restore-on-startup")) return;
        
        for (Crate crate : crates.values()) {
            if (crate.getLocation() != null) {
                placeChestAtLocation(crate.getLocation());
            }
        }
    }
}