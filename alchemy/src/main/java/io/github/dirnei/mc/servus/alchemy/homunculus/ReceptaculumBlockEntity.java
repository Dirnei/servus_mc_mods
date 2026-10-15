package io.github.dirnei.mc.servus.alchemy.homunculus;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Gift;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinData.VeinDef;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Receptaculum: catches what the homunculus in the Vas above harvests in its yield slots, and holds the gifts it
 * brings the gnomes and the Elixir it lives on. Pipes take the yield and fill the gifts and the Elixir; the data line
 * sees the yields of the Hollows in the homunculus' reach. Its fire never goes out.
 */
public class ReceptaculumBlockEntity extends MachineBlockEntity {

    public static final int YIELD_SLOTS = 27;
    public static final int GIFT_FIRST = YIELD_SLOTS;
    public static final int GIFT_SLOTS = 3;
    public static final int SLOT_ELIXIR = GIFT_FIRST + GIFT_SLOTS;
    public static final int SLOTS = SLOT_ELIXIR + 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "receptaculum");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final ProductionProvider production = new ProductionProvider() {
        @Override
        public List<ProductionRecipe> recipes() {
            return vasAbove().filter(vas -> vas.life().grown()).map(ReceptaculumBlockEntity::recipes).orElse(List.of());
        }

        @Override
        public Optional<Identifier> active() {
            Optional<VasBlockEntity> vas = vasAbove();
            if (vas.isEmpty() || !vas.get().works(hasElixir())) {
                return Optional.empty();
            }
            return recipes().stream().map(ProductionRecipe::id).findFirst();
        }
    };

    public ReceptaculumBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.RECEPTACULUM.get(), pos, state, SLOTS);
    }

    @Override
    protected Identifier machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= GIFT_FIRST && slot <= SLOT_ELIXIR;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot < YIELD_SLOTS;
    }

    /** The Elixir slot takes only the Elixir of Sight; the gift slots take anything else. */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        boolean elixir = stack.is(ElementalSight.ELIXIR.get());
        return slot == SLOT_ELIXIR ? elixir : !elixir;
    }

    /** The fire always burns. */
    @Override
    protected boolean tick() {
        return true;
    }

    public boolean hasElixir() {
        return !inventory.getStackInSlot(SLOT_ELIXIR).isEmpty();
    }

    /** Uses up one Elixir of Sight; returns whether there was one. */
    public boolean takeElixir() {
        return !inventory.extractItem(SLOT_ELIXIR, 1, false).isEmpty();
    }

    /** How many items like {@code stack} the yield slots still take. */
    public int yieldRoom(ItemStack stack) {
        int room = 0;
        ItemStack full = stack.copyWithCount(stack.getMaxStackSize());
        for (int slot = 0; slot < YIELD_SLOTS; slot++) {
            room += full.getCount() - inventory.insertItem(slot, full, true).getCount();
        }
        return room;
    }

    /** Puts {@code stack} into the yield slots and returns what did not fit. */
    public ItemStack insertYield(ItemStack stack) {
        ItemStack left = stack.copy();
        for (int slot = 0; slot < YIELD_SLOTS && !left.isEmpty(); slot++) {
            left = inventory.insertItem(slot, left, false);
        }
        return left;
    }

    /** Offers the gift slots to the gnome in order; the first gift it takes loses one item. Returns whether a gift was taken. */
    public boolean giveGift(GnomeHollowBlockEntity hollow) {
        for (int slot = GIFT_FIRST; slot < GIFT_FIRST + GIFT_SLOTS; slot++) {
            ItemStack gift = inventory.getStackInSlot(slot);
            if (gift.isEmpty()) {
                continue;
            }
            Gift answer = hollow.offerGift(gift);
            if (answer == Gift.TAKEN) {
                inventory.extractItem(slot, 1, false);
                return true;
            }
            if (answer == Gift.NO_GNOME) {
                return false;
            }
        }
        return false;
    }

    private Optional<VasBlockEntity> vasAbove() {
        return level != null && level.getBlockEntity(worldPosition.above()) instanceof VasBlockEntity vas ? Optional.of(vas) : Optional.empty();
    }

    /** One recipe per vein of the bound, ripe Hollows in the reach of {@code vas}: nothing in, its yield out. */
    private static List<ProductionRecipe> recipes(VasBlockEntity vas) {
        int multiplier = vas.multiplier();
        Map<Identifier, ProductionRecipe> recipes = new LinkedHashMap<>();
        for (GnomeHollowBlockEntity hollow : vas.hollowsInReach()) {
            Optional<VeinDef> def = hollow.veinDef();
            Optional<Identifier> vein = hollow.vein();
            if (def.isEmpty() || vein.isEmpty() || !hollow.growth().bound || !hollow.growth().ripe(def.get().growth())) {
                continue;
            }
            recipes.putIfAbsent(vein.get(), new ProductionRecipe(vein.get(), List.of(),
                    List.of(def.get().yield().copyWithCount(multiplier)), def.get().growth().yieldTicks()));
        }
        return List.copyOf(recipes.values());
    }

    public ProductionProvider production() {
        return production;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReceptaculumMenu(containerId, playerInventory, this);
    }
}
