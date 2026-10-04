package com.tkm.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import com.tkm.TKMPlugin;
import com.tkm.gui.GamesMenu;
import com.tkm.gui.MainMenu;
import com.tkm.model.Choice;
import com.tkm.util.Msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /tkm komutu
 *  /tkm            -> oyun kurma menüsü (cf tarzı)
 *  /tkm kur 10000  -> direkt oyun kur
 *  /tkm katil <isim> -> oyuna katıl
 *  /tkm sec tas    -> taş/kağıt/makas seç (chat butonları bunu çalıştırır)
 *  /tkm iptal      -> oyun iptal
 *  /tkm liste      -> aktif oyunlar
 */
public class TKMCommand implements CommandExecutor, TabCompleter {

    private final TKMPlugin plugin;

    public TKMCommand(TKMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Msg.color(plugin.getConfig().getString("prefix", "") + "&cBu komutu sadece oyuncular kullanabilir!"));
            return true;
        }
        Player p = (Player) sender;

        if (!p.hasPermission("tkm.use")) {
            p.sendMessage(plugin.getGameManager().msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            MainMenu.open(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        // seçim: /tkm sec tas
        if (sub.equals("sec") || sub.equals("seç") || sub.equals("secim") || sub.equals("choose")) {
            if (args.length < 2) {
                p.sendMessage(plugin.getGameManager().msg("invalid-choice"));
                return true;
            }
            Choice c = Choice.parse(args[1]);
            if (c == null) {
                p.sendMessage(plugin.getGameManager().msg("invalid-choice"));
                return true;
            }
            plugin.getGameManager().choose(p, c);
            return true;
        }

        if (sub.equals("kur") || sub.equals("olustur") || sub.equals("oluştur") || sub.equals("create") || sub.equals("oyunkur")) {
            if (args.length < 2) {
                // miktar verilmediyse menüyü aç
                MainMenu.open(p);
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[1].replace(".", "").replace(",", "."));
                // Türkçe format: 10.000 yazarsa 10000 olmalı - yukarıda noktaları sildik
                // ama 10.000.5 gibi garip girdi olursa parse hatası verir, sorun değil
            } catch (NumberFormatException e) {
                p.sendMessage(plugin.getGameManager().msg("invalid-amount"));
                return true;
            }
            plugin.getGameManager().createGame(p, amount);
            return true;
        }

        if (sub.equals("katil") || sub.equals("katıl") || sub.equals("join") || sub.equals("kabul")) {
            if (args.length < 2) {
                GamesMenu.open(p);
                return true;
            }
            plugin.getGameManager().joinGame(p, args[1]);
            return true;
        }

        if (sub.equals("iptal") || sub.equals("cancel") || sub.equals("sil")) {
            plugin.getGameManager().cancelGame(p);
            return true;
        }

        if (sub.equals("liste") || sub.equals("list") || sub.equals("oyunlar") || sub.equals("games")) {
            GamesMenu.open(p);
            return true;
        }

        if (sub.equals("yardim") || sub.equals("yardım") || sub.equals("help")) {
            for (String line : plugin.getConfig().getStringList("messages.help")) {
                p.sendMessage(Msg.color(line));
            }
            return true;
        }

        if (sub.equals("reload") || sub.equals("yenile")) {
            if (!p.hasPermission("tkm.admin")) {
                p.sendMessage(plugin.getGameManager().msg("no-permission"));
                return true;
            }
            plugin.reload();
            p.sendMessage(plugin.getGameManager().msg("reloaded"));
            return true;
        }

        // /tkm <miktar> yazarsa direkt kur (cf alışkanlığı)
        try {
            double amount = Double.parseDouble(sub.replace(".", "").replace(",", "."));
            plugin.getGameManager().createGame(p, amount);
            return true;
        } catch (NumberFormatException ignored) {}

        // /tkm <oyuncu> yazarsa direkt katıl
        if (plugin.getGameManager().findWaiting(sub) != null) {
            plugin.getGameManager().joinGame(p, sub);
            return true;
        }

        for (String line : plugin.getConfig().getStringList("messages.help")) {
            p.sendMessage(Msg.color(line));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> base = Arrays.asList("kur", "katil", "iptal", "liste", "yardim");
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : base) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            // oyuncu isimleri (katılmak için)
            for (com.tkm.model.Game g : plugin.getGameManager().getWaitingGames()) {
                if (g.getCreatorName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(g.getCreatorName());
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("sec"))) {
            for (String s : Arrays.asList("tas", "kagit", "makas")) {
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("katil") || args[0].equalsIgnoreCase("join"))) {
            for (com.tkm.model.Game g : plugin.getGameManager().getWaitingGames()) {
                if (g.getCreatorName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(g.getCreatorName());
            }
        }
        return out;
    }
}
