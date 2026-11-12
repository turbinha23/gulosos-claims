package pt.gulosos.claims;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {

    private final GulososClaims plugin;

    public Commands(GulososClaims plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();

        if (name.equals("blocos") && args.length >= 1 && args[0].equalsIgnoreCase("dar")) {
            return giveBlocks(sender, args);
        }

        if (!(sender instanceof Player p)) {
            Msg.send(sender, "<red>Este comando só pode ser usado por jogadores.");
            return true;
        }

        switch (name) {
            case "claim" -> {
                if (args.length == 0) plugin.giveTool(p);
                else if (args[0].equalsIgnoreCase("confirmar")) plugin.shovel().confirm(p);
                else if (args[0].equalsIgnoreCase("cancelar")) plugin.shovel().cancel(p);
                else Msg.send(p, "<gray>Uso: <white>/claim <gray>| <white>/claim confirmar <gray>| <white>/claim cancelar");
            }
            case "blocos" -> plugin.showBlocks(p);
            case "terrenos" -> plugin.menus().main(p);
        }
        return true;
    }

    private boolean giveBlocks(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gulosos.admin")) {
            Msg.send(sender, "<red>Sem permissão.");
            return true;
        }
        if (args.length < 3) {
            Msg.send(sender, "<gray>Uso: <white>/blocos dar <jogador> <quantidade>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            Msg.send(sender, "<red>Jogador não encontrado (tem de já ter entrado no servidor).");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            Msg.send(sender, "<red>Quantidade inválida.");
            return true;
        }
        plugin.claims().addBonus(target.getUniqueId(), amount);
        Msg.send(sender, "<green>Deste <white><a></white> blocos a <white><n></white>.",
                Msg.p("a", amount), Msg.p("n", target.getName()));
        Player online = target.getPlayer();
        if (online != null) {
            Msg.send(online, "<green>Recebeste <white><a></white> blocos de terreno!", Msg.p("a", amount));
            Fx.successSound(online);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String n = cmd.getName().toLowerCase();
        if (n.equals("claim") && args.length == 1) {
            for (String s : List.of("confirmar", "cancelar")) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (n.equals("blocos") && sender.hasPermission("gulosos.admin")) {
            if (args.length == 1 && "dar".startsWith(args[0].toLowerCase())) out.add("dar");
            if (args.length == 2 && args[0].equalsIgnoreCase("dar"))
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
        }
        return out;
    }
}
