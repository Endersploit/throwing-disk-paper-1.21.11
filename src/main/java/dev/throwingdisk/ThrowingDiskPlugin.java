package dev.throwingdisk;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThrowingDiskPlugin extends JavaPlugin {
    private NamespacedKey diskKey;
    private DiskManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        diskKey = new NamespacedKey(this, "throwing_disk");
        manager = new DiskManager(this);
        getServer().getPluginManager().registerEvents(new DiskListener(this, manager), this);
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
    }

    public NamespacedKey diskKey() {
        return diskKey;
    }
}
