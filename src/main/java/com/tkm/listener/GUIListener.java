package com.tkm.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.tkm.TKMPlugin;
import com.tkm.gui.GamesMenu;
import com.tkm.gui.MainMenu;
import com.tkm.model.Game;
import com.tkm.util.Msg;

import java.util.List;

/**
 * GUI tıklamaları: miktar seç -> oyun kur, oyuna tıkla -> katıl.
 */
public class GUIListener implements Listener {

    private final TKMPlugin plugin;

    public GUIListener(TKMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getClickedInventory() == null) return;
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        String mainTitle = MainMenu.title(plugin);
        String gamesTitle = GamesMenu.title(plugin);
        String opened;
        // 1.8 dahil tüm sürümlerde derlenebilmesi için getInventory().getTitle() kullanıldı
        // (yeni sürümlerde deprecated ama hala çalışıyor)
        try {
            opened = e.getInventory().getTitle();
        } catch (Exception ex) {
            return;
        }

        boolean isMain = opened != null && opened.equals(mainTitle);
        boolean isGames = opened != null && opened.equals(gamesTitle);
        if (!isMain && !isGames) return;

        e.setCancelled(true);
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        if (isMain) {
            handleMain(p, e.getSlot(), item);
        } else {
            handleGames(p, e.getSlot(), item);
        }
    }

    private void handleMain(Player p, int slot, ItemStack item) {
        // hazır miktarlar 10-16
        if (slot >= 10 && slot <= 16) {
            double amount = parseAmount(item);
            if (amount > 0) {
                p.closeInventory();
                plugin.getGameManager().createGame(p, amount);
            }
            return;
        }
        if (slot == 29) {
            // özel miktar -> chat girişi
            p.closeInventory();
            plugin.getChatListener().expectAmount(p);
            p.sendMessage(plugin.getGameManager().msg("custom-amount-ask"));
            return;
        }
        if (slot == 31) {
            GamesMenu.open(p);
            return;
        }
        if (slot == 33) {
            p.closeInventory();
            plugin.getGameManager().cancelGame(p);
            return;
        }
    }

    private void handleGames(Player p, int slot, ItemStack item) {
        if (slot == 49) {
            MainMenu.open(p);
            return;
        }
        if (!item.hasItemMeta()) return;
        String name = item.getItemMeta().getDisplayName();
        if (name == null) return;
        // kafa ismi = kurucu ismi (renkleri temizle)
        String creator = org.bukkit.ChatColor.stripColor(name).trim();
        if (creator.isEmpty() || creator.equalsIgnoreCase("Aktif oyun yok!")) {
            MainMenu.open(p);
            return;
        }
        Game g = plugin.getGameManager().findWaiting(creator);
        if (g == null) {
            p.sendMessage(plugin.getGameManager().msg("game-not-found").replace("{player}", creator));
            GamesMenu.open(p); // listeyi yenile
            return;
        }
        p.closeInventory();
        plugin.getGameManager().joinGame(p, creator);
    }

    private double parseAmount(ItemStack item) {
        try {
            String name = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
            // "10.000 TL" -> "10000"
            name = name.replace("TL", "").trim().replace(".", "").replace(",", "").replace(" ", "");
            return Double.parseDouble(name);
        } catch (Exception e) {
            return -1;
        }
    }
}
