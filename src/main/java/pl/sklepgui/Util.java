package pl.sklepgui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Narzedzia: kolory, czas, budowanie przedmiotow z configu, operacje na ekwipunku. */
public final class Util {

    private static final DecimalFormat DF = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final Pattern TIME_PART = Pattern.compile("(\\d+)([dhms])");

    private Util() {}

    // ------------------------------------------------------------ tekst

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String strip(String s) {
        return s == null ? "" : ChatColor.stripColor(color(s));
    }

    public static String num(double v) {
        return DF.format(v);
    }

    public static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String apply(String s, Map<String, String> ph) {
        if (s == null) return "";
        if (ph != null) {
            for (Map.Entry<String, String> e : ph.entrySet()) {
                s = s.replace(e.getKey(), e.getValue());
            }
        }
        return s;
    }

    /** ph("%a%", "1", "%b%", "2") */
    public static Map<String, String> ph(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    public static String prettyMaterial(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String part : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ czas

    /** "4h", "2d", "1d12h30m", "90" (sekundy) -> sekundy; -1 gdy blad. */
    public static long parseTime(String in) {
        if (in == null) return -1;
        String s = in.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (s.isEmpty()) return -1;
        try {
            if (s.matches("\\d+")) return Long.parseLong(s);
            Matcher m = TIME_PART.matcher(s);
            long total = 0;
            int end = 0;
            while (m.find()) {
                if (m.start() != end) return -1;
                long n = Long.parseLong(m.group(1));
                switch (m.group(2).charAt(0)) {
                    case 'd' -> total += n * 86400L;
                    case 'h' -> total += n * 3600L;
                    case 'm' -> total += n * 60L;
                    default -> total += n;
                }
                end = m.end();
            }
            return (end == s.length() && end > 0) ? total : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static String formatTime(long sec) {
        if (sec <= 0) return "0s";
        long d = sec / 86400L;
        long h = (sec % 86400L) / 3600L;
        long m = (sec % 3600L) / 60L;
        long s = sec % 60L;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0) sb.append(s).append("s ");
        return sb.toString().trim();
    }

    // ------------------------------------------------------------ przedmioty

    @SuppressWarnings("deprecation")
    public static Enchantment enchant(String name) {
        if (name == null) return null;
        return Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).trim()));
    }

    /**
     * Buduje przedmiot z sekcji configu.
     * Obslugiwane klucze: material, amount, name, lore, enchants, stored-enchants,
     * custom-model-data, flags, glow, skull-owner.
     * cosmetic=false pomija name/lore/flags/glow (przedmiot "czysty", np. do wydania graczowi).
     */
    @SuppressWarnings("deprecation")
    public static ItemStack item(ConfigurationSection s, Map<String, String> ph, boolean cosmetic) {
        String matName = s.getString("material", "STONE");
        Material m = Material.matchMaterial(matName);
        if (m == null || m.isAir() || !m.isItem()) {
            Bukkit.getLogger().warning("[SklepGUI] Nieznany material '" + matName + "' w " + s.getCurrentPath());
            m = Material.BARRIER;
        }
        ItemStack it = new ItemStack(m, Math.max(1, s.getInt("amount", 1)));
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        ConfigurationSection en = s.getConfigurationSection("enchants");
        if (en != null) {
            for (String k : en.getKeys(false)) {
                Enchantment e = enchant(k);
                if (e == null) {
                    Bukkit.getLogger().warning("[SklepGUI] Nieznane zaklecie '" + k + "' w " + s.getCurrentPath());
                    continue;
                }
                meta.addEnchant(e, en.getInt(k), true);
            }
        }
        ConfigurationSection st = s.getConfigurationSection("stored-enchants");
        if (st != null && meta instanceof EnchantmentStorageMeta sm) {
            for (String k : st.getKeys(false)) {
                Enchantment e = enchant(k);
                if (e == null) {
                    Bukkit.getLogger().warning("[SklepGUI] Nieznane zaklecie '" + k + "' w " + s.getCurrentPath());
                    continue;
                }
                sm.addStoredEnchant(e, st.getInt(k), true);
            }
        }
        if (s.contains("custom-model-data")) {
            meta.setCustomModelData(s.getInt("custom-model-data"));
        }
        if (cosmetic) {
            for (String f : s.getStringList("flags")) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(f.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    Bukkit.getLogger().warning("[SklepGUI] Nieznana flaga '" + f + "' w " + s.getCurrentPath());
                }
            }
            if (s.getBoolean("glow", false)) {
                meta.setEnchantmentGlintOverride(true);
            }
        }
        String owner = s.getString("skull-owner");
        if (owner != null && !owner.isEmpty() && meta instanceof SkullMeta sk) {
            sk.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        }
        it.setItemMeta(meta);
        if (cosmetic) {
            decorate(it, s.getString("name"), s.getStringList("lore"), ph);
        }
        return it;
    }

    /** Ustawia nazwe i lore (z kolorami i placeholderami); &r na poczatku usuwa domyslna kursywe. */
    @SuppressWarnings("deprecation")
    public static void decorate(ItemStack it, String name, List<String> lore, Map<String, String> ph) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(ChatColor.RESET + color(apply(name, ph)));
        }
        if (lore != null && !lore.isEmpty()) {
            List<String> l = new ArrayList<>();
            for (String line : lore) l.add(ChatColor.RESET + color(apply(line, ph)));
            meta.setLore(l);
        }
        it.setItemMeta(meta);
    }

    // ------------------------------------------------------------ ekwipunek

    /** Czy wszystkie przedmioty zmieszcza sie w ekwipunku (symulacja na kopii). */
    public static boolean canFit(Player p, Collection<ItemStack> items) {
        ItemStack[] cur = p.getInventory().getStorageContents();
        ItemStack[] copy = new ItemStack[36];
        for (int i = 0; i < 36 && i < cur.length; i++) {
            copy[i] = cur[i] == null ? null : cur[i].clone();
        }
        Inventory tmp = Bukkit.createInventory(null, 36);
        tmp.setContents(copy);
        for (ItemStack it : items) {
            if (!tmp.addItem(it.clone()).isEmpty()) return false;
        }
        return true;
    }

    public static int count(Player p, Predicate<ItemStack> f) {
        int n = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) {
            if (it != null && f.test(it)) n += it.getAmount();
        }
        return n;
    }

    /** Usuwa do 'amount' pasujacych przedmiotow; zwraca ile faktycznie usunieto. */
    public static int remove(Player p, Predicate<ItemStack> f, int amount) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] c = inv.getStorageContents();
        int left = amount;
        for (int i = 0; i < c.length && left > 0; i++) {
            ItemStack it = c[i];
            if (it == null || !f.test(it)) continue;
            int take = Math.min(left, it.getAmount());
            left -= take;
            if (take >= it.getAmount()) {
                inv.setItem(i, null);
            } else {
                it.setAmount(it.getAmount() - take);
                inv.setItem(i, it);
            }
        }
        return amount - left;
    }

    public static void give(Player p, Collection<ItemStack> items) {
        for (ItemStack it : items) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(it.clone());
            for (ItemStack rest : left.values()) {
                p.getWorld().dropItem(p.getLocation(), rest);
            }
        }
    }
}
