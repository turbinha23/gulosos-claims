package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Mensagens com o logo GulososSMP e glifos do resource pack. */
public final class Msg {

    // Glifos definidos em assets/minecraft/font/default.json do resource pack
    public static final String TAG_FOUNDER = "\ue000";
    public static final String TAG_ADMIN   = "\ue001";
    public static final String TAG_MOD     = "\ue002";
    public static final String TAG_HELPER  = "\ue003";
    public static final String TAG_BUILDER = "\ue004";
    public static final String TAG_VIP     = "\ue005";
    public static final String TAG_MEMBRO  = "\ue006";
    public static final String LOGO_32     = "\ue007";
    public static final String LOGO_24     = "\ue008"; // usado nos titulos
    public static final String LOGO_40     = "\ue009";
    public static final String LOGO_48     = "\ue00a";
    public static final String LOGO_CHAT   = "\ue00b"; // horizontal, 1 linha

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Msg() {}

    /** Glifo em branco para a cor nao alterar a textura. */
    public static Component glyph(String g) {
        return Component.text(g, NamedTextColor.WHITE);
    }

    public static Component prefix() {
        return glyph(LOGO_CHAT).append(Component.text(" "));
    }

    public static Component mm(String text, TagResolver... r) {
        return MM.deserialize(text, r);
    }

    public static void send(CommandSender to, String text, TagResolver... r) {
        to.sendMessage(prefix().append(MM.deserialize(text, r)));
    }

    public static void actionBar(Player p, String text, TagResolver... r) {
        p.sendActionBar(MM.deserialize(text, r));
    }

    public static TagResolver p(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
}
