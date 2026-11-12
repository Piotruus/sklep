package pl.sklepgui;

import org.bukkit.entity.Player;

/** Wejscie do sklepu za walute: Kupno / Sprzedaz. */
final class CurrencyHubMenu extends Menu {

    CurrencyHubMenu(SklepPlugin plugin) {
        super(plugin, plugin.waluta().getString("hub.title", "&7Sklep"), plugin.waluta().getInt("hub.rows", 5));
    }

    @Override
    protected void render() {
        staticItems(plugin.waluta().getConfigurationSection("hub.static-items"), null);
    }

    @Override
    protected void custom(Player p, String action) {
        switch (action) {
            case "buy" -> new BuyListMenu(plugin).open(p);
            case "sell" -> new SellMenu(plugin).open(p);
            default -> { }
        }
    }
}
