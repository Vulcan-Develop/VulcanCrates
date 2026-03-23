package net.vulcandev.vulcancrates.listener;

import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.SerializableLocation;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.event.CrateOpenEvent;
import net.vulcandev.vulcancrates.gui.CratePreviewGUI;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;

/**
 * Listens for player interactions with crate blocks.
 * Left-click opens preview GUI, right-click attempts to open the crate.
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
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            new CratePreviewGUI(plugin, player, crate).open();
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!plugin.getKeyManager().canOpenCrate(player, crate)) {
                String message = plugin.conf().getString("messages.no-keys",
                                "%prefix% &7You need a %crateType% %keyType% key to open this crate.")
                        .replace("%prefix%", plugin.conf().getString("messages.prefix"))
                        .replace("%crateName%", crate.getName())
                        .replace("%crateType%", crate.getDisplayName())
                        .replace("%keyMode%", plugin.getKeyManager().getConfiguredKeyMode().getDisplayName())
                        .replace("%keyType%", plugin.getKeyManager().getKeyTypeLabel(plugin.getKeyManager().getConfiguredKeyMode()));
                player.sendMessage(Colour.colour(message));
                return;
            }
            openCrate(player, crate);
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

    private void openCrate(Player player, Crate crate) {
        Prize prize = crate.getRandomPrize();
        if (prize == null) {
            player.sendMessage(Colour.colour("&cError: No prizes configured for this crate!"));
            return;
        }

        if (!plugin.getKeyManager().useKey(player, crate)) {
            String message = plugin.conf().getString("messages.no-keys",
                            "%prefix% &7You need a %crateType% %keyType% key to open this crate.")
                    .replace("%prefix%", plugin.conf().getString("messages.prefix"))
                    .replace("%crateName%", crate.getName())
                    .replace("%crateType%", crate.getDisplayName())
                    .replace("%keyMode%", plugin.getKeyManager().getConfiguredKeyMode().getDisplayName())
                    .replace("%keyType%", plugin.getKeyManager().getKeyTypeLabel(plugin.getKeyManager().getConfiguredKeyMode()));
            player.sendMessage(Colour.colour(message));
            return;
        }

        CrateOpenEvent event = new CrateOpenEvent(player, crate, prize);
        plugin.getServer().getPluginManager().callEvent(event);

        if (prize.getCommands() != null) {
            for (String command : prize.getCommands()) {
                String processedCommand = command.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
            }
        }

        String playerMessage = plugin.conf().getString("messages.crate-rolled-reward")
                .replace("%prefix%", plugin.conf().getString("messages.prefix"))
                .replace("%prizeName%", prize.getName())
                .replace("%crateType%", crate.getDisplayName());
        player.sendMessage(Colour.colour(playerMessage));

        if (prize.isAnnounce()) {
            String announceMessage = plugin.conf().getString("messages.crate-rolled-reward-announce")
                    .replace("%prefix%", plugin.conf().getString("messages.prefix"))
                    .replace("%player%", player.getName())
                    .replace("%prizeName%", prize.getName())
                    .replace("%crateType%", crate.getDisplayName());
            plugin.getServer().broadcastMessage(Colour.colour(announceMessage));
        }

        playEffects(player, crate);
    }

    private void playEffects(Player player, Crate crate) {
        if (crate.getLocation() == null) return;
        Location loc = crate.getLocation().getLocation();
        if (loc == null || loc.getWorld() == null) return;

        // Center the location on the block
        Location center = loc.clone().add(0.5, 0.5, 0.5);

        if (crate.getOpenSound() != null && !crate.getOpenSound().isEmpty()) {
            try {
                Sound sound = Sound.valueOf(crate.getOpenSound().toUpperCase());
                loc.getWorld().playSound(center, sound, crate.getOpenSoundVolume(), crate.getOpenSoundPitch());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound '" + crate.getOpenSound() + "' in crate '" + crate.getName() + "'");
            }
        }

        if (crate.getOpenParticle() != null && !crate.getOpenParticle().isEmpty()) {
            try {
                Particle particle = Particle.valueOf(crate.getOpenParticle().toUpperCase());
                loc.getWorld().spawnParticle(
                        particle,
                        center,
                        crate.getOpenParticleCount(),
                        crate.getOpenParticleOffsetX(),
                        crate.getOpenParticleOffsetY(),
                        crate.getOpenParticleOffsetZ(),
                        crate.getOpenParticleSpeed()
                );
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle '" + crate.getOpenParticle() + "' in crate '" + crate.getName() + "'");
            }
        }
    }
}
