package pl.sklepgui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Lista przedmiotow do kupienia za walute (waluta.yml -> buy). */
final class BuyListMenu extends Menu {

    BuyListMenu(SklepPlugin plugin) {
        super(plugin, plugin.waluta().getString("buy.title", "&7Sklep-Kupno"), plugin.waluta().getInt("buy.rows", 6));
    }

    @Override
    protected void render() {
        FileConfiguration c = plugin.waluta();
        staticItems(c.getConfigurationSection("buy.static-items"), null);
        ConfigurationSection items = c.getConfigurationSection("buy.items");
        if (items == null) return;
        List<String> lore = c.getStringList("buy.list-lore");

        for (String id : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(id);
            if (s == null) continue;
            if (!s.contains("price") || s.getDouble("price") < 0) {
                plugin.getLogger().warning("Brak/zla cena (price) dla buy." + id + " - pominieto.");
                continue;
            }
            Map<String, String> ph = Util.ph("%price%", Util.num(s.getDouble("price")));
            ItemStack it = Util.item(s, ph, false);
            Util.decorate(it, s.getString("name"), lore, ph);
            set(s.getInt("slot", -1), it, e -> {
                if (!e.isLeftClick()) return;
                if (e.getWhoClicked() instanceof Player p) new BuyAmountMenu(plugin, id).open(p);
            });
        }
    }

    @Override
    protected void custom(Player p, String action) {
        if (action.equals("hub")) new CurrencyHubMenu(plugin).open(p);
    }
}
