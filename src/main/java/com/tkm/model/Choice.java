package com.tkm.model;

/**
 * Taş / Kağıt / Makas seçimleri.
 * Taş > Makas > Kağıt > Taş
 */
public enum Choice {
    TAS("Taş", "tas"),
    KAGIT("Kağıt", "kagit"),
    MAKAS("Makas", "makas");

    private final String display;
    private final String cmd;

    Choice(String display, String cmd) {
        this.display = display;
        this.cmd = cmd;
    }

    public String getDisplay() {
        return display;
    }

    public String getCmd() {
        return cmd;
    }

    /** Bu seçim diğerini yeniyor mu? */
    public boolean beats(Choice other) {
        if (other == null) return false;
        switch (this) {
            case TAS:   return other == MAKAS;
            case KAGIT: return other == TAS;
            case MAKAS: return other == KAGIT;
            default: return false;
        }
    }

    /** Chat / komut girdisinden çözümle. Türkçe karakter toleranslı. */
    public static Choice parse(String s) {
        if (s == null) return null;
        s = s.toLowerCase().trim()
                .replace("ş", "s").replace("ğ", "g")
                .replace("ü", "u").replace("ö", "o")
                .replace("ç", "c").replace("ı", "i");
        if (s.equals("tas") || s.equals("taş") || s.equals("rock") || s.equals("1")) return TAS;
        if (s.equals("kagit") || s.equals("kağıt") || s.equals("kagıt") || s.equals("paper") || s.equals("2")) return KAGIT;
        if (s.equals("makas") || s.equals("scissors") || s.equals("3")) return MAKAS;
        return null;
    }
}
