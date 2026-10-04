package com.tkm.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.tkm.TKMPlugin;
import com.tkm.model.Game;
import com.tkm.util.Msg;
import com.tkm.util.SoundUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aktif (bekleyen) oyunlar menüsü - tıklayınca katılınır.
 */
public final class GamesMenu {

    private GamesMenu() {}

    public static String title(TKMPlugin plugin) {
        return Msg.color(plugin.getConfig().getString("gui.games-title", "&8» &eAktif Oyunlar"));
    }

    @SuppressWarnings("deprecation")
    public static void open(Player p) {
        TKMPlugin plugin = TKMPlugin.getInstance();
        Collection<Game> games = plugin.getGameManager().getWaitingGames();

        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, title(plugin));

        if (games.isEmpty()) {
            ItemStack barrier;
            try { barrier = new ItemStack(Material.BARRIER); }
            catch (Exception e) { barrier = new ItemStack(Material.PAPER); }
            ItemMeta m = barrier.getItemMeta();
            m.setDisplayName(Msg.color("&cAktif oyun yok!"));
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7Sen kurmak ister misin?"));
            lore.add(Msg.color(""));
            lore.add(Msg.color("&e» Geri dönmek için tıkla"));
            m.setLore(lore);
            barrier.setItemMeta(m);
            inv.setItem(22, barrier);
        } else {
            int slot = 10;
            for (Game g : games) {
                if (slot >= 44) break;
                // kafa yoksa kağıt kullan
                ItemStack head;
                try {
                    head = new ItemStack(SoundUtil.skull(), 1);
                    // 1.8'de data 3 = oyuncu kafası
                    if (head.getType().name().equals("SKULL_ITEM")) {
                        head.setDurability((short) 3);
                    }
                    SkullMeta sm = (SkullMeta) head.getItemMeta();
                    sm.setOwner(g.getCreatorName());
                    sm.setDisplayName(Msg.color("&e" + g.getCreatorName()));
                    List<String> lore = new ArrayList<>();
                    lore.add(Msg.color("&7Bahis: &a" + Msg.formatNumber(g.getBet())));
                    lore.add(Msg.color(""));
                    if (g.getCreator().equals(p.getUniqueId())) {
                        lore.add(Msg.color("&cBu senin oyunun!"));
                    } else {
                        lore.add(Msg.color("&a» Tıkla ve katıl (&e" + Msg.formatNumber(g.getBet()) + "&a)"));
                    }
                    sm.setLore(lore);
                    head.setItemMeta(sm);
                } catch (Exception e) {
                    head = new ItemStack(Material.PAPER);
                    ItemMeta m = head.getItemMeta();
                    m.setDisplayName(Msg.color("&e" + g.getCreatorName()));
                    List<String> lore = new ArrayList<>();
                    lore.add(Msg.color("&7Bahis: &a" + Msg.formatNumber(g.getBet())));
                    m.setLore(lore);
                    head.setItemMeta(m);
                }
                inv.setItem(slot, head);
                slot++;
                // satır sonlarında atla: 17,18 - 26,27 - 35,36
                if (slot % 9 == 8) slot += 2;
            }
        }

        // geri butonu
        try {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta bm = back.getItemMeta();
            bm.setDisplayName(Msg.color("&c» Geri"));
            back.setItemMeta(bm);
            inv.setItem(49, back);
        } catch (Exception ignored) {}

        p.openInventory(inv);
        SoundUtil.click(p);
    }
}
