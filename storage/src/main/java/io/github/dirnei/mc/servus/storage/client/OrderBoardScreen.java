package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.board.CancelOrderPayload;
import io.github.dirnei.mc.servus.storage.board.MachineRow;
import io.github.dirnei.mc.servus.storage.board.OrderRow;
import io.github.dirnei.mc.servus.storage.board.StepRow;
import io.github.dirnei.mc.servus.storage.menu.OrderBoardMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Machines at the top, a scrollable list of orders and their steps below. Click an open order to cancel it. */
public class OrderBoardScreen extends AbstractContainerScreen<OrderBoardMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/order_board.png");
    private static final int WIDTH = 240;
    private static final int HEIGHT = 200;
    private static final int MARGIN = 8;
    private static final int MACHINES_Y = 18;
    private static final int MACHINE_ROW = 11;
    private static final int MACHINE_ROWS = 4;
    private static final int ORDERS_LABEL_Y = MACHINES_Y + 10 + MACHINE_ROWS * MACHINE_ROW + 4;
    private static final int LIST_Y = ORDERS_LABEL_Y + 11;
    private static final int LIST_BOTTOM = HEIGHT - 8;
    private static final int ORDER_ROW = 18;
    private static final int STEP_ROW = 10;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DIM = 0x707070;
    private static final int COLOUR_ALERT = 0xB03030;
    private static final int COLOUR_GOOD = 0x2E7D32;

    /** One drawn line of the orders list. */
    private record Line(OrderRow order, StepRow step, int height) {
    }

    private final List<Line> lines = new ArrayList<>();
    private int linesVersion = -1;
    private int scroll;

    public OrderBoardScreen(OrderBoardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        ClientOrders.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = Integer.MIN_VALUE;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Line hovered = lineAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            if (hovered.step() != null) {
                tooltip.add(Component.literal(hovered.step().machine() + " @ " + hovered.step().dock().toShortString()));
                if (!hovered.step().reason().isEmpty()) {
                    tooltip.add(Component.literal(hovered.step().reason()));
                }
            } else {
                OrderRow order = hovered.order();
                tooltip.add(Component.literal("#" + order.id() + " " + order.amount() + " x " + order.item().getHoverName().getString()));
                tooltip.add(Component.translatable(origin(order)));
                if (!order.reason().isEmpty()) {
                    tooltip.add(Component.literal(order.reason()));
                }
                if (order.open()) {
                    tooltip.add(Component.translatable("gui.servus_storage.board.cancel_hint"));
                }
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
        MachineRow machine = machineAt(mouseX, mouseY);
        if (machine != null) {
            graphics.renderTooltip(font, Component.literal(machine.machine() + " @ " + machine.dock().toShortString()), mouseX, mouseY);
        }
    }

    private static String origin(OrderRow order) {
        if (order.requestId() > 0) {
            return "gui.servus_storage.board.origin.request";
        }
        return order.requestId() < 0 ? "gui.servus_storage.board.origin.rule" : "gui.servus_storage.board.origin.manual";
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, COLOUR_TEXT, false);
        if (!ClientOrders.online()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal." + (ClientOrders.reason().isEmpty() ? "not_connected" : ClientOrders.reason())),
                    MARGIN, MACHINES_Y, COLOUR_ALERT, false);
            return;
        }
        renderMachines(graphics);
        graphics.drawString(font, Component.translatable("gui.servus_storage.board.orders"), MARGIN, ORDERS_LABEL_Y, COLOUR_TEXT, false);
        renderOrders(graphics, mouseX, mouseY);
    }

    private void renderMachines(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gui.servus_storage.board.machines"), MARGIN, MACHINES_Y, COLOUR_TEXT, false);
        List<MachineRow> machines = ClientOrders.machines();
        int y = MACHINES_Y + 10;
        if (machines.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.board.no_machines"), MARGIN + 4, y, COLOUR_DIM, false);
            return;
        }
        for (int i = 0; i < machines.size() && i < MACHINE_ROWS; i++) {
            MachineRow machine = machines.get(i);
            Component state = switch (machine.state()) {
                case "working" -> Component.translatable("gui.servus_storage.board.machine_state.working", machine.working().getHoverName());
                default -> Component.translatable("gui.servus_storage.board.machine_state." + machine.state());
            };
            int colour = machine.state().equals("working") ? COLOUR_GOOD : machine.state().equals("idle") ? COLOUR_TEXT : COLOUR_DIM;
            graphics.drawString(font, Component.literal(machine.machine() + ": ").append(state), MARGIN + 4, y + i * MACHINE_ROW, colour, false);
        }
        if (machines.size() > MACHINE_ROWS) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.board.more", machines.size() - MACHINE_ROWS),
                    MARGIN + 4, y + MACHINE_ROWS * MACHINE_ROW, COLOUR_DIM, false);
        }
    }

    private void rebuildLines() {
        if (linesVersion == ClientOrders.version()) {
            return;
        }
        linesVersion = ClientOrders.version();
        lines.clear();
        for (OrderRow order : ClientOrders.orders()) {
            lines.add(new Line(order, null, ORDER_ROW));
            for (StepRow step : order.steps()) {
                lines.add(new Line(order, step, STEP_ROW));
            }
        }
        scroll = Math.min(scroll, maxScroll());
    }

    private int totalHeight() {
        int total = 0;
        for (Line line : lines) {
            total += line.height();
        }
        return total;
    }

    private int maxScroll() {
        return Math.max(0, totalHeight() - (LIST_BOTTOM - LIST_Y));
    }

    private void renderOrders(GuiGraphics graphics, int mouseX, int mouseY) {
        rebuildLines();
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.board.no_orders"), MARGIN + 4, LIST_Y, COLOUR_DIM, false);
            return;
        }
        graphics.enableScissor(leftPos + MARGIN, topPos + LIST_Y, leftPos + WIDTH - MARGIN, topPos + LIST_BOTTOM);
        Line hovered = lineAt(mouseX, mouseY);
        int y = LIST_Y - scroll;
        for (Line line : lines) {
            if (y + line.height() > LIST_Y && y < LIST_BOTTOM) {
                if (line.step() == null) {
                    renderOrderLine(graphics, line.order(), y, hovered != null && hovered.order() == line.order() && hovered.step() == null);
                } else {
                    renderStepLine(graphics, line.step(), y);
                }
            }
            y += line.height();
        }
        graphics.disableScissor();
    }

    private void renderOrderLine(GuiGraphics graphics, OrderRow order, int y, boolean hovered) {
        if (hovered && order.open()) {
            graphics.fill(MARGIN, y, WIDTH - MARGIN, y + ORDER_ROW, 0x30000000);
        }
        graphics.renderItem(order.item(), MARGIN + 2, y + 1);
        Component head = Component.literal("#" + order.id() + "  " + order.amount() + " x ").append(order.item().getHoverName());
        graphics.drawString(font, head, MARGIN + 22, y + 1, COLOUR_TEXT, false);
        Component state = Component.translatable("gui.servus_storage.board.order_state." + order.state());
        int colour = switch (order.state()) {
            case "running" -> COLOUR_GOOD;
            case "blocked" -> COLOUR_ALERT;
            default -> COLOUR_DIM;
        };
        String tail = order.reason().isEmpty() ? "" : ", " + order.reason();
        graphics.drawString(font, state.copy().append(tail), MARGIN + 22, y + 10, colour, false);
        Component origin = Component.translatable(origin(order));
        int originWidth = font.width(origin);
        graphics.drawString(font, origin, WIDTH - MARGIN - originWidth - 2, y + 1, COLOUR_DIM, false);
    }

    private void renderStepLine(GuiGraphics graphics, StepRow step, int y) {
        ItemStack output = step.output();
        Component text = Component.literal(step.machine() + ": ")
                .append(output.isEmpty() ? Component.literal("?") : output.getHoverName())
                .append(" x" + step.batches() + ", ")
                .append(Component.translatable("gui.servus_storage.board.step_state." + step.state()))
                .append(step.reason().isEmpty() ? "" : ", " + step.reason());
        int colour = switch (step.state()) {
            case "producing" -> COLOUR_GOOD;
            case "done" -> COLOUR_DIM;
            default -> step.reason().isEmpty() ? COLOUR_TEXT : COLOUR_ALERT;
        };
        graphics.drawString(font, text, MARGIN + 24, y + 1, colour, false);
    }

    private Line lineAt(double mouseX, double mouseY) {
        if (!ClientOrders.online()) {
            return null;
        }
        int x = (int) mouseX - leftPos;
        int yy = (int) mouseY - topPos;
        if (x < MARGIN || x >= WIDTH - MARGIN || yy < LIST_Y || yy >= LIST_BOTTOM) {
            return null;
        }
        int y = LIST_Y - scroll;
        for (Line line : lines) {
            if (yy >= y && yy < y + line.height()) {
                return line;
            }
            y += line.height();
        }
        return null;
    }

    private MachineRow machineAt(double mouseX, double mouseY) {
        if (!ClientOrders.online()) {
            return null;
        }
        int x = (int) mouseX - leftPos;
        int yy = (int) mouseY - topPos - (MACHINES_Y + 10);
        if (x < MARGIN || x >= WIDTH - MARGIN || yy < 0) {
            return null;
        }
        int row = yy / MACHINE_ROW;
        List<MachineRow> machines = ClientOrders.machines();
        return row < MACHINE_ROWS && row < machines.size() ? machines.get(row) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Line line = lineAt(mouseX, mouseY);
        if (line != null && line.step() == null && line.order().open()) {
            PacketDistributor.sendToServer(new CancelOrderPayload(line.order().id()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (scrollY * STEP_ROW)));
        return true;
    }
}
