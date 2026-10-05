package io.github.dirnei.mc.servus.logistics.client;

import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.duct.Channel;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.network.RemoveChannelPayload;
import io.github.dirnei.mc.servus.logistics.network.SetDuctModePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Per side and per channel mode buttons for one utility duct. */
public class UtilityDuctScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int LABEL_WIDTH = 50;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 6;

    private final BlockPos pos;
    private final List<ModeButton> buttons = new ArrayList<>();
    private List<Channel> channels = List.of();

    public UtilityDuctScreen(BlockPos pos) {
        super(Component.translatable("gui.servus_logistics.utility_duct"));
        this.pos = pos;
    }

    private UtilityDuctBlockEntity duct() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            return duct;
        }
        return null;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        UtilityDuctBlockEntity duct = duct();
        channels = duct == null ? List.of() : new ArrayList<>(duct.channels());
        int columns = Math.max(1, channels.size());
        int width = LABEL_WIDTH + columns * (BUTTON_WIDTH + GAP);
        int left = (this.width - width) / 2;
        int top = (this.height - (Direction.values().length + 2) * ROW_HEIGHT) / 2 + ROW_HEIGHT;
        for (int c = 0; c < channels.size(); c++) {
            Channel channel = channels.get(c);
            int x = left + LABEL_WIDTH + c * (BUTTON_WIDTH + GAP);
            addRenderableWidget(Button.builder(Component.translatable("gui.servus_logistics.remove"),
                            button -> {
                                PacketDistributor.sendToServer(new RemoveChannelPayload(pos, channel.ordinal()));
                                onClose();
                            })
                    .bounds(x, top, BUTTON_WIDTH, 20).build());
            for (Direction direction : Direction.values()) {
                int y = top + (direction.ordinal() + 1) * ROW_HEIGHT;
                ModeButton button = new ModeButton(x, y, channel, direction);
                buttons.add(button);
                addRenderableWidget(button);
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + (width - 60) / 2, top + (Direction.values().length + 1) * ROW_HEIGHT + 4, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int columns = Math.max(1, channels.size());
        int width = LABEL_WIDTH + columns * (BUTTON_WIDTH + GAP);
        int left = (this.width - width) / 2;
        int top = (this.height - (Direction.values().length + 2) * ROW_HEIGHT) / 2 + ROW_HEIGHT;
        guiGraphics.drawCenteredString(font, title, this.width / 2, top - 16, 0xFFFFFF);
        for (int c = 0; c < channels.size(); c++) {
            int x = left + LABEL_WIDTH + c * (BUTTON_WIDTH + GAP);
            guiGraphics.drawCenteredString(font, Component.translatable("gui.servus_logistics.channel." + channels.get(c).id()),
                    x + BUTTON_WIDTH / 2, top - 4 - 10, 0xA0A0A0);
        }
        for (Direction direction : Direction.values()) {
            int y = top + (direction.ordinal() + 1) * ROW_HEIGHT + 6;
            guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.side." + direction.getSerializedName()),
                    left, y, 0xFFFFFF, false);
        }
        if (channels.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.servus_logistics.no_channels"),
                    this.width / 2, top + ROW_HEIGHT, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Shows the current mode and cycles it on click. */
    private final class ModeButton extends Button {

        private final Channel channel;
        private final Direction direction;

        private ModeButton(int x, int y, Channel channel, Direction direction) {
            super(x, y, BUTTON_WIDTH, 20, Component.empty(), button -> {
            }, DEFAULT_NARRATION);
            this.channel = channel;
            this.direction = direction;
        }

        private SideMode mode() {
            UtilityDuctBlockEntity duct = duct();
            return duct == null ? SideMode.BOTH : duct.mode(channel, direction);
        }

        @Override
        public void onPress() {
            SideMode next = UtilityDuctBlockEntity.cycle(mode());
            UtilityDuctBlockEntity duct = duct();
            if (duct != null) {
                duct.setMode(channel, direction, next);
            }
            PacketDistributor.sendToServer(new SetDuctModePayload(pos, channel.ordinal(), direction.ordinal(), next.ordinal()));
        }

        @Override
        public Component getMessage() {
            return Component.translatable("gui.servus_logistics.mode." + mode().name().toLowerCase(Locale.ROOT));
        }
    }
}
