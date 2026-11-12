package pt.gulosos.claims;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClaimManager {

    public enum Result { OK, OTHER_WORLD, BLOCKED_WORLD, TOO_SMALL, NO_BLOCKS, OVERLAP, MAX_CLAIMS }

    public record Check(Result result, int area, Claim overlapped) {}

    private final GulososClaims plugin;
    private final File file;
    private final Map<Integer, Claim> claims = new LinkedHashMap<>();
    /** mundo -> chunk -> claims (indice espacial) */
    private final Map<String, Map<Long, List<Claim>>> index = new HashMap<>();
    private final Map<UUID, Double> accrued = new HashMap<>();
    private final Map<UUID, Integer> bonus = new HashMap<>();
    private int nextId = 1;
    private boolean dirty = false;

    public ClaimManager(GulososClaims plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dados.yml");
        load();
    }

    // ---------- blocos ----------

    public int totalBlocks(UUID id) {
        int max = plugin.getConfig().getInt("blocos.maximo-total", 100000);
        int earned = (int) Math.min(accrued.getOrDefault(id, 0.0), max);
        return plugin.getConfig().getInt("blocos.iniciais", 200) + earned + bonus.getOrDefault(id, 0);
    }

    public int usedBlocks(UUID id) {
        int sum = 0;
        for (Claim c : claims.values()) if (c.owner.equals(id)) sum += c.area();
        return sum;
    }

    public int remainingBlocks(UUID id) {
        return totalBlocks(id) - usedBlocks(id);
    }

    public void addBonus(UUID id, int amount) {
        bonus.merge(id, amount, Integer::sum);
        dirty = true;
    }

    public void accrueOnline() {
        double perMinute = plugin.getConfig().getDouble("blocos.por-hora", 100) / 60.0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            accrued.merge(p.getUniqueId(), perMinute, Double::sum);
        }
        dirty = true;
    }

    // ---------- consultas ----------

    public Claim get(int id) { return claims.get(id); }

    public List<Claim> claimsOf(UUID owner) {
        List<Claim> list = new ArrayList<>();
        for (Claim c : claims.values()) if (c.owner.equals(owner)) list.add(c);
        return list;
    }

    public Claim getAt(Location l) {
        if (l == null || l.getWorld() == null) return null;
        return getAt(l.getWorld().getName(), l.getBlockX(), l.getBlockZ());
    }

    public Claim getAt(String world, int x, int z) {
        Map<Long, List<Claim>> w = index.get(world);
        if (w == null) return null;
        List<Claim> list = w.get(key(x >> 4, z >> 4));
        if (list == null) return null;
        for (Claim c : list) if (c.contains(world, x, z)) return c;
        return null;
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    // ---------- criar / apagar ----------

    public Check check(Player p, Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return new Check(Result.OTHER_WORLD, 0, null);
        String world = a.getWorld().getName();
        if (plugin.getConfig().getStringList("terrenos.mundos-bloqueados").contains(world))
            return new Check(Result.BLOCKED_WORLD, 0, null);

        int x1 = Math.min(a.getBlockX(), b.getBlockX()), x2 = Math.max(a.getBlockX(), b.getBlockX());
        int z1 = Math.min(a.getBlockZ(), b.getBlockZ()), z2 = Math.max(a.getBlockZ(), b.getBlockZ());
        int w = x2 - x1 + 1, l = z2 - z1 + 1, area = w * l;
        int min = plugin.getConfig().getInt("terrenos.tamanho-minimo", 5);
        if (w < min || l < min) return new Check(Result.TOO_SMALL, area, null);

        if (claimsOf(p.getUniqueId()).size() >= plugin.getConfig().getInt("terrenos.maximo-por-jogador", 10))
            return new Check(Result.MAX_CLAIMS, area, null);

        for (Claim c : claims.values()) {
            if (c.overlaps(world, x1, z1, x2, z2)) return new Check(Result.OVERLAP, area, c);
        }
        if (area > remainingBlocks(p.getUniqueId())) return new Check(Result.NO_BLOCKS, area, null);
        return new Check(Result.OK, area, null);
    }

    public Claim create(Player p, Location a, Location b) {
        Claim c = new Claim(nextId++);
        c.owner = p.getUniqueId();
        c.ownerName = p.getName();
        c.world = a.getWorld().getName();
        c.minX = Math.min(a.getBlockX(), b.getBlockX());
        c.maxX = Math.max(a.getBlockX(), b.getBlockX());
        c.minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        c.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
        c.name = "Terreno #" + c.id;
        claims.put(c.id, c);
        indexAdd(c);
        dirty = true;
        save();
        return c;
    }

    public void delete(Claim c) {
        claims.remove(c.id);
        indexRemove(c);
        dirty = true;
        save();
    }

    public void markDirty() { dirty = true; }

    private void indexAdd(Claim c) {
        Map<Long, List<Claim>> w = index.computeIfAbsent(c.world, k -> new HashMap<>());
        for (int cx = c.minX >> 4; cx <= c.maxX >> 4; cx++)
            for (int cz = c.minZ >> 4; cz <= c.maxZ >> 4; cz++)
                w.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(c);
    }

    private void indexRemove(Claim c) {
        Map<Long, List<Claim>> w = index.get(c.world);
        if (w == null) return;
        for (int cx = c.minX >> 4; cx <= c.maxX >> 4; cx++)
            for (int cz = c.minZ >> 4; cz <= c.maxZ >> 4; cz++) {
                List<Claim> list = w.get(key(cx, cz));
                if (list != null) {
                    list.remove(c);
                    if (list.isEmpty()) w.remove(key(cx, cz));
                }
            }
    }

    // ---------- guardar / carregar ----------

    public void saveIfDirty() { if (dirty) save(); }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("proximo-id", nextId);
        for (Claim c : claims.values()) {
            String p = "terrenos." + c.id;
            y.set(p + ".dono", c.owner.toString());
            y.set(p + ".dono-nome", c.ownerName);
            y.set(p + ".mundo", c.world);
            y.set(p + ".minX", c.minX);
            y.set(p + ".minZ", c.minZ);
            y.set(p + ".maxX", c.maxX);
            y.set(p + ".maxZ", c.maxZ);
            y.set(p + ".nome", c.name);
            y.set(p + ".criado", c.created);
            y.set(p + ".pvp", c.pvp);
            y.set(p + ".explosoes", c.explosions);
            y.set(p + ".fogo", c.fire);
            y.set(p + ".mobs", c.mobGrief);
            List<String> t = new ArrayList<>();
            for (UUID u : c.trusted) t.add(u.toString());
            y.set(p + ".membros", t);
        }
        for (Map.Entry<UUID, Double> e : accrued.entrySet()) y.set("acumulados." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, Integer> e : bonus.entrySet()) y.set("bonus." + e.getKey(), e.getValue());
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Erro a guardar dados.yml: " + ex.getMessage());
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        nextId = y.getInt("proximo-id", 1);
        ConfigurationSection s = y.getConfigurationSection("terrenos");
        if (s != null) {
            for (String k : s.getKeys(false)) {
                ConfigurationSection c = s.getConfigurationSection(k);
                if (c == null) continue;
                Claim claim = new Claim(Integer.parseInt(k));
                claim.owner = UUID.fromString(c.getString("dono"));
                claim.ownerName = c.getString("dono-nome", "?");
                claim.world = c.getString("mundo");
                claim.minX = c.getInt("minX");
                claim.minZ = c.getInt("minZ");
                claim.maxX = c.getInt("maxX");
                claim.maxZ = c.getInt("maxZ");
                claim.name = c.getString("nome", "Terreno #" + k);
                claim.created = c.getLong("criado");
                claim.pvp = c.getBoolean("pvp");
                claim.explosions = c.getBoolean("explosoes");
                claim.fire = c.getBoolean("fogo");
                claim.mobGrief = c.getBoolean("mobs");
                for (String u : c.getStringList("membros")) claim.trusted.add(UUID.fromString(u));
                claims.put(claim.id, claim);
                indexAdd(claim);
                nextId = Math.max(nextId, claim.id + 1);
            }
        }
        ConfigurationSection a = y.getConfigurationSection("acumulados");
        if (a != null) for (String k : a.getKeys(false)) accrued.put(UUID.fromString(k), a.getDouble(k));
        ConfigurationSection b = y.getConfigurationSection("bonus");
        if (b != null) for (String k : b.getKeys(false)) bonus.put(UUID.fromString(k), b.getInt(k));
        plugin.getLogger().info("Carregados " + claims.size() + " terrenos.");
    }
}
