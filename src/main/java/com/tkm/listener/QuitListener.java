package com.tkm.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import com.tkm.TKMPlugin;

/**
 * Oyundan ayrılma: bekleyen oyun iptal+iade, seçim aşamasında ayrılan kaybeder.
 */
public class QuitListener implements Listener {

    private final TKMPlugin plugin;

    public QuitListener(TKMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getGameManager().handleQuit(e.getPlayer());
    }
}
