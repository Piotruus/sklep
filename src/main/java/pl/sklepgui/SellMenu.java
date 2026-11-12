package pl.sklepgui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Sprzedaz przedmiotow za walute: PPM = 1 szt., SHIFT+PPM = wszystko. */
final class SellMenu extends Menu {

    SellMenu(SklepPlugin plugin) {
        super(plugin, plugin.waluta().getString("sell.title", "&7Sklep-Sprzedaz"), plugin.waluta().getInt("sell.rows", 6));
    }

    @Override
    protected void render() {
        FileConfiguration c = plugin.waluta();
        staticItems(c.getConfigurationSection("sell.static-items"), null);
        ConfigurationSection items = c.getConfigurationSection("sell.items");
        if (items == null) return;
        List<String> lore = c.getStringList("sell.list-lore");
        boolean plain = c.getBoolean("sell.only-plain-items", true);

        for (String id : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(id);
            if (s == null) continue;
            Material mat = Material.matchMaterial(s.getString("material", ""));
            if (mat == null || !s.contains("price")) {
                plugin.getLogger().warning("Zly material/cena dla sell." + id + " - pominieto.");
                continue;
            }
            double price = s.getDouble("price");
            Map<String, String> ph = Util.ph("%price%", Util.num(price));
            ItemStack it = Util.item(s, ph, false);
            Util.decorate(it, s.getString("name"), lore, ph);
            String name = s.getString("name") != null ? Util.strip(s.getString("name")) : Util.prettyMaterial(mat);
            set(s.getInt("slot", -1), it, e -> {
                if (e.getWhoClicked() instanceof Player p) sell(p, mat, price, name, plain, e.getClick());
            });
        }
    }

    private void sell(Player p, Material mat, double price, String name, boolean plain, ClickType click) {
        boolean all = click == ClickType.SHIFT_RIGHT;
        if (click != ClickType.RIGHT && !all) return;

        EconomyHook eco = plugin.economy();
        if (eco == null) {
            plugin.send(p, "no-economy");
            return;
        }
        Predicate<ItemStack> f = it -> it.getType() == mat && (!plain || !it.hasItemMeta());
        int have = Util.count(p, f);
        if (have <= 0) {
            plugin.send(p, "nothing-to-sell");
            return;
        }
        int amt = all ? have : 1;
        double total = Util.round2(price * amt);
        if (!eco.deposit(p, total)) return;      // najpierw wyplata, dopiero potem zabranie przedmiotow
        Util.remove(p, f, amt);
        plugin.send(p, "sold", "%amount%", String.valueOf(amt), "%item%", name, "%total%", Util.num(total));
    }

    @Override
    protected void custom(Player p, String action) {
        if (action.equals("hub")) new CurrencyHubMenu(plugin).open(p);
    }
}
