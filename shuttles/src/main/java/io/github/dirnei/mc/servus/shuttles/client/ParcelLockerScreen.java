package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.NamedMachineScreen;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity.BoxView;
import io.github.dirnei.mc.servus.shuttles.menu.ParcelLockerMenu;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Parcel Locker GUI: a map of the ten boxes as the locker stands, the
 * player's own boxes green, other players' boxes as taken, free boxes dark,
 * the head in the middle; below it the items of the player's boxes to take,
 * then the player inventory. Opening it ends the Handheld's parcel note.
 */
public class ParcelLockerScreen extends NamedMachineScreen<ParcelLockerMenu> {

    private static final String KEY = "gui.servus_shuttles.parcel_locker.";
    private static final int MAP_X = 9;
    private static final int COLUMN_W = 29;
    private static final int COLUMN_GAP = 3;
    private static final float LABEL_SCALE = 0.75F;
    private static final int TEXT = 0xFF404040;
    private static final int FREE = 0xFF2B2B2B;
    private static final int MINE = 0xFF3C9A4A;
    private static final int TAKEN = 0xFFB98C0E;
    private static final int HEAD = 0xFF5A5D63;
    private static final int LABEL_LIGHT = 0xFFE8E8E8;
    private static final int LABEL_DARK = 0xFF2A1F00;

    public ParcelLockerScreen(ParcelLockerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, ParcelLockerMenu.WIDTH, ParcelLockerMenu.HEIGHT);
        ClientHandheld.forgetParcel();
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = ParcelLockerMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        LockerLayout.Box box = hoveredBox(mouseX, mouseY);
        if (box != null) {
            BoxView view = menu.views().get(box.index());
            graphics.setTooltipForNextFrame(font, Component.translatable(KEY + "box." + view.name().toLowerCase(Locale.ROOT), box.id()),
                    mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        int headX = leftPos + columnX(LockerLayout.HEAD_COLUMN);
        graphics.fill(headX, topPos + ParcelLockerMenu.MAP_Y, headX + COLUMN_W, topPos + ParcelLockerMenu.MAP_Y + ParcelLockerMenu.MAP_H, HEAD);
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            drawBox(graphics, box, menu.views().get(box.index()));
        }
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, ParcelLockerMenu.GRID_Y, 9 * 18, ParcelLockerMenu.GRID_ROWS * 18), 9,
                menu.boxSlots());
        PanelPainter.playerInventory(graphics, leftPos, topPos, ParcelLockerMenu.INVENTORY_Y, ParcelLockerMenu.INVENTORY_Y + 58);
    }

    /** One box of the map: a cell in its column at its height, coloured by whose it is, with its label. */
    private void drawBox(GuiGraphicsExtractor graphics, LockerLayout.Box box, BoxView view) {
        int x = leftPos + columnX(box.column());
        int top = boxTop(box);
        int bottom = boxBottom(box);
        int colour = switch (view) {
            case FREE -> FREE;
            case MINE -> MINE;
            case TAKEN -> TAKEN;
        };
        graphics.fill(x, top, x + COLUMN_W, bottom - 1, colour);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 2, top + 1);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);
        graphics.text(font, box.id(), 0, 0, view == BoxView.TAKEN ? LABEL_DARK : LABEL_LIGHT, false);
        graphics.pose().popMatrix();
    }

    private static int columnX(int column) {
        return MAP_X + column * (COLUMN_W + COLUMN_GAP);
    }

    private int boxTop(LockerLayout.Box box) {
        return topPos + ParcelLockerMenu.MAP_Y + (2 * LockerLayout.BLOCK - box.top()) * ParcelLockerMenu.MAP_H / (2 * LockerLayout.BLOCK);
    }

    private int boxBottom(LockerLayout.Box box) {
        return topPos + ParcelLockerMenu.MAP_Y + (2 * LockerLayout.BLOCK - box.bottom()) * ParcelLockerMenu.MAP_H / (2 * LockerLayout.BLOCK);
    }

    private LockerLayout.Box hoveredBox(int mouseX, int mouseY) {
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            int x = leftPos + columnX(box.column());
            if (mouseX >= x && mouseX < x + COLUMN_W && mouseY >= boxTop(box) && mouseY < boxBottom(box) - 1) {
                return box;
            }
        }
        return null;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component line = menu.boxSlots() == 0 ? Component.translatable(KEY + "nothing") : Component.translatable(KEY + "yours");
        graphics.text(font, line, 8, ParcelLockerMenu.GRID_Y - 10, TEXT, false);
    }
}
