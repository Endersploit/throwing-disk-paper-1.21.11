package dev.throwingdisk;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class DiskItem {
    public static final NamespacedKey MODEL = new NamespacedKey("throwingdisk", "disk");

    private DiskItem() {}

    public static ItemStack create(ThrowingDiskPlugin plugin, int amount) {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("item.material", "ECHO_SHARD"));
        if (mat == null || !mat.isItem()) {
            mat = Material.ECHO_SHARD;
        }
        ItemStack stack = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        String name = plugin.getConfig().getString("item.name", "Throwing Disk");
        meta.displayName(Component.text(name, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Right-click to throw.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("It returns to you.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.setItemModel(MODEL);
        meta.setMaxStackSize(16);
        meta.getPersistentDataContainer().set(plugin.diskKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isDisk(ThrowingDiskPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(plugin.diskKey(), PersistentDataType.BYTE);
    }
}
