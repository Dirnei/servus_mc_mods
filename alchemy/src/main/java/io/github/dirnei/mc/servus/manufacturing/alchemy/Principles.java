package io.github.dirnei.mc.servus.manufacturing.alchemy;

/**
 * An amount of each of the three principles in mB. Pure value type: no Minecraft types.
 *
 * @param salt    mB of Salt, never negative
 * @param sulfur  mB of Sulfur, never negative
 * @param mercury mB of Mercury, never negative
 */
public record Principles(int salt, int sulfur, int mercury) {

    public static final Principles NONE = new Principles(0, 0, 0);

    public Principles {
        if (salt < 0 || sulfur < 0 || mercury < 0) {
            throw new IllegalArgumentException("Principles cannot be negative: " + salt + ", " + sulfur + ", " + mercury);
        }
    }

    public int get(Principle principle) {
        return switch (principle) {
            case SALT -> salt;
            case SULFUR -> sulfur;
            case MERCURY -> mercury;
        };
    }

    public Principles with(Principle principle, int amount) {
        return switch (principle) {
            case SALT -> new Principles(amount, sulfur, mercury);
            case SULFUR -> new Principles(salt, amount, mercury);
            case MERCURY -> new Principles(salt, sulfur, amount);
        };
    }

    public Principles plus(Principles other) {
        return new Principles(salt + other.salt, sulfur + other.sulfur, mercury + other.mercury);
    }

    /** These minus {@code other}; refuses to go below zero. */
    public Principles minus(Principles other) {
        return new Principles(salt - other.salt, sulfur - other.sulfur, mercury - other.mercury);
    }

    /** Whether there is at least as much of every principle as {@code needed}. */
    public boolean covers(Principles needed) {
        return salt >= needed.salt && sulfur >= needed.sulfur && mercury >= needed.mercury;
    }

    /** Whether adding {@code extra} keeps every principle at or below {@code capacity}. */
    public boolean fits(Principles extra, int capacity) {
        return salt + extra.salt <= capacity && sulfur + extra.sulfur <= capacity && mercury + extra.mercury <= capacity;
    }

    public boolean isEmpty() {
        return salt == 0 && sulfur == 0 && mercury == 0;
    }

    public int total() {
        return salt + sulfur + mercury;
    }
}
