package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;

/** Titulos, animacoes e efeitos visuais. */
public final class Fx {
    private Fx() {}

    public static void title(Player p, Component title, Component sub, int in, int stay, int out) {
        p.showTitle(Title.title(title, sub, Title.Times.times(
                Duration.ofMillis(in * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(out * 50L))));
    }

    /** Titulo com logo + subtitulo a aparecer letra a letra. */
    public static void typewriter(GulososClaims plugin, Player p, Component title, String text, NamedTextColor color) {
        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                i++;
                String shown = text.substring(0, Math.min(i, text.length()));
                p.showTitle(Title.title(title, Component.text(shown, color), Title.Times.times(
                        Duration.ZERO, Duration.ofMillis(2200), Duration.ofMillis(600))));
                if (i <= text.length()) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.2f, 1.8f);
                if (i >= text.length()) cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Logo pequeno no subtitulo (metade do tamanho de um titulo) com texto letra a letra. */
    public static void typewriterSub(GulososClaims plugin, Player p, String text, NamedTextColor color) {
        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                i++;
                String shown = text.substring(0, Math.min(i, text.length()));
                Component sub = Msg.glyph(Msg.LOGO_SUB).append(Component.text("  " + shown, color));
                p.showTitle(Title.title(Component.empty(), sub, Title.Times.times(
                        Duration.ZERO, Duration.ofMillis(1800), Duration.ofMillis(500))));
                if (i <= text.length()) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.2f, 1.8f);
                if (i >= text.length()) cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Pilar de particulas douradas num canto selecionado. */
    public static void pillar(GulososClaims plugin, Player p, Location corner) {
        World w = corner.getWorld();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 190, 0), 1.6f);
        new BukkitRunnable() {
            int n = 0;
            @Override public void run() {
                if (!p.isOnline() || n++ >= 15) { cancel(); return; }
                for (double y = 0; y <= 7; y += 0.5) {
                    p.spawnParticle(Particle.DUST, corner.getBlockX() + 0.5, corner.getBlockY() + y,
                            corner.getBlockZ() + 0.5, 1, 0, 0, 0, 0, dust);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    /** Desenha o limite de um retangulo com particulas, so para um jogador. */
    public static void border(GulososClaims plugin, Player p, int minX, int minZ, int maxX, int maxZ,
                              int seconds, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.4f);
        World w = p.getWorld();
        new BukkitRunnable() {
            int n = 0;
            @Override public void run() {
                if (!p.isOnline() || !p.getWorld().equals(w) || n++ >= seconds * 4) { cancel(); return; }
                double y = p.getLocation().getY() + 1.0;
                double x0 = minX, x1 = maxX + 1.0, z0 = minZ, z1 = maxZ + 1.0;
                double perimeter = 2 * ((x1 - x0) + (z1 - z0));
                double step = Math.max(1.0, perimeter / 160.0);
                Location pl = p.getLocation();
                for (double x = x0; x <= x1; x += step) { dot(p, dust, x, y, z0, pl); dot(p, dust, x, y, z1, pl); }
                for (double z = z0; z <= z1; z += step) { dot(p, dust, x0, y, z, pl); dot(p, dust, x1, y, z, pl); }
                for (double dy = -2; dy <= 5; dy += 1) {
                    dot(p, dust, x0, y + dy, z0, pl); dot(p, dust, x1, y + dy, z0, pl);
                    dot(p, dust, x0, y + dy, z1, pl); dot(p, dust, x1, y + dy, z1, pl);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private static void dot(Player p, Particle.DustOptions dust, double x, double y, double z, Location pl) {
        double dx = pl.getX() - x, dz = pl.getZ() - z;
        if (dx * dx + dz * dz > 40 * 40) return;
        p.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
    }

    /** Selo 3D: logo GulososSMP + pa dourada a rodar e a subir sobre o terreno criado. */
    public static void seal(GulososClaims plugin, Claim c) {
        World w = Bukkit.getWorld(c.world);
        if (w == null) return;
        int cx = c.centerX(), cz = c.centerZ();
        Location base = new Location(w, cx + 0.5, w.getHighestBlockYAt(cx, cz) + 2.5, cz + 0.5);

        ItemDisplay item = w.spawn(base, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.GOLDEN_SHOVEL));
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(2.2f, 2.2f, 2.2f), new AxisAngle4f()));
        });
        TextDisplay text = w.spawn(base.clone().add(0, 1.7, 0), TextDisplay.class, t -> {
            t.text(Msg.glyph(Msg.LOGO_SUB)
                    .append(Component.newline())
                    .append(Component.text(c.name, NamedTextColor.GOLD)));
            t.setBillboard(Display.Billboard.CENTER);
            t.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            t.setPersistent(false);
            t.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(2.5f, 2.5f, 2.5f), new AxisAngle4f()));
        });

        w.playSound(base, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        new BukkitRunnable() {
            int t = 0;
            float angle = 0;
            @Override public void run() {
                if (!item.isValid()) { text.remove(); cancel(); return; }
                t++;
                angle += (float) (Math.PI / 2);
                item.setInterpolationDelay(0);
                item.setInterpolationDuration(10);
                item.setTransformation(new Transformation(
                        new Vector3f(0, Math.min(t, 10) * 0.12f, 0),
                        new AxisAngle4f(angle, 0, 1, 0),
                        new Vector3f(2.2f, 2.2f, 2.2f), new AxisAngle4f()));
                w.spawnParticle(Particle.END_ROD, base.clone().add(0, 0.6, 0), 8, 0.5, 0.5, 0.5, 0.03);
                w.spawnParticle(Particle.FIREWORK, base.clone().add(0, 0.6, 0), 4, 0.6, 0.3, 0.6, 0.05);
                if (t >= 14) { item.remove(); text.remove(); cancel(); }
            }
        }.runTaskTimer(plugin, 1L, 10L);
    }

    /** Contagem decrescente de teleporte com logo no titulo. */
    public static void teleportCountdown(GulososClaims plugin, Player p, Location dest, String name) {
        Location start = p.getLocation();
        Msg.send(p, "<gray>A teletransportar para <gold><n></gold>... <dark_gray>(não te mexas)", Msg.p("n", name));
        new BukkitRunnable() {
            int left = 3;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (p.getWorld() != start.getWorld() || p.getLocation().distanceSquared(start) > 0.6) {
                    Msg.send(p, "<red>Teleporte cancelado porque te mexeste.");
                    cancel();
                    return;
                }
                if (left == 0) {
                    p.teleportAsync(dest).thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        p.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.2);
                    }));
                    cancel();
                    return;
                }
                title(p, Msg.glyph(Msg.LOGO_24), Component.text(String.valueOf(left), NamedTextColor.YELLOW), 0, 22, 0);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 0.8f + (3 - left) * 0.3f);
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static void successSound(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }

    public static void errorSound(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }
}
