package com.tkm.util;

import org.bukkit.ChatColor;

/**
 * Mesaj / renk yardımcısı.
 */
public final class Msg {

    private Msg() {}

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String formatNumber(double d) {
        if (d == (long) d) {
            return String.format("%,d", (long) d).replace(',', '.');
        }
        return String.valueOf(d);
    }
}
