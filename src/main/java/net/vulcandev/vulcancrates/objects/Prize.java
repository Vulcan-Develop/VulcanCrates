package net.vulcandev.vulcancrates.objects;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.List;

/**
 * Represents a prize that can be won from a crate.
 * Contains reward commands, display settings, and probability configuration.
 */
@Setter
@Getter
public class Prize {
    private String name;
    private List<String> commands;
    private double chance;
    private boolean announce;
    private String url;
    private Material material;
    private int amount;
    private byte data;
    private List<String> lore;
    private boolean glow;

    public Prize() {
        this.chance = 1.0;
        this.announce = false;
        this.material = Material.STONE;
        this.amount = 1;
        this.data = 0;
        this.glow = false;
    }

}