package net.vulcandev.vulcancrates.manager;

import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.api.VulcanAPIWrapper;
import net.vulcandev.vulcancrates.gui.CrateSpinGUI;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;
import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.SoundUtil;
import net.xantharddev.vulcanlib.libs.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CrateOpenManager {

    private final VulcanCrates plugin;
    private final Map<UUID, CrateSpinGUI> sessions = new HashMap<>();

    public CrateOpenManager(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    public boolean isSpinEnabled() {
        return plugin.conf().getBoolean("spin.enabled", true);
    }

    public int getKeysPerWindow() {
        int rowLimit = Math.max(1, plugin.conf().getInt("spin.max-rows", 1));
        return Math.max(1, plugin.conf().getInt("spin.keys-per-window", rowLimit));
    }

    public CrateSpinGUI getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void open(Player player, Crate crate) {
        if (crate.getPrizes() == null || crate.getPrizes().isEmpty()) {
            message(player, "messages.no-prizes", "%prefix% &cThis crate has no prizes configured.", crate);
            return;
        }

        if (sessions.containsKey(player.getUniqueId())) {
            message(player, "messages.already-opening", "%prefix% &cFinish your current crate before opening another.", crate);
            return;
        }

        int usableKeys = plugin.getKeyManager().getUsableKeys(player, crate);
        if (usableKeys < 1) {
            sendNotEnoughKeys(player, crate, 1, usableKeys);
            return;
        }

        if (!isSpinEnabled()) {
            openInstantly(player, crate);
            return;
        }

        CrateSpinGUI gui = new CrateSpinGUI(plugin, player, crate);
        sessions.put(player.getUniqueId(), gui);
        gui.open();
        if (player.getOpenInventory().getTopInventory().getHolder() != gui) {
            sessions.remove(player.getUniqueId());
        }
    }

    public boolean beginSpin(Player player, Crate crate, int selectedRows) {
        int requiredKeys = Math.max(1, selectedRows);
        int usableKeys = plugin.getKeyManager().getUsableKeys(player, crate);
        if (usableKeys < requiredKeys || !plugin.getKeyManager().useKeys(player, crate, requiredKeys)) {
            sendNotEnoughKeys(player, crate, requiredKeys, usableKeys);
            return false;
        }

        playOpenEffects(crate);
        return true;
    }

    public void finish(Player player) {
        CrateSpinGUI gui = sessions.remove(player.getUniqueId());
        if (gui != null) gui.abort();
    }

    public void finishAll() {
        List<CrateSpinGUI> open = new ArrayList<>(sessions.values());
        sessions.clear();
        for (CrateSpinGUI gui : open) {
            gui.abort();
            gui.closeView();
        }
    }

    public void award(Player player, Crate crate, Prize prize) {
        if (prize == null) return;

        if (plugin.shouldUseAPI()) {
            VulcanAPIWrapper.fireCrateOpenEvent(player, crate, prize);
        }

        if (prize.getCommands() != null) {
            for (String command : prize.getCommands()) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                        command.replace("%player%", player.getName()));
            }
        }

        player.sendMessage(Colour.colour(prizeMessage("messages.crate-rolled-reward",
                "%prefix% &7You won &6%prizeName%&7!", player, crate, prize)));

        if (prize.isAnnounce()) {
            plugin.getServer().broadcastMessage(Colour.colour(prizeMessage("messages.crate-rolled-reward-announce",
                    "%prefix% &r%player% &7just rolled a &6%prizeName% &7from a %crateType%&7!", player, crate, prize)));
        }
    }

    public void sendSelectPrompt(Player player, int spins) {
        String template = plugin.conf().getString("messages.spin-select-prompt", "");
        if (template == null || template.trim().isEmpty()) return;

        player.sendMessage(Colour.colour(template
                .replace("%prefix%", prefix())
                .replace("%rows%", String.valueOf(spins))
                .replace("%spins%", String.valueOf(spins))));
    }

    public void sendSelectRequired(Player player, Crate crate) {
        message(player, "messages.spin-select-required", "%prefix% &7Select at least one row first.", crate);
    }

    public void sendRowLimit(Player player, Crate crate, int limit) {
        String template = plugin.conf().getString("messages.spin-row-limit",
                "%prefix% &7You can select up to &6%limit% &7row(s).");
        if (template == null || template.trim().isEmpty()) return;

        player.sendMessage(Colour.colour(template
                .replace("%prefix%", prefix())
                .replace("%crateName%", crate.getName())
                .replace("%crateType%", crate.getDisplayName())
                .replace("%limit%", String.valueOf(limit))));
    }

    public void playOpenEffects(Crate crate) {
        if (crate.getLocation() == null) return;

        Location loc = crate.getLocation().getLocation();
        if (loc == null || loc.getWorld() == null) return;

        Location center = loc.clone().add(0.5, 0.5, 0.5);

        if (crate.getOpenSound() != null && !crate.getOpenSound().isEmpty()) {
            SoundUtil.play(center, crate.getOpenSound(), crate.getOpenSoundVolume(), crate.getOpenSoundPitch());
        }

        if (crate.getOpenParticle() != null && !crate.getOpenParticle().isEmpty()) {
            Utils.spawnParticle(
                    center,
                    crate.getOpenParticle(),
                    crate.getOpenParticleCount(),
                    crate.getOpenParticleOffsetX(),
                    crate.getOpenParticleOffsetY(),
                    crate.getOpenParticleOffsetZ(),
                    crate.getOpenParticleSpeed()
            );
        }
    }

    public void sendNoKeys(Player player, Crate crate) {
        KeyManager keyManager = plugin.getKeyManager();
        String message = plugin.conf().getString("messages.no-keys",
                        "%prefix% &7You need a %crateType% %keyType% key to open this crate.")
                .replace("%prefix%", prefix())
                .replace("%crateName%", crate.getName())
                .replace("%crateType%", crate.getDisplayName())
                .replace("%keyMode%", keyManager.getConfiguredKeyMode().getDisplayName())
                .replace("%keyType%", keyManager.getKeyTypeLabel(keyManager.getConfiguredKeyMode()));
        player.sendMessage(Colour.colour(message));
    }

    public void sendNotEnoughKeys(Player player, Crate crate, int required, int available) {
        KeyManager keyManager = plugin.getKeyManager();
        String message = plugin.conf().getString("messages.not-enough-keys",
                        "%prefix% &7You need &6%required% &7%crateType% %keyType% key(s), but only have &6%available%&7.")
                .replace("%prefix%", prefix())
                .replace("%crateName%", crate.getName())
                .replace("%crateType%", crate.getDisplayName())
                .replace("%keyMode%", keyManager.getConfiguredKeyMode().getDisplayName())
                .replace("%keyType%", keyManager.getKeyTypeLabel(keyManager.getConfiguredKeyMode()))
                .replace("%required%", String.valueOf(required))
                .replace("%available%", String.valueOf(available));
        player.sendMessage(Colour.colour(message));
    }

    private void openInstantly(Player player, Crate crate) {
        if (!plugin.getKeyManager().useKey(player, crate)) {
            sendNoKeys(player, crate);
            return;
        }

        award(player, crate, crate.getRandomPrize());
        playOpenEffects(crate);
    }

    private void message(Player player, String path, String fallback, Crate crate) {
        String template = plugin.conf().getString(path, fallback);
        if (template == null || template.trim().isEmpty()) return;

        player.sendMessage(Colour.colour(template
                .replace("%prefix%", prefix())
                .replace("%crateName%", crate.getName())
                .replace("%crateType%", crate.getDisplayName())));
    }

    private String prizeMessage(String path, String fallback, Player player, Crate crate, Prize prize) {
        return plugin.conf().getString(path, fallback)
                .replace("%prefix%", prefix())
                .replace("%player%", player.getName())
                .replace("%prizeName%", prize.getName())
                .replace("%crateName%", crate.getName())
                .replace("%crateType%", crate.getDisplayName());
    }

    private String prefix() {
        return plugin.conf().getString("messages.prefix", "&6[Crates]");
    }
}
