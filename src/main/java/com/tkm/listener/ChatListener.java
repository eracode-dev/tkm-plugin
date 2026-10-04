package com.tkm.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import com.tkm.TKMPlugin;
import com.tkm.model.Choice;
import com.tkm.model.Game;
import com.tkm.model.GameState;
import com.tkm.util.Msg;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Chat dinleyicisi - 2 iş yapar:
 *  1) Özel miktar girişi (menüde ANVIL'e tıklayan oyuncu chat'e sayı yazar)
 *  2) Taş/kağıt/makas seçimi (seçim aşamasındaki oyuncu chat'e "taş" yazabilir)
 */
public class ChatListener implements Listener {

    private final TKMPlugin plugin;
    private final Set<UUID> awaitingAmount = new HashSet<>();

    public ChatListener(TKMPlugin plugin) {
        this.plugin = plugin;
    }

    public void expectAmount(Player p) {
        awaitingAmount.add(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        // 1) özel miktar girişi
        if (awaitingAmount.contains(p.getUniqueId())) {
            e.setCancelled(true);
            // async -> sync'e taşı (envanter/ekonomi işlemleri için)
            final String message = e.getMessage().trim();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    awaitingAmount.remove(p.getUniqueId());
                    if (message.equalsIgnoreCase("iptal") || message.equalsIgnoreCase("cancel")) {
                        p.sendMessage(plugin.getGameManager().msg("custom-amount-cancelled"));
                        return;
                    }
                    double amount;
                    try {
                        amount = Double.parseDouble(message.replace(".", "").replace(",", "."));
                    } catch (NumberFormatException ex) {
                        p.sendMessage(plugin.getGameManager().msg("invalid-amount"));
                        return;
                    }
                    plugin.getGameManager().createGame(p, amount);
                }
            });
            return;
        }

        // 2) chat ile taş/kağıt/makas seçimi
        if (!plugin.getConfig().getBoolean("allow-chat-choice", true)) return;
        Game g = plugin.getGameManager().getGame(p);
        if (g == null || g.getState() != GameState.CHOOSING) return;
        if (g.getChoice(p.getUniqueId()) != null) return;

        Choice c = Choice.parse(e.getMessage());
        if (c == null) return; // normal sohbet, dokunma

        e.setCancelled(true);
        final Choice pick = c;
        // choose() sync olmalı
        org.bukkit.Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getGameManager().choose(p, pick);
            }
        });
    }
}
