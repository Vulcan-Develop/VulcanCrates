package net.vulcandev.vulcancrates.manager;

import com.google.gson.reflect.TypeToken;
import net.xantharddev.vulcanlib.async.VulcanTasks;
import net.xantharddev.vulcanlib.libs.DataUtils;
import org.bukkit.Bukkit;
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
    private static final long AUTOSAVE_INTERVAL_TICKS = 20L * 60L * 5L;

    private final VulcanCrates plugin;
    private final CrateManager crateManager;
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private final Object writeLock = new Object();
    private File dataFile;
    private long queuedSaves;
    private long latestWrittenSave;
    private boolean saveScheduled;

    public PlayerDataManager(VulcanCrates plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
    }

    public void initialize() {
        this.dataFile = new File(plugin.getDataFolder(), "data/players.json");

        File dataFolder = dataFile.getParentFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadPlayerData();
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAllDataAsync, AUTOSAVE_INTERVAL_TICKS, AUTOSAVE_INTERVAL_TICKS);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public void saveAllData() {
        write(snapshot(), ++queuedSaves);
    }

    public void saveAllDataAsync() {
        Map<UUID, PlayerData> snapshot = snapshot();
        long save = ++queuedSaves;
        VulcanTasks.async(plugin, () -> write(snapshot, save));
    }

    // A used virtual key must be on disk before a crash can bring it back after its prize was paid; one save a second at most.
    public void saveSoon() {
        if (saveScheduled) return;
        saveScheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            saveAllDataAsync();
        }, 20L);
    }

    private Map<UUID, PlayerData> snapshot() {
        Map<UUID, PlayerData> snapshot = new HashMap<>(playerData.size());
        for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().copy());
        }
        return snapshot;
    }

    private void write(Map<UUID, PlayerData> snapshot, long save) {
        synchronized (writeLock) {
            if (save < latestWrittenSave) return;
            latestWrittenSave = save;
            DataUtils.saveToJson(dataFile, snapshot, false);
        }
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
