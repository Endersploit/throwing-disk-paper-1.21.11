# Throwing Disk (Paper 1.21.11)

Server-side Paper plugin. Right-click a Throwing Disk to throw it; it flies with gravity, spins (ItemDisplay entity), damages entities, bounces off blocks, and returns to you like a boomerang.

## Install
1. Download the JAR from the latest **Actions** run (artifact `ThrowingDisk-jar`) or from **Releases**.
2. Put it in `plugins/` on a Paper 1.21.11 server (Java 21) and start.
3. Run `/throwingdisk pack` to generate `plugins/ThrowingDisk/ThrowingDiskPack.zip` (custom model + texture). Host it and set `resource-pack` / `resource-pack-sha1` in `server.properties`. Without the pack the item shows as an echo shard.
4. `/throwingdisk give [player] [amount]`.

## Commands / permissions
- `/throwingdisk give|reload|pack` (alias `/tdisk`), permission `throwingdisk.admin` (op).
- `throwingdisk.use` (default true) allows throwing.

## Config
See `config.yml`: speed, gravity, range, return speed, damage, spin speed, scale, volume, max active disks.

## Anti-dupe
The item is removed from the hand on throw and exists only as a display entity; it is handed back on catch, owner quit/death, flight timeout, or plugin disable. Disks cannot be used in crafting.

## Build
Push to `main` builds automatically; tag `vX.Y.Z` creates a release. Locally: JDK 21 + Gradle 8.14, run `build.bat` or `gradle build`. JAR: `build/libs/`. (Gradle wrapper JAR is binary and not included; run `gradle wrapper` once to generate it.)
