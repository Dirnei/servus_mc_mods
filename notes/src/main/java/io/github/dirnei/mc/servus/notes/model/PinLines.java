package io.github.dirnei.mc.servus.notes.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * The lines the HUD pin shows below the list name: the open tasks in list order, then the goals of
 * open tasks whose target is not reached, cut to a line count with a "+n more" line at the end.
 */
public final class PinLines {

    /** One line of the HUD pin. */
    public sealed interface Line {
    }

    /** An open task; {@link #label()} is the text as drawn. */
    public record TaskLine(String text, boolean sub) implements Line {

        /** "[ ] text", indented by two spaces for a subtask. */
        public String label() {
            return (sub ? "  " : "") + "[ ] " + text;
        }
    }

    /** A goal that is not reached yet, with the item's name and how many of it the viewer carries. */
    public record GoalLine(String itemName, int count, int target, int carried) implements Line {

        /** How the bar fills: the shared count, then the carried items on top. */
        public GoalFill fill() {
            return GoalFill.of(count, target, carried);
        }
    }

    /** How many lines were left out. */
    public record More(int count) implements Line {
    }

    private PinLines() {
    }

    /**
     * The lines of {@code list}, at most {@code maxLines} plus a {@link More} line when some were left out.
     *
     * @param itemName the name of an item from its registry id
     * @param carried  how many of an item, by registry id, the viewer carries
     */
    public static List<Line> of(NoteList list, int maxLines, Function<String, String> itemName, ToIntFunction<String> carried) {
        List<Line> all = new ArrayList<>();
        for (Task task : list.tasks()) {
            if (!task.isDone()) {
                all.add(new TaskLine(task.text(), task.sub()));
            }
        }
        for (Task task : list.tasks()) {
            if (task.isDone()) {
                continue;
            }
            for (Goal goal : task.goals()) {
                if (!goal.reached()) {
                    all.add(new GoalLine(itemName.apply(goal.itemId()), goal.count(), goal.target(), carried.applyAsInt(goal.itemId())));
                }
            }
        }
        int shown = Math.max(0, maxLines);
        if (all.size() <= shown) {
            return List.copyOf(all);
        }
        List<Line> cut = new ArrayList<>(all.subList(0, shown));
        cut.add(new More(all.size() - shown));
        return List.copyOf(cut);
    }
}
