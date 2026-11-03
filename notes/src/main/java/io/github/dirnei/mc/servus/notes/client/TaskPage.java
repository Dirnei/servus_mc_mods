package io.github.dirnei.mc.servus.notes.client;

import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.FADED_INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.HEADING_INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.PAGE_MARGIN;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.RULE;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.Ago;
import io.github.dirnei.mc.servus.notes.model.DoneRecord;
import io.github.dirnei.mc.servus.notes.model.Goal;
import io.github.dirnei.mc.servus.notes.model.GoalFill;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.NoteEdits;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Places;
import io.github.dirnei.mc.servus.notes.model.Rights;
import io.github.dirnei.mc.servus.notes.model.Spot;
import io.github.dirnei.mc.servus.notes.model.Task;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The right page of an open project: the chosen task's name as a heading with a bin to delete it, who
 * does it, its item goals with typed counts, its place with a name, and its notes filling the rest of the page. Every
 * value is a field or a labelled button for players who may change it and plain text for the others.
 * The task's name and tick live in the task list on the left. The item picker and the Share popup open
 * over this page.
 */
final class TaskPage {

    private static final int ICON = 16;
    private static final int GOAL_ROW = 18;
    private static final int AMOUNT_WIDTH = 36;
    private static final int SECTION_GAP = 5;
    private static final int LABEL_HEIGHT = 13;
    private static final int NOTES_MIN = 30;
    private static final int ASSIGN_ROW = 11;
    private static final int ASSIGN_ROWS = 8;
    private static final long CONFIRM_MILLIS = 3000;
    private static final int BIN = 9;
    private static final Identifier BIN_SPRITE = Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notebook/bin");
    private static final Identifier BIN_ARMED_SPRITE = Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notebook/bin_armed");

    private static final int DONE_BY = 0xFF2E6B1F;
    private static final int REMOVE = 0xFFA8322A;
    private static final int BAR_BACK = 0xFFE9DCBC;
    private static final int BAR_FILL = 0xFF4F8A32;
    private static final int BAR_FULL = 0xFFC9A42A;
    private static final int BAR_CARRIED = 0xFFE8C547;
    private static final int CHIP = 0xFFFFF6D8;
    private static final int CHIP_ON = 0xFFFFE98A;
    private static final int CHIP_ON_EDGE = 0xFFC9A400;
    private static final int POPUP = 0xFFF1E4C3;
    private static final int POPUP_EDGE = 0xFFC9B07A;

    /** A count or target being typed. */
    private record AmountEdit(UUID taskId, UUID goalId, boolean target) {
    }

    /** A place on the page this frame. */
    private record Rect(int x, int y, int width, int height) {

        boolean contains(double mouseX, double mouseY) {
            return PageHits.over(mouseX, mouseY, x, y, width, height);
        }
    }

    /** A button of the place section. */
    private record PlaceButton(Component label, Runnable run, boolean lit, int ink) {
    }

    private static final Rect NOWHERE = new Rect(0, 0, 0, 0);

    private final NotebookScreen screen;
    private final NotebookMotion motion;
    private final SharePopup share;
    private final ItemPickerPopup picker;
    private MultiLineEditBox notesBox;
    private EditBox amountBox;
    private EditBox placeNameBox;
    private @Nullable UUID taskId;
    /** The notes and place name the fields last took from the server; a field follows the server only when they change. */
    private @Nullable String syncedNotes;
    private @Nullable String syncedPlaceName;
    private @Nullable AmountEdit amountEdit;
    /** The goal whose target opens for typing once it arrives from the server, after it was picked. */
    private @Nullable UUID targetWhenArrives;
    private long deleteArmedAt = Long.MIN_VALUE;
    private boolean assignOpen;
    private int assignScroll;
    private int goalScroll;
    /** Goals whose carried items this viewer just added, with how many they carried then; the button hides until that changes. */
    private final Map<UUID, Integer> submitted = new HashMap<>();
    private Rect notesField = NOWHERE;
    private Rect amountField = NOWHERE;
    private Rect placeNameField = NOWHERE;
    private Rect deleteButton = NOWHERE;
    private Rect assignButton = NOWHERE;
    private Rect goalsArea = NOWHERE;

    TaskPage(NotebookScreen screen, NotebookMotion motion) {
        this.screen = screen;
        this.motion = motion;
        this.share = new SharePopup(screen);
        this.picker = new ItemPickerPopup(screen);
    }

    /** Creates the page's fields for the book's size, keeping what they held across a resize. */
    void init(Font font, Consumer<AbstractWidget> add) {
        String notes = notesBox == null ? "" : notesBox.getValue();
        String amount = amountBox == null ? "" : amountBox.getValue();
        String placeName = placeNameBox == null ? "" : placeNameBox.getValue();
        BookLayout book = screen.layout();
        notesBox = MultiLineEditBox.builder()
                .setTextColor(INK)
                .setCursorColor(INK)
                .setTextShadow(false)
                .setShowBackground(false)
                .setShowDecorations(false)
                .setPlaceholder(hint("gui.servus_notes.notebook.notes_hint"))
                .build(font, x1(book) - x0(book) - 2, NOTES_MIN, Component.translatable("gui.servus_notes.notebook.section.notes"));
        notesBox.setValue(notes);
        notesBox.visible = false;
        amountBox = paperBox(font, Component.translatable("gui.servus_notes.notebook.goal.amount"));
        amountBox.setMaxLength(Integer.toString(NoteEdits.MAX_AMOUNT).length());
        amountBox.setFilter(value -> value.chars().allMatch(Character::isDigit));
        amountBox.setValue(amount);
        placeNameBox = paperBox(font, Component.translatable("gui.servus_notes.notebook.place.name"));
        placeNameBox.setMaxLength(NoteEdits.MAX_PLACE_NAME);
        placeNameBox.setHint(hint("gui.servus_notes.notebook.place.name_hint"));
        placeNameBox.setValue(placeName);
        applyLimits();
        add.accept(notesBox);
        add.accept(amountBox);
        add.accept(placeNameBox);
        picker.init(font, add);
    }

    /** A field's placeholder: faded ink without shadow, readable on paper. */
    static Component hint(String key) {
        return Component.translatable(key).withColor(FADED_INK & 0xFFFFFF).withoutShadow();
    }

    /** Sets a field's width and scrolls its text again, which {@link EditBox#setWidth} alone does not. */
    static void fitWidth(EditBox box, int width) {
        if (box.getWidth() == width) {
            return;
        }
        box.setWidth(width);
        int cursor = box.getCursorPosition();
        box.setCursorPosition(0);
        box.setCursorPosition(cursor);
    }

    static EditBox paperBox(Font font, Component narration) {
        EditBox box = new EditBox(font, 0, 0, 10, 9, narration);
        box.setBordered(false);
        box.setTextColor(INK);
        box.setTextShadow(false);
        box.setInvertHighlightedTextColor(false);
        box.setVisible(false);
        return box;
    }

    /** Applies the server's length limits to the fields. */
    void applyLimits() {
        notesBox.setCharacterLimit(ClientNotes.limits().maxNotesLength());
    }

    private static int x0(BookLayout book) {
        return book.rightPageX() + PAGE_MARGIN + 4;
    }

    private static int x1(BookLayout book) {
        return book.rightPageX() + book.rightPageWidth() - PAGE_MARGIN;
    }

    // ---------- what the other pages ask ----------

    /** Whether the item picker is open. */
    boolean picking() {
        return picker.isOpen();
    }

    /** The task shown on this page, null while none is. */
    @Nullable UUID selectedTask() {
        return taskId;
    }

    /** The project changed or the notebook closes: typed values are kept, the page empties. */
    void reset() {
        commitAll();
        closePopups();
        taskId = null;
        forgetTask();
    }

    /** Shows {@code task}, turning the page. */
    void open(Task task) {
        if (task.id().equals(taskId)) {
            return;
        }
        commitAll();
        closePopups();
        taskId = task.id();
        forgetTask();
        unfocus();
        motion.flipRight();
    }

    void openShare(UUID listId) {
        commitAll();
        closePopups();
        share.open(listId);
    }

    private void forgetTask() {
        syncedNotes = null;
        syncedPlaceName = null;
        amountEdit = null;
        targetWhenArrives = null;
        deleteArmedAt = Long.MIN_VALUE;
        goalScroll = 0;
    }

    private void closePopups() {
        share.close();
        picker.close();
        assignOpen = false;
    }

    private void unfocus() {
        if (screen.getFocused() == notesBox || screen.getFocused() == amountBox || screen.getFocused() == placeNameBox) {
            screen.setFocused(null);
        }
    }

    // ---------- drawing ----------

    void extract(GuiGraphicsExtractor graphics, BookLayout book, @Nullable ListView view, double mouseX, double mouseY) {
        notesField = NOWHERE;
        amountField = NOWHERE;
        placeNameField = NOWHERE;
        deleteButton = NOWHERE;
        assignButton = NOWHERE;
        goalsArea = NOWHERE;
        if (view == null) {
            syncWidgets(null);
            return;
        }
        Font font = screen.font();
        NoteList list = view.list();
        Rights rights = view.rights();
        Task task = taskId == null ? null : list.task(taskId).orElse(null);
        if (taskId != null && task == null) {
            taskId = null;
            forgetTask();
        }
        if (share.isOpen() && !rights.manage()) {
            share.close();
        }
        if (picker.isOpen() && !rights.edit()) {
            picker.close();
        }
        if (share.isOpen()) {
            share.extract(graphics, font, book, list, mouseX, mouseY);
        } else if (picker.isOpen()) {
            picker.extract(graphics, font, book, mouseX, mouseY);
        } else if (task == null) {
            String hint = rights.edit() ? "gui.servus_notes.notebook.pick_task_edit" : "gui.servus_notes.notebook.pick_task";
            List<FormattedCharSequence> lines = font.split(Component.translatable(hint), x1(book) - x0(book));
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(font, lines.get(i), x0(book), book.pageY() + 12 + i * 10, FADED_INK, false);
            }
        } else {
            extractTask(graphics, font, book, list, rights, task, mouseX, mouseY);
        }
        syncWidgets(task);
    }

    /**
     * Top down: heading, Who, Items, Place, then Notes filling what is left down to the page bottom. Items take as many
     * goal rows as fit while Notes keep their smallest height; the rest of the goals scroll.
     */
    private void extractTask(GuiGraphicsExtractor graphics, Font font, BookLayout book, NoteList list, Rights rights, Task task,
                             double mouseX, double mouseY) {
        int x0 = x0(book);
        int x1 = x1(book);
        int headingRight = x1;
        if (rights.edit()) {
            extractBin(graphics, list, task, x1 - BIN, book.pageY() + 8, mouseX, mouseY);
            headingRight -= BIN + 4;
        }
        int y = extractHeading(graphics, font, task, x0, headingRight, book.pageY() + 8);
        y = extractWho(graphics, font, rights, task, x0, x1, y + SECTION_GAP, mouseX, mouseY);
        int bottom = book.pageBottom() - PAGE_MARGIN;
        int placeHeight = placeHeight(font, task, rights, x0, x1);
        int itemsTop = y + SECTION_GAP;
        int room = bottom - itemsTop - SECTION_GAP - placeHeight - SECTION_GAP - LABEL_HEIGHT - NOTES_MIN;
        int goalRows = Math.max(task.goals().isEmpty() ? 0 : 1, Math.min(task.goals().size(), (room - LABEL_HEIGHT) / GOAL_ROW));
        int itemsHeight = LABEL_HEIGHT + (task.goals().isEmpty() ? 12 : goalRows * GOAL_ROW);
        extractItems(graphics, font, rights, task, x0, x1, itemsTop, goalRows, mouseX, mouseY);
        int placeTop = itemsTop + itemsHeight + SECTION_GAP;
        extractPlace(graphics, font, rights, task, x0, x1, placeTop, mouseX, mouseY);
        extractNotes(graphics, font, rights, task, x0, x1, placeTop + placeHeight + SECTION_GAP, bottom);
        if (assignOpen) {
            extractAssign(graphics, font, book, task, mouseX, mouseY);
        }
    }

    /** The task's name and done record, as plain text; returns the y below them. */
    private int extractHeading(GuiGraphicsExtractor graphics, Font font, Task task, int x0, int x1, int y) {
        Component name = Component.literal(task.text()).withStyle(ChatFormatting.BOLD);
        List<FormattedCharSequence> lines = font.split(name, x1 - x0);
        int shown = Math.max(1, Math.min(2, lines.size()));
        for (int i = 0; i < Math.min(shown, lines.size()); i++) {
            graphics.text(font, lines.get(i), x0, y + i * 10, task.isDone() ? TaskListPage.DONE_INK : INK, false);
        }
        y += shown * 10;
        Optional<DoneRecord> done = task.done();
        if (done.isPresent()) {
            Component record = Component.translatable("gui.servus_notes.notebook.done_by", done.get().by().name(),
                    done.get().day(), ago(done.get().epochMillis())).withStyle(ChatFormatting.ITALIC);
            graphics.text(font, NotebookScreen.ellipsized(font, record, x1 - x0), x0, y, DONE_BY, false);
            y += 10;
        }
        return y;
    }

    private static Component ago(long epochMillis) {
        Ago ago = Ago.between(System.currentTimeMillis(), epochMillis);
        return Component.translatable("gui.servus_notes.notebook.ago." + ago.unit().name().toLowerCase(Locale.ROOT), ago.amount());
    }

    /** A section's label with a ruled line under it; returns the y below it. */
    private static int label(GuiGraphicsExtractor graphics, Font font, String name, int x0, int x1, int y) {
        graphics.text(font, Component.translatable("gui.servus_notes.notebook.section." + name), x0, y + 1, HEADING_INK, false);
        graphics.fill(x0, y + 11, x1, y + 12, RULE);
        return y + LABEL_HEIGHT;
    }

    private void extractNotes(GuiGraphicsExtractor graphics, Font font, Rights rights, Task task, int x0, int x1, int top, int bottom) {
        int y = label(graphics, font, "notes", x0, x1, top);
        int height = Math.max(NOTES_MIN, bottom - y);
        if (rights.edit()) {
            String count = notesBox.getValue().length() + "/" + ClientNotes.limits().maxNotesLength();
            graphics.text(font, count, x1 - font.width(count), top + 1, FADED_INK, false);
            if (screen.getFocused() != notesBox && !task.notes().equals(syncedNotes)) {
                notesBox.setValue(task.notes());
                syncedNotes = task.notes();
            }
            notesField = new Rect(x0, y, x1 - x0, height);
            PaperButton.drawField(graphics, x0, y, x1 - x0, height, screen.getFocused() == notesBox);
            return;
        }
        if (task.notes().isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_notes.notebook.no_notes"), x0, y + 1, FADED_INK, false);
            return;
        }
        List<FormattedCharSequence> lines = font.split(Component.literal(task.notes()).withStyle(ChatFormatting.ITALIC), x1 - x0);
        int shown = Math.min(lines.size(), height / 10);
        for (int i = 0; i < shown; i++) {
            graphics.text(font, lines.get(i), x0, y + 1 + i * 10, INK, false);
        }
    }

    private int extractWho(GuiGraphicsExtractor graphics, Font font, Rights rights, Task task, int x0, int x1, int y,
                           double mouseX, double mouseY) {
        y = label(graphics, font, "who", x0, x1, y);
        int right = x1;
        if (rights.edit()) {
            Component assign = Component.translatable("gui.servus_notes.notebook.assign");
            int width = PaperButton.width(font, assign);
            right -= width;
            assignButton = new Rect(right, y, width, PaperButton.HEIGHT);
            screen.button(graphics, assign, right, y, mouseX, mouseY, INK, () -> {
                assignOpen = !assignOpen;
                assignScroll = 0;
            });
            right -= 3;
        }
        Optional<Person> assignee = task.assignee();
        UUID self = ClientNotes.self();
        boolean mine = assignee.map(person -> person.id().equals(self)).orElse(false);
        boolean canClaim = !task.isDone() && assignee.isEmpty() && rights.tick();
        if (!mine && (canClaim || rights.edit())) {
            Component claim = Component.translatable("gui.servus_notes.notebook.claim");
            right -= PaperButton.width(font, claim);
            screen.button(graphics, claim, right, y, mouseX, mouseY, INK, () -> claim(task, canClaim));
            right -= 3;
        }
        Component who = assignee.<Component>map(person -> Component.translatable("gui.servus_notes.notebook.assignee", person.name()))
                .orElseGet(() -> Component.translatable("gui.servus_notes.notebook.nobody"));
        graphics.text(font, NotebookScreen.ellipsized(font, who, right - x0 - 2), x0, y + 2,
                assignee.isPresent() ? TaskListPage.ASSIGNEE : FADED_INK, false);
        return y + PaperButton.HEIGHT;
    }

    /** The players to assign, below the Assign button: "nobody" first, then every known player by name. */
    private void extractAssign(GuiGraphicsExtractor graphics, Font font, BookLayout book, Task task, double mouseX, double mouseY) {
        List<Optional<Person>> choices = new ArrayList<>();
        choices.add(Optional.empty());
        ClientNotes.knownPlayers().stream()
                .sorted(Comparator.comparing(person -> person.name().toLowerCase(Locale.ROOT)))
                .forEach(person -> choices.add(Optional.of(person)));
        int width = Math.max(assignButton.width(), font.width(Component.translatable("gui.servus_notes.notebook.assign_nobody")) + 10);
        for (Optional<Person> choice : choices) {
            width = Math.max(width, choice.map(person -> font.width(person.name()) + 10).orElse(0));
        }
        int x1 = assignButton.x() + assignButton.width();
        int x0 = Math.max(x0(book) - 2, x1 - width);
        int top = assignButton.y() + PaperButton.HEIGHT + 1;
        int rows = Math.max(1, Math.min(Math.min(ASSIGN_ROWS, choices.size()), (book.pageBottom() - 4 - top) / ASSIGN_ROW));
        assignScroll = Mth.clamp(assignScroll, 0, choices.size() - rows);
        screen.hits().add(book.rightPageX(), book.pageY(), book.rightPageWidth(), book.pageHeight(), () -> assignOpen = false);
        graphics.fill(x0, top, x1, top + rows * ASSIGN_ROW + 2, POPUP);
        graphics.outline(x0, top, x1 - x0, rows * ASSIGN_ROW + 2, POPUP_EDGE);
        for (int row = 0; row < rows; row++) {
            Optional<Person> choice = choices.get(assignScroll + row);
            int y = top + 1 + row * ASSIGN_ROW;
            if (PageHits.over(mouseX, mouseY, x0, y, x1 - x0, ASSIGN_ROW)) {
                graphics.fill(x0 + 1, y, x1 - 1, y + ASSIGN_ROW, ProjectsPage.HOVERED);
            }
            Component name = choice.<Component>map(person -> Component.literal(person.name()))
                    .orElseGet(() -> Component.translatable("gui.servus_notes.notebook.assign_nobody"));
            boolean current = choice.map(Person::id).equals(task.assignee().map(Person::id));
            graphics.text(font, NotebookScreen.ellipsized(font, name, x1 - x0 - 6), x0 + 3, y + 2, current ? TaskListPage.ASSIGNEE : INK, false);
            screen.hits().add(x0, y, x1 - x0, ASSIGN_ROW, () -> {
                assignOpen = false;
                send(new NoteEdit.SetAssignee(listId(), task.id(), choice));
            });
        }
    }

    private void extractItems(GuiGraphicsExtractor graphics, Font font, Rights rights, Task task, int x0, int x1, int top, int rows,
                              double mouseX, double mouseY) {
        int y = label(graphics, font, "items", x0, x1, top);
        Component add = Component.translatable("gui.servus_notes.notebook.add_item");
        if (rights.edit()) {
            screen.button(graphics, add, x1 - PaperButton.width(font, add), top - 1, mouseX, mouseY, INK,
                    () -> openPicker(Component.translatable("gui.servus_notes.notebook.picker.for", task.text()), itemId -> addGoal(task.id(), itemId)));
        }
        List<Goal> goals = task.goals();
        openArrivedTarget(task);
        if (goals.isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_notes.notebook.no_items"), x0, y + 2, FADED_INK, false);
            return;
        }
        goalsArea = new Rect(x0, y, x1 - x0, rows * GOAL_ROW);
        goalScroll = Mth.clamp(goalScroll, 0, Math.max(0, goals.size() - rows));
        for (int row = 0; row < rows && goalScroll + row < goals.size(); row++) {
            extractGoal(graphics, font, rights, task, goals.get(goalScroll + row), x0, x1, y + row * GOAL_ROW, mouseX, mouseY);
        }
        if (goals.size() > rows) {
            Component more = Component.literal((goalScroll + 1) + "-" + Math.min(goals.size(), goalScroll + rows) + " / " + goals.size());
            int moreRight = rights.edit() ? x1 - PaperButton.width(font, add) - 6 : x1;
            graphics.text(font, more, moreRight - font.width(more), top + 1, FADED_INK, false);
        }
    }

    /**
     * One goal on one line: icon, name, the button that adds what the viewer carries, bar (its tooltip says what
     * the viewer carries), count and target fields, remove.
     */
    private void extractGoal(GuiGraphicsExtractor graphics, Font font, Rights rights, Task task, Goal goal, int x0, int x1, int top,
                             double mouseX, double mouseY) {
        int y = top + 4;
        graphics.item(GoalItems.icon(goal.itemId()), x0, top + 1);
        int right = x1;
        if (rights.edit()) {
            String remove = "×";
            int removeX = right - font.width(remove) - 2;
            boolean hovered = PageHits.over(mouseX, mouseY, removeX - 2, top + 2, font.width(remove) + 5, 12);
            graphics.text(font, remove, removeX, y, hovered ? INK : REMOVE, false);
            screen.hits().add(removeX - 2, top + 2, font.width(remove) + 5, 12, () -> send(new NoteEdit.RemoveGoal(listId(), task.id(), goal.id())));
            if (hovered) {
                screen.tooltip(Component.translatable("gui.servus_notes.notebook.goal.remove"));
            }
            right = removeX - 4;
        }
        right = amount(graphics, font, task, goal, true, rights.edit(), right, top + 2, mouseX, mouseY);
        String slash = "/";
        right -= font.width(slash) + 2;
        graphics.text(font, slash, right, y, FADED_INK, false);
        right = amount(graphics, font, task, goal, false, rights.tick(), right - 2, top + 2, mouseX, mouseY);
        LocalPlayer player = Minecraft.getInstance().player;
        int carried = player == null ? 0 : Carried.count(player, goal.itemId());
        int barWidth = Mth.clamp((x1 - x0) / 5, 20, 60);
        int barX = right - 4 - barWidth;
        extractBar(graphics, goal, carried, barX, y, barWidth);
        if (PageHits.over(mouseX, mouseY, barX, top, barWidth, GOAL_ROW)) {
            screen.tooltip(Component.translatable("gui.servus_notes.notebook.goal.carry", carried));
        }
        int nameRight = barX - 4;
        int added = goal.countWithCarried(carried) - goal.count();
        Integer submittedAt = submitted.get(goal.id());
        if (submittedAt != null && submittedAt != carried) {
            submitted.remove(goal.id());
            submittedAt = null;
        }
        if (rights.tick() && added > 0 && submittedAt == null) {
            Component submit = Component.translatable("gui.servus_notes.notebook.goal.add_carried", added);
            int submitWidth = PaperButton.width(font, submit);
            int submitX = nameRight - submitWidth;
            screen.button(graphics, submit, submitX, top + 3, mouseX, mouseY, INK, () -> {
                commitAmount();
                submitted.put(goal.id(), carried);
                send(new NoteEdit.SetGoalCount(listId(), task.id(), goal.id(), goal.countWithCarried(carried)));
            });
            if (PageHits.over(mouseX, mouseY, submitX, top + 3, submitWidth, PaperButton.HEIGHT)) {
                screen.tooltip(Component.translatable("gui.servus_notes.notebook.goal.add_carried.tooltip", added));
            }
            nameRight = submitX - 4;
        }
        int nameX = x0 + ICON + 4;
        Component name = GoalItems.name(goal.itemId());
        graphics.text(font, NotebookScreen.ellipsized(font, name, nameRight - nameX), nameX, y, INK, false);
        if (PageHits.over(mouseX, mouseY, x0, top + 1, ICON, ICON)) {
            if (rights.edit()) {
                screen.tooltip(GoalItems.icon(goal.itemId()), Component.translatable("gui.servus_notes.notebook.goal.change"));
            } else {
                screen.tooltip(GoalItems.icon(goal.itemId()));
            }
        }
        if (rights.edit()) {
            screen.hits().add(x0, top + 1, ICON, ICON, () -> openPicker(Component.translatable("gui.servus_notes.notebook.picker.change", name),
                    itemId -> send(new NoteEdit.SetGoalItem(listId(), task.id(), goal.id(), itemId))));
        }
    }

    /**
     * A goal's count or target ending at {@code right}: a field to type in when {@code editable}, else plain text.
     * Returns its left edge.
     */
    private int amount(GuiGraphicsExtractor graphics, Font font, Task task, Goal goal, boolean target, boolean editable, int right, int top,
                       double mouseX, double mouseY) {
        String value = Integer.toString(target ? goal.target() : goal.count());
        int y = top + 2;
        if (!editable) {
            int x = right - font.width(value);
            graphics.text(font, value, x, y, target ? FADED_INK : INK, false);
            return x;
        }
        int width = Math.max(AMOUNT_WIDTH, font.width(value) + 6);
        int x = right - width;
        boolean typing = amountEdit != null && amountEdit.goalId().equals(goal.id()) && amountEdit.target() == target;
        PaperButton.drawField(graphics, x, top, width, PaperButton.FIELD_HEIGHT - 1, typing);
        if (typing) {
            amountField = new Rect(x + 3, y, width - 5, 9);
        } else {
            graphics.text(font, value, x + width - 3 - font.width(value), y, INK, false);
            screen.hits().add(x, top, width, PaperButton.FIELD_HEIGHT - 1, () -> startAmountEdit(task, goal, target));
            if (PageHits.over(mouseX, mouseY, x, top, width, PaperButton.FIELD_HEIGHT - 1)) {
                screen.tooltip(Component.translatable(target ? "gui.servus_notes.notebook.goal.type_target" : "gui.servus_notes.notebook.goal.type_count"));
            }
        }
        return x;
    }

    private static void extractBar(GuiGraphicsExtractor graphics, Goal goal, int carried, int x, int y, int width) {
        int height = 7;
        graphics.fill(x, y, x + width, y + height, BAR_BACK);
        int inner = width - 2;
        GoalFill fill = GoalFill.of(goal, goal.reached() ? 0 : carried);
        int countedEnd = x + 1 + Math.round(inner * fill.counted());
        int carriedEnd = x + 1 + Math.round(inner * (fill.counted() + fill.carried()));
        graphics.fill(x + 1, y + 1, countedEnd, y + height - 1, goal.reached() ? BAR_FULL : BAR_FILL);
        graphics.fill(countedEnd, y + 1, carriedEnd, y + height - 1, BAR_CARRIED);
        graphics.outline(x, y, width, height, INK);
    }

    private List<PlaceButton> placeButtons(Rights rights, Task task) {
        List<PlaceButton> buttons = new ArrayList<>();
        if (rights.edit()) {
            String name = task.spot().map(Spot::name).orElse("");
            buttons.add(new PlaceButton(Component.translatable("gui.servus_notes.notebook.place.here"),
                    () -> here().ifPresent(spot -> send(new NoteEdit.SetSpot(listId(), task.id(), Optional.of(spot.withName(name))))), false, INK));
        }
        task.spot().ifPresent(spot -> {
            boolean on = Beams.isOn(spot);
            buttons.add(new PlaceButton(Component.translatable(on ? "gui.servus_notes.notebook.place.beam_off" : "gui.servus_notes.notebook.place.beam_on"),
                    () -> Beams.toggle(spot), on, INK));
            if (rights.edit()) {
                buttons.add(new PlaceButton(Component.translatable("gui.servus_notes.notebook.place.clear"),
                        () -> send(new NoteEdit.SetSpot(listId(), task.id(), Optional.empty())), false, REMOVE));
            }
        });
        return buttons;
    }

    private int buttonsWidth(Font font, List<PlaceButton> buttons) {
        int width = 0;
        for (PlaceButton button : buttons) {
            width += PaperButton.width(font, button.label()) + 3;
        }
        return width;
    }

    /** What the chip says: editors name the place in their own field, so their chip shows only the coordinates. */
    private static Component placeText(Task task, Rights rights) {
        return task.spot().<Component>map(spot -> Component.literal(rights.edit() ? Places.coordinates(spot) : Places.chip(spot)))
                .orElseGet(() -> Component.translatable("gui.servus_notes.notebook.place.none"));
    }

    /** Whether the place and its buttons share one line. */
    private boolean placeOnOneLine(Font font, Task task, Rights rights, int x0, int x1) {
        return font.width(placeText(task, rights)) + 10 + buttonsWidth(font, placeButtons(rights, task)) <= x1 - x0;
    }

    private boolean hasNameField(Task task, Rights rights) {
        return rights.edit() && task.spot().isPresent();
    }

    private int placeHeight(Font font, Task task, Rights rights, int x0, int x1) {
        boolean twoLines = !placeOnOneLine(font, task, rights, x0, x1) && !placeButtons(rights, task).isEmpty();
        return LABEL_HEIGHT + PaperButton.HEIGHT + (twoLines ? PaperButton.HEIGHT + 2 : 0)
                + (hasNameField(task, rights) ? PaperButton.FIELD_HEIGHT + 2 : 0);
    }

    private void extractPlace(GuiGraphicsExtractor graphics, Font font, Rights rights, Task task, int x0, int x1, int top,
                              double mouseX, double mouseY) {
        int y = label(graphics, font, "place", x0, x1, top);
        if (hasNameField(task, rights)) {
            String name = task.spot().map(Spot::name).orElse("");
            if (screen.getFocused() != placeNameBox && !name.equals(syncedPlaceName)) {
                placeNameBox.setValue(name);
                syncedPlaceName = name;
            }
            placeNameField = new Rect(x0, y, x1 - x0, PaperButton.FIELD_HEIGHT);
            PaperButton.drawField(graphics, x0, y, x1 - x0, PaperButton.FIELD_HEIGHT, screen.getFocused() == placeNameBox);
            y += PaperButton.FIELD_HEIGHT + 2;
        }
        List<PlaceButton> buttons = placeButtons(rights, task);
        boolean oneLine = placeOnOneLine(font, task, rights, x0, x1);
        int textRight = oneLine ? x1 - buttonsWidth(font, buttons) - 4 : x1;
        Component place = placeText(task, rights);
        if (task.spot().isPresent()) {
            boolean on = Beams.isOn(task.spot().get());
            FormattedCharSequence text = NotebookScreen.ellipsized(font, place, textRight - x0 - 6);
            int width = font.width(text) + 6;
            graphics.fill(x0, y, x0 + width, y + PaperButton.HEIGHT - 1, on ? CHIP_ON : CHIP);
            graphics.outline(x0, y, width, PaperButton.HEIGHT - 1, on ? CHIP_ON_EDGE : FADED_INK);
            graphics.text(font, text, x0 + 3, y + 2, INK, false);
        } else {
            graphics.text(font, NotebookScreen.ellipsized(font, place, textRight - x0), x0, y + 2, FADED_INK, false);
        }
        int buttonY = oneLine ? y : y + PaperButton.HEIGHT + 2;
        int x = x1 - buttonsWidth(font, buttons) + 3;
        for (PlaceButton button : buttons) {
            int width = PaperButton.width(font, button.label());
            if (button.lit()) {
                PaperButton.drawLit(graphics, font, button.label(), x, buttonY, width, PageHits.over(mouseX, mouseY, x, buttonY, width, PaperButton.HEIGHT), true);
                screen.hits().add(x, buttonY, width, PaperButton.HEIGHT, button.run());
            } else {
                screen.button(graphics, button.label(), x, buttonY, mouseX, mouseY, button.ink(), button.run());
            }
            x += width + 3;
        }
    }

    /** The bin at the top right: the first click arms it red, the second deletes the task. */
    private void extractBin(GuiGraphicsExtractor graphics, NoteList list, Task task, int x, int y, double mouseX, double mouseY) {
        boolean armed = deleteArmed();
        deleteButton = new Rect(x - 2, y - 2, BIN + 4, BIN + 4);
        if (deleteButton.contains(mouseX, mouseY)) {
            graphics.fill(x - 1, y - 1, x + BIN + 1, y + BIN + 1, ProjectsPage.HOVERED);
            screen.tooltip(Component.translatable(armed ? "gui.servus_notes.notebook.delete_task.confirm" : "gui.servus_notes.notebook.delete_task"));
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, armed ? BIN_ARMED_SPRITE : BIN_SPRITE, x, y, BIN, BIN);
        screen.hits().add(deleteButton.x(), deleteButton.y(), deleteButton.width(), deleteButton.height(), () -> deletePressed(list, task));
    }

    /** Places the fields where this frame drew them and shows only the ones in use. */
    private void syncWidgets(@Nullable Task task) {
        boolean overlay = share.isOpen() || picker.isOpen();
        boolean showNotes = task != null && !overlay && notesField != NOWHERE;
        notesBox.visible = showNotes;
        if (showNotes) {
            notesBox.setX(notesField.x() + 1);
            notesBox.setY(notesField.y() + 1);
            notesBox.setHeight(notesField.height() - 2);
        } else if (screen.getFocused() == notesBox) {
            screen.setFocused(null);
        }
        boolean showAmount = task != null && !overlay && !assignOpen && amountField != NOWHERE;
        show(amountBox, showAmount);
        if (showAmount) {
            amountBox.setX(amountField.x());
            amountBox.setY(amountField.y());
            fitWidth(amountBox, amountField.width());
        }
        boolean showPlaceName = task != null && !overlay && !assignOpen && placeNameField != NOWHERE;
        show(placeNameBox, showPlaceName);
        if (showPlaceName) {
            placeNameBox.setX(placeNameField.x() + 3);
            placeNameBox.setY(placeNameField.y() + 3);
            fitWidth(placeNameBox, placeNameField.width() - 6);
        }
    }

    private void show(EditBox box, boolean visible) {
        box.setVisible(visible);
        if (!visible && screen.getFocused() == box) {
            screen.setFocused(null);
        }
    }

    // ---------- actions ----------

    private static @Nullable UUID listId() {
        return ClientNotes.selected();
    }

    private static void send(NoteEdit edit) {
        ClientNotes.send(edit);
    }

    private static Optional<Task> task(@Nullable UUID id) {
        return id == null ? Optional.empty() : ClientNotes.get(listId()).flatMap(view -> view.list().task(id));
    }

    /** "I'll do it": claims an open task nobody does, else an editor sets themselves as the assignee. */
    private static void claim(Task task, boolean canClaim) {
        if (canClaim) {
            send(new NoteEdit.Claim(listId(), task.id()));
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            send(new NoteEdit.SetAssignee(listId(), task.id(), Optional.of(new Person(player.getUUID(), player.getName().getString()))));
        }
    }

    /** Where the player stands, as a spot without a name. */
    private static Optional<Spot> here() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }
        BlockPos pos = player.blockPosition();
        return Optional.of(new Spot(pos.getX(), pos.getY(), pos.getZ(), player.level().dimension().identifier().toString()));
    }

    private void openPicker(Component title, Consumer<String> pick) {
        commitAll();
        assignOpen = false;
        unfocus();
        picker.open(title, pick);
    }

    private void addGoal(UUID task, String itemId) {
        UUID goalId = UUID.randomUUID();
        send(new NoteEdit.AddGoal(listId(), task, goalId, itemId));
        targetWhenArrives = goalId;
    }

    /** Opens the target of a just picked goal for typing once it is in the task, scrolled into view. */
    private void openArrivedTarget(Task task) {
        if (targetWhenArrives == null) {
            return;
        }
        task.goal(targetWhenArrives).ifPresent(goal -> {
            targetWhenArrives = null;
            goalScroll = task.goals().size();
            startAmountEdit(task, goal, true);
        });
    }

    private void startAmountEdit(Task task, Goal goal, boolean target) {
        commitAmount();
        amountEdit = new AmountEdit(task.id(), goal.id(), target);
        amountBox.setValue(Integer.toString(target ? goal.target() : goal.count()));
        amountBox.setVisible(true);
        amountBox.moveCursorToEnd(false);
        amountBox.setHighlightPos(0);
        screen.setFocused(amountBox);
    }

    private void commitAmount() {
        if (amountEdit == null) {
            return;
        }
        AmountEdit edit = amountEdit;
        amountEdit = null;
        String text = amountBox.getValue().strip();
        if (text.isEmpty()) {
            return;
        }
        int typed = Integer.parseInt(text);
        task(edit.taskId()).flatMap(task -> task.goal(edit.goalId())).ifPresent(goal -> {
            if (edit.target() && typed != goal.target()) {
                send(new NoteEdit.SetGoalTarget(listId(), edit.taskId(), edit.goalId(), typed));
            } else if (!edit.target() && typed != goal.count()) {
                send(new NoteEdit.SetGoalCount(listId(), edit.taskId(), edit.goalId(), typed));
            }
        });
    }

    private void commitNotes() {
        if (syncedNotes == null) {
            return;
        }
        task(taskId).ifPresent(task -> {
            String notes = notesBox.getValue().strip();
            if (!notes.equals(task.notes())) {
                send(new NoteEdit.EditNotes(listId(), task.id(), notes));
            }
        });
    }

    private void commitPlaceName() {
        if (syncedPlaceName == null) {
            return;
        }
        task(taskId).ifPresent(task -> task.spot().ifPresent(spot -> {
            String name = placeNameBox.getValue().strip();
            if (!name.equals(spot.name())) {
                send(new NoteEdit.SetSpot(listId(), task.id(), Optional.of(spot.withName(name))));
            }
        }));
    }

    private void commitAll() {
        if (notesBox == null) {
            return;
        }
        commitAmount();
        commitNotes();
        commitPlaceName();
    }

    private boolean deleteArmed() {
        return deleteArmedAt != Long.MIN_VALUE && Util.getMillis() - deleteArmedAt < CONFIRM_MILLIS;
    }

    private void deletePressed(NoteList list, Task task) {
        if (deleteArmed()) {
            deleteArmedAt = Long.MIN_VALUE;
            send(new NoteEdit.DeleteTask(list.id(), task.id()));
        } else {
            deleteArmedAt = Util.getMillis();
        }
    }

    // ---------- input ----------

    /** Before a click is handled: typed values are sent when the click lands outside their field. */
    void beforeClick(double mouseX, double mouseY) {
        if (notesBox.visible && !notesField.contains(mouseX, mouseY)) {
            commitNotes();
        }
        if (placeNameBox.isVisible() && !placeNameField.contains(mouseX, mouseY)) {
            commitPlaceName();
        }
        if (amountEdit != null && !amountField.contains(mouseX, mouseY)) {
            commitAmount();
        }
        if (deleteArmed() && !deleteButton.contains(mouseX, mouseY)) {
            deleteArmedAt = Long.MIN_VALUE;
        }
    }

    /** Enter sends a one line field, Escape backs out of the innermost popup or field; false when the key is not this page's. */
    boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return escape();
        }
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER) {
            return false;
        }
        if (screen.getFocused() == amountBox && amountEdit != null) {
            commitAmount();
            screen.setFocused(null);
            return true;
        }
        if (screen.getFocused() == placeNameBox && placeNameBox.isVisible()) {
            commitPlaceName();
            screen.setFocused(null);
            return true;
        }
        return false;
    }

    private boolean escape() {
        if (share.escape() || picker.escape()) {
            return true;
        }
        if (assignOpen) {
            assignOpen = false;
        } else if (amountEdit != null) {
            amountEdit = null;
            screen.setFocused(null);
        } else if (screen.getFocused() == placeNameBox) {
            task(taskId).flatMap(Task::spot).ifPresent(spot -> placeNameBox.setValue(spot.name()));
            screen.setFocused(null);
        } else if (screen.getFocused() == notesBox) {
            commitNotes();
            screen.setFocused(null);
        } else if (deleteArmed()) {
            deleteArmedAt = Long.MIN_VALUE;
        } else {
            return false;
        }
        return true;
    }

    /** Scrolls a popup, the assign list or the goals under the mouse; false when the wheel is not this page's. */
    boolean scrolled(double mouseX, double mouseY, double amount) {
        BookLayout book = screen.layout();
        if (!PageHits.over(mouseX, mouseY, book.rightPageX(), book.pageY(), book.rightPageWidth(), book.pageHeight())) {
            return false;
        }
        int step = (int) Math.signum(amount);
        if (share.isOpen()) {
            return share.scrolled(amount);
        }
        if (picker.isOpen()) {
            return picker.scrolled(amount);
        }
        if (assignOpen) {
            assignScroll -= step;
            return true;
        }
        if (goalsArea.contains(mouseX, mouseY)) {
            commitAmount();
            goalScroll -= step;
            return true;
        }
        return false;
    }
}
