package net.vulcandev.vulcancrates.manager;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.xantharddev.vulcanlib.Logger;
import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.Location;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages hologram creation and removal for crate locations.
 * Uses the DecentHolograms API to display floating text above crates.
 */
public class HologramManager {
    private final VulcanCrates plugin;

    /** Map of crate names to their hologram objects */
    private final Map<String, Hologram> crateHolograms = new ConcurrentHashMap<>();

    public HologramManager(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a hologram for a crate at its location.
     * Removes any existing hologram before creating a new one.
     * @param crate the crate to create a hologram for
     */
    public void createHologram(Crate crate) {
        if (crate.getLocation() == null) return;

        Location bukkitLocation = crate.getLocation().getLocation();
        if (bukkitLocation == null || bukkitLocation.getWorld() == null) return;

        // Remove existing hologram if it exists
        removeHologram(crate.getName());

        // Get hologram configuration for this crate
        List<String> hologramLines = crate.getHologramLines();
        if (hologramLines == null || hologramLines.isEmpty()) return;

        double yOffset = crate.getHologramYOffset();

        // Process color codes in hologram lines
        List<String> processedLines = new ArrayList<>();
        for (String line : hologramLines) {
            processedLines.add(Colour.colour(line));
        }

        // Calculate hologram location (center of block + y offset)
        Location hologramLocation = bukkitLocation.clone().add(0.5, yOffset, 0.5);

        // Create hologram ID
        String hologramId = "crate_" + crate.getName().toLowerCase();

        try {
            // Create the hologram using Decent Holograms API
            Hologram hologram = DHAPI.createHologram(hologramId, hologramLocation, processedLines);

            // Configure hologram settings
            hologram.setDisplayRange(16);
            hologram.setUpdateRange(16);
            hologram.setUpdateInterval(20);

            // Store the hologram reference
            crateHolograms.put(crate.getName(), hologram);

            Logger.log(plugin, "Created hologram for crate: " + crate.getName());
        } catch (Exception e) {
            Logger.log(plugin, "Failed to create hologram for crate: " + crate.getName() + " - " + e.getMessage());
        }
    }

    /**
     * Removes the hologram for a crate.
     * @param crateName the name of the crate whose hologram to remove
     */
    public void removeHologram(String crateName) {
        Hologram hologram = crateHolograms.get(crateName);
        if (hologram != null) {
            try {
                DHAPI.removeHologram(hologram.getName());
                crateHolograms.remove(crateName);
                Logger.log(plugin, "Removed hologram for crate: " + crateName);
            } catch (Exception e) {
                Logger.log(plugin, "Failed to remove hologram for crate: " + crateName + " - " + e.getMessage());
            }
        }
    }

    /**
     * Loads holograms for all crates that have a location set.
     * Called during plugin startup and reload.
     */
    public void loadAllHolograms() {
        Logger.log(plugin, "Loading holograms for all placed crates...");

        for (Crate crate : plugin.getCrateManager().getAllCrates()) {
            if (crate.getLocation() != null) {
                createHologram(crate);
            }
        }
    }

    /**
     * Removes all crate holograms.
     * Called during plugin shutdown and reload.
     */
    public void removeAllHolograms() {
        Logger.log(plugin, "Removing all crate holograms...");

        for (String crateName : crateHolograms.keySet()) {
            removeHologram(crateName);
        }
        crateHolograms.clear();
    }
}