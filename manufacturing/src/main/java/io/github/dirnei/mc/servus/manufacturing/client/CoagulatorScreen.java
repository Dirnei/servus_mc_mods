package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The Coagulator: slots, gauges, and a scrolled list of every coagulation recipe to pick from on the right. */
public class CoagulatorScreen extends MachineScreen {

    public static final Layout LAYOUT = new Layout(false, true, true, MachineScreens.gui("coagulator"));

    private static final int LIST_GAP = 4;
    private static final int LIST_WIDTH = 24;
    private static final int ROW = 20;
    private static final int VISIBLE = 7;
    private static final int SELECTED = 0xFF5FA05F;
    private static final int FRAME = 0xFF373737;
    private static final int BACK = 0xFFC6C6C6;

    private final PrincipleGauges gauges;
    private int scroll;

    public CoagulatorScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, LAYOUT);
        this.gauges = new PrincipleGauges((PrincipleMenu) menu);
    }

    private List<RecipeHolder<CoagulationRecipe>> recipes() {
        return minecraft == null || minecraft.level == null ? List.of()
                : CoagulatorBlockEntity.sortedRecipes(minecraft.level.getRecipeManager());
    }

    private int listX() {
        return leftPos + imageWidth + LIST_GAP;
    }

    private int listY() {
        return topPos + 4;
    }

    @Override
    protected void init() {
        super.init();
        gauges.place(leftPos, topPos);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        gauges.render(guiGraphics);
        List<RecipeHolder<CoagulationRecipe>> recipes = recipes();
        int x = listX();
        int y = listY();
        guiGraphics.fill(x - 1, y - 1, x + LIST_WIDTH + 1, y + VISIBLE * ROW + 1, FRAME);
        guiGraphics.fill(x, y, x + LIST_WIDTH, y + VISIBLE * ROW, BACK);
        int selected = ((CoagulatorMenu) menu).selectedIndex();
        for (int row = 0; row < VISIBLE && scroll + row < recipes.size(); row++) {
            int index = scroll + row;
            int rowY = y + row * ROW;
            if (index == selected) {
                guiGraphics.fill(x, rowY, x + LIST_WIDTH, rowY + ROW, SELECTED);
            }
            guiGraphics.renderItem(recipes.get(index).value().result(), x + 4, rowY + 2);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
        int index = indexAt(mouseX, mouseY);
        List<RecipeHolder<CoagulationRecipe>> recipes = recipes();
        if (index >= 0 && index < recipes.size()) {
            CoagulationRecipe recipe = recipes.get(index).value();
            List<Component> lines = new ArrayList<>();
            lines.add(recipe.result().getHoverName());
            for (Principle principle : Principle.values()) {
                int amount = recipe.input().get(principle);
                if (amount > 0) {
                    lines.add(Component.translatable("gui.servus_manufacturing.coagulator.needs", amount,
                            Component.translatable("fluid_type.servus_manufacturing." + principle.id())));
                }
            }
            recipe.ingredient().ifPresent(sized -> {
                if (sized.ingredient().getItems().length > 0) {
                    lines.add(Component.literal(sized.count() + " x ").append(sized.ingredient().getItems()[0].getHoverName()));
                }
            });
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private int indexAt(double mouseX, double mouseY) {
        int x = listX();
        int y = listY();
        if (mouseX < x || mouseX >= x + LIST_WIDTH || mouseY < y || mouseY >= y + VISIBLE * ROW) {
            return -1;
        }
        return scroll + (int) ((mouseY - y) / ROW);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = indexAt(mouseX, mouseY);
        if (index >= 0 && index < recipes().size() && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CoagulatorMenu.RECIPE_BUTTON_FIRST + index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (indexAt(mouseX, mouseY) >= 0 || (mouseX >= listX() && mouseX < listX() + LIST_WIDTH)) {
            int max = Math.max(0, recipes().size() - VISIBLE);
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
