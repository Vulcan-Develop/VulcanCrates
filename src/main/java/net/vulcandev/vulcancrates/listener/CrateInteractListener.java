package net.vulcandev.vulcancrates.listener;

import net.xantharddev.vulcanlib.VulcanLib;
import net.xantharddev.vulcanlib.libs.SerializableLocation;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.gui.CratePreviewGUI;
import net.vulcandev.vulcancrates.objects.Crate;

/**
 * Listens for player interactions with crate blocks.
 * Left-click opens preview GUI and right-click opens the row selection menu.
 */
public class CrateInteractListener implements Listener {

    private final VulcanCrates plugin;

    public CrateInteractListener(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrateInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        Player player = event.getPlayer();
        SerializableLocation location = new SerializableLocation(block.getLocation());
        
        // Find crate at this location
        Crate crate = findCrateAtLocation(location);
        if (crate == null) return;
        if(block.getType() != MaterialDb.get(crate.getMaterial())) return;

        event.setCancelled(true);
        if (!VulcanLib.getInstance().isLegacy() && event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            new CratePreviewGUI(plugin, player, crate).open();
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getCrateOpenManager().open(player, crate);
        }
    }

    private Crate findCrateAtLocation(SerializableLocation location) {
        for (Crate crate : plugin.getCrateManager().getAllCrates()) {
            if (crate.getLocation() != null && crate.getLocation().equals(location)) {
                return crate;
            }
        }

        return null;
    }
}
