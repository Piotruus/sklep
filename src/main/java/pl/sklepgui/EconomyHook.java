package pl.sklepgui;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Cienka nakladka na Vault. Ladowana tylko gdy Vault jest na serwerze. */
final class EconomyHook {

    private final Economy eco;

    private EconomyHook(Economy eco) {
        this.eco = eco;
    }

    static EconomyHook find() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : new EconomyHook(rsp.getProvider());
    }

    boolean has(Player p, double amount) {
        return eco.has(p, amount);
    }

    boolean withdraw(Player p, double amount) {
        return eco.withdrawPlayer(p, amount).transactionSuccess();
    }

    boolean deposit(Player p, double amount) {
        return eco.depositPlayer(p, amount).transactionSuccess();
    }
}
