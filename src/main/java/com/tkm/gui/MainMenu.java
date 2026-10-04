package com.tkm.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.tkm.TKMPlugin;
import com.tkm.util.Msg;
import com.tkm.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * /tkm ana menüsü - cf tarzı miktar seçme ekranı.
 *  - Üst kısım: hazır miktarlar (config amounts)
 *  - Alt kısım: özel miktar + aktif oyunlar + yardım
 */
public final class MainMenu {

    private MainMenu() {}

    public static String title(TKMPlugin plugin) {
        return Msg.color(plugin.getConfig().getString("gui.main-title", "&8» &eTaş Kağıt Makas"));
    }

    public static void open(Player p) {
        TKMPlugin plugin = TKMPlugin.getInstance();
        Inventory inv = Bukkit.createInventory(null, 36, title(plugin));

        // cam doldur
        ItemStack glass = simplePane();
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 27; i < 36; i++) {
            if (i == 31 || i == 32 || i == 33) continue;
            inv.setItem(i, glass);
        }

        // hazır miktarlar: slot 10-16 (7 adet)
        List<String> amounts = plugin.getConfig().getStringList("amounts");
        // amounts double listesi de olabilir, ikisini de destekle
        List<Double> parsed = new ArrayList<>();
        try {
            for (double d : plugin.getConfig().getDoubleList("amounts")) parsed.add(d);
        } catch (Exception ignored) {}
        if (parsed.isEmpty()) {
            for (String s : amounts) {
                try { parsed.add(Double.parseDouble(s)); } catch (Exception ignored) {}
            }
        }
        if (parsed.isEmpty()) {
            parsed.add(1000.0); parsed.add(5000.0); parsed.add(10000.0);
            parsed.add(50000.0); parsed.add(100000.0); parsed.add(500000.0); parsed.add(1000000.0);
        }

        Material[] icons = new Material[]{
                Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.EMERALD,
                Material.DIAMOND, Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK, Material.GOLD_BLOCK
        };
        // yoksa fallback (1.13+ isimleri 1.8'de de genelde aynı, değilse PAPER)
        for (int i = 0; i < icons.length; i++) {
            if (icons[i] == null) icons[i] = Material.PAPER;
        }

        int slot = 10;
        for (int i = 0; i < parsed.size() && slot <= 16; i++, slot++) {
            double amount = parsed.get(i);
            Material mat = i < icons.length && icons[i] != null ? icons[i] : Material.PAPER;
            // materyal yoksa (eski sürümde EMERALD_BLOCK var zaten, sorun olmaz)
            try {
                ItemStack it = new ItemStack(mat);
                ItemMeta m = it.getItemMeta();
                m.setDisplayName(Msg.color("&a&l" + Msg.formatNumber(amount) + " &aTL"));
                List<String> lore = new ArrayList<>();
                lore.add(Msg.color("&7Bu miktarla oyun kurmak için tıkla"));
                lore.add(Msg.color("&7Bakiyen: &e" + Msg.formatNumber(plugin.getEconomy().getBalance(p))));
                lore.add(Msg.color(""));
                lore.add(Msg.color("&e» Tıkla ve oyunu kur"));
                m.setLore(lore);
                it.setItemMeta(m);
                inv.setItem(slot, it);
            } catch (Exception e) {
                // materyal hatası olursa atla
            }
            // 17. slotu atla (7 slot doldurduk, 10-16 arası)
            if (slot == 13) { /* ortada boşluk yok, devam */ }
        }

        // özel miktar (anvil)
        try {
            ItemStack anvil = new ItemStack(Material.ANVIL);
            ItemMeta am = anvil.getItemMeta();
            am.setDisplayName(Msg.color("&e&lÖzel Miktar"));
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7Kendi miktarını yazarak oyun kur"));
            lore.add(Msg.color(""));
            lore.add(Msg.color("&e» Tıkla ve chat'e yaz"));
            am.setLore(lore);
            anvil.setItemMeta(am);
            inv.setItem(29, anvil);
        } catch (Exception ignored) {}

        // aktif oyunlar
        try {
            ItemStack chest = new ItemStack(Material.CHEST);
            ItemMeta cm = chest.getItemMeta();
            int count = TKMPlugin.getInstance().getGameManager().getWaitingGames().size();
            cm.setDisplayName(Msg.color("&6&lAktif Oyunlar &8(&e" + count + "&8)"));
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7Bekleyen oyunları gör ve katıl"));
            lore.add(Msg.color(""));
            lore.add(Msg.color("&e» Tıkla ve oyunlara göz at"));
            cm.setLore(lore);
            chest.setItemMeta(cm);
            inv.setItem(31, chest);
        } catch (Exception ignored) {}

        // kendi oyunun / iptal
        try {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            // 1.8'de BARRIER var
            ItemMeta bm = barrier.getItemMeta();
            bm.setDisplayName(Msg.color("&c&lOyunumu İptal Et"));
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7Aktif oyununu iptal et ve paranı geri al"));
            lore.add(Msg.color(""));
            lore.add(Msg.color("&c» Tıkla ve iptal et"));
            bm.setLore(lore);
            barrier.setItemMeta(bm);
            inv.setItem(33, barrier);
        } catch (Exception ignored) {}

        p.openInventory(inv);
        SoundUtil.click(p);
    }

    private static ItemStack simplePane() {
        try {
            ItemStack it = new ItemStack(SoundUtil.glassPane(), 1, (short) 7);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(" ");
            it.setItemMeta(m);
            return it;
        } catch (Exception e) {
            ItemStack it = new ItemStack(Material.GLASS);
            try {
                ItemMeta m = it.getItemMeta();
                m.setDisplayName(" ");
                it.setItemMeta(m);
            } catch (Exception ignored) {}
            return it;
        }
    }
}
