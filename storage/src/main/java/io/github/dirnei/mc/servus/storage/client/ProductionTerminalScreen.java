package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Production;
import io.github.dirnei.mc.servus.storage.pattern.CraftingGrid;
import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.client.RecipeEditorPopup;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.production.CancelProductionOrderPayload;
import io.github.dirnei.mc.servus.storage.production.EditPatternPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.JobRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.DuctRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PackingRow;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

/**
 * Title, a tab row (Crafting, Patterns, Machines) and a scrollable list below. Each tab turns the last
 * synced rows into lines; a line can draw itself, show a tooltip and react to
 * a click. More tabs are more entries in {@link #tabs}. The Patterns tab opens
 * the shared recipe editor popup over the panel, with the player inventory
 * below it while it is open.
 */
public class ProductionTerminalScreen extends AbstractContainerScreen<ProductionTerminalMenu> {

    private static final int WIDTH = Production.WIDTH;
    private static final int HEIGHT = Production.HEIGHT;
    private static final int MARGIN = Production.LIST.x();
    private static final int TAB_Y = Production.TABS.y();
    private static final int TAB_HEIGHT = Production.TABS.h();
    private static final int TAB_WIDTH = Production.TAB_WIDTH;
    private static final int LIST_Y = Production.LIST.y();
    private static final int LIST_BOTTOM = Production.LIST.bottom();
    private static final int ORDER_ROW = 18;
    private static final int ROW = 10;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_DIM = 0xFF707070;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int COLOUR_GOOD = 0xFF2E7D32;
    private static final int COLOUR_HEAD = 0xFF1F4E5A;

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
            new Tab("machines", this::machineLines));
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

    @Override
    protected void init() {
        super.init();
        // Every show, also after another screen was in front, registers the terminal again.
        ClientProduction.open(menu.terminal().getBlockPos());
        this.inventoryLabelY = Integer.MIN_VALUE;
    }

    // Drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + MARGIN, topPos + LIST_Y - 2, leftPos + WIDTH - MARGIN, topPos + LIST_Y - 1, 0xFF8B8B8B);
        if (menu.isEditing()) {
            boolean existing = menu.editing() == ProductionTerminalMenu.EDIT_EXISTING;
            Component title = Component.translatable(existing ? "gui.servus_storage.production.edit_pattern"
                    : "gui.servus_storage.production.new_pattern_title");
            popup.render(graphics, font, leftPos, topPos, mouseX, mouseY, title, existing, preview(),
                    Component.translatable("gui.servus_storage.crafting_station.save"),
                    Component.translatable("gui.servus_storage.crafting_station.delete"),
                    Component.translatable("gui.servus_storage.crafting_station.cancel"));
            for (int row = 0; row < 4; row++) {
                int y = topPos + (row < 3 ? ProductionTerminalMenu.INVENTORY_Y + row * 18 : ProductionTerminalMenu.HOTBAR_Y) - 1;
                for (int column = 0; column < 9; column++) {
                    int x = leftPos + ProductionTerminalMenu.INVENTORY_X + column * 18 - 1;
                    graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
                    graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
                }
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
                    graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_storage.crafting_station.no_match"), mouseX, mouseY);
                } else {
                    graphics.setTooltipForNextFrame(font, preview, mouseX, mouseY);
                }
            } else {
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

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.isEditing()) {
            return;
        }
        graphics.text(font, title, Production.TITLE.x(), Production.TITLE.y(), COLOUR_TEXT, false);
        renderTabs(graphics);
        if (!ClientProduction.loaded()) {
            graphics.text(font, Component.translatable("gui.servus_storage.production.loading"), MARGIN, LIST_Y, COLOUR_DIM, false);
            return;
        }
        if (!ClientProduction.online()) {
            String reason = ClientProduction.reason().isEmpty() ? "no_line" : ClientProduction.reason();
            graphics.text(font, Component.translatable("gui.servus_storage.production." + reason), MARGIN, LIST_Y, COLOUR_ALERT, false);
            return;
        }
        rebuildLines();
        Line hovered = lineAt(mouseX, mouseY);
        // Labels draw in window coordinates and the scissor follows the pose, so no leftPos or topPos here.
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

    private void renderTabs(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < tabs.size(); i++) {
            int x = tabX(i);
            boolean active = i == selected;
            graphics.fill(x, TAB_Y, x + TAB_WIDTH, TAB_Y + TAB_HEIGHT, active ? 0xFF8B8B8B : 0xFFC6C6C6);
            graphics.outline(x, TAB_Y, TAB_WIDTH, TAB_HEIGHT, 0xFF373737);
            Component label = Component.translatable("gui.servus_storage.production.tab." + tabs.get(i).key());
            graphics.text(font, label, x + (TAB_WIDTH - font.width(label)) / 2, TAB_Y + 2, active ? 0xFFFFFFFF : COLOUR_TEXT, false);
        }
    }

    /** Tabs sit left aligned on their own row below the title. */
    private int tabX(int index) {
        return Production.tabX(index);
    }

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

    // Crafting tab

    /** Ended jobs fade out over this many ticks after they end. */
    private static final int FADE_TICKS = 100;

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
            result.add(text(Component.translatable("gui.servus_storage.production.no_cpu"), COLOUR_DIM, 0));
            return result;
        }
        int slots = 0;
        int inUse = 0;
        for (CpuRow cpu : cpus) {
            slots += cpu.slots();
            inUse += cpu.inUse();
        }
        Component head = cpus.size() == 1
                ? Component.translatable("gui.servus_storage.production.cpus_header_one", inUse, slots)
                : Component.translatable("gui.servus_storage.production.cpus_header", cpus.size(), inUse, slots);
        Supplier<List<Component>> headTip = () -> {
            List<Component> list = new ArrayList<>();
            for (CpuRow cpu : cpus) {
                list.add(Component.translatable("gui.servus_storage.production.cpu_line", cpu.cpu().toShortString(), cpu.inUse(),
                        cpu.slots(), cpu.waiting()));
            }
            return list;
        };
        result.add(new Line(Production.HEADER_HEIGHT,
                (graphics, y, hovered) -> graphics.text(font, head, MARGIN, y + 2, COLOUR_HEAD, false), headTip, null));
        int shown = 0;
        for (JobRow job : ClientProduction.jobs()) {
            if (visibility(job) <= 0f) {
                continue;
            }
            result.add(jobLine(job));
            shown++;
        }
        if (shown == 0) {
            result.add(text(Component.translatable("gui.servus_storage.production.no_jobs"), COLOUR_DIM, 0));
        }
        return result;
    }

    private Component status(JobRow job) {
        List<String> args = job.args();
        if (job.status().equals("working") && args.size() == 4) {
            String verbKey = "gui.servus_storage.production.verb." + args.get(1);
            Component verb = I18n.exists(verbKey) ? Component.translatable(verbKey) : Component.literal(args.get(1));
            return Component.translatable("gui.servus_storage.production.job.working", args.get(0), verb, args.get(2), args.get(3));
        }
        return Component.translatable("gui.servus_storage.production.job." + job.status(), args.toArray());
    }

    private static int alpha(int rgb, float visibility) {
        return ((int) (Math.max(0.1f, visibility) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private Line jobLine(JobRow job) {
        Painter painter = (graphics, y, hovered) -> {
            float visible = visibility(job);
            if (hovered && job.open()) {
                graphics.fill(MARGIN, y, WIDTH - MARGIN, y + Production.JOB_HEIGHT, 0x20000000);
            }
            if (visible >= 0.5f) {
                graphics.item(job.item(), MARGIN + Production.JOB_ICON.x(), y + Production.JOB_ICON.y());
            }
            String title = job.amount() + " " + job.item().getHoverName().getString();
            graphics.text(font, font.plainSubstrByWidth(title, Production.JOB_TITLE.w()), MARGIN + Production.JOB_TITLE.x(),
                    y + Production.JOB_TITLE.y(), alpha(COLOUR_TEXT, visible), false);
            int barX = MARGIN + Production.JOB_BAR.x();
            int barY = y + Production.JOB_BAR.y();
            int barW = Production.JOB_BAR.w();
            graphics.fill(barX, barY, barX + barW, barY + Production.JOB_BAR.h(), alpha(0x373737, visible));
            int filled = job.amount() <= 0 ? 0 : Math.min(barW - 2, job.crafted() * (barW - 2) / job.amount());
            int barColour = switch (job.state()) {
                case "done" -> 0x2E7D32;
                case "cancelled" -> 0x8B8B8B;
                case "blocked" -> 0xB03030;
                default -> 0x2E8B7A;
            };
            graphics.fill(barX + 1, barY + 1, barX + 1 + filled, barY + Production.JOB_BAR.h() - 1, alpha(barColour, visible));
            int statusColour = switch (job.state()) {
                case "done" -> COLOUR_GOOD;
                case "blocked" -> COLOUR_ALERT;
                case "cancelled", "waiting" -> COLOUR_DIM;
                default -> job.status().equals("missing") || job.status().equals("out_of_fuel") || job.status().equals("blocked")
                        ? COLOUR_ALERT : COLOUR_TEXT;
            };
            String text = status(job).getString();
            graphics.text(font, font.plainSubstrByWidth(text, Production.JOB_STATUS.w()), MARGIN + Production.JOB_STATUS.x(),
                    y + Production.JOB_STATUS.y(), alpha(statusColour, visible), false);
            if (job.open()) {
                int cx = MARGIN + Production.JOB_CANCEL.x();
                int cy = y + Production.JOB_CANCEL.y();
                graphics.fill(cx, cy, cx + Production.JOB_CANCEL.w(), cy + Production.JOB_CANCEL.h(), 0xFF373737);
                graphics.fill(cx + 1, cy + 1, cx + Production.JOB_CANCEL.w() - 1, cy + Production.JOB_CANCEL.h() - 1, 0xFFB03030);
                graphics.text(font, "x", cx + (Production.JOB_CANCEL.w() - font.width("x")) / 2, cy + 2, 0xFFFFFFFF, false);
            }
        };
        Hit hit = new Hit() {
            @Override
            public List<Component> tooltip(int lx, int ly) {
                if (job.open() && Production.JOB_CANCEL.contains(lx, ly)) {
                    return List.of(Component.translatable("gui.servus_storage.production.job.cancel"));
                }
                List<Component> list = new ArrayList<>();
                list.add(Component.literal(job.amount() + " ").append(job.item().getHoverName()));
                list.add(Component.literal(job.crafted() + " / " + job.amount()));
                list.add(status(job));
                list.add(Component.translatable(origin(job)));
                return list;
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

    private static String origin(JobRow job) {
        if (job.requestId() > 0) {
            return "gui.servus_storage.production.origin.request";
        }
        return job.requestId() < 0 ? "gui.servus_storage.production.origin.rule" : "gui.servus_storage.production.origin.manual";
    }

    // Patterns tab

    private List<Line> patternLines() {
        List<Line> result = new ArrayList<>();
        PatternsView patterns = ClientProduction.patterns();
        if (patterns.total() == 0) {
            result.add(text(Component.translatable("gui.servus_storage.production.no_pattern_storage"), COLOUR_DIM, 0));
            return result;
        }
        Component capacity = Component.translatable("gui.servus_storage.production.patterns_capacity", patterns.used(), patterns.total());
        int barWidth = 80;
        result.add(new Line(14, (graphics, y, hovered) -> {
            graphics.text(font, capacity, MARGIN, y + 3, COLOUR_HEAD, false);
            int x = WIDTH - MARGIN - barWidth - 2;
            graphics.fill(x, y + 3, x + barWidth, y + 10, 0xFF373737);
            int filled = patterns.total() == 0 ? 0 : Math.min(barWidth - 2, patterns.used() * (barWidth - 2) / patterns.total());
            int colour = patterns.used() >= patterns.total() ? 0xFFB03030 : 0xFF2E8B7A;
            graphics.fill(x + 1, y + 4, x + 1 + filled, y + 9, colour);
        }, null, null));
        Component add = Component.translatable("gui.servus_storage.production.new_pattern");
        result.add(new Line(16, (graphics, y, hovered) -> RecipeEditorPopup.drawButton(graphics, font, MARGIN, y + 1,
                font.width(add) + 10, 13, add.getString(), hovered),
                () -> List.of(Component.translatable("gui.servus_storage.production.new_pattern_hint")),
                () -> press(RecipeEditorButtons.OPEN_NEW)));
        if (patterns.rows().isEmpty()) {
            result.add(text(Component.translatable("gui.servus_storage.production.no_patterns"), COLOUR_DIM, 0));
        }
        for (PatternRow row : patterns.rows()) {
            result.add(patternLine(row));
        }
        return result;
    }

    private Line patternLine(PatternRow row) {
        Component text = Component.translatable("gui.servus_storage.production.pattern_recipe", row.result().getCount(),
                row.result().getHoverName(), CraftingGrid.stacks(row.inputs()));
        Painter painter = (graphics, y, hovered) -> {
            if (hovered) {
                graphics.fill(MARGIN, y, WIDTH - MARGIN, y + ORDER_ROW, 0x30000000);
            }
            graphics.item(row.result(), MARGIN + 4, y + 1);
            graphics.text(font, text, MARGIN + 24, y + 5, COLOUR_TEXT, false);
        };
        Supplier<List<Component>> tooltip = () -> List.of(text,
                Component.translatable("gui.servus_storage.production.pattern_storage_at", row.storage().toShortString(), row.slot() + 1),
                Component.translatable("gui.servus_storage.production.pattern_edit_hint"));
        return new Line(ORDER_ROW, painter, tooltip, () -> ClientPacketDistributor.sendToServer(new EditPatternPayload(row.storage(), row.slot())));
    }

    private void press(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    // Machines tab

    private List<Line> machineLines() {
        List<Line> result = new ArrayList<>();
        List<MachineRow> machines = ClientProduction.machines();
        if (machines.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_storage.production.no_machines"), COLOUR_DIM, 0));
        }
        for (MachineRow machine : machines) {
            result.add(machineLine(machine));
        }
        List<PackingRow> stations = ClientProduction.packing();
        List<DuctRow> busiest = ClientProduction.busiest();
        if (stations.isEmpty() && busiest.isEmpty()) {
            return result;
        }
        result.add(text(Component.translatable("gui.servus_storage.production.data_line"), COLOUR_TEXT, 0));
        if (busiest.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_storage.production.line_idle"), COLOUR_DIM, 0));
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
        Component line = Component.translatable("gui.servus_storage.production.busiest_duct", duct.name(), duct.load(), duct.bandwidth());
        int colour = PackingRow.busy(duct.load(), duct.bandwidth()) ? COLOUR_ALERT : COLOUR_DIM;
        Painter painter = (graphics, y, hovered) -> graphics.text(font,
                font.plainSubstrByWidth(line.getString(), WIDTH - 2 * MARGIN), MARGIN, y + 1, colour, false);
        Supplier<List<Component>> tooltip = () -> List.of(line,
                Component.translatable("gui.servus_storage.production.machine.at", duct.pos().toShortString()));
        return new Line(ROW, painter, tooltip, null);
    }

    private static Component packingUse(String direction, int used, int rate) {
        String key = "gui.servus_storage.production.packing." + direction + (PackingRow.busy(used, rate) ? "_busy" : "");
        return Component.translatable(key, used, rate);
    }

    /** A Packing Station as a machine row: its use of the rate to network and to storage, busy directions marked. */
    private Line packingLine(PackingRow station) {
        ItemStack icon = new ItemStack(ServusStorageBlocks.PACKING_STATION.get());
        Component toNetwork = packingUse("to_network", station.toNetwork(), station.rate());
        Component toStorage = packingUse("to_storage", station.toStorage(), station.rate());
        Component state = Component.translatable("gui.servus_storage.production.packing.use", toNetwork, toStorage);
        Painter painter = (graphics, y, hovered) -> {
            if (hovered) {
                graphics.fill(MARGIN, y, WIDTH - MARGIN, y + Production.MACHINE_HEIGHT, 0x20000000);
            }
            graphics.item(icon, MARGIN + Production.MACHINE_ICON.x(), y + Production.MACHINE_ICON.y());
            graphics.text(font, font.plainSubstrByWidth(icon.getHoverName().getString(), Production.MACHINE_NAME.w()),
                    MARGIN + Production.MACHINE_NAME.x(), y + Production.MACHINE_NAME.y(), COLOUR_TEXT, false);
            boolean busy = station.busyToNetwork() || station.busyToStorage();
            boolean working = station.toNetwork() > 0 || station.toStorage() > 0;
            int colour = busy ? COLOUR_ALERT : working ? COLOUR_GOOD : COLOUR_DIM;
            graphics.text(font, font.plainSubstrByWidth(state.getString(), Production.MACHINE_STATE.w()),
                    MARGIN + Production.MACHINE_STATE.x(), y + Production.MACHINE_STATE.y(), colour, false);
        };
        Supplier<List<Component>> tooltip = () -> List.of(icon.getHoverName(), toNetwork, toStorage,
                Component.translatable("gui.servus_storage.production.machine.at", station.pos().toShortString()));
        return new Line(Production.MACHINE_HEIGHT, painter, tooltip, null);
    }

    private Component machineState(MachineRow machine) {
        return switch (machine.state()) {
            case "crafting" -> machine.left() > 0
                    ? Component.translatable("gui.servus_storage.production.machine.crafting_left", machine.item().getHoverName(), machine.left())
                    : Component.translatable("gui.servus_storage.production.machine.crafting", machine.item().getHoverName());
            case "blocked" -> Component.translatable("gui.servus_storage.production.machine.blocked", machine.reason());
            default -> Component.translatable("gui.servus_storage.production.machine." + machine.state());
        };
    }

    private Line machineLine(MachineRow machine) {
        Painter painter = (graphics, y, hovered) -> {
            if (hovered) {
                graphics.fill(MARGIN, y, WIDTH - MARGIN, y + Production.MACHINE_HEIGHT, 0x20000000);
            }
            graphics.item(machine.icon(), MARGIN + Production.MACHINE_ICON.x(), y + Production.MACHINE_ICON.y());
            graphics.text(font, font.plainSubstrByWidth(machine.name(), Production.MACHINE_NAME.w()),
                    MARGIN + Production.MACHINE_NAME.x(), y + Production.MACHINE_NAME.y(), COLOUR_TEXT, false);
            int colour = machine.problem() ? COLOUR_ALERT : machine.state().equals("crafting") ? COLOUR_GOOD : COLOUR_DIM;
            graphics.text(font, font.plainSubstrByWidth(machineState(machine).getString(), Production.MACHINE_STATE.w()),
                    MARGIN + Production.MACHINE_STATE.x(), y + Production.MACHINE_STATE.y(), colour, false);
            if (machine.upkeepLevel() > 0) {
                int barX = MARGIN + Production.MACHINE_BAR.x();
                int barY = y + Production.MACHINE_BAR.y();
                int barW = Production.MACHINE_BAR.w();
                graphics.fill(barX, barY, barX + barW, barY + Production.MACHINE_BAR.h(), 0xFF373737);
                int filled = Math.min(barW - 2, machine.upkeepCount() * (barW - 2) / machine.upkeepLevel());
                graphics.fill(barX + 1, barY + 1, barX + 1 + filled, barY + Production.MACHINE_BAR.h() - 1,
                        machine.upkeepCount() > 0 ? 0xFFE0A030 : 0xFFB03030);
            }
        };
        Supplier<List<Component>> tooltip = () -> {
            List<Component> list = new ArrayList<>();
            list.add(Component.literal(machine.name()));
            list.add(machineState(machine));
            if (machine.makes() < 0) {
                list.add(Component.translatable("gui.servus_storage.production.machine.any_pattern"));
            } else if (machine.makes() > 0) {
                list.add(Component.translatable("gui.servus_storage.production.machine.makes", machine.makes()));
            }
            if (machine.upkeepLevel() > 0) {
                list.add(Component.translatable("gui.servus_storage.production.upkeep", machine.upkeep().getHoverName(),
                        machine.upkeepCount(), machine.upkeepLevel()));
            }
            list.add(Component.translatable("gui.servus_storage.production.machine.at", machine.pos().toShortString()));
            return list;
        };
        return new Line(Production.MACHINE_HEIGHT, painter, tooltip, null);
    }

    private Line text(Component text, int colour, int indent) {
        return new Line(ROW, (graphics, y, hovered) -> graphics.text(font, text, MARGIN + indent, y + 1, colour, false), null, null);
    }

    // Input

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
        int yy = (int) mouseY - topPos;
        if (x < MARGIN || x >= WIDTH - MARGIN || yy < LIST_Y || yy >= LIST_BOTTOM) {
            return null;
        }
        int y = LIST_Y - scrolls[selected];
        for (Line line : lines) {
            if (yy >= y && yy < y + line.height()) {
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
        int button = event.button();
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
            for (int i = 0; i < tabs.size(); i++) {
                if (x >= tabX(i) && x < tabX(i) + TAB_WIDTH) {
                    selected = i;
                    return true;
                }
            }
        }
        Line line = lineAt(mouseX, mouseY);
        if (line != null && line.hit() != null) {
            if (line.hit().click((int) mouseX - leftPos - MARGIN, (int) mouseY - topPos - lineTop(line))) {
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
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (menu.isEditing() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            press(RecipeEditorButtons.CANCEL);
            return true;
        }
        return super.keyPressed(event);
    }
}
