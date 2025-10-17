package net.vulcandev.vulcancrates.objects;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores player-specific crate data including key counts and statistics.
 * This data is persisted to disk and loaded on server startup.
 */
public class PlayerData {
    private final Map<String, Integer> crateKeys = new HashMap<>();
    @Setter @Getter
    private int totalCratesOpened = 0;
    
    public Map<String, Integer> getCrateKeys() {return Collections.unmodifiableMap(crateKeys);}

    /**
     * Gets the number of keys the player has for a specific crate.
     * @param crateName the name of the crate
     * @return the number of keys, or 0 if none
     */
    public int getKeys(String crateName) {
        return crateKeys.getOrDefault(crateName, 0);
    }

    /**
     * Sets the number of keys for a specific crate.
     * The amount cannot be negative.
     * @param crateName the name of the crate
     * @param amount the number of keys to set
     */
    public void setKeys(String crateName, int amount) {
        crateKeys.put(crateName, Math.max(0, amount));
    }

    /**
     * Adds keys to the player's count for a specific crate.
     * @param crateName the name of the crate
     * @param amount the number of keys to add (can be negative to remove)
     */
    public void addKeys(String crateName, int amount) {
        int current = getKeys(crateName);
        setKeys(crateName, current + amount);
    }

    /**
     * Checks if the player has any entry for the specified crate.
     * @param crateName the name of the crate
     * @return true if the player has an entry for this crate
     */
    public boolean hasKeys(String crateName) {
        return crateKeys.containsKey(crateName);
    }

    /**
     * Checks if the player has at least one key for the specified crate.
     * @param crateName the name of the crate
     * @return true if the player has at least one key
     */
    public boolean canOpenCrate(String crateName) {
        return getKeys(crateName) > 0;
    }

    /**
     * Uses one key for the specified crate and increments the total opened count.
     * Only uses a key if the player has at least one.
     * @param crateName the name of the crate
     */
    public void useKey(String crateName) {
        if (canOpenCrate(crateName)) {
            addKeys(crateName, -1);
            totalCratesOpened++;
        }
    }

    /**
     * Calculates the total number of keys across all crate types.
     * @return the sum of all keys
     */
    public int getTotalKeys() {
        return crateKeys.values().stream().mapToInt(Integer::intValue).sum();
    }
}