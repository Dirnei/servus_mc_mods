package io.github.dirnei.mc.servus.alchemy;

/** The three principles of Paracelsus' Tria Prima: the body, the soul and the spirit of matter. */
public enum Principle {
    SALT("salt", 0xFFF2F0EA),
    SULFUR("sulfur", 0xFFE8C531),
    MERCURY("mercury", 0xFFB9C3CC);

    private final String id;
    private final int color;

    Principle(String id, int color) {
        this.id = id;
        this.color = color;
    }

    /** The path of the principle's fluid and bucket ids. */
    public String id() {
        return id;
    }

    /** ARGB tint of the principle's fluid. */
    public int color() {
        return color;
    }
}
