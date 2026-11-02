package io.github.dirnei.mc.servus.foundation.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.dirnei.mc.servus.foundation.command.ServusRoot;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import org.yaml.snakeyaml.Yaml;

/**
 * Compares the commands a mod registers under {@code /servus <branch>} with the Edict catalog
 * it ships: the same command keys (the fixed words), and per command the same parameters in
 * order with the same {@code required}. SnakeYAML is on the dev run classpath only, so call
 * this from GameTests alone.
 */
public final class CommandCatalogCheck {

    /** One parameter: its name and whether every invocation needs it. */
    public record Param(String name, boolean required) {

        @Override
        public String toString() {
            return required ? "<" + name + ">" : "[" + name + "]";
        }
    }

    private CommandCatalogCheck() {
    }

    /** Fails the test naming every difference between the mod's {@code /servus <branch>} commands and its catalog. */
    public static void assertMatches(GameTestHelper helper, String branch, Class<?> owner, String modId) {
        List<String> problems = problems(helper.getLevel().getServer().getCommands().getDispatcher(), branch, owner, modId);
        helper.assertTrue(problems.isEmpty(), "the commands and the catalog of " + modId + " differ:\n" + String.join("\n", problems));
    }

    /**
     * Every difference between the registered branch and the catalog, empty when they match. The
     * catalog is the resource {@code /edict/<modId>.edict.yaml} as the class {@code owner} of the mod sees it.
     */
    public static List<String> problems(CommandDispatcher<CommandSourceStack> dispatcher, String branch, Class<?> owner, String modId) {
        Map<String, List<Param>> registered = registered(dispatcher, branch);
        Map<String, List<Param>> listed = catalog(owner, "/edict/" + modId + ".edict.yaml");
        List<String> problems = new ArrayList<>();
        for (String key : new TreeSet<>(registered.keySet())) {
            if (!listed.containsKey(key)) {
                problems.add("missing from the catalog: " + key);
            } else if (!listed.get(key).equals(registered.get(key))) {
                problems.add("parameters differ for " + key + ": registered " + registered.get(key) + ", catalog " + listed.get(key));
            }
        }
        for (String key : new TreeSet<>(listed.keySet())) {
            if (!registered.containsKey(key)) {
                problems.add("listed but not registered: " + key);
            }
        }
        return problems;
    }

    /**
     * The commands under {@code /servus <branch>}: each key is the branch and the literals before the
     * first argument; the parameters come from the longest executable path, those beyond the shortest
     * one optional.
     */
    public static Map<String, List<Param>> registered(CommandDispatcher<CommandSourceStack> dispatcher, String branch) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(ServusRoot.ROOT);
        CommandNode<CommandSourceStack> node = root == null ? null : root.getChild(branch);
        Map<String, List<List<String>>> paths = new LinkedHashMap<>();
        if (node != null) {
            walk(node, List.of(branch), List.of(), paths);
        }
        Map<String, List<Param>> commands = new LinkedHashMap<>();
        paths.forEach((key, variants) -> {
            List<String> longest = variants.stream().max((a, b) -> Integer.compare(a.size(), b.size())).orElseThrow();
            int needed = variants.stream().mapToInt(List::size).min().orElse(0);
            List<Param> params = new ArrayList<>();
            for (int i = 0; i < longest.size(); i++) {
                params.add(new Param(longest.get(i), i < needed));
            }
            commands.put(key, params);
        });
        return commands;
    }

    private static void walk(CommandNode<CommandSourceStack> node, List<String> words, List<String> args, Map<String, List<List<String>>> paths) {
        if (node.getCommand() != null) {
            paths.computeIfAbsent(String.join(" ", words), key -> new ArrayList<>()).add(args);
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode<CommandSourceStack> literal) {
                if (!args.isEmpty()) {
                    throw new IllegalStateException("The literal " + literal.getLiteral() + " follows a parameter in "
                            + String.join(" ", words) + "; commands start with their fixed words");
                }
                List<String> more = new ArrayList<>(words);
                more.add(literal.getLiteral());
                walk(child, more, args, paths);
            } else if (child instanceof ArgumentCommandNode<CommandSourceStack, ?> argument) {
                List<String> more = new ArrayList<>(args);
                more.add(argument.getName());
                walk(child, words, more, paths);
            }
        }
    }

    /** The commands of the catalog resource with their parameters. */
    @SuppressWarnings("unchecked")
    public static Map<String, List<Param>> catalog(Class<?> owner, String resource) {
        Map<String, Object> catalog;
        try (InputStream in = owner.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("No catalog " + resource);
            }
            catalog = new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
        Map<String, List<Param>> commands = new LinkedHashMap<>();
        Map<String, Object> listed = (Map<String, Object>) catalog.getOrDefault("commands", Map.of());
        listed.forEach((key, value) -> {
            List<Param> params = new ArrayList<>();
            for (Object entry : (List<Object>) ((Map<String, Object>) value).getOrDefault("params", List.of())) {
                Map<String, Object> param = (Map<String, Object>) entry;
                params.add(new Param(String.valueOf(param.get("name")), Boolean.TRUE.equals(param.get("required"))));
            }
            commands.put(key, params);
        });
        return commands;
    }
}
