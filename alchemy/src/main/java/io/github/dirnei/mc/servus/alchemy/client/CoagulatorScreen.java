package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.CoagulatorMenu;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.client.RecipePickerPopup;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The Coagulator: slots, gauges, and a select button that opens a popup of every coagulation recipe. */
public class CoagulatorScreen extends MachineScreen {

    /** No side panel: the Coagulator's ports are fixed by its build. */
    public static final Layout LAYOUT = new Layout(false, true, false, false, false, MachineScreens.gui("coagulator"));

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
                : CoagulatorBlockEntity.sortedRecipes(minecraft.level);
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
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
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
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(bx + 2, by + 1.5F);
            guiGraphics.pose().scale(0.75F, 0.75F);
            guiGraphics.item(result, 0, 0);
            guiGraphics.pose().popMatrix();
            label = result.getHoverName();
            textX = bx + 17;
        } else {
            label = Component.translatable("gui.servus_alchemy.coagulator.select");
        }
        String text = font.plainSubstrByWidth(label.getString(), bx + BUTTON_W - 10 - textX);
        guiGraphics.text(font, text, textX, by + 4, 0xFF202020, false);
        guiGraphics.text(font, "v", bx + BUTTON_W - 9, by + 4, 0xFF202020, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // An open popup hides the mouse from everything below it.
        super.extractRenderState(guiGraphics, popupOpen ? -1 : mouseX, popupOpen ? -1 : mouseY, partialTick);
        List<RecipeHolder<CoagulationRecipe>> recipes = recipes();
        if (!popupOpen) {
            gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
            int index = selected();
            if (buttonHovered(mouseX, mouseY) && index >= 0 && index < recipes.size()) {
                guiGraphics.setComponentTooltipForNextFrame(font, tooltip(recipes.get(index).value()), mouseX, mouseY);
            }
            return;
        }
        List<ItemStack> results = recipes.stream().map(holder -> holder.value().result()).toList();
        popup.render(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight,
                Component.translatable("gui.servus_alchemy.coagulator.pick"), results, selected(), mouseX, mouseY);
        int index = popup.indexAt(mouseX, mouseY, results.size());
        if (index >= 0) {
            guiGraphics.setComponentTooltipForNextFrame(font, tooltip(recipes.get(index).value()), mouseX, mouseY);
        }
    }

    private List<Component> tooltip(CoagulationRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(recipe.result().getHoverName());
        for (Principle principle : Principle.values()) {
            int amount = recipe.input().get(principle);
            if (amount > 0) {
                lines.add(Component.translatable("gui.servus_alchemy.coagulator.needs", amount,
                        Component.translatable("fluid_type.servus_alchemy." + principle.id())));
            }
        }
        recipe.ingredient().ifPresent(sized -> {
            ItemStack first = ServusRecipes.firstItem(sized.ingredient());
            if (!first.isEmpty()) {
                lines.add(Component.literal(sized.count() + " x ").append(first.getHoverName()));
            }
        });
        recipe.chapter().ifPresent(chapter -> lines.add(Component.translatable("gui.servus_alchemy.coagulator.chapter",
                Component.translatable("journal." + chapter.getNamespace() + "." + chapter.getPath())).withStyle(ChatFormatting.GRAY)));
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (popupOpen) {
            int count = recipes().size();
            int index = popup.indexAt(mouseX, mouseY, count);
            if (index >= 0 && clickButton(CoagulatorMenu.RECIPE_BUTTON_FIRST + index)) {
                popupOpen = false;
            } else if (!popup.clickBar(mouseX, mouseY, count) && !popup.contains(mouseX, mouseY)) {
                popupOpen = false;
            }
            return true;
        }
        if (gauges.click(this::clickButton, menu.getCarried(), mouseX, mouseY)) {
            return true;
        }
        if (buttonHovered(mouseX, mouseY)) {
            popup.open(leftPos, topPos, imageWidth, imageHeight, recipes().size(), selected());
            popupOpen = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (popupOpen) {
            popup.drag(event.y(), recipes().size());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (popupOpen) {
            popup.release();
            return true;
        }
        return super.mouseReleased(event);
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
    public boolean keyPressed(KeyEvent event) {
        if (popupOpen) {
            if (event.key() == KEY_ESCAPE) {
                popupOpen = false;
            }
            return true;
        }
        return super.keyPressed(event);
    }
}
