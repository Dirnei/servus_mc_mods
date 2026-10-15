package io.github.dirnei.mc.servus.alchemy.client.journal;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.client.render.page.BookPageRenderer;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.ClientJournalProgress;
import io.github.dirnei.mc.servus.alchemy.journal.JournalProgress.ChapterProgress;
import io.github.dirnei.mc.servus.alchemy.journal.JournalProgress.TaskProgress;
import io.github.dirnei.mc.servus.alchemy.journal.JournalTasksPage;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/** Draws a chapter's tasks: a box per task, ticked when done, its name, and the count over a small bar. */
public class JournalTasksPageRenderer extends BookPageRenderer<JournalTasksPage> {

    private static final int PAGE_WIDTH = 124;
    private static final int LINE = 9;
    private static final int BOX = 7;
    private static final int TEXT_X = BOX + 3;
    private static final int INK = 0xFF2B1D0E;
    private static final int FADED = 0xFF7A6A55;
    private static final int DONE = 0xFF2F7A1F;
    private static final int BAR_BACK = 0x30000000;
    private static final int BAR = 0xFF8A5A2B;

    private static final String KEY = "journal." + ServusAlchemyIds.MOD_ID + ".tasks.";

    public JournalTasksPageRenderer(JournalTasksPage page) {
        super(page);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float ticks) {
        renderTitle(graphics, new BookTextHolder(Component.translatable(KEY + "title")), true, PAGE_WIDTH / 2, 0);
        int y = 19;
        Optional<ChapterProgress> chapter = ClientJournalProgress.chapter(page.chapter());
        if (chapter.isEmpty()) {
            drawLines(graphics, Component.translatable(KEY + "waiting"), 0, y, PAGE_WIDTH, FADED);
            return;
        }
        if (chapter.get().finished()) {
            drawCenteredStringNoShadow(graphics, Component.translatable(KEY + "finished").getVisualOrderText(),
                    PAGE_WIDTH / 2, y, DONE, 1.0F);
            y += LINE + 4;
        }
        for (TaskProgress task : chapter.get().tasks()) {
            y = drawTask(graphics, task, y);
        }
    }

    /** Draws one task from {@code y} and returns where the next one starts. */
    private int drawTask(GuiGraphicsExtractor graphics, TaskProgress task, int y) {
        drawBox(graphics, y, task.done());
        int nameBottom = drawLines(graphics, taskName(task), TEXT_X, y, PAGE_WIDTH - TEXT_X, task.done() ? FADED : INK);
        String count = task.count() + " / " + task.needed();
        int countWidth = font.width(count);
        int barRight = PAGE_WIDTH - countWidth - 4;
        int barY = nameBottom + 3;
        graphics.fill(TEXT_X, barY, barRight, barY + 2, BAR_BACK);
        int filled = (int) ((long) (barRight - TEXT_X) * task.count() / task.needed());
        graphics.fill(TEXT_X, barY, TEXT_X + filled, barY + 2, task.done() ? DONE : BAR);
        graphics.text(font, count, PAGE_WIDTH - countWidth, nameBottom, task.done() ? DONE : INK, false);
        return nameBottom + LINE + 4;
    }

    private void drawBox(GuiGraphicsExtractor graphics, int y, boolean done) {
        graphics.fill(0, y, BOX, y + BOX, INK);
        graphics.fill(1, y + 1, BOX - 1, y + BOX - 1, done ? DONE : 0xFFF2E6CC);
    }

    /** Draws wrapped text and returns the y below its last line. */
    private int drawLines(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, x, y, color, false);
            y += LINE;
        }
        return y;
    }

    private Component taskName(TaskProgress task) {
        Identifier chapter = Identifier.parse(page.chapter());
        String key = "journal." + chapter.getNamespace() + "." + chapter.getPath() + ".task." + task.id();
        return Component.translatableWithFallback(key, task.id());
    }
}
