package net.vulcandev.vulcancrates.manager;

import com.google.gson.reflect.TypeToken;
import net.xantharddev.vulcanlib.libs.DataUtils;
import org.bukkit.entity.Player;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.PlayerData;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data including crate key counts and statistics.
 */
public class PlayerDataManager {
    private final VulcanCrates plugin;
    private final CrateManager crateManager;
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private File dataFile;

    public PlayerDataManager(VulcanCrates plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
    }

    public void initialize() {
        this.dataFile = new File(plugin.getDataFolder(), "data/players.json");
        loadPlayerData();
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public void saveAllData() {
        DataUtils.saveToJson(dataFile, playerData, true);
    }

    private void loadPlayerData() {
        if (!dataFile.exists()) {
            return;
        }
        
        Type mapType = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
        Map<UUID, PlayerData> loadedData = DataUtils.loadFromJson(dataFile, mapType, HashMap::new);

        if (loadedData != null) {
            playerData.putAll(loadedData);

            for (PlayerData data : playerData.values()) {
                for (String crateName : crateManager.getCrateNames()) {
                    if (!data.hasKeys(crateName)) {
                        data.setKeys(crateName, 0);
                    }
                }
            }
        }
    }

    public void shutdown() {
        saveAllData();
    }
}