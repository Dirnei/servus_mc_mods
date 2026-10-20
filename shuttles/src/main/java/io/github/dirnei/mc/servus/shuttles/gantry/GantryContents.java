package io.github.dirnei.mc.servus.shuttles.gantry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a dropped Fluid Gantry item keeps: both tanks, their locks (a fluid id,
 * empty for Any), the mode and how many Capacity Upgrades it holds.
 */
public record GantryContents(FluidStack left, FluidStack right, Optional<Identifier> leftLock, Optional<Identifier> rightLock,
                             GantryMode mode, int upgrades) {

    public static final GantryContents EMPTY =
            new GantryContents(FluidStack.EMPTY, FluidStack.EMPTY, Optional.empty(), Optional.empty(), GantryMode.STORAGE, 0);

    public static final Codec<GantryContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.OPTIONAL_CODEC.optionalFieldOf("left", FluidStack.EMPTY).forGetter(GantryContents::left),
            FluidStack.OPTIONAL_CODEC.optionalFieldOf("right", FluidStack.EMPTY).forGetter(GantryContents::right),
            Identifier.CODEC.optionalFieldOf("left_lock").forGetter(GantryContents::leftLock),
            Identifier.CODEC.optionalFieldOf("right_lock").forGetter(GantryContents::rightLock),
            GantryMode.CODEC.optionalFieldOf("mode", GantryMode.STORAGE).forGetter(GantryContents::mode),
            Codec.INT.optionalFieldOf("upgrades", 0).forGetter(GantryContents::upgrades)
    ).apply(instance, GantryContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GantryContents> STREAM_CODEC = StreamCodec.composite(
            FluidStack.OPTIONAL_STREAM_CODEC, GantryContents::left,
            FluidStack.OPTIONAL_STREAM_CODEC, GantryContents::right,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), GantryContents::leftLock,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), GantryContents::rightLock,
            GantryMode.STREAM_CODEC, GantryContents::mode,
            ByteBufCodecs.VAR_INT, GantryContents::upgrades,
            GantryContents::new);

    /** The fluid of tank 0 (left) or 1 (right). */
    public FluidStack tank(int tank) {
        return tank == 0 ? left : right;
    }

    /** The lock of tank 0 (left) or 1 (right). */
    public Optional<Identifier> lock(int tank) {
        return tank == 0 ? leftLock : rightLock;
    }

    /** Whether a gantry with these contents is the same as a freshly crafted one. */
    public boolean isEmpty() {
        return equals(EMPTY);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GantryContents that
                && FluidStack.matches(left, that.left) && FluidStack.matches(right, that.right)
                && leftLock.equals(that.leftLock) && rightLock.equals(that.rightLock)
                && mode == that.mode && upgrades == that.upgrades;
    }

    @Override
    public int hashCode() {
        return Objects.hash(FluidStack.hashFluidAndComponents(left), left.getAmount(), FluidStack.hashFluidAndComponents(right),
                right.getAmount(), leftLock, rightLock, mode, upgrades);
    }
}
