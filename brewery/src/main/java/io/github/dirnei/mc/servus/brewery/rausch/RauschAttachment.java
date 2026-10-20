package io.github.dirnei.mc.servus.brewery.rausch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The player data that holds each player's {@link Rausch}. Saved with the player, not copied on death. */
public final class RauschAttachment {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ServusBreweryIds.MOD_ID);

    private static final MapCodec<Rausch> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("points").forGetter(Rausch::points),
            Codec.INT.fieldOf("progress").forGetter(Rausch::progress),
            Codec.LONG.fieldOf("due_day").forGetter(Rausch::dueDay),
            Codec.LONG.fieldOf("hangover_until").forGetter(Rausch::hangoverUntil)
    ).apply(instance, Rausch::new));

    public static final Supplier<AttachmentType<Rausch>> RAUSCH = ATTACHMENT_TYPES.register("rausch",
            () -> AttachmentType.builder(() -> Rausch.SOBER).serialize(CODEC).build());

    private RauschAttachment() {
    }
}
