package com.tkm.util;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Sürüm uyumluluğu için güvenli ses / materyal yardımcısı.
 * 1.8'den 1.21'e kadar çalışır (bilinmeyen isimde sessizce geçer).
 */
public final class SoundUtil {

    private SoundUtil() {}

    public static void play(Player p, String... candidates) {
        if (p == null) return;
        for (String name : candidates) {
            try {
                Sound s = Sound.valueOf(name);
                p.playSound(p.getLocation(), s, 1f, 1f);
                return;
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static void click(Player p) {
        // 1.8: CLICK - 1.9+: UI_BUTTON_CLICK / BLOCK_NOTE_BLOCK_PLING
        play(p, "UI_BUTTON_CLICK", "BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING", "CLICK");
    }

    public static void win(Player p) {
        play(p, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
    }

    public static void lose(Player p) {
        play(p, "ENTITY_VILLAGER_NO", "VILLAGER_NO");
    }

    public static void draw(Player p) {
        play(p, "BLOCK_NOTE_BLOCK_BASS", "NOTE_BASS", "NOTE_BASS_GUITAR");
    }

    /** Sürüme göre cam materyali döndürür. */
    public static Material glassPane() {
        Material m = Material.getMaterial("BLACK_STAINED_GLASS_PANE");
        if (m != null) return m;
        m = Material.getMaterial("STAINED_GLASS_PANE");
        if (m != null) return m;
        return Material.GLASS;
    }

    /** Sürüme göre kafa materyali döndürür. */
    public static Material skull() {
        Material m = Material.getMaterial("PLAYER_HEAD");
        if (m != null) return m;
        // 1.8-1.12
        m = Material.getMaterial("SKULL_ITEM");
        if (m != null) return m;
        return Material.PAPER;
    }
}
