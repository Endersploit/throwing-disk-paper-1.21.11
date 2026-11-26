package dev.throwingdisk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class DiskManager {
    private final ThrowingDiskPlugin plugin;
    private final Set<ActiveDisk> disks = new LinkedHashSet<>();
    private BukkitTask task;

    public DiskManager(ThrowingDiskPlugin plugin) {
        this.plugin = plugin;
    }

    public int activeCount(UUID owner) {
        int n = 0;
        for (ActiveDisk d : disks) {
            if (d.owner.equals(owner)) {
                n++;
            }
        }
        return n;
    }

    public boolean canThrow(Player p) {
        return activeCount(p.getUniqueId()) < plugin.getConfig().getInt("throw.max-active-per-player", 2);
    }

    public void launch(Player p, ItemStack template) {
        FileConfiguration c = plugin.getConfig();
        ItemStack single = template.clone();
        single.setAmount(1);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        Location start = p.getEyeLocation().add(dir.clone().multiply(0.6)).subtract(0, 0.25, 0);
        Vector vel = dir.multiply(c.getDouble("throw.speed", 1.6));
        float scale = (float) c.getDouble("visual.scale", 0.9);
        float view = (float) c.getDouble("visual.view-range", 2.0);
        ItemDisplay display = start.getWorld().spawn(start, ItemDisplay.class, d -> {
            d.setItemStack(single);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setGravity(false);
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(1);
            d.setTeleportDuration(1);
            d.setViewRange(view);
            d.setTransformation(transform(0, scale));
        });
        ActiveDisk disk = new ActiveDisk(p.getUniqueId(), display, single, start.clone(), vel);
        disks.add(disk);
        sound(start, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.3f);
        ensureTask();
    }

    private Transformation transform(double angleDeg, float scale) {
        Quaternionf flat = new Quaternionf().rotateX((float) Math.toRadians(90));
        Quaternionf spin = new Quaternionf().rotateY((float) Math.toRadians(angleDeg));
        return new Transformation(new Vector3f(), spin, new Vector3f(scale, scale, scale), flat);
    }

    private void ensureTask() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    private void tick() {
        Iterator<ActiveDisk> it = disks.iterator();
        while (it.hasNext()) {
            ActiveDisk d = it.next();
            boolean done;
            try {
                done = step(d);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Disk error, returning item: " + ex);
                finish(d, false);
                done = true;
            }
            if (done) {
                it.remove();
            }
        }
        if (disks.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    /** @return true when the disk is finished and should be removed. */
    private boolean step(ActiveDisk d) {
        FileConfiguration c = plugin.getConfig();
        Player owner = Bukkit.getPlayer(d.owner);
        World w = d.loc.getWorld();
        if (!d.display.isValid() || owner == null || !owner.isOnline() || owner.isDead()
                || owner.getWorld() != w) {
            finish(d, false);
            return true;
        }
        d.age++;
        if (d.age > c.getInt("throw.max-flight-ticks", 200)) {
            finish(d, true);
            return true;
        }
        double rSpeed = c.getDouble("throw.return-speed", 1.4);
        if (!d.returning) {
            d.vel.setY(d.vel.getY() - c.getDouble("throw.gravity", 0.006));
            if (d.traveled >= c.getDouble("throw.max-range", 22.0)) {
                startReturn(d, false);
            }
        } else {
            Location target = owner.getLocation().add(0, 1.0, 0);
            Vector to = target.toVector().subtract(d.loc.toVector());
            if (to.length() < c.getDouble("throw.catch-distance", 1.3) + 0.0) {
                finish(d, true);
                return true;
            }
            Vector desired = to.normalize().multiply(rSpeed);
            d.vel.multiply(0.6).add(desired.multiply(0.4));
        }
        double speed = d.vel.length();
        if (speed < 1.0E-4) {
            return false;
        }
        Vector dir = d.vel.clone().normalize();
        double moveDist = speed;

        if (!d.returning) {
            RayTraceResult blk = w.rayTraceBlocks(d.loc, dir, speed, FluidCollisionMode.NEVER, true);
            if (blk != null && blk.getHitPosition() != null) {
                moveDist = Math.max(0, d.loc.toVector().distance(blk.getHitPosition()) - 0.1);
                hitEntities(d, owner, dir, moveDist);
                d.loc.add(dir.clone().multiply(moveDist));
                impactEffects(d.loc, Particle.CRIT, Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);
                startReturn(d, true);
                updateVisual(d);
                return false;
            }
        }
        hitEntities(d, owner, dir, moveDist);
        d.loc.add(dir.clone().multiply(moveDist));
        if (!d.returning) {
            d.traveled += moveDist;
        }
        w.spawnParticle(Particle.CRIT, d.loc, 2, 0.05, 0.05, 0.05, 0.0);
        if (d.age % 3 == 0) {
            w.spawnParticle(Particle.END_ROD, d.loc, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (d.age % 4 == 0) {
            sound(d.loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.35f, d.returning ? 1.6f : 1.2f);
        }
        updateVisual(d);
        return false;
    }

    private void startReturn(ActiveDisk d, boolean bounce) {
        d.returning = true;
        if (plugin.getConfig().getBoolean("damage.hit-again-on-return", true)) {
            d.hit.clear();
        }
        if (bounce) {
            d.vel.multiply(-0.3);
        }
        sound(d.loc, Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.4f);
    }

    private void hitEntities(ActiveDisk d, Player owner, Vector dir, double dist) {
        World w = d.loc.getWorld();
        double damage = plugin.getConfig().getDouble("damage.amount", 6.0);
        for (int i = 0; i < 4; i++) {
            RayTraceResult r = w.rayTraceEntities(d.loc, dir, Math.max(dist, 0.01), 0.35, e -> valid(e, d));
            if (r == null || r.getHitEntity() == null) {
                return;
            }
            Entity e = r.getHitEntity();
            d.hit.add(e.getUniqueId());
            if (e instanceof LivingEntity le) {
                le.damage(damage, owner);
            }
            Location at = r.getHitPosition() == null ? e.getLocation() : r.getHitPosition().toLocation(w);
            impactEffects(at, Particle.ENCHANTED_HIT, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 1.3f);
        }
    }

    private boolean valid(Entity e, ActiveDisk d) {
        return e instanceof LivingEntity
                && !(e instanceof ArmorStand)
                && !e.getUniqueId().equals(d.owner)
                && !d.hit.contains(e.getUniqueId());
    }

    private void impactEffects(Location at, Particle particle, Sound s, float vol, float pitch) {
        at.getWorld().spawnParticle(particle, at, 10, 0.2, 0.2, 0.2, 0.1);
        sound(at, s, vol, pitch);
    }

    private void sound(Location at, Sound s, float vol, float pitch) {
        float v = (float) plugin.getConfig().getDouble("sound.volume", 1.0) * vol;
        at.getWorld().playSound(at, s, SoundCategory.PLAYERS, v, pitch);
    }

    private void updateVisual(ActiveDisk d) {
        d.angle = (d.angle + plugin.getConfig().getDouble("visual.spin-degrees-per-tick", 55.0)) % 360.0;
        float scale = (float) plugin.getConfig().getDouble("visual.scale", 0.9);
        d.display.setInterpolationDelay(0);
        d.display.setInterpolationDuration(1);
        d.display.setTransformation(transform(d.angle, scale));
        d.display.teleport(d.loc);
    }

    private void finish(ActiveDisk d, boolean caught) {
        Location where = d.display.isValid() ? d.display.getLocation() : d.loc;
        d.display.remove();
        Player owner = Bukkit.getPlayer(d.owner);
        if (owner != null && owner.isOnline() && !owner.isDead() && owner.getWorld() == where.getWorld()) {
            var left = owner.getInventory().addItem(d.stack.clone());
            for (ItemStack s : left.values()) {
                owner.getWorld().dropItem(owner.getLocation(), s);
            }
            if (caught) {
                sound(owner.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
            }
        } else {
            where.getWorld().dropItem(where, d.stack.clone());
        }
    }

    /** Returns every disk owned by the player right now (used on quit). */
    public void recall(Player p) {
        Iterator<ActiveDisk> it = disks.iterator();
        while (it.hasNext()) {
            ActiveDisk d = it.next();
            if (d.owner.equals(p.getUniqueId())) {
                finish(d, false);
                it.remove();
            }
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ActiveDisk d : new ArrayList<>(disks)) {
            finish(d, false);
        }
        disks.clear();
    }
}
