package pl.sklepgui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SklepPlugin extends JavaPlugin implements Listener, TabExecutor {

    private FileConfiguration serca;
    private FileConfiguration waluta;
    private FileConfiguration czas;
    private TimeManager time;
    private PoolManager pools;
    private EconomyHook economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadFiles();
        time = new TimeManager(this);
        pools = new PoolManager(this);
        time.start();

        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        for (String c : new String[]{"sklep", "sklepzaserca", "sklepzawalute", "sklepzaczas", "sklepadmin"}) {
            if (getCommand(c) != null) {
                getCommand(c).setExecutor(this);
                getCommand(c).setTabCompleter(this);
            }
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            getLogger().warning("Nie znaleziono Vault - sklep za walute bedzie nieaktywny (reszta dziala).");
        }
        getLogger().info("SklepGUI wlaczony.");
    }

    @Override
    public void onDisable() {
        if (time != null) time.save();
    }

    // ------------------------------------------------------------ pliki

    private void loadFiles() {
        reloadConfig();
        serca = load("serca.yml");
        waluta = load("waluta.yml");
        czas = load("czas.yml");
    }

    private FileConfiguration load(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) saveResource(name, false);
        return YamlConfiguration.loadConfiguration(f);
    }

    FileConfiguration serca() { return serca; }
    FileConfiguration waluta() { return waluta; }
    FileConfiguration czas() { return czas; }
    TimeManager time() { return time; }
    PoolManager pools() { return pools; }

    /** Ekonomia z Vault (lazy - uslugi ekonomii rejestruja sie po starcie pluginow). */
    EconomyHook economy() {
        if (economy == null && Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            economy = EconomyHook.find();
        }
        return economy;
    }

    void send(CommandSender to, String key, String... kv) {
        String m = getConfig().getString("messages." + key, "");
        if (m.isEmpty()) return;
        to.sendMessage(Util.color(getConfig().getString("prefix", "") + Util.apply(m, Util.ph(kv))));
    }

    // ------------------------------------------------------------ eventy

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        time.join(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        time.quit(e.getPlayer());
    }

    // ------------------------------------------------------------ komendy

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        if (name.equals("sklepadmin")) return admin(sender, args);

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy.");
            return true;
        }
        switch (name) {
            case "sklep" -> new MainMenu(this).open(p);
            case "sklepzaserca" -> new RewardShopMenu(this, RewardShopMenu.Type.HEARTS, p).open(p);
            case "sklepzawalute" -> new CurrencyHubMenu(this).open(p);
            case "sklepzaczas" -> new RewardShopMenu(this, RewardShopMenu.Type.TIME, p).open(p);
            default -> { }
        }
        return true;
    }

    private boolean admin(CommandSender s, String[] a) {
        if (!s.hasPermission("sklep.admin")) {
            send(s, "no-permission");
            return true;
        }
        if (a.length == 0) {
            help(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                loadFiles();
                send(s, "reloaded");
            }
            case "czas" -> adminTime(s, a);
            case "pool" -> adminPool(s, a);
            case "dajserce" -> {
                if (!(s instanceof Player p)) {
                    s.sendMessage("Tylko dla graczy.");
                    return true;
                }
                ConfigurationSection c = serca.getConfigurationSection("currency-item");
                if (c == null) {
                    s.sendMessage(Util.color("&cBrak sekcji currency-item w serca.yml"));
                    return true;
                }
                int amount = 1;
                if (a.length > 1) {
                    try {
                        amount = Math.max(1, Math.min(64, Integer.parseInt(a[1])));
                    } catch (NumberFormatException ignored) {
                        amount = 1;
                    }
                }
                ItemStack it = Util.item(c, null, true);
                it.setAmount(amount);
                Util.give(p, List.of(it));
                s.sendMessage(Util.color("&aOtrzymano &f" + amount + "x &aserce."));
            }
            default -> help(s);
        }
        return true;
    }

    private void adminTime(CommandSender s, String[] a) {
        // /sklepadmin czas <get|set|add|remove> <gracz> [czas]
        if (a.length < 3) {
            help(s);
            return;
        }
        OfflinePlayer t = Bukkit.getPlayerExact(a[2]);
        if (t == null) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(a[2]);
            if (off.hasPlayedBefore()) t = off;
        }
        if (t == null) {
            send(s, "player-not-found");
            return;
        }
        String op = a[1].toLowerCase(Locale.ROOT);
        if (op.equals("get")) {
            s.sendMessage(Util.color("&7Czas do wydania gracza &f" + a[2] + "&7: &f"
                    + Util.formatTime(time.get(t.getUniqueId()))));
            return;
        }
        if (a.length < 4) {
            help(s);
            return;
        }
        long v = Util.parseTime(a[3]);
        if (v < 0) {
            send(s, "bad-time");
            return;
        }
        switch (op) {
            case "set" -> time.set(t.getUniqueId(), v);
            case "add" -> time.add(t.getUniqueId(), v);
            case "remove" -> time.add(t.getUniqueId(), -v);
            default -> {
                help(s);
                return;
            }
        }
        s.sendMessage(Util.color("&aGracz &f" + a[2] + "&a ma teraz &f"
                + Util.formatTime(time.get(t.getUniqueId())) + "&a do wydania."));
    }

    private void adminPool(CommandSender s, String[] a) {
        // /sklepadmin pool <add|clear|list> <nazwa>
        if (a.length < 3 || !a[2].matches("[a-zA-Z0-9_\\-]+")) {
            help(s);
            return;
        }
        String pool = a[2].toLowerCase(Locale.ROOT);
        switch (a[1].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (!(s instanceof Player p)) {
                    s.sendMessage("Tylko dla graczy.");
                    return;
                }
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) {
                    send(s, "hold-item");
                    return;
                }
                pools.add(pool, hand);
                send(s, "pool-added", "%pool%", pool, "%size%", String.valueOf(pools.list(pool).size()));
            }
            case "clear" -> {
                pools.clear(pool);
                send(s, "pool-cleared", "%pool%", pool);
            }
            case "list" -> s.sendMessage(Util.color("&7Pula &f" + pool + "&7: &f" + pools.list(pool).size() + " &7przedmiot(ow)."));
            default -> help(s);
        }
    }

    private void help(CommandSender s) {
        s.sendMessage(Util.color("&6SklepGUI &7- komendy:"));
        s.sendMessage(Util.color("&e/sklepadmin reload &7- przeladuj pliki config"));
        s.sendMessage(Util.color("&e/sklepadmin czas <get|set|add|remove> <gracz> [czas] &7- np. 4h, 2d, 1d12h"));
        s.sendMessage(Util.color("&e/sklepadmin pool <add|clear|list> <nazwa> &7- pula losowych przedmiotow (add = przedmiot z reki)"));
        s.sendMessage(Util.color("&e/sklepadmin dajserce [ilosc] &7- daje przedmiot-serce z serca.yml"));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        if (!cmd.getName().equalsIgnoreCase("sklepadmin") || !s.hasPermission("sklep.admin")) return List.of();
        List<String> out = new ArrayList<>();
        if (a.length == 1) out = List.of("reload", "czas", "pool", "dajserce");
        else if (a.length == 2 && a[0].equalsIgnoreCase("czas")) out = List.of("get", "set", "add", "remove");
        else if (a.length == 2 && a[0].equalsIgnoreCase("pool")) out = List.of("add", "clear", "list");
        else if (a.length == 3 && a[0].equalsIgnoreCase("czas")) {
            out = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        } else if (a.length == 3 && a[0].equalsIgnoreCase("pool")) out = List.of("ksiegi");
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }
}
