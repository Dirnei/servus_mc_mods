package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.pattern.CraftingGrid;
import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.client.RecipeEditorPopup;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.pattern.PatternItem;
import io.github.dirnei.mc.servus.storage.production.CancelProductionOrderPayload;
import io.github.dirnei.mc.servus.storage.production.EditPatternPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.OrderRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.RecipeRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.StepRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Tabs on the title row, a scrollable list below. Each tab turns the last
 * synced rows into lines; a line can draw itself, show a tooltip and react to
 * a click. More tabs are more entries in {@link #tabs}. The Patterns tab opens
 * the shared recipe editor popup over the panel, with the player inventory
 * below it while it is open.
 */
public class ProductionTerminalScreen extends AbstractContainerScreen<ProductionTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/production_terminal.png");
    private static final int WIDTH = 240;
    private static final int HEIGHT = 200;
    private static final int MARGIN = 8;
    private static final int TAB_Y = 4;
    private static final int TAB_HEIGHT = 12;
    private static final int TAB_WIDTH = 56;
    private static final int LIST_Y = 22;
    private static final int LIST_BOTTOM = HEIGHT - 8;
    private static final int ORDER_ROW = 18;
    private static final int ROW = 10;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DIM = 0x707070;
    private static final int COLOUR_ALERT = 0xB03030;
    private static final int COLOUR_GOOD = 0x2E7D32;
    private static final int COLOUR_HEAD = 0x1F4E5A;

    /** Draws one line at its top edge. */
    private interface Painter {
        void paint(GuiGraphics graphics, int y, boolean hovered);
    }

    /** One drawn line: its height, how it looks, its tooltip and what a click does (null for nothing). */
    private record Line(int height, Painter painter, Supplier<List<Component>> tooltip, Runnable click) {
    }

    /** One tab: its title key and how it lists the synced rows. */
    private record Tab(String key, Supplier<List<Line>> lines) {
    }

    private final List<Tab> tabs = List.of(
            new Tab("orders", this::orderLines),
            new Tab("machines", this::machineLines),
            new Tab("patterns", this::patternLines));
    private final int[] scrolls = new int[tabs.size()];
    private final List<Line> lines = new ArrayList<>();
    private final RecipeEditorPopup popup = new RecipeEditorPopup(ProductionTerminalMenu.POPUP_X, ProductionTerminalMenu.POPUP_Y, WIDTH, HEIGHT);
    private int selected;
    private int linesVersion = -1;
    private int linesTab = -1;

    public ProductionTerminalScreen(ProductionTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        ClientProduction.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = Integer.MIN_VALUE;
    }

    // Drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
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
                .map(holder -> holder.value().getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.isEditing()) {
            if (popup.previewHovered(leftPos, topPos, mouseX, mouseY)) {
                ItemStack preview = preview();
                if (preview.isEmpty()) {
                    graphics.renderTooltip(font, Component.translatable("gui.servus_storage.crafting_station.no_match"), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(font, preview, mouseX, mouseY);
                }
            } else {
                renderTooltip(graphics, mouseX, mouseY);
            }
            return;
        }
        Line hovered = lineAt(mouseX, mouseY);
        if (hovered != null && hovered.tooltip() != null) {
            List<Component> tooltip = hovered.tooltip().get();
            if (!tooltip.isEmpty()) {
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.isEditing()) {
            return;
        }
        graphics.drawString(font, title, titleLabelX, titleLabelY, COLOUR_TEXT, false);
        renderTabs(graphics);
        if (!ClientProduction.online()) {
            String reason = ClientProduction.reason().isEmpty() ? "no_line" : ClientProduction.reason();
            graphics.drawString(font, Component.translatable("gui.servus_storage.production." + reason), MARGIN, LIST_Y, COLOUR_ALERT, false);
            return;
        }
        rebuildLines();
        Line hovered = lineAt(mouseX, mouseY);
        graphics.enableScissor(leftPos + MARGIN, topPos + LIST_Y, leftPos + WIDTH - MARGIN, topPos + LIST_BOTTOM);
        int y = LIST_Y - scrolls[selected];
        for (Line line : lines) {
            if (y + line.height() > LIST_Y && y < LIST_BOTTOM) {
                line.painter().paint(graphics, y, line == hovered);
            }
            y += line.height();
        }
        graphics.disableScissor();
    }

    private void renderTabs(GuiGraphics graphics) {
        for (int i = 0; i < tabs.size(); i++) {
            int x = tabX(i);
            boolean active = i == selected;
            graphics.fill(x, TAB_Y, x + TAB_WIDTH, TAB_Y + TAB_HEIGHT, active ? 0xFF8B8B8B : 0xFFC6C6C6);
            graphics.renderOutline(x, TAB_Y, TAB_WIDTH, TAB_HEIGHT, 0xFF373737);
            Component label = Component.translatable("gui.servus_storage.production.tab." + tabs.get(i).key());
            graphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2, TAB_Y + 2, active ? 0xFFFFFF : COLOUR_TEXT, false);
        }
    }

    /** Tabs sit right aligned on the title row, the first tab leftmost. */
    private int tabX(int index) {
        return WIDTH - MARGIN - (tabs.size() - index) * (TAB_WIDTH + 2);
    }

    private void rebuildLines() {
        if (linesVersion == ClientProduction.version() && linesTab == selected) {
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

    // Orders tab

    private List<Line> orderLines() {
        List<Line> result = new ArrayList<>();
        List<CpuRow> cpus = ClientProduction.cpus();
        if (cpus.isEmpty()) {
            result.add(text(Component.translatable("gui.servus_storage.production.no_cpu"), COLOUR_DIM, 0));
            return result;
        }
        for (CpuRow cpu : cpus) {
            Component head = Component.translatable("gui.servus_storage.production.cpu", cpu.cpu().toShortString(),
                    cpu.inUse(), cpu.slots(), cpu.waiting());
            result.add(new Line(12, (graphics, y, hovered) -> graphics.drawString(font, head, MARGIN, y + 2, COLOUR_HEAD, false),
                    null, null));
            if (cpu.orders().isEmpty()) {
                result.add(text(Component.translatable("gui.servus_storage.production.no_orders"), COLOUR_DIM, 6));
            }
            for (OrderRow order : cpu.orders()) {
                result.add(orderLine(cpu, order));
                for (StepRow step : order.steps()) {
                    result.add(stepLine(step));
                }
            }
        }
        return result;
    }

    private Line orderLine(CpuRow cpu, OrderRow order) {
        Painter painter = (graphics, y, hovered) -> {
            if (hovered && order.open()) {
                graphics.fill(MARGIN, y, WIDTH - MARGIN, y + ORDER_ROW, 0x30000000);
            }
            graphics.renderItem(order.item(), MARGIN + 4, y + 1);
            Component head = Component.literal("#" + order.id() + "  " + order.amount() + " x ").append(order.item().getHoverName());
            graphics.drawString(font, head, MARGIN + 24, y + 1, COLOUR_TEXT, false);
            int colour = switch (order.state()) {
                case "running" -> COLOUR_GOOD;
                case "blocked" -> COLOUR_ALERT;
                default -> COLOUR_DIM;
            };
            MutableComponent state = Component.translatable("gui.servus_storage.production.order_state." + order.state());
            graphics.drawString(font, state.append(order.reason().isEmpty() ? "" : ", " + order.reason()), MARGIN + 24, y + 10, colour, false);
            Component origin = Component.translatable(origin(order));
            graphics.drawString(font, origin, WIDTH - MARGIN - font.width(origin) - 2, y + 1, COLOUR_DIM, false);
        };
        Supplier<List<Component>> tooltip = () -> {
            List<Component> list = new ArrayList<>();
            list.add(Component.literal("#" + order.id() + " " + order.amount() + " x " + order.item().getHoverName().getString()));
            list.add(Component.translatable(origin(order)));
            if (!order.reason().isEmpty()) {
                list.add(Component.literal(order.reason()));
            }
            if (order.open()) {
                list.add(Component.translatable("gui.servus_storage.production.cancel_hint"));
            }
            return list;
        };
        Runnable click = order.open() ? () -> PacketDistributor.sendToServer(new CancelProductionOrderPayload(cpu.cpu(), order.id())) : null;
        return new Line(ORDER_ROW, painter, tooltip, click);
    }

    private Line stepLine(StepRow step) {
        Component text = Component.literal(step.machine() + ": ")
                .append(step.output().isEmpty() ? Component.literal("?") : step.output().getHoverName())
                .append(" x" + step.batches() + ", ")
                .append(Component.translatable("gui.servus_storage.production.step_state." + step.state()))
                .append(step.reason().isEmpty() ? "" : ", " + step.reason());
        int colour = switch (step.state()) {
            case "producing" -> COLOUR_GOOD;
            case "done" -> COLOUR_DIM;
            default -> step.reason().isEmpty() ? COLOUR_TEXT : COLOUR_ALERT;
        };
        Supplier<List<Component>> tooltip = () -> {
            List<Component> list = new ArrayList<>();
            list.add(Component.literal(step.machine() + " @ " + step.producer().toShortString()));
            if (!step.reason().isEmpty()) {
                list.add(Component.literal(step.reason()));
            }
            return list;
        };
        return new Line(ROW, (graphics, y, hovered) -> graphics.drawString(font, text, MARGIN + 26, y + 1, colour, false), tooltip, null);
    }

    private static String origin(OrderRow order) {
        if (order.requestId() > 0) {
            return "gui.servus_storage.production.origin.request";
        }
        return order.requestId() < 0 ? "gui.servus_storage.production.origin.rule" : "gui.servus_storage.production.origin.manual";
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
            graphics.drawString(font, capacity, MARGIN, y + 3, COLOUR_HEAD, false);
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
        Component text = Component.translatable("item.servus_storage.pattern.recipe", row.result().getCount(),
                row.result().getHoverName(), PatternItem.stacks(row.inputs()));
        Painter painter = (graphics, y, hovered) -> {
            if (hovered) {
                graphics.fill(MARGIN, y, WIDTH - MARGIN, y + ORDER_ROW, 0x30000000);
            }
            graphics.renderItem(row.result(), MARGIN + 4, y + 1);
            graphics.drawString(font, text, MARGIN + 24, y + 5, COLOUR_TEXT, false);
        };
        Supplier<List<Component>> tooltip = () -> List.of(text,
                Component.translatable("gui.servus_storage.production.pattern_storage_at", row.storage().toShortString(), row.slot() + 1),
                Component.translatable("gui.servus_storage.production.pattern_edit_hint"));
        return new Line(ORDER_ROW, painter, tooltip, () -> PacketDistributor.sendToServer(new EditPatternPayload(row.storage(), row.slot())));
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
            return result;
        }
        for (MachineRow machine : machines) {
            Component state = switch (machine.state()) {
                case "working" -> Component.translatable("gui.servus_storage.production.machine_state.working", machine.working().getHoverName());
                case "station_crafting" -> Component.translatable("gui.servus_storage.production.machine_state.station_crafting",
                        machine.working().getHoverName(), machine.left());
                default -> Component.translatable("gui.servus_storage.production.machine_state." + machine.state());
            };
            boolean busy = machine.state().equals("working") || machine.state().equals("station_crafting");
            boolean idle = machine.state().equals("idle") || machine.state().equals("station_idle");
            int colour = busy ? COLOUR_GOOD : idle ? COLOUR_TEXT : COLOUR_DIM;
            Component head = Component.literal(machine.name() + ": ").append(state);
            result.add(new Line(12, (graphics, y, hovered) -> graphics.drawString(font, head, MARGIN, y + 2, colour, false),
                    () -> List.of(Component.literal(machine.name() + " @ " + machine.pos().toShortString())), null));
            if (machine.state().startsWith("station_")) {
                result.add(text(Component.translatable("gui.servus_storage.production.any_pattern"), COLOUR_DIM, 8));
            }
            for (RecipeRow recipe : machine.recipes()) {
                result.add(text(recipe(recipe), COLOUR_TEXT, 8));
            }
        }
        return result;
    }

    private static Component recipe(RecipeRow recipe) {
        return Component.translatable("gui.servus_storage.production.recipe", stacks(recipe.outputs()), stacks(recipe.inputs()),
                String.format(Locale.ROOT, "%.1f", recipe.ticks() / 20.0));
    }

    private static Component stacks(List<ItemStack> stacks) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(stacks.get(i).getCount() + " ").append(stacks.get(i).getHoverName());
        }
        return text;
    }

    private Line text(Component text, int colour, int indent) {
        return new Line(ROW, (graphics, y, hovered) -> graphics.drawString(font, text, MARGIN + indent, y + 1, colour, false), null, null);
    }

    // Input

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
            return super.mouseClicked(mouseX, mouseY, button);
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
        if (line != null && line.click() != null) {
            line.click().run();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (menu.isEditing() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            press(RecipeEditorButtons.CANCEL);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
