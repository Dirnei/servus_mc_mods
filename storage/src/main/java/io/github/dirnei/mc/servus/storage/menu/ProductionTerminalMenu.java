package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.pattern.CraftingGrid;
import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.pattern.PatternEditing;
import io.github.dirnei.mc.servus.storage.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.OrderRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.StepRow;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.production.ProductionView;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Syncs the CPUs, orders, producers and patterns of the terminal's line to the
 * open screen, cancels orders, and runs the pattern editor: nine ghost slots
 * and the player inventory, both active only while the editor is open.
 */
public class ProductionTerminalMenu extends AbstractContainerMenu {

    public static final int POPUP_X = TerminalLayout.Production.POPUP.x();
    public static final int POPUP_Y = TerminalLayout.Production.POPUP.y();
    public static final int INVENTORY_X = TerminalLayout.Production.INVENTORY.x();
    public static final int INVENTORY_Y = TerminalLayout.Production.INVENTORY.y();
    public static final int HOTBAR_Y = TerminalLayout.Production.HOTBAR.y();
    /** Editor state for an existing pattern; the server keeps which one. */
    public static final int EDIT_EXISTING = 0;
    private static final int SYNC_INTERVAL = 10;

    private final ProductionTerminalBlockEntity terminal;
    private final ContainerLevelAccess access;
    private final Player player;
    private final ItemStackHandler grid = new ItemStackHandler(EncodedPattern.GRID);
    private final DataSlot editing = DataSlot.standalone();
    private final int gridStart;
    private PatternLibrary.Free editingSlot;
    private int syncTimer;
    private long lastSignature = Long.MIN_VALUE;
    private long firstSyncTick = -1;

    public ProductionTerminalMenu(int containerId, Inventory playerInventory, ProductionTerminalBlockEntity terminal) {
        super(ServusStorageMenus.PRODUCTION_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
        editing.set(RecipeEditorButtons.EDIT_CLOSED);
        gridStart = slots.size();
        for (int i = 0; i < EncodedPattern.GRID; i++) {
            addSlot(new GhostSlot(() -> grid, i, RecipeEditorButtons.gridX(POPUP_X) + (i % 3) * 18,
                    RecipeEditorButtons.gridY(POPUP_Y) + (i / 3) * 18) {
                @Override
                public boolean isActive() {
                    return isEditing();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return isEditing();
                    }
                });
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INVENTORY_X + column * 18, HOTBAR_Y) {
                @Override
                public boolean isActive() {
                    return isEditing();
                }
            });
        }
        addDataSlot(editing);
    }

    public static ProductionTerminalMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ProductionTerminalBlockEntity terminal) {
            return new ProductionTerminalMenu(containerId, playerInventory, terminal);
        }
        throw new IllegalStateException("No production terminal at " + pos);
    }

    public ProductionTerminalBlockEntity terminal() {
        return terminal;
    }

    /** Cancels an open order of a CPU on the terminal's line. */
    public boolean cancel(BlockPos cpuPos, int orderId) {
        if (!(terminal.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        boolean cancelled = false;
        var line = DataNetworks.networkAt(level, terminal.getBlockPos());
        if (line.isPresent()) {
            for (CpuBlockEntity cpu : CpuOrders.cpus(level, line.get())) {
                if (cpu.getBlockPos().equals(cpuPos)) {
                    cancelled = cpu.book().cancel(orderId);
                }
            }
        }
        resync();
        return cancelled;
    }

    /** What the screen should show now. */
    public ProductionPayload view() {
        if (terminal.getLevel() instanceof ServerLevel level) {
            return ProductionView.of(level, terminal.getBlockPos());
        }
        return new ProductionPayload(false, "no_line", List.of(), List.of(), PatternsView.EMPTY);
    }

    private void resync() {
        lastSignature = Long.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
    }

    // Pattern editor

    /** {@link RecipeEditorButtons#EDIT_CLOSED}, {@link RecipeEditorButtons#EDIT_NEW} or {@link #EDIT_EXISTING}. */
    public int editing() {
        return editing.get();
    }

    public boolean isEditing() {
        return editing() != RecipeEditorButtons.EDIT_CLOSED;
    }

    public boolean isGridSlot(int slotId) {
        return slotId >= gridStart && slotId < gridStart + EncodedPattern.GRID;
    }

    /** The ghost grid's cells, for the preview and for saving. */
    public List<ItemStack> gridCells() {
        List<ItemStack> cells = new ArrayList<>(EncodedPattern.GRID);
        for (int i = 0; i < EncodedPattern.GRID; i++) {
            cells.add(grid.getStackInSlot(i));
        }
        return cells;
    }

    private Optional<DataNetwork> line() {
        return terminal.getLevel() instanceof ServerLevel level ? DataNetworks.networkAt(level, terminal.getBlockPos()) : Optional.empty();
    }

    private void fill(List<ItemStack> template) {
        for (int i = 0; i < EncodedPattern.GRID; i++) {
            grid.setStackInSlot(i, i < template.size() ? template.get(i).copy() : ItemStack.EMPTY);
        }
    }

    private void closeEditor() {
        fill(List.of());
        editingSlot = null;
        editing.set(RecipeEditorButtons.EDIT_CLOSED);
    }

    /** Opens the editor on a new, empty pattern. */
    public void openNew() {
        fill(List.of());
        editingSlot = null;
        editing.set(RecipeEditorButtons.EDIT_NEW);
    }

    /** Opens the editor on a pattern of the line. */
    public boolean openExisting(BlockPos storage, int slot) {
        if (!(terminal.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        Optional<DataNetwork> line = line();
        Optional<PatternLibrary.Entry> entry = line.flatMap(network -> PatternLibrary.find(level, network, storage, slot));
        if (entry.isEmpty()) {
            return false;
        }
        fill(entry.get().pattern().template());
        editingSlot = new PatternLibrary.Free(storage, slot);
        editing.set(EDIT_EXISTING);
        return true;
    }

    /** Opens the editor prefilled with a vanilla crafting recipe from the recipe viewer. */
    public void openEditorWith(ResourceLocation id) {
        if (player.level().isClientSide()) {
            return;
        }
        CraftingGrid.templateOf(player.level(), id).ifPresent(template -> {
            fill(template);
            if (!isEditing()) {
                editingSlot = null;
                editing.set(RecipeEditorButtons.EDIT_NEW);
            }
        });
    }

    /** Puts a single copy of the stack into a ghost cell, as a click with the cursor does. */
    public void setGhost(int cell, ItemStack stack) {
        grid.setStackInSlot(cell, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    /**
     * Saves the editor's grid as a pattern of the line.
     *
     * @return an empty string on success, else the refusal from {@link PatternEditing#save}
     */
    public String save() {
        if (!(terminal.getLevel() instanceof ServerLevel level) || !isEditing()) {
            return "empty";
        }
        Optional<DataNetwork> line = line();
        if (line.isEmpty()) {
            return "full";
        }
        String result = PatternEditing.save(level, line.get(), player, gridCells(), editing() == EDIT_EXISTING ? editingSlot : null);
        if (result.isEmpty()) {
            closeEditor();
            resync();
        }
        return result;
    }

    /** Deletes the pattern being edited and gives the player a Blank Pattern. */
    public boolean delete() {
        if (!(terminal.getLevel() instanceof ServerLevel level) || editing() != EDIT_EXISTING || editingSlot == null) {
            return false;
        }
        boolean deleted = line().map(network -> PatternEditing.delete(level, network, player, editingSlot)).orElse(false);
        closeEditor();
        resync();
        return deleted;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide()) {
            return false;
        }
        if (id == RecipeEditorButtons.OPEN_NEW) {
            openNew();
            return true;
        }
        if (id == RecipeEditorButtons.SAVE && isEditing()) {
            String result = save();
            if (!result.isEmpty()) {
                player.displayClientMessage(Component.translatable("gui.servus_storage.production.pattern_message." + result)
                        .withStyle(ChatFormatting.RED), true);
            }
            return true;
        }
        if (id == RecipeEditorButtons.DELETE && isEditing()) {
            delete();
            return true;
        }
        if (id == RecipeEditorButtons.CANCEL) {
            closeEditor();
            return true;
        }
        return false;
    }

    /** Grid cells take a copy of the cursor and never hand anything out. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isGridSlot(slotId)) {
            if (isEditing() && !player.level().isClientSide()) {
                setGhost(slotId - gridStart, getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            closeEditor();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.PRODUCTION_TERMINAL.get());
    }

    /**
     * Called by {@code setSynchronizer} when the game opens the screen: the
     * content goes out at once instead of with the next periodic check.
     */
    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();
        sendNow();
    }

    /** A resync sends the full content too. */
    @Override
    public void broadcastFullState() {
        super.broadcastFullState();
        sendNow();
    }

    private void sendNow() {
        if (player instanceof ServerPlayer serverPlayer) {
            ProductionPayload payload = view();
            lastSignature = signature(payload);
            syncTimer = 0;
            if (firstSyncTick < 0) {
                firstSyncTick = serverPlayer.serverLevel().getGameTime();
            }
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** The game time the content was first sent, -1 before that. */
    public long firstSyncTick() {
        return firstSyncTick;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (++syncTimer < SYNC_INTERVAL) {
            return;
        }
        syncTimer = 0;
        ProductionPayload payload = view();
        long signature = signature(payload);
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    private static long signature(ProductionPayload payload) {
        long hash = payload.online() ? 1 : 0;
        hash = hash * 31 + payload.reason().hashCode();
        for (MachineRow machine : payload.machines()) {
            hash = hash * 31 + machine.pos().hashCode();
            hash = hash * 31 + machine.state().hashCode();
            hash = hash * 31 + machine.working().getItem().hashCode();
            hash = hash * 31 + machine.left();
            hash = hash * 31 + machine.recipes().size();
            for (var recipe : machine.recipes()) {
                for (ItemStack output : recipe.outputs()) {
                    hash = hash * 31 + output.getItem().hashCode() * 7 + output.getCount();
                }
            }
        }
        for (CpuRow cpu : payload.cpus()) {
            hash = hash * 31 + cpu.cpu().hashCode();
            hash = hash * 31 + cpu.slots() * 7 + cpu.inUse() * 3 + cpu.waiting();
            for (OrderRow order : cpu.orders()) {
                hash = hash * 31 + order.id();
                hash = hash * 31 + order.state().hashCode();
                hash = hash * 31 + order.reason().hashCode();
                for (StepRow step : order.steps()) {
                    hash = hash * 31 + step.state().hashCode();
                    hash = hash * 31 + step.reason().hashCode();
                }
            }
        }
        PatternsView patterns = payload.patterns();
        hash = hash * 31 + patterns.used() * 7 + patterns.total();
        for (PatternRow row : patterns.rows()) {
            hash = hash * 31 + row.storage().hashCode() + row.slot();
            hash = hash * 31 + row.result().getItem().hashCode() * 7 + row.result().getCount();
            for (ItemStack input : row.inputs()) {
                hash = hash * 31 + input.getItem().hashCode() * 7 + input.getCount();
            }
        }
        return hash;
    }
}
