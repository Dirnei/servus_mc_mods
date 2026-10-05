package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.terminal.PullItemPayload;
import io.github.dirnei.mc.servus.storage.terminal.PushCarriedPayload;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Search box, scrollable stock grid, player inventory. */
public class CounterTerminalScreen extends AbstractContainerScreen<CounterTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/counter_terminal.png");
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 34;
    private static final int CELL = 18;

    private EditBox search;
    private final List<StockEntry> filtered = new ArrayList<>();
    private int filteredVersion = -1;
    private String filteredQuery = "";
    private int scrollRow;

    public CounterTerminalScreen(CounterTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 194;
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
    }

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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!ClientStock.online()) {
            Component reason = Component.translatable("gui.servus_storage.terminal." + ClientStock.reason());
            guiGraphics.drawCenteredString(font, reason, leftPos + imageWidth / 2, topPos + GRID_Y + 30, 0xFFFFFF);
            renderTooltip(guiGraphics, mouseX, mouseY);
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
            guiGraphics.renderItem(entry.stack(), x, y);
            guiGraphics.renderItemDecorations(font, entry.stack(), x, y, abbreviate(entry.count()));
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            int x = leftPos + GRID_X + ((cell - first) % COLUMNS) * CELL + 1;
            int y = topPos + GRID_Y + ((cell - first) / COLUMNS) * CELL + 1;
            if (cell >= first && cell < first + COLUMNS * ROWS) {
                guiGraphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            }
        }
        entryAt(mouseX, mouseY).ifPresentOrElse(entry -> {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.stack()));
            lines.add(Component.translatable("gui.servus_storage.stored", entry.count()));
            guiGraphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        }, () -> renderTooltip(guiGraphics, mouseX, mouseY));
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (cellAt(mouseX, mouseY) >= 0 && ClientStock.online()) {
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                PacketDistributor.sendToServer(PushCarriedPayload.INSTANCE);
                return true;
            }
            Optional<StockEntry> entry = entryAt(mouseX, mouseY);
            if (entry.isPresent()) {
                PacketDistributor.sendToServer(new PullItemPayload(entry.get().stack(), hasShiftDown()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxRow = Math.max(0, (filtered().size() + COLUMNS - 1) / COLUMNS - ROWS);
        scrollRow = Math.max(0, Math.min(maxRow, scrollRow - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
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
