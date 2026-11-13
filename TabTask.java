package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lista TAB: tag para todos e nome animado para FOUNDER e ADMIN. */
public class TabTask extends BukkitRunnable {

    private final Map<UUID, Component> cache = new HashMap<>();
    private int tick = 0;

    @Override
    public void run() {
        tick++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            int role = PlayerListener.roleIndex(p);
            String name = p.getName();
            Component styled;
            if (role == 0) {
                double shine = (tick * 0.45) % (name.length() + 10) - 3;
                styled = NameStyle.founder(name, tick * 0.015, shine);
            } else if (role == 1) {
                boolean burst = (tick / 5) % 12 == 0; // de vez em quando o nome "falha"
                styled = NameStyle.admin(name, burst ? 0.28 : 0.0, -tick * 0.02, false);
            } else {
                styled = Component.text(name);
            }
            Component full = PlayerListener.tagComponent(role).append(Component.text(" ")).append(styled);
            if (!full.equals(cache.get(p.getUniqueId()))) {
                p.playerListName(full);
                cache.put(p.getUniqueId(), full);
            }
        }
        if (tick % 200 == 0) cache.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
    }
}
