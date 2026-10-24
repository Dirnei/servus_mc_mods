package io.github.dirnei.mc.servus.alchemy.client.journal;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.client.render.page.BookPageRenderer;
import io.github.dirnei.mc.servus.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationDimension;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationRecipes;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Entry;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Heading;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Line;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTable.Table;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTablePage;
import io.github.dirnei.mc.servus.alchemy.journal.ClientJournalProgress;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Draws one page of the calcination table: a column header, the group headings and a row per recipe. A row the
 * reader's team has tried shows its item, name and principles; any other row a question mark and its hint.
 */
public class CalcinationTablePageRenderer extends BookPageRenderer<CalcinationTablePage> {

    private static final int PAGE_WIDTH = 124;
    private static final int TITLE_SPACE = 19;
    private static final int HEADER_SPACE = 11;
    private static final int LINE = 9;
    private static final int ICON = 16;
    private static final int TEXT_X = ICON + 3;
    private static final int TEXT_WIDTH = PAGE_WIDTH - TEXT_X;
    private static final int MIN_COLUMN_GAP = 3;
    private static final int INK = 0xFF2B1D0E;
    private static final int FADED = 0xFF7A6A55;
    private static final int BOX = 0xFFE4D5B5;
    private static final String ELLIPSIS = "...";
    private static final Map<Principle, Integer> PRINCIPLE_INK = Map.of(
            Principle.SALT, 0xFF5E5A52,
            Principle.SULFUR, 0xFF8A6A00,
            Principle.MERCURY, 0xFF3F5F78);

    /** One recipe as the table shows it. */
    private record Row(String id, CalcinationDimension dimension, Principles output, List<ItemStack> burns) {
    }

    private static RecipeMap laidOutFor;
    private static final Map<Integer, Table<Row>> TABLES = new HashMap<>();

    public CalcinationTablePageRenderer(CalcinationTablePage page) {
        super(page);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float ticks) {
        int y = 0;
        if (page.part() == 0) {
            renderTitle(graphics, new BookTextHolder(Component.translatable(CalcinationTable.KEY + "title")), true, PAGE_WIDTH / 2, 0);
            y = TITLE_SPACE;
        }
        ClientLevel level = mc.level;
        Table<Row> table = level == null ? new Table<>(List.of(), 0) : table(level, page.last() ? page.part() + 1 : Integer.MAX_VALUE);
        if (page.part() >= table.pages().size()) {
            drawLines(graphics, Component.translatable(CalcinationTable.KEY + "blank"), 0, y, PAGE_WIDTH, 0, FADED);
            return;
        }
        int[] columns = columns();
        drawHeader(graphics, columns, y);
        y += HEADER_SPACE;
        Set<String> tried = ClientJournalProgress.tried();
        for (Line<Row> line : table.pages().get(page.part())) {
            y = switch (line) {
                case Heading<Row> heading -> drawHeading(graphics, heading.dimension(), y);
                case Entry<Row> entry -> drawRow(graphics, entry.row(), tried.contains(entry.row().id()), columns, y, level);
            };
        }
        if (page.last() && table.leftOut() > 0) {
            graphics.text(font, Component.translatable(CalcinationTable.KEY + "more", table.leftOut()), 0, y, FADED, false);
        }
    }

    /** The table for the client's current recipes, laid out for at most {@code maxPages} pages. */
    private static Table<Row> table(ClientLevel level, int maxPages) {
        RecipeMap map = ServusRecipes.recipeMap(level);
        if (map != laidOutFor) {
            laidOutFor = map;
            TABLES.clear();
        }
        return TABLES.computeIfAbsent(maxPages, limit -> CalcinationTable.layout(rows(level), Row::id, Row::dimension,
                CalcinationTable.PAGE, limit));
    }

    private static List<Row> rows(ClientLevel level) {
        return ServusRecipes.all(level, ServusAlchemyRecipes.CALCINATION.get()).stream()
                .map(holder -> row(level, holder))
                .filter(row -> !row.burns().isEmpty())
                .toList();
    }

    private static Row row(ClientLevel level, RecipeHolder<CalcinationRecipe> holder) {
        CalcinationRecipe recipe = holder.value();
        return new Row(holder.id().identifier().toString(), recipe.dimension(), recipe.output(), CalcinationRecipes.burns(level, holder));
    }

    /** The x of each principle's column, spread over the text width after the icon. */
    private int[] columns() {
        Principle[] principles = Principle.values();
        int total = 0;
        for (Principle principle : principles) {
            total += font.width(columnName(principle));
        }
        int gap = Math.max(MIN_COLUMN_GAP, (TEXT_WIDTH - total) / (principles.length - 1));
        int[] columns = new int[principles.length];
        int x = TEXT_X;
        for (Principle principle : principles) {
            columns[principle.ordinal()] = x;
            x += font.width(columnName(principle)) + gap;
        }
        return columns;
    }

    private static Component columnName(Principle principle) {
        return Component.translatable(CalcinationTable.KEY + "column." + principle.id());
    }

    private void drawHeader(GuiGraphicsExtractor graphics, int[] columns, int y) {
        for (Principle principle : Principle.values()) {
            graphics.text(font, columnName(principle), columns[principle.ordinal()], y, PRINCIPLE_INK.get(principle), false);
        }
    }

    private int drawHeading(GuiGraphicsExtractor graphics, CalcinationDimension dimension, int y) {
        Component heading = Component.translatable(CalcinationTable.headingKey(dimension)).withStyle(style -> style.withBold(true));
        graphics.text(font, heading, 0, y + 1, INK, false);
        return y + CalcinationTable.PAGE.heading();
    }

    private int drawRow(GuiGraphicsExtractor graphics, Row row, boolean tried, int[] columns, int y,
                        ClientLevel level) {
        if (tried) {
            ItemStack icon = row.burns().get((int) (level.getGameTime() / 20 % row.burns().size()));
            graphics.fakeItem(icon, 0, y);
            Component name = Component.translatableWithFallback(CalcinationTable.nameKey(row.id()), icon.getHoverName().getString());
            graphics.text(font, cut(name, TEXT_WIDTH), TEXT_X, y, INK, false);
            for (Principle principle : Principle.values()) {
                int amount = row.output().get(principle);
                graphics.text(font, amount > 0 ? Integer.toString(amount) : "-", columns[principle.ordinal()], y + LINE,
                        amount > 0 ? PRINCIPLE_INK.get(principle) : FADED, false);
            }
        } else {
            drawUnknownBox(graphics, y);
            Component hint = Component.translatableWithFallback(CalcinationTable.hintKey(row.id()),
                    Component.translatable(CalcinationTable.KEY + "untried").getString()).withStyle(style -> style.withItalic(true));
            drawLines(graphics, hint, TEXT_X, y, TEXT_WIDTH, 2, FADED);
        }
        return y + CalcinationTable.PAGE.row();
    }

    private void drawUnknownBox(GuiGraphicsExtractor graphics, int y) {
        graphics.fill(0, y, ICON, y + ICON, FADED);
        graphics.fill(1, y + 1, ICON - 1, y + ICON - 1, BOX);
        drawCenteredStringNoShadow(graphics, "?", ICON / 2, y + 4, INK, 1.0F);
    }

    /** The text cut to {@code width}, ending in "..." when it was longer. */
    private FormattedCharSequence cut(Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String shortened = font.plainSubstrByWidth(text.getString(), width - font.width(ELLIPSIS)).stripTrailing();
        return Component.literal(shortened + ELLIPSIS).withStyle(text.getStyle()).getVisualOrderText();
    }

    /** Draws wrapped text, at most {@code maxLines} lines when above 0, the last one cut with "..." when more follow. */
    private void drawLines(GuiGraphicsExtractor graphics, Component text, int x, int y, int width,
                           int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        boolean more = maxLines > 0 && lines.size() > maxLines;
        if (more) {
            lines = lines.subList(0, maxLines);
        }
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            if (more && i == lines.size() - 1) {
                line = cut(Component.literal(plain(line) + " " + ELLIPSIS).withStyle(text.getStyle()), width);
            }
            graphics.text(font, line, x, y, color, false);
            y += LINE;
        }
    }

    private static String plain(FormattedCharSequence line) {
        StringBuilder text = new StringBuilder();
        line.accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            return true;
        });
        return text.toString();
    }
}
