package net.vulcandev.vulcancrates.gui;

import me.plugin.libs.YamlDocument;
import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.GUI;
import net.xantharddev.vulcanlib.libs.SimpleItem;
import net.xantharddev.vulcanlib.libs.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.Prize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for previewing crate prizes.
 * Displays all prizes in a paginated inventory with navigation controls.
 * Admins can click prizes to test them.
 */
public class CratePreviewGUI extends GUI<Integer> {
    private static final int CLOSE_INDEX = -5;
    private static final int NEXT_INDEX = -7;
    private static final int PREV_INDEX = -6;
    private final VulcanCrates plugin;
    private final YamlDocument config;
    private final Crate crate;
    private final List<Prize> prizes;
    private final int page;

    public CratePreviewGUI(VulcanCrates plugin, Player user, Crate crate, int page) {
        super(user, plugin.guiConf().getInt("crate-preview.rows"));
        this.plugin = plugin;
        this.config = plugin.guiConf();
        this.crate = crate;
        this.prizes = crate.getPrizes() != null ? new ArrayList<>(crate.getPrizes().values()) : new ArrayList<>();
        this.page = page;
        build();
    }

    public CratePreviewGUI(VulcanCrates plugin, Player user, Crate crate) {
        this(plugin, user, crate, 0);
    }
    
    private int getStartIndex() {
        return page * getPrizeSlots().size();
    }
    
    private List<Integer> getPrizeSlots() {
        return config.getIntList("crate-preview.previewSlots");
    }
    
    @Override
    protected String getName() {
        return Colour.colour(config.getString("crate-preview.title").replace("%crate%", crate.getDisplayName()));
    }
    
    @Override
    protected String parse(String toParse, Integer index) {
        if (index >= 0 && index < prizes.size()) {
            Prize prize = prizes.get(index);
            return toParse
                    .replace("%prize%", prize.getName())
                    .replace("%chance%", String.valueOf(prize.getChance()))
                    .replace("%announce%", prize.isAnnounce() ? "Yes" : "No")
                    .replace("%url%", prize.getUrl() != null ? prize.getUrl() : "");
        }
        return toParse.replace("%crate%", crate.getDisplayName());
    }
    
    @Override
    protected void onClick(Integer index, ClickType clickType) {
        switch (index) {
            case NEXT_INDEX:
                new CratePreviewGUI(plugin, user, crate, page + 1).open();
                break;
            case PREV_INDEX:
                new CratePreviewGUI(plugin, user, crate, page - 1).open();
                break;
            case CLOSE_INDEX:
                user.closeInventory();
                break;
            default:
                // Check if it's a prize click and user has admin permissions
                if (index >= 0 && index < prizes.size() && user.hasPermission("crates.admin.test")) {
                    Prize prize = prizes.get(index);
                    testPrize(prize);
                }
                break;
        }
    }

    /**
     * Tests a prize by giving it to the admin user.
     * Only available to users with crates.admin.test permission.
     * @param prize the prize to test
     */
    private void testPrize(Prize prize) {
        // Execute prize commands
        if (prize.getCommands() != null && !prize.getCommands().isEmpty()) {
            for (String command : prize.getCommands()) {
                String processedCommand = command.replace("%player%", user.getName());
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        processedCommand
                );
            }
        }
        
        // Send confirmation message
        String message = plugin.conf().getString("messages.admin-test-prize").replace("%prizeName%", prize.getName());
        user.sendMessage(Colour.colour(message));
        
        // If announce is enabled, show what would be announced (but don't actually announce)
        if (prize.isAnnounce()) {
            String announcePreview = plugin.conf().getString("messages.crate-rolled-reward-announce")
                    .replace("%prefix%", plugin.conf().getString("messages.prefix"))
                    .replace("%player%", user.getName())
                    .replace("%prizeName%", prize.getName())
                    .replace("%crateType%", crate.getDisplayName());
            user.sendMessage(Colour.colour("&a[ADMIN TEST] &7Would announce: " + announcePreview));
        }
    }

    private int getMaxPages() {
        int slotsPerPage = getPrizeSlots().size();
        return (int) Math.ceil((double) prizes.size() / slotsPerPage);
    }
    
    @Override
    protected Map<Integer, Integer> createSlotMap() {
        HashMap<Integer, Integer> slotMap = new HashMap<>();
        List<Integer> prizeSlots = getPrizeSlots();
        int startIndex = getStartIndex();
        int endIndex = Math.min(startIndex + prizeSlots.size(), prizes.size());
        
        // Map prize slots to prize indices
        for (int i = startIndex; i < endIndex && i - startIndex < prizeSlots.size(); i++) {
            slotMap.put(prizeSlots.get(i - startIndex), i);
        }
        
        // Add navigation buttons
        if (page > 0) {
            Utils.addIndexToMap(slotMap, config, "crate-preview.prev.slot", PREV_INDEX, size);
        }
        if (page < getMaxPages() - 1) {
            Utils.addIndexToMap(slotMap, config, "crate-preview.next.slot", NEXT_INDEX, size);
        }
        
        // Add close button
        Utils.addIndexToMap(slotMap, config, "crate-preview.close.slot", CLOSE_INDEX, size);
        return slotMap;
    }
    
    @Override
    protected SimpleItem getItem(Integer index) {
        if (index >= 0) {
            Prize prize = prizes.get(index);
            
            SimpleItem.Builder builder = SimpleItem.builder()
                    .setMaterial(prize.getMaterial())
                    .setAmount(prize.getAmount())
                    .setDamage(prize.getData())
                    .setLore(new ArrayList<>(prize.getLore()))
                    .setUrl(prize.getUrl())
                    .setName(Colour.colour(prize.getName()));
            
            // Add admin test info if user has permission
            if (user.hasPermission("crates.admin.test")) {
                List<String> lore = new ArrayList<>(prize.getLore());
                lore.add("");
                lore.add(Colour.colour("&a&l[ADMIN] &7Click to test this prize!"));
                lore.add(Colour.colour("&7This will execute all commands and give you the reward"));
                builder.setLore(lore);
            }

            return builder.build();
        }
        
        switch (index) {
            case PREV_INDEX:
                return Utils.createSimpleItemFromConfig(user, config, "crate-preview.prev");
            case NEXT_INDEX:
                return Utils.createSimpleItemFromConfig(user, config, "crate-preview.next");
            case CLOSE_INDEX:
                return Utils.createSimpleItemFromConfig(user, config, "crate-preview.close");
        }
        return SimpleItem.builder().build();
    }
    
    @Override
    protected Map<Integer, SimpleItem> createDummyItems() {
        return Utils.createDummyItems(user, config, "crate-preview.fillers");
    }
}