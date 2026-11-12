package pl.sklepgui;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Pule przedmiotow do losowania (zastepuje {ksiegi::*} ze Skripta). Zapis w pools.yml. */
public final class PoolManager {

    private final SklepPlugin plugin;
    private final File file;
    private final YamlConfiguration yml;

    PoolManager(SklepPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pools.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    public List<ItemStack> list(String name) {
        List<ItemStack> r = new ArrayList<>();
        List<?> raw = yml.getList("pools." + name);
        if (raw == null) return r;
        for (Object o : raw) {
            if (o instanceof ItemStack i) r.add(i);
        }
        return r;
    }

    public void add(String name, ItemStack item) {
        List<ItemStack> l = list(name);
        ItemStack c = item.clone();
        c.setAmount(1);
        l.add(c);
        yml.set("pools." + name, l);
        save();
    }

    public void clear(String name) {
        yml.set("pools." + name, null);
        save();
    }

    public ItemStack random(String name) {
        List<ItemStack> l = list(name);
        if (l.isEmpty()) return null;
        return l.get(ThreadLocalRandom.current().nextInt(l.size())).clone();
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Nie mozna zapisac pools.yml: " + ex.getMessage());
        }
    }
}
