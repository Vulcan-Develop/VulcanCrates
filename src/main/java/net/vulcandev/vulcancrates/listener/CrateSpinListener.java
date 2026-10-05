package net.vulcandev.vulcancrates.listener;

import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.gui.CrateSpinGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CrateSpinListener implements Listener {

    private final VulcanCrates plugin;

    public CrateSpinListener(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpinInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        CrateSpinGUI gui = plugin.getCrateOpenManager().getSession(player);
        if (gui == null || event.getInventory().getHolder() != gui) return;

        plugin.getCrateOpenManager().finish(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpinnerQuit(PlayerQuitEvent event) {
        plugin.getCrateOpenManager().finish(event.getPlayer());
    }
}
