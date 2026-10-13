package io.github.dirnei.mc.servus.manufacturing.journal;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.TaskDef;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the journal chapters from {@code data/<namespace>/servus_manufacturing/journal/<chapter>.json}. */
public final class JournalChapters extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("servus_manufacturing", "journal_chapters");

    public static final String DIRECTORY = "servus_manufacturing/journal";

    private static final Codec<TaskDef> TASK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(TaskDef::id),
            Codec.STRING.fieldOf("event").forGetter(TaskDef::event),
            Codec.STRING.optionalFieldOf("filter", "").forGetter(TaskDef::filter),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(TaskDef::count)
    ).apply(instance, TaskDef::new));

    /** The chapter as written in JSON; the id comes from the file name. */
    private record ChapterJson(Optional<Identifier> parent, int order, List<TaskDef> tasks) {

        static final Codec<ChapterJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.optionalFieldOf("parent").forGetter(ChapterJson::parent),
                Codec.INT.optionalFieldOf("order", 0).forGetter(ChapterJson::order),
                TASK_CODEC.listOf().optionalFieldOf("tasks", List.of()).forGetter(ChapterJson::tasks)
        ).apply(instance, ChapterJson::new));
    }

    public static final JournalChapters INSTANCE = new JournalChapters();

    private volatile List<ChapterDef> chapters = List.of();

    private JournalChapters() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<ChapterDef> loaded = files.entrySet().stream()
                .flatMap(entry -> ChapterJson.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> ServusManufacturing.LOGGER.error("Invalid journal chapter {}: {}", entry.getKey(), error))
                        .map(json -> new ChapterDef(entry.getKey().toString(), json.parent().map(Identifier::toString),
                                json.order(), json.tasks()))
                        .stream())
                .sorted(Comparator.comparingInt(ChapterDef::order).thenComparing(ChapterDef::id))
                .toList();
        chapters = loaded;
        ServusManufacturing.LOGGER.info("Loaded {} journal chapters", loaded.size());
    }

    /** Every chapter in order. */
    public List<ChapterDef> chapters() {
        return chapters;
    }
}
