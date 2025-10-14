package net.vulcandev.vulcancrates.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;

/**
 * Event called when a player opens a crate and receives a prize.
 * This event is called after the prize is determined but before commands are executed.
 */
public class CrateOpenEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    /** The player who opened the crate */
    private final Player player;

    /** The crate that was opened */
    private final Crate crate;

    /** The prize that was won */
    private final Prize prize;

    /**
     * Creates a new crate open event.
     * @param player the player opening the crate
     * @param crate the crate being opened
     * @param prize the prize that was won
     */
    public CrateOpenEvent(Player player, Crate crate, Prize prize) {
        this.player = player;
        this.crate = crate;
        this.prize = prize;
    }

    /**
     * Gets the handler list (required by Bukkit).
     * @return the static handler list
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public Prize getPrize() {
        return prize;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

}