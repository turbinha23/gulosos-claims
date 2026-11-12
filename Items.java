package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class Items {
    private Items() {}

    public static String esc(String s) {
        return MiniMessage.miniMessage().escapeTags(s);
    }

    private static Component line(String mm) {
        return Msg.mm(mm).decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack of(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(line(name));
        if (lore.length > 0) {
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(line(s));
            meta.lore(l);
        }
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack glow(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack pane(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(" "));
        it.setItemMeta(meta);
        return it;
    }

    // ---- pa de claims ----

    public static NamespacedKey toolKey(GulososClaims plugin) {
        return new NamespacedKey(plugin, "claim_tool");
    }

    public static ItemStack claimTool(GulososClaims plugin) {
        ItemStack it = of(Material.GOLDEN_SHOVEL, "<gold><bold>Pá de Terrenos",
                "<gray>Clica com o <white>botão direito</white> num canto",
                "<gray>e depois no canto oposto do terreno.",
                "",
                "<yellow>/claim confirmar <gray>para criar");
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(toolKey(plugin), PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    public static boolean isClaimTool(GulososClaims plugin, ItemStack it) {
        if (it == null || it.getType() != Material.GOLDEN_SHOVEL || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(toolKey(plugin), PersistentDataType.BYTE);
    }
}
