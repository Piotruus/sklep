package pl.sklepgui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wspolny sklep dla dwoch walut:
 *  - HEARTS: platnosc przedmiotami-sercami (serca.yml)
 *  - TIME:   platnosc czasem gry (czas.yml)
 * Nagrody: przedmioty (rewards), losowa pula (pool), komendy konsoli (commands).
 */
final class RewardShopMenu extends Menu {

    enum Type { HEARTS, TIME }

    private final Type type;
    private final UUID viewer;
    private final FileConfiguration cfg;

    RewardShopMenu(SklepPlugin plugin, Type type, Player viewer) {
        super(plugin, cfgOf(plugin, type).getString("title", "&8Sklep"), cfgOf(plugin, type).getInt("rows", 5));
        this.type = type;
        this.viewer = viewer.getUniqueId();
        this.cfg = cfgOf(plugin, type);
    }

    private static FileConfiguration cfgOf(SklepPlugin plugin, Type type) {
        return type == Type.HEARTS ? plugin.serca() : plugin.czas();
    }

    // ------------------------------------------------------------ render

    @Override
    protected void render() {
        Map<String, String> ph = new HashMap<>();
        ph.put("%time%", Util.formatTime(plugin.time().get(viewer)));
        staticItems(cfg.getConfigurationSection("static-items"), ph);

        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items == null) return;
        for (String id : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(id);
            if (s == null) continue;
            long cost = costOf(s);
            if (cost < 0) {
                plugin.getLogger().warning("Zly 'cost' dla przedmiotu '" + id + "' w " + type + " - pominieto.");
                continue;
            }
            Map<String, String> p2 = new HashMap<>(ph);
            p2.put("%cost%", type == Type.HEARTS ? String.valueOf(cost) : Util.formatTime(cost));
            set(s.getInt("slot", -1), Util.item(s, p2, true), e -> {
                if (!e.isLeftClick()) return;
                if (e.getWhoClicked() instanceof Player p) purchase(p, s, cost);
            });
        }
    }

    private long costOf(ConfigurationSection s) {
        if (type == Type.HEARTS) {
            return s.contains("cost") ? s.getLong("cost", -1) : -1;
        }
        return Util.parseTime(s.getString("cost"));
    }

    // ------------------------------------------------------------ platnosc

    private boolean isHeart(ItemStack it) {
        ConfigurationSection c = cfg.getConfigurationSection("currency-item");
        if (c == null || it == null) return false;
        Material m = Material.matchMaterial(c.getString("material", "RED_DYE"));
        if (m == null || it.getType() != m) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        if (c.contains("custom-model-data")) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != c.getInt("custom-model-data")) return false;
        }
        if (c.contains("name")) {
            if (!meta.hasDisplayName()) return false;
            if (!Util.strip(c.getString("name")).equals(Util.strip(meta.getDisplayName()))) return false;
        }
        return true;
    }

    private boolean canPay(Player p, long cost) {
        if (type == Type.HEARTS) return Util.count(p, this::isHeart) >= cost;
        return plugin.time().get(p.getUniqueId()) >= cost;
    }

    private void charge(Player p, long cost) {
        if (type == Type.HEARTS) Util.remove(p, this::isHeart, (int) cost);
        else plugin.time().add(p.getUniqueId(), -cost);
    }

    // ------------------------------------------------------------ zakup

    private void purchase(Player p, ConfigurationSection s, long cost) {
        if (!canPay(p, cost)) {
            feedback(p, cfg.getConfigurationSection("fail"));
            if (cfg.getBoolean("fail.reopen", type == Type.TIME)) refresh();
            else p.closeInventory();
            return;
        }

        List<ItemStack> give = new ArrayList<>();
        ConfigurationSection rs = s.getConfigurationSection("rewards");
        if (rs != null) {
            List<ConfigurationSection> list = new ArrayList<>();
            for (String k : rs.getKeys(false)) {
                ConfigurationSection r = rs.getConfigurationSection(k);
                if (r != null) list.add(r);
            }
            if (!list.isEmpty()) {
                if ("random".equalsIgnoreCase(s.getString("rewards-mode", "all"))) {
                    ConfigurationSection pick = list.get(ThreadLocalRandom.current().nextInt(list.size()));
                    give.add(Util.item(pick, null, true));
                } else {
                    for (ConfigurationSection r : list) give.add(Util.item(r, null, true));
                }
            }
        }

        String pool = s.getString("pool", "");
        if (!pool.isEmpty()) {
            ItemStack drawn = plugin.pools().random(pool);
            if (drawn == null) {
                p.closeInventory();
                plugin.send(p, "pool-empty");
                return; // nic nie pobrano - gracz nie traci serc
            }
            give.add(drawn);
        }

        if (!give.isEmpty() && !Util.canFit(p, give)) {
            plugin.send(p, "no-space");
            return;
        }

        charge(p, cost);
        Util.give(p, give);
        for (String c : s.getStringList("commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("%player%", p.getName()));
        }
        p.closeInventory();
        feedback(p, cfg.getConfigurationSection("success"));
    }

    private void feedback(Player p, ConfigurationSection f) {
        if (f == null) return;
        String msg = f.getString("message", "");
        if (!msg.isEmpty()) p.sendMessage(Util.color(msg));
        String sub = f.getString("subtitle", "");
        if (!sub.isEmpty()) p.sendTitle("", Util.color(sub), 5, 20, 10);
        String snd = f.getString("sound", "");
        if (!snd.isEmpty()) p.playSound(p.getLocation(), snd, 1.0f, 1.0f);
    }
}
