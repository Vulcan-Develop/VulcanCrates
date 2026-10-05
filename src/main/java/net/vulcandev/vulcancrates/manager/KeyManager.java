package net.vulcandev.vulcancrates.manager;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.KeyMode;
import net.vulcandev.vulcancrates.objects.PlayerData;
import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.SimpleItem;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles crate key behavior across virtual balances and physical key items.
 */
public class KeyManager {
    private static final String LEGACY_KEY_MARKER_PREFIX = "§0VC_KEY:";
    private static final String NBT_CRATE_KEY = "vc_crate_key";
    private final VulcanCrates plugin;

    public KeyManager(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    public KeyMode getConfiguredKeyMode() {
        return KeyMode.fromConfig(plugin.conf().getString("keys.mode", "VIRTUAL"));
    }

    public KeyMode getConfiguredGiveMode() {
        KeyMode fallbackMode = getConfiguredKeyMode() == KeyMode.PHYSICAL ? KeyMode.PHYSICAL : KeyMode.VIRTUAL;
        return normalizeBinaryMode(KeyMode.fromConfig(
                plugin.conf().getString("keys.default-give-mode", fallbackMode.name())),
                fallbackMode
        );
    }

    public KeyMode getConsumePriorityMode() {
        return normalizeBinaryMode(KeyMode.fromConfig(
                plugin.conf().getString("keys.consume-priority", "PHYSICAL")),
                KeyMode.PHYSICAL
        );
    }

    public String getKeyTypeLabel(KeyMode keyMode) {
        switch (keyMode) {
            case PHYSICAL:
                return "physical";
            case BOTH:
                return "physical or virtual";
            default:
                return "virtual";
        }
    }

    public int getUsableKeys(Player player, Crate crate) {
        if (crate == null) {
            return 0;
        }

        switch (getConfiguredKeyMode()) {
            case PHYSICAL:
                return getPhysicalKeys(player, crate);
            case BOTH:
                return getPhysicalKeys(player, crate) + getVirtualKeys(player, crate);
            default:
                return getVirtualKeys(player, crate);
        }
    }

    public int getTotalUsableKeys(Player player) {
        switch (getConfiguredKeyMode()) {
            case PHYSICAL:
                return getTotalPhysicalKeys(player);
            case BOTH:
                return getTotalPhysicalKeys(player) + getTotalVirtualKeys(player);
            default:
                return getTotalVirtualKeys(player);
        }
    }

    public int getVirtualKeys(Player player, Crate crate) {
        if (crate == null) {
            return 0;
        }
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        return playerData.getKeys(crate.getName());
    }

    public int getVirtualKeys(Player player, String crateName) {
        Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
        return crate == null ? 0 : getVirtualKeys(player, crate);
    }

    public int getTotalVirtualKeys(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        return playerData.getTotalKeys();
    }

    public int getPhysicalKeys(Player player, Crate crate) {
        if (crate == null) {
            return 0;
        }
        return getPhysicalKeys(player, crate.getName());
    }

    public int getPhysicalKeys(Player player, String crateName) {
        if (crateName == null || crateName.trim().isEmpty()) {
            return 0;
        }

        String normalizedCrateName = crateName.toLowerCase(Locale.ROOT);
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            String taggedCrateName = getTaggedCrateName(item);
            if (taggedCrateName != null && taggedCrateName.equals(normalizedCrateName)) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    public int getTotalPhysicalKeys(Player player) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (getTaggedCrateName(item) != null) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    public boolean canOpenCrate(Player player, Crate crate) {
        return canOpenCrate(player, crate, 1);
    }

    public boolean canOpenCrate(Player player, Crate crate, int amount) {
        return amount > 0 && getUsableKeys(player, crate) >= amount;
    }

    public boolean useKey(Player player, Crate crate) {
        return useKeys(player, crate, 1);
    }

    public boolean useKeys(Player player, Crate crate, int amount) {
        if (!canOpenCrate(player, crate, amount)) {
            return false;
        }

        KeyMode keyMode = getConfiguredKeyMode();
        int physicalAmount = 0;
        int virtualAmount = 0;
        if (keyMode == KeyMode.PHYSICAL) {
            physicalAmount = amount;
        } else if (keyMode == KeyMode.VIRTUAL) {
            virtualAmount = amount;
        } else if (getConsumePriorityMode() == KeyMode.PHYSICAL) {
            physicalAmount = Math.min(amount, getPhysicalKeys(player, crate));
            virtualAmount = amount - physicalAmount;
        } else {
            virtualAmount = Math.min(amount, getVirtualKeys(player, crate));
            physicalAmount = amount - virtualAmount;
        }

        consumePhysicalKeys(player, crate, physicalAmount);
        consumeVirtualKeys(player, crate, virtualAmount);

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        playerData.setTotalCratesOpened(playerData.getTotalCratesOpened() + amount);
        return true;
    }

    public KeyGrantResult giveConfiguredKeys(Player player, Crate crate, int amount) {
        return giveKeys(player, crate, amount, getConfiguredGiveMode());
    }

    public KeyGrantResult giveKeys(Player player, Crate crate, int amount, KeyMode keyMode) {
        keyMode = normalizeBinaryMode(keyMode, getConfiguredGiveMode());
        if (keyMode == KeyMode.PHYSICAL) {
            return givePhysicalKeys(player, crate, amount);
        }
        return giveVirtualKeys(player, crate, amount);
    }

    public ItemStack createPhysicalKey(Crate crate, int amount) {
        Material material = MaterialDb.get(crate.getKeyMaterial(), Material.TRIPWIRE_HOOK);
        List<String> lore = new ArrayList<>(colourize(parseTemplates(getKeyLore(crate), crate)));

        SimpleItem.Builder builder = SimpleItem.builder()
                .setMaterial(material)
                .setAmount(Math.max(1, amount))
                .setDamage(crate.getKeyDamage())
                .setName(Colour.colour(parseTemplate(getKeyDisplayName(crate), crate)))
                .setLore(lore)
                .setGlowing(crate.isKeyGlowing())
                .setUnbreakable(crate.isKeyUnbreakable());

        if (crate.getKeyCustomModelData() != null) {
            builder.setCustomModelData(crate.getKeyCustomModelData());
        }
        if (crate.getKeyOwner() != null && !crate.getKeyOwner().trim().isEmpty()) {
            builder.setOwner(crate.getKeyOwner());
        }
        if (crate.getKeyUrl() != null && !crate.getKeyUrl().trim().isEmpty()) {
            builder.setUrl(crate.getKeyUrl());
        }

        ItemStack item = builder.build().get();

        NBT.modify(item, (Consumer<ReadWriteItemNBT>) nbt -> nbt.setString(NBT_CRATE_KEY, crate.getName().toLowerCase(Locale.ROOT)));
        return item;
    }

    public boolean isPhysicalKey(ItemStack item, Crate crate) {
        String taggedCrateName = getTaggedCrateName(item);
        return taggedCrateName != null && taggedCrateName.equals(crate.getName().toLowerCase(Locale.ROOT));
    }

    private KeyGrantResult giveVirtualKeys(Player player, Crate crate, int amount) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        playerData.addKeys(crate.getName(), amount);
        return new KeyGrantResult(KeyMode.VIRTUAL, amount, playerData.getKeys(crate.getName()), 0);
    }

    private void consumeVirtualKeys(Player player, Crate crate, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        playerData.addKeys(crate.getName(), -amount);
    }

    private KeyGrantResult givePhysicalKeys(Player player, Crate crate, int amount) {
        if (amount <= 0) {
            return new KeyGrantResult(KeyMode.PHYSICAL, 0, getPhysicalKeys(player, crate), 0);
        }

        int remaining = amount;
        int droppedAmount = 0;
        PlayerInventory inventory = player.getInventory();

        while (remaining > 0) {
            ItemStack template = createPhysicalKey(crate, 1);
            int stackSize = Math.min(template.getMaxStackSize(), remaining);
            template.setAmount(stackSize);

            Map<Integer, ItemStack> leftovers = inventory.addItem(template);
            if (!leftovers.isEmpty()) {
                for (ItemStack leftover : leftovers.values()) {
                    droppedAmount += leftover.getAmount();
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }

            remaining -= stackSize;
        }

        return new KeyGrantResult(KeyMode.PHYSICAL, amount, getPhysicalKeys(player, crate), droppedAmount);
    }

    private void consumePhysicalKeys(Player player, Crate crate, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        int heldSlot = inventory.getHeldItemSlot();
        ItemStack mainHand = inventory.getItem(heldSlot);
        if (isPhysicalKey(mainHand, crate)) {
            assert mainHand != null;
            amount = decreaseStack(inventory, heldSlot, mainHand, amount);
        }

        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            if (i == heldSlot) {
                continue;
            }

            ItemStack item = contents[i];
            if (!isPhysicalKey(item, crate)) {
                continue;
            }

            amount = decreaseStack(inventory, i, item, amount);
        }
    }

    private int decreaseStack(PlayerInventory inventory, int slot, ItemStack item, int amount) {
        int consumed = Math.min(item.getAmount(), amount);
        int updatedAmount = item.getAmount() - consumed;
        inventory.setItem(slot, updatedAmount <= 0 ? null : withAmount(item, updatedAmount));
        return amount - consumed;
    }

    private ItemStack withAmount(ItemStack item, int amount) {
        ItemStack cloned = item.clone();
        cloned.setAmount(amount);
        return cloned;
    }

    private String getTaggedCrateName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }

        // Check NBT tag first (new keys)
        String nbtValue = NBT.get(item, (Function<ReadableItemNBT, String>) nbt -> nbt.getString(NBT_CRATE_KEY));
        if (nbtValue != null && !nbtValue.isEmpty()) {
            return nbtValue;
        }

        // Fall back to legacy lore tag for keys given before this update
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null || !itemMeta.hasLore()) {
            return null;
        }

        List<String> lore = itemMeta.getLore();
        if (lore == null || lore.isEmpty()) {
            return null;
        }

        for (String line : lore) {
            if (line == null || !line.startsWith(LEGACY_KEY_MARKER_PREFIX)) {
                continue;
            }
            return line.substring(LEGACY_KEY_MARKER_PREFIX.length()).toLowerCase(Locale.ROOT);
        }

        return null;
    }

    private String getKeyDisplayName(Crate crate) {
        if (crate.getKeyName() != null && !crate.getKeyName().trim().isEmpty()) {
            return crate.getKeyName();
        }

        String displayName = crate.getDisplayName() != null ? crate.getDisplayName() : crate.getName();
        if (displayName.toLowerCase(Locale.ROOT).contains("crate")) {
            return displayName.replace("Crate", "Key").replace("crate", "key");
        }
        return displayName + " &7Key";
    }

    private List<String> getKeyLore(Crate crate) {
        if (crate.getKeyLore() != null) {
            return crate.getKeyLore();
        }

        return Collections.singletonList("&7Use this key to open " + crate.getDisplayName() + "&7.");
    }

    private List<String> parseTemplates(List<String> lines, Crate crate) {
        List<String> parsed = new ArrayList<>();
        for (String line : lines) {
            parsed.add(parseTemplate(line, crate));
        }
        return parsed;
    }

    private List<String> colourize(List<String> lines) {
        List<String> coloured = new ArrayList<>();
        for (String line : lines) {
            coloured.add(Colour.colour(line));
        }
        return coloured;
    }

    private String parseTemplate(String value, Crate crate) {
        if (value == null) {
            return "";
        }

        return value
                .replace("%crate%", crate.getName())
                .replace("%crate_name%", crate.getName())
                .replace("%crate_display%", crate.getDisplayName())
                .replace("%crate_display_name%", crate.getDisplayName());
    }

    private KeyMode normalizeBinaryMode(KeyMode configuredMode, KeyMode fallback) {
        if (configuredMode == KeyMode.PHYSICAL || configuredMode == KeyMode.VIRTUAL) {
            return configuredMode;
        }
        return fallback;
    }

    @Getter
    public static class KeyGrantResult {
        private final KeyMode keyMode;
        private final int requestedAmount;
        private final int currentAmount;
        private final int droppedAmount;

        public KeyGrantResult(KeyMode keyMode, int requestedAmount, int currentAmount, int droppedAmount) {
            this.keyMode = keyMode;
            this.requestedAmount = requestedAmount;
            this.currentAmount = currentAmount;
            this.droppedAmount = droppedAmount;
        }

        public String getKeyTypeLabel() {
            return keyMode.getDisplayName();
        }
    }
}
