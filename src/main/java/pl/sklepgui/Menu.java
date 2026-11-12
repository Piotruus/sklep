package pl.sklepgui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Bazowa klasa wszystkich GUI. Kazde GUI jest wlascicielem swojego Inventory. */
abstract class Menu implements InventoryHolder {

    protected final SklepPlugin plugin;
    protected final Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(SklepPlugin plugin, String title, int rows) {
        this.plugin = plugin;
        int r = Math.max(1, Math.min(6, rows));
        this.inv = Bukkit.createInventory(this, r * 9, Util.color(title));
    }

    /** Wypelnia inventory (wolane przy kazdym odswiezeniu). */
    protected abstract void render();

    /** Wlasne akcje ze static-items (action: nazwa). */
    protected void custom(Player p, String action) {}

    public void open(Player p) {
        refresh();
        p.openInventory(inv);
    }

    public void refresh() {
        inv.clear();
        actions.clear();
        render();
        for (HumanEntity v : new ArrayList<>(inv.getViewers())) {
            if (v instanceof Player pl) pl.updateInventory();
        }
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= inv.getSize()) {
            plugin.getLogger().warning("Slot " + slot + " poza zakresem GUI (0-" + (inv.getSize() - 1) + ").");
            return;
        }
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    public void handle(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> a = actions.get(e.getRawSlot());
        if (a != null) a.accept(e);
    }

    /** Przedmioty statyczne: slot, item, + command / action (close, refresh, wlasne). */
    protected void staticItems(ConfigurationSection sec, Map<String, String> ph) {
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            ItemStack it = Util.item(s, ph, true);
            set(s.getInt("slot", -1), it, e -> {
                if (s.getBoolean("left-only", true) && !e.isLeftClick()) return;
                if (e.getWhoClicked() instanceof Player p) staticClick(p, s);
            });
        }
    }

    private void staticClick(Player p, ConfigurationSection s) {
        String cmd = s.getString("command", "");
        String act = s.getString("action", "");
        if (!cmd.isEmpty()) {
            runCommand(p, cmd);
        } else if (act.equalsIgnoreCase("close")) {
            p.closeInventory();
        } else if (act.equalsIgnoreCase("refresh")) {
            refresh();
        } else if (!act.isEmpty()) {
            custom(p, act.toLowerCase());
        }
    }

    protected void runCommand(Player p, String cmd) {
        String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        Bukkit.getScheduler().runTask(plugin, () -> p.performCommand(c));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
