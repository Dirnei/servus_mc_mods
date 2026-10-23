package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.RecipeEditorPopup;
import io.github.dirnei.mc.servus.foundation.picker.PickerStock;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.pattern.CraftingGrid;
import io.github.dirnei.mc.servus.shuttles.production.CancelProductionOrderPayload;
import io.github.dirnei.mc.servus.shuttles.production.EditPatternPayload;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.BufferItem;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.BufferLine;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.DuctRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.ItemCount;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.JobRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.PackingRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.StorageRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.ZoneRow;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Production;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Title, a tab row (Crafting, Patterns, Machines, CPUs, Storage) and a scrollable list
 * below. Each tab turns the last synced rows into lines; a line can draw
 * itself, show a tooltip and react to a click. The Patterns tab opens the
 * shared recipe editor popup over the panel, with the player inventory below
 * it while it is open.
 */
public class ProductionTerminalScreen extends AbstractContainerScreen<ProductionTerminalMenu> {

    private static final int WIDTH = Production.WIDTH;
    private static final int HEIGHT = Production.HEIGHT;
    private static final int MARGIN = Production.LIST.x();
    private static final int TAB_Y = Production.TABS.y();
    private static final int TAB_HEIGHT = Production.TABS.h();
    private static final int LIST_Y = Production.LIST.y();
    private static final int LIST_BOTTOM = Production.LIST.bottom();
    private static final int PATTERN_ROW = 18;
    private static final int ROW = 10;
    private static final int CAPACITY_BAR_WIDTH = 80;
    /** Ended jobs fade out over this many ticks after they end. */
    private static final int FADE_TICKS = 100;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_DIM = 0xFF707070;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int COLOUR_GOOD = 0xFF2E7D32;
    private static final int COLOUR_HEAD = 0xFF1F4E5A;
    private static final int COLOUR_BUFFER = 0xFF8A5A00;
    private static final int COLOUR_TEAL = 0xFF2E8B7A;
    private static final int COLOUR_AMBER = 0xFFE0A030;
    private static final int COLOUR_FLUID = 0xFF3A6EC0;
    private static final int HOVER_SHADE = 0x20000000;
    private static final int PATTERN_HOVER_SHADE = 0x30000000;

    /** Draws one line at its top edge. */
    private interface Painter {
        void paint(GuiGraphicsExtractor graphics, int y, boolean hovered);
    }

    /** Tooltip and click by position inside a line, relative to the list's left edge and the line's top. */
    private interface Hit {
        List<Component> tooltip(int lx, int ly);

        boolean click(int lx, int ly);
    }

    /** One drawn line: its height, how it looks, its tooltip and what a click does (null for nothing), or a hit for parts. */
    private record Line(int height, Painter painter, Supplier<List<Component>> tooltip, Runnable click, Hit hit) {
        Line(int height, Painter painter, Supplier<List<Component>> tooltip, Runnable click) {
            this(height, painter, tooltip, click, null);
        }
    }

    /** One tab: its title key and how it lists the synced rows. */
    private record Tab(String key, Supplier<List<Line>> lines) {
    }

    private final List<Tab> tabs = List.of(
            new Tab("crafting", this::craftingLines),
            new Tab("patterns", this::patternLines),
            new Tab("machines", this::machineLines),
            new Tab("cpus", this::cpuLines),
            new Tab("storage", this::storageLines));
    private final int[] scrolls = new int[tabs.size()];
    private final List<Line> lines = new ArrayList<>();
    private final RecipeEditorPopup popup = new RecipeEditorPopup(ProductionTerminalMenu.POPUP_X, ProductionTerminalMenu.POPUP_Y, WIDTH, HEIGHT);
    private int selected;
    private int linesVersion = -1;
    private int linesTab = -1;

    public ProductionTerminalScreen(ProductionTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIDTH, HEIGHT);
    }

    @Override
    public void removed() {
        super.removed();
        ClientProduction.close();
    }

    /** Registers the terminal on every show, also after another screen was in front. */
    @Override
    protected void init() {
        super.init();
        ClientProduction.open(menu.terminal().getBlockPos());
        this.inventoryLabelY = Integer.MIN_VALUE;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + MARGIN, topPos + LIST_Y - 2, leftPos + WIDTH - MARGIN, topPos + LIST_Y - 1, PanelPainter.SLOT);
        if (menu.isEditing()) {
            renderEditor(graphics, mouseX, mouseY);
        }
    }

    /** The recipe editor popup with the player inventory's slot frames below it. */
    private void renderEditor(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean existing = menu.editing() == ProductionTerminalMenu.EDIT_EXISTING;
        Component title = Component.translatable(existing ? "gui.servus_shuttles.production.edit_pattern"
                : "gui.servus_shuttles.production.new_pattern_title");
        popup.render(graphics, font, leftPos, topPos, mouseX, mouseY, title, existing, preview(),
                Component.translatable("gui.servus_shuttles.crafting_station.save"),
                Component.translatable("gui.servus_shuttles.crafting_station.delete"),
                Component.translatable("gui.servus_shuttles.crafting_station.cancel"));
        for (int row = 0; row < 4; row++) {
            int y = topPos + (row < 3 ? ProductionTerminalMenu.INVENTORY_Y + row * 18 : ProductionTerminalMenu.HOTBAR_Y);
            for (int column = 0; column < 9; column++) {
                int x = leftPos + ProductionTerminalMenu.INVENTORY_X + column * 18;
                PanelPainter.bordered(graphics, x, y, 16, 16, PanelPainter.DARK, PanelPainter.SLOT);
            }
        }
    }

    /** The result the ghost grid matches on the client, empty when none. */
    private ItemStack preview() {
        Level level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            return ItemStack.EMPTY;
        }
        return CraftingGrid.match(level, menu.gridCells())
                .map(holder -> ServusRecipes.displayResult(level, holder.value()))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.isEditing()) {
            if (popup.previewHovered(leftPos, topPos, mouseX, mouseY)) {
                ItemStack preview = preview();
                if (preview.isEmpty()) {
                    graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_shuttles.crafting_station.no_match"), mouseX, mouseY);
                } else {
                    graphics.setTooltipForNextFrame(font, preview, mouseX, mouseY);
                }
            }
            return;
        }
        Line hovered = lineAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> tooltip = hovered.hit() != null
                    ? hovered.hit().tooltip(mouseX - leftPos - MARGIN, mouseY - topPos - lineTop(hovered))
                    : hovered.tooltip() == null ? List.of() : hovered.tooltip().get();
            if (!tooltip.isEmpty()) {
                graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
        }
    }

    /** Labels draw in panel coordinates and the scissor follows the pose, so the list needs no leftPos or topPos. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.isEditing()) {
            return;
        }
        graphics.text(font, title, Production.TITLE.x(), Production.TITLE.y(), COLOUR_TEXT, false);
        renderTabs(graphics);
        if (!ClientProduction.loaded()) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.production.loading"), MARGIN, LIST_Y, COLOUR_DIM, false);
            return;
        }
        if (!ClientProduction.online()) {
            String reason = ClientProduction.reason().isEmpty() ? "no_line" : ClientProduction.reason();
            graphics.text(font, Component.translatable("gui.servus_shuttles.production." + reason), MARGIN, LIST_Y, COLOUR_ALERT, false);
            return;
        }
        rebuildLines();
        Line hovered = lineAt(mouseX, mouseY);
        graphics.enableScissor(MARGIN, LIST_Y, WIDTH - MARGIN, LIST_BOTTOM);
        int y = LIST_Y - scrolls[selected];
        for (Line line : lines) {
            if (y + line.height() > LIST_Y && y < LIST_BOTTOM) {
                line.painter().paint(graphics, y, line == hovered);
            }
            y += line.height();
        }
        graphics.disableScissor();
    }

    private static Component tabLabel(Tab tab) {
        return Component.translatable("gui.servus_shuttles.production.tab." + tab.key());
    }

    /** The tabs on the tab row, each sized to its label. */
    private List<Rect> tabRects() {
        int[] widths = new int[tabs.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = font.width(tabLabel(tabs.get(i)));
        }
        return Production.tabs(widths);
    }

    private void renderTabs(GuiGraphicsExtractor graphics) {
        List<Rect> rects = tabRects();
        for (int i = 0; i < tabs.size(); i++) {
            Rect tab = rects.get(i);
            boolean active = i == selected;
            graphics.fill(tab.x(), TAB_Y, tab.right(), TAB_Y + TAB_HEIGHT, active ? PanelPainter.SLOT : PanelPainter.FACE);
            graphics.outline(tab.x(), TAB_Y, tab.w(), TAB_HEIGHT, PanelPainter.DARK);
            String label = font.plainSubstrByWidth(tabLabel(tabs.get(i)).getString(), tab.w() - 2);
            graphics.text(font, label, tab.x() + (tab.w() - font.width(label)) / 2, TAB_Y + 2, active ? 0xFFFFFFFF : COLOUR_TEXT, false);
        }
    }

    /** Rebuilds the lines when the data or the tab changed; the Crafting tab every frame, since its jobs fade. */
    private void rebuildLines() {
        boolean timed = tabs.get(selected).key().equals("crafting");
        if (!timed && linesVersion == ClientProduction.version() && linesTab == selected) {
            return;
        }
        linesVersion = ClientProduction.version();
        linesTab = selected;
        lines.clear();
        lines.addAll(tabs.get(selected).lines().get());
        scrolls[selected] = Math.min(scrolls[selected], maxScroll());
    }

    private int maxScroll() {
        int total = 0;
        for (Line line : lines) {
            total += line.height();
        }
        return Math.max(0, total - (LIST_BOTTOM - LIST_Y));
    }

    /** Text cut to a width, in list coordinates. */
    private void clipped(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int colour) {
        graphics.text(font, font.plainSubstrByWidth(text, width), x, y, colour, false);
    }

    /** Text cut to a part of a line, the part relative to the line's left edge on the list and its top. */
    private void clipped(GuiGraphicsExtractor graphics, String text, Rect part, int lineY, int colour) {
        clipped(graphics, text, MARGIN + part.x(), lineY + part.y(), part.w(), colour);
    }

    /** The light shade behind a hovered line. */
    private static void shade(GuiGraphicsExtractor graphics, int y, int height, boolean hovered) {
        if (hovered) {
            graphics.fill(MARGIN, y, WIDTH - MARGIN, y + height, HOVER_SHADE);
        }
    }

    /** A bar on a part of a line, the part relative to the line's left edge on the list and its top. */
    private static void bar(GuiGraphicsExtractor graphics, Rect part, int lineY, long value, long max, int colour) {
        PanelPainter.bar(graphics, MARGIN + part.x(), lineY + part.y(), part.w(), part.h(), value, max, colour, PanelPainter.DARK);
    }

    private Line text(Component text, int colour) {
        return new Line(ROW, (graphics, y, hovered) -> graphics.text(font, text, MARGIN, y + 1, colour, false), null, null);
    }

    /** "4 Oak Planks, 2 Iron Ingot": items with counts in one line. */
    private static String itemList(List<ItemCount> items) {
        StringBuilder text = new StringBuilder();
        for (ItemCount item : items) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(item.count()).append(' ').append(item.item().getHoverName().getString());
        }
        return text.toString();
    }

    private static List<Component> itemTooltip(Component head, List<ItemCount> items) {
        List<Component> list = new ArrayList<>();
        list.add(head);
        for (ItemCount item : items) {
            list.add(Component.literal(item.count() + " ").append(item.item().getHoverName()));
        }
        return list;
    }

    private long clientTime() {
        return minecraft == null || minecraft.level == null ? 0 : minecraft.level.getGameTime();
    }

    /** How visible an ended job still is, 1 for open jobs, 0 once faded. */
    private float visibility(JobRow job) {
        if (job.open() || job.doneAt() < 0) {
            return 1f;
        }
        long age = clientTime() - job.doneAt();
        return age >= FADE_TICKS ? 0f : 1f - Math.max(0, age) / (float) FADE_TICKS;
    }

    private List<Line> craftingLines() {
        List<Line> result = new ArrayList<>();
        List<CpuRow> cpus = ClientProduction.cpus();
        if (cpus.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.no_cpu"), COLOUR_DIM));
            return result;
        }
        result.add(cpuHeaderLine(cpus));
        int shown = 0;
        for (JobRow job : ClientProduction.jobs()) {
            if (visibility(job) <= 0f) {
                continue;
            }
            result.add(jobLine(job));
            if (job.open()) {
                for (BufferLine line : job.buffer()) {
                    result.add(bufferLine(line));
                }
            }
            if (!job.overflow().isEmpty() && visibility(job) >= 0.5f) {
                result.add(overflowLine(job.overflow()));
            }
            shown++;
        }
        if (shown == 0) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.no_jobs"), COLOUR_DIM));
        }
        return result;
    }

    /** The CPUs' slots in use of all slots, one line per CPU in the tooltip. */
    private Line cpuHeaderLine(List<CpuRow> cpus) {
        int slots = 0;
        int inUse = 0;
        for (CpuRow cpu : cpus) {
            slots += cpu.slots();
            inUse += cpu.inUse();
        }
        Component head = cpus.size() == 1
                ? Component.translatable("gui.servus_shuttles.production.cpus_header_one", inUse, slots)
                : Component.translatable("gui.servus_shuttles.production.cpus_header", cpus.size(), inUse, slots);
        Supplier<List<Component>> tooltip = () -> {
            List<Component> list = new ArrayList<>();
            for (CpuRow cpu : cpus) {
                list.add(cpuLineText(cpu));
            }
            return list;
        };
        return new Line(Production.HEADER_HEIGHT, (graphics, y, hovered) -> graphics.text(font, head, MARGIN, y + 2, COLOUR_HEAD, false),
                tooltip, null);
    }

    private static Component cpuLineText(CpuRow cpu) {
        return Component.translatable("gui.servus_shuttles.production.cpu_line", cpu.cpu().toShortString(), cpu.inUse(), cpu.slots(),
                cpu.waiting());
    }

    private Component status(JobRow job) {
        List<String> args = job.args();
        if (job.status().equals("working") && args.size() == 4) {
            String verbKey = "gui.servus_shuttles.production.verb." + args.get(1);
            Component verb = I18n.exists(verbKey) ? Component.translatable(verbKey) : Component.literal(args.get(1));
            return Component.translatable("gui.servus_shuttles.production.job.working", args.get(0), verb, args.get(2), args.get(3));
        }
        return Component.translatable("gui.servus_shuttles.production.job." + job.status(), args.toArray());
    }

    /** The colour with the alpha of how visible a fading job is, never below a tenth. */
    private static int alpha(int rgb, float visibility) {
        return ((int) (Math.max(0.1f, visibility) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int jobBarColour(JobRow job) {
        return switch (job.state()) {
            case "done" -> 0x2E7D32;
            case "cancelled" -> 0x8B8B8B;
            case "blocked" -> 0xB03030;
            default -> 0x2E8B7A;
        };
    }

    private static int jobStatusColour(JobRow job) {
        return switch (job.state()) {
            case "done" -> COLOUR_GOOD;
            case "blocked" -> COLOUR_ALERT;
            case "cancelled", "waiting" -> COLOUR_DIM;
            default -> job.status().equals("missing") || job.status().equals("out_of_fuel") || job.status().equals("blocked")
                    ? COLOUR_ALERT : COLOUR_TEXT;
        };
    }

    /** One job: icon, amount and item, progress bar, status line and a cancel button while open; ended jobs fade out. */
    private Line jobLine(JobRow job) {
        Painter painter = (graphics, y, hovered) -> {
            float visible = visibility(job);
            shade(graphics, y, Production.JOB_HEIGHT, hovered && job.open());
            if (visible >= 0.5f) {
                graphics.item(job.item(), MARGIN + Production.JOB_ICON.x(), y + Production.JOB_ICON.y());
            }
            clipped(graphics, job.amount() + " " + job.item().getHoverName().getString(), Production.JOB_TITLE, y, alpha(COLOUR_TEXT, visible));
            Rect barPart = Production.JOB_BAR;
            PanelPainter.bar(graphics, MARGIN + barPart.x(), y + barPart.y(), barPart.w(), barPart.h(), job.crafted(), job.amount(),
                    alpha(jobBarColour(job), visible), alpha(0x373737, visible));
            clipped(graphics, status(job).getString(), Production.JOB_STATUS, y, alpha(jobStatusColour(job), visible));
            if (job.open()) {
                Rect cancel = Production.JOB_CANCEL;
                int cx = MARGIN + cancel.x();
                int cy = y + cancel.y();
                PanelPainter.bordered(graphics, cx + 1, cy + 1, cancel.w() - 2, cancel.h() - 2, PanelPainter.DARK, COLOUR_ALERT);
                graphics.text(font, "x", cx + (cancel.w() - font.width("x")) / 2, cy + 2, 0xFFFFFFFF, false);
            }
        };
        Hit hit = new Hit() {
            @Override
            public List<Component> tooltip(int lx, int ly) {
                if (job.open() && Production.JOB_CANCEL.contains(lx, ly)) {
                    return List.of(Component.translatable("gui.servus_shuttles.production.job.cancel"));
                }
                return List.of(Component.literal(job.amount() + " ").append(job.item().getHoverName()),
                        Component.literal(job.crafted() + " / " + job.amount()), status(job), Component.translatable(origin(job)));
            }

            @Override
            public boolean click(int lx, int ly) {
                if (job.open() && Production.JOB_CANCEL.contains(lx, ly)) {
                    ClientPacketDistributor.sendToServer(new CancelProductionOrderPayload(job.cpu(), job.id()));
                    return true;
                }
                return false;
            }
        };
        return new Line(Production.JOB_HEIGHT, painter, null, null, hit);
    }

    /** Under a job: what waits in the CPU buffer for one later step. */
    private Line bufferLine(BufferLine line) {
        Component text = Component.translatable("gui.servus_shuttles.production.job.buffer", line.machine(), itemList(line.items()));
        return subLine(text, COLOUR_BUFFER, () -> itemTooltip(
                Component.translatable("gui.servus_shuttles.production.job.buffer_for", line.machine()), line.items()));
    }

    /** Under a job: intermediates that went to storage because the buffer was full. */
    private Line overflowLine(List<ItemCount> items) {
        Component text = Component.translatable("gui.servus_shuttles.production.job.overflow", itemList(items));
        return subLine(text, COLOUR_ALERT, () -> itemTooltip(Component.translatable("gui.servus_shuttles.production.job.overflow_head"), items));
    }

    private Line subLine(Component text, int colour, Supplier<List<Component>> tooltip) {
        Painter painter = (graphics, y, hovered) -> clipped(graphics, text.getString(), Production.BUFFER_LINE, y, colour);
        return new Line(Production.BUFFER_LINE_HEIGHT, painter, tooltip, null);
    }

    private static String origin(JobRow job) {
        if (job.requestId() > 0) {
            return "gui.servus_shuttles.production.origin.request";
        }
        return job.requestId() < 0 ? "gui.servus_shuttles.production.origin.rule" : "gui.servus_shuttles.production.origin.manual";
    }

    private List<Line> cpuLines() {
        List<Line> result = new ArrayList<>();
        List<CpuRow> cpus = ClientProduction.cpus();
        if (cpus.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.no_cpu"), COLOUR_DIM));
            return result;
        }
        for (CpuRow cpu : cpus) {
            result.add(cpuLine(cpu));
        }
        return result;
    }

    /** One CPU: icon, position and size, slot and buffer bars, and the buffered items cut short with a rest count. */
    private Line cpuLine(CpuRow cpu) {
        ItemStack icon = new ItemStack(ServusShuttlesBlocks.CRAFTING_CPU.get());
        Component title = Component.translatable("gui.servus_shuttles.production.cpu.title", cpu.cpu().toShortString(),
                cpu.sizeX(), cpu.sizeY(), cpu.sizeZ());
        Painter painter = (graphics, y, hovered) -> {
            shade(graphics, y, Production.CPU_HEIGHT, hovered);
            graphics.item(icon, MARGIN + Production.CPU_ICON.x(), y + Production.CPU_ICON.y());
            clipped(graphics, title.getString(), Production.CPU_TITLE, y, COLOUR_TEXT);
            bar(graphics, Production.CPU_SLOT_BAR, y, cpu.inUse(), cpu.slots(), COLOUR_TEAL);
            if (cpu.room() > 0) {
                bar(graphics, Production.CPU_BUFFER_BAR, y, cpu.used(), cpu.room(), cpu.used() >= cpu.room() ? COLOUR_ALERT : COLOUR_AMBER);
            }
            Component state = cpu.waiting() > 0
                    ? Component.translatable("gui.servus_shuttles.production.cpu.waiting", cpu.waiting())
                    : Component.translatable("gui.servus_shuttles.production.cpu.slots", cpu.inUse(), cpu.slots());
            clipped(graphics, state.getString(), Production.CPU_STATE, y, cpu.waiting() > 0 ? COLOUR_ALERT : COLOUR_DIM);
            paintBufferedItems(graphics, cpu, y);
        };
        return new Line(Production.CPU_HEIGHT, painter, () -> cpuTooltip(cpu, title), null);
    }

    /** The CPU's buffered items in a strip, leaving ones marked, cut short with a rest count. */
    private void paintBufferedItems(GuiGraphicsExtractor graphics, CpuRow cpu, int y) {
        int itemsX = MARGIN + Production.CPU_ITEMS.x();
        int itemsY = y + Production.CPU_ITEMS.y();
        if (cpu.room() <= 0) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.production.cpu.no_buffer"), itemsX, itemsY + 4, COLOUR_DIM, false);
            return;
        }
        List<BufferItem> items = cpu.buffered();
        int shown = Production.cpuItemsShown(items.size());
        for (int i = 0; i < shown; i++) {
            BufferItem item = items.get(i);
            int x = itemsX + i * Production.CPU_ITEM_STEP;
            graphics.item(item.item(), x, itemsY);
            graphics.itemDecorations(font, item.item(), x, itemsY, String.valueOf(item.count()));
            if (item.leaving()) {
                graphics.fill(x, itemsY + 14, x + 16, itemsY + 16, PanelPainter.SLOT);
            }
        }
        if (shown < items.size()) {
            graphics.text(font, "+" + (items.size() - shown), itemsX + shown * Production.CPU_ITEM_STEP + 2, itemsY + 4, COLOUR_DIM, false);
        }
    }

    private static List<Component> cpuTooltip(CpuRow cpu, Component title) {
        List<Component> list = new ArrayList<>();
        list.add(title);
        list.add(Component.translatable("gui.servus_shuttles.production.cpu.blocks", cpu.cpuBlocks(), cpu.bufferBlocks()));
        list.add(cpuLineText(cpu));
        if (cpu.room() <= 0) {
            list.add(Component.translatable("gui.servus_shuttles.production.cpu.no_buffer"));
            return list;
        }
        list.add(Component.translatable("gui.servus_shuttles.production.cpu.buffer", cpu.used(), cpu.room()));
        for (BufferItem item : cpu.buffered()) {
            list.add(Component.translatable(item.leaving() ? "gui.servus_shuttles.production.cpu.item_leaving"
                    : "gui.servus_shuttles.production.cpu.item", item.count(), item.item().getHoverName(), item.order()));
        }
        return list;
    }

    private List<Line> storageLines() {
        List<Line> result = new ArrayList<>();
        List<StorageRow> warehouses = ClientProduction.storage();
        if (warehouses.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.no_warehouse"), COLOUR_DIM));
            return result;
        }
        for (StorageRow warehouse : warehouses) {
            result.add(storageHeader(warehouse));
            if (warehouse.total() <= 0) {
                result.add(storageLine(Component.translatable("gui.servus_shuttles.storage.racks_none").getString(), "", 0, 0,
                        COLOUR_DIM, COLOUR_TEAL));
            }
            for (ZoneRow zone : warehouse.zones()) {
                String name = zone.unzoned() ? Component.translatable("gui.servus_shuttles.storage.no_zone").getString() : zone.name();
                result.add(storageLine(name, slots(zone.used(), zone.total()), zone.used(), zone.total(),
                        zone.full() ? COLOUR_ALERT : COLOUR_TEXT, zone.full() ? COLOUR_ALERT : COLOUR_TEAL));
            }
            if (warehouse.fluidCapacity() > 0) {
                result.add(storageLine(Component.translatable("gui.servus_shuttles.storage.fluid_row").getString(),
                        Component.translatable("gui.servus_shuttles.storage.mb", PickerStock.shortCount(warehouse.fluid()),
                                PickerStock.shortCount(warehouse.fluidCapacity())).getString(), warehouse.fluid(), warehouse.fluidCapacity(),
                        warehouse.fluidFull() ? COLOUR_ALERT : COLOUR_TEXT, warehouse.fluidFull() ? COLOUR_ALERT : COLOUR_FLUID));
            }
        }
        return result;
    }

    private static String slots(long used, long total) {
        return Component.translatable("gui.servus_shuttles.storage.slots", PickerStock.shortCount(used), PickerStock.shortCount(total)).getString();
    }

    /** A warehouse: its name, its slots and a bar; the tooltip counts its rack bays and Storage gantries. */
    private Line storageHeader(StorageRow warehouse) {
        String name = warehouse.name().isEmpty() ? Component.translatable("gui.servus_shuttles.storage.warehouse").getString() : warehouse.name();
        String numbers = warehouse.total() > 0 ? slots(warehouse.used(), warehouse.total()) : "";
        int colour = warehouse.full() ? COLOUR_ALERT : COLOUR_TEAL;
        Painter painter = (graphics, y, hovered) -> {
            shade(graphics, y, Production.STORAGE_HEADER_HEIGHT, hovered);
            clipped(graphics, name, Production.STORAGE_NAME, y + 2, COLOUR_HEAD);
            numbers(graphics, numbers, y + 2, warehouse.full() ? COLOUR_ALERT : COLOUR_HEAD);
            if (warehouse.total() > 0) {
                bar(graphics, Production.STORAGE_BAR, y + 2, warehouse.used(), warehouse.total(), colour);
            }
        };
        return new Line(Production.STORAGE_HEADER_HEIGHT, painter, () -> List.of(Component.literal(name),
                Component.translatable("gui.servus_shuttles.storage.bays", warehouse.bays()),
                Component.translatable("gui.servus_shuttles.storage.gantries", warehouse.gantries())), null);
    }

    /** A zone or the fluids of a warehouse, indented under its header, with numbers and a bar when there is a total. */
    private Line storageLine(String name, String numbers, long used, long total, int textColour, int barColour) {
        Rect namePart = new Rect(Production.STORAGE_NAME.x() + Production.STORAGE_INDENT, Production.STORAGE_NAME.y(),
                Production.STORAGE_NAME.w() - Production.STORAGE_INDENT, Production.STORAGE_NAME.h());
        Painter painter = (graphics, y, hovered) -> {
            clipped(graphics, name, namePart, y, textColour);
            numbers(graphics, numbers, y, textColour);
            if (total > 0) {
                bar(graphics, Production.STORAGE_BAR, y, used, total, barColour);
            }
        };
        return new Line(Production.STORAGE_ROW_HEIGHT, painter, null, null);
    }

    /** Numbers right aligned in the numbers part of a storage row. */
    private void numbers(GuiGraphicsExtractor graphics, String numbers, int lineY, int colour) {
        Rect part = Production.STORAGE_NUMBERS;
        String shown = font.plainSubstrByWidth(numbers, part.w());
        graphics.text(font, shown, MARGIN + part.right() - font.width(shown), lineY + part.y(), colour, false);
    }

    private List<Line> patternLines() {
        List<Line> result = new ArrayList<>();
        PatternsView patterns = ClientProduction.patterns();
        if (patterns.total() == 0) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.no_pattern_storage"), COLOUR_DIM));
            return result;
        }
        Component capacity = Component.translatable("gui.servus_shuttles.production.patterns_capacity", patterns.used(), patterns.total());
        result.add(new Line(14, (graphics, y, hovered) -> {
            graphics.text(font, capacity, MARGIN, y + 3, COLOUR_HEAD, false);
            PanelPainter.bar(graphics, WIDTH - MARGIN - CAPACITY_BAR_WIDTH - 2, y + 3, CAPACITY_BAR_WIDTH, 7, patterns.used(), patterns.total(),
                    patterns.used() >= patterns.total() ? COLOUR_ALERT : COLOUR_TEAL, PanelPainter.DARK);
        }, null, null));
        Component add = Component.translatable("gui.servus_shuttles.production.new_pattern");
        result.add(new Line(16, (graphics, y, hovered) -> RecipeEditorPopup.drawButton(graphics, font, MARGIN, y + 1,
                font.width(add) + 10, 13, add.getString(), hovered),
                () -> List.of(Component.translatable("gui.servus_shuttles.production.new_pattern_hint")),
                () -> press(RecipeEditorButtons.OPEN_NEW)));
        if (patterns.rows().isEmpty()) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.no_patterns"), COLOUR_DIM));
        }
        for (PatternRow row : patterns.rows()) {
            result.add(patternLine(row));
        }
        return result;
    }

    private Line patternLine(PatternRow row) {
        Component text = Component.translatable("gui.servus_shuttles.production.pattern_recipe", row.result().getCount(),
                row.result().getHoverName(), CraftingGrid.stacks(row.inputs()));
        Painter painter = (graphics, y, hovered) -> {
            if (hovered) {
                graphics.fill(MARGIN, y, WIDTH - MARGIN, y + PATTERN_ROW, PATTERN_HOVER_SHADE);
            }
            graphics.item(row.result(), MARGIN + 4, y + 1);
            graphics.text(font, text, MARGIN + 24, y + 5, COLOUR_TEXT, false);
        };
        Supplier<List<Component>> tooltip = () -> List.of(text,
                Component.translatable("gui.servus_shuttles.production.pattern_storage_at", row.storage().toShortString(), row.slot() + 1),
                Component.translatable("gui.servus_shuttles.production.pattern_edit_hint"));
        return new Line(PATTERN_ROW, painter, tooltip, () -> ClientPacketDistributor.sendToServer(new EditPatternPayload(row.storage(), row.slot())));
    }

    private void press(int button) {
        MenuButtons.press(minecraft, menu, button);
    }

    private List<Line> machineLines() {
        List<Line> result = new ArrayList<>();
        List<MachineRow> machines = ClientProduction.machines();
        if (machines.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.no_machines"), COLOUR_DIM));
        }
        for (MachineRow machine : machines) {
            result.add(machineLine(machine));
        }
        List<PackingRow> stations = ClientProduction.packing();
        List<DuctRow> busiest = ClientProduction.busiest();
        if (stations.isEmpty() && busiest.isEmpty()) {
            return result;
        }
        result.add(text(Component.translatable("gui.servus_shuttles.production.data_line"), COLOUR_TEXT));
        if (busiest.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_shuttles.production.line_idle"), COLOUR_DIM));
        } else {
            result.add(ductLine(busiest.get(0)));
        }
        for (PackingRow station : stations) {
            result.add(packingLine(station));
        }
        return result;
    }

    /** "Busiest duct: Data Cable, 30 of 32 items/s" with its position in the tooltip. */
    private Line ductLine(DuctRow duct) {
        Component line = Component.translatable("gui.servus_shuttles.production.busiest_duct", duct.name(), duct.load(), duct.bandwidth());
        int colour = PackingRow.busy(duct.load(), duct.bandwidth()) ? COLOUR_ALERT : COLOUR_DIM;
        Painter painter = (graphics, y, hovered) -> clipped(graphics, line.getString(), MARGIN, y + 1, WIDTH - 2 * MARGIN, colour);
        Supplier<List<Component>> tooltip = () -> List.of(line,
                Component.translatable("gui.servus_shuttles.production.machine.at", duct.pos().toShortString()));
        return new Line(ROW, painter, tooltip, null);
    }

    private static Component packingUse(String direction, int used, int rate) {
        String key = "gui.servus_shuttles.production.packing." + direction + (PackingRow.busy(used, rate) ? "_busy" : "");
        return Component.translatable(key, used, rate);
    }

    /** A machine row's icon, name and coloured state line. */
    private void paintMachineRow(GuiGraphicsExtractor graphics, int y, boolean hovered, ItemStack icon, String name, String state, int colour) {
        shade(graphics, y, Production.MACHINE_HEIGHT, hovered);
        graphics.item(icon, MARGIN + Production.MACHINE_ICON.x(), y + Production.MACHINE_ICON.y());
        clipped(graphics, name, Production.MACHINE_NAME, y, COLOUR_TEXT);
        clipped(graphics, state, Production.MACHINE_STATE, y, colour);
    }

    /** A Packing Station as a machine row: its use of the rate to network and to storage, busy directions marked. */
    private Line packingLine(PackingRow station) {
        ItemStack icon = new ItemStack(ServusShuttlesBlocks.PACKING_STATION.get());
        Component toNetwork = packingUse("to_network", station.toNetwork(), station.rate());
        Component toStorage = packingUse("to_storage", station.toStorage(), station.rate());
        Component state = Component.translatable("gui.servus_shuttles.production.packing.use", toNetwork, toStorage);
        Painter painter = (graphics, y, hovered) -> {
            boolean busy = station.busyToNetwork() || station.busyToStorage();
            boolean working = station.toNetwork() > 0 || station.toStorage() > 0;
            int colour = busy ? COLOUR_ALERT : working ? COLOUR_GOOD : COLOUR_DIM;
            paintMachineRow(graphics, y, hovered, icon, icon.getHoverName().getString(), state.getString(), colour);
        };
        Supplier<List<Component>> tooltip = () -> List.of(icon.getHoverName(), toNetwork, toStorage,
                Component.translatable("gui.servus_shuttles.production.machine.at", station.pos().toShortString()));
        return new Line(Production.MACHINE_HEIGHT, painter, tooltip, null);
    }

    private Component machineState(MachineRow machine) {
        return switch (machine.state()) {
            case "crafting" -> machine.left() > 0
                    ? Component.translatable("gui.servus_shuttles.production.machine.crafting_left", machine.item().getHoverName(), machine.left())
                    : Component.translatable("gui.servus_shuttles.production.machine.crafting", machine.item().getHoverName());
            case "blocked" -> Component.translatable("gui.servus_shuttles.production.machine.blocked", machine.reason());
            default -> Component.translatable("gui.servus_shuttles.production.machine." + machine.state());
        };
    }

    /** A machine with its state and, when it has upkeep, a bar of the upkeep items it holds. */
    private Line machineLine(MachineRow machine) {
        Painter painter = (graphics, y, hovered) -> {
            int colour = machine.problem() ? COLOUR_ALERT : machine.state().equals("crafting") ? COLOUR_GOOD : COLOUR_DIM;
            paintMachineRow(graphics, y, hovered, machine.icon(), machine.name(), machineState(machine).getString(), colour);
            if (machine.upkeepLevel() > 0) {
                bar(graphics, Production.MACHINE_BAR, y, machine.upkeepCount(), machine.upkeepLevel(),
                        machine.upkeepCount() > 0 ? COLOUR_AMBER : COLOUR_ALERT);
            }
        };
        return new Line(Production.MACHINE_HEIGHT, painter, () -> machineTooltip(machine), null);
    }

    private List<Component> machineTooltip(MachineRow machine) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(machine.name()));
        list.add(machineState(machine));
        if (machine.makes() < 0) {
            list.add(Component.translatable("gui.servus_shuttles.production.machine.any_pattern"));
        } else if (machine.makes() > 0) {
            list.add(Component.translatable("gui.servus_shuttles.production.machine.makes", machine.makes()));
        }
        if (machine.upkeepLevel() > 0) {
            list.add(Component.translatable("gui.servus_shuttles.production.upkeep", machine.upkeep().getHoverName(),
                    machine.upkeepCount(), machine.upkeepLevel()));
        }
        list.add(Component.translatable("gui.servus_shuttles.production.machine.at", machine.pos().toShortString()));
        return list;
    }

    /** The top of a line on the panel, with the current scroll. */
    private int lineTop(Line target) {
        int y = LIST_Y - scrolls[selected];
        for (Line line : lines) {
            if (line == target) {
                return y;
            }
            y += line.height();
        }
        return y;
    }

    private Line lineAt(double mouseX, double mouseY) {
        if (!ClientProduction.online()) {
            return null;
        }
        int x = (int) mouseX - leftPos;
        int mouse = (int) mouseY - topPos;
        if (x < MARGIN || x >= WIDTH - MARGIN || mouse < LIST_Y || mouse >= LIST_BOTTOM) {
            return null;
        }
        int y = LIST_Y - scrolls[selected];
        for (Line line : lines) {
            if (mouse >= y && mouse < y + line.height()) {
                return line;
            }
            y += line.height();
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (menu.isEditing()) {
            switch (popup.buttonAt(leftPos, topPos, mouseX, mouseY, menu.editing() == ProductionTerminalMenu.EDIT_EXISTING)) {
                case SAVE -> {
                    press(RecipeEditorButtons.SAVE);
                    return true;
                }
                case DELETE -> {
                    press(RecipeEditorButtons.DELETE);
                    return true;
                }
                case CANCEL -> {
                    press(RecipeEditorButtons.CANCEL);
                    return true;
                }
                case NONE -> {
                }
            }
            return super.mouseClicked(event, doubleClick);
        }
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (y >= TAB_Y && y < TAB_Y + TAB_HEIGHT) {
            List<Rect> rects = tabRects();
            for (int i = 0; i < tabs.size(); i++) {
                if (x >= rects.get(i).x() && x < rects.get(i).right()) {
                    selected = i;
                    return true;
                }
            }
        }
        Line line = lineAt(mouseX, mouseY);
        if (line != null && line.hit() != null) {
            if (line.hit().click(x - MARGIN, y - lineTop(line))) {
                return true;
            }
        } else if (line != null && line.click() != null) {
            line.click().run();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.isEditing()) {
            return true;
        }
        scrolls[selected] = Math.max(0, Math.min(maxScroll(), scrolls[selected] - (int) (scrollY * ROW)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (menu.isEditing() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            press(RecipeEditorButtons.CANCEL);
            return true;
        }
        return super.keyPressed(event);
    }
}
