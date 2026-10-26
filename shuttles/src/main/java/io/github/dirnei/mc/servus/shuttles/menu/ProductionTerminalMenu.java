package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.shuttles.pattern.CraftingGrid;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.pattern.AnyGroup;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternEditing;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternGrid;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.shuttles.production.ProductionPayload;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.JobRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.PatternRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.shuttles.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.production.ProductionView;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalWarehouses;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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

    private final ProductionTerminalBlockEntity terminal;
    private final ContainerLevelAccess access;
    private final Player player;
    private final PatternGrid grid = new PatternGrid();
    private final DataSlot editing = DataSlot.standalone();
    private final int gridStart;
    private final PayloadSync<ProductionPayload> sync = new PayloadSync<>(10, false, this::view, ProductionTerminalMenu::signature);
    private PatternLibrary.Free editingSlot;
    private long firstSyncTick = -1;

    public ProductionTerminalMenu(int containerId, Inventory playerInventory, ProductionTerminalBlockEntity terminal) {
        super(ServusShuttlesMenus.PRODUCTION_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
        editing.set(RecipeEditorButtons.EDIT_CLOSED);
        gridStart = slots.size();
        grid.slots(RecipeEditorButtons.gridX(POPUP_X), RecipeEditorButtons.gridY(POPUP_Y, true), this::isEditing).forEach(this::addSlot);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(editorSlot(playerInventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(editorSlot(playerInventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        addDataSlot(editing);
        addDataSlot(grid.modeSlot());
    }

    /** A player inventory slot that is there only while the editor is open. */
    private Slot editorSlot(Inventory playerInventory, int index, int x, int y) {
        return new Slot(playerInventory, index, x, y) {
            @Override
            public boolean isActive() {
                return isEditing();
            }
        };
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
        sync.force();
        return cancelled;
    }

    /** What the screen should show now. */
    public ProductionPayload view() {
        if (terminal.getLevel() instanceof ServerLevel level) {
            return ProductionView.of(level, terminal.getBlockPos());
        }
        return new ProductionPayload(terminal.getBlockPos(), false, "no_line", List.of(), List.of(), List.of(), PatternsView.EMPTY);
    }

    /** {@link RecipeEditorButtons#EDIT_CLOSED}, {@link RecipeEditorButtons#EDIT_NEW} or {@link #EDIT_EXISTING}. */
    public int editing() {
        return editing.get();
    }

    public boolean isEditing() {
        return editing() != RecipeEditorButtons.EDIT_CLOSED;
    }

    /** The mode asked for in the editor; the pattern is saved Exact where Any is not possible. */
    public EncodedPattern.Mode mode() {
        return grid.mode();
    }

    /** Index of the first ghost cell of the editor. */
    public int gridStart() {
        return gridStart;
    }

    public boolean isGridSlot(int slotId) {
        return slotId >= gridStart && slotId < gridStart + EncodedPattern.GRID;
    }

    /** The ghost grid's cells, for the preview and for saving. */
    public List<ItemStack> gridCells() {
        return grid.cells();
    }

    private Optional<DataNetwork> line() {
        return terminal.getLevel() instanceof ServerLevel level ? DataNetworks.networkAt(level, terminal.getBlockPos()) : Optional.empty();
    }

    private void closeEditor() {
        grid.clear();
        editingSlot = null;
        editing.set(RecipeEditorButtons.EDIT_CLOSED);
    }

    /** Opens the editor on a new, empty pattern. */
    public void openNew() {
        grid.clear();
        editingSlot = null;
        grid.resetMode();
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
        grid.fill(entry.get().pattern().template());
        grid.setMode(entry.get().pattern().mode());
        editingSlot = new PatternLibrary.Free(storage, slot);
        editing.set(EDIT_EXISTING);
        return true;
    }

    /**
     * Opens the editor prefilled with a vanilla crafting recipe from the recipe
     * viewer, each cell holding the accepted stack the line's warehouse has the most of.
     */
    public void openEditorWith(Identifier id) {
        if (!(terminal.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Map.Entry<ItemKey, Long>> stock = TerminalWarehouses.online(level, terminal.getBlockPos())
                .map(TerminalWarehouses::stock).orElse(List.of());
        CraftingGrid.templateOf(level, id, stock).ifPresent(template -> {
            grid.fill(template);
            if (!isEditing()) {
                editingSlot = null;
                grid.resetMode();
                editing.set(RecipeEditorButtons.EDIT_NEW);
            }
        });
    }

    /** Puts a single copy of the stack into a ghost cell, as a click with the cursor does. */
    public void setGhost(int cell, ItemStack stack) {
        grid.setGhost(cell, stack);
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
        String result = PatternEditing.save(level, line.get(), player, gridCells(), editing() == EDIT_EXISTING ? editingSlot : null, mode());
        if (result.isEmpty()) {
            closeEditor();
            sync.force();
        }
        return result;
    }

    /** Deletes the pattern being edited and closes the editor. */
    public boolean delete() {
        if (!(terminal.getLevel() instanceof ServerLevel level) || editing() != EDIT_EXISTING || editingSlot == null) {
            return false;
        }
        boolean deleted = line().map(network -> PatternEditing.delete(level, network, player, editingSlot)).orElse(false);
        closeEditor();
        sync.force();
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
                player.sendOverlayMessage(Component.translatable("gui.servus_shuttles.production.pattern_message." + result)
                        .withStyle(ChatFormatting.RED));
            }
            return true;
        }
        if ((id == RecipeEditorButtons.MODE || id == RecipeEditorButtons.CLEAR) && isEditing()) {
            return grid.button(id);
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
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
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
        return stillValid(access, player, ServusShuttlesBlocks.PRODUCTION_TERMINAL.get());
    }

    /** Sends the content at once when the screen opens instead of with the next periodic check. */
    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();
        sendNow();
    }

    @Override
    public void broadcastFullState() {
        super.broadcastFullState();
        sendNow();
    }

    private void sendNow() {
        if (player instanceof ServerPlayer serverPlayer) {
            if (firstSyncTick < 0) {
                firstSyncTick = serverPlayer.level().getGameTime();
            }
            sync.sendNow(serverPlayer);
        }
    }

    /** The game time the content was first sent, -1 before that. */
    public long firstSyncTick() {
        return firstSyncTick;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        sync.tick(player);
    }

    private static long signature(ProductionPayload payload) {
        long hash = payload.online() ? 1 : 0;
        hash = hash * 31 + payload.reason().hashCode();
        for (MachineRow machine : payload.machines()) {
            hash = hash * 31 + machine.pos().hashCode();
            hash = hash * 31 + machine.state().hashCode();
            hash = hash * 31 + machine.item().getItem().hashCode();
            hash = hash * 31 + machine.left();
            hash = hash * 31 + machine.reason().hashCode();
            hash = hash * 31 + machine.upkeepCount() * 7 + machine.upkeepLevel();
            hash = hash * 31 + machine.makes();
        }
        for (CpuRow cpu : payload.cpus()) {
            hash = hash * 31 + cpu.cpu().hashCode();
            hash = hash * 31 + cpu.slots() * 7 + cpu.inUse() * 3 + cpu.waiting();
            hash = hash * 31 + cpu.room() * 7 + cpu.used();
            for (ProductionRows.BufferItem item : cpu.buffered()) {
                hash = hash * 31 + item.item().getItem().hashCode() * 7 + item.count();
                hash = hash * 31 + item.order() * 2 + (item.leaving() ? 1 : 0);
            }
        }
        for (JobRow job : payload.jobs()) {
            hash = hash * 31 + job.cpu().hashCode() + job.id();
            hash = hash * 31 + job.state().hashCode();
            hash = hash * 31 + job.crafted();
            hash = hash * 31 + Long.hashCode(job.doneAt());
            hash = hash * 31 + job.status().hashCode();
            hash = hash * 31 + job.args().hashCode();
            for (ProductionRows.BufferLine line : job.buffer()) {
                hash = hash * 31 + line.machine().hashCode();
                for (ProductionRows.ItemCount item : line.items()) {
                    hash = hash * 31 + item.item().getItem().hashCode() * 7 + item.count();
                }
            }
            for (ProductionRows.ItemCount item : job.overflow()) {
                hash = hash * 31 + item.item().getItem().hashCode() * 7 + item.count();
            }
        }
        for (ProductionRows.PackingRow station : payload.packing()) {
            hash = hash * 31 + station.pos().hashCode();
            hash = hash * 31 + station.toStorage() * 7 + station.toNetwork() * 3 + station.rate();
        }
        for (ProductionRows.DuctRow duct : payload.busiest()) {
            hash = hash * 31 + duct.pos().hashCode() + duct.name().hashCode();
            hash = hash * 31 + duct.load() * 7 + duct.bandwidth();
        }
        for (ProductionRows.StorageRow row : payload.storage()) {
            hash = hash * 31 + row.hashCode();
        }
        PatternsView patterns = payload.patterns();
        hash = hash * 31 + patterns.used() * 7 + patterns.total();
        for (PatternRow row : patterns.rows()) {
            hash = hash * 31 + row.storage().hashCode() + row.slot();
            hash = hash * 31 + row.result().getItem().hashCode() * 7 + row.result().getCount();
            for (AnyGroup group : row.any()) {
                hash = hash * 31 + group.tagKey().hashCode() * 7 + group.total();
                for (ItemStack item : group.items()) {
                    hash = hash * 31 + item.getItem().hashCode();
                }
            }
            for (ItemStack input : row.inputs()) {
                hash = hash * 31 + input.getItem().hashCode() * 7 + input.getCount();
            }
        }
        return hash;
    }
}
