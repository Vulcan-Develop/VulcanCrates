package net.vulcandev.vulcancrates.objects;

import lombok.Getter;
import lombok.Setter;
import net.xantharddev.vulcanlib.libs.SerializableLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a crate with prizes, location, and hologram configuration.
 * Crates can be placed in the world and opened by players with keys.
 */
@Setter
@Getter
public class Crate {
    private String name;
    private String displayName;
    private Map<String, Prize> prizes;
    private SerializableLocation location;
    private List<String> hologramLines;
    private double hologramYOffset;
    private String material;
    private String customModelData;
    private String keyMaterial;
    private String keyName;
    private List<String> keyLore;
    private boolean keyGlowing;
    private Integer keyCustomModelData;
    private String keyUrl;
    private String keyOwner;
    private short keyDamage;
    private boolean keyUnbreakable;

    // Effects
    private String openSound;
    private float openSoundVolume;
    private float openSoundPitch;
    private String openParticle;
    private int openParticleCount;
    private double openParticleSpeed;
    private double openParticleOffsetX;
    private double openParticleOffsetY;
    private double openParticleOffsetZ;

    public Crate(String name) {
        this.name = name;
        this.prizes = new LinkedHashMap<>();
    }

    public Prize getRandomPrize() {
        if (prizes == null || prizes.isEmpty()) return null;

        double totalChance = prizes.values().stream()
                .mapToDouble(Prize::getChance)
                .sum();

        if (totalChance <= 0) return prizes.values().iterator().next();

        double random = Math.random() * totalChance;
        double currentChance = 0;

        for (Prize prize : prizes.values()) {
            currentChance += prize.getChance();
            if (random < currentChance) {
                return prize;
            }
        }

        return prizes.values().iterator().next();
    }
}
