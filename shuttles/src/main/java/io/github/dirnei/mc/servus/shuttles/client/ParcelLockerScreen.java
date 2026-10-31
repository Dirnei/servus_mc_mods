package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.NamedMachineScreen;
import io.github.dirnei.mc.servus.shuttles.locker.BoxInfo;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity.BoxView;
import io.github.dirnei.mc.servus.shuttles.menu.ParcelLockerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Parcel Locker GUI: a map of the ten boxes as the locker stands, the
 * player's own boxes green, other players' boxes as taken, free boxes dark,
 * the head in the middle. Each taken box names who ordered it; hovering a box
 * says how far its order is. Opening it ends the Handheld's parcel note.
 */
public class ParcelLockerScreen extends NamedMachineScreen<ParcelLockerMenu> {

    private static final int MAP_X = 9;
    private static final int COLUMN_W = 29;
    private static final int COLUMN_GAP = 3;
    private static final float LABEL_SCALE = 0.6F;
    private static final int LABEL_LINE = 6;
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
        inventoryLabelY = -1000;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        LockerLayout.Box box = hoveredBox(mouseX, mouseY);
        if (box != null) {
            graphics.setTooltipForNextFrame(font, describe(box), mouseX, mouseY);
        }
    }

    private Component describe(LockerLayout.Box box) {
        return menu.boxes().get(box.index()).describe(box);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        int headX = leftPos + columnX(LockerLayout.HEAD_COLUMN);
        graphics.fill(headX, topPos + ParcelLockerMenu.MAP_Y, headX + COLUMN_W, topPos + ParcelLockerMenu.MAP_Y + ParcelLockerMenu.MAP_H, HEAD);
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            drawBox(graphics, box, menu.boxes().get(box.index()));
        }
    }

    /** One box of the map: a cell in its column at its height, coloured by whose it is, with its id and orderer. */
    private void drawBox(GuiGraphicsExtractor graphics, LockerLayout.Box box, BoxInfo info) {
        int x = leftPos + columnX(box.column());
        int top = boxTop(box);
        int bottom = boxBottom(box);
        int colour = switch (info.view()) {
            case FREE -> FREE;
            case MINE -> MINE;
            case TAKEN -> TAKEN;
        };
        graphics.fill(x, top, x + COLUMN_W, bottom - 1, colour);
        int label = info.view() == BoxView.TAKEN ? LABEL_DARK : LABEL_LIGHT;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 2, top + 2);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);
        graphics.text(font, box.id(), 0, 0, label, false);
        if (!info.owner().isEmpty()) {
            String owner = font.plainSubstrByWidth(info.owner(), (int) ((COLUMN_W - 3) / LABEL_SCALE));
            graphics.text(font, owner, 0, (int) (LABEL_LINE / LABEL_SCALE), label, false);
        }
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
}
