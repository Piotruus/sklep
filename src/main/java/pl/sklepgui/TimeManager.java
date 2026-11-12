package pl.sklepgui;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Liczy czas gry do wydania w sklepie za czas (odpowiednik {czas.%player%} ze Skripta). */
public final class TimeManager {

    private final SklepPlugin plugin;
    private final File file;
    private final Map<UUID, Long> seconds = new HashMap<>();
    private final Map<UUID, Long> mark = new HashMap<>();
    private boolean dirty = false;

    TimeManager(SklepPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("czas");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            try {
                seconds.put(UUID.fromString(k), s.getLong(k));
            } catch (IllegalArgumentException ignored) {
                // zly klucz - pomijamy
            }
        }
    }

    void start() {
        for (Player p : Bukkit.getOnlinePlayers()) join(p);
        Bukkit.getScheduler().runTaskTimer(plugin, this::flushAll, 200L, 200L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    void join(Player p) {
        UUID id = p.getUniqueId();
        if (!seconds.containsKey(id)) {
            long start = plugin.getConfig().getBoolean("time.seed-from-playtime", false) ? vanillaSeconds(p) : 0L;
            seconds.put(id, start);
            dirty = true;
        }
        mark.put(id, System.currentTimeMillis());
    }

    void quit(Player p) {
        flush(p.getUniqueId());
        mark.remove(p.getUniqueId());
    }

    private long vanillaSeconds(Player p) {
        for (String n : new String[]{"PLAY_ONE_MINUTE", "PLAY_TIME"}) {
            try {
                return p.getStatistic(Statistic.valueOf(n)) / 20L;
            } catch (Exception ignored) {
                // sprobuj nastepna nazwe
            }
        }
        return 0L;
    }

    private void flush(UUID id) {
        Long m = mark.get(id);
        if (m == null) return;
        long d = (System.currentTimeMillis() - m) / 1000L;
        if (d > 0) {
            seconds.merge(id, d, Long::sum);
            mark.put(id, m + d * 1000L);
            dirty = true;
        }
    }

    private void flushAll() {
        for (UUID id : new ArrayList<>(mark.keySet())) flush(id);
    }

    /** Aktualna ilosc sekund do wydania. */
    public long get(UUID id) {
        flush(id);
        return seconds.getOrDefault(id, 0L);
    }

    public void set(UUID id, long value) {
        flush(id);
        seconds.put(id, Math.max(0L, value));
        dirty = true;
    }

    public void add(UUID id, long delta) {
        set(id, get(id) + delta);
    }

    public void save() {
        flushAll();
        if (!dirty) return;
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : seconds.entrySet()) {
            y.set("czas." + e.getKey(), e.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Nie mozna zapisac data.yml: " + ex.getMessage());
        }
    }
}
