package net.vulcandev.vulcancrates.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;

/**
 * Event called when a player opens a crate and receives a prize.
 * This event is called after the prize is determined but before commands are executed.
 */
@Getter
public class CrateOpenEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Crate crate;
    private final Prize prize;
    public CrateOpenEvent(Player player, Crate crate, Prize prize) {
        this.player = player;
        this.crate = crate;
        this.prize = prize;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

}