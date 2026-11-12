package pt.gulosos.claims;

import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Claim {
    public final int id;
    public UUID owner;
    public String ownerName;
    public String world;
    public int minX, minZ, maxX, maxZ;
    public String name;
    public final Set<UUID> trusted = new LinkedHashSet<>();
    public boolean pvp = false;
    public boolean explosions = false;
    public boolean fire = false;
    public boolean mobGrief = false;
    public long created = System.currentTimeMillis();

    public Claim(int id) {
        this.id = id;
    }

    public int width()  { return maxX - minX + 1; }
    public int length() { return maxZ - minZ + 1; }
    public int area()   { return width() * length(); }

    public boolean contains(String w, int x, int z) {
        return world.equals(w) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean contains(Location l) {
        return l.getWorld() != null && contains(l.getWorld().getName(), l.getBlockX(), l.getBlockZ());
    }

    public boolean overlaps(String w, int x1, int z1, int x2, int z2) {
        return world.equals(w) && !(x2 < minX || x1 > maxX || z2 < minZ || z1 > maxZ);
    }

    public int centerX() { return (minX + maxX) / 2; }
    public int centerZ() { return (minZ + maxZ) / 2; }
}
