package io.github.dirnei.mc.servus.notes.client;

import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.FADED_INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.PAGE_MARGIN;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.RULE_PITCH;

import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Scope;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The Share popup over the right page, for the project's owner: scope buttons with a hint, the guests
 * with edit or read and remove, and a picker of the players who have been on the server. Its places
 * follow the book's size and are worked out at the start of every frame.
 */
final class SharePopup {

    private static final int PICKER_ROWS = 6;

    private static final int PAPER = 0xFFF1E4C3;
    private static final int EDGE = 0xFFC9B07A;
    private static final int STRIPE = 0xFFE9D9B2;
    private static final int REMOVE = 0xFFA8322A;

    private final NotebookScreen screen;
    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;
    private int inX0;
    private int inX1;
    private int headingY;
    private int scopesY;
    private int hintY;
    private int guestsY;
    private int guestRows;
    private int addY;
    private int noteY;
    private int doneY;
    private @Nullable UUID listId;
    private int guestScroll;
    private boolean pickerOpen;
    private int pickerScroll;
    private @Nullable Person picked;

    SharePopup(NotebookScreen screen) {
        this.screen = screen;
    }

    boolean isOpen() {
        return listId != null;
    }

    void open(UUID list) {
        listId = list;
        guestScroll = 0;
        pickerOpen = false;
        picked = null;
    }

    void close() {
        listId = null;
        pickerOpen = false;
    }

    /** Escape closes the picker first, then the popup. */
    boolean escape() {
        if (pickerOpen) {
            pickerOpen = false;
            return true;
        }
        if (isOpen()) {
            close();
            return true;
        }
        return false;
    }

    private void place(BookLayout book) {
        pageX = book.rightPageX();
        pageY = book.pageY();
        pageWidth = book.rightPageWidth();
        pageHeight = book.pageHeight();
        inX0 = pageX + PAGE_MARGIN + 2;
        inX1 = pageX + pageWidth - PAGE_MARGIN;
        int y0 = pageY + 4;
        int y1 = book.pageBottom() - 4;
        headingY = y0 + 6;
        scopesY = y0 + 18;
        hintY = y0 + 34;
        guestsY = y0 + 56;
        doneY = y1 - PaperButton.HEIGHT - 4;
        noteY = doneY - 12;
        addY = noteY - PaperButton.HEIGHT - 4;
        guestRows = Math.max(1, (addY - 4 - guestsY) / RULE_PITCH);
    }

    /** Draws the popup for {@code list}; closes it when it shows another list. */
    void extract(GuiGraphicsExtractor graphics, Font font, BookLayout book, NoteList list, double mouseX, double mouseY) {
        if (!list.id().equals(listId)) {
            close();
            return;
        }
        place(book);
        PageHits hits = screen.hits();
        hits.add(pageX, pageY, pageWidth, pageHeight, () -> pickerOpen = false);
        int x0 = pageX + 4;
        int x1 = pageX + pageWidth - 4;
        graphics.fill(x0, pageY + 4, x1, book.pageBottom() - 4, PAPER);
        graphics.outline(x0, pageY + 4, x1 - x0, pageHeight - 8, EDGE);
        Component heading = Component.translatable("gui.servus_notes.notebook.share.title", list.name());
        graphics.text(font, NotebookScreen.ellipsized(font, heading.getString(), inX1 - inX0), inX0, headingY, INK, false);
        extractScopes(graphics, font, list, mouseX, mouseY);
        List<FormattedCharSequence> hint = font.split(Component.translatable("gui.servus_notes.notebook.share.hint." + list.scope().key()), inX1 - inX0);
        for (int i = 0; i < Math.min(2, hint.size()); i++) {
            graphics.text(font, hint.get(i), inX0, hintY + i * 9, FADED_INK, false);
        }
        extractGuests(graphics, font, list, mouseX, mouseY);
        extractAddRow(graphics, font, list, mouseX, mouseY);
        graphics.text(font, Component.translatable("gui.servus_notes.notebook.share.known_note"), inX0, noteY, FADED_INK, false);
        Component done = Component.translatable("gui.servus_notes.notebook.share.done");
        int doneWidth = PaperButton.width(font, done);
        int doneX = inX1 - doneWidth;
        PaperButton.draw(graphics, font, done, doneX, doneY, doneWidth, PageHits.over(mouseX, mouseY, doneX, doneY, doneWidth, PaperButton.HEIGHT), INK);
        hits.add(doneX, doneY, doneWidth, PaperButton.HEIGHT, this::close);
        if (pickerOpen) {
            extractPicker(graphics, font, list, mouseX, mouseY);
        }
    }

    private void extractScopes(GuiGraphicsExtractor graphics, Font font, NoteList list, double mouseX, double mouseY) {
        int x = inX0;
        for (Scope scope : offeredScopes(list)) {
            Component label = ScopeSeal.word(scope);
            int width = PaperButton.width(font, label) + 6;
            boolean on = list.scope() == scope;
            PaperButton.drawChoice(graphics, font, label, x, scopesY, width,
                    PageHits.over(mouseX, mouseY, x, scopesY, width, PaperButton.HEIGHT), on);
            if (!on) {
                screen.hits().add(x, scopesY, width, PaperButton.HEIGHT, () -> ClientNotes.send(new NoteEdit.SetScope(list.id(), scope)));
            }
            x += width + 3;
        }
    }

    /** Private and Server always; Team while the player is in a party, or while the list already is a Team list. */
    private static List<Scope> offeredScopes(NoteList list) {
        boolean team = ClientNotes.teamsInstalled() && ClientNotes.party().isPresent() || list.scope() == Scope.TEAM;
        return team ? List.of(Scope.PRIVATE, Scope.TEAM, Scope.SERVER) : List.of(Scope.PRIVATE, Scope.SERVER);
    }

    private void extractGuests(GuiGraphicsExtractor graphics, Font font, NoteList list, double mouseX, double mouseY) {
        List<Guest> guests = list.guests();
        if (guests.isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_notes.notebook.share.no_guests"), inX0, guestsY + 2, FADED_INK, false);
            return;
        }
        guestScroll = Mth.clamp(guestScroll, 0, Math.max(0, guests.size() - guestRows));
        Component edit = Component.translatable("gui.servus_notes.notebook.share.edit");
        Component read = Component.translatable("gui.servus_notes.notebook.share.read");
        Component remove = Component.literal("x");
        int removeWidth = PaperButton.width(font, remove);
        int readWidth = PaperButton.width(font, read);
        int editWidth = PaperButton.width(font, edit);
        int removeX = inX1 - removeWidth;
        int readX = removeX - 4 - readWidth;
        int editX = readX - editWidth;
        for (int row = 0; row < guestRows && guestScroll + row < guests.size(); row++) {
            Guest guest = guests.get(guestScroll + row);
            int top = guestsY + row * RULE_PITCH;
            if (row % 2 == 0) {
                graphics.fill(inX0 - 2, top, inX1 + 2, top + RULE_PITCH, STRIPE);
            }
            graphics.text(font, NotebookScreen.ellipsized(font, guest.who().name(), editX - inX0 - 4), inX0, top + 2, INK, false);
            UUID id = guest.who().id();
            choice(graphics, font, edit, editX, top, editWidth, guest.right() == Guest.Right.EDIT, list, id, Guest.Right.EDIT, mouseX, mouseY);
            choice(graphics, font, read, readX, top, readWidth, guest.right() == Guest.Right.READ, list, id, Guest.Right.READ, mouseX, mouseY);
            PaperButton.draw(graphics, font, remove, removeX, top, removeWidth,
                    PageHits.over(mouseX, mouseY, removeX, top, removeWidth, PaperButton.HEIGHT), REMOVE);
            screen.hits().add(removeX, top, removeWidth, PaperButton.HEIGHT, () -> ClientNotes.send(new NoteEdit.RemoveGuest(list.id(), id)));
        }
        if (guests.size() > guestRows) {
            int height = guestRows * RULE_PITCH;
            int thumb = Math.max(6, height * guestRows / guests.size());
            int thumbTop = guestsY + (height - thumb) * guestScroll / (guests.size() - guestRows);
            graphics.fill(inX1 + 1, thumbTop, inX1 + 3, thumbTop + thumb, FADED_INK);
        }
    }

    private void choice(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, boolean on,
                        NoteList list, UUID guest, Guest.Right right, double mouseX, double mouseY) {
        PaperButton.drawChoice(graphics, font, label, x, y, width, PageHits.over(mouseX, mouseY, x, y, width, PaperButton.HEIGHT), on);
        if (!on) {
            screen.hits().add(x, y, width, PaperButton.HEIGHT, () -> ClientNotes.send(new NoteEdit.SetGuestRight(list.id(), guest, right)));
        }
    }

    private void extractAddRow(GuiGraphicsExtractor graphics, Font font, NoteList list, double mouseX, double mouseY) {
        List<Person> candidates = candidates(list);
        if (picked != null && candidates.stream().noneMatch(person -> person.id().equals(picked.id()))) {
            picked = null;
        }
        Component add = Component.translatable("gui.servus_notes.notebook.share.add_guest");
        int addWidth = PaperButton.width(font, add);
        int addX = inX1 - addWidth;
        int boxX1 = addX - 4;
        boolean hovered = PageHits.over(mouseX, mouseY, inX0, addY, boxX1 - inX0, PaperButton.HEIGHT);
        Component shown = picked != null ? Component.literal(picked.name())
                : Component.translatable(candidates.isEmpty() ? "gui.servus_notes.notebook.share.nobody" : "gui.servus_notes.notebook.share.pick");
        graphics.fill(inX0, addY, boxX1, addY + PaperButton.HEIGHT - 1, hovered && !candidates.isEmpty() ? 0xFFFBF3DE : 0xFFF7EDD3);
        graphics.outline(inX0, addY, boxX1 - inX0, PaperButton.HEIGHT - 1, EDGE);
        graphics.text(font, NotebookScreen.ellipsized(font, shown, boxX1 - inX0 - 14), inX0 + 3, addY + 2, picked != null ? INK : FADED_INK, false);
        graphics.text(font, Component.literal(pickerOpen ? "^" : "v"), boxX1 - 8, addY + 2, FADED_INK, false);
        if (!candidates.isEmpty()) {
            screen.hits().add(inX0, addY, boxX1 - inX0, PaperButton.HEIGHT, () -> {
                pickerOpen = !pickerOpen;
                pickerScroll = 0;
            });
        }
        PaperButton.draw(graphics, font, add, addX, addY, addWidth, PageHits.over(mouseX, mouseY, addX, addY, addWidth, PaperButton.HEIGHT),
                picked != null ? INK : FADED_INK);
        if (picked != null) {
            Person guest = picked;
            screen.hits().add(addX, addY, addWidth, PaperButton.HEIGHT, () -> {
                ClientNotes.send(new NoteEdit.AddGuest(list.id(), guest, Guest.Right.READ));
                picked = null;
            });
        }
    }

    /** The dropdown of candidates above the picker box; any click outside it closes it. */
    private void extractPicker(GuiGraphicsExtractor graphics, Font font, NoteList list, double mouseX, double mouseY) {
        List<Person> candidates = candidates(list);
        if (candidates.isEmpty()) {
            pickerOpen = false;
            return;
        }
        int rows = Math.min(PICKER_ROWS, candidates.size());
        pickerScroll = Mth.clamp(pickerScroll, 0, candidates.size() - rows);
        int top = addY - rows * RULE_PITCH - 1;
        screen.hits().add(pageX, pageY, pageWidth, pageHeight, () -> pickerOpen = false);
        graphics.fill(inX0, top, inX1, addY, PAPER);
        graphics.outline(inX0, top, inX1 - inX0, addY - top, EDGE);
        for (int row = 0; row < rows; row++) {
            Person person = candidates.get(pickerScroll + row);
            int y = top + 1 + row * RULE_PITCH;
            if (PageHits.over(mouseX, mouseY, inX0, y, inX1 - inX0, RULE_PITCH)) {
                graphics.fill(inX0 + 1, y, inX1 - 1, y + RULE_PITCH, STRIPE);
            }
            graphics.text(font, NotebookScreen.ellipsized(font, person.name(), inX1 - inX0 - 6), inX0 + 3, y + 2, INK, false);
            screen.hits().add(inX0, y, inX1 - inX0, RULE_PITCH, () -> {
                picked = person;
                pickerOpen = false;
            });
        }
    }

    /** Known players who are neither the owner nor a guest yet, by name. */
    private static List<Person> candidates(NoteList list) {
        List<Person> candidates = new ArrayList<>();
        for (Person person : ClientNotes.knownPlayers()) {
            if (!person.id().equals(list.owner()) && list.guest(person.id()).isEmpty()) {
                candidates.add(person);
            }
        }
        candidates.sort(Comparator.comparing(person -> person.name().toLowerCase(Locale.ROOT)));
        return candidates;
    }

    /** Scrolls the picker or the guests while the popup is open; true when the popup took the wheel. */
    boolean scrolled(double amount) {
        if (!isOpen()) {
            return false;
        }
        if (pickerOpen) {
            pickerScroll -= (int) Math.signum(amount);
        } else {
            guestScroll -= (int) Math.signum(amount);
        }
        return true;
    }
}
