package net.vulcandev.vulcancrates.gui;

import me.plugin.libs.YamlDocument;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;
import net.xantharddev.vulcanlib.libs.GUI;
import net.xantharddev.vulcanlib.libs.SimpleItem;
import net.xantharddev.vulcanlib.libs.SoundUtil;
import net.xantharddev.vulcanlib.libs.Utils;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CrateSpinGUI extends GUI<Integer> {
    private static final int COLUMNS = 9;
    private static final int UNIQUE_ROW_TRIES = 12;
    private static final Random RANDOM = new Random();

    private final VulcanCrates plugin;
    private final YamlDocument config;
    private final Crate crate;
    private final int rows;
    private final int startSlot;
    private final List<Integer> rollingColumns;
    private final boolean[] rollingColumnMap = new boolean[COLUMNS];
    private final List<Integer> selectorColumns;
    private final boolean[] selectorColumnMap = new boolean[COLUMNS];
    private final List<Integer> selectableRows;
    private final Set<Integer> selectedRows = new LinkedHashSet<>();
    private final Set<Integer> claimedRows = new HashSet<>();
    private final Map<Integer, Integer> claimedColumns = new HashMap<>();
    private final Map<Prize, ItemStack> reelItems = new HashMap<>();
    private final Map<Prize, ItemStack> claimableItems = new HashMap<>();
    private final ItemStack inactiveFiller;
    private final SpinSound spinSound;
    private final SpinSound columnStopSound;
    private final SpinSound finishSound;
    private final SpinSound claimSound;
    private final SpinSound countdownSound;

    private Phase phase = Phase.SELECTING;
    private Prize[][] strips;
    private int[] stopSteps;
    private int totalSteps;
    private int step;
    private int countdownRemaining;
    private int countdownTask = -1;
    private int animationTask = -1;
    private int closeTask = -1;

    public CrateSpinGUI(VulcanCrates plugin, Player user, Crate crate) {
        super(user, inventoryRows(plugin));
        this.plugin = plugin;
        this.config = plugin.guiConf();
        this.crate = crate;
        this.rows = size / COLUMNS;
        this.startSlot = validSlot(config.getInt("crate-spin.start-opening.slot",
                config.getInt("crate-spin.selection.confirm.slot", size - 5)), size - 5);
        this.rollingColumns = loadRollingColumns();
        for (Integer column : rollingColumns) {
            rollingColumnMap[column] = true;
        }
        this.selectorColumns = loadSelectorColumns();
        for (Integer column : selectorColumns) {
            selectorColumnMap[column] = true;
        }
        this.selectableRows = loadSelectableRows();
        this.inactiveFiller = fillerItem("crate-spin.inactive-filler", (short) 7);
        this.spinSound = loadSound("crate-spin.spin-sound");
        this.columnStopSound = loadSound("crate-spin.column-stop-sound");
        this.finishSound = loadSound("crate-spin.finish-sound");
        this.claimSound = loadSound("crate-spin.claim-sound");
        this.countdownSound = loadSound("crate-spin.countdown.sound");
        build();
    }

    public void abort() {
        cancelCountdown();
        cancelAnimation();
        cancelClose();

        if (phase == Phase.SELECTING || strips == null || phase == Phase.COMPLETE) {
            phase = Phase.COMPLETE;
            return;
        }

        phase = Phase.COMPLETE;
        step = totalSteps;
        for (Integer row : selectedRows) {
            if (claimedRows.contains(row)) continue;

            int column = rollingColumns.get(RANDOM.nextInt(rollingColumns.size()));
            claimedRows.add(row);
            claimedColumns.put(row, column);
            plugin.getCrateOpenManager().award(user, crate, finalPrize(row, column));
        }
    }

    public void closeView() {
        if (user.isOnline() && user.getOpenInventory().getTopInventory().getHolder() == this) {
            user.closeInventory();
        }
    }

    private void toggleRow(int row) {
        if (selectedRows.remove(row)) {
            renderAll();
            return;
        }

        int limit = selectionLimit();
        if (selectedRows.size() >= limit) {
            plugin.getCrateOpenManager().sendRowLimit(user, crate, limit);
            return;
        }

        selectedRows.add(row);
        renderAll();
    }

    private void confirmSelection() {
        if (selectedRows.isEmpty()) {
            plugin.getCrateOpenManager().sendSelectRequired(user, crate);
            return;
        }

        if (!plugin.getCrateOpenManager().beginSpin(user, crate, selectedRows.size())) {
            renderAll();
            return;
        }

        totalSteps = buildStrips();
        phase = Phase.COUNTDOWN;
        countdownRemaining = Math.max(0, config.getInt("crate-spin.countdown.seconds", 3));
        renderAll();

        if (countdownRemaining == 0) {
            startAnimation();
            return;
        }

        countdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickCountdown, 0L, 20L);
    }

    private void tickCountdown() {
        if (!user.isOnline()) {
            plugin.getCrateOpenManager().finish(user);
            return;
        }

        if (countdownRemaining <= 0) {
            startAnimation();
            return;
        }

        renderAll();
        play(countdownSound, countdownPitch());
        countdownRemaining--;
    }

    private float countdownPitch() {
        int seconds = Math.max(1, config.getInt("crate-spin.countdown.seconds", 3));
        float high = config.getDouble("crate-spin.countdown.sound.high-pitch", 1.8D).floatValue();
        float low = config.getDouble("crate-spin.countdown.sound.low-pitch", 0.8D).floatValue();
        if (seconds == 1) return high;

        float elapsed = seconds - countdownRemaining;
        return high + ((low - high) * (elapsed / (seconds - 1.0F)));
    }

    private void startAnimation() {
        cancelCountdown();
        phase = Phase.SPINNING;
        step = 0;
        renderAll();

        int interval = Math.max(1, config.getInt("crate-spin.animation.tick-interval", 2));
        animationTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickAnimation, interval, interval);
    }

    private void tickAnimation() {
        if (!user.isOnline()) {
            plugin.getCrateOpenManager().finish(user);
            return;
        }

        step++;
        for (int col = 0; col < COLUMNS; col++) {
            if (step == stopSteps[col]) play(columnStopSound);
        }

        renderAll();
        play(spinSound);

        if (step >= totalSteps) stopReel();
    }

    private void stopReel() {
        cancelAnimation();
        phase = Phase.CLAIMING;
        renderAll();
        play(finishSound);
        plugin.getCrateOpenManager().sendSelectPrompt(user, selectedRows.size());
    }

    private void claim(int slot) {
        int row = slot / COLUMNS;
        int col = slot % COLUMNS;
        if (!selectedRows.contains(row) || claimedRows.contains(row) || !rollingColumnMap[col]) return;

        Prize prize = finalPrize(row, col);
        claimedRows.add(row);
        claimedColumns.put(row, col);
        renderAll();
        play(claimSound);
        plugin.getCrateOpenManager().award(user, crate, prize);

        if (claimedRows.size() == selectedRows.size()) scheduleClose();
    }

    private void scheduleClose() {
        phase = Phase.COMPLETE;
        if (!config.getBoolean("crate-spin.auto-close", true)) return;

        int delay = Math.max(1, config.getInt("crate-spin.close-delay", 40));
        closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            closeTask = -1;
            closeView();
        }, delay).getTaskId();
    }

    private int buildStrips() {
        int baseSteps = Math.max(rows + 1, config.getInt("crate-spin.animation.base-steps", 26));
        int stagger = Math.max(0, config.getInt("crate-spin.animation.column-stagger", 2));
        strips = new Prize[COLUMNS][];
        stopSteps = new int[COLUMNS];

        int lastStop = baseSteps;
        for (int index = 0; index < rollingColumns.size(); index++) {
            int col = rollingColumns.get(index);
            stopSteps[col] = baseSteps + (index * stagger);
            Prize[] strip = new Prize[stopSteps[col] + rows];
            for (int stripIndex = 0; stripIndex < strip.length; stripIndex++) {
                strip[stripIndex] = crate.getRandomPrize();
            }
            strips[col] = strip;
            lastStop = stopSteps[col];
        }

        if (config.getBoolean("crate-spin.no-duplicates-per-row", true)) {
            removeWinningRowDuplicates();
        }

        return lastStop;
    }

    private void removeWinningRowDuplicates() {
        if (crate.getPrizes() == null || crate.getPrizes().size() < rollingColumns.size()) return;

        for (int row = 0; row < rows; row++) {
            Set<Prize> used = new HashSet<>();
            for (Integer col : rollingColumns) {
                int index = stopSteps[col] + row;
                for (int tries = 0; tries < UNIQUE_ROW_TRIES && used.contains(strips[col][index]); tries++) {
                    strips[col][index] = crate.getRandomPrize();
                }
                used.add(strips[col][index]);
            }
        }
    }

    private ItemStack itemAt(int slot) {
        int row = slot / COLUMNS;
        int col = slot % COLUMNS;

        if (phase == Phase.SELECTING) {
            if (slot == startSlot) return selectionItem("crate-spin.start-opening", row, Material.EMERALD_BLOCK, (short) 0);
            if (isSelectorSlot(slot)) return selectorItem(row);
            return inactiveFiller;
        }

        if (phase == Phase.COUNTDOWN) {
            if (slot == startSlot) return selectionItem("crate-spin.countdown.item", row,
                    MaterialDb.get("WATCH", Material.STONE), (short) 0);
        }

        if (isSelectorSlot(slot) && selectedRows.contains(row)) {
            return selectionItem("crate-spin.selection.locked-row", row,
                    MaterialDb.get("STAINED_GLASS_PANE", Material.STONE), (short) 5);
        }

        if (phase == Phase.COUNTDOWN) {
            return inactiveFiller;
        }

        if (phase == Phase.SPINNING) {
            return rollingColumnMap[col] ? reelItem(visiblePrize(row, col)) : inactiveFiller;
        }

        if (!rollingColumnMap[col]) return inactiveFiller;
        if (!selectedRows.contains(row)) return reelItem(finalPrize(row, col));
        if (!claimedRows.contains(row)) return claimableItem(finalPrize(row, col));

        Integer claimedColumn = claimedColumns.get(row);
        if (claimedColumn != null && claimedColumn == col) return claimedItem(finalPrize(row, col));
        return reelItem(finalPrize(row, col));
    }

    private ItemStack selectorItem(int row) {
        String path = selectedRows.contains(row)
                ? "crate-spin.selection.selected-row"
                : "crate-spin.selection.unselected-row";
        return selectionItem(path, row, MaterialDb.get("STAINED_GLASS_PANE", Material.STONE),
                selectedRows.contains(row) ? (short) 5 : (short) 7);
    }

    private Prize visiblePrize(int row, int col) {
        return strips[col][Math.min(step, stopSteps[col]) + row];
    }

    private Prize finalPrize(int row, int col) {
        return strips[col][stopSteps[col] + row];
    }

    private ItemStack reelItem(Prize prize) {
        ItemStack cached = reelItems.get(prize);
        if (cached == null) {
            cached = buildPrizeItem(prize, null, false);
            reelItems.put(prize, cached);
        }
        return cached;
    }

    private ItemStack claimableItem(Prize prize) {
        ItemStack cached = claimableItems.get(prize);
        if (cached == null) {
            cached = buildPrizeItem(prize, config.getStringList("crate-spin.claim-lore"), true);
            claimableItems.put(prize, cached);
        }
        return cached;
    }

    private ItemStack claimedItem(Prize prize) {
        return buildPrizeItem(prize, config.getStringList("crate-spin.claimed-lore"), true);
    }

    private ItemStack buildPrizeItem(Prize prize, List<String> extraLore, boolean glow) {
        if (prize == null) return inactiveFiller;

        List<String> lore = new ArrayList<>();
        if (prize.getLore() != null) lore.addAll(prize.getLore());
        if (extraLore != null) lore.addAll(extraLore);

        SimpleItem.Builder builder = SimpleItem.builder()
                .setMaterial(prize.getMaterial() != null ? prize.getMaterial() : Material.STONE)
                .setAmount(Math.max(1, prize.getAmount()))
                .setDamage(prize.getData())
                .setName(prize.getName())
                .setLore(lore)
                .setGlowing(glow || prize.isGlow());

        if (prize.getUrl() != null && !prize.getUrl().trim().isEmpty()) {
            builder.setUrl(prize.getUrl());
        }

        return builder.build().get();
    }

    private ItemStack selectionItem(String path, int row, Material fallbackMaterial, short fallbackDamage) {
        SimpleItem item;
        if (config.getSection(path) != null) {
            item = Utils.createSimpleItemFromConfig(user, config, path);
        } else {
            item = SimpleItem.builder()
                    .setMaterial(fallbackMaterial)
                    .setDamage(fallbackDamage)
                    .setName("&7Row %row%")
                    .setLore(Collections.<String>emptyList())
                    .build();
        }

        item.setName(placeholders(item.getName(), row));
        if (item.getLore() != null) {
            List<String> lore = new ArrayList<>();
            for (String line : item.getLore()) {
                lore.add(placeholders(line, row));
            }
            item.setLore(lore);
        }
        return item.get();
    }

    private String placeholders(String value, int row) {
        if (value == null) return null;

        String parsed = value
                .replace("%crate%", crate.getDisplayName())
                .replace("%row%", String.valueOf(row + 1))
                .replace("%selected%", String.valueOf(selectedRows.size()))
                .replace("%cost%", String.valueOf(selectedRows.size()))
                .replace("%seconds%", String.valueOf(countdownRemaining));
        if (parsed.contains("%keys%")) {
            parsed = parsed.replace("%keys%", String.valueOf(plugin.getKeyManager().getUsableKeys(user, crate)));
        }
        if (parsed.contains("%max%")) {
            parsed = parsed.replace("%max%", String.valueOf(selectionLimit()));
        }
        return parsed;
    }

    private ItemStack fillerItem(String path, short fallbackDamage) {
        if (config.getSection(path) != null) {
            return Utils.createSimpleItemFromConfig(user, config, path).get();
        }

        return SimpleItem.builder()
                .setMaterial(MaterialDb.get("STAINED_GLASS_PANE", Material.STONE))
                .setDamage(fallbackDamage)
                .setName("&7")
                .setLore(Collections.<String>emptyList())
                .build()
                .get();
    }

    private List<Integer> loadSelectableRows() {
        List<Integer> loaded = new ArrayList<>();
        for (Integer row : config.getIntList("crate-spin.selectable-rows")) {
            if (row == null || row < 0 || row >= rows || loaded.contains(row)) continue;
            loaded.add(row);
        }

        if (loaded.isEmpty()) {
            for (int row = 0; row < rows; row++) {
                loaded.add(row);
            }
        }

        Collections.sort(loaded);
        return loaded;
    }

    private List<Integer> loadRollingColumns() {
        List<Integer> loaded = new ArrayList<>();
        for (Integer column : config.getIntList("crate-spin.rolling-columns")) {
            if (column == null || column < 1 || column > COLUMNS) continue;

            int index = column - 1;
            if (!loaded.contains(index)) loaded.add(index);
        }

        if (loaded.isEmpty()) {
            for (int column = 1; column < COLUMNS - 1; column++) {
                loaded.add(column);
            }
        }

        Collections.sort(loaded);
        return loaded;
    }

    private List<Integer> loadSelectorColumns() {
        List<Integer> loaded = new ArrayList<>();
        for (Integer column : config.getIntList("crate-spin.selection.columns")) {
            if (column == null || column < 1 || column > COLUMNS) continue;

            int index = column - 1;
            if (!loaded.contains(index)) loaded.add(index);
        }

        if (loaded.isEmpty()) {
            loaded.add(0);
            loaded.add(COLUMNS - 1);
        }

        Collections.sort(loaded);
        return loaded;
    }

    private boolean isSelectorSlot(int slot) {
        int row = slot / COLUMNS;
        int column = slot % COLUMNS;
        return selectableRows.contains(row) && selectorColumnMap[column];
    }

    private int selectionLimit() {
        int rowLimit = Math.max(1, plugin.conf().getInt("spin.max-rows", selectableRows.size()));
        int keyLimit = Math.min(plugin.getCrateOpenManager().getKeysPerWindow(),
                plugin.getKeyManager().getUsableKeys(user, crate));
        return Math.min(Math.min(rowLimit, selectableRows.size()), keyLimit);
    }

    private void renderAll() {
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, itemAt(slot));
        }
        updateClient();
    }

    @SuppressWarnings("deprecation")
    private void updateClient() {
        if (user.isOnline()) user.updateInventory();
    }

    private void cancelCountdown() {
        if (countdownTask == -1) return;
        Bukkit.getScheduler().cancelTask(countdownTask);
        countdownTask = -1;
    }

    private void cancelAnimation() {
        if (animationTask == -1) return;
        Bukkit.getScheduler().cancelTask(animationTask);
        animationTask = -1;
    }

    private void cancelClose() {
        if (closeTask == -1) return;
        Bukkit.getScheduler().cancelTask(closeTask);
        closeTask = -1;
    }

    private void play(SpinSound sound) {
        if (sound != null) play(sound, sound.pitch);
    }

    private void play(SpinSound sound, float pitch) {
        if (sound == null || !user.isOnline()) return;
        SoundUtil.play(user, user.getLocation(), sound.name, sound.volume, pitch);
    }

    private SpinSound loadSound(String path) {
        String name = resolveSoundName(config.getString(path + ".sound", ""),
                config.getString(path + ".legacy-sound", ""));
        if (name == null) return null;

        float volume = config.getDouble(path + ".volume", 1.0D).floatValue();
        float pitch = config.getDouble(path + ".pitch", 1.0D).floatValue();
        return new SpinSound(name, volume, pitch);
    }

    private String resolveSoundName(String name, String legacyName) {
        if (soundExists(name)) return name;
        if (soundExists(legacyName)) return legacyName;
        if (name != null && name.contains(".")) return name;
        return null;
    }

    private boolean soundExists(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        try {
            Sound.valueOf(name.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private int validSlot(int configured, int fallback) {
        return configured >= 0 && configured < size ? configured : fallback;
    }

    private static int inventoryRows(VulcanCrates plugin) {
        return Math.max(2, Math.min(6, plugin.guiConf().getInt("crate-spin.rows", 6)));
    }

    @Override
    protected String getName() {
        return config.getString("crate-spin.title", "&eOpening &8| %crate%")
                .replace("%crate%", crate.getDisplayName());
    }

    @Override
    protected String parse(String toParse, Integer slot) {
        return placeholders(toParse, slot == null ? 0 : slot / COLUMNS);
    }

    @Override
    protected void onClick(Integer slot, ClickType clickType) {
        if (phase == Phase.SELECTING) {
            if (slot == startSlot) {
                confirmSelection();
                return;
            }

            if (isSelectorSlot(slot)) toggleRow(slot / COLUMNS);
            return;
        }

        if (phase == Phase.CLAIMING) claim(slot);
    }

    @Override
    protected Map<Integer, Integer> createSlotMap() {
        Map<Integer, Integer> slotMap = new HashMap<>();
        for (int slot = 0; slot < size; slot++) {
            slotMap.put(slot, slot);
        }
        return slotMap;
    }

    @Override
    protected ItemStack getItem(Integer slot) {
        return itemAt(slot);
    }

    @Override
    protected Map<Integer, SimpleItem> createDummyItems() {
        return Collections.emptyMap();
    }

    private enum Phase {
        SELECTING,
        COUNTDOWN,
        SPINNING,
        CLAIMING,
        COMPLETE
    }

    private static final class SpinSound {
        private final String name;
        private final float volume;
        private final float pitch;

        private SpinSound(String name, float volume, float pitch) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
