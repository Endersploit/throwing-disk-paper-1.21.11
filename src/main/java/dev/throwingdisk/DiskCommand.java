package dev.throwingdisk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class DiskCommand implements CommandExecutor, TabCompleter {
    private final ThrowingDiskPlugin plugin;

    public DiskCommand(ThrowingDiskPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            s.sendMessage("/throwingdisk <give [player] [amount]|reload|pack>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "give" -> {
                Player target = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player pl ? pl : null);
                if (target == null) {
                    s.sendMessage("Player not found.");
                    return true;
                }
                int amount = 1;
                if (a.length > 2) {
                    try {
                        amount = Math.max(1, Math.min(16, Integer.parseInt(a[2])));
                    } catch (NumberFormatException ex) {
                        s.sendMessage("Invalid amount.");
                        return true;
                    }
                }
                target.getInventory().addItem(DiskItem.create(plugin, amount))
                        .values().forEach(i -> target.getWorld().dropItem(target.getLocation(), i));
                s.sendMessage("Gave " + amount + " Throwing Disk to " + target.getName());
            }
            case "reload" -> {
                plugin.reloadConfig();
                s.sendMessage("ThrowingDisk config reloaded.");
            }
            case "pack" -> {
                try {
                    Path out = ResourcePackGenerator.generate(plugin.getDataFolder().toPath());
                    s.sendMessage("Resource pack written to " + out);
                } catch (IOException ex) {
                    s.sendMessage("Failed: " + ex.getMessage());
                }
            }
            default -> s.sendMessage("Unknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) {
            return List.of("give", "reload", "pack");
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
