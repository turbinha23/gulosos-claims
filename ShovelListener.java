package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Selecao de cantos com a pa dourada + confirmacao. */
public class ShovelListener implements Listener {

    private final GulososClaims plugin;
    private final Map<UUID, Location[]> sel = new HashMap<>();

    public ShovelListener(GulososClaims plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!Items.isClaimTool(plugin, e.getItem())) return;
        e.setCancelled(true);

        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (b == null) return;
        Location loc = b.getLocation();
        Location[] s = sel.get(p.getUniqueId());

        if (s == null || s[1] != null || !s[0].getWorld().equals(loc.getWorld())) {
            sel.put(p.getUniqueId(), new Location[]{loc, null});
            Msg.send(p, "<gold>Canto 1<gray> definido em <white><x>, <z></white>. Agora clica no <yellow>canto oposto<gray>.",
                    Msg.p("x", loc.getBlockX()), Msg.p("z", loc.getBlockZ()));
            Fx.pillar(plugin, p, loc);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
            return;
        }

        s[1] = loc;
        Fx.pillar(plugin, p, loc);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        ClaimManager.Check ck = plugin.claims().check(p, s[0], s[1]);
        Color color = ck.result() == ClaimManager.Result.OK ? Color.fromRGB(255, 200, 0) : Color.fromRGB(255, 50, 50);
        Fx.border(plugin, p, Math.min(s[0].getBlockX(), s[1].getBlockX()), Math.min(s[0].getBlockZ(), s[1].getBlockZ()),
                Math.max(s[0].getBlockX(), s[1].getBlockX()), Math.max(s[0].getBlockZ(), s[1].getBlockZ()), 12, color);
        report(p, s, ck);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { sel.remove(e.getPlayer().getUniqueId()); }

    private void report(Player p, Location[] s, ClaimManager.Check ck) {
        int w = Math.abs(s[0].getBlockX() - s[1].getBlockX()) + 1;
        int l = Math.abs(s[0].getBlockZ() - s[1].getBlockZ()) + 1;
        int min = plugin.getConfig().getInt("terrenos.tamanho-minimo", 5);
        switch (ck.result()) {
            case OK -> {
                int rem = plugin.claims().remainingBlocks(p.getUniqueId());
                Msg.send(p, "<green>Terreno válido! <gray>Tamanho <white><w>x<l></white> (<white><a></white> blocos). Ficas com <white><rest></white> blocos.",
                        Msg.p("w", w), Msg.p("l", l), Msg.p("a", ck.area()), Msg.p("rest", rem - ck.area()));
                Component yes = Component.text("[CONFIRMAR]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/claim confirmar"))
                        .hoverEvent(HoverEvent.showText(Component.text("Clica para criar o terreno")));
                Component no = Component.text("[CANCELAR]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/claim cancelar"))
                        .hoverEvent(HoverEvent.showText(Component.text("Clica para cancelar")));
                p.sendMessage(Msg.prefix().append(yes).append(Component.text("  ")).append(no));
            }
            case TOO_SMALL -> Msg.send(p, "<red>Terreno demasiado pequeno (<w>x<l>). Cada lado tem de ter pelo menos <m> blocos.",
                    Msg.p("w", w), Msg.p("l", l), Msg.p("m", min));
            case NO_BLOCKS -> Msg.send(p, "<red>Precisas de <white><a></white> blocos mas só tens <white><rest></white>. Espera ou escolhe uma área mais pequena.",
                    Msg.p("a", ck.area()), Msg.p("rest", plugin.claims().remainingBlocks(p.getUniqueId())));
            case OVERLAP -> Msg.send(p, "<red>Esta área sobrepõe-se ao terreno <white><n></white> de <white><o></white>.",
                    Msg.p("n", ck.overlapped().name), Msg.p("o", ck.overlapped().ownerName));
            case MAX_CLAIMS -> Msg.send(p, "<red>Já atingiste o máximo de terrenos (<m>).",
                    Msg.p("m", plugin.getConfig().getInt("terrenos.maximo-por-jogador", 10)));
            case BLOCKED_WORLD -> Msg.send(p, "<red>Não podes criar terrenos neste mundo.");
            case OTHER_WORLD -> Msg.send(p, "<red>Os dois cantos têm de estar no mesmo mundo.");
        }
        if (ck.result() != ClaimManager.Result.OK) Fx.errorSound(p);
    }

    public void confirm(Player p) {
        Location[] s = sel.get(p.getUniqueId());
        if (s == null || s[1] == null) {
            Msg.send(p, "<red>Marca primeiro os dois cantos com a pá dourada.");
            return;
        }
        ClaimManager.Check ck = plugin.claims().check(p, s[0], s[1]);
        if (ck.result() != ClaimManager.Result.OK) { report(p, s, ck); return; }

        Claim c = plugin.claims().create(p, s[0], s[1]);
        sel.remove(p.getUniqueId());

        Msg.send(p, "<green>Terreno <gold><n></gold> criado! <gray>Usaste <white><a></white> blocos, restam <white><rest></white>. Usa <yellow>/terrenos<gray> para o gerir.",
                Msg.p("n", c.name), Msg.p("a", c.area()), Msg.p("rest", plugin.claims().remainingBlocks(p.getUniqueId())));
        Fx.typewriter(plugin, p, Msg.glyph(Msg.LOGO_24), "Terreno criado!", NamedTextColor.GOLD);
        Fx.border(plugin, p, c.minX, c.minZ, c.maxX, c.maxZ, 8, Color.fromRGB(80, 255, 80));
        if (plugin.getConfig().getBoolean("efeitos.selo-3d", true)) Fx.seal(plugin, c);
        else Fx.successSound(p);
    }

    public void cancel(Player p) {
        if (sel.remove(p.getUniqueId()) != null) Msg.send(p, "<yellow>Seleção cancelada.");
        else Msg.send(p, "<gray>Não tens nenhuma seleção ativa.");
    }
}
