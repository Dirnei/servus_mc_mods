package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.terminal.CancelRequestPayload;
import io.github.dirnei.mc.servus.storage.terminal.PushCarriedPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestItemPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.terminal.SetStockRulePayload;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Search box, stock grid with an amount prompt, open requests, output slots, player inventory. */
public class CounterTerminalScreen extends AbstractContainerScreen<CounterTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/counter_terminal.png");
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 34;
    private static final int CELL = 18;
    private static final int REQUESTS_Y = 110;
    private static final int REQUEST_ROW = 12;
    private static final int REQUEST_ROWS = 3;
    private static final int LIST_WIDTH = 160;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_ALERT = 0xB03030;

    private EditBox search;
    private final List<StockEntry> filtered = new ArrayList<>();
    private int filteredVersion = -1;
    private String filteredQuery = "";
    private int scrollRow;
    private int requestScroll;

    // Amount prompt
    private ItemStack prompting = ItemStack.EMPTY;
    private boolean ruleMode;
    private EditBox amount;
    private EditBox maximum;
    private final List<Button> requestButtons = new ArrayList<>();
    private final List<Button> ruleButtons = new ArrayList<>();
    private Button modeToggle;
    private Button cancelButton;

    public CounterTerminalScreen(CounterTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        ClientStock.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = CounterTerminalMenu.INVENTORY_Y - 11;
        search = new EditBox(font, leftPos + GRID_X, topPos + 18, 160, 12, Component.translatable("gui.servus_storage.search"));
        search.setMaxLength(50);
        search.setHint(Component.translatable("gui.servus_storage.search"));
        search.setBordered(true);
        search.setResponder(text -> {
            scrollRow = 0;
            filteredVersion = -1;
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        amount = new EditBox(font, leftPos + 60, topPos + GRID_Y + 22, 56, 12, Component.translatable("gui.servus_storage.terminal.amount"));
        amount.setMaxLength(6);
        amount.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        addRenderableWidget(amount);
        maximum = new EditBox(font, leftPos + 120, topPos + GRID_Y + 22, 48, 12, Component.translatable("gui.servus_storage.terminal.rule_max"));
        maximum.setMaxLength(6);
        maximum.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        addRenderableWidget(maximum);
        requestButtons.clear();
        ruleButtons.clear();
        requestButtons.add(addRenderableWidget(Button.builder(Component.literal("+1"), b -> bump(1))
                .bounds(leftPos + 18, topPos + GRID_Y + 38, 44, 14).build()));
        requestButtons.add(addRenderableWidget(Button.builder(Component.literal("+10"), b -> bump(10))
                .bounds(leftPos + 66, topPos + GRID_Y + 38, 44, 14).build()));
        requestButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.plus_stack"), b -> bump(prompting.getMaxStackSize()))
                .bounds(leftPos + 114, topPos + GRID_Y + 38, 44, 14).build()));
        requestButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.request"), b -> confirmPrompt())
                .bounds(leftPos + 18, topPos + GRID_Y + 54, 46, 14).build()));
        ruleButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.rule_save"), b -> saveRule())
                .bounds(leftPos + 18, topPos + GRID_Y + 54, 46, 14).build()));
        ruleButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.rule_remove"), b -> removeRule())
                .bounds(leftPos + 18, topPos + GRID_Y + 38, 92, 14).build()));
        modeToggle = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.rule"), b -> toggleMode())
                .bounds(leftPos + 66, topPos + GRID_Y + 54, 46, 14).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.cancel"), b -> closePrompt())
                .bounds(leftPos + 114, topPos + GRID_Y + 54, 46, 14).build());
        closePrompt();
    }

    // Amount prompt

    private boolean prompting() {
        return !prompting.isEmpty();
    }

    private void openPrompt(ItemStack stack) {
        prompting = stack.copyWithCount(1);
        ruleMode = false;
        amount.setValue(Integer.toString(stack.getMaxStackSize()));
        applyMode();
        setFocused(amount);
        amount.setFocused(true);
        search.setFocused(false);
    }

    private void toggleMode() {
        ruleMode = !ruleMode;
        RuleRow rule = ClientStock.ruleFor(prompting);
        if (ruleMode) {
            amount.setValue(rule == null ? "0" : Integer.toString(rule.min()));
            maximum.setValue(rule == null ? Integer.toString(prompting.getMaxStackSize()) : Integer.toString(rule.max()));
        } else {
            amount.setValue(Integer.toString(prompting.getMaxStackSize()));
        }
        applyMode();
    }

    private void applyMode() {
        boolean open = prompting();
        amount.visible = open;
        maximum.visible = open && ruleMode;
        requestButtons.forEach(button -> button.visible = open && !ruleMode);
        ruleButtons.forEach(button -> button.visible = open && ruleMode);
        modeToggle.visible = open;
        modeToggle.setMessage(Component.translatable(ruleMode ? "gui.servus_storage.terminal.request" : "gui.servus_storage.terminal.rule"));
        cancelButton.visible = open;
    }

    private void closePrompt() {
        prompting = ItemStack.EMPTY;
        ruleMode = false;
        amount.setFocused(false);
        maximum.setFocused(false);
        applyMode();
    }

    private void saveRule() {
        if (!prompting()) {
            return;
        }
        int min = promptAmount();
        int max;
        try {
            max = Math.max(min, Integer.parseInt(maximum.getValue().trim()));
        } catch (NumberFormatException e) {
            max = min;
        }
        PacketDistributor.sendToServer(new SetStockRulePayload(prompting, min, max));
        closePrompt();
    }

    private void removeRule() {
        if (prompting()) {
            PacketDistributor.sendToServer(new SetStockRulePayload(prompting, -1, -1));
        }
        closePrompt();
    }

    private int promptAmount() {
        try {
            return Math.max(0, Integer.parseInt(amount.getValue().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void bump(int by) {
        amount.setValue(Integer.toString(Math.min(99999, promptAmount() + by)));
    }

    private void confirmPrompt() {
        int wanted = promptAmount();
        if (wanted > 0 && prompting()) {
            PacketDistributor.sendToServer(new RequestItemPayload(prompting, wanted));
        }
        closePrompt();
    }

    // Stock grid

    private List<StockEntry> filtered() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        if (filteredVersion != ClientStock.version() || !query.equals(filteredQuery)) {
            filtered.clear();
            for (StockEntry entry : ClientStock.entries()) {
                if (query.isEmpty() || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(entry);
                }
            }
            filteredVersion = ClientStock.version();
            filteredQuery = query;
            int maxRow = Math.max(0, (filtered.size() + COLUMNS - 1) / COLUMNS - ROWS);
            scrollRow = Math.min(scrollRow, maxRow);
        }
        return filtered;
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos - GRID_X;
        int y = (int) mouseY - topPos - GRID_Y;
        if (x < 0 || y < 0 || x >= COLUMNS * CELL || y >= ROWS * CELL) {
            return -1;
        }
        return (y / CELL + scrollRow) * COLUMNS + x / CELL;
    }

    private Optional<StockEntry> entryAt(double mouseX, double mouseY) {
        int cell = cellAt(mouseX, mouseY);
        List<StockEntry> list = filtered();
        return cell >= 0 && cell < list.size() ? Optional.of(list.get(cell)) : Optional.empty();
    }

    private int requestRowAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos - GRID_X;
        int y = (int) mouseY - topPos - REQUESTS_Y;
        if (x < 0 || y < 0 || x >= LIST_WIDTH || y >= REQUEST_ROWS * REQUEST_ROW) {
            return -1;
        }
        int row = y / REQUEST_ROW + requestScroll;
        return row < ClientStock.requests().size() ? row : -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!ClientStock.online()) {
            Component reason = Component.translatable("gui.servus_storage.terminal." + ClientStock.reason());
            graphics.drawCenteredString(font, reason, leftPos + imageWidth / 2, topPos + GRID_Y + 30, 0xFFFFFF);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        if (prompting()) {
            renderPrompt(graphics);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        List<StockEntry> list = filtered();
        int first = scrollRow * COLUMNS;
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            int index = first + i;
            if (index >= list.size()) {
                break;
            }
            StockEntry entry = list.get(index);
            int x = leftPos + GRID_X + (i % COLUMNS) * CELL + 1;
            int y = topPos + GRID_Y + (i / COLUMNS) * CELL + 1;
            graphics.renderItem(entry.stack(), x, y);
            graphics.renderItemDecorations(font, entry.stack(), x, y, abbreviate(entry.count()));
            if (ClientStock.ruleFor(entry.stack()) != null) {
                graphics.fill(x, y, x + 3, y + 3, 0xFF40A0FF);
            }
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= first && cell < first + COLUMNS * ROWS) {
            int x = leftPos + GRID_X + ((cell - first) % COLUMNS) * CELL + 1;
            int y = topPos + GRID_Y + ((cell - first) / COLUMNS) * CELL + 1;
            graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
        renderRequests(graphics, mouseX, mouseY);
        Optional<StockEntry> hovered = entryAt(mouseX, mouseY);
        int requestRow = requestRowAt(mouseX, mouseY);
        if (hovered.isPresent()) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), hovered.get().stack()));
            lines.add(Component.translatable("gui.servus_storage.stored", hovered.get().count()));
            RuleRow rule = ClientStock.ruleFor(hovered.get().stack());
            if (rule != null) {
                lines.add(Component.translatable("gui.servus_storage.terminal.rule_line", rule.min(), rule.max(),
                        Component.translatable("gui.servus_storage.terminal.rule_state." + rule.state())));
            }
            lines.add(Component.translatable("gui.servus_storage.terminal.click_hint"));
            graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        } else if (requestRow >= 0) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.terminal.cancel_hint"), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderPrompt(GuiGraphics graphics) {
        int x = leftPos + GRID_X;
        int y = topPos + GRID_Y;
        graphics.fill(x, y, x + COLUMNS * CELL, y + ROWS * CELL, 0xF0C6C6C6);
        graphics.renderItem(prompting, x + 4, y + 4);
        graphics.drawString(font, prompting.getHoverName(), x + 24, y + 8, COLOUR_TEXT, false);
        if (ruleMode) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.rule_min"), x + 4, y + 24, COLOUR_TEXT, false);
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.rule_max"), x + 112, y + 24, COLOUR_TEXT, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.amount"), x + 4, y + 24, COLOUR_TEXT, false);
        }
    }

    private void renderRequests(GuiGraphics graphics, int mouseX, int mouseY) {
        List<RequestRow> rows = ClientStock.requests();
        int hovered = requestRowAt(mouseX, mouseY);
        if (rows.isEmpty()) {
            String message = ClientStock.message();
            Component text = message.isEmpty()
                    ? Component.translatable("gui.servus_storage.terminal.no_requests")
                    : Component.translatable("gui.servus_storage.terminal.message." + message);
            graphics.drawString(font, text, leftPos + GRID_X + 2, topPos + REQUESTS_Y + 2, message.isEmpty() ? 0x8A8A8A : COLOUR_ALERT, false);
            return;
        }
        for (int i = 0; i < REQUEST_ROWS; i++) {
            int index = requestScroll + i;
            if (index >= rows.size()) {
                break;
            }
            RequestRow row = rows.get(index);
            int y = topPos + REQUESTS_Y + i * REQUEST_ROW;
            if (index == hovered) {
                graphics.fill(leftPos + GRID_X, y, leftPos + GRID_X + LIST_WIDTH, y + REQUEST_ROW, 0x30FFFFFF);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + GRID_X + 1, y + 1, 0);
            graphics.pose().scale(0.625F, 0.625F, 1.0F);
            graphics.renderItem(row.stack(), 0, 0);
            graphics.pose().popPose();
            String progress = row.delivered() + " / " + row.requested();
            graphics.drawString(font, progress, leftPos + GRID_X + 14, y + 2, COLOUR_TEXT, false);
            Component state = row.state().equals("MISSING")
                    ? Component.translatable("gui.servus_storage.terminal.state.missing", row.missing())
                    : Component.translatable("gui.servus_storage.terminal.state." + row.state().toLowerCase(Locale.ROOT));
            int colour = row.state().equals("MISSING") ? COLOUR_ALERT : COLOUR_TEXT;
            graphics.drawString(font, state, leftPos + GRID_X + LIST_WIDTH - 2 - font.width(state), y + 2, colour, false);
        }
        if (!ClientStock.message().isEmpty()) {
            Component text = Component.translatable("gui.servus_storage.terminal.message." + ClientStock.message());
            graphics.drawString(font, text, leftPos + GRID_X, topPos + REQUESTS_Y + REQUEST_ROWS * REQUEST_ROW + 1, COLOUR_ALERT, false);
        }
    }

    static String abbreviate(long count) {
        if (count < 1000) {
            return Long.toString(count);
        }
        if (count < 10000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        if (count < 1000000) {
            return (count / 1000) + "k";
        }
        return String.format(Locale.ROOT, "%.1fM", count / 1000000.0);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.requests"), GRID_X, REQUESTS_Y - 9, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.output"), GRID_X, CounterTerminalMenu.OUTPUT_Y - 10, COLOUR_TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (prompting()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (ClientStock.online()) {
            if (cellAt(mouseX, mouseY) >= 0) {
                ItemStack carried = menu.getCarried();
                if (!carried.isEmpty()) {
                    PacketDistributor.sendToServer(PushCarriedPayload.INSTANCE);
                    return true;
                }
                Optional<StockEntry> entry = entryAt(mouseX, mouseY);
                if (entry.isPresent()) {
                    if (hasShiftDown()) {
                        PacketDistributor.sendToServer(new RequestItemPayload(entry.get().stack(), entry.get().stack().getMaxStackSize()));
                    } else {
                        openPrompt(entry.get().stack());
                    }
                    return true;
                }
            }
            int row = requestRowAt(mouseX, mouseY);
            if (row >= 0) {
                PacketDistributor.sendToServer(new CancelRequestPayload(ClientStock.requests().get(row).id()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= topPos + REQUESTS_Y && mouseY < topPos + REQUESTS_Y + REQUEST_ROWS * REQUEST_ROW) {
            int maxRow = Math.max(0, ClientStock.requests().size() - REQUEST_ROWS);
            requestScroll = Math.max(0, Math.min(maxRow, requestScroll - (int) Math.signum(scrollY)));
            return true;
        }
        int maxRow = Math.max(0, (filtered().size() + COLUMNS - 1) / COLUMNS - ROWS);
        scrollRow = Math.max(0, Math.min(maxRow, scrollRow - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (prompting()) {
                closePrompt();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (prompting()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (ruleMode) {
                    saveRule();
                } else {
                    confirmPrompt();
                }
                return true;
            }
            if (maximum.isFocused() && (maximum.keyPressed(keyCode, scanCode, modifiers) || maximum.canConsumeInput())) {
                return true;
            }
            if (amount.keyPressed(keyCode, scanCode, modifiers) || amount.canConsumeInput()) {
                return true;
            }
            return true;
        }
        if (search.isFocused() && (search.keyPressed(keyCode, scanCode, modifiers) || search.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        ClientStock.clear();
    }
}
