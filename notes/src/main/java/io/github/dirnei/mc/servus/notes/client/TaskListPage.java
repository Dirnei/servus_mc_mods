package io.github.dirnei.mc.servus.notes.client;

import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.FADED_INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.PAGE_MARGIN;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.RULE;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.RULE_PITCH;

import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.NoteEdits;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.Places;
import io.github.dirnei.mc.servus.notes.model.Rights;
import io.github.dirnei.mc.servus.notes.model.Scope;
import io.github.dirnei.mc.servus.notes.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The left page of an open project: a link back to the projects, the project's name, a line with scope,
 * done count and guests, the project's buttons, and its tasks on ruled lines, one line each, with tick
 * box, text and small marks for items, place and assignee. A click on the box ticks, a click on the line
 * opens the task on the right page, a click on the open task's text edits it in place. "+ New task" adds
 * a line to write in. Editors drag a line to move the task; how far right it is dropped makes it a subtask.
 */
final class TaskListPage {

    private static final int SUB_INDENT = 10;
    private static final int BOX = 7;
    private static final int TEXT_GAP = 11;
    private static final int BUTTON_GAP = 3;
    private static final long CONFIRM_MILLIS = 3000;
    private static final int BANNER_LINES = 3;
    /** How far the mouse moves with the button down before a press becomes a drag. */
    private static final int DRAG_START = 4;
    private static final long SCROLL_MILLIS = 120;
    private static final String GOALS_MARK = "▣";
    private static final String PLACE_MARK = "◉";

    static final int DONE_INK = 0xFF9A8460;
    static final int STRIKE = 0xFFA8322A;
    static final int ASSIGNEE = 0xFF2F5AA0;
    private static final int TASK_HOVER = 0x0C000000;
    private static final int SCROLL_TRACK = 0x30000000;
    private static final int BANNER = 0xFFF7E7A8;
    private static final int BANNER_EDGE = 0xFFB5842F;
    private static final int BANNER_INK = 0xFF6A4A10;
    private static final int DRAGGED = 0x60F1E4C3;
    private static final int DROP_LINE = 0xFFA8322A;
    private static final int GHOST = 0xE0E5D0A0;

    /** A button of the project's button row. */
    private record Action(Component label, int ink, Runnable run, @Nullable Component tooltip) {
    }

    /** Where this frame drew the rows. */
    private record Rows(int x0, int x1, int top, int count) {

        int bottom() {
            return top + count * RULE_PITCH;
        }
    }

    private final NotebookScreen screen;
    private final NotebookMotion motion;
    private EditBox titleBox;
    private EditBox rowBox;
    private boolean renaming;
    private @Nullable UUID renameWhenArrives;
    private long renameAskedAt = Long.MIN_VALUE;
    private long deleteArmedAt = Long.MIN_VALUE;
    /** Where the project's Delete button lies this frame, as {x, y, width}; a click elsewhere disarms it. */
    private int[] deleteButton = {0, 0, 0};
    private int scroll;
    /** Keeps the rows scrolled to the end after adding a task, until the player scrolls or leaves the project. */
    private boolean stickToEnd;
    /** The task whose text is edited in its line, or null. */
    private @Nullable UUID editingTask;
    /** Whether a line for a new task is open at the end of the list. */
    private boolean drafting;
    /** The task this client just added; the right page opens it once it arrives. */
    private @Nullable UUID openWhenArrives;
    /** Where this frame's row field lies, as {x, y, width}, or null while no line is edited. */
    private int @Nullable [] rowField;
    private @Nullable Rows rows;
    private double pressX;
    private double pressY;
    private @Nullable UUID pressed;
    private boolean pressedOnText;
    private boolean pressedMayDrag;
    private boolean dragging;
    private double dragX;
    private double dragY;
    private long lastAutoScroll;

    TaskListPage(NotebookScreen screen, NotebookMotion motion) {
        this.screen = screen;
        this.motion = motion;
    }

    /** Creates the name and line fields, keeping what they held across a resize. */
    void init(Font font, Consumer<AbstractWidget> add) {
        String titleText = titleBox == null ? "" : titleBox.getValue();
        String rowText = rowBox == null ? "" : rowBox.getValue();
        titleBox = TaskPage.paperBox(font, Component.translatable("gui.servus_notes.notebook.project_name"));
        titleBox.setValue(titleText);
        rowBox = TaskPage.paperBox(font, Component.translatable("gui.servus_notes.notebook.task_text"));
        rowBox.setValue(rowText);
        applyLimits();
        add.accept(titleBox);
        add.accept(rowBox);
    }

    /** Applies the server's length limits to the fields. */
    void applyLimits() {
        titleBox.setMaxLength(ClientNotes.limits().maxNameLength());
        rowBox.setMaxLength(ClientNotes.limits().maxTaskLength());
    }

    /** The open project changed: edits in progress end, the rows start at the top. */
    void reset() {
        commitRename();
        commitRow();
        editingTask = null;
        drafting = false;
        openWhenArrives = null;
        cancelDrag();
        deleteArmedAt = Long.MIN_VALUE;
        scroll = 0;
        stickToEnd = false;
    }

    /** Opens the name for renaming once the project {@code listId} arrives from the server. */
    void renameWhenArrives(UUID listId) {
        renameWhenArrives = listId;
        renameAskedAt = Util.getMillis();
    }

    /** Whether the page still waits for the new project {@code listId}; it gives up after a while or once the server refused. */
    boolean awaiting(UUID listId) {
        boolean refusedSince = ClientNotes.refusal().map(refusal -> refusal.at() >= renameAskedAt).orElse(false);
        return listId.equals(renameWhenArrives) && !refusedSince && Util.getMillis() < renameAskedAt + CONFIRM_MILLIS;
    }

    // ---------- drawing ----------

    void extract(GuiGraphicsExtractor graphics, BookLayout book, ListView view, double mouseX, double mouseY) {
        Font font = screen.font();
        NoteList list = view.list();
        Rights rights = view.rights();
        if (list.id().equals(renameWhenArrives)) {
            renameWhenArrives = null;
            startRename(list);
        }
        if (!rights.edit()) {
            editingTask = null;
            drafting = false;
            dragging = false;
        }
        if (editingTask != null && list.task(editingTask).isEmpty()) {
            editingTask = null;
        }
        if (openWhenArrives != null) {
            list.task(openWhenArrives).ifPresent(task -> {
                openWhenArrives = null;
                screen.taskPage().open(task);
            });
        }
        int x0 = book.leftPageX() + PAGE_MARGIN;
        int x1 = book.rightPageX() - PAGE_MARGIN - 4;
        int y = book.pageY() + 6;
        extractBackLink(graphics, font, x0, y, mouseX, mouseY);
        y += 13;
        extractName(graphics, font, list, rights, x0, x1, y);
        y += 13;
        int sealWidth = ScopeSeal.draw(graphics, font, list.scope(), x0, y - 1);
        int done = list.tasks().size() - list.openCount();
        Component meta = list.guests().isEmpty()
                ? Component.translatable("gui.servus_notes.notebook.project_meta", list.ownerName(), done, list.tasks().size())
                : Component.translatable("gui.servus_notes.notebook.project_meta_guests", list.ownerName(), done, list.tasks().size(),
                        list.guests().size());
        int metaX = x0 + sealWidth + 4;
        graphics.text(font, NotebookScreen.ellipsized(font, meta.getString(), x1 - metaX), metaX, y, FADED_INK, false);
        y += 14;
        y = extractActions(graphics, font, list, rights, x0, x1, y, mouseX, mouseY) + 4;
        if (!rights.edit()) {
            y = extractBanner(graphics, font, list, x0, x1, y) + 2;
        }
        int bottom = book.pageBottom() - PAGE_MARGIN;
        if (rights.edit()) {
            int buttonY = bottom - PaperButton.HEIGHT;
            Component label = Component.translatable("gui.servus_notes.notebook.new_task");
            int width = PaperButton.width(font, label) + 8;
            PaperButton.draw(graphics, font, label, x0, buttonY, width, PageHits.over(mouseX, mouseY, x0, buttonY, width, PaperButton.HEIGHT), INK);
            screen.hits().add(x0, buttonY, width, PaperButton.HEIGHT, this::startDraft);
            bottom = buttonY - 4;
        }
        rowField = null;
        extractRows(graphics, font, list, rights, x0, x1, y, bottom, mouseX, mouseY);
        syncWidgets(x0, x1, book.pageY() + 19);
    }

    private void extractBackLink(GuiGraphicsExtractor graphics, Font font, int x, int y, double mouseX, double mouseY) {
        Component back = Component.translatable("gui.servus_notes.notebook.back");
        boolean hovered = PageHits.over(mouseX, mouseY, x - 2, y - 2, font.width(back) + 4, 12);
        graphics.text(font, back, x, y, hovered ? INK : FADED_INK, false);
        screen.hits().add(x - 2, y - 2, font.width(back) + 4, 12, () -> screen.openProject(null));
    }

    private void extractName(GuiGraphicsExtractor graphics, Font font, NoteList list, Rights rights, int x0, int x1, int y) {
        if (renaming) {
            graphics.fill(x0, y + 9, x1, y + 10, FADED_INK);
            return;
        }
        graphics.text(font, NotebookScreen.ellipsized(font, Component.literal(list.name()).withStyle(style -> style.withBold(true)), x1 - x0),
                x0, y, INK, false);
        if (rights.edit()) {
            screen.hits().add(x0, y - 2, x1 - x0, 12, () -> startRename(list));
        }
    }

    /** The project's buttons, flowing onto a second line when the page is narrow; returns the y below them. */
    private int extractActions(GuiGraphicsExtractor graphics, Font font, NoteList list, Rights rights, int x0, int x1, int y,
                               double mouseX, double mouseY) {
        List<Action> actions = new ArrayList<>();
        if (rights.edit()) {
            actions.add(new Action(Component.translatable("gui.servus_notes.notebook.rename"), INK, () -> startRename(list), null));
        }
        if (rights.manage()) {
            actions.add(new Action(Component.translatable("gui.servus_notes.notebook.share"), INK, () -> {
                commitRename();
                screen.taskPage().openShare(list.id());
            }, null));
        }
        boolean pinned = ClientNotes.isPinned(list.id());
        UUID id = list.id();
        actions.add(new Action(Component.translatable(pinned ? "gui.servus_notes.notebook.unpin" : "gui.servus_notes.notebook.pin"), INK,
                () -> ClientNotes.pin(pinned ? Optional.empty() : Optional.of(id)),
                Component.translatable(pinned ? "gui.servus_notes.notebook.unpin.tooltip" : "gui.servus_notes.notebook.pin.tooltip")));
        if (rights.manage()) {
            boolean armed = deleteArmed();
            actions.add(new Action(Component.translatable(armed ? "gui.servus_notes.notebook.delete_confirm" : "gui.servus_notes.notebook.delete"),
                    STRIKE, () -> deletePressed(list), null));
        }
        int x = x0;
        deleteButton = new int[] {0, 0, 0};
        for (Action action : actions) {
            int width = PaperButton.width(font, action.label());
            if (x > x0 && x + width > x1) {
                x = x0;
                y += PaperButton.HEIGHT + 2;
            }
            screen.button(graphics, action.label(), x, y, mouseX, mouseY, action.ink(), action.run());
            if (action.tooltip() != null && PageHits.over(mouseX, mouseY, x, y, width, PaperButton.HEIGHT)) {
                screen.tooltip(action.tooltip());
            }
            if (action.ink() == STRIKE) {
                deleteButton = new int[] {x, y, width};
            }
            x += width + BUTTON_GAP;
        }
        return y + PaperButton.HEIGHT;
    }

    /** For a project the player may not edit: a banner saying why; returns the y below it. */
    private static int extractBanner(GuiGraphicsExtractor graphics, Font font, NoteList list, int x0, int x1, int y) {
        List<FormattedCharSequence> banner = font.split(Component.translatable(readOnlyReason(list)), x1 - x0 - 6);
        int lines = Math.min(BANNER_LINES, banner.size());
        int bottom = y + lines * 10 + 3;
        graphics.fill(x0 - 2, y, x1 + 2, bottom, BANNER);
        graphics.fill(x0 - 2, y, x0, bottom, BANNER_EDGE);
        for (int i = 0; i < lines; i++) {
            graphics.text(font, banner.get(i), x0 + 3, y + 2 + i * 10, BANNER_INK, false);
        }
        return bottom;
    }

    private static String readOnlyReason(NoteList list) {
        if (list.guest(ClientNotes.self()).isPresent()) {
            return "gui.servus_notes.notebook.read_only.guest";
        }
        if (list.scope() == Scope.SERVER) {
            return "gui.servus_notes.notebook.read_only.server";
        }
        return "gui.servus_notes.notebook.read_only.other";
    }

    private void extractRows(GuiGraphicsExtractor graphics, Font font, NoteList list, Rights rights, int x0, int x1, int top, int bottom,
                             double mouseX, double mouseY) {
        int count = Math.max(1, (bottom - top) / RULE_PITCH);
        rows = new Rows(x0, x1, top, count);
        for (int row = 0; row < count; row++) {
            int ruleY = top + (row + 1) * RULE_PITCH - 1;
            graphics.fill(x0 - 2, ruleY, x1 + 2, ruleY + 1, RULE);
        }
        List<Task> tasks = list.tasks();
        int lines = tasks.size() + (drafting ? 1 : 0);
        if (lines == 0) {
            graphics.text(font, Component.translatable("gui.servus_notes.notebook.no_tasks"), x0, top + 2, FADED_INK, false);
        }
        if (dragging) {
            autoScroll();
        }
        int maxScroll = Math.max(0, lines - count);
        scroll = stickToEnd && !dragging ? maxScroll : Mth.clamp(scroll, 0, maxScroll);
        UUID selected = screen.taskPage().selectedTask();
        for (int row = 0; row < count && scroll + row < lines; row++) {
            int rowTop = top + row * RULE_PITCH;
            if (scroll + row == tasks.size()) {
                extractDraftRow(graphics, x0, x1, rowTop);
            } else {
                extractRow(graphics, font, tasks.get(scroll + row), rights, x0, x1, rowTop, selected, mouseX, mouseY);
            }
        }
        if (lines > count) {
            int x = x1 + 3;
            int height = count * RULE_PITCH;
            int thumb = Math.max(8, height * count / lines);
            int thumbTop = top + (height - thumb) * scroll / (lines - count);
            graphics.fill(x, top, x + 1, top + height, SCROLL_TRACK);
            graphics.fill(x - 1, thumbTop, x + 2, thumbTop + thumb, FADED_INK);
        }
        if (dragging) {
            extractDrag(graphics, font, tasks);
        }
    }

    private void extractRow(GuiGraphicsExtractor graphics, Font font, Task task, Rights rights, int x0, int x1, int rowTop,
                            @Nullable UUID selected, double mouseX, double mouseY) {
        int boxX = x0 + (task.sub() ? SUB_INDENT : 0);
        int textX = boxX + TEXT_GAP;
        int y = rowTop + 2;
        boolean hovered = !dragging && PageHits.over(mouseX, mouseY, x0 - 2, rowTop, x1 - x0 + 4, RULE_PITCH - 1);
        if (task.id().equals(selected)) {
            graphics.fill(x0 - 2, rowTop, x1 + 2, rowTop + RULE_PITCH - 1, ProjectsPage.SELECTED);
            graphics.fill(x0 - 4, rowTop, x0 - 2, rowTop + RULE_PITCH - 1, ProjectsPage.MARK);
        } else if (hovered) {
            graphics.fill(x0 - 2, rowTop, x1 + 2, rowTop + RULE_PITCH - 1, TASK_HOVER);
        }
        graphics.outline(boxX, y, BOX, BOX, INK);
        if (task.isDone()) {
            motion.extractCheck(graphics, task.id(), boxX, y);
        }
        int marksX = extractMarks(graphics, font, task, x1, y, mouseX, mouseY);
        if (task.id().equals(editingTask)) {
            PaperButton.drawField(graphics, textX - 2, rowTop, marksX - textX, RULE_PITCH - 1, true);
            rowField = new int[] {textX + 1, rowTop + 2, marksX - textX - 5};
        } else {
            String text = NotebookScreen.ellipsized(font, task.text(), marksX - 3 - textX);
            graphics.text(font, text, textX, y, task.isDone() ? DONE_INK : INK, false);
            if (task.isDone()) {
                int width = Math.round(font.width(text) * motion.strike(task.id()));
                graphics.fill(textX - 1, y + 4, textX - 1 + width + 2, y + 5, STRIKE);
            }
        }
        boolean mayDrag = rights.edit();
        int textEnd = marksX;
        screen.hits().add(x0 - 2, rowTop, x1 - x0 + 4, RULE_PITCH - 1,
                () -> press(task, mayDrag, pressX >= textX && pressX < textEnd));
        if (rights.tick()) {
            screen.hits().add(boxX - 2, rowTop, BOX + 4, RULE_PITCH - 1, () -> tick(task));
            if (!dragging && PageHits.over(mouseX, mouseY, boxX - 2, rowTop, BOX + 4, RULE_PITCH - 1)) {
                screen.tooltip(Component.translatable(task.isDone() ? "gui.servus_notes.notebook.action.untick" : "gui.servus_notes.notebook.action.tick"));
            }
        }
    }

    /** The line of a new task at the end of the list: a faded box and the field to write in. */
    private void extractDraftRow(GuiGraphicsExtractor graphics, int x0, int x1, int rowTop) {
        graphics.outline(x0, rowTop + 2, BOX, BOX, FADED_INK);
        int textX = x0 + TEXT_GAP;
        PaperButton.drawField(graphics, textX - 2, rowTop, x1 - textX + 2, RULE_PITCH - 1, true);
        rowField = new int[] {textX + 1, rowTop + 2, x1 - textX - 3};
    }

    /** The small marks for assignee, place and items, right-aligned to {@code right}; returns their left edge. */
    private int extractMarks(GuiGraphicsExtractor graphics, Font font, Task task, int right, int y, double mouseX, double mouseY) {
        int x = right;
        if (task.assignee().isPresent()) {
            String who = Component.translatable("gui.servus_notes.notebook.assignee", task.assignee().get().name()).getString();
            who = NotebookScreen.ellipsized(font, who, 50);
            x -= font.width(who);
            graphics.text(font, who, x, y, ASSIGNEE, false);
            x -= 3;
        }
        if (task.spot().isPresent()) {
            x -= font.width(PLACE_MARK);
            graphics.text(font, PLACE_MARK, x, y, FADED_INK, false);
            if (!dragging && PageHits.over(mouseX, mouseY, x - 1, y - 2, font.width(PLACE_MARK) + 2, RULE_PITCH - 1)) {
                screen.tooltip(Component.literal(Places.chip(task.spot().get())));
            }
            x -= 3;
        }
        if (!task.goals().isEmpty()) {
            String goals = GOALS_MARK + task.goals().size();
            x -= font.width(goals);
            graphics.text(font, goals, x, y, FADED_INK, false);
            x -= 3;
        }
        return x;
    }

    private void syncWidgets(int x0, int x1, int titleY) {
        titleBox.setX(x0);
        titleBox.setY(titleY);
        TaskPage.fitWidth(titleBox, x1 - x0);
        titleBox.setVisible(renaming);
        if (!renaming && screen.getFocused() == titleBox) {
            screen.setFocused(null);
        }
        boolean showRow = rowField != null && !dragging;
        rowBox.setVisible(showRow);
        if (showRow) {
            rowBox.setX(rowField[0]);
            rowBox.setY(rowField[1]);
            TaskPage.fitWidth(rowBox, rowField[2]);
            rowBox.setHint(drafting ? TaskPage.hint("gui.servus_notes.notebook.new_task_hint") : Component.empty());
        } else if (screen.getFocused() == rowBox) {
            screen.setFocused(null);
        }
    }

    /** Hides the fields while no project is open. */
    void hide() {
        titleBox.setVisible(false);
        rowBox.setVisible(false);
    }

    // ---------- dragging ----------

    /** Where a drag would put the task now: the edit to send, or empty while it would not move or may not go there. */
    private Optional<NoteEdit.PlaceTask> dropTarget(List<Task> tasks) {
        if (rows == null || pressed == null) {
            return Optional.empty();
        }
        int index = indexOf(tasks, pressed);
        if (index < 0) {
            return Optional.empty();
        }
        Task moved = tasks.get(index);
        int end = index + 1;
        while (!moved.sub() && end < tasks.size() && tasks.get(end).sub()) {
            end++;
        }
        int gap = Mth.clamp((int) Math.round((dragY - rows.top()) / RULE_PITCH) + scroll, 0, tasks.size());
        int above = gap - 1;
        if (above >= index && above < end) {
            above = index - 1;
        }
        Optional<UUID> after = above >= 0 ? Optional.of(tasks.get(above).id()) : Optional.empty();
        double indent = (moved.sub() ? SUB_INDENT : 0) + dragX - pressX;
        NoteEdit.PlaceTask place = new NoteEdit.PlaceTask(ClientNotes.selected(), moved.id(), after, indent >= SUB_INDENT / 2.0);
        if (NoteEdits.placeRefusal(tasks, place).isPresent()) {
            place = new NoteEdit.PlaceTask(place.listId(), place.taskId(), after, false);
        }
        if (NoteEdits.placeRefusal(tasks, place).isPresent() || NoteEdits.placed(tasks, place).equals(tasks)) {
            return Optional.empty();
        }
        return Optional.of(place);
    }

    /** The dragged lines faded, a red line where the task lands, and the dragged line under the mouse. */
    private void extractDrag(GuiGraphicsExtractor graphics, Font font, List<Task> tasks) {
        Rows at = rows;
        int index = indexOf(tasks, pressed);
        if (at == null || index < 0) {
            cancelDrag();
            return;
        }
        Task moved = tasks.get(index);
        for (int i = index; i < tasks.size() && (i == index || !moved.sub() && tasks.get(i).sub()); i++) {
            int row = i - scroll;
            if (row >= 0 && row < at.count()) {
                int rowTop = at.top() + row * RULE_PITCH;
                graphics.fill(at.x0() - 2, rowTop, at.x1() + 2, rowTop + RULE_PITCH - 1, DRAGGED);
            }
        }
        dropTarget(tasks).ifPresent(place -> {
            List<Task> result = NoteEdits.placed(tasks, place);
            int landed = indexOf(result, moved.id());
            int lineY = at.top();
            if (landed > 0) {
                int before = indexOf(tasks, result.get(landed - 1).id());
                lineY = at.top() + (before + 1 - scroll) * RULE_PITCH - 1;
            }
            if (lineY >= at.top() - 1 && lineY <= at.bottom()) {
                int lineX = at.x0() + (result.get(landed).sub() ? SUB_INDENT : 0);
                graphics.fill(lineX - 2, lineY - 1, at.x1() + 2, lineY + 1, DROP_LINE);
            }
        });
        int ghostX = (int) Math.round(at.x0() + (moved.sub() ? SUB_INDENT : 0) + dragX - pressX);
        int ghostY = (int) Math.round(dragY) - RULE_PITCH / 2;
        String text = NotebookScreen.ellipsized(font, moved.text(), at.x1() - at.x0() - TEXT_GAP);
        int width = TEXT_GAP + font.width(text) + 4;
        graphics.fill(ghostX - 2, ghostY, ghostX + width, ghostY + RULE_PITCH - 1, GHOST);
        graphics.outline(ghostX, ghostY + 2, BOX, BOX, INK);
        graphics.text(font, text, ghostX + TEXT_GAP, ghostY + 2, INK, false);
    }

    /** Scrolls the rows while the dragged line is held near their top or bottom edge. */
    private void autoScroll() {
        if (rows == null || Util.getMillis() - lastAutoScroll < SCROLL_MILLIS) {
            return;
        }
        if (dragY < rows.top() + 4) {
            scroll--;
        } else if (dragY > rows.bottom() - 4) {
            scroll++;
        } else {
            return;
        }
        stickToEnd = false;
        lastAutoScroll = Util.getMillis();
    }

    private void cancelDrag() {
        dragging = false;
        pressed = null;
    }

    private static int indexOf(List<Task> tasks, @Nullable UUID taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(taskId)) {
                return i;
            }
        }
        return -1;
    }

    // ---------- actions ----------

    private void press(Task task, boolean mayDrag, boolean onText) {
        pressed = task.id();
        pressedMayDrag = mayDrag;
        pressedOnText = onText;
        dragX = pressX;
        dragY = pressY;
    }

    /** A click on a line without a drag: the open task's text turns into a field for editors, any other line opens. */
    private void clicked(ListView view, Task task) {
        boolean open = task.id().equals(screen.taskPage().selectedTask());
        if (open && pressedOnText && view.rights().edit()) {
            startEdit(task);
        } else {
            screen.taskPage().open(task);
        }
    }

    private void tick(Task task) {
        boolean done = !task.isDone();
        if (done) {
            motion.tickSent(task.id());
        }
        ClientNotes.send(new NoteEdit.Tick(ClientNotes.selected(), task.id(), done));
    }

    private void startEdit(Task task) {
        commitRow();
        drafting = false;
        editingTask = task.id();
        rowBox.setValue(task.text());
        rowBox.setVisible(true);
        rowBox.moveCursorToEnd(false);
        screen.setFocused(rowBox);
    }

    private void startDraft() {
        commitRow();
        editingTask = null;
        drafting = true;
        stickToEnd = true;
        rowBox.setValue("");
        rowBox.setVisible(true);
        screen.setFocused(rowBox);
    }

    /** Sends what the line field holds: the edited task's new text, or the new task. Empty text changes nothing. */
    private void commitRow() {
        if (rowBox == null) {
            return;
        }
        String text = rowBox.getValue().strip();
        UUID listId = ClientNotes.selected();
        if (drafting) {
            if (!text.isEmpty() && listId != null) {
                UUID taskId = UUID.randomUUID();
                ClientNotes.send(new NoteEdit.AddTask(listId, taskId, text, Optional.empty()));
                openWhenArrives = taskId;
                stickToEnd = true;
            }
            rowBox.setValue("");
            return;
        }
        if (editingTask == null) {
            return;
        }
        UUID taskId = editingTask;
        editingTask = null;
        ClientNotes.get(listId).flatMap(view -> view.list().task(taskId)).ifPresent(task -> {
            if (!text.isEmpty() && !text.equals(task.text())) {
                ClientNotes.send(new NoteEdit.EditText(listId, taskId, text));
            }
        });
    }

    private boolean deleteArmed() {
        return deleteArmedAt != Long.MIN_VALUE && Util.getMillis() - deleteArmedAt < CONFIRM_MILLIS;
    }

    private void deletePressed(NoteList list) {
        if (deleteArmed()) {
            deleteArmedAt = Long.MIN_VALUE;
            ClientNotes.send(new NoteEdit.DeleteList(list.id()));
        } else {
            deleteArmedAt = Util.getMillis();
        }
    }

    private void startRename(NoteList list) {
        renaming = true;
        titleBox.setValue(list.name());
        titleBox.setVisible(true);
        titleBox.moveCursorToEnd(false);
        titleBox.setHighlightPos(0);
        screen.setFocused(titleBox);
    }

    private void commitRename() {
        if (!renaming) {
            return;
        }
        renaming = false;
        String name = titleBox.getValue().strip();
        ClientNotes.get(ClientNotes.selected()).ifPresent(view -> {
            if (!name.isEmpty() && !name.equals(view.list().name())) {
                ClientNotes.send(new NoteEdit.RenameList(view.list().id(), name));
            }
        });
    }

    // ---------- input ----------

    /**
     * Before a click is handled: a rename or line edit ends when the click lands outside its field, a new task line
     * closes, an armed Delete disarms. Remembers where the press was, for a drag.
     */
    void beforeClick(double mouseX, double mouseY) {
        pressX = mouseX;
        pressY = mouseY;
        pressed = null;
        dragging = false;
        if (renaming && !titleBox.isMouseOver(mouseX, mouseY)) {
            commitRename();
        }
        if ((editingTask != null || drafting) && !rowBox.isMouseOver(mouseX, mouseY)) {
            commitRow();
            editingTask = null;
            drafting = false;
        }
        if (deleteArmed() && !PageHits.over(mouseX, mouseY, deleteButton[0], deleteButton[1], deleteButton[2], PaperButton.HEIGHT)) {
            deleteArmedAt = Long.MIN_VALUE;
        }
    }

    /** The mouse moved with the button down: a press on a line of an editor becomes a drag once it moved far enough. */
    boolean dragged(double mouseX, double mouseY) {
        if (pressed == null) {
            return false;
        }
        dragX = mouseX;
        dragY = mouseY;
        if (!dragging && pressedMayDrag && Math.hypot(mouseX - pressX, mouseY - pressY) >= DRAG_START) {
            dragging = true;
        }
        return true;
    }

    /** The button came up: a drag drops the task where the line shows, a press without a drag acts as a click. */
    boolean released(@Nullable ListView view, double mouseX, double mouseY) {
        if (pressed == null) {
            return false;
        }
        UUID taskId = pressed;
        boolean wasDragging = dragging;
        cancelDrag();
        if (view == null) {
            return true;
        }
        List<Task> tasks = view.list().tasks();
        if (wasDragging) {
            dragX = mouseX;
            dragY = mouseY;
            pressed = taskId;
            boolean inside = rows != null && PageHits.over(mouseX, mouseY, rows.x0() - 8, rows.top() - RULE_PITCH,
                    rows.x1() - rows.x0() + 16, rows.count() * RULE_PITCH + 2 * RULE_PITCH);
            Optional<NoteEdit.PlaceTask> place = inside ? dropTarget(tasks) : Optional.empty();
            pressed = null;
            place.ifPresent(ClientNotes::send);
        } else {
            view.list().task(taskId).ifPresent(task -> clicked(view, task));
        }
        return true;
    }

    /** Enter sends a field, Escape backs out of a drag, a field or the armed Delete; false when the key is not this page's. */
    boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (dragging) {
                cancelDrag();
            } else if (renaming) {
                renaming = false;
                screen.setFocused(null);
            } else if (editingTask != null || drafting) {
                editingTask = null;
                drafting = false;
                rowBox.setValue("");
                screen.setFocused(null);
            } else if (deleteArmed()) {
                deleteArmedAt = Long.MIN_VALUE;
            } else {
                return false;
            }
            return true;
        }
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER) {
            return false;
        }
        if (renaming && screen.getFocused() == titleBox) {
            commitRename();
            screen.setFocused(null);
            return true;
        }
        if (screen.getFocused() == rowBox && drafting) {
            commitRow();
            return true;
        }
        if (screen.getFocused() == rowBox && editingTask != null) {
            commitRow();
            screen.setFocused(null);
            return true;
        }
        return false;
    }

    /** Scrolls the rows when the mouse is over this page. */
    boolean scrolled(BookLayout book, double mouseX, double mouseY, double amount) {
        if (!PageHits.over(mouseX, mouseY, book.leftPageX(), book.pageY(), book.leftPageWidth(), book.pageHeight())) {
            return false;
        }
        scroll -= (int) Math.signum(amount);
        stickToEnd = false;
        return true;
    }
}
