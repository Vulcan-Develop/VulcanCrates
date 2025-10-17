package net.vulcandev.vulcancrates.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.PlayerData;

/**
 * PlaceholderAPI expansion for crate-related placeholders.
 * Provides placeholders for key counts and statistics.
 * <p>
 * Available placeholders:
 * - %vulcancrates_keys_<cratename>% - Number of keys for a specific crate
 * - %vulcancrates_total_keys% - Total keys across all crates
 * - %vulcancrates_opened% - Total number of crates opened
 */
public class CratePlaceholders extends PlaceholderExpansion {
    private final VulcanCrates plugin;
    public CratePlaceholders(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }
    @Override
    public String getIdentifier() {
        return "vulcancrates";
    }
    @Override
    public String getAuthor() {return plugin.getDescription().getAuthors().toString();}
    @Override
    public String getVersion() {return plugin.getDescription().getVersion();}

    /**
     * Handles placeholder requests.
     * @param player the player for whom the placeholder is being requested
     * @param params the placeholder parameters
     * @return the placeholder value, or null if not recognized
     */
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        // %vulcancrates_keys_<cratename>% - Get keys for specific crate
        if (params.startsWith("keys_")) {
            String crateName = params.substring(5);
            return String.valueOf(playerData.getKeys(crateName));
        }

        // %vulcancrates_total_keys% - Get total keys
        if (params.equals("total_keys")) return String.valueOf(playerData.getTotalKeys());

        // %vulcancrates_opened% - Get total crates opened
        if (params.equals("opened")) return String.valueOf(playerData.getTotalCratesOpened());

        return null;
    }
}