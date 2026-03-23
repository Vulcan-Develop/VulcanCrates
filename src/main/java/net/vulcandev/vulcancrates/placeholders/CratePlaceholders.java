package net.vulcandev.vulcancrates.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.vulcandev.vulcancrates.objects.Crate;
import org.bukkit.entity.Player;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.PlayerData;

/**
 * PlaceholderAPI expansion for crate-related placeholders.
 * Provides placeholders for key counts and statistics.
 * <p>
 * Available placeholders:
 * - %vulcancrates_keys_<cratename>% - Active key count for a specific crate
 * - %vulcancrates_virtual_keys_<cratename>% - Virtual key count for a specific crate
 * - %vulcancrates_physical_keys_<cratename>% - Physical key item count for a specific crate
 * - %vulcancrates_total_keys% - Total active keys across all crates
 * - %vulcancrates_total_virtual_keys% - Total virtual keys
 * - %vulcancrates_total_physical_keys% - Total physical keys
 * - %vulcancrates_key_mode% - The configured key mode
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

        // %vulcancrates_keys_<cratename>% - Get active key count for specific crate
        if (params.startsWith("keys_")) {
            String crateName = params.substring(5);
            Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
            return String.valueOf(crate == null ? 0 : plugin.getKeyManager().getUsableKeys(player, crate));
        }

        // %vulcancrates_virtual_keys_<cratename>% - Get virtual keys for specific crate
        if (params.startsWith("virtual_keys_")) {
            String crateName = params.substring(13);
            return String.valueOf(plugin.getKeyManager().getVirtualKeys(player, crateName));
        }

        // %vulcancrates_physical_keys_<cratename>% - Get physical keys for specific crate
        if (params.startsWith("physical_keys_")) {
            String crateName = params.substring(14);
            return String.valueOf(plugin.getKeyManager().getPhysicalKeys(player, crateName));
        }

        // %vulcancrates_total_keys% - Get total active keys
        if (params.equals("total_keys")) return String.valueOf(plugin.getKeyManager().getTotalUsableKeys(player));

        // %vulcancrates_total_virtual_keys% - Get total virtual keys
        if (params.equals("total_virtual_keys")) return String.valueOf(plugin.getKeyManager().getTotalVirtualKeys(player));

        // %vulcancrates_total_physical_keys% - Get total physical keys
        if (params.equals("total_physical_keys")) return String.valueOf(plugin.getKeyManager().getTotalPhysicalKeys(player));

        // %vulcancrates_key_mode% - Get configured key mode
        if (params.equals("key_mode")) return plugin.getKeyManager().getConfiguredKeyMode().getDisplayName();

        // %vulcancrates_opened% - Get total crates opened
        if (params.equals("opened")) return String.valueOf(playerData.getTotalCratesOpened());

        return null;
    }
}
