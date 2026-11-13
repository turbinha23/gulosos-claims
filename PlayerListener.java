package pt.gulosos.claims;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Boas-vindas, entrar/sair de terrenos e chat (tags + pedidos de texto dos menus). */
public class PlayerListener implements Listener {

    private static final String[] TAG_ORDER = {"founder", "admin", "mod", "helper", "builder", "vip"};
    private static final String[] TAG_GLYPH = {Msg.TAG_FOUNDER, Msg.TAG_ADMIN, Msg.TAG_MOD,
            Msg.TAG_HELPER, Msg.TAG_BUILDER, Msg.TAG_VIP};

    private final GulososClaims plugin;
    private final Map<UUID, Integer> lastClaim = new HashMap<>();

    public PlayerListener(GulososClaims plugin) { this.plugin = plugin; }

    // ---------- entrada / saida do servidor ----------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Claim c = plugin.claims().getAt(p.getLocation());
        lastClaim.put(p.getUniqueId(), c == null ? -1 : c.id);
        if (plugin.getConfig().getBoolean("titulos.boas-vindas", true)) {
            // pequeno atraso para o resource pack ja estar carregado
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline())
                    Fx.typewriter(plugin, p, Msg.glyph(Msg.LOGO_24), "Bem-vindo, " + p.getName() + "!", NamedTextColor.YELLOW);
            }, 80L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastClaim.remove(e.getPlayer().getUniqueId());
        plugin.cancelPrompt(e.getPlayer().getUniqueId());
    }

    // ---------- entrar / sair de terrenos ----------

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedBlock()) return;
        update(e.getPlayer(), e.getTo());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        update(e.getPlayer(), e.getTo());
    }

    private void update(Player p, Location to) {
        Claim c = plugin.claims().getAt(to);
        int id = c == null ? -1 : c.id;
        int prev = lastClaim.getOrDefault(p.getUniqueId(), -1);
        if (id == prev) return;
        lastClaim.put(p.getUniqueId(), id);

        if (c != null) {
            boolean own = c.owner.equals(p.getUniqueId());
            if (plugin.getConfig().getBoolean("titulos.entrar-terreno", true)) {
                String text = own ? "O teu terreno" : "Terreno de " + c.ownerName;
                NamedTextColor col = own ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
                String mode = plugin.getConfig().getString("titulos.entrar-terreno-modo", "subtitulo");
                if (mode.equalsIgnoreCase("actionbar")) {
                    p.sendActionBar(Msg.prefix().append(Component.text(text, col)));
                } else {
                    Fx.typewriterSub(plugin, p, text, col);
                }
            }
            if (own && !plugin.getConfig().getString("titulos.entrar-terreno-modo", "subtitulo").equalsIgnoreCase("actionbar")) {
                Msg.actionBar(p, "<gold><n> <dark_gray>| <gray>Blocos restantes: <white><rest>",
                        Msg.p("n", c.name), Msg.p("rest", plugin.claims().remainingBlocks(p.getUniqueId())));
            }
        } else {
            Claim old = plugin.claims().get(prev);
            if (old != null) {
                Msg.actionBar(p, "<gray>Saíste do terreno de <white><n>", Msg.p("n", old.ownerName));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.7f);
            }
        }
    }

    // ---------- chat ----------

    /** Pedidos de texto feitos pelos menus (renomear, adicionar membro). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrompt(AsyncChatEvent e) {
        Player p = e.getPlayer();
        Consumer<String> cb = plugin.takePrompt(p.getUniqueId());
        if (cb == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancelar")) Msg.send(p, "<yellow>Cancelado.");
            else cb.accept(text);
        });
    }

    /** Formato do chat: tag + nome (com estilo para FOUNDER/ADMIN) + mensagem. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("chat.tags", true)) return;
        boolean special = plugin.getConfig().getBoolean("chat.nomes-especiais", true);
        e.renderer(ChatRenderer.viewerUnaware((source, name, message) -> {
            int role = roleIndex(source);
            Component shown = name;
            if (special && role >= 0 && role <= 1) {
                String plain = PlainTextComponentSerializer.plainText().serialize(name);
                shown = role == 0
                        ? NameStyle.founder(plain, ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextInt(plain.length() + 4) - 2)
                        : NameStyle.admin(plain, 0.10, ThreadLocalRandom.current().nextDouble(), true);
            }
            return tagComponent(role).append(Component.text(" "))
                    .append(shown)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(message);
        }));
    }

    /** 0=founder 1=admin 2=mod 3=helper 4=builder 5=vip, -1=membro */
    public static int roleIndex(Player p) {
        for (int i = 0; i < TAG_ORDER.length; i++) {
            if (p.hasPermission("gulosos.tag." + TAG_ORDER[i])) return i;
        }
        return -1;
    }

    public static Component tagComponent(int role) {
        return Msg.glyph(role >= 0 ? TAG_GLYPH[role] : Msg.TAG_MEMBRO);
    }
}
