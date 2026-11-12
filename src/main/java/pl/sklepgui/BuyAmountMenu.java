package pl.sklepgui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Wybor ilosci i zatwierdzenie zakupu jednego przedmiotu za walute. */
final class BuyAmountMenu extends Menu {

    private final ConfigurationSection entry;
    private final double price;
    private final int max;
    private final String itemName;
    private int amount = 1;

    BuyAmountMenu(SklepPlugin plugin, String id) {
        super(plugin, titleFor(plugin, id), plugin.waluta().getInt("buy.amount-menu.rows", 5));
        this.entry = plugin.waluta().getConfigurationSection("buy.items." + id);
        this.price = entry == null ? 0 : entry.getDouble("price");
        this.max = Math.max(1, plugin.waluta().getInt("buy.max-amount", 64));
        this.itemName = nameOf(entry);
    }

    private static String nameOf(ConfigurationSection s) {
        if (s == null) return "?";
        String n = s.getString("name");
        if (n != null && !n.isEmpty()) return Util.strip(n);
        Material m = Material.matchMaterial(s.getString("material", "STONE"));
        return m == null ? "?" : Util.prettyMaterial(m);
    }

    private static String titleFor(SklepPlugin plugin, String id) {
        String t = plugin.waluta().getString("buy.amount-menu.title", "&7Sklep-Kupno | %item%");
        return t.replace("%item%", nameOf(plugin.waluta().getConfigurationSection("buy.items." + id)));
    }

    private double total() {
        return Util.round2(price * amount);
    }

    @Override
    protected void render() {
        ConfigurationSection am = plugin.waluta().getConfigurationSection("buy.amount-menu");
        if (am == null || entry == null) return;
        Map<String, String> ph = Util.ph(
                "%price%", Util.num(price),
                "%amount%", String.valueOf(amount),
                "%total%", Util.num(total()),
                "%item%", itemName);

        staticItems(am.getConfigurationSection("static-items"), ph);

        // produkt
        ItemStack prod = Util.item(entry, ph, false);
        prod.setAmount(amount);
        Util.decorate(prod, entry.getString("name"), am.getStringList("item-lore"), ph);
        set(am.getInt("slots.item", 19), prod, null);

        // zatwierdz
        ConfigurationSection cf = am.getConfigurationSection("confirm");
        if (cf != null) {
            set(cf.getInt("slot", 31), Util.item(cf, ph, true), e -> {
                if (e.isLeftClick() && e.getWhoClicked() instanceof Player p) confirm(p);
            });
        }

        // przyciski +N / -N
        buttons(am, "add", +1, ph);
        buttons(am, "remove", -1, ph);
    }

    private void buttons(ConfigurationSection am, String kind, int sign, Map<String, String> ph) {
        ConfigurationSection tpl = am.getConfigurationSection(kind + "-button");
        ConfigurationSection slots = am.getConfigurationSection(kind + "-slots");
        if (tpl == null || slots == null) return;
        for (String key : slots.getKeys(false)) {
            int parsed = -1;
            try {
                parsed = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                // pomijamy zly klucz
            }
            if (parsed < 1) continue;
            final int n = parsed;
            boolean visible = sign > 0 ? amount < max : (amount > 1 && (n == 1 || amount >= n));
            if (!visible) continue;
            Map<String, String> p2 = new java.util.HashMap<>(ph);
            p2.put("%n%", String.valueOf(n));
            set(slots.getInt(key), Util.item(tpl, p2, true), e -> {
                if (!e.isLeftClick()) return;
                amount = sign > 0 ? Math.min(max, amount + n) : Math.max(1, amount - n);
                refresh();
            });
        }
    }

    private void confirm(Player p) {
        EconomyHook eco = plugin.economy();
        if (eco == null) {
            plugin.send(p, "no-economy");
            return;
        }
        double total = total();
        if (!eco.has(p, total)) {
            plugin.send(p, "not-enough-money");
            return;
        }
        ItemStack give = Util.item(entry, null, false);
        give.setAmount(amount);
        List<ItemStack> list = List.of(give);
        if (!Util.canFit(p, list)) {
            plugin.send(p, "no-space");
            return;
        }
        if (!eco.withdraw(p, total)) {
            plugin.send(p, "not-enough-money");
            return;
        }
        Util.give(p, list);
        p.closeInventory();
        plugin.send(p, "buy-success", "%amount%", String.valueOf(amount), "%item%", itemName, "%total%", Util.num(total));
    }

    @Override
    protected void custom(Player p, String action) {
        if (action.equals("buylist")) new BuyListMenu(plugin).open(p);
    }
}
