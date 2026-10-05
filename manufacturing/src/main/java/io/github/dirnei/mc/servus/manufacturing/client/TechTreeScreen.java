package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.network.SelectResearchPayload;
import io.github.dirnei.mc.servus.manufacturing.research.ClientResearch;
import io.github.dirnei.mc.servus.manufacturing.research.TechDefinition;
import io.github.dirnei.mc.servus.manufacturing.research.Techs;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Minimal tech tree: a list of techs with status. Click an available one to research it. */
public class TechTreeScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int TOP = 32;
    private static final int WIDTH = 300;

    private final Screen parent;
    private List<Techs.Entry> techs = List.of();

    public TechTreeScreen(Screen parent) {
        super(Component.translatable("gui.servus_manufacturing.tech_tree"));
        this.parent = parent;
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    @Override
    protected void init() {
        super.init();
        techs = minecraft != null && minecraft.level != null ? Techs.all(minecraft.level.registryAccess()) : List.of();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20)
                .build());
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < left() || mouseX >= left() + WIDTH || mouseY < TOP) {
            return -1;
        }
        int row = (int) ((mouseY - TOP) / ROW_HEIGHT);
        return row >= 0 && row < techs.size() ? row : -1;
    }

    private Component status(Techs.Entry entry) {
        if (ClientResearch.isCompleted(entry.id())) {
            return Component.translatable("gui.servus_manufacturing.research.completed");
        }
        if (ClientResearch.current().filter(entry.id()::equals).isPresent()) {
            int total = entry.definition().cost().stream().mapToInt(ItemStack::getCount).sum();
            return Component.translatable("gui.servus_manufacturing.research.researching", ClientResearch.totalConsumed(), total);
        }
        if (ClientResearch.isAvailable(entry)) {
            return Component.translatable("gui.servus_manufacturing.research.available");
        }
        return Component.translatable("gui.servus_manufacturing.research.locked");
    }

    private static Component costText(TechDefinition definition) {
        StringBuilder text = new StringBuilder();
        for (ItemStack stack : definition.cost()) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(stack.getCount()).append("x ").append(stack.getHoverName().getString());
        }
        return Component.literal(text.toString());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int hovered = rowAt(mouseX, mouseY);
        for (int i = 0; i < techs.size(); i++) {
            Techs.Entry entry = techs.get(i);
            int x = left();
            int y = TOP + i * ROW_HEIGHT;
            boolean available = ClientResearch.isAvailable(entry);
            int background = i == hovered && available ? 0xA0FFFFFF : 0x80000000;
            guiGraphics.fill(x, y, x + WIDTH, y + ROW_HEIGHT - 2, background);
            guiGraphics.renderItem(entry.definition().icon(), x + 2, y + 2);
            guiGraphics.drawString(font, entry.name(), x + 24, y + 2, 0xFFFFFF, false);
            guiGraphics.drawString(font, costText(entry.definition()), x + 24, y + 12, 0xA0A0A0, false);
            Component status = status(entry);
            guiGraphics.drawString(font, status, x + WIDTH - 4 - font.width(status), y + 7, 0xFFD080, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            Techs.Entry entry = techs.get(row);
            if (ClientResearch.isAvailable(entry)) {
                PacketDistributor.sendToServer(new SelectResearchPayload(entry.id()));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
