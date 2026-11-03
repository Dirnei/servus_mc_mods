package io.github.dirnei.mc.servus.notes.client;

import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.FADED_INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.HEADING_INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.PAGE_MARGIN;

import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * The left page while no project is open: the projects of the active tab, each with its name, a dotted
 * leader to the number of open tasks and a line with scope, owner and a "pinned" mark; "+ New project"
 * at the bottom of the Mine tab. Clicking a project opens it.
 */
final class ProjectsPage {

    static final int SELECTED = 0xFFE5D0A0;
    static final int HOVERED = 0xFFE9D9B2;
    static final int MARK = 0xFFA8322A;
    private static final int LEADER = 0xFFA08A60;
    private static final int ENTRY_HEIGHT = 22;

    private final NotebookScreen screen;
    private int scroll;

    ProjectsPage(NotebookScreen screen) {
        this.screen = screen;
    }

    private static int x0(BookLayout book) {
        return book.leftPageX() + PAGE_MARGIN;
    }

    private static int contentWidth(BookLayout book) {
        return book.leftPageWidth() - 2 * PAGE_MARGIN - 4;
    }

    private static int entriesTop(BookLayout book) {
        return book.pageY() + 24;
    }

    void extract(GuiGraphicsExtractor graphics, BookLayout book, double mouseX, double mouseY) {
        Font font = screen.font();
        int x = x0(book);
        int width = contentWidth(book);
        graphics.text(font, Component.translatable("gui.servus_notes.notebook.projects"), x, book.pageY() + 8, HEADING_INK, false);
        List<ListView> lists = screen.tabLists();
        boolean mine = screen.activeTab() == NotebookTab.MINE;
        int pageBottom = book.pageBottom() - PAGE_MARGIN;
        int buttonY = pageBottom - PaperButton.HEIGHT;
        int bottom = mine ? buttonY - 4 : pageBottom;
        int top = entriesTop(book);
        if (lists.isEmpty()) {
            String empty = switch (screen.activeTab()) {
                case MINE -> "gui.servus_notes.notebook.no_projects";
                case SHARED -> "gui.servus_notes.notebook.no_projects_shared";
                default -> "gui.servus_notes.notebook.no_projects_here";
            };
            List<FormattedCharSequence> lines = font.split(Component.translatable(empty), width);
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(font, lines.get(i), x, top + 2 + i * 10, FADED_INK, false);
            }
        }
        int visible = Math.max(1, (bottom - top) / ENTRY_HEIGHT);
        scroll = Mth.clamp(scroll, 0, Math.max(0, lists.size() - visible));
        for (int i = 0; i < visible && scroll + i < lists.size(); i++) {
            extractEntry(graphics, font, lists.get(scroll + i).list(), x, width, top + i * ENTRY_HEIGHT, mouseX, mouseY);
        }
        if (mine) {
            Component label = Component.translatable("gui.servus_notes.notebook.new_project");
            PaperButton.draw(graphics, font, label, x, buttonY, width,
                    PageHits.over(mouseX, mouseY, x, buttonY, width, PaperButton.HEIGHT), INK);
            screen.hits().add(x, buttonY, width, PaperButton.HEIGHT, this::newProject);
        }
    }

    private void extractEntry(GuiGraphicsExtractor graphics, Font font, NoteList list, int x, int width, int y, double mouseX, double mouseY) {
        int top = y - 2;
        if (PageHits.over(mouseX, mouseY, x - 4, top, width + 6, ENTRY_HEIGHT - 1)) {
            graphics.fill(x - 4, top, x + width + 2, top + ENTRY_HEIGHT - 1, HOVERED);
        }
        String count = Integer.toString(list.openCount());
        int countX = x + width - font.width(count);
        String name = NotebookScreen.ellipsized(font, list.name(), countX - x - 6);
        graphics.text(font, name, x, y, INK, false);
        graphics.text(font, count, countX, y, INK, false);
        for (int dot = x + font.width(name) + 3; dot < countX - 2; dot += 3) {
            graphics.fill(dot, y + 7, dot + 1, y + 8, LEADER);
        }
        Component meta = Component.translatable("gui.servus_notes.notebook.entry_meta", ScopeSeal.word(list.scope()), list.ownerName());
        int metaWidth = width;
        if (ClientNotes.isPinned(list.id())) {
            Component pinned = Component.translatable("gui.servus_notes.notebook.pinned");
            int pinnedX = x + width - font.width(pinned);
            graphics.text(font, pinned, pinnedX, y + 10, MARK, false);
            metaWidth = pinnedX - x - 4;
        }
        graphics.text(font, NotebookScreen.ellipsized(font, meta.getString(), metaWidth), x, y + 10, FADED_INK, false);
        UUID id = list.id();
        screen.hits().add(x - 4, top, width + 6, ENTRY_HEIGHT - 1, () -> screen.openProject(id));
    }

    private void newProject() {
        UUID id = UUID.randomUUID();
        ClientNotes.send(new NoteEdit.CreateList(id, Component.translatable("gui.servus_notes.notebook.new_project_name").getString()));
        screen.openNewProject(id);
    }

    /** Scrolls the entries when the mouse is over this page. */
    boolean scrolled(BookLayout book, double mouseX, double mouseY, double amount) {
        if (!PageHits.over(mouseX, mouseY, book.leftPageX(), book.pageY(), book.leftPageWidth(), book.pageHeight())) {
            return false;
        }
        scroll -= (int) Math.signum(amount);
        return true;
    }
}
