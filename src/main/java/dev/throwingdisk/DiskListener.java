package dev.throwingdisk;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class DiskListener implements Listener {
    private final ThrowingDiskPlugin plugin;
    private final DiskManager manager;

    public DiskListener(ThrowingDiskPlugin plugin, DiskManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = e.getHand();
        if (hand == null) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack held = p.getInventory().getItem(hand);
        if (!DiskItem.isDisk(plugin, held)) {
            return;
        }
        e.setCancelled(true);
        if (!p.hasPermission("throwingdisk.use") || !manager.canThrow(p)) {
            return;
        }
        manager.launch(p, held);
        if (held.getAmount() <= 1) {
            p.getInventory().setItem(hand, null);
        } else {
            ItemStack rest = held.clone();
            rest.setAmount(held.getAmount() - 1);
            p.getInventory().setItem(hand, rest);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.recall(e.getPlayer());
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack s : e.getInventory().getMatrix()) {
            if (DiskItem.isDisk(plugin, s)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }
}
