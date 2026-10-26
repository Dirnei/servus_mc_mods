package io.github.dirnei.mc.servus.shuttles.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The station counters a dismantled Warehouse Controller carries in its item. */
public record CarriedCounters(int inbound, int outbound, int packing) {

    public static final Codec<CarriedCounters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("inbound").forGetter(CarriedCounters::inbound),
            Codec.INT.fieldOf("outbound").forGetter(CarriedCounters::outbound),
            Codec.INT.optionalFieldOf("packing", 0).forGetter(CarriedCounters::packing)
    ).apply(instance, CarriedCounters::new));

    public static final StreamCodec<ByteBuf, CarriedCounters> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CarriedCounters::inbound,
            ByteBufCodecs.VAR_INT, CarriedCounters::outbound,
            ByteBufCodecs.VAR_INT, CarriedCounters::packing,
            CarriedCounters::new);

    public static CarriedCounters of(StationCounters counters) {
        return new CarriedCounters(counters.last(StationCounters.Kind.INBOUND), counters.last(StationCounters.Kind.OUTBOUND),
                counters.last(StationCounters.Kind.PACKING));
    }

    public StationCounters counters() {
        return new StationCounters(inbound, outbound, packing);
    }
}
