package dev.throwingdisk;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThrowingDiskPlugin extends JavaPlugin {
    private NamespacedKey diskKey;
    private DiskManager manager;
    private PackServer packServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        diskKey = new NamespacedKey(this, "throwing_disk");
        manager = new DiskManager(this);
        if (getConfig().getBoolean("resourcepack.enabled", true)) {
            packServer = new PackServer(this);
            packServer.start();
        }
        getServer().getPluginManager().registerEvents(new DiskListener(this, manager, packServer), this);
        PluginCommand cmd = getCommand("throwingdisk");
        if (cmd != null) {
            DiskCommand handler = new DiskCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
        if (packServer != null) {
            packServer.stop();
        }
    }

    public NamespacedKey diskKey() {
        return diskKey;
    }
}
