package io.github.dirnei.mc.servus.delicacies.teller;

import static io.github.dirnei.mc.servus.delicacies.teller.MealPart.food;

import java.util.List;
import java.util.stream.Stream;

/**
 * The plated meals and how they look on the plate. Base parts stay until the meal is finished; of the
 * {@link MealPortions#FULL} portion parts the first {@code left} are shown, so the last one is eaten first.
 */
public enum MealKind {

    ROAST_PORK_PLATE("roast_pork_plate",
            List.of(food("plate_gravy", 3, 1, 6, 10, 1.2F, 13)),
            List.of(food("plate_crackling", "plate_roast", 3.5F, 1.2F, 7.5F, 9.5F, 3.4F, 12),
                    food("plate_dumpling", 4.5F, 1, 3, 8.5F, 5, 7),
                    food("plate_dumpling", 9.5F, 1, 3, 13.5F, 5, 7),
                    food("plate_red_cabbage", 10.05F, 1, 9, 14, 3.5F, 12.5F)),
            List.of()),
    SCHNITZEL_PLATE("schnitzel_plate",
            List.of(),
            List.of(food("plate_schnitzel", 3, 1, 3, 10, 2, 8),
                    food("plate_potato_salad", 9.5F, 1, 9, 13.5F, 3, 13),
                    food("plate_schnitzel", 3, 1, 8.5F, 9, 2, 13),
                    food("plate_potato_salad", 10.5F, 1, 3.5F, 13.5F, 2.5F, 7)),
            List.of()),
    WEISSWURST_BREAKFAST("weisswurst_breakfast",
            Breakfast.BASE, Breakfast.PORTIONS, List.of()),
    WEISSWURST_BREAKFAST_WITH_WEISSBIER("weisswurst_breakfast_with_weissbier",
            Breakfast.BASE, Breakfast.PORTIONS,
            List.of(food("plate_weissbier_top", "plate_weissbier_side", 3.5F, 1, 9.5F, 6.5F, 9, 12.5F)));

    private final String id;
    private final List<MealPart> base;
    private final List<MealPart> portions;
    private final List<MealPart> extras;

    MealKind(String id, List<MealPart> base, List<MealPart> portions, List<MealPart> extras) {
        if (portions.size() != MealPortions.FULL) {
            throw new IllegalArgumentException(id + " needs one part per portion");
        }
        this.id = id;
        this.base = base;
        this.portions = portions;
        this.extras = extras;
    }

    /** The block and item id. */
    public String id() {
        return id;
    }

    /** The parts that stay until the meal is finished: the base and the extras. */
    public List<MealPart> lasting() {
        return Stream.concat(base.stream(), extras.stream()).toList();
    }

    /** The part of each portion, in the order they appear. */
    public List<MealPart> portions() {
        return portions;
    }

    /** Whether the meal comes with a beer that is handed over when it is finished. */
    public boolean withBeer() {
        return this == WEISSWURST_BREAKFAST_WITH_WEISSBIER;
    }

    /** The meal of {@code id}. */
    public static MealKind byId(String id) {
        for (MealKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("no meal " + id);
    }

    /** The Weisswurst breakfast's parts, shared by both breakfasts. */
    private static final class Breakfast {

        static final List<MealPart> BASE = List.of(food("plate_mustard", 10, 1, 10, 13, 1.3F, 13));
        static final List<MealPart> PORTIONS = List.of(food("plate_weisswurst", 3, 1, 2.5F, 10, 3, 4.5F),
                food("plate_pretzel", 10.5F, 1, 3, 13.5F, 3, 5),
                food("plate_weisswurst", 3, 1, 5.5F, 10, 3, 7.5F),
                food("plate_pretzel", 10.5F, 1, 5.5F, 13.5F, 3, 7.5F));
    }
}
