package pt.gulosos.claims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.concurrent.ThreadLocalRandom;

/** Estilos de nome: FOUNDER (dourado com brilho) e ADMIN (hacker / matrix). */
public final class NameStyle {

    private static final int[] GOLD   = {0xFFF3A0, 0xFFC61A, 0xFF8C00, 0xFFC61A};
    private static final int[] MATRIX = {0x00FF41, 0x00B32C, 0x007A1E, 0x00B32C};
    private static final String GLITCH = "01#$%&@*<>/\\|=+";

    private NameStyle() {}

    private static int mix(int a, int b, double k) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = (int) Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    /** Gradiente ciclico: t pode ser qualquer valor, repete-se de 0 a 1. */
    private static int at(int[] stops, double t) {
        t = t - Math.floor(t);
        double scaled = t * stops.length;
        int i = ((int) Math.floor(scaled)) % stops.length;
        int j = (i + 1) % stops.length;
        return mix(stops[i], stops[j], scaled - Math.floor(scaled));
    }

    /**
     * FOUNDER: dourado em negrito. phase desloca o gradiente, shine e a posicao
     * (em letras) de um brilho branco que "varre" o nome. Usa shine negativo grande para nao ter brilho.
     */
    public static Component founder(String name, double phase, double shine) {
        Component out = Component.empty();
        int n = Math.max(1, name.length());
        for (int i = 0; i < name.length(); i++) {
            int c = at(GOLD, (double) i / n + phase);
            double d = Math.abs(i - shine);
            if (d < 2.0) c = mix(c, 0xFFFFFF, (1 - d / 2.0) * 0.85);
            out = out.append(Component.text(String.valueOf(name.charAt(i)), TextColor.color(c))
                    .decoration(TextDecoration.BOLD, true));
        }
        return out;
    }

    /**
     * ADMIN (hacker): verde matrix. glitchChance e a probabilidade de cada letra
     * virar um simbolo aleatorio; flanks poe caracteres que tremem (ofuscados) nos lados.
     */
    public static Component admin(String name, double glitchChance, double phase, boolean flanks) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Component out = Component.empty();
        if (flanks) out = out.append(Component.text("0", TextColor.color(0x007A1E)).decoration(TextDecoration.OBFUSCATED, true));
        int n = Math.max(1, name.length());
        for (int i = 0; i < name.length(); i++) {
            boolean glitch = glitchChance > 0 && r.nextDouble() < glitchChance;
            String s = glitch ? String.valueOf(GLITCH.charAt(r.nextInt(GLITCH.length()))) : String.valueOf(name.charAt(i));
            int c = glitch ? 0xB8FFCC : at(MATRIX, (double) i / n + phase);
            out = out.append(Component.text(s, TextColor.color(c)));
        }
        if (flanks) out = out.append(Component.text("1", TextColor.color(0x00FF41)).decoration(TextDecoration.OBFUSCATED, true));
        return out;
    }
}
