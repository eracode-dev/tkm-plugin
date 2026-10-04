package com.tkm.manager;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import com.tkm.TKMPlugin;
import com.tkm.model.Choice;
import com.tkm.model.Game;
import com.tkm.model.GameState;
import com.tkm.util.Msg;
import com.tkm.util.SoundUtil;

import java.util.*;

/**
 * Oyunların tüm mantığı: kur / katıl / seç / süre / sonuç.
 */
public class GameManager {

    private final TKMPlugin plugin;

    // oyuncu UUID -> oyun (kurucu da katılımcı da burada)
    private final Map<UUID, Game> activeByPlayer = new HashMap<>();
    // kurucu isim (küçük harf) -> oyun (sadece WAITING olanlar)
    private final Map<String, Game> waitingByName = new HashMap<>();
    private final Map<Game, BukkitTask> timers = new HashMap<>();

    public GameManager(TKMPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- yardımcılar ----------

    public String prefix() {
        return Msg.color(plugin.getConfig().getString("prefix", "&8[&eTKM&8] "));
    }

    public String msg(String key) {
        String s = plugin.getConfig().getString("messages." + key, key);
        return Msg.color(prefix() + s);
    }

    public String msgRaw(String key) {
        return Msg.color(plugin.getConfig().getString("messages." + key, key));
    }

    public void send(Player p, String key) {
        p.sendMessage(msg(key));
    }

    public Collection<Game> getWaitingGames() {
        return new ArrayList<>(waitingByName.values());
    }

    public Game getGame(Player p) {
        return activeByPlayer.get(p.getUniqueId());
    }

    public Game findWaiting(String creatorName) {
        if (creatorName == null) return null;
        return waitingByName.get(creatorName.toLowerCase());
    }

    public String fmt(double d) {
        return Msg.formatNumber(d);
    }

    // ---------- oyun kur ----------

    public boolean createGame(Player p, double amount) {
        if (getGame(p) != null) {
            p.sendMessage(msg("already-in-game"));
            return false;
        }
        double min = plugin.getConfig().getDouble("min-bet", 1000);
        double max = plugin.getConfig().getDouble("max-bet", 10000000);
        if (amount < min || amount > max) {
            p.sendMessage(msg("amount-range")
                    .replace("{min}", fmt(min))
                    .replace("{max}", fmt(max)));
            return false;
        }
        double balance = plugin.getEconomy().getBalance(p);
        if (balance < amount) {
            p.sendMessage(msg("not-enough-money")
                    .replace("{amount}", fmt(amount))
                    .replace("{balance}", fmt(balance)));
            return false;
        }

        plugin.getEconomy().withdrawPlayer(p, amount);

        Game g = new Game(p.getUniqueId(), p.getName(), amount);
        activeByPlayer.put(p.getUniqueId(), g);
        waitingByName.put(p.getName().toLowerCase(), g);

        p.sendMessage(msg("game-created").replace("{amount}", fmt(amount)));
        SoundUtil.click(p);

        if (plugin.getConfig().getBoolean("broadcast-create", true)) {
            String bc = msg("game-create-broadcast")
                    .replace("{player}", p.getName())
                    .replace("{amount}", fmt(amount));
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (!o.equals(p)) o.sendMessage(bc);
            }
            // Konsola da düşsün
            Bukkit.getConsoleSender().sendMessage(bc);
        }
        return true;
    }

    // ---------- oyuna katıl ----------

    public boolean joinGame(Player joiner, String creatorName) {
        Game g = findWaiting(creatorName);
        if (g == null) {
            joiner.sendMessage(msg("game-not-found").replace("{player}", creatorName));
            return false;
        }
        if (g.getCreator().equals(joiner.getUniqueId())) {
            joiner.sendMessage(msg("cant-join-own"));
            return false;
        }
        if (g.getState() != GameState.WAITING) {
            joiner.sendMessage(msg("game-full"));
            return false;
        }
        if (getGame(joiner) != null) {
            joiner.sendMessage(msg("already-in-game"));
            return false;
        }
        double amount = g.getBet();
        double balance = plugin.getEconomy().getBalance(joiner);
        if (balance < amount) {
            joiner.sendMessage(msg("not-enough-money")
                    .replace("{amount}", fmt(amount))
                    .replace("{balance}", fmt(balance)));
            return false;
        }

        Player creator = Bukkit.getPlayer(g.getCreator());
        if (creator == null || !creator.isOnline()) {
            cleanup(g);
            joiner.sendMessage(msg("game-not-found").replace("{player}", creatorName));
            return false;
        }

        plugin.getEconomy().withdrawPlayer(joiner, amount);

        g.setJoiner(joiner.getUniqueId(), joiner.getName());
        g.setState(GameState.CHOOSING);
        activeByPlayer.put(joiner.getUniqueId(), g);
        waitingByName.remove(g.getCreatorName().toLowerCase());

        joiner.sendMessage(msg("join-success").replace("{amount}", fmt(amount)));
        creator.sendMessage(msg("join-opponent").replace("{player}", joiner.getName()));

        if (plugin.getConfig().getBoolean("broadcast-join", false)) {
            Bukkit.broadcastMessage(prefix() + Msg.color("&e" + joiner.getName() + " &7adlı oyuncu &e" + creator.getName() + " &7adlı oyuncunun &a" + fmt(amount) + " &7miktarlı oyununa katıldı!"));
        }

        startChoicePhase(g);
        return true;
    }

    // ---------- iptal ----------

    public boolean cancelGame(Player p) {
        Game g = getGame(p);
        if (g == null) {
            p.sendMessage(msg("no-game"));
            return false;
        }
        if (g.getState() != GameState.WAITING) {
            p.sendMessage(Msg.color(prefix() + "&cOyun zaten başladı, iptal edemezsin!"));
            return false;
        }
        if (!g.getCreator().equals(p.getUniqueId())) {
            p.sendMessage(Msg.color(prefix() + "&cSadece oyunu kuran iptal edebilir!"));
            return false;
        }
        plugin.getEconomy().depositPlayer(p, g.getBet());
        cleanup(g);
        p.sendMessage(msg("cancelled").replace("{amount}", fmt(g.getBet())));
        SoundUtil.lose(p);
        return true;
    }

    // ---------- seçim ----------

    public boolean choose(Player p, Choice c) {
        Game g = getGame(p);
        if (g == null) {
            p.sendMessage(msg("no-game"));
            return false;
        }
        if (g.getState() != GameState.CHOOSING) {
            p.sendMessage(Msg.color(prefix() + "&cRakip bekleniyor, henüz seçim aşamasında değilsin!"));
            return false;
        }
        if (g.getChoice(p.getUniqueId()) != null) {
            p.sendMessage(msg("already-chose"));
            return false;
        }
        g.setChoice(p.getUniqueId(), c);
        p.sendMessage(msg("choice-made").replace("{choice}", c.getDisplay()));
        SoundUtil.click(p);

        Player opponent = getOpponentPlayer(p, g);
        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(msg("opponent-chose"));
        }

        if (g.bothChose()) {
            finishRound(g);
        }
        return true;
    }

    // ---------- seçim fazı + süre ----------

    private void startChoicePhase(Game g) {
        g.clearChoices();
        g.setState(GameState.CHOOSING);
        int time = plugin.getConfig().getInt("choice-time", 30);
        g.setChooseDeadline(System.currentTimeMillis() + time * 1000L);

        stopTimer(g);

        Player c = Bukkit.getPlayer(g.getCreator());
        Player j = g.getJoiner() == null ? null : Bukkit.getPlayer(g.getJoiner());

        if (g.getRound() == 1) {
            // ilk tur bilgisi
            if (c != null) c.sendMessage(Msg.color(prefix() + "&7Rakibin: &e" + g.getJoinerName() + " &7Bahis: &a" + fmt(g.getBet())));
            if (j != null) j.sendMessage(Msg.color(prefix() + "&7Rakibin: &e" + g.getCreatorName() + " &7Bahis: &a" + fmt(g.getBet())));
        } else {
            String rematch = Msg.color(prefix() + "&eBeraberlik! &7Yeni tur (" + g.getRound() + ". raunt) - tekrar seçin!");
            if (c != null) c.sendMessage(rematch);
            if (j != null) j.sendMessage(rematch);
        }

        sendChoiceMessage(c, time);
        sendChoiceMessage(j, time);

        // her saniye hatırlatma + süre dolunca sonuçlandır
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int left = time;
            @Override
            public void run() {
                // oyun bitmişse dur
                if (g.getState() != GameState.CHOOSING || !activeByPlayer.containsValue(g)) {
                    stopTimer(g);
                    return;
                }
                if (left <= 0) {
                    handleTimeout(g);
                    return;
                }
                // son 10 saniye + her 10 saniyede hatırlat
                if (left <= 10 || left % 10 == 0) {
                    Player cc = Bukkit.getPlayer(g.getCreator());
                    Player jj = g.getJoiner() == null ? null : Bukkit.getPlayer(g.getJoiner());
                    boolean cChose = g.getCreatorChoice() != null;
                    boolean jChose = g.getJoinerChoice() != null;
                    if (cc != null && cc.isOnline() && !cChose) {
                        cc.sendMessage(Msg.color(prefix() + "&cSeçim için son &e" + left + " &csaniye!"));
                        SoundUtil.play(cc, "BLOCK_NOTE_BLOCK_HAT", "NOTE_STICKS");
                    }
                    if (jj != null && jj.isOnline() && !jChose) {
                        jj.sendMessage(Msg.color(prefix() + "&cSeçim için son &e" + left + " &csaniye!"));
                        SoundUtil.play(jj, "BLOCK_NOTE_BLOCK_HAT", "NOTE_STICKS");
                    }
                }
                left--;
            }
        }, 20L, 20L);
        timers.put(g, task);
        g.setTaskId(task.getTaskId());
    }

    /** Tıklanabilir [TAŞ] [KAĞIT] [MAKAS] mesajı */
    public void sendChoiceMessage(Player p, int time) {
        if (p == null || !p.isOnline()) return;
        if (getGame(p) == null) return;
        // zaten seçtiyse tekrar sorma
        Game g = getGame(p);
        if (g.getChoice(p.getUniqueId()) != null) return;

        p.sendMessage(msg("choice-ask").replace("{time}", String.valueOf(time)));

        TextComponent tas = new TextComponent(Msg.color(" &8[&7TAŞ&8] "));
        tas.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tkm sec tas"));
        tas.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new TextComponent[]{new TextComponent(Msg.color("&7Taş seçmek için tıkla"))}));

        TextComponent kagit = new TextComponent(Msg.color(" &8[&eKAĞIT&8] "));
        kagit.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tkm sec kagit"));
        kagit.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new TextComponent[]{new TextComponent(Msg.color("&eKağıt seçmek için tıkla"))}));

        TextComponent makas = new TextComponent(Msg.color(" &8[&cMAKAS&8] "));
        makas.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tkm sec makas"));
        makas.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new TextComponent[]{new TextComponent(Msg.color("&cMakas seçmek için tıkla"))}));

        // 1.8 uyumu: spigot() mevcut
        try {
            p.spigot().sendMessage(tas, kagit, makas);
        } catch (NoSuchMethodError e) {
            p.sendMessage(Msg.color("&8[&7TAŞ&8] &8[&eKAĞIT&8] &8[&cMAKAS&8] &7- seçim için &e/tkm sec <tas/kagit/makas>"));
        }
        p.sendMessage(Msg.color("&7veya chat'e &e taş / kağıt / makas &7yazabilirsin."));
    }

    private void handleTimeout(Game g) {
        stopTimer(g);
        boolean timeoutLose = plugin.getConfig().getBoolean("timeout-lose", true);

        Player c = Bukkit.getPlayer(g.getCreator());
        Player j = g.getJoiner() == null ? null : Bukkit.getPlayer(g.getJoiner());
        boolean cChose = g.getCreatorChoice() != null;
        boolean jChose = g.getJoinerChoice() != null;

        // ikisi de seçmediyse
        if (!cChose && !jChose) {
            if (timeoutLose) {
                // ikisi de kaybetti sayılmaz, iade et (suistimali önlemek için)
                refundAndCleanup(g, "timeout-refund");
            } else {
                refundAndCleanup(g, "timeout-refund");
            }
            return;
        }

        if (!timeoutLose) {
            refundAndCleanup(g, "timeout-refund");
            return;
        }

        // seçmeyen kaybeder
        if (!cChose && jChose) {
            // joiner kazanır
            winByTimeout(g, j, c);
        } else if (cChose && !jChose) {
            winByTimeout(g, c, j);
        } else {
            // ikisi de seçtiyse normalde buraya gelmez ama güvenlik
            finishRound(g);
        }
    }

    private void winByTimeout(Game g, Player winner, Player loser) {
        double pot = g.getBet() * 2;
        double tax = plugin.getConfig().getDouble("tax-percent", 0);
        double reward = pot;
        if (tax > 0) reward = pot - (pot * tax / 100.0);

        if (winner != null && winner.isOnline()) {
            plugin.getEconomy().depositPlayer(winner, reward);
            winner.sendMessage(msg("timeout-win").replace("{amount}", fmt(reward)));
            SoundUtil.win(winner);
        }
        if (loser != null && loser.isOnline()) {
            loser.sendMessage(msg("timeout-lose"));
            SoundUtil.lose(loser);
        }
        broadcastWin(g, winner, loser, true);
        cleanup(g);
    }

    // ---------- raunt sonucu ----------

    private void finishRound(Game g) {
        stopTimer(g);

        Player c = Bukkit.getPlayer(g.getCreator());
        Player j = g.getJoiner() == null ? null : Bukkit.getPlayer(g.getJoiner());
        Choice cc = g.getCreatorChoice();
        Choice jc = g.getJoinerChoice();

        if (cc == null || jc == null) return;

        // berabere
        if (cc == jc) {
            String drawAction = plugin.getConfig().getString("draw-action", "REMATCH");
            if (drawAction.equalsIgnoreCase("REFUND")) {
                if (c != null && c.isOnline()) {
                    plugin.getEconomy().depositPlayer(c, g.getBet());
                    c.sendMessage(msg("draw-refund"));
                }
                if (j != null && j.isOnline()) {
                    plugin.getEconomy().depositPlayer(j, g.getBet());
                    j.sendMessage(msg("draw-refund"));
                }
                cleanup(g);
                return;
            } else {
                if (c != null && c.isOnline()) {
                    c.sendMessage(msg("draw").replace("{choice}", cc.getDisplay()));
                    SoundUtil.draw(c);
                }
                if (j != null && j.isOnline()) {
                    j.sendMessage(msg("draw").replace("{choice}", cc.getDisplay()));
                    SoundUtil.draw(j);
                }
                g.nextRound();
                startChoicePhase(g);
                return;
            }
        }

        // kazananı belirle
        boolean creatorWins = cc.beats(jc);
        Player winner = creatorWins ? c : j;
        Player loser = creatorWins ? j : c;
        Choice winChoice = creatorWins ? cc : jc;
        Choice loseChoice = creatorWins ? jc : cc;

        double pot = g.getBet() * 2;
        double tax = plugin.getConfig().getDouble("tax-percent", 0);
        double reward = pot;
        if (tax > 0) reward = pot - (pot * tax / 100.0);

        if (winner != null && winner.isOnline()) {
            plugin.getEconomy().depositPlayer(winner, reward);
            Choice myC = winner.getUniqueId().equals(g.getCreator()) ? cc : jc;
            Choice opC = winner.getUniqueId().equals(g.getCreator()) ? jc : cc;
            winner.sendMessage(msg("win")
                    .replace("{you}", myC.getDisplay())
                    .replace("{opponent}", opC.getDisplay())
                    .replace("{amount}", fmt(reward)));
            SoundUtil.win(winner);
        }
        if (loser != null && loser.isOnline()) {
            Choice myC = loser.getUniqueId().equals(g.getCreator()) ? cc : jc;
            Choice opC = loser.getUniqueId().equals(g.getCreator()) ? jc : cc;
            loser.sendMessage(msg("lose")
                    .replace("{you}", myC.getDisplay())
                    .replace("{opponent}", opC.getDisplay()));
            SoundUtil.lose(loser);
        }

        broadcastWin(g, winner, loser, false);
        // sohbet özeti
        String summary = Msg.color(prefix() + "&7Sonuç: &e" + g.getCreatorName() + "(" + cc.getDisplay() + ") &7vs &e" + g.getJoinerName() + "(" + jc.getDisplay() + ") &7Kazanan: &a" + (winner != null ? winner.getName() : "?"));
        if (c != null && c.isOnline()) c.sendMessage(summary);
        if (j != null && j.isOnline()) j.sendMessage(summary);

        cleanup(g);
    }

    private void broadcastWin(Game g, Player winner, Player loser, boolean timeout) {
        if (!plugin.getConfig().getBoolean("broadcast-win", true)) return;
        if (winner == null || loser == null) return;
        // sadece online ise isimler doğru olur
        String w = winner.getName();
        String l = (loser != null && loser.isOnline()) ? loser.getName()
                : (loser != null ? loser.getName() : opponentName(g, winner));
        double pot = g.getBet() * 2;
        double tax = plugin.getConfig().getDouble("tax-percent", 0);
        double reward = tax > 0 ? pot - (pot * tax / 100.0) : pot;

        Game full = g;
        Choice wc = null, lc = null;
        try {
            if (winner.getUniqueId().equals(g.getCreator())) { wc = g.getCreatorChoice(); lc = g.getJoinerChoice(); }
            else { wc = g.getJoinerChoice(); lc = g.getCreatorChoice(); }
        } catch (Exception ignored) {}

        String bc = msg("win-broadcast")
                .replace("{winner}", w)
                .replace("{loser}", l)
                .replace("{amount}", fmt(reward))
                .replace("{choice-winner}", wc == null ? (timeout ? "Süre" : "?") : wc.getDisplay())
                .replace("{choice-loser}", lc == null ? (timeout ? "Süre" : "?") : lc.getDisplay());
        for (Player o : Bukkit.getOnlinePlayers()) {
            o.sendMessage(bc);
        }
    }

    // ---------- oyundan düşme ----------

    public void handleQuit(Player p) {
        Game g = getGame(p);
        if (g == null) return;
        // plugin kapanırken refundAll zaten hallediyor, burada normal çıkış:
        if (g.getState() == GameState.WAITING) {
            // kurucu çıktıysa oyunu sil + iade (çevrimdışıyken de iade et)
            if (g.getCreator().equals(p.getUniqueId())) {
                plugin.getEconomy().depositPlayer(p, g.getBet());
                cleanup(g);
                Bukkit.getConsoleSender().sendMessage(prefix() + Msg.color("&7" + p.getName() + " oyundan ayrıldı, TKM oyunu iptal + iade."));
            }
        } else if (g.getState() == GameState.CHOOSING) {
            // seçim aşamasında çıkan kaybeder, rakip kazanır
            Player opponent = getOpponentPlayer(p, g);
            double pot = g.getBet() * 2;
            double tax = plugin.getConfig().getDouble("tax-percent", 0);
            double reward = tax > 0 ? pot - (pot * tax / 100.0) : pot;
            if (opponent != null && opponent.isOnline()) {
                plugin.getEconomy().depositPlayer(opponent, reward);
                opponent.sendMessage(msg("quit-win").replace("{amount}", fmt(reward)));
                SoundUtil.win(opponent);
            } else {
                // rakip de yoksa ikisine de iade etmeye çalış
                try {
                    plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(g.getCreator()), g.getBet());
                    if (g.getJoiner() != null)
                        plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(g.getJoiner()), g.getBet());
                } catch (Exception ignored) {}
            }
            // çıkan oyuncuya iade YOK (kaybetti)
            cleanup(g);
        }
    }

    // ---------- temizlik ----------

    private Player getOpponentPlayer(Player p, Game g) {
        UUID op = g.getOpponent(p.getUniqueId());
        if (op == null) return null;
        return Bukkit.getPlayer(op);
    }

    private String opponentName(Game g, Player winner) {
        if (winner.getUniqueId().equals(g.getCreator())) return g.getJoinerName();
        return g.getCreatorName();
    }

    private void refundAndCleanup(Game g, String msgKey) {
        try {
            plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(g.getCreator()), g.getBet());
            if (g.getJoiner() != null)
                plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(g.getJoiner()), g.getBet());
        } catch (Exception ignored) {}
        Player c = Bukkit.getPlayer(g.getCreator());
        Player j = g.getJoiner() == null ? null : Bukkit.getPlayer(g.getJoiner());
        if (c != null && c.isOnline()) c.sendMessage(msg(msgKey));
        if (j != null && j.isOnline()) j.sendMessage(msg(msgKey));
        cleanup(g);
    }

    public void refundAll(String reason) {
        for (Game g : new ArrayList<>(new HashSet<>(activeByPlayer.values()))) {
            try {
                plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(g.getCreator()), g.getBet());
                if (g.getJoiner() != null)
                    plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(g.getJoiner()), g.getBet());
            } catch (Exception ignored) {}
            stopTimer(g);
        }
        activeByPlayer.clear();
        waitingByName.clear();
    }

    private void stopTimer(Game g) {
        BukkitTask t = timers.remove(g);
        if (t != null) {
            try { t.cancel(); } catch (Exception ignored) {}
        }
    }

    public void cleanup(Game g) {
        stopTimer(g);
        activeByPlayer.remove(g.getCreator());
        if (g.getJoiner() != null) activeByPlayer.remove(g.getJoiner());
        waitingByName.remove(g.getCreatorName().toLowerCase());
        g.setState(GameState.FINISHED);
    }
}
