package pt.gulosos.claims;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionListener implements Listener {

    private final GulososClaims plugin;
    private final ClaimManager cm;
    private final Map<UUID, Long> lastMsg = new HashMap<>();

    public ProtectionListener(GulososClaims plugin) {
        this.plugin = plugin;
        this.cm = plugin.claims();
    }

    private boolean allowed(Player p, Claim c) {
        return c == null || c.owner.equals(p.getUniqueId()) || c.trusted.contains(p.getUniqueId())
                || p.hasPermission("gulosos.admin");
    }

    /** true = deve ser bloqueado */
    private boolean deny(Player p, Location loc) {
        Claim c = cm.getAt(loc);
        if (allowed(p, c)) return false;
        long now = System.currentTimeMillis();
        Long last = lastMsg.get(p.getUniqueId());
        if (last == null || now - last > 1500) {
            lastMsg.put(p.getUniqueId(), now);
            Msg.actionBar(p, "<red>Este terreno pertence a <white><n></white>", Msg.p("n", c.ownerName));
            Fx.errorSound(p);
        }
        return true;
    }

    private Player source(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (b == null) return;

        if (e.getAction() == Action.PHYSICAL) {
            Material t = b.getType();
            if ((t == Material.FARMLAND || t == Material.TURTLE_EGG) && deny(p, b.getLocation())) e.setCancelled(true);
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        boolean sneakWithItem = p.isSneaking() && item != null && !item.getType().isAir();
        if (b.getType().isInteractable() && !sneakWithItem) {
            if (deny(p, b.getLocation())) e.setCancelled(true);
            return;
        }
        if (item != null && isPlacingItem(item.getType())) {
            if (deny(p, b.getRelative(e.getBlockFace()).getLocation()) || deny(p, b.getLocation())) e.setCancelled(true);
        }
    }

    private boolean isPlacingItem(Material m) {
        String n = m.name();
        return n.endsWith("_BUCKET") || n.endsWith("_SPAWN_EGG") || n.endsWith("BOAT") || n.endsWith("RAFT")
                || n.endsWith("MINECART") || m == Material.ARMOR_STAND || m == Material.FLINT_AND_STEEL
                || m == Material.FIRE_CHARGE || m == Material.END_CRYSTAL;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent e) {
        if (e.getPlayer() != null && deny(e.getPlayer(), e.getEntity().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        Player p = source(e.getRemover());
        if (p != null && deny(p, e.getEntity().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Player) return;
        if (deny(e.getPlayer(), e.getRightClicked().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (deny(e.getPlayer(), e.getRightClicked().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player attacker = source(e.getDamager());
        if (attacker == null) return;
        Entity victim = e.getEntity();
        Claim c = cm.getAt(victim.getLocation());
        if (c == null) return;

        if (victim instanceof Player) {
            if (!c.pvp) {
                e.setCancelled(true);
                Msg.actionBar(attacker, "<red>PvP desativado neste terreno");
            }
            return;
        }
        if (victim instanceof Enemy) return;
        if (deny(attacker, victim.getLocation())) e.setCancelled(true);
    }

    // ---------- explosoes, fogo, mobs, liquidos, pistoes ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> {
            Claim c = cm.getAt(b.getLocation());
            return c != null && !c.explosions;
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> {
            Claim c = cm.getAt(b.getLocation());
            return c != null && !c.explosions;
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        Claim c = cm.getAt(e.getBlock().getLocation());
        if (c != null && !c.fire) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (e.getPlayer() != null) {
            if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
            return;
        }
        Claim c = cm.getAt(e.getBlock().getLocation());
        if (c != null && !c.fire) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobGrief(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof Mob)) return;
        Claim c = cm.getAt(e.getBlock().getLocation());
        if (c != null && !c.mobGrief) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        Claim to = cm.getAt(e.getToBlock().getLocation());
        if (to == null) return;
        if (cm.getAt(e.getBlock().getLocation()) != to) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent e) {
        Claim from = cm.getAt(e.getBlock().getLocation());
        for (Block b : e.getBlocks()) {
            Claim dest = cm.getAt(b.getRelative(e.getDirection()).getLocation());
            if (dest != null && dest != from) { e.setCancelled(true); return; }
        }
        Claim front = cm.getAt(e.getBlock().getRelative(e.getDirection()).getLocation());
        if (front != null && front != from) e.setCancelled(true);
    }
}
