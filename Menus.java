package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class Menus {

    private static final int[] LIST_SLOTS = new int[28];
    static {
        int i = 0;
        for (int r = 1; r <= 4; r++) for (int c = 1; c <= 7; c++) LIST_SLOTS[i++] = r * 9 + c;
    }

    private final GulososClaims plugin;

    public Menus(GulososClaims plugin) { this.plugin = plugin; }

    private static Component title(String sub) {
        return Msg.glyph(Msg.LOGO_CHAT).append(Component.text("  " + sub, NamedTextColor.DARK_GRAY));
    }

    private ClaimManager cm() { return plugin.claims(); }

    // =============== MENU PRINCIPAL ===============

    public void main(Player p) {
        UUID id = p.getUniqueId();
        int total = cm().totalBlocks(id), used = cm().usedBlocks(id), rem = total - used;
        int count = cm().claimsOf(id).size();
        int max = plugin.getConfig().getInt("terrenos.maximo-por-jogador", 10);

        Menu m = new Menu(title("Terrenos"), 6);

        m.set(10, Items.of(Material.EXPERIENCE_BOTTLE, "<gold><bold>Os teus blocos",
                "<gray>Total: <white>" + total,
                "<gray>Usados: <white>" + used,
                "<gray>Restantes: <green>" + rem,
                "<gray>Ganhas <yellow>+" + plugin.getConfig().getInt("blocos.por-hora", 100) + " <gray>por hora de jogo",
                "", "<yellow>Clica para ver no chat"), e -> {
            p.closeInventory();
            plugin.showBlocks(p);
        });

        m.set(12, Items.of(Material.GOLDEN_SHOVEL, "<gold><bold>Receber Pá de Terrenos",
                "<gray>Recebe a pá dourada para",
                "<gray>marcar os cantos do terreno.",
                "", "<yellow>Clica para receber"), e -> {
            p.closeInventory();
            plugin.giveTool(p);
        });

        m.set(14, Items.of(Material.FILLED_MAP, "<aqua><bold>Os meus terrenos",
                "<gray>Terrenos: <white>" + count + "<gray>/<white>" + max,
                "", "<yellow>Clica para ver a lista"), e -> list(p, 0));

        m.set(16, Items.of(Material.COMPASS, "<green><bold>Terreno onde estou",
                "<gray>Abre as opções do terreno",
                "<gray>em que estás neste momento.",
                "", "<yellow>Clica para abrir"), e -> {
            Claim c = cm().getAt(p.getLocation());
            if (c == null) {
                p.closeInventory();
                Msg.send(p, "<red>Não estás dentro de nenhum terreno.");
                Fx.errorSound(p);
            } else if (!c.owner.equals(id) && !p.hasPermission("gulosos.admin")) {
                p.closeInventory();
                Msg.send(p, "<red>Este terreno pertence a <white><n></white>.", Msg.p("n", c.ownerName));
                Fx.errorSound(p);
            } else detail(p, c);
        });

        m.set(28, Items.of(Material.ENDER_EYE, "<light_purple><bold>Ver limites",
                "<gray>Mostra com partículas os limites",
                "<gray>do terreno onde estás.",
                "", "<yellow>Clica para mostrar"), e -> {
            p.closeInventory();
            Claim c = cm().getAt(p.getLocation());
            if (c == null) { Msg.send(p, "<red>Não estás dentro de nenhum terreno."); Fx.errorSound(p); return; }
            showBorder(p, c);
        });

        m.set(30, Items.of(Material.BOOK, "<yellow><bold>Como criar um terreno",
                "<gray>1. Recebe a pá com <white>/claim",
                "<gray>2. Clica com o botão direito num canto",
                "<gray>3. Clica no canto oposto",
                "<gray>4. Confirma com <white>/claim confirmar",
                "", "<dark_gray>O terreno vai do fundo ao topo do mundo."), null);

        m.set(32, Items.of(Material.SHIELD, "<blue><bold>Proteção",
                "<gray>Só tu e os membros podem:",
                "<white>• <gray>construir e partir blocos",
                "<white>• <gray>abrir baús e portas",
                "<white>• <gray>ferir animais e aldeões",
                "", "<dark_gray>Configura PvP, fogo e explosões",
                "<dark_gray>nas opções de cada terreno."), null);

        m.set(49, Items.of(Material.BARRIER, "<red>Fechar"), e -> p.closeInventory());
        m.animateBorder(plugin).open(p);
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.3f);
    }

    // =============== LISTA DE TERRENOS ===============

    public void list(Player p, int page) {
        List<Claim> claims = cm().claimsOf(p.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(claims.size() / 28.0));
        page = Math.max(0, Math.min(page, pages - 1));
        final int pg = page;

        Menu m = new Menu(title("Os meus terrenos"), 6);
        if (claims.isEmpty()) {
            m.set(22, Items.of(Material.PAPER, "<gray>Ainda não tens terrenos",
                    "<yellow>Usa /claim para receberes a pá."), null);
        }
        int start = page * 28;
        for (int i = 0; i < 28 && start + i < claims.size(); i++) {
            Claim c = claims.get(start + i);
            m.set(LIST_SLOTS[i], Items.of(Material.GRASS_BLOCK, "<green><bold>" + Items.esc(c.name),
                    "<gray>Tamanho: <white>" + c.width() + "x" + c.length() + " <dark_gray>(" + c.area() + " blocos)",
                    "<gray>Centro: <white>" + c.centerX() + ", " + c.centerZ(),
                    "<gray>Mundo: <white>" + c.world,
                    "<gray>Membros: <white>" + c.trusted.size(),
                    "", "<yellow>Clica para gerir"), e -> detail(p, c));
        }
        if (pg > 0) m.set(45, Items.of(Material.ARROW, "<yellow>« Página anterior"), e -> list(p, pg - 1));
        if (pg < pages - 1) m.set(53, Items.of(Material.ARROW, "<yellow>Página seguinte »"), e -> list(p, pg + 1));
        m.set(49, Items.of(Material.DARK_OAK_DOOR, "<yellow>Voltar"), e -> main(p));
        m.animateBorder(plugin).open(p);
    }

    // =============== DETALHE DO TERRENO ===============

    public void detail(Player p, Claim c) {
        Menu m = new Menu(title(c.name.length() > 18 ? c.name.substring(0, 18) : c.name), 6);

        m.set(4, Items.glow(Items.of(Material.GRASS_BLOCK, "<green><bold>" + Items.esc(c.name),
                "<gray>Dono: <white>" + Items.esc(c.ownerName),
                "<gray>Tamanho: <white>" + c.width() + "x" + c.length() + " <dark_gray>(" + c.area() + " blocos)",
                "<gray>Canto: <white>" + c.minX + ", " + c.minZ + " <gray>→ <white>" + c.maxX + ", " + c.maxZ)), null);

        m.set(10, Items.of(Material.ENDER_PEARL, "<light_purple><bold>Teleportar",
                "<gray>Teletransporta-te para o centro", "<gray>deste terreno.", "", "<yellow>Clica para ir"), e -> {
            p.closeInventory();
            World w = Bukkit.getWorld(c.world);
            if (w == null || w.getEnvironment() != World.Environment.NORMAL) {
                Msg.send(p, "<red>O teleporte só funciona em terrenos do Overworld.");
                return;
            }
            Location dest = new Location(w, c.centerX() + 0.5, w.getHighestBlockYAt(c.centerX(), c.centerZ()) + 1, c.centerZ() + 0.5);
            Fx.teleportCountdown(plugin, p, dest, c.name);
        });

        m.set(12, Items.of(Material.NAME_TAG, "<yellow><bold>Renomear",
                "<gray>Muda o nome do terreno.", "", "<yellow>Clica e escreve no chat"), e -> {
            plugin.prompt(p, "<gray>Escreve o <yellow>novo nome<gray> no chat <dark_gray>(máx. 24 letras, 'cancelar' para sair)", text -> {
                String n = text.trim();
                if (n.isEmpty() || n.length() > 24) {
                    Msg.send(p, "<red>Nome inválido (1 a 24 caracteres).");
                } else {
                    c.name = n;
                    cm().markDirty();
                    Msg.send(p, "<green>Terreno renomeado para <white><n></white>.", Msg.p("n", n));
                    Fx.successSound(p);
                }
                detail(p, c);
            });
        });

        m.set(14, Items.of(Material.PLAYER_HEAD, "<aqua><bold>Membros",
                "<gray>Quem pode construir neste terreno.", "<gray>Membros: <white>" + c.trusted.size(),
                "", "<yellow>Clica para gerir"), e -> members(p, c));

        m.set(16, Items.of(Material.ENDER_EYE, "<light_purple><bold>Ver limites",
                "<gray>Mostra os limites com partículas.", "", "<yellow>Clica para mostrar"), e -> {
            p.closeInventory();
            showBorder(p, c);
        });

        m.set(28, flag(Material.IRON_SWORD, "PvP", "Jogadores podem lutar aqui.", c.pvp), e -> {
            c.pvp = !c.pvp; cm().markDirty(); detail(p, c);
        });
        m.set(30, flag(Material.TNT, "Explosões", "TNT e creepers podem partir blocos.", c.explosions), e -> {
            c.explosions = !c.explosions; cm().markDirty(); detail(p, c);
        });
        m.set(32, flag(Material.FLINT_AND_STEEL, "Fogo", "O fogo pode espalhar-se e queimar.", c.fire), e -> {
            c.fire = !c.fire; cm().markDirty(); detail(p, c);
        });
        m.set(34, flag(Material.CREEPER_HEAD, "Mobs destrutivos", "Endermen e outros mobs mexem em blocos.", c.mobGrief), e -> {
            c.mobGrief = !c.mobGrief; cm().markDirty(); detail(p, c);
        });

        m.set(40, Items.of(Material.TNT_MINECART, "<red><bold>Abandonar terreno",
                "<gray>Apaga o terreno e devolve", "<gray>os <white>" + c.area() + "<gray> blocos.",
                "", "<red>Não dá para desfazer!"), e -> confirmAbandon(p, c));

        m.set(49, Items.of(Material.DARK_OAK_DOOR, "<yellow>Voltar"), e -> list(p, 0));
        m.animateBorder(plugin).open(p);
    }

    private ItemStack flag(Material mat, String name, String desc, boolean on) {
        ItemStack it = Items.of(mat, (on ? "<green><bold>" : "<red><bold>") + name,
                "<gray>" + desc, "",
                on ? "<green>● Ativado" : "<red>● Desativado",
                "<yellow>Clica para alterar");
        return on ? Items.glow(it) : it;
    }

    // =============== MEMBROS ===============

    public void members(Player p, Claim c) {
        Menu m = new Menu(title("Membros"), 6);
        int i = 0;
        for (UUID u : c.trusted) {
            if (i >= 28) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(u);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(Msg.mm("<white><bold>" + Items.esc(op.getName() == null ? u.toString() : op.getName()))
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            meta.lore(List.of(Msg.mm("<red>Clica para remover")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
            head.setItemMeta(meta);
            m.set(LIST_SLOTS[i++], head, e -> {
                c.trusted.remove(u);
                cm().markDirty();
                Msg.send(p, "<yellow><n></yellow> foi removido do terreno.",
                        Msg.p("n", op.getName() == null ? "Jogador" : op.getName()));
                members(p, c);
            });
        }
        if (c.trusted.isEmpty()) {
            m.set(22, Items.of(Material.PAPER, "<gray>Sem membros", "<gray>Adiciona amigos com o botão abaixo."), null);
        }
        m.set(53, Items.of(Material.EMERALD, "<green><bold>Adicionar membro",
                "<gray>Dá permissão para construir.", "", "<yellow>Clica e escreve o nome no chat"), e -> {
            plugin.prompt(p, "<gray>Escreve o <yellow>nome do jogador<gray> no chat <dark_gray>('cancelar' para sair)", text -> {
                OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(text.trim());
                if (op == null) {
                    Player online = Bukkit.getPlayerExact(text.trim());
                    op = online;
                }
                if (op == null) {
                    Msg.send(p, "<red>Jogador não encontrado. Tem de já ter entrado no servidor.");
                    Fx.errorSound(p);
                } else if (op.getUniqueId().equals(c.owner)) {
                    Msg.send(p, "<red>Esse jogador já é o dono do terreno.");
                } else if (c.trusted.add(op.getUniqueId())) {
                    cm().markDirty();
                    Msg.send(p, "<green><n></green> já pode construir no teu terreno.", Msg.p("n", op.getName()));
                    Fx.successSound(p);
                } else {
                    Msg.send(p, "<yellow>Esse jogador já é membro.");
                }
                members(p, c);
            });
        });
        m.set(49, Items.of(Material.DARK_OAK_DOOR, "<yellow>Voltar"), e -> detail(p, c));
        m.animateBorder(plugin).open(p);
    }

    // =============== CONFIRMAR ABANDONO ===============

    private void confirmAbandon(Player p, Claim c) {
        Menu m = new Menu(title("Abandonar?"), 3);
        m.set(11, Items.of(Material.LIME_CONCRETE, "<green><bold>Sim, abandonar",
                "<gray>Perdes o terreno <white>" + Items.esc(c.name),
                "<gray>Recuperas <white>" + c.area() + "<gray> blocos."), e -> {
            p.closeInventory();
            if (cm().get(c.id) == null) return;
            cm().delete(c);
            Msg.send(p, "<yellow>Terreno abandonado. Recuperaste <white><blocos></white> blocos.", Msg.p("blocos", c.area()));
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_DESTROY, 0.7f, 1f);
        });
        m.set(15, Items.of(Material.RED_CONCRETE, "<red><bold>Não, cancelar"), e -> detail(p, c));
        m.open(p);
    }

    // =============== UTIL ===============

    public void showBorder(Player p, Claim c) {
        Fx.border(plugin, p, c.minX, c.minZ, c.maxX, c.maxZ, 15, Color.fromRGB(255, 200, 0));
        Msg.send(p, "<gray>A mostrar os limites de <gold><n></gold> durante 15 segundos.", Msg.p("n", c.name));
    }
}
