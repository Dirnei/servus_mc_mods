package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.ListView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The notebook: an open leather book with paper tabs on the right page's top edge. It has three levels:
 * the projects of the active tab on the left page; once a project is open its tasks on the left page and
 * the chosen task on the right page. Everything, the text fields included, is drawn in book coordinates
 * (0,0 is the cover's top left corner, see {@link BookLayout}); {@link #bookX} and {@link #bookY} turn
 * mouse positions into them, and mouse events reach the widgets already turned. The game keeps running
 * behind it.
 */
public class NotebookScreen extends Screen {

    /** How far the active tab stands above the cover; inactive tabs sit {@link #TAB_DROP} lower. */
    public static final int TAB_RISE = BookLayout.TAB_RISE;
    public static final int TAB_DROP = 3;
    public static final int TAB_HEIGHT = 18;
    public static final int TAB_GAP = 2;
    public static final int TAB_PADDING = 4;
    /** The left edge of the first tab, measured from the spine. */
    private static final int TAB_INSET = 2;
    /** Inner margin of text on a page. */
    public static final int PAGE_MARGIN = 8;
    /** The height of a ruled line. */
    public static final int RULE_PITCH = 12;

    public static final int INK = 0xFF3B2A1A;
    public static final int FADED_INK = 0xFF8A6A40;
    public static final int HEADING_INK = 0xFF7A4A28;
    public static final int RULE = 0xFFD7C49A;
    public static final int TAB_TEXT = 0xFFFFFFFF;
    private static final int FOOTER_TEXT = 0xFFF1E4C3;
    private static final int TAB_COUNT_TEXT = 0xD0FFFFFF;

    private static final Identifier COVER = sprite("cover");
    private static final Identifier LEFT_PAGE = sprite("page_left");
    private static final Identifier RIGHT_PAGE = sprite("page_right");
    private static final long FOOTER_MILLIS = 3000;
    private static final String ELLIPSIS = "...";

    private final PageHits hits = new PageHits();
    private final List<ItemDrop> itemDrops = new ArrayList<>();
    private final NotebookMotion motion = new NotebookMotion();
    private final ProjectsPage projects = new ProjectsPage(this);
    private final TaskListPage taskList = new TaskListPage(this, motion);
    private final TaskPage taskPage = new TaskPage(this, motion);
    private final ClientNotes.Listener listener = new ClientNotes.Listener() {
        @Override
        public void listChanged(@Nullable ListView before, @Nullable ListView after) {
            motion.listChanged(after);
        }

        @Override
        public void allReplaced() {
            taskList.applyLimits();
            taskPage.applyLimits();
        }
    };

    private NotebookTab activeTab = NotebookTab.MINE;
    private @Nullable Component tooltip;
    private ItemStack tooltipStack = ItemStack.EMPTY;
    private @Nullable Component tooltipExtra;

    public NotebookScreen() {
        super(Component.translatable("gui.servus_notes.notebook"));
    }

    @Override
    protected void init() {
        taskList.init(font, this::addRenderableWidget);
        taskPage.init(font, this::addRenderableWidget);
    }

    @Override
    public void added() {
        super.added();
        ClientNotes.addListener(listener);
    }

    @Override
    public void removed() {
        taskList.reset();
        taskPage.reset();
        ClientNotes.removeListener(listener);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    /** The book's place and size this frame; it moves left to leave JEI room while an item is picked. */
    BookLayout layout() {
        return BookLayout.fit(width, height, picking());
    }

    /** Whether the item picker is open, the only time JEI shows beside the notebook. */
    public boolean picking() {
        return taskPage.picking();
    }

    /** Book x of a screen x. */
    protected double bookX(double screenX) {
        BookLayout book = layout();
        return (screenX - book.left()) / book.scale();
    }

    /** Book y of a screen y. */
    protected double bookY(double screenY) {
        BookLayout book = layout();
        return (screenY - book.top()) / book.scale();
    }

    private MouseButtonEvent toBook(MouseButtonEvent event) {
        return new MouseButtonEvent(bookX(event.x()), bookY(event.y()), event.buttonInfo());
    }

    // ---------- what the pages ask ----------

    Font font() {
        return font;
    }

    PageHits hits() {
        return hits;
    }

    TaskPage taskPage() {
        return taskPage;
    }

    TaskListPage taskList() {
        return taskList;
    }

    /** The active tab; Team falls back to Mine once the player is in no party. */
    NotebookTab activeTab() {
        if (!activeTab.available(ClientNotes.teamsInstalled(), ClientNotes.party())) {
            activeTab = NotebookTab.MINE;
        }
        return activeTab;
    }

    /** The tabs the notebook shows now, from left to right. */
    private List<NotebookTab> tabs() {
        return Arrays.stream(NotebookTab.values())
                .filter(tab -> tab.available(ClientNotes.teamsInstalled(), ClientNotes.party()))
                .toList();
    }

    private static List<ListView> listsOf(NotebookTab tab) {
        UUID self = ClientNotes.self();
        Optional<UUID> party = ClientNotes.party();
        return ClientNotes.lists().stream().filter(view -> tab.shows(view, self, party)).toList();
    }

    /** Shows {@code text} as a tooltip at the mouse this frame. */
    void tooltip(Component text) {
        tooltip = text;
        tooltipStack = ItemStack.EMPTY;
    }

    /** Shows the item's own tooltip at the mouse this frame. */
    void tooltip(ItemStack stack) {
        tooltip(stack, null);
    }

    /** Shows the item's own tooltip with {@code extra} as its last line at the mouse this frame. */
    void tooltip(ItemStack stack, @Nullable Component extra) {
        tooltip = null;
        tooltipStack = stack;
        tooltipExtra = extra;
    }

    /** The places that take a dropped item this frame, in book coordinates; the pages add to it while they draw. */
    List<ItemDrop> itemDrops() {
        return itemDrops;
    }

    /** The book's area on the screen, tabs included, as {x, y, width, height}; JEI keeps its item list beside it. */
    public int[] bookArea() {
        BookLayout book = layout();
        int top = Math.round(book.top() - TAB_RISE * book.scale());
        return new int[] {book.left(), top, Math.round(book.width() * book.scale()), Math.round((book.height() + TAB_RISE) * book.scale())};
    }

    /** The places that take a dropped item, in screen coordinates, for JEI. Empty unless an item is picked, and while a page turns. */
    public List<ItemDrop> itemDropsOnScreen() {
        if (!picking() || motion.flipping()) {
            return List.of();
        }
        BookLayout book = layout();
        float scale = book.scale();
        return itemDrops.stream().map(drop -> new ItemDrop(
                Math.round(book.left() + drop.x() * scale), Math.round(book.top() + drop.y() * scale),
                Math.round(drop.width() * scale), Math.round(drop.height() * scale), drop.accept())).toList();
    }

    /** The projects of the active tab, in the order they were made. */
    List<ListView> tabLists() {
        return listsOf(activeTab());
    }

    /** Opens a project: its tasks on the left page, no task on the right yet. Null goes back to the projects. */
    void openProject(@Nullable UUID listId) {
        taskList.reset();
        taskPage.reset();
        ClientNotes.select(listId);
        motion.flipBoth();
    }

    /** Opens a project this client just asked the server to make; its name opens for renaming when it arrives. */
    void openNewProject(UUID listId) {
        openProject(listId);
        taskList.renameWhenArrives(listId);
    }

    /** The open project if the active tab shows it; a project that is gone closes back to the projects. */
    private @Nullable ListView project() {
        UUID selected = ClientNotes.selected();
        if (selected == null) {
            return null;
        }
        for (ListView view : tabLists()) {
            if (view.list().id().equals(selected)) {
                return view;
            }
        }
        if (!taskList.awaiting(selected)) {
            taskList.reset();
            taskPage.reset();
            ClientNotes.select(null);
        }
        return null;
    }

    // ---------- drawing ----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        itemDrops.clear();
        tooltip = null;
        tooltipStack = ItemStack.EMPTY;
        tooltipExtra = null;
        BookLayout book = layout();
        double x = bookX(mouseX);
        double y = bookY(mouseY);
        ListView view = project();
        graphics.pose().pushMatrix();
        graphics.pose().translate(book.left(), book.top());
        graphics.pose().scale(book.scale(), book.scale());
        extractTabs(graphics, book, x, y);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COVER, 0, 0, book.width(), book.height());
        extractLeftPage(graphics, book, view, x, y);
        extractRightPage(graphics, book, view, x, y);
        if (!motion.flipping()) {
            for (Renderable renderable : renderables) {
                renderable.extractRenderState(graphics, (int) Math.floor(x), (int) Math.floor(y), partialTick);
            }
        }
        extractFooter(graphics, book);
        graphics.pose().popMatrix();
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        } else if (!tooltipStack.isEmpty() && tooltipExtra != null) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, tooltipStack));
            lines.add(tooltipExtra.copy().withStyle(ChatFormatting.GRAY));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, tooltipStack);
        } else if (!tooltipStack.isEmpty()) {
            graphics.setTooltipForNextFrame(font, tooltipStack, mouseX, mouseY);
        }
    }

    private void extractLeftPage(GuiGraphicsExtractor graphics, BookLayout book, @Nullable ListView view, double x, double y) {
        int spine = book.rightPageX();
        graphics.pose().pushMatrix();
        float flip = motion.leftFlipScale();
        if (flip < 1F) {
            graphics.pose().translate(spine, 0);
            graphics.pose().scale(flip, 1F);
            graphics.pose().translate(-spine, 0);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LEFT_PAGE, book.leftPageX(), book.pageY(), book.leftPageWidth(), book.pageHeight());
        if (view == null) {
            taskList.hide();
            projects.extract(graphics, book, x, y);
        } else {
            taskList.extract(graphics, book, view, x, y);
        }
        graphics.pose().popMatrix();
    }

    private void extractRightPage(GuiGraphicsExtractor graphics, BookLayout book, @Nullable ListView view, double x, double y) {
        int spine = book.rightPageX();
        graphics.pose().pushMatrix();
        float flip = motion.rightFlipScale();
        if (flip < 1F) {
            graphics.pose().translate(spine, 0);
            graphics.pose().scale(flip, 1F);
            graphics.pose().translate(-spine, 0);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, RIGHT_PAGE, spine, book.pageY(), book.rightPageWidth(), book.pageHeight());
        if (view == null) {
            int x0 = spine + PAGE_MARGIN + 4;
            Component hint = Component.translatable(tabLists().isEmpty() ? "gui.servus_notes.notebook.no_projects_hint" : "gui.servus_notes.notebook.pick_project");
            List<FormattedCharSequence> lines = font.split(hint, book.rightPageWidth() - 2 * PAGE_MARGIN - 8);
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(font, lines.get(i), x0, book.pageY() + 12 + i * 10, FADED_INK, false);
            }
        }
        taskPage.extract(graphics, book, view, x, y);
        graphics.pose().popMatrix();
    }

    /** The tabs with the number of projects in each; the active one stands up. */
    private void extractTabs(GuiGraphicsExtractor graphics, BookLayout book, double mouseX, double mouseY) {
        int x = book.rightPageX() + TAB_INSET;
        NotebookTab current = activeTab();
        for (NotebookTab tab : tabs()) {
            boolean active = tab == current;
            int width = tabWidth(tab);
            int y = -TAB_RISE + (active ? 0 : TAB_DROP);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, tab.sprite(active), x, y, width, TAB_HEIGHT);
            graphics.text(font, tab.label(), x + TAB_PADDING, y + 4, TAB_TEXT, true);
            graphics.text(font, count(tab), x + TAB_PADDING + font.width(tab.label()) + 3, y + 4, TAB_COUNT_TEXT, true);
            if (PageHits.over(mouseX, mouseY, x, -TAB_RISE, width, TAB_RISE)) {
                tooltip(tab.tooltip());
            }
            x += width + TAB_GAP;
        }
    }

    private static String count(NotebookTab tab) {
        return Integer.toString(listsOf(tab).size());
    }

    /** The last refused edit, for a few seconds, on the leather below the pages. */
    private void extractFooter(GuiGraphicsExtractor graphics, BookLayout book) {
        ClientNotes.refusal()
                .filter(refusal -> Util.getMillis() - refusal.at() < FOOTER_MILLIS)
                .ifPresent(refusal -> graphics.centeredText(font,
                        Component.translatable("gui.servus_notes.notebook.refused." + refusal.reasonKey()),
                        book.width() / 2, book.pageBottom() + 2, FOOTER_TEXT));
    }

    private int tabWidth(NotebookTab tab) {
        return font.width(tab.label()) + 3 + font.width(count(tab)) + 2 * TAB_PADDING;
    }

    /**
     * Draws a paper button with its left edge at {@code x} and makes it clickable; returns its width.
     * A null {@code action} draws it greyed and not clickable.
     */
    int button(GuiGraphicsExtractor graphics, Component label, int x, int y, double mouseX, double mouseY, int ink,
               @Nullable Runnable action) {
        int width = PaperButton.width(font, label);
        boolean hovered = action != null && PageHits.over(mouseX, mouseY, x, y, width, PaperButton.HEIGHT);
        PaperButton.draw(graphics, font, label, x, y, width, hovered, action == null ? FADED_INK : ink);
        if (action != null) {
            hits.add(x, y, width, PaperButton.HEIGHT, action);
        }
        return width;
    }

    /** The text cut to {@code width} with "..." when it does not fit. */
    static String ellipsized(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ELLIPSIS))) + ELLIPSIS;
    }

    /** The styled text cut to {@code width} with "..." when it does not fit. */
    static FormattedCharSequence ellipsized(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        FormattedText cut = font.substrByWidth(text, Math.max(0, width - font.width(ELLIPSIS)));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of(ELLIPSIS, text.getStyle())));
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = bookX(event.x());
        double y = bookY(event.y());
        NotebookTab clicked = tabAt(x, y);
        if (clicked != null) {
            if (clicked != activeTab() || ClientNotes.selected() != null) {
                activeTab = clicked;
                openProject(null);
            }
            return true;
        }
        GuiEventListener focused = getFocused();
        if (focused != null && !focused.isMouseOver(x, y)) {
            setFocused(null);
        }
        taskList.beforeClick(x, y);
        taskPage.beforeClick(x, y);
        if (getChildAt(x, y).isEmpty() && hits.click(x, y)) {
            return true;
        }
        return super.mouseClicked(toBook(event), doubleClick);
    }

    private @Nullable NotebookTab tabAt(double x, double y) {
        if (y < -TAB_RISE || y >= 0) {
            return null;
        }
        int left = layout().rightPageX() + TAB_INSET;
        for (NotebookTab tab : tabs()) {
            int width = tabWidth(tab);
            if (x >= left && x < left + width) {
                return tab;
            }
            left += width + TAB_GAP;
        }
        return null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (taskList.released(project(), bookX(event.x()), bookY(event.y()))) {
            return true;
        }
        return super.mouseReleased(toBook(event));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (taskList.dragged(bookX(event.x()), bookY(event.y()))) {
            return true;
        }
        float scale = layout().scale();
        return super.mouseDragged(toBook(event), dx / scale, dy / scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double x = bookX(mouseX);
        double y = bookY(mouseY);
        if (taskPage.scrolled(x, y, scrollY)) {
            return true;
        }
        boolean left = ClientNotes.selected() == null ? projects.scrolled(layout(), x, y, scrollY) : taskList.scrolled(layout(), x, y, scrollY);
        if (left) {
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(bookX(mouseX), bookY(mouseY));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (taskList.keyPressed(event) || taskPage.keyPressed(event)) {
            return true;
        }
        boolean typing = getFocused() instanceof EditBox || getFocused() instanceof MultiLineEditBox;
        if (!typing && NotesKeys.OPEN_NOTEBOOK.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notebook/" + name);
    }
}
