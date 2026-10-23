package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.HexColor;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTierData;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Phases;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Sound;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Sounds;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Turn;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the Athanor rituals from {@code data/<namespace>/servus_alchemy/athanor_ritual/<name>.json}. */
public final class AthanorRitualData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "athanor_ritual_data");

    public static final String DIRECTORY = "servus_alchemy/athanor_ritual";

    /** The rituals Alchemy ships, by file name. */
    public static final List<String> SHIPPED = List.of("citrinitas", "rubedo");

    private static final Codec<Integer> COLOR = Codec.STRING.xmap(color -> HexColor.parse(color).orElse(0xFFFFFF), HexColor::format);

    private static final Codec<Phases> PHASES = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("burst").forGetter(Phases::burst),
            Codec.INT.fieldOf("fill").forGetter(Phases::fill),
            Codec.INT.fieldOf("grow").forGetter(Phases::grow),
            Codec.INT.fieldOf("settle").forGetter(Phases::settle)
    ).apply(instance, Phases::new));

    private static final Codec<Sound> SOUND = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Sound::id),
            Codec.FLOAT.optionalFieldOf("volume", 1F).forGetter(Sound::volume),
            Codec.FLOAT.optionalFieldOf("pitch", 1F).forGetter(Sound::pitch)
    ).apply(instance, Sound::new));

    private static final Codec<Sounds> SOUNDS = RecordCodecBuilder.create(instance -> instance.group(
            SOUND.optionalFieldOf("offering").forGetter(Sounds::offering),
            SOUND.optionalFieldOf("burst").forGetter(Sounds::burst),
            SOUND.optionalFieldOf("land").forGetter(Sounds::land),
            SOUND.optionalFieldOf("grow").forGetter(Sounds::grow),
            SOUND.optionalFieldOf("end").forGetter(Sounds::end)
    ).apply(instance, Sounds::new));

    private static final Codec<Turn> TURN = RecordCodecBuilder.create(instance -> instance.group(
            AthanorTierData.OFFSET.forGetter(turn -> List.of(turn.from().dx(), turn.from().dy(), turn.from().dz())),
            AthanorTierData.STATE_CODEC.fieldOf("from").forGetter(Turn::from),
            AthanorTierData.STATE_CODEC.fieldOf("to").forGetter(Turn::to),
            Codec.BOOL.optionalFieldOf("stream", true).forGetter(Turn::stream)
    ).apply(instance, (offset, from, to, stream) -> new Turn(at(from, offset), at(to, offset), stream)));

    /** A ritual as written in JSON; the name comes from the file and is only written when a ritual is saved or synced. */
    public static final Codec<AthanorRitual> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(AthanorRitual::name),
            Codec.STRING.fieldOf("offering").forGetter(AthanorRitual::offering),
            Codec.intRange(0, 3).fieldOf("tier").forGetter(AthanorRitual::tier),
            COLOR.fieldOf("color").forGetter(AthanorRitual::color),
            COLOR.fieldOf("result_color").forGetter(AthanorRitual::resultColor),
            Codec.intRange(1, 1200).fieldOf("duration").forGetter(AthanorRitual::duration),
            PHASES.fieldOf("phases").forGetter(AthanorRitual::phases),
            SOUNDS.optionalFieldOf("sounds", Sounds.NONE).forGetter(AthanorRitual::sounds),
            TURN.listOf().fieldOf("turns").forGetter(AthanorRitual::turns)
    ).apply(instance, AthanorRitual::new));

    public static final AthanorRitualData INSTANCE = new AthanorRitualData();

    private volatile List<AthanorRitual> rituals = List.of();

    private AthanorRitualData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    private static BlockAt at(BlockAt block, List<Integer> offset) {
        return block.at(offset.get(0), offset.get(1), offset.get(2));
    }

    /** Reads a shipped ritual straight from the mod's own resources, for the data generator and the tests. */
    public static AthanorRitual readShipped(String name) {
        String path = "/data/servus_alchemy/" + DIRECTORY + "/" + name + ".json";
        try (var stream = AthanorRitualData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing shipped Athanor ritual " + path);
            }
            JsonElement json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return named(CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), name);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read shipped Athanor ritual " + path, e);
        }
    }

    private static AthanorRitual named(AthanorRitual ritual, String name) {
        return new AthanorRitual(name, ritual.offering(), ritual.tier(), ritual.color(), ritual.resultColor(), ritual.duration(),
                ritual.phases(), ritual.sounds(), ritual.turns());
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<AthanorRitual> loaded = files.entrySet().stream()
                .flatMap(entry -> parse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(AthanorRitual::tier))
                .toList();
        rituals = loaded;
        ServusAlchemy.LOGGER.info("Loaded {} Athanor rituals", loaded.size());
    }

    /** A ritual file, or nothing when it is invalid; the records refuse phases out of order and turns that move. */
    private static Stream<AthanorRitual> parse(Identifier id, JsonElement json) {
        try {
            return CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid Athanor ritual {}: {}", id, error))
                    .map(ritual -> named(ritual, id.getPath()))
                    .stream();
        } catch (IllegalArgumentException e) {
            ServusAlchemy.LOGGER.error("Invalid Athanor ritual {}: {}", id, e.getMessage());
            return Stream.empty();
        }
    }

    public List<AthanorRitual> rituals() {
        return rituals;
    }

    /** The rituals whose offering is the item {@code item}. */
    public List<AthanorRitual> forOffering(String item) {
        return rituals.stream().filter(ritual -> ritual.offering().equals(item)).toList();
    }

    /** A ritual as NBT, with its name, for the block entity's save and its client update. */
    public static Tag toTag(AthanorRitual ritual) {
        return CODEC.encodeStart(NbtOps.INSTANCE, ritual).getOrThrow();
    }

    /** A ritual read back from {@link #toTag}, empty when it no longer reads. */
    public static Optional<AthanorRitual> fromTag(Tag tag) {
        try {
            return CODEC.parse(NbtOps.INSTANCE, tag).result();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
