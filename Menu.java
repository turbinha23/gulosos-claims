package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Menu implements InventoryHolder {

    private static final Material[] WAVE = {
            Material.YELLOW_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE
    };

    private final Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private BukkitTask anim;

    public Menu(Component title, int rows) {
        this.inv = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override public Inventory getInventory() { return inv; }

    public Menu set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action); else actions.remove(slot);
        return this;
    }

    public void click(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> c = actions.get(e.getRawSlot());
        if (c != null) c.accept(e);
    }

    public void open(Player p) { p.openInventory(inv); }

    /** Moldura animada: uma onda de cores a percorrer o contorno do menu. */
    public Menu animateBorder(GulososClaims plugin) {
        int rows = inv.getSize() / 9;
        List<Integer> ring = new ArrayList<>();
        for (int c = 0; c < 9; c++) ring.add(c);
        for (int r = 1; r < rows; r++) ring.add(r * 9 + 8);
        for (int c = 7; c >= 0; c--) ring.add((rows - 1) * 9 + c);
        for (int r = rows - 2; r >= 1; r--) ring.add(r * 9);
        ring.removeIf(s -> inv.getItem(s) != null);
        int[] phase = {0};
        anim = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            phase[0]++;
            for (int i = 0; i < ring.size(); i++) {
                inv.setItem(ring.get(i), Items.pane(WAVE[(i + phase[0]) % WAVE.length]));
            }
        }, 0L, 3L);
        return this;
    }

    public void stop() {
        if (anim != null) { anim.cancel(); anim = null; }
    }
}
