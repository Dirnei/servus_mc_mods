package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulatorMenu;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorSeeds;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Coagulator: the seed slot under the arrow, the ingredient and output slots and the principle gauges. */
public class CoagulatorScreen extends MachineScreen {

    /** No side panel: the Coagulator's ports are fixed by its build. */
    public static final Layout LAYOUT = new Layout(false, true, false, false, false, MachineScreens.gui("coagulator"));

    private final PrincipleGauges gauges;

    public CoagulatorScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, LAYOUT);
        this.gauges = new PrincipleGauges((PrincipleMenu) menu);
    }

    private CoagulatorMenu coagulatorMenu() {
        return (CoagulatorMenu) menu;
    }

    @Override
    protected void init() {
        super.init();
        gauges.place(leftPos, topPos);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        gauges.render(guiGraphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
        Slot seed = coagulatorMenu().seedSlot();
        if (menu.getCarried().isEmpty() && isHovering(seed.x, seed.y, 16, 16, mouseX, mouseY)) {
            showTooltip(guiGraphics, seedTooltip(seed.getItem()), mouseX, mouseY);
        }
    }

    /** Shows the tooltip in place of the item's own one. */
    private void showTooltip(GuiGraphicsExtractor guiGraphics, List<Component> lines, int mouseX, int mouseY) {
        guiGraphics.setTooltipForNextFrame(font, lines.stream().map(Component::getVisualOrderText).toList(),
                DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
    }

    /** What the seed grows and needs; for an empty slot, what belongs there. */
    private List<Component> seedTooltip(ItemStack seed) {
        if (seed.isEmpty() || minecraft == null || minecraft.level == null) {
            return List.of(Component.translatable("gui.servus_alchemy.coagulator.seed"),
                    Component.translatable("gui.servus_alchemy.coagulator.seed.empty").withStyle(ChatFormatting.GRAY));
        }
        List<Component> lines = new ArrayList<>();
        lines.add(seed.getHoverName());
        CoagulatorSeeds.recipeFor(minecraft.level, seed).map(holder -> holder.value()).ifPresent(recipe -> {
            lines.add(seedUse(recipe, seed));
            addRecipe(lines, recipe);
        });
        return lines;
    }

    /** That one seed is used up per cycle, and what it grows into. */
    public static Component seedUse(CoagulationRecipe recipe, ItemStack seed) {
        ItemStack grown = recipe.resultFrom(seed);
        return Component.translatable("gui.servus_alchemy.coagulator.seed.used_up", grown.getCount(), grown.getHoverName())
                .withStyle(ChatFormatting.GRAY);
    }

    private void addRecipe(List<Component> lines, CoagulationRecipe recipe) {
        lines.add(Component.translatable("gui.servus_alchemy.coagulator.grows", recipe.result().getHoverName()).withStyle(ChatFormatting.GRAY));
        for (Principle principle : Principle.values()) {
            int amount = recipe.input().get(principle);
            if (amount > 0) {
                lines.add(Component.translatable("gui.servus_alchemy.coagulator.needs", amount,
                        Component.translatable("fluid_type.servus_alchemy." + principle.id())).withStyle(ChatFormatting.GRAY));
            }
        }
        recipe.ingredient().ifPresent(sized -> {
            ItemStack first = ServusRecipes.firstItem(sized.ingredient());
            if (!first.isEmpty()) {
                lines.add(Component.literal(sized.count() + " x ").append(first.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        });
        recipe.chapter().ifPresent(chapter -> lines.add(Component.translatable("gui.servus_alchemy.coagulator.chapter",
                Component.translatable("journal." + chapter.getNamespace() + "." + chapter.getPath()))
                .withStyle(coagulatorMenu().seedLocked() ? ChatFormatting.RED : ChatFormatting.GRAY)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (gauges.click(this::clickButton, menu.getCarried(), event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
