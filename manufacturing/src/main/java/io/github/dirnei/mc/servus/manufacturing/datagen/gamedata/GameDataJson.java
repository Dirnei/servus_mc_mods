package io.github.dirnei.mc.servus.manufacturing.datagen.gamedata;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Writes the game data export (schema 2) for tools outside the game. Plain Java: entries are sorted by id,
 * keys come in a fixed order and there is no timestamp, so the same data always gives the same text.
 */
public final class GameDataJson {

    public static final int SCHEMA = 2;

    /** An item or tag stack. Exactly one of {@code item} and {@code tag} is set. */
    public record Stack(String item, String tag, int count) {
        public static Stack item(String item, int count) {
            return new Stack(item, null, count);
        }

        public static Stack tag(String tag, int count) {
            return new Stack(null, tag, count);
        }
    }

    /** {@code stats} keeps its iteration order, so callers pass a fixed order. */
    public record Machine(String id, String kind, List<String> recipeTypes, Map<String, Integer> stats) {
    }

    public record Recipe(String id, String type, List<Stack> inputs, List<Stack> outputs, int time) {
    }

    /**
     * A crafting table style recipe. {@code pattern} and {@code key} are null for shapeless and special recipes; a
     * special recipe has no fixed ingredients or result, so its inputs and outputs are empty.
     */
    public record Crafting(String id, String type, boolean special, List<String> pattern, Map<String, Stack> key,
                           List<Stack> inputs, List<Stack> outputs) {
    }

    public record Deposit(String id, String block, Stack yield) {
    }

    public record Tech(String id, List<String> parents, List<Stack> cost, int ticksPerPack, List<String> unlocks) {
    }

    public record GameData(String modVersion, List<Machine> machines, List<Recipe> recipes, List<Crafting> crafting,
                           List<Deposit> deposits, List<Tech> techs, Map<String, Integer> fuels,
                           Map<String, String> itemNames) {
    }

    private final StringBuilder out = new StringBuilder();
    private int depth;

    private GameDataJson() {
    }

    public static String write(GameData data) {
        GameDataJson json = new GameDataJson();
        json.gameData(data);
        return json.out.append('\n').toString();
    }

    private void gameData(GameData data) {
        open('{');
        key("schema").append(SCHEMA);
        next();
        key("mod_version").append(quote(data.modVersion()));
        next();
        key("machines");
        objectOf(data.machines(), Machine::id, this::machine);
        next();
        key("recipes");
        objectOf(data.recipes(), Recipe::id, this::recipe);
        next();
        key("crafting");
        objectOf(data.crafting(), Crafting::id, this::crafting);
        next();
        key("deposits");
        objectOf(data.deposits(), Deposit::id, this::deposit);
        next();
        key("techs");
        objectOf(data.techs(), Tech::id, this::tech);
        next();
        key("fuels");
        intMap(new TreeMap<>(data.fuels()));
        next();
        key("items");
        objectOf(new TreeMap<>(data.itemNames()).entrySet().stream().toList(), Map.Entry::getKey, entry -> {
            open('{');
            key("name").append(quote(entry.getValue()));
            close('}');
        });
        close('}');
    }

    private void machine(Machine machine) {
        open('{');
        key("kind").append(quote(machine.kind()));
        next();
        key("recipe_types");
        strings(machine.recipeTypes());
        for (Map.Entry<String, Integer> stat : machine.stats().entrySet()) {
            next();
            key(stat.getKey()).append(stat.getValue());
        }
        close('}');
    }

    private void recipe(Recipe recipe) {
        open('{');
        key("type").append(quote(recipe.type()));
        next();
        key("inputs");
        stacks(recipe.inputs());
        next();
        key("outputs");
        stacks(recipe.outputs());
        next();
        key("time").append(recipe.time());
        close('}');
    }

    private void crafting(Crafting crafting) {
        open('{');
        key("type").append(quote(crafting.type()));
        if (crafting.special()) {
            next();
            key("special").append(true);
        }
        if (crafting.pattern() != null) {
            next();
            key("pattern");
            strings(crafting.pattern());
            next();
            key("key");
            objectOf(new TreeMap<>(crafting.key()).entrySet().stream().toList(), Map.Entry::getKey,
                    entry -> ingredient(entry.getValue()));
        }
        next();
        key("inputs");
        stacks(crafting.inputs());
        next();
        key("outputs");
        stacks(crafting.outputs());
        close('}');
    }

    private void deposit(Deposit deposit) {
        open('{');
        key("block").append(quote(deposit.block()));
        next();
        key("yield");
        stack(deposit.yield());
        close('}');
    }

    private void tech(Tech tech) {
        open('{');
        key("parents");
        strings(tech.parents());
        next();
        key("cost");
        stacks(tech.cost());
        next();
        key("ticks_per_pack").append(tech.ticksPerPack());
        next();
        key("unlocks");
        strings(tech.unlocks());
        close('}');
    }

    private <T> void objectOf(List<T> entries, Function<T, String> id,
                              Consumer<T> body) {
        List<T> sorted = entries.stream().sorted(Comparator.comparing(id)).toList();
        if (sorted.isEmpty()) {
            out.append("{}");
            return;
        }
        open('{');
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                next();
            }
            key(id.apply(sorted.get(i)));
            body.accept(sorted.get(i));
        }
        close('}');
    }

    private void intMap(Map<String, Integer> map) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        open('{');
        boolean first = true;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) {
                next();
            }
            first = false;
            key(entry.getKey()).append(entry.getValue());
        }
        close('}');
    }

    private void stacks(List<Stack> stacks) {
        if (stacks.isEmpty()) {
            out.append("[]");
            return;
        }
        open('[');
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) {
                next();
            }
            stack(stacks.get(i));
        }
        close(']');
    }

    private void stack(Stack stack) {
        out.append('{');
        out.append(stack.item() != null ? "\"item\": " + quote(stack.item()) : "\"tag\": " + quote(stack.tag()));
        out.append(", \"count\": ").append(stack.count()).append('}');
    }

    /** A stack without its count, as a grid key entry. */
    private void ingredient(Stack stack) {
        out.append('{');
        out.append(stack.item() != null ? "\"item\": " + quote(stack.item()) : "\"tag\": " + quote(stack.tag()));
        out.append('}');
    }

    private void strings(List<String> values) {
        out.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(quote(values.get(i)));
        }
        out.append(']');
    }

    private void open(char bracket) {
        out.append(bracket);
        depth++;
        newline();
    }

    private void close(char bracket) {
        depth--;
        newline();
        out.append(bracket);
    }

    private void next() {
        out.append(',');
        newline();
    }

    private void newline() {
        out.append('\n').append("  ".repeat(depth));
    }

    private StringBuilder key(String key) {
        return out.append(quote(key)).append(": ");
    }

    static String quote(String value) {
        StringBuilder quoted = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (c < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) c));
                    } else {
                        quoted.append(c);
                    }
                }
            }
        }
        return quoted.append('"').toString();
    }
}
