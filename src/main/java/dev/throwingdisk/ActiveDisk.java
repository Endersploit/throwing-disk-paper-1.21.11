package dev.throwingdisk;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

final class ActiveDisk {
    final UUID owner;
    final ItemDisplay display;
    final ItemStack stack;
    final Location loc;
    final Vector vel;
    final Set<UUID> hit = new HashSet<>();
    boolean returning;
    int age;
    double traveled;
    double angle;

    ActiveDisk(UUID owner, ItemDisplay display, ItemStack stack, Location loc, Vector vel) {
        this.owner = owner;
        this.display = display;
        this.stack = stack;
        this.loc = loc;
        this.vel = vel;
    }
}
