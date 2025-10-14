package net.vulcandev.vulcancrates;

import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import me.plugin.libs.YamlDocument;
import net.xantharddev.vulcanlib.ConfigFile;
import net.xantharddev.vulcanlib.Logger;
import net.xantharddev.vulcanlib.command.CommandManager;
import net.xantharddev.vulcanlib.command.FeatureCommand;
import net.xantharddev.vulcanlib.libs.DataUtils;
import net.xantharddev.vulcanlib.libs.SerializableLocation;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import net.vulcandev.vulcancrates.command.CrateCommand;
import net.vulcandev.vulcancrates.listener.CrateInteractListener;
import net.vulcandev.vulcancrates.manager.CrateManager;
import net.vulcandev.vulcancrates.manager.HologramManager;
import net.vulcandev.vulcancrates.manager.PlayerDataManager;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.placeholders.CratePlaceholders;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main plugin class for VulcanCrates.
 * Manages crate functionality including configuration, managers, commands, and persistence.
 */
public final class VulcanCrates extends JavaPlugin {

    private static VulcanCrates instance;
    private final List<FeatureCommand> featureCommands = new ArrayList<>();

    private YamlDocument config;
    private YamlDocument guiConfig;
    @Getter
    private CrateManager crateManager;
    @Getter
    private PlayerDataManager playerDataManager;
    private HologramManager hologramManager;
    private File locationFile;
    private static final Type LOCATIONS_TYPE = new TypeToken<Map<String, SerializableLocation>>(){}.getType();

    public YamlDocument conf() {return config;}
    public YamlDocument guiConf() {return guiConfig;}

    @Override
    public void onEnable() {
        instance = this;

        this.config = ConfigFile.createConfig(this, "config.yml");
        this.guiConfig = ConfigFile.createConfig(this, "gui.yml");
        this.locationFile = new File(getDataFolder(), "crate-locations.json");

        this.crateManager = new CrateManager(this);
        this.playerDataManager = new PlayerDataManager(this, crateManager);
        this.hologramManager = new HologramManager(this);

        crateManager.loadAllCrates();
        loadCrateLocations();
        playerDataManager.initialize();
        crateManager.loadAndRestoreChests();

        registerCommands();
        registerListeners();
        registerPlaceholders();

        Bukkit.getScheduler().runTaskLater(this, () -> hologramManager.loadAllHolograms(), 40L);

        Logger.log(this, "Plugin Loaded Successfully");
    }

    private void registerCommands() {
        featureCommands.add(new CrateCommand(this));
        CommandManager.getInstance().registerCommands("Crates", featureCommands);
    }

    private void registerListeners() {
        CrateInteractListener crateInteractListener = new CrateInteractListener(this);
        Bukkit.getPluginManager().registerEvents(crateInteractListener, this);
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CratePlaceholders(this).register();
        }
    }

    public void onReload() {
        hologramManager.removeAllHolograms();

        CommandManager.getInstance().unregisterAllCommands(featureCommands);
        featureCommands.clear();

        ConfigFile.reloadConfig(config);
        ConfigFile.reloadConfig(guiConfig);
        crateManager.loadAllCrates();
        loadCrateLocations();

        registerCommands();

        Bukkit.getScheduler().runTaskLater(this, () -> hologramManager.loadAllHolograms(), 10L);

        Logger.log(this, "Configuration reloaded.");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) hologramManager.removeAllHolograms();
        if (playerDataManager != null) playerDataManager.shutdown();

        saveCrateLocations();

        CommandManager.getInstance().unregisterAllCommands(featureCommands);

        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        Logger.log(this, "Plugin Shut Down");
    }

    public static VulcanCrates get() {return instance;}

    public void loadCrateLocations() {
        Map<String, SerializableLocation> locations = DataUtils.loadFromJson(locationFile, LOCATIONS_TYPE, ConcurrentHashMap::new);

        if (locations != null) {
            for (Map.Entry<String, SerializableLocation> entry : locations.entrySet()) {
                Crate crate = crateManager.getCrate(entry.getKey());
                if (crate != null) {
                    crate.setLocation(entry.getValue());
                }
            }
        }
    }

    public void saveCrateLocations() {
        Map<String, SerializableLocation> locations = new ConcurrentHashMap<>();

        if (crateManager != null) {
            for (Crate crate : crateManager.getAllCrates()) {
                if (crate.getLocation() != null) {
                    locations.put(crate.getName(), crate.getLocation());
                }
            }
        }

        DataUtils.saveToJson(locationFile, locations, true);
    }

    public void setCrateLocation(String crateName, SerializableLocation location) {
        Crate crate = crateManager.getCrate(crateName);
        if (crate != null) {
            hologramManager.removeHologram(crateName);
            crate.setLocation(location);
            hologramManager.createHologram(crate);
        }
        saveCrateLocations();
    }

    public void removeCrateLocation(String crateName) {
        Crate crate = crateManager.getCrate(crateName);
        if (crate != null) {
            hologramManager.removeHologram(crateName);
            crate.setLocation(null);
        }
        saveCrateLocations();
    }
}