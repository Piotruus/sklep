package pl.sklepgui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Blokuje wszelkie wyjmowanie przedmiotow z naszych GUI i przekazuje klikniecia do Menu. */
final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getInventory();
        if (!(top.getHolder() instanceof Menu menu)) return;
        e.setCancelled(true);
        Inventory clicked = e.getClickedInventory();
        if (clicked == null || clicked != top) return;
        menu.handle(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) {
            e.setCancelled(true);
        }
    }
}
