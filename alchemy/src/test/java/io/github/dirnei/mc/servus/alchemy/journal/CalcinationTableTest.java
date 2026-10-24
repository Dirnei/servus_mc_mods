package io.github.dirnei.mc.servus.alchemy.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationDimension;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Entry;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Heading;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Heights;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Line;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Table;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalcinationTableTest {

    private record Row(String id, CalcinationDimension dimension) {
    }

    /** Room for a heading and two rows per page. */
    private static final Heights SMALL = new Heights(50, 50, 10, 20, 10);

    private static Table<Row> layout(List<Row> rows, Heights heights, int maxPages) {
        return CalcinationTable.layout(rows, Row::id, Row::dimension, heights, maxPages);
    }

    private static List<Row> rows(CalcinationDimension dimension, String... ids) {
        List<Row> rows = new ArrayList<>();
        for (String id : ids) {
            rows.add(new Row(id, dimension));
        }
        return rows;
    }

    private static String describe(Line<Row> line) {
        return switch (line) {
            case Heading<Row> heading -> "#" + heading.dimension().id();
            case Entry<Row> entry -> entry.row().id();
        };
    }

    private static List<List<String>> describe(Table<Row> table) {
        return table.pages().stream().map(page -> page.stream().map(CalcinationTableTest::describe).toList()).toList();
    }

    @Test
    void groupsInDimensionOrderAndRowsById() {
        List<Row> rows = new ArrayList<>();
        rows.addAll(rows(CalcinationDimension.END, "e:pearl"));
        rows.addAll(rows(CalcinationDimension.NETHER, "n:wart", "n:blaze"));
        rows.addAll(rows(CalcinationDimension.OVERWORLD, "o:stone", "o:bones"));
        Table<Row> table = layout(rows, new Heights(1000, 1000, 10, 20, 10), 10);
        assertEquals(List.of(List.of("#overworld", "o:bones", "o:stone", "#nether", "n:blaze", "n:wart", "#end", "e:pearl")),
                describe(table));
        assertEquals(0, table.leftOut());
    }

    @Test
    void continuedGroupRepeatsItsHeading() {
        Table<Row> table = layout(rows(CalcinationDimension.OVERWORLD, "a", "b", "c"), SMALL, 10);
        assertEquals(List.of(List.of("#overworld", "a", "b"), List.of("#overworld", "c")), describe(table));
    }

    @Test
    void headingNeverEndsAPage() {
        List<Row> rows = new ArrayList<>(rows(CalcinationDimension.OVERWORLD, "a"));
        rows.addAll(rows(CalcinationDimension.NETHER, "b"));
        // After "#overworld a" 20 px are left: a heading fits, a heading with its row does not.
        Table<Row> table = layout(rows, SMALL, 10);
        assertEquals(List.of(List.of("#overworld", "a"), List.of("#nether", "b")), describe(table));
        for (List<Line<Row>> page : table.pages()) {
            assertInstanceOf(Entry.class, page.getLast(), "a page should end with a row");
        }
    }

    @Test
    void emptyGroupHasNoHeading() {
        List<Row> rows = new ArrayList<>(rows(CalcinationDimension.OVERWORLD, "a"));
        rows.addAll(rows(CalcinationDimension.END, "b"));
        Table<Row> table = layout(rows, new Heights(1000, 1000, 10, 20, 10), 10);
        assertEquals(List.of(List.of("#overworld", "a", "#end", "b")), describe(table));
    }

    @Test
    void noRowsGiveNoPages() {
        Table<Row> table = layout(List.of(), SMALL, 3);
        assertTrue(table.pages().isEmpty());
        assertEquals(0, table.leftOut());
    }

    @Test
    void pageLimitReservesTheFooterAndCountsTheRest() {
        Table<Row> table = layout(rows(CalcinationDimension.OVERWORLD, "a", "b", "c", "d", "e"), SMALL, 2);
        // The second page loses 10 px to the footer, so it holds only the heading and one row.
        assertEquals(List.of(List.of("#overworld", "a", "b"), List.of("#overworld", "c")), describe(table));
        assertEquals(2, table.leftOut());
    }

    @Test
    void exactFitNeedsNoFooter() {
        Table<Row> table = layout(rows(CalcinationDimension.OVERWORLD, "a", "b", "c", "d"), SMALL, 2);
        assertEquals(List.of(List.of("#overworld", "a", "b"), List.of("#overworld", "c", "d")), describe(table));
        assertEquals(0, table.leftOut());
    }

    @Test
    void shippedRowsFitTheirPages() {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            rows.add(new Row("o" + (char) ('a' + i), CalcinationDimension.OVERWORLD));
        }
        for (int i = 0; i < 9; i++) {
            rows.add(new Row("n" + (char) ('a' + i), CalcinationDimension.NETHER));
        }
        for (int i = 0; i < 6; i++) {
            rows.add(new Row("e" + (char) ('a' + i), CalcinationDimension.END));
        }
        Table<Row> table = layout(rows, CalcinationTable.PAGE, Integer.MAX_VALUE);
        assertEquals(6, table.pages().size());
        assertEquals(0, table.leftOut());
        assertEquals(30, table.pages().stream().flatMap(List::stream).filter(line -> line instanceof Entry).count());
        for (int i = 0; i < table.pages().size(); i++) {
            int room = i == 0 ? CalcinationTable.PAGE.first() : CalcinationTable.PAGE.rest();
            int used = table.pages().get(i).stream()
                    .mapToInt(line -> line instanceof Heading ? CalcinationTable.PAGE.heading() : CalcinationTable.PAGE.row()).sum();
            assertTrue(used <= room, "page " + i + " uses " + used + " of " + room);
        }
    }

    @Test
    void keysDropTheCalcinationFolder() {
        assertEquals("journal.servus_alchemy.calcination.servus_alchemy.blaze_rod.hint",
                CalcinationTable.hintKey("servus_alchemy:calcination/blaze_rod"));
        assertEquals("journal.servus_alchemy.calcination.servus_alchemy.blaze_rod.name",
                CalcinationTable.nameKey("servus_alchemy:calcination/blaze_rod"));
    }

    @Test
    void keysKeepOtherPathsWithDots() {
        assertEquals("journal.servus_alchemy.calcination.mypack.burning.ash_wood.hint",
                CalcinationTable.hintKey("mypack:burning/ash_wood"));
        assertEquals("journal.servus_alchemy.calcination.mypack.trees.ash_wood.hint",
                CalcinationTable.hintKey("mypack:calcination/trees/ash_wood"));
    }

    @Test
    void headingKeyNamesTheDimension() {
        assertEquals("journal.servus_alchemy.calcination.dimension.nether", CalcinationTable.headingKey(CalcinationDimension.NETHER));
    }
}
