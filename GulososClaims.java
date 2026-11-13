package pt.gulosos.claims;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GulososClaims extends JavaPlugin {

    private ClaimManager claims;
    private Menus menus;
    private ShovelListener shovel;
    private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        claims = new ClaimManager(this);
        menus = new Menus(this);
        shovel = new ShovelListener(this);

        getServer().getPluginManager().registerEvents(shovel, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        Commands handler = new Commands(this);
        for (String c : new String[]{"claim", "blocos", "terrenos"}) {
            PluginCommand pc = getCommand(c);
            if (pc != null) {
                pc.setExecutor(handler);
                pc.setTabCompleter(handler);
            }
        }

        // +blocos por tempo jogado e gravacao automatica, de minuto a minuto
        getServer().getScheduler().runTaskTimer(this, () -> {
            claims.accrueOnline();
            claims.saveIfDirty();
        }, 1200L, 1200L);

        if (getConfig().getBoolean("tab.ativo", true)) {
            new TabTask().runTaskTimer(this, 20L, 3L);
        }

        getLogger().info("GulososClaims ativo.");
    }

    @Override
    public void onDisable() {
        if (claims != null) claims.save();
    }

    public ClaimManager claims() { return claims; }
    public Menus menus() { return menus; }
    public ShovelListener shovel() { return shovel; }

    // ---------- accoes partilhadas ----------

    public void giveTool(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (Items.isClaimTool(this, it)) {
                Msg.send(p, "<yellow>Já tens a Pá de Terrenos no inventário.");
                return;
            }
        }
        Map<Integer, ItemStack> left = p.getInventory().addItem(Items.claimTool(this));
        left.values().forEach(i -> p.getWorld().dropItem(p.getLocation(), i));
        Msg.send(p, "<gold>Recebeste a <bold>Pá de Terrenos</bold>! <gray>Clica com o botão direito em dois cantos opostos.");
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
    }

    public void showBlocks(Player p) {
        UUID id = p.getUniqueId();
        int total = claims.totalBlocks(id), used = claims.usedBlocks(id);
        Msg.send(p, "<gold><bold>Blocos de terreno");
        Msg.send(p, "<gray>Total: <white><total> <dark_gray>| <gray>Usados: <white><usados> <dark_gray>| <gray>Restantes: <green><rest>",
                Msg.p("total", total), Msg.p("usados", used), Msg.p("rest", total - used));
        Msg.send(p, "<gray>Ganhas <yellow>+<hora></yellow> blocos por hora de jogo.",
                Msg.p("hora", getConfig().getInt("blocos.por-hora", 100)));
        Msg.actionBar(p, "<gold>Blocos restantes: <white><rest>", Msg.p("rest", total - used));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }

    /** Pede uma linha de texto ao jogador pelo chat. */
    public void prompt(Player p, String message, Consumer<String> callback) {
        p.closeInventory();
        prompts.put(p.getUniqueId(), callback);
        Msg.send(p, message);
    }

    public Consumer<String> takePrompt(UUID id) { return prompts.remove(id); }
    public void cancelPrompt(UUID id) { prompts.remove(id); }
}
