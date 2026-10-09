package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.client.RecipePickerPopup;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The Coagulator: slots, gauges, and a select button that opens a popup of every coagulation recipe. */
public class CoagulatorScreen extends MachineScreen {

    public static final Layout LAYOUT = new Layout(false, true, true, MachineScreens.gui("coagulator"));

    private static final int BUTTON_X = 48;
    private static final int BUTTON_Y = 55;
    private static final int BUTTON_W = 100;
    private static final int BUTTON_H = 15;
    private static final int KEY_ESCAPE = 256;

    private final PrincipleGauges gauges;
    private final RecipePickerPopup popup = new RecipePickerPopup();
    private boolean popupOpen;

    public CoagulatorScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, LAYOUT);
        this.gauges = new PrincipleGauges((PrincipleMenu) menu);
    }

    private List<RecipeHolder<CoagulationRecipe>> recipes() {
        return minecraft == null || minecraft.level == null ? List.of()
                : CoagulatorBlockEntity.sortedRecipes(minecraft.level.getRecipeManager());
    }

    private int selected() {
        return ((CoagulatorMenu) menu).selectedIndex();
    }

    private boolean buttonHovered(double mouseX, double mouseY) {
        return mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + BUTTON_W
                && mouseY >= topPos + BUTTON_Y && mouseY < topPos + BUTTON_Y + BUTTON_H;
    }

    @Override
    protected void init() {
        super.init();
        gauges.place(leftPos, topPos);
        popupOpen = false;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        gauges.render(guiGraphics);
        List<RecipeHolder<CoagulationRecipe>> recipes = recipes();
        int index = selected();
        boolean hovered = !popupOpen && buttonHovered(mouseX, mouseY);
        int bx = leftPos + BUTTON_X;
        int by = topPos + BUTTON_Y;
        guiGraphics.fill(bx, by, bx + BUTTON_W, by + BUTTON_H, 0xFF373737);
        guiGraphics.fill(bx + 1, by + 1, bx + BUTTON_W - 1, by + BUTTON_H - 1, hovered ? 0xFFDDDDDD : 0xFFA0A0A0);
        Component label;
        int textX = bx + 4;
        if (index >= 0 && index < recipes.size()) {
            ItemStack result = recipes.get(index).value().result();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(bx + 2, by + 1.5F, 0);
            guiGraphics.pose().scale(0.75F, 0.75F, 1);
            guiGraphics.renderItem(result, 0, 0);
            guiGraphics.pose().popPose();
            label = result.getHoverName();
            textX = bx + 17;
        } else {
            label = Component.translatable("gui.servus_manufacturing.coagulator.select");
        }
        String text = font.plainSubstrByWidth(label.getString(), bx + BUTTON_W - 10 - textX);
        guiGraphics.drawString(font, text, textX, by + 4, 0xFF202020, false);
        guiGraphics.drawString(font, "v", bx + BUTTON_W - 9, by + 4, 0xFF202020, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // While the popup is open nothing below it reacts to the mouse.
        super.render(guiGraphics, popupOpen ? -1 : mouseX, popupOpen ? -1 : mouseY, partialTick);
        List<RecipeHolder<CoagulationRecipe>> recipes = recipes();
        if (!popupOpen) {
            gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
            int index = selected();
            if (buttonHovered(mouseX, mouseY) && index >= 0 && index < recipes.size()) {
                guiGraphics.renderComponentTooltip(font, tooltip(recipes.get(index).value()), mouseX, mouseY);
            }
            return;
        }
        List<ItemStack> results = recipes.stream().map(holder -> holder.value().result()).toList();
        popup.render(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight,
                Component.translatable("gui.servus_manufacturing.coagulator.pick"), results, selected(), mouseX, mouseY);
        int index = popup.indexAt(mouseX, mouseY, results.size());
        if (index >= 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 400);
            guiGraphics.renderComponentTooltip(font, tooltip(recipes.get(index).value()), mouseX, mouseY);
            guiGraphics.pose().popPose();
        }
    }

    private List<Component> tooltip(CoagulationRecipe recipe) {
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
        recipe.chapter().ifPresent(chapter -> lines.add(Component.translatable("gui.servus_manufacturing.coagulator.chapter",
                Component.translatable("journal." + chapter.getNamespace() + "." + chapter.getPath())).withStyle(ChatFormatting.GRAY)));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (popupOpen) {
            int count = recipes().size();
            int index = popup.indexAt(mouseX, mouseY, count);
            if (index >= 0 && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CoagulatorMenu.RECIPE_BUTTON_FIRST + index);
                popupOpen = false;
            } else if (!popup.clickBar(mouseX, mouseY, count) && !popup.contains(mouseX, mouseY)) {
                popupOpen = false;
            }
            return true;
        }
        if (buttonHovered(mouseX, mouseY)) {
            popup.open(leftPos, topPos, imageWidth, imageHeight, recipes().size(), selected());
            popupOpen = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (popupOpen) {
            popup.drag(mouseY, recipes().size());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (popupOpen) {
            popup.release();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (popupOpen) {
            popup.scroll(scrollY, recipes().size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (popupOpen) {
            if (keyCode == KEY_ESCAPE) {
                popupOpen = false;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
