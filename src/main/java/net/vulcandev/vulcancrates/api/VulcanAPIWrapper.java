package net.vulcandev.vulcancrates.api;

import net.vulcandev.vulcanapi.event.VulcanEventManager;
import net.vulcandev.vulcanapi.vulcancrates.events.CrateOpenEvent;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;
import net.xantharddev.vulcanlib.Logger;
import org.bukkit.entity.Player;

/**
 * Wrapper class for VulcanAPI crate events.
 * This class is only loaded when the API is enabled.
 */
public final class VulcanAPIWrapper {
    private VulcanAPIWrapper() {}

    public static void fireCrateOpenEvent(Player player, Crate crate, Prize prize) {
        try {
            CrateOpenEvent event = new CrateOpenEvent(
                    player,
                    new CrateOpenEvent.CrateSnapshot(crate.getName(), crate.getDisplayName()),
                    new CrateOpenEvent.PrizeSnapshot(
                            prize.getName(),
                            prize.getCommands(),
                            prize.getChance(),
                            prize.isAnnounce(),
                            prize.getUrl(),
                            prize.getMaterial(),
                            prize.getAmount(),
                            prize.getData(),
                            prize.getLore(),
                            prize.isGlow()
                    )
            );

            VulcanEventManager.getInstance().callEvent(event);
        } catch (Exception e) {
            Logger.log(VulcanCrates.get(), "&cFailed to fire CrateOpenEvent: " + e.getMessage());
        }
    }
}
