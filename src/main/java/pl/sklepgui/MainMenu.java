package pl.sklepgui;

/** Glowne GUI /sklep - wszystko definiowane w config.yml (main-menu). */
final class MainMenu extends Menu {

    MainMenu(SklepPlugin plugin) {
        super(plugin, plugin.getConfig().getString("main-menu.title", "&7Sklep"),
                plugin.getConfig().getInt("main-menu.rows", 5));
    }

    @Override
    protected void render() {
        staticItems(plugin.getConfig().getConfigurationSection("main-menu.items"), null);
    }
}
